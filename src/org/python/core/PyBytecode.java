package org.python.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.python.core.Opcode.*;

public class PyBytecode extends PyBaseCode implements Traverseproc {

    // for debugging
    public static boolean defaultDebug = false;
    private static boolean debug;
    private static PyObject dis, opname;

    private int count = 0; // total number of opcodes run so far in this code obj
    private int maxCount = -1; // if -1, no cap on number of opcodes than can be run

    private static synchronized PyObject get_dis() {
        if (dis == null) {
            dis = __builtin__.__import__("dis");
        }
        return dis;
    }

    private static synchronized PyObject get_opname() {
        if (opname == null) {
            opname = get_dis().__getattr__("opname");
        }
        return opname;
    }

    public static void _allDebug(boolean setting) {
        defaultDebug = setting;
    }

    public PyObject _debug(int maxCount) {
        debug = maxCount > 0;
        this.maxCount = maxCount;
        return Py.None;
    }
    // end debugging

    public final static int CO_MAXBLOCKS = 20; // same as in CPython
    public final byte[] co_code; // widened to char to avoid signed byte issues
    public final PyObject[] co_consts;
    public final String[] co_names;
    public final int co_stacksize;
    public final byte[] co_lnotab;
    private final static int CALL_FLAG_VAR = 1;
    private final static int CALL_FLAG_KW = 2;

    // follows new.code's interface
    public PyBytecode(int argcount, int nlocals, int stacksize, int flags,
            String codestring, PyObject[] constants, String[] names, String varnames[],
            String filename, String name, int firstlineno, String lnotab) {
        this(argcount, nlocals, stacksize, flags, codestring,
                constants, names, varnames, filename, name, firstlineno, lnotab,
                null, null);
    }

    // XXX - intern names HERE instead of in marshal
    public PyBytecode(int argcount, int nlocals, int stacksize, int flags,
            String codestring, PyObject[] constants, String[] names, String varnames[],
            String filename, String name, int firstlineno, String lnotab,
            String[] cellvars, String[] freevars) {

        debug = defaultDebug;

        if (argcount < 0) {
            throw Py.ValueError("code: argcount must not be negative");
        } else if (nlocals < 0) {
            throw Py.ValueError("code: nlocals must not be negative");
        }

        co_argcount = nargs = argcount;
        co_varnames = varnames;
        co_nlocals = nlocals; // maybe assert = varnames.length;
        co_filename = filename;
        co_firstlineno = firstlineno;
        co_cellvars = cellvars;
        co_freevars = freevars;
        co_name = name;
        co_flags = new CompilerFlags(flags);
        varargs = co_flags.isFlagSet(CodeFlag.CO_VARARGS);
        varkwargs = co_flags.isFlagSet(CodeFlag.CO_VARKEYWORDS);

        co_stacksize = stacksize;
        co_consts = constants;
        co_names = names;
        co_code = getBytes(codestring);
        co_lnotab = getBytes(lnotab);
    }

    private static final String[] __members__ = {
        "co_name", "co_argcount",
        "co_varnames", "co_filename", "co_firstlineno",
        "co_flags", "co_cellvars", "co_freevars", "co_nlocals",
        "co_code", "co_consts", "co_names", "co_lnotab", "co_stacksize"
    };

    @Override
    public PyObject __dir__() {
        PyString members[] = new PyString[__members__.length];
        for (int i = 0; i < __members__.length; i++) {
            members[i] = new PyString(__members__[i]);
        }
        return new PyList(members);
    }

    private void throwReadonly(String name) {
        for (int i = 0; i < __members__.length; i++) {
            if (__members__[i] == name) {
                throw Py.TypeError("readonly attribute");
            }
        }
        throw Py.AttributeError(name);
    }

    @Override
    public void __setattr__(String name, PyObject value) {
        // no writable attributes
        throwReadonly(name);
    }

    @Override
    public void __delattr__(String name) {
        throwReadonly(name);
    }

    private static PyTuple toPyStringTuple(String[] ar) {
        if (ar == null) {
            return Py.EmptyTuple;
        }
        int sz = ar.length;
        PyString[] pystr = new PyString[sz];
        for (int i = 0; i < sz; i++) {
            pystr[i] = new PyString(ar[i]);
        }
        return new PyTuple(pystr);
    }

    @Override
    public PyObject __findattr_ex__(String name) {
        // have to craft co_varnames specially
        if (name == "co_varnames") {
            return toPyStringTuple(co_varnames);
        }
        if (name == "co_cellvars") {
            return toPyStringTuple(co_cellvars);
        }
        if (name == "co_freevars") {
            return toPyStringTuple(co_freevars);
        }
        if (name == "co_filename") {
            return Py.fileSystemEncode(co_filename); // bytes object expected by clients
        }
        if (name == "co_name") {
            return new PyString(co_name);
        }
        if (name == "co_code") {
            return new PyString(getString(co_code));
        }
        if (name == "co_lnotab") {
            return new PyString(getString(co_lnotab));
        }
        if (name == "co_consts") {
            return new PyTuple(co_consts);
        }
        if (name == "co_flags") {
            return Py.newInteger(co_flags.toBits());
        }
        return super.__findattr_ex__(name);
    }

    enum Why {
        NOT,       /* No error */
        EXCEPTION, /* Exception occurred */
        RERAISE,   /* Exception re-raised by 'finally' */
        RETURN,    /* 'return' statement */
        BREAK,     /* 'break' statement */
        CONTINUE,  /* 'continue' statement */
        YIELD      /* 'yield' operator */

    };

    // to enable why's to be stored on a PyStack
    @Untraversable
    private static class PyStackWhy extends PyObject {
        Why why;

        PyStackWhy(Why why) {
            this.why = why;
        }

        @Override
        public String toString() {
            return why.toString();
        }
    }

    private static class PyStackException extends PyObject implements Traverseproc {
        PyException exception;

        PyStackException(PyException exception) {
            this.exception = exception;
        }

        @Override
        public String toString() {
            return String.format("PyStackException<%s,%s,%.100s>", exception.type, exception.value, exception.traceback);
        }


        /* Traverseproc implementation */
        @Override
        public int traverse(Visitproc visit, Object arg) {
            return exception != null ? exception.traverse(visit, arg) : 0;
        }

        @Override
        public boolean refersDirectlyTo(PyObject ob) {
            return ob != null && exception.refersDirectlyTo(ob);
        }
    }

    private static String stringify_blocks(PyFrame f) {
        if (f.f_exits == null || f.f_lineno == 0) {
            return "[]";
        }
        StringBuilder buf = new StringBuilder("[");
        for (int i = 0; i < f.f_exits.length; i++) {
            buf.append(f.f_exits[i].toString());
            if (i < f.f_exits.length - 1) {
                buf.append(", ");
            }
        }
        buf.append("]");
        return buf.toString();
    }

    private void print_debug(int count, int next_instr, int line, int opcode, int oparg, PyStack stack, PyFrame f) {
        if (debug) {
            System.err.println(co_name + " " + line + ":" +
                    count + "," + f.f_lasti + "> " + opcode+" "+
                    get_opname().__getitem__(Py.newInteger(opcode)) +
                    (opcode >= HAVE_ARGUMENT ? " " + oparg : "") +
                    ", stack: " + stack.toString() +
                    ", blocks: " + stringify_blocks(f));
        }
    }

    // the following code exploits the fact that f_exits is only used by code compiled to Java bytecode;
    // in their place we implement the block stack for PBC-VM, as mapped below in the comments of pushBlock

    private static PyTryBlock popBlock(PyFrame f) {
        return (PyTryBlock)(((PyList)f.f_exits[0]).pop());
    }

    private static void pushBlock(PyFrame f, PyTryBlock block) {
        if (f.f_exits == null) { // allocate in the frame where they can fit! TODO consider supporting directly in the frame
            f.f_exits = new PyObject[1]; // f_blockstack in CPython - a simple ArrayList might be best
            f.f_exits[0] = new PyList();
        }
        ((PyList)f.f_exits[0]).append(block);
    }

    private boolean blocksLeft(PyFrame f) {
        if (f.f_exits != null) {
            return ((PyList)f.f_exits[0]).__nonzero__();
        } else {
            return false;
        }
    }

    @Override
    protected PyObject interpret(PyFrame f, ThreadState ts) {
        final PyStack stack = new PyStack(co_stacksize);
        int next_instr = -1;
        int opcode;    /* Current opcode */
        int oparg = 0; /* Current opcode argument, if any */
        Why why = Why.NOT;
        PyObject retval = null;
        LineCache lineCache = null;
        int last_line = -1;
        int line = 0;

        // XXX - optimization opportunities
        // 1. consider detaching the setting/getting of frame fields to improve performance, instead do this
        // in a shadow version of the frame that we copy back to on entry/exit and downcalls
        if (debug) {
            System.err.println(co_name + ":" + f.f_lasti + "/" + co_code.length +
                    ", cells:" + Arrays.toString(co_cellvars) + ", free:" + Arrays.toString(co_freevars));
            int i = 0;
            for (String cellvar : co_cellvars) {
                System.err.println(cellvar + " = " + f.f_env[i++]);
            }
            for (String freevar : co_freevars) {
                System.err.println(freevar + " = " + f.f_env[i++]);
            }
            get_dis().invoke("disassemble", this);

        }
        if (f.f_lasti >= co_code.length) {
            throw Py.SystemError(""); // XXX - chose an appropriate error!!!
        }

        next_instr = f.f_lasti;

        // the restore stack aspects should occur ONLY after a yield
        boolean checkGeneratorInput = false;
        if (f.f_savedlocals != null) {
            for (int i = 0; i < f.f_savedlocals.length; i++) {
                PyObject v = (PyObject) (f.f_savedlocals[i]);
                stack.push(v);
            }
            checkGeneratorInput = true;
            f.f_savedlocals = null;
        }

        while (!debug || (maxCount == -1 || count < maxCount)) { // XXX - replace with while(true)

            if (f.tracefunc != null || debug) {
                if (lineCache == null) {
                    lineCache = new LineCache();
                    if (debug) {
                        System.err.println("LineCache: " + lineCache.toString());
                    }
                }
                line = lineCache.getline(next_instr); // XXX - should also return the range this is valid to avoid an unnecessary bisect
                if (line != last_line) {
                    f.setline(line);
                }
            }

            try {

                if (checkGeneratorInput) {
                    checkGeneratorInput = false;
                    Object generatorInput = f.getGeneratorInput();
                    if (generatorInput instanceof PyException) {
                        throw (PyException) generatorInput;
                    }
                    stack.push((PyObject) generatorInput);
                }

                opcode = getUnsigned(co_code, next_instr);
                if (opcode >= HAVE_ARGUMENT) {
                    next_instr += 2;
                    oparg = (getUnsigned(co_code, next_instr) << 8) + getUnsigned(co_code, next_instr - 1);
                }
                print_debug(count, next_instr, line, opcode, oparg, stack, f);

                count += 1;
                next_instr += 1;
                f.f_lasti = next_instr;

                switch (opcode) {
                    case NOP:
                        break;

                    case LOAD_FAST:
                        stack.push(f.getlocal(oparg));
                        break;

                    case LOAD_CONST:
                        stack.push(co_consts[oparg]);
                        break;

                    case STORE_FAST:
                        f.setlocal(oparg, stack.pop());
                        break;

                    case POP_TOP:
                        stack.pop();
                        break;

                    case ROT_TWO:
                        stack.rot2();
                        break;

                    case ROT_THREE:
                        stack.rot3();
                        break;

                    case ROT_FOUR:
                        stack.rot4();
                        break;

                    case DUP_TOP:
                        stack.dup();
                        break;

                    case DUP_TOPX: {
                        if (oparg == 2 || oparg == 3) {
                            stack.dup(oparg);
                        } else {
                            throw Py.RuntimeError("invalid argument to DUP_TOPX" +
                                    " (bytecode corruption?)");
                        }
                        break;
                    }

                    case UNARY_POSITIVE:
                        stack.push(stack.pop().__pos__());
                        break;

                    case UNARY_NEGATIVE:
                        stack.push(stack.pop().__neg__());
                        break;

                    case UNARY_NOT:
                        stack.push(stack.pop().__not__());
                        break;

                    case UNARY_CONVERT:
                        stack.push(stack.pop().__repr__());
                        break;

                    case UNARY_INVERT:
                        stack.push(stack.pop().__invert__());
                        break;

                    case BINARY_POWER: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._pow(b));
                        break;
                    }

                    case BINARY_MULTIPLY: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._mul(b));
                        break;
                    }

                    case BINARY_DIVIDE: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();

                        if (!co_flags.isFlagSet(CodeFlag.CO_FUTURE_DIVISION)) {
                            stack.push(a._div(b));
                        } else {
                            stack.push(a._truediv(b));
                        }
                        break;
                    }

                    case BINARY_TRUE_DIVIDE: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._truediv(b));
                        break;
                    }

                    case BINARY_FLOOR_DIVIDE: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._floordiv(b));
                        break;
                    }

                    case BINARY_MODULO: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._mod(b));
                        break;
                    }

                    case BINARY_ADD: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._add(b));
                        break;
                    }

                    case BINARY_SUBTRACT: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._sub(b));
                        break;
                    }

                    case BINARY_SUBSCR: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a.__getitem__(b));
                        break;
                    }

                    case BINARY_LSHIFT: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._lshift(b));
                        break;
                    }

                    case BINARY_RSHIFT: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._rshift(b));
                        break;
                    }

                    case BINARY_AND: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._and(b));
                        break;
                    }


                    case BINARY_XOR: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._xor(b));
                        break;
                    }

                    case BINARY_OR: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._or(b));
                        break;
                    }

                    case LIST_APPEND: {
                        PyObject b = stack.pop();
                        PyList a = (PyList) stack.top(oparg);
                        a.append(b);
                        break;
                    }

                    case SET_ADD: {
                        PyObject b = stack.pop();
                        PySet a = (PySet) stack.top(oparg);
                        a.add(b);
                        break;
                    }

                    case INPLACE_POWER: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._ipow(b));
                        break;
                    }

                    case INPLACE_MULTIPLY: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._imul(b));
                        break;
                    }

                    case INPLACE_DIVIDE: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        if (!co_flags.isFlagSet(CodeFlag.CO_FUTURE_DIVISION)) {
                            stack.push(a._idiv(b));
                        } else {
                            stack.push(a._itruediv(b));
                        }
                        break;
                    }

                    case INPLACE_TRUE_DIVIDE: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._itruediv(b));
                        break;
                    }

                    case INPLACE_FLOOR_DIVIDE: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._ifloordiv(b));
                        break;
                    }

                    case INPLACE_MODULO: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._imod(b));
                        break;
                    }

                    case INPLACE_ADD: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._iadd(b));
                        break;
                    }

                    case INPLACE_SUBTRACT: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._isub(b));
                        break;
                    }

                    case INPLACE_LSHIFT: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._ilshift(b));
                        break;
                    }

                    case INPLACE_RSHIFT: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._irshift(b));
                        break;
                    }

                    case INPLACE_AND: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._iand(b));
                        break;
                    }

                    case INPLACE_XOR: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._ixor(b));
                        break;
                    }

                    case INPLACE_OR: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();
                        stack.push(a._ior(b));
                        break;
                    }

                    case SLICE:
                    case SLICE_1:
                    case SLICE_2:
                    case SLICE_3: {
                        PyObject stop = (((opcode - SLICE) & 2) != 0) ? stack.pop() : null;
                        PyObject start = (((opcode - SLICE) & 1) != 0) ? stack.pop() : null;
                        PyObject obj = stack.pop();
                        stack.push(obj.__getslice__(start, stop));
                        break;
                    }

                    case STORE_SLICE:
                    case STORE_SLICE_1:
                    case STORE_SLICE_2:
                    case STORE_SLICE_3: {
                        PyObject stop = (((opcode - STORE_SLICE) & 2) != 0) ? stack.pop() : null;
                        PyObject start = (((opcode - STORE_SLICE) & 1) != 0) ? stack.pop() : null;
                        PyObject obj = stack.pop();
                        PyObject value = stack.pop();
                        obj.__setslice__(start, stop, value);
                        break;
                    }

                    case DELETE_SLICE:
                    case DELETE_SLICE_1:
                    case DELETE_SLICE_2:
                    case DELETE_SLICE_3: {
                        PyObject stop = (((opcode - DELETE_SLICE) & 2) != 0) ? stack.pop() : null;
                        PyObject start = (((opcode - DELETE_SLICE) & 1) != 0) ? stack.pop() : null;
                        PyObject obj = stack.pop();
                        obj.__delslice__(start, stop);
                        break;
                    }

                    case STORE_SUBSCR: {
                        PyObject key = stack.pop();
                        PyObject obj = stack.pop();
                        PyObject value = stack.pop();
                        obj.__setitem__(key, value);
                        break;
                    }

                    case DELETE_SUBSCR: {
                        PyObject key = stack.pop();
                        PyObject obj = stack.pop();
                        obj.__delitem__(key);
                        break;
                    }

                    case PRINT_EXPR:
                        PySystemState.displayhook(stack.pop());
                        break;

                    case PRINT_ITEM_TO:
                        Py.printComma(stack.pop(), stack.pop());
                        break;

                    case PRINT_ITEM:
                        Py.printComma(stack.pop());
                        break;

                    case PRINT_NEWLINE_TO:
                        Py.printlnv(stack.pop());
                        break;

                    case PRINT_NEWLINE:
                        Py.println();
                        break;

                    case RAISE_VARARGS:

                        switch (oparg) {
                            case 3: {
                                PyTraceback tb = (PyTraceback) (stack.pop());
                                PyObject value = stack.pop();
                                PyObject type = stack.pop();
                                throw PyException.doRaise(type, value, tb);
                            }
                            case 2: {
                                PyObject value = stack.pop();
                                PyObject type = stack.pop();
                                throw PyException.doRaise(type, value, null);
                            }
                            case 1: {
                                PyObject type = stack.pop();
                                throw PyException.doRaise(type, null, null);
                            }
                            case 0:
                                throw PyException.doRaise(null, null, null);
                            default:
                                throw Py.SystemError("bad RAISE_VARARGS oparg");
                        }

                    case LOAD_LOCALS:
                        stack.push(f.f_locals);
                        break;

                    case RETURN_VALUE:
                        retval = stack.pop();
                        why = Why.RETURN;
                        break;

                    case YIELD_VALUE:
                        retval = stack.pop();
                        // Note: CPython calls f->f_stacktop = stack_pointer; here
                        why = Why.YIELD;
                        break;

                    case EXEC_STMT: {
                        PyObject locals = stack.pop();
                        PyObject globals = stack.pop();
                        PyObject code = stack.pop();
                        //Todo: Better make it possible to use PyFrame f here:
                        Py.exec(code, globals == Py.None ? null : globals, locals == Py.None ? null : locals);
                        break;
                    }

                    case POP_BLOCK: {
                        PyTryBlock b = popBlock(f);
                        while (stack.size() > b.b_level) {
                            stack.pop();
                        }
                        break;
                    }

                    case END_FINALLY: {
                        // Todo: Review this regarding Python 2.7-update
                        PyObject v = stack.pop();
                        if (v instanceof PyStackWhy) {
                            why = ((PyStackWhy) v).why;
                            assert (why != Why.YIELD);
                            if (why == Why.RETURN || why == Why.CONTINUE) {
                                retval = stack.pop();
                            }
                        } else if (v instanceof PyStackException) {
                            stack.top -= 2; // to pop value, traceback
                            ts.exception = ((PyStackException) v).exception;
                            why = Why.RERAISE;
                        } else if (v instanceof PyString) {
                            // This shouldn't happen, because Jython always pushes
                            // exception type as PyException-object.
                            // Todo: Test, if it can be removed.
                            PyObject value = stack.pop();
                            PyObject traceback = stack.pop();
                            ts.exception = new PyException(v, value, (PyTraceback) traceback);
                            why = Why.RERAISE;
                        } else if (v != Py.None) {
                            throw Py.SystemError("'finally' pops bad exception");
                        }
                        break;
                    }

                    case BUILD_CLASS: {
                        PyObject methods = stack.pop();
                        PyObject bases[] = ((PySequenceList) (stack.pop())).getArray();
                        String name = stack.pop().toString();
                        stack.push(Py.makeClass(name, bases, methods));
                        break;
                    }

                    case STORE_NAME:
                        f.setlocal(co_names[oparg], stack.pop());
                        break;

                    case DELETE_NAME:
                        f.dellocal(co_names[oparg]);
                        break;

                    case UNPACK_SEQUENCE:
                        unpack_iterable(oparg, stack);
                        break;

                    case STORE_ATTR: {
                        PyObject obj = stack.pop();
                        PyObject v = stack.pop();
                        obj.__setattr__(co_names[oparg], v);
                        break;
                    }

                    case DELETE_ATTR:
                        stack.pop().__delattr__(co_names[oparg]);
                        break;

                    case STORE_GLOBAL:
                        f.setglobal(co_names[oparg], stack.pop());
                        break;

                    case DELETE_GLOBAL:
                        f.delglobal(co_names[oparg]);
                        break;

                    case LOAD_NAME:
                        stack.push(f.getname(co_names[oparg]));
                        break;

                    case LOAD_GLOBAL:
                        stack.push(f.getglobal(co_names[oparg]));
                        break;

                    case DELETE_FAST:
                        f.dellocal(oparg);
                        break;

                    case LOAD_CLOSURE: {
                        // Todo: Review this regarding Python 2.7-update
                        PyCell cell = (PyCell) (f.getclosure(oparg));
                        if (cell.ob_ref == null) {
                            String name;
                            if (oparg >= co_cellvars.length) {
                                name = co_freevars[oparg - co_cellvars.length];
                            } else {
                                name = co_cellvars[oparg];
                            }
                            // XXX - consider some efficient lookup mechanism, like a hash :),
                            // at least if co_varnames is much greater than say a certain
                            // size (but i would think, it's not going to happen in real code. profile?)
                            if (f.f_fastlocals != null) {
                                int i = 0;
                                boolean matched = false;
                                for (String match : co_varnames) {
                                    if (match.equals(name)) {
                                        matched = true;
                                        break;
                                    }
                                    i++;
                                }
                                if (matched) {
                                    cell.ob_ref = f.f_fastlocals[i];
                                }
                            } else {
                                cell.ob_ref = f.f_locals.__finditem__(name);
                            }
                        }
                        stack.push(cell);
                        break;
                    }

                    case LOAD_DEREF: {
                        // Todo: Review this regarding Python 2.7-update
                        // common code from LOAD_CLOSURE
                        PyCell cell = (PyCell) (f.getclosure(oparg));
                        if (cell.ob_ref == null) {
                            String name;
                            if (oparg >= co_cellvars.length) {
                                name = co_freevars[oparg - co_cellvars.length];
                            } else {
                                name = co_cellvars[oparg];
                            }
                            // XXX - consider some efficient lookup mechanism, like a hash :),
                            // at least if co_varnames is much greater than say a certain
                            // size (but i would think, it's not going to happen in real code. profile?)
                            if (f.f_fastlocals != null) {
                                int i = 0;
                                boolean matched = false;
                                for (String match : co_varnames) {
                                    if (match.equals(name)) {
                                        matched = true;
                                        break;
                                    }
                                    i++;
                                }
                                if (matched) {
                                    cell.ob_ref = f.f_fastlocals[i];
                                }
                            } else {
                                cell.ob_ref = f.f_locals.__finditem__(name);
                            }
                        }
                        stack.push(cell.ob_ref);
                        break;
                    }

                    case STORE_DEREF:
                        f.setderef(oparg, stack.pop());
                        break;

                    case BUILD_TUPLE:
                        stack.push(new PyTuple(stack.popN(oparg)));
                        break;

                    case BUILD_LIST:
                        stack.push(new PyList(stack.popN(oparg)));
                        break;

                    case BUILD_SET:
                        stack.push(new PySet(stack.popN(oparg)));
                        break;

                    case BUILD_MAP:
                        // oparg contains initial capacity:
                        stack.push(new PyDictionary(PyDictionary.TYPE, oparg));
                        break;

                    case STORE_MAP: {
                        PyObject key = stack.pop();
                        PyObject val = stack.pop();
                        stack.top().__setitem__(key, val);
                        break;
                    }

                    case MAP_ADD: {
                        PyObject key = stack.pop();
                        PyObject val = stack.pop();
                        stack.top(oparg).__setitem__(key, val);
                        break;
                    }

                    case LOAD_ATTR: {
                        String name = co_names[oparg];
                        stack.push(stack.pop().__getattr__(name));
                        break;
                    }

                    case COMPARE_OP: {
                        PyObject b = stack.pop();
                        PyObject a = stack.pop();

                        switch (oparg) {

                            case PyCmp_LT:
                                stack.push(a._lt(b));
                                break;
                            case PyCmp_LE:
                                stack.push(a._le(b));
                                break;
                            case PyCmp_EQ:
                                stack.push(a._eq(b));
                                break;
                            case PyCmp_NE:
                                stack.push(a._ne(b));
                                break;
                            case PyCmp_GT:
                                stack.push(a._gt(b));
                                break;
                            case PyCmp_GE:
                                stack.push(a._ge(b));
                                break;
                            case PyCmp_IN:
                                stack.push(a._in(b));
                                break;
                            case PyCmp_NOT_IN:
                                stack.push(a._notin(b));
                                break;
                            case PyCmp_IS:
                                stack.push(a._is(b));
                                break;
                            case PyCmp_IS_NOT:
                                stack.push(a._isnot(b));
                                break;
                            case PyCmp_EXC_MATCH:
                                // Todo: Review this regarding Python 2.7-update
                                if (a instanceof PyStackException) {
                                    PyException pye = ((PyStackException) a).exception;
                                    stack.push(Py.newBoolean(pye.match(b)));
                                } else {
                                    stack.push(Py.newBoolean(new PyException(a).match(b)));
                                }
                                break;

                        }
                        break;
                    }

                    case IMPORT_NAME: {
                        // Todo: Review this regarding Python 2.7-update
                        PyObject __import__ = f.f_builtins.__finditem__("__import__");
                        if (__import__ == null) {
                            throw Py.ImportError("__import__ not found");
                        }
                        PyString name = Py.newString(co_names[oparg]);
                        PyObject fromlist = stack.pop();
                        PyObject level = stack.pop();

                        if (level.asInt() != -1) {
                            stack.push(__import__.__call__(new PyObject[]{name, f.f_globals, f.f_locals, fromlist, level}));
                        } else {
                            stack.push(__import__.__call__(new PyObject[]{name, f.f_globals, f.f_locals, fromlist}));
                        }
                        break;
                    }

                    case IMPORT_STAR: {
                        // Todo: Review this regarding Python 2.7-update
                        PyObject module = stack.pop();
                        imp.importAll(module, f);
                        break;
                    }

                    case IMPORT_FROM:
                        String name = co_names[oparg];
                        try {
                            stack.push(stack.top().__getattr__(name));

                        } catch (PyException pye) {
                            if (pye.match(Py.AttributeError)) {
                                throw Py.ImportError(String.format("cannot import name %.230s", name));
                            } else {
                                throw pye;
                            }
                        }
                        break;

                    case JUMP_FORWARD:
                        next_instr += oparg;
                        break;

                    case POP_JUMP_IF_FALSE:
                        if (!stack.pop().__nonzero__()) {
                            next_instr = oparg;
                        }
                        break;

                    case POP_JUMP_IF_TRUE:
                        if (stack.pop().__nonzero__()) {
                            next_instr = oparg;
                        }
                        break;

                    case JUMP_IF_FALSE_OR_POP:
                        if (stack.top().__nonzero__()) {
                            --stack.top;
                        } else {
                            next_instr = oparg;
                        }
                        break;

                    case JUMP_IF_TRUE_OR_POP:
                        if (!stack.top().__nonzero__()) {
                            --stack.top;
                        } else {
                            next_instr = oparg;
                        }
                        break;

                    case JUMP_ABSOLUTE:
                        next_instr = oparg;
                        break;

                    case GET_ITER: {
                        PyObject it = stack.top().__iter__();
                        if (it != null) {
                            stack.set_top(it);
                        }
                        break;
                    }

                    case FOR_ITER: {
                        PyObject it = stack.pop();
                        try {
                            PyObject x = it.__iternext__();
                            if (x != null) {
                                stack.push(it);
                                stack.push(x);
                                break;
                            }
                        } catch (PyException pye) {
                            if (!pye.match(Py.StopIteration)) {
                                throw pye;
                            }
                        }
                        next_instr += oparg;
                        break;
                    }

                    case BREAK_LOOP:
                        why = Why.BREAK;
                        break;

                    case CONTINUE_LOOP:
                        retval = Py.newInteger(oparg);
                        if (retval.__nonzero__()) {
                            why = Why.CONTINUE;
                        }
                        break;

                    case SETUP_LOOP:
                    case SETUP_EXCEPT:
                    case SETUP_FINALLY:
                        pushBlock(f, new PyTryBlock(opcode, next_instr + oparg, stack.size()));
                        break;

                    case SETUP_WITH: {
                        PyObject w = stack.top();
                        PyObject exit = w.__getattr__("__exit__");
                        if (exit == null) {
                            break;
                        }
                        stack.set_top(exit);
                        PyObject enter = w.__getattr__("__enter__");
                        if (enter == null) {
                            break;
                        }
                        w = enter.__call__();
                        if (w == null) {
                            break;
                        }
                        /* Setup a finally block (SETUP_WITH as a block is
                        equivalent to SETUP_FINALLY except it normalizes
                        the exception) before pushing the result of
                        __enter__ on the stack. */
                        pushBlock(f, new PyTryBlock(opcode, next_instr + oparg, stack.size()));
                        stack.push(w);
                        break;
                    }

                    case WITH_CLEANUP: {
                     /* At the top of the stack are 1-3 values indicating
                        how/why we entered the finally clause:
                        - TOP = None
                        - (TOP, SECOND) = (WHY_{RETURN,CONTINUE}), retval
                        - TOP = WHY_*; no retval below it
                        - (TOP, SECOND, THIRD) = exc_info()
                        Below them is EXIT, the context.__exit__ bound method.
                        In the last case, we must call
                          EXIT(TOP, SECOND, THIRD)
                        otherwise we must call
                          EXIT(None, None, None)

                        In all cases, we remove EXIT from the stack, leaving
                        the rest in the same order.

                        In addition, if the stack represents an exception,
                        *and* the function call returns a 'true' value, we
                        "zap" this information, to prevent END_FINALLY from
                        re-raising the exception.  (But non-local gotos
                        should still be resumed.)
                     */
                        PyObject exit;
                        PyObject u = stack.pop(), v, w;
                        if (u == Py.None) {
                            exit = stack.top();
                            stack.set_top(u);
                            v = w = Py.None;
                        } else if (u instanceof PyStackWhy) {
                            switch (((PyStackWhy) u).why) {
                            case RETURN:
                            case CONTINUE:
                                exit = stack.top(2);
                                stack.set_top(2, stack.top());
                                stack.set_top(u);
                                break;
                            default:
                                exit = stack.top();
                                stack.set_top(u);
                            }
                            u = v = w = Py.None;
                        } else {
                            v = stack.top();
                            w = stack.top(2);
                            exit = stack.top(3);
                            stack.set_top(u);
                            stack.set_top(2, v);
                            stack.set_top(3, w);
                        }
                        PyObject x = null;
                        if (u instanceof PyStackException) {
                            PyException exc = ((PyStackException) u).exception;
                            x = exit.__call__(exc.type, exc.value, exc.traceback);
                        } else {
                            x = exit.__call__(u, v, w);
                        }

                        if (u != Py.None && x != null && x.__nonzero__()) {
                            stack.top -= 2; // XXX - consider stack.stackadj op
                            stack.set_top(Py.None);
                        }
                        break;
                    }

                    case CALL_FUNCTION: {
                        // Todo: Review this regarding Python 2.7-update
                        int na = oparg & 0xff;
                        int nk = (oparg >> 8) & 0xff;

                        if (nk == 0) {
                            call_function(na, stack);
                        } else {
                            call_function(na, nk, stack);
                        }
                        break;
                    }

                    case CALL_FUNCTION_VAR:
                    case CALL_FUNCTION_KW:
                    case CALL_FUNCTION_VAR_KW: {
                        // Todo: Review this regarding Python 2.7-update
                        int na = oparg & 0xff;
                        int nk = (oparg >> 8) & 0xff;
                        int flags = (opcode - CALL_FUNCTION) & 3;
                        call_function(na, nk,
                                (flags & CALL_FLAG_VAR) != 0,
                                (flags & CALL_FLAG_KW) != 0,
                                stack);
                        break;
                    }

                    case MAKE_FUNCTION: {
                        // Todo: Review this regarding Python 2.7-update
                        PyCode code = (PyCode) stack.pop();
                        PyObject[] defaults = stack.popN(oparg);
                        PyObject doc = null;
                        if (code instanceof PyBytecode && ((PyBytecode) code).co_consts.length > 0) {
                            doc = ((PyBytecode) code).co_consts[0];
                        }
                        PyFunction func = new PyFunction(f.f_globals, defaults, code, doc);
                        stack.push(func);
                        break;
                    }

                    case MAKE_CLOSURE: {
                        PyCode code = (PyCode) stack.pop();
                        PyObject[] closure_cells = ((PySequenceList) (stack.pop())).getArray();
                        PyObject[] defaults = stack.popN(oparg);
                        PyObject doc = null;
                        if (code instanceof PyBytecode && ((PyBytecode) code).co_consts.length > 0) {
                            doc = ((PyBytecode) code).co_consts[0];
                        }
                        PyFunction func = new PyFunction(f.f_globals, defaults, code, doc, closure_cells);
                        stack.push(func);
                        break;
                    }

                    case BUILD_SLICE: {
                        PyObject step = oparg == 3 ? stack.pop() : null;
                        PyObject stop = stack.pop();
                        PyObject start = stack.pop();
                        stack.push(new PySlice(start, stop, step));
                        break;
                    }

                    case EXTENDED_ARG:
                        // Todo: Review this regarding Python 2.7-update
                        opcode = getUnsigned(co_code, next_instr++);
                        next_instr += 2;
                        oparg = oparg << 16 | ((getUnsigned(co_code, next_instr) << 8) +
                                getUnsigned(co_code, next_instr - 1));
                        break;

                    default:
                        Py.print(Py.getSystemState().stderr,
                                Py.newString(
                                String.format("XXX lineno: %d, opcode: %d\n",
                                f.f_lasti, opcode)));
                        throw Py.SystemError("unknown opcode");

                } // end switch
            } // end try
            catch (Throwable t) {
                PyException pye = Py.setException(t, f);
                why = Why.EXCEPTION;
                ts.exception = pye;
                if (debug) {
                    System.err.println("Caught exception:" + pye);
                }
            }

            if (why == Why.YIELD) {
                break;
            }

            // do some trace handling here, but for now just convert to EXCEPTION
            if (why == Why.RERAISE) {
                why = Why.EXCEPTION;
            }

            while (why != Why.NOT && blocksLeft(f)) {
                PyTryBlock b = popBlock(f);
                if (debug) {
                    System.err.println("Processing block: " + b);
                }
                assert (why != Why.YIELD);
                if (b.b_type == SETUP_LOOP && why == Why.CONTINUE) {
                    pushBlock(f, b);
                    why = Why.NOT;
                    next_instr = retval.asInt();
                    break;
                }
                while (stack.size() > b.b_level) {
                    stack.pop();
                }
                if (b.b_type == SETUP_LOOP && why == Why.BREAK) {
                    why = Why.NOT;
                    next_instr = b.b_handler;
                    break;
                }
                if (b.b_type == SETUP_FINALLY || (b.b_type == SETUP_EXCEPT && why == Why.EXCEPTION)
                        || b.b_type == SETUP_WITH) {
                    if (why == Why.EXCEPTION) {
                        PyException exc = ts.exception;
                        if (b.b_type == SETUP_EXCEPT || b.b_type == SETUP_WITH) {
                            exc.normalize();
                        }
                        stack.push(exc.traceback);
                        stack.push(exc.value);
                        stack.push(new PyStackException(exc)); // instead of stack.push(exc.type), like CPython
                    } else {
                        if (why == Why.RETURN || why == Why.CONTINUE) {
                            stack.push(retval);
                        }
                        stack.push(new PyStackWhy(why));
                    }
                    why = Why.NOT;
                    next_instr = b.b_handler;
                    break;
                }
            } // unwind block stack

            if (why != Why.NOT) {
                break;
            }

        } // end-while of the instruction loop

        if (why != Why.YIELD) {
            while (stack.size() > 0) {
                stack.pop();
            }
            if (why != Why.RETURN) {
                retval = Py.None;
            }
        } else {
            // store the stack in the frame for reentry from the yield;
            f.f_savedlocals = stack.popN(stack.size());
        }

        f.f_lasti = next_instr; // need to update on function entry, etc

        if (debug) {
            System.err.println(count + "," + f.f_lasti + "> Returning from " + why + ": " + retval +
                    ", stack: " + stack.toString() +
                    ", blocks: " + stringify_blocks(f));
        }

        if (why == Why.EXCEPTION) {
            throw ts.exception;
        }

        if (co_flags.isFlagSet(CodeFlag.CO_GENERATOR) && why == Why.RETURN && retval == Py.None) {
            f.f_lasti = -1;
        }

        return retval;
    }

    private static void call_function(int na, PyStack stack) {
        switch (na) {
            case 0: {
                PyObject callable = stack.pop();
                stack.push(callable.__call__());
                break;
            }
            case 1: {
                PyObject arg = stack.pop();
                PyObject callable = stack.pop();
                stack.push(callable.__call__(arg));
                break;
            }
            case 2: {
                PyObject arg1 = stack.pop();
                PyObject arg0 = stack.pop();
                PyObject callable = stack.pop();
                stack.push(callable.__call__(arg0, arg1));
                break;
            }
            case 3: {
                PyObject arg2 = stack.pop();
                PyObject arg1 = stack.pop();
                PyObject arg0 = stack.pop();
                PyObject callable = stack.pop();
                stack.push(callable.__call__(arg0, arg1, arg2));
                break;
            }
            case 4: {
                PyObject arg3 = stack.pop();
                PyObject arg2 = stack.pop();
                PyObject arg1 = stack.pop();
                PyObject arg0 = stack.pop();
                PyObject callable = stack.pop();
                stack.push(callable.__call__(arg0, arg1, arg2, arg3));
                break;
            }
            default: {
                PyObject args[] = stack.popN(na);
                PyObject callable = stack.pop();
                stack.push(callable.__call__(args));
            }
        }
    }

    private static void call_function(int na, int nk, PyStack stack) {
        int n = na + nk * 2;
        PyObject params[] = stack.popN(n);
        PyObject callable = stack.pop();

        PyObject args[] = new PyObject[na + nk];
        String keywords[] = new String[nk];
        int i;
        for (i = 0; i < na; i++) {
            args[i] = params[i];
        }
        for (int j = 0; i < n; i += 2, j++) {
            keywords[j] = params[i].toString();
            args[na + j] = params[i + 1];
        }
        stack.push(callable.__call__(args, keywords));
    }

    private static void call_function(int na, int nk, boolean var, boolean kw, PyStack stack) {
        int n = na + nk * 2;
        PyObject kwargs = kw ? stack.pop() : null;
        PyObject starargs = var ? stack.pop() : null;
        PyObject params[] = stack.popN(n);
        PyObject callable = stack.pop();

        PyObject args[] = new PyObject[na + nk];
        String keywords[] = new String[nk];
        int i;
        for (i = 0; i < na; i++) {
            args[i] = params[i];
        }
        for (int j = 0; i < n; i += 2, j++) {
            keywords[j] = params[i].toString();
            args[na + j] = params[i + 1];

        }
        stack.push(callable._callextra(args, keywords, starargs, kwargs));
    }

    private static void unpack_iterable(int oparg, PyStack stack) {
        PyObject v = stack.pop();
        int i = oparg;
        PyObject items[] = new PyObject[oparg];
        for (PyObject item : v.asIterable()) {
            if (i <= 0) {
                throw Py.ValueError("too many values to unpack");
            }
            i--;
            items[i] = item;
        }
        if (i > 0) {
            throw Py.ValueError(String.format("need more than %d value%s to unpack",
                    i, i == 1 ? "" : "s"));
        }
        for (i = 0; i < oparg; i++) {
            stack.push(items[i]);
        }
    }

    private static class PyStack {
        final PyObject[] stack;
        int top = -1;

        PyStack(int size) {
            stack = new PyObject[size];
        }

        PyObject top() {
            return stack[top];
        }

        PyObject top(int n) {
            return stack[top - n + 1];
        }

        PyObject pop() {
            return stack[top--];
        }

        void push(PyObject v) {
            stack[++top] = v;
        }

        void set_top(PyObject v) {
            stack[top] = v;
        }

        void set_top(int n, PyObject v) {
            stack[top - n + 1] = v;
        }

        void dup() {
            stack[top + 1] = stack[top];
            top++;
        }

        void dup(int n) {
            int oldTop = top;
            top += n;

            for (int i = 0; i < n; i++) {
                stack[top - i] = stack[oldTop - i];
            }
        }

        PyObject[] popN(int n) {
            PyObject ret[] = new PyObject[n];
            top -= n;

            for (int i = 0; i < n; i++) {
                ret[i] = stack[top + i + 1];
            }

            return ret;
        }

        void rot2() {
            PyObject topv = stack[top];
            stack[top] = stack[top - 1];
            stack[top - 1] = topv;
        }

        void rot3() {
            PyObject v = stack[top];
            PyObject w = stack[top - 1];
            PyObject x = stack[top - 2];
            stack[top] = w;
            stack[top - 1] = x;
            stack[top - 2] = v;
        }

        void rot4() {
            PyObject u = stack[top];
            PyObject v = stack[top - 1];
            PyObject w = stack[top - 2];
            PyObject x = stack[top - 3];
            stack[top] = v;
            stack[top - 1] = w;
            stack[top - 2] = x;
            stack[top - 3] = u;
        }

        int size() {
            return top + 1;
        }

        @Override
        public String toString() {
            StringBuilder buffer = new StringBuilder();
            int size = size();
            int N = size > 4 ? 4 : size;
            buffer.append("[");
            for (int i = 0; i < N; i++) {
                if (i > 0) {
                    buffer.append(", ");
                }
                PyObject item = stack[N - i - 1];
                buffer.append(upto(item.__repr__().toString()));
            }
            if (N < size) {
                buffer.append(String.format(", %d more...", size - N));
            }
            buffer.append("]");
            return buffer.toString();
        }

        private String upto(String x) {
            return upto(x, 100);
        }

        private String upto(String x, int n) {
            x = x.replace('\n', '|');
            if (x.length() > n) {
                StringBuilder item = new StringBuilder(x.substring(0, n));
                item.append("...");
                return item.toString();
            } else {
                return x;
            }
        }
    }

    @Untraversable
    private static class PyTryBlock extends PyObject {
        // purely to sit on top of the existing PyFrame in f_exits!!!
        int b_type;         /* what kind of block this is */
        int b_handler;      /* where to jump to find handler */
        int b_level;        /* value stack level to pop to */

        PyTryBlock(int type, int handler, int level) {
            b_type = type;
            b_handler = handler;
            b_level = level;
        }

        @Override
        public String toString() {
            return "<" + get_opname().__getitem__(Py.newInteger(b_type)) + "," +
                    b_handler + "," + b_level + ">";
        }
    }

    @Override
    protected int getline(PyFrame f) {
        int addrq = f.f_lasti;
        int size = co_lnotab.length / 2;
        int p = 0;
        int line = co_firstlineno;
        int addr = 0;
        while (--size >= 0) {
            addr += getUnsigned(co_lnotab, p++);
            if (addr > addrq) {
                break;
            }
            line += getUnsigned(co_lnotab, p++);
        }
        return line;
    }

    private class LineCache {
        List<Integer> addr_breakpoints = new ArrayList<Integer>();
        List<Integer> lines = new ArrayList<Integer>(); // length should be one more than addr_breakpoints

        private LineCache() { // based on dis.findlinestarts
            int size = co_lnotab.length / 2;
            int p = 0;
            int lastline = -1;
            int line = co_firstlineno;
            int addr = 0;
            while (--size >= 0) {
                int byte_incr = getUnsigned(co_lnotab, p++);
                int line_incr = getUnsigned(co_lnotab, p++);
                if (byte_incr > 0) {
                    if (line != lastline) {
                        addr_breakpoints.add(addr);
                        lines.add(line);
                        lastline = line;
                    }
                    addr += byte_incr;
                }
                line += line_incr;
            }
            if (line != lastline) {
                lines.add(line);
            }
        }

        private int getline(int addrq) { // bisect_right to the lineno
            int lo = 0;
            int hi = addr_breakpoints.size();
            while (lo < hi) {
                int mid = (lo + hi) / 2;
                if (addrq < addr_breakpoints.get(mid)) {
                    hi = mid;
                } else {
                    lo = mid + 1;
                }
            }
            return lines.get(lo);
        }

        @Override
        public String toString() {
            return addr_breakpoints.toString() + ";" + lines.toString();
        }
    }

    // Utility functions to enable storage of unsigned bytes in co_code, co_lnotab byte[] arrays
    private static char getUnsigned(byte[] x, int i) {
        byte b = x[i];
        if (b < 0) {
            return (char) (b + 256);
        } else {
            return (char) b;
        }
    }

    private static String getString(byte[] x) {
        StringBuilder buffer = new StringBuilder(x.length);
        for (int i = 0; i < x.length; i++) {
            buffer.append(getUnsigned(x, i));
        }
        return buffer.toString();
    }

    private static byte[] getBytes(String s) {
        int len = s.length();
        byte[] x = new byte[len];
        for (int i = 0; i < len; i++) {
            x[i] = (byte) (s.charAt(i) & 0xFF);
        }
        return x;
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retValue;
        if (co_consts != null) {
            for (PyObject ob: co_consts) {
                if (ob != null) {
                    retValue = visit.visit(ob, arg);
                    if (retValue != 0) {
                        return retValue;
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        if (ob == null || co_consts == null) {
            return false;
        } else {
            for (PyObject obj: co_consts) {
                if (obj == ob) {
                    return true;
                }
            }
            return false;
        }
    }
}
