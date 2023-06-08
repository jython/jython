// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandle;
import java.util.EnumSet;
import java.util.Map;

import org.python.base.InterpreterError;
import org.python.core.PyCode.Layout;
import org.python.core.PyCode.Trait;
import org.python.core.PyDict.MergeMode;

/** A {@link PyFrame} for executing CPython 3.11 byte code. */
class CPython311Frame extends PyFrame<CPython311Code> {

    /**
     * All local variables, named in {@link Layout#localnames()
     * code.layout.localnames}.
     */
    final Object[] fastlocals;

    /** Value stack. */
    final Object[] valuestack;

    /** Index of first empty space on the value stack. */
    int stacktop = 0;

    /** Assigned eventually by return statement (or stays None). */
    Object returnValue = Py.None;

    /**
     * The built-in objects from {@link #func}, wrapped (if necessary)
     * to make it a {@code Map}. Inside the wrapper it will be accessed
     * using the Python mapping protocol.
     */
    private final Map<Object, Object> builtins;

    /**
     * Create a {@code CPython38Frame}, which is a {@code PyFrame} with
     * the storage and mechanism to execute a module or isolated code
     * object (compiled to a {@link CPython311Code}.
     * <p>
     * This will set the {@link #func} and (sometimes) {@link #locals}
     * fields of the frame. The {@code globals} and {@code builtins}
     * properties, exposed to Python as {@code f_globals} and
     * {@code f_builtins}, are determined by {@code func}.
     * <p>
     * The func argument also locates the code object for the frame, the
     * properties of which determine many characteristics of the frame.
     * <ul>
     * <li>If the {@code code} argument has the {@link Trait#NEWLOCALS}
     * the {@code locals} argument is ignored.
     * <ul>
     * <li>If the code does not additionally have the trait
     * {@link Trait#OPTIMIZED}, a new empty {@code dict} will be
     * provided as {@link #locals}.</li>
     * <li>Otherwise, the code has the trait {@code OPTIMIZED}, and
     * {@link #locals} will be {@code null} until possibly set
     * later.</li>
     * </ul>
     * </li>
     * <li>Otherwise, {@code code} does not have the trait
     * {@code NEWLOCALS} and expects an object with the map protocol to
     * act as {@link PyFrame#locals}.
     * <ul>
     * <li>If the argument {@code locals} is not {@code null} it
     * specifies {@link #locals}.</li>
     * <li>Otherwise, the argument {@code locals} is {@code null} and
     * {@link #locals} will be the same as {@code #globals}.</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param func that this frame executes
     * @param locals local name space (may be {@code null})
     */
    // Compare CPython _PyFrame_New_NoTrack in frameobject.c
    protected CPython311Frame(CPython311Function func, Object locals) {

        // Initialise the basics.
        super(func);

        CPython311Code code = func.code;
        this.valuestack = new Object[code.stacksize];
        int nfast = 0;

        // The need for a dictionary of locals depends on the code
        EnumSet<PyCode.Trait> traits = code.traits;
        if (traits.contains(Trait.NEWLOCALS)) {
            // Ignore locals argument
            if (traits.contains(Trait.OPTIMIZED)) {
                // We can create it later but probably won't need to
                this.locals = null;
                // Instead locals are in an array
                nfast = code.layout.size();
            } else {
                this.locals = new PyDict();
            }
        } else if (locals == null) {
            // Default to same as globals.
            this.locals = func.globals;
        } else {
            /*
             * Use supplied locals. As it may not implement j.u.Map, we wrap any
             * Python object as a Map. Depending on the operations attempted,
             * this may break later.
             */
            this.locals = locals;
        }

        // Locally present the func.__builtins__ as a Map
        this.builtins = PyMapping.map(func.builtins);

        // Initialise local variables (plain and cell)
        this.fastlocals = nfast > 0 ? new Object[nfast] : EMPTY_OBJECT_ARRAY;
        // Free variables are initialised by opcode COPY_FREE_VARS
    }

    @Override
    Object eval() {

        // Evaluation stack and index
        final Object[] s = valuestack;
        int sp = stacktop;

        /*
         * Because we use a word array, our ip is half the CPython ip. The
         * latter, and all jump arguments, are always even, so we have to
         * halve the jump distances or destinations.
         */
        int ip = 0;

        /*
         * We read each 16-bit instruction from wordcode[] into opword. Bits
         * 8-15 are the opcode itself. The bottom 8 bits are an argument.
         * (The oparg after an EXTENDED_ARG gets special treatment to
         * produce the chaining of argument values.)
         */
        final CPython311Code code = this.code;
        int opword = code.wordcode[ip++] & 0xffff;

        // Opcode argument (where needed).
        int oparg = opword & 0xff;

        // @formatter:off
        // The structure of the interpreter loop is:
        // while (ip <= END) {
        //     switch (opword >> 8) {
        //     case Opcode311.LOAD_CONST:
        //         s[sp++] = consts[oparg]; break;
        //     // other cases
        //     case Opcode311.RETURN_VALUE:
        //         returnValue = s[--sp]; break loop;
        //     case Opcode311.EXTENDED_ARG:
        //         opword = wordcode[ip++] & 0xffff;
        //         oparg = (oparg << 8) | opword & 0xff;
        //         continue;
        //     default:
        //         throw new InterpreterError("...");
        //     }
        //     opword = wordcode[ip++] & 0xffff;
        //     oparg = opword & 0xff;
        // }
        // @formatter:on

        // Cached references from code
        final String[] names = code.names;
        final Object[] consts = code.consts;
        final short[] wordcode = code.wordcode;
        final int END = wordcode.length;

        final PyDict globals = func.globals;
        assert globals != null;

        // Wrap locals (any type) as a minimal kind of Java map
        Map<Object, Object> locals = localsMapOrNull();

        // Holds keyword names argument between KW_NAMES and CALL
        PyTuple kwnames = null;

        loop: while (ip <= END) {
            /*
             * Here every so often, or maybe inside the try, and conditional on
             * the opcode, CPython would have us check for asynchronous events
             * that need handling. Some are not relevant to this implementation
             * (GIL drop request). Some probably are.
             */

            // Comparison with CPython macros in c.eval:
            // TOP() : s[sp-1]
            // PEEK(n) : s[sp-n]
            // POP() : s[--sp]
            // PUSH(v) : s[sp++] = v
            // SET_TOP(v) : s[sp-1] = v
            // GETLOCAL(oparg) : fastlocals[oparg];
            // PyCell_GET(cell) : cell.get()
            // PyCell_SET(cell, v) : cell.set(v)

            try {
                // Interpret opcode
                switch (opword >> 8) {
                    // Cases ordered as CPython to aid comparison

                    case Opcode311.NOP:
                    case Opcode311.RESUME:
                        break;

                    case Opcode311.LOAD_CONST:
                        s[sp++] = consts[oparg];
                        break;

                    case Opcode311.PUSH_NULL:
                        s[sp++] = null;
                        break;

                    case Opcode311.UNARY_NEGATIVE: {
                        int top = sp - 1;
                        s[top] = PyNumber.negative(s[top]);
                        break;
                    }

                    case Opcode311.UNARY_INVERT: {
                        int top = sp - 1;
                        s[top] = PyNumber.invert(s[top]);
                        break;
                    }

                    case Opcode311.BINARY_SUBSCR: {
                        // w | v | -> | w[v] |
                        // -------^sp --------^sp
                        Object v = s[--sp];
                        int top = sp - 1;
                        s[top] = PySequence.getItem(s[top], v);
                        ip += Opcode311.INLINE_CACHE_ENTRIES_BINARY_SUBSCR;
                        break;
                    }

                    case Opcode311.STORE_SUBSCR: // w[v] = u
                        // u | w | v | -> |
                        // -----------^sp -^sp
                        sp -= 3;
                        // setItem(w, v, u)
                        PySequence.setItem(s[sp + 1], s[sp + 2], s[sp]);
                        ip += Opcode311.INLINE_CACHE_ENTRIES_STORE_SUBSCR;
                        break;

                    case Opcode311.DELETE_SUBSCR: // del w[v]
                        // w | v | -> |
                        // -------^sp -^sp
                        sp -= 2;
                        // delItem(w, v)
                        PySequence.delItem(s[sp], s[sp + 1]);
                        break;

                    case Opcode311.RETURN_VALUE:
                        returnValue = s[--sp]; // POP
                        break loop;

                    case Opcode311.STORE_NAME: {
                        String name = names[oparg];
                        try {
                            locals.put(name, s[--sp]);
                        } catch (NullPointerException npe) {
                            throw noLocals("storing", name);
                        }
                        break;
                    }

                    case Opcode311.DELETE_NAME: {
                        String name = names[oparg];
                        try {
                            locals.remove(name);
                        } catch (NullPointerException npe) {
                            throw noLocals("deleting", name);
                        }
                        break;
                    }

                    case Opcode311.LOAD_NAME: {
                        // Resolve against locals, globals and builtins
                        String name = names[oparg];
                        Object v;
                        try {
                            v = locals.get(name);
                        } catch (NullPointerException npe) {
                            throw noLocals("loading", name);
                        }

                        if (v == null) {
                            v = globals.loadGlobal(builtins, name);
                            if (v == null)
                                throw new NameError(NAME_ERROR_MSG, name);
                        }
                        s[sp++] = v; // PUSH
                        break;
                    }

                    case Opcode311.BUILD_TUPLE:
                        // w[0] | ... | w[oparg-1] | -> | tpl |
                        // -------------------------^sp -------^sp
                        // Group the N=oparg elements on the stack
                        // into a single tuple.
                        sp -= oparg;
                        s[sp] = new PyTuple(s, sp++, oparg);
                        break;

                    case Opcode311.BUILD_LIST:
                        // w[0] | ... | w[oparg-1] | -> | lst |
                        // -------------------------^sp -------^sp
                        /*
                         * Group the N=oparg elements on the stack into a single list.
                         */
                        sp -= oparg;
                        s[sp] = new PyList(s, sp++, oparg);
                        break;

                    case Opcode311.LIST_EXTEND: {
                        Object iterable = s[--sp];
                        PyList list = (PyList)s[sp - oparg];
                        list.list_extend(iterable,
                                () -> Abstract.typeError(VALUE_AFTER_STAR, iterable));
                        break;
                    }

                    case Opcode311.BUILD_MAP:
                        // k1 | v1 | ... | kN | vN | -> | map |
                        // -------------------------^sp -------^sp
                        /*
                         * Build dictionary from the N=oparg key-value pairs on the stack in
                         * order.
                         */
                        sp -= oparg * 2;
                        s[sp] = PyDict.fromKeyValuePairs(s, sp++, oparg);
                        break;

                    case Opcode311.DICT_MERGE: {
                        // f | map | ... | v | -> | f | map | ... |
                        // -------------------^sp -----------------^sp
                        /*
                         * Update a dictionary from another map v on the stack. There are
                         * N=oparg arguments including v on the stack, but only v is merged.
                         * In practice N=1. The function f is only used as context in error
                         * messages.
                         */
                        Object map = s[--sp];
                        PyDict dict = (PyDict)s[sp - oparg];
                        try {
                            dict.merge(map, MergeMode.UNIQUE);
                        } catch (AttributeError ae) {
                            throw kwargsTypeError(s[sp - (oparg + 2)], map);
                        } catch (KeyError.Duplicate ke) {
                            throw kwargsKeyError(ke, s[sp - (oparg + 2)]);
                        }
                        break;
                    }

                    case Opcode311.LOAD_ATTR: {
                        // v | -> | v.name |
                        // ---^sp ----------^sp
                        int top = sp - 1;
                        s[top] = Abstract.getAttr(s[top], names[oparg]);
                        ip += Opcode311.INLINE_CACHE_ENTRIES_LOAD_ATTR;
                        break;
                    }

                    case Opcode311.COMPARE_OP: {
                        // v | w | -> | op(v,w) |
                        // -------^sp -----------^sp
                        Object w = s[--sp]; // POP
                        int top = sp - 1;
                        Object v = s[top]; // TOP
                        s[top] = Comparison.from(oparg).apply(v, w);
                        ip += Opcode311.INLINE_CACHE_ENTRIES_COMPARE_OP;
                        break;
                    }

                    case Opcode311.IS_OP: {
                        // v | w | -> | (v is w) ^ oparg |
                        // -------^sp --------------------^sp
                        Object w = s[--sp]; // POP
                        int top = sp - 1;
                        Object v = s[top]; // TOP
                        Comparison op = oparg == 0 ? Comparison.IS : Comparison.IS_NOT;
                        s[top] = op.apply(v, w);
                        break;
                    }

                    case Opcode311.CONTAINS_OP: {
                        // v | w | -> | (v in w) ^ oparg |
                        // -------^sp --------------------^sp
                        Object w = s[--sp]; // POP
                        int top = sp - 1;
                        Object v = s[top]; // TOP
                        Comparison op = oparg == 0 ? Comparison.IN : Comparison.NOT_IN;
                        s[top] = op.apply(v, w);
                        break;
                    }

                    case Opcode311.JUMP_FORWARD:
                        ip += oparg;
                        break;

                    case Opcode311.JUMP_BACKWARD: {
                        ip -= oparg;
                        break;
                    }

                    case Opcode311.POP_JUMP_BACKWARD_IF_FALSE: {
                        if (!Abstract.isTrue(s[--sp])) { ip -= oparg; }
                        break;
                    }
                    case Opcode311.POP_JUMP_FORWARD_IF_FALSE: {
                        if (!Abstract.isTrue(s[--sp])) { ip += oparg; }
                        break;
                    }

                    case Opcode311.POP_JUMP_BACKWARD_IF_TRUE: {
                        if (Abstract.isTrue(s[--sp])) { ip -= oparg; }
                        break;

                    }

                    case Opcode311.POP_JUMP_FORWARD_IF_TRUE: {
                        if (Abstract.isTrue(s[--sp])) { ip += oparg; }
                        break;

                    }

                    case Opcode311.POP_JUMP_BACKWARD_IF_NOT_NONE: {
                        if (s[--sp] != Py.None) { ip -= oparg; }
                        break;
                    }

                    case Opcode311.POP_JUMP_FORWARD_IF_NOT_NONE: {
                        if (s[--sp] != Py.None) { ip += oparg; }
                        break;
                    }

                    case Opcode311.POP_JUMP_BACKWARD_IF_NONE: {
                        if (s[--sp] == Py.None) { ip -= oparg; }
                        break;
                    }

                    case Opcode311.POP_JUMP_FORWARD_IF_NONE: {
                        if (s[--sp] == Py.None) { ip += oparg; }
                        break;
                    }

                    case Opcode311.JUMP_IF_FALSE_OR_POP: {
                        Object v = s[--sp]; // POP
                        if (!Abstract.isTrue(v)) {
                            sp += 1;    // UNPOP
                            ip += oparg;
                        }
                        break;
                    }

                    case Opcode311.JUMP_IF_TRUE_OR_POP: {
                        Object v = s[--sp]; // POP
                        if (Abstract.isTrue(v)) {
                            sp += 1;    // UNPOP
                            ip += oparg;
                        }
                        break;
                    }

                    case Opcode311.JUMP_BACKWARD_NO_INTERRUPT: {
                        // Same as plain JUMP_BACKWARD for us
                        ip -= oparg;
                        break;
                    }

                    case Opcode311.JUMP_BACKWARD_QUICK: {
                        // Same as plain JUMP_BACKWARD for us
                        ip -= oparg;
                        break;
                    }

                    case Opcode311.LOAD_METHOD:
                        /*
                         * Emitted when compiling obj.meth(...). Works in tandem with CALL.
                         * If we can bypass temporary bound method:
                         */
                        // obj | -> | desc | self |
                        // -----^sp ---------------^sp
                        // Otherwise almost conventional LOAD_ATTR:
                        // obj | -> | null | meth |
                        // -----^sp ---------------^sp
                        getMethod(s[--sp], names[oparg], sp);
                        sp += 2;
                        ip += Opcode311.INLINE_CACHE_ENTRIES_LOAD_METHOD;
                        break;

                    case Opcode311.PRECALL:
                        /*
                         * CPython gains from recognising that a callable is actually a
                         * bound method, and so each call is includes a PUSH_NULL
                         * beforehand. PRECALL uses that space to un-bundle (if it can) the
                         * callable into an unbound callable and its 'self' argument.
                         *
                         * There is no proof this would help in Jython. It might, but we can
                         * safely make this a no-op and CALL will still do the right thing.
                         */
                        ip += Opcode311.INLINE_CACHE_ENTRIES_PRECALL;
                        break;

                    case Opcode311.KW_NAMES:
                        assert (kwnames == null);
                        assert PyTuple.TYPE.checkExact(consts[oparg]);
                        kwnames = (PyTuple)consts[oparg];
                        break;

                    case Opcode311.CALL: {
                        /*
                         * Works in tandem with LOAD_METHOD or PRECALL. If LOAD_METHOD
                         * bypassed the method binding or PRECALL un-bundled a bound object:
                         */
                        // desc | self | arg[n] | -> | res |
                        // ----------------------^sp -------^sp
                        // Otherwise:
                        // null | meth | arg[n] | -> | res |
                        // ----------------------^sp -------^sp
                        // oparg = n
                        sp -= oparg + 2;
                        if (s[sp] != null) {
                            // We bypassed the method binding. Stack:
                            // desc | self | arg[n] |
                            // ^sp
                            // call desc(self, arg1 ... argN)
                            s[sp] = Callables.vectorcall(s[sp++], s, sp, oparg + 1, kwnames);
                        } else {
                            // meth is the bound method self.name
                            // null | meth | arg[n] |
                            // ^sp
                            // call meth(arg1 ... argN)
                            s[sp++] = Callables.vectorcall(s[sp], s, sp + 1, oparg, kwnames);
                        }
                        kwnames = null;
                        ip += Opcode311.INLINE_CACHE_ENTRIES_CALL;
                        break;
                    }

                    case Opcode311.CALL_FUNCTION_EX: {
                        // Call with positional & kw args. Stack:
                        // f | args | kwdict? | -> res |
                        // --------------------^sp -----^sp
                        // oparg is 0 (no kwdict) or 1 (kwdict present)
                        Object w = (oparg & 0x1) == 0 ? null : s[--sp];
                        Object v = s[--sp]; // args tuple
                        sp -= 1;
                        assert s[sp - 1] == null; // from PUSH_NULL
                        s[sp - 1] = Callables.callEx(s[sp], v, w);
                        break;
                    }

                    case Opcode311.BINARY_OP: {
                        Object w = s[--sp]; // POP
                        int top = sp - 1;
                        Object v = s[top]; // TOP
                        s[top] = switch (oparg) {
                            default -> Py.NotImplemented;
                            case Opcode311.NB_ADD -> PyNumber.add(v, w);
                            case Opcode311.NB_AND -> PyNumber.and(v, w);
                            // case Opcode311.NB_FLOOR_DIVIDE -> PyNumber.FloorDivide(v, w);
                            // case Opcode311.NB_LSHIFT -> PyNumber.Lshift(v, w);
                            // case Opcode311.NB_MATRIX_MULTIPLY
                            // -> PyNumber.MatrixMultiply(v, w);
                            case Opcode311.NB_MULTIPLY -> PyNumber.multiply(v, w);
                            // case Opcode311.NB_REMAINDER -> PyNumber.Remainder(v, w);
                            case Opcode311.NB_OR -> PyNumber.or(v, w);
                            // case Opcode311.NB_POWER -> PyNumber.PowerNoMod(v, w);
                            // case Opcode311.NB_RSHIFT -> PyNumber.Rshift(v, w);
                            case Opcode311.NB_SUBTRACT -> PyNumber.subtract(v, w);
                            // case Opcode311.NB_TRUE_DIVIDE -> PyNumber.TrueDivide(v, w);
                            case Opcode311.NB_XOR -> PyNumber.xor(v, w);
                            // case Opcode311.NB_INPLACE_ADD -> PyNumber.InPlaceAdd(v, w);
                            // case Opcode311.NB_INPLACE_AND -> PyNumber.InPlaceAnd(v, w);
                            // case Opcode311.NB_INPLACE_FLOOR_DIVIDE
                            // -> PyNumber.InPlaceFloorDivide(v, w);
                            // case Opcode311.NB_INPLACE_LSHIFT -> PyNumber.InPlaceLshift(v, w);
                            // case Opcode311.NB_INPLACE_MATRIX_MULTIPLY
                            // -> PyNumber.InPlaceMatrixMultiply(v, w);
                            // case Opcode311.NB_INPLACE_MULTIPLY
                            // -> PyNumber.InPlaceMultiply(v, w);
                            // case Opcode311.NB_INPLACE_REMAINDER
                            // -> PyNumber.InPlaceRemainder(v, w);
                            // case Opcode311.NB_INPLACE_OR -> PyNumber.InPlaceOr(v, w);
                            // case Opcode311.NB_INPLACE_POWER
                            // -> PyNumber.InPlacePowerNoMod(v, w);
                            // case Opcode311.NB_INPLACE_RSHIFT -> PyNumber.InPlaceRshift(v, w);
                            // case Opcode311.NB_INPLACE_SUBTRACT
                            // -> PyNumber.InPlaceSubtract(v, w);
                            // case Opcode311.NB_INPLACE_TRUE_DIVIDE -> //
                            // PyNumber.InPlaceTrueDivide(v, w);
                            // case Opcode311.NB_INPLACE_XOR -> PyNumber.InPlaceXor(v, w);
                        };
                        ip += Opcode311.INLINE_CACHE_ENTRIES_BINARY_OP;
                        break;
                    }

                    case Opcode311.EXTENDED_ARG:
                        // Pick up the next instruction.
                        opword = wordcode[ip++] & 0xffff;
                        // The current oparg *prefixes* the next oparg,
                        // which could of course be another
                        // EXTENDED_ARG. (Trust me, it'll be fine.)
                        oparg = (oparg << 8) | opword & 0xff;
                        // This is *instead of* the post-switch fetch.
                        continue;

                    default:
                        throw new InterpreterError("%s at ip: %d, unknown opcode: %d",
                                code.qualname, 2 * (ip - 1), opword >> 8);
                } // switch

                /*
                 * Pick up the next instruction and argument. Because we use a word
                 * array, our ip is half the CPython ip. The latter, and all jump
                 * arguments, are always even, so we have to halve the jump
                 * distances or destinations.
                 */
                opword = wordcode[ip++] & 0xffff;
                oparg = opword & 0xff;

            } catch (PyException pye) {
                /*
                 * We ought here to check for exception handlers (defined in Python
                 * and reflected in the byte code) potentially resuming the loop
                 * with ip at the handler code, or in a Python finally clause.
                 */
                // Should handle within Python, but for now, stop.
                System.err.println(pye);
                throw pye;
            } catch (InterpreterError | AssertionError ie) {
                /*
                 * An InterpreterError signals an internal error, recognised by our
                 * implementation: stop.
                 */
                System.err.println(ie);
                throw ie;
            } catch (Throwable t) {
                /*
                 * A non-Python exception signals an internal error, in our
                 * implementation, in user-supplied Java, or from a Java library
                 * misused from Python.
                 */
                // Should handle within Python, but for now, stop.
                t.printStackTrace();
                throw new InterpreterError(t, "Non-PyException at ip: %d, opcode: %d", 2 * (ip - 1),
                        opword >> 8);
            }
        } // loop

        // ThreadState.get().swap(back);
        return returnValue;
    }

    // Supporting definitions and methods -----------------------------

    private static final Object[] EMPTY_OBJECT_ARRAY = Py.EMPTY_ARRAY;
    private static final String NAME_ERROR_MSG = "name '%.200s' is not defined";
    private static final String VALUE_AFTER_STAR = "Value after * must be an iterable, not %.200s";

    /**
     * A specialised version of {@code object.__getattribute__}
     * specifically to support the {@code LOAD_METHOD} and
     * {@code CALL_METHOD} opcode pair generated by the CPython byte
     * code compiler. This method will place two entries in the stack at
     * the offset given that are either:
     * <ol>
     * <li>an unbound method and the object passed ({@code obj}),
     * or</li>
     * <li>{@code null} and a bound method object.</li>
     * </ol>
     * <p>
     * The normal behaviour of {@code object.__getattribute__} is
     * represented by case 2.
     * <p>
     * Case 1 supports an optimisation that is possible when the type of
     * the self object {@code obj} has not overridden
     * {@code __getattribute__}, and the {@code name} resolves to a
     * regular method in it. {@code CALL_METHOD} will detect and use
     * this optimised form if the first element is not {@code null}.
     *
     * @param obj of which the callable is an attribute
     * @param name of callable attribute
     * @param offset in stack at which to place results
     * @throws AttributeError if the named attribute does not exist
     * @throws Throwable from other errors
     */
    // Compare CPython _PyObject_GetMethod in object.c
    private void getMethod(Object obj, String name, int offset) throws AttributeError, Throwable {

        PyType objType = PyType.of(obj);

        // If type(obj) defines its own __getattribute__ use that.
        if (!objType.hasGenericGetAttr()) {
            valuestack[offset] = null;
            valuestack[offset + 1] = Abstract.getAttr(obj, name);
            return;
        }

        /*
         * From here, the code is a version of the default attribute access
         * mechanism PyBaseObject.__getattribute__ in which, if the look-up
         * leads to a method descriptor, we avoid binding the descriptor
         * into a short-lived bound method object.
         */

        MethodHandle descrGet = null;
        boolean methFound = false;

        // Look up the name in the type (null if not found).
        Object typeAttr = objType.lookup(name);
        if (typeAttr != null) {
            // Found in the type, it might be a descriptor
            Operations typeAttrOps = Operations.of(typeAttr);
            descrGet = typeAttrOps.op_get;
            if (typeAttrOps.isMethodDescr()) {
                /*
                 * We found a method descriptor, but will check the instance
                 * dictionary for a shadowing definition.
                 */
                methFound = true;
            } else if (typeAttrOps.isDataDescr()) {
                // typeAttr is a data descriptor so call its __get__.
                try {
                    valuestack[offset] = null;
                    valuestack[offset + 1] = descrGet.invokeExact(typeAttr, obj, objType);
                    return;
                } catch (Slot.EmptyException e) {
                    /*
                     * Only __set__ or __delete__ was defined. We do not catch
                     * AttributeError: it's definitive. Suppress trying __get__ again.
                     */
                    descrGet = null;
                }
            }
        }

        /*
         * At this stage: typeAttr is the value from the type, or a non-data
         * descriptor, or null if the attribute was not found. It's time to
         * give the object instance dictionary a chance.
         */
        if (obj instanceof DictPyObject) {
            Map<Object, Object> d = ((DictPyObject)obj).getDict();
            Object instanceAttr = d.get(name);
            if (instanceAttr != null) {
                // Found the callable in the instance dictionary.
                valuestack[offset] = null;
                valuestack[offset + 1] = instanceAttr;
                return;
            }
        }

        /*
         * The name wasn't in the instance dictionary (or there wasn't an
         * instance dictionary). typeAttr is the result of look-up on the
         * type: a value , a non-data descriptor, or null if the attribute
         * was not found.
         */
        if (methFound) {
            /*
             * typeAttr is a method descriptor and was not shadowed by an entry
             * in the instance dictionary.
             */
            valuestack[offset] = typeAttr;
            valuestack[offset + 1] = obj;
            return;
        } else if (descrGet != null) {
            // typeAttr may be a non-data descriptor: call __get__.
            try {
                valuestack[offset] = null;
                valuestack[offset + 1] = descrGet.invokeExact(typeAttr, obj, objType);
                return;
            } catch (Slot.EmptyException e) {}
        }

        if (typeAttr != null) {
            /*
             * The attribute obtained from the type, and that turned out not to
             * be a descriptor, is the callable.
             */
            valuestack[offset] = null;
            valuestack[offset + 1] = typeAttr;
            return;
        }

        // All the look-ups and descriptors came to nothing :(
        throw Abstract.noAttributeError(obj, name);
    }

    /**
     * Generate error to throw when we cannot access locals.
     *
     * @param action "loading", "storing" or "deleting"
     * @param name variable name
     * @return
     */
    private static SystemError noLocals(String action, String name) {
        return new SystemError("no locals found when %s '%s'", name);
    }

    /**
     * Create a {@link TypeError} to throw when keyword arguments appear
     * not to be a mapping. {@code dict.merge} raises
     * {@link AttributeError} (percolated from an attempt to get 'keys'
     * attribute) if its second argument is not a mapping, which we
     * convert to a {@link TypeError}.
     *
     * @param func providing a function name for context
     * @param kwargs the alleged mapping
     * @return an exception to throw
     */
    // Compare CPython format_kwargs_error in ceval.c
    private static TypeError kwargsTypeError(Object func, Object kwargs) {
        String funcstr = PyObjectUtil.functionStr(func);
        return Abstract.argumentTypeError(funcstr, "**", "a mapping", kwargs);
    }

    /**
     * Create a {@link TypeError} to throw when a duplicate key turns up
     * while merging keyword arguments to a function call.
     *
     * @param ke the duplicate key error
     * @param func providing a function name for context
     * @return an exception to throw
     */
    // Compare CPython format_kwargs_error in ceval.c
    private static TypeError kwargsKeyError(KeyError.Duplicate ke, Object func) {
        /*
         * PyDict.merge raises KeyError.Duplicate (percolated from an
         * attempt to assign an existing key), which we convert to a
         * TypeError.
         */
        String funcstr = PyObjectUtil.functionStr(func);
        return new TypeError("%s got multiple values for keyword argument '%s'", funcstr, ke.key);
    }
}
