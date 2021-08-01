package org.python.core;

import static org.python.core.ClassShorthand.B;
import static org.python.core.ClassShorthand.I;
import static org.python.core.ClassShorthand.O;
import static org.python.core.ClassShorthand.OA;
import static org.python.core.ClassShorthand.S;
import static org.python.core.ClassShorthand.SA;
import static org.python.core.ClassShorthand.T;
import static org.python.core.ClassShorthand.V;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.Map;

import org.python.base.InterpreterError;
import org.python.base.MissingFeature;
import org.python.core.ArgumentError.Mode;

/**
 * This {@code enum} provides a set of structured constants that are
 * used to refer to the special methods of the Python data model.
 * <p>
 * These are structured constants that provide not only the
 * {@code String} method name, but also a signature, and much
 * information used internally by the run-time system in the
 * creation of type objects, the interpretation of code and the
 * creation of call sites.
 * <p>
 * In principle, any Python object may support all of the special
 * methods, through "slots" in the Python type object
 * {@code PyType}. These slots have identical names to the
 * corresponding constant in this {@code enum}. The "slots" in the
 * Python type object hold pointers ({@code MethodHandle}s) to their
 * implementations in Java for that type, which of course define the
 * behaviour of instances in Python. Where a special method is
 * absent from the implementation of a type, a default "empty"
 * handle is provided from the {@code Slot} constant.
 */
// Compare CPython struct wrapperbase in descrobject.h
// also typedef slotdef and slotdefs[] table in typeobject.h
enum Slot {
    /*
     * The order of the members is not significant, but we take it from
     * the slotdefs[] table for definiteness. We do not have quite the
     * same entries, and no duplicates. There may yet be special methods
     * here that need not be cached, and maybe properties it would be
     * useful to add.
     */
    /**
     * Defines {@link Operations#op_repr}, support for built-in
     * {@code repr()}, with signature {@link Signature#UNARY}.
     */
    op_repr(Signature.UNARY),
    /**
     * Defines {@link Operations#op_hash}, support for object hashing,
     * with signature {@link Signature#LEN}.
     */
    op_hash(Signature.LEN),
    /**
     * Defines {@link Operations#op_call}, support for calling an
     * object, with signature {@link Signature#CALL}.
     */
    op_call(Signature.CALL),
    /**
     * Defines {@link Operations#op_str}, support for built-in
     * {@code str()}, with signature {@link Signature#UNARY}.
     */
    op_str(Signature.UNARY),

    /**
     * Defines {@link Operations#op_getattribute}, attribute get, with
     * signature {@link Signature#GETATTR}.
     */
    op_getattribute(Signature.GETATTR),
    /**
     * Defines {@link Operations#op_getattr}, attribute get, with
     * signature {@link Signature#GETATTR}.
     */
    op_getattr(Signature.GETATTR),
    /**
     * Defines {@link Operations#op_setattr}, attribute set, with
     * signature {@link Signature#SETATTR}.
     */
    op_setattr(Signature.SETATTR),
    /**
     * Defines {@link Operations#op_delattr}, attribute deletion, with
     * signature {@link Signature#DELATTR}.
     */
    op_delattr(Signature.DELATTR),

    /**
     * Defines {@link Operations#op_lt}, the {@code <} operation, with
     * signature {@link Signature#BINARY}.
     */
    op_lt(Signature.BINARY, "<"),
    /**
     * Defines {@link Operations#op_le}, the {@code <=} operation, with
     * signature {@link Signature#BINARY}.
     */
    op_le(Signature.BINARY, "<="),
    /**
     * Defines {@link Operations#op_eq}, the {@code ==} operation, with
     * signature {@link Signature#BINARY}.
     */
    op_eq(Signature.BINARY, "=="),
    /**
     * Defines {@link Operations#op_ne}, the {@code !=} operation, with
     * signature {@link Signature#BINARY}.
     */
    op_ne(Signature.BINARY, "!="),
    /**
     * Defines {@link Operations#op_gt}, the {@code >} operation, with
     * signature {@link Signature#BINARY}.
     */
    op_gt(Signature.BINARY, ">"),
    /**
     * Defines {@link Operations#op_ge}, the {@code >=} operation, with
     * signature {@link Signature#BINARY}.
     */
    op_ge(Signature.BINARY, ">="),

    /**
     * Defines {@link Operations#op_iter}, get an iterator, with
     * signature {@link Signature#UNARY}.
     */
    op_iter(Signature.UNARY), // unexplored territory
    /**
     * Defines {@link Operations#op_next}, advance an iterator, with
     * signature {@link Signature#UNARY}.
     */
    op_next(Signature.UNARY), // unexplored territory

    /**
     * Defines {@link Operations#op_get}, descriptor {@code __get__},
     * with signature {@link Signature#DESCRGET}.
     */
    op_get(Signature.DESCRGET),
    /**
     * Defines {@link Operations#op_set}, descriptor {@code __set__},
     * with signature {@link Signature#SETITEM}.
     */
    op_set(Signature.SETITEM),
    /**
     * Defines {@link Operations#op_delete}, descriptor
     * {@code __delete__}, with signature {@link Signature#DELITEM}.
     */
    op_delete(Signature.DELITEM),

    /**
     * Defines {@link Operations#op_init}, object {@code __init__}, with
     * signature {@link Signature#INIT}.
     */
    op_init(Signature.INIT),
    // __new__ is not a slot
    // __del__ is not a slot

    /**
     * Defines {@link Operations#op_await}, with signature
     * {@link Signature#UNARY}.
     */
    op_await(Signature.UNARY), // unexplored territory
    /**
     * Defines {@link Operations#op_aiter}, with signature
     * {@link Signature#UNARY}.
     */
    op_aiter(Signature.UNARY), // unexplored territory
    /**
     * Defines {@link Operations#op_anext}, with signature
     * {@link Signature#UNARY}.
     */
    op_anext(Signature.UNARY), // unexplored territory

    // Binary ops: reflected form comes first so we can reference it.
    /**
     * Defines {@link Operations#op_radd}, the reflected {@code +}
     * operation, with signature {@link Signature#BINARY}.
     */
    op_radd(Signature.BINARY, "+"),
    /**
     * Defines {@link Operations#op_rsub}, the reflected {@code -}
     * operation, with signature {@link Signature#BINARY}.
     */
    op_rsub(Signature.BINARY, "-"),
    /**
     * Defines {@link Operations#op_rmul}, the reflected {@code *}
     * operation, with signature {@link Signature#BINARY}.
     */
    op_rmul(Signature.BINARY, "*"),
    /**
     * Defines {@link Operations#op_rmod}, the reflected {@code %}
     * operation, with signature {@link Signature#BINARY}.
     */
    op_rmod(Signature.BINARY, "%"),
    /**
     * Defines {@link Operations#op_rdivmod}, the reflected
     * {@code divmod} operation, with signature
     * {@link Signature#BINARY}.
     */
    op_rdivmod(Signature.BINARY, "divmod()"),
    /**
     * Defines {@link Operations#op_rpow}, the reflected {@code pow}
     * operation, with signature {@link Signature#BINARY} (not
     * {@link Signature#TERNARY} since only an infix operation can be
     * reflected).
     */
    op_rpow(Signature.BINARY, "**"), // unexplored territory
    /**
     * Defines {@link Operations#op_rlshift}, the reflected {@code <<}
     * operation, with signature {@link Signature#BINARY}.
     */
    op_rlshift(Signature.BINARY, "<<"),
    /**
     * Defines {@link Operations#op_rrshift}, the reflected {@code >>}
     * operation, with signature {@link Signature#BINARY}.
     */
    op_rrshift(Signature.BINARY, ">>"),
    /**
     * Defines {@link Operations#op_rand}, the reflected {@code &}
     * operation, with signature {@link Signature#BINARY}.
     */
    op_rand(Signature.BINARY, "&"),
    /**
     * Defines {@link Operations#op_rxor}, the reflected {@code ^}
     * operation, with signature {@link Signature#BINARY}.
     */
    op_rxor(Signature.BINARY, "^"),
    /**
     * Defines {@link Operations#op_ror}, the reflected {@code |}
     * operation, with signature {@link Signature#BINARY}.
     */
    op_ror(Signature.BINARY, "|"),
    /**
     * Defines {@link Operations#op_rfloordiv}, the reflected {@code //}
     * operation, with signature {@link Signature#BINARY}.
     */
    op_rfloordiv(Signature.BINARY, "//"),
    /**
     * Defines {@link Operations#op_rtruediv}, the reflected {@code /}
     * operation, with signature {@link Signature#BINARY}.
     */
    op_rtruediv(Signature.BINARY, "/"),
    /**
     * Defines {@link Operations#op_rmatmul}, the reflected {@code @}
     * operation, with signature {@link Signature#BINARY}.
     */
    op_rmatmul(Signature.BINARY, "@"),

    /**
     * Defines {@link Operations#op_add}, the {@code +} operation, with
     * signature {@link Signature#BINARY}.
     */
    op_add(Signature.BINARY, "+", op_radd),
    /**
     * Defines {@link Operations#op_sub}, the {@code -} operation, with
     * signature {@link Signature#BINARY}.
     */
    op_sub(Signature.BINARY, "-", op_rsub),
    /**
     * Defines {@link Operations#op_mul}, the {@code *} operation, with
     * signature {@link Signature#BINARY}.
     */
    op_mul(Signature.BINARY, "*", op_rmul),
    /**
     * Defines {@link Operations#op_mod}, the {@code %} operation, with
     * signature {@link Signature#BINARY}.
     */
    op_mod(Signature.BINARY, "%", op_rmod),
    /**
     * Defines {@link Operations#op_divmod}, the {@code divmod}
     * operation, with signature {@link Signature#BINARY}.
     */
    op_divmod(Signature.BINARY, "divmod()", op_rdivmod),
    /**
     * Defines {@link Operations#op_pow}, the {@code pow} operation,
     * with signature {@link Signature#TERNARY}.
     */
    op_pow(Signature.TERNARY, "**", op_rpow), // unexplored territory

    /**
     * Defines {@link Operations#op_neg}, the unary {@code -} operation,
     * with signature {@link Signature#UNARY}.
     */
    op_neg(Signature.UNARY, "unary -"),
    /**
     * Defines {@link Operations#op_pos}, the unary {@code +} operation,
     * with signature {@link Signature#UNARY}.
     */
    op_pos(Signature.UNARY, "unary +"),
    /**
     * Defines {@link Operations#op_abs}, the {@code abs()} operation,
     * with signature {@link Signature#UNARY}.
     */
    op_abs(Signature.UNARY, "abs()"),
    /**
     * Defines {@link Operations#op_bool}, conversion to a truth value,
     * with signature {@link Signature#PREDICATE}.
     */
    op_bool(Signature.PREDICATE),
    /**
     * Defines {@link Operations#op_invert}, the unary {@code ~}
     * operation, with signature {@link Signature#UNARY}.
     */
    op_invert(Signature.UNARY, "unary ~"),

    /**
     * Defines {@link Operations#op_lshift}, the {@code <<} operation,
     * with signature {@link Signature#BINARY}.
     */
    op_lshift(Signature.BINARY, "<<", op_rlshift),
    /**
     * Defines {@link Operations#op_rshift}, the {@code >>} operation,
     * with signature {@link Signature#BINARY}.
     */
    op_rshift(Signature.BINARY, ">>", op_rrshift),
    /**
     * Defines {@link Operations#op_and}, the {@code &} operation, with
     * signature {@link Signature#BINARY}.
     */
    op_and(Signature.BINARY, "&", op_rand),
    /**
     * Defines {@link Operations#op_xor}, the {@code ^} operation, with
     * signature {@link Signature#BINARY}.
     */
    op_xor(Signature.BINARY, "^", op_rxor),
    /**
     * Defines {@link Operations#op_or}, the {@code |} operation, with
     * signature {@link Signature#BINARY}.
     */
    op_or(Signature.BINARY, "|", op_ror),

    /**
     * Defines {@link Operations#op_int}, conversion to an integer
     * value, with signature {@link Signature#UNARY}.
     */
    op_int(Signature.UNARY),
    /**
     * Defines {@link Operations#op_float}, conversion to a float value,
     * with signature {@link Signature#UNARY}.
     */
    op_float(Signature.UNARY),

    /**
     * Defines {@link Operations#op_iadd}, the {@code +=} operation,
     * with signature {@link Signature#BINARY}.
     */
    op_iadd(Signature.BINARY, "+="), // in-place: unexplored territory
    /**
     * Defines {@link Operations#op_isub}, the {@code -=} operation,
     * with signature {@link Signature#BINARY}.
     */
    op_isub(Signature.BINARY, "-="),
    /**
     * Defines {@link Operations#op_imul}, the {@code *=} operation,
     * with signature {@link Signature#BINARY}.
     */
    op_imul(Signature.BINARY, "*="),
    /**
     * Defines {@link Operations#op_imod}, the {@code %=} operation,
     * with signature {@link Signature#BINARY}.
     */
    op_imod(Signature.BINARY, "%="),
    /**
     * Defines {@link Operations#op_iand}, the {@code &=} operation,
     * with signature {@link Signature#BINARY}.
     */
    op_iand(Signature.BINARY, "&="),
    /**
     * Defines {@link Operations#op_ixor}, the {@code ^=} operation,
     * with signature {@link Signature#BINARY}.
     */
    op_ixor(Signature.BINARY, "^="),
    /**
     * Defines {@link Operations#op_ior}, the {@code |=} operation, with
     * signature {@link Signature#BINARY}.
     */
    op_ior(Signature.BINARY, "|="),

    /**
     * Defines {@link Operations#op_floordiv}, the {@code //} operation,
     * with signature {@link Signature#BINARY}.
     */
    op_floordiv(Signature.BINARY, "//", op_rfloordiv),
    /**
     * Defines {@link Operations#op_truediv}, the {@code /} operation,
     * with signature {@link Signature#BINARY}.
     */
    op_truediv(Signature.BINARY, "/", op_rtruediv),
    /**
     * Defines {@link Operations#op_ifloordiv}, the {@code //=}
     * operation, with signature {@link Signature#BINARY}.
     */
    op_ifloordiv(Signature.BINARY, "//="),
    /**
     * Defines {@link Operations#op_itruediv}, the {@code /=} operation,
     * with signature {@link Signature#BINARY}.
     */
    op_itruediv(Signature.BINARY, "/="),

    /**
     * Defines {@link Operations#op_index}, conversion to an index
     * value, with signature {@link Signature#UNARY}.
     */
    op_index(Signature.UNARY),

    /**
     * Defines {@link Operations#op_matmul}, the {@code @} (matrix
     * multiply) operation, with signature {@link Signature#BINARY}.
     */
    op_matmul(Signature.BINARY, "@", op_rmatmul),
    /**
     * Defines {@link Operations#op_imatmul}, the {@code @=} (matrix
     * multiply in place) operation, with signature
     * {@link Signature#BINARY}.
     */
    op_imatmul(Signature.BINARY, "@="),

    /*
     * Note that CPython repeats for "mappings" the following "sequence"
     * slots, and slots for __add_ and __mul__, but that we do not need
     * to.
     */
    /**
     * Defines {@link Operations#op_len}, support for built-in
     * {@code len()}, with signature {@link Signature#LEN}.
     */
    op_len(Signature.LEN, "len()"),
    /**
     * Defines {@link Operations#op_getitem}, get at index, with
     * signature {@link Signature#BINARY}.
     */
    op_getitem(Signature.BINARY),
    /**
     * Defines {@link Operations#op_setitem}, set at index, with
     * signature {@link Signature#SETITEM}.
     */
    op_setitem(Signature.SETITEM),
    /**
     * Defines {@link Operations#op_delitem}, delete from index, with
     * signature {@link Signature#DELITEM}.
     */
    op_delitem(Signature.DELITEM),
    /**
     * Defines {@link Operations#op_contains}, the {@code in} operation,
     * with signature {@link Signature#BINARY_PREDICATE}.
     */
    op_contains(Signature.BINARY_PREDICATE);

    /** Method signature to match when filling this slot. */
    final Signature signature;
    /** Name of implementation method to bind e.g. "{@code __add__}". */
    final String methodName;
    /** Name to use in error messages, e.g. "{@code +}" */
    final String opName;
    /** Handle to throw a {@link TypeError} (same signature as slot). */
    private MethodHandle operandError;
    /** Description to use in help messages */
    final String doc;
    /** Reference to field holding this slot in an {@link Operations} */
    final VarHandle slotHandle;
    /** The alternate slot e.g. {@code __radd__} in {@code __add__}. */
    final Slot alt;

    /**
     * Constructor for enum constants.
     *
     * @param signature of the function to be called
     * @param opName symbol (such as "+")
     * @param methodName implementation method (e.g. "__add__")
     * @param alt alternate slot (e.g. "op_radd")
     */
    Slot(Signature signature, String opName, String methodName, Slot alt) {
        this.opName = opName == null ? name() : opName;
        this.methodName = dunder(methodName);
        this.signature = signature;
        this.slotHandle = Util.slotHandle(this);
        this.alt = alt;
        // XXX Need something convenient as in CPython.
        this.doc = "Doc of " + this.opName;
    }

    Slot(Signature signature) { this(signature, null, null, null); }

    Slot(Signature signature, String opName) { this(signature, opName, null, null); }

    Slot(Signature signature, String opName, Slot alt) { this(signature, opName, null, alt); }

    /** Compute corresponding double-underscore method name. */
    private String dunder(String methodName) {
        if (methodName != null)
            return methodName;
        else {
            String s = name();
            int i = s.indexOf('_');
            if (i == 2)
                s = "__" + s.substring(i + 1) + "__";
            return s;
        }
    }

    @Override
    public java.lang.String toString() {
        return "Slot." + name() + " ( " + methodName + signature.type + " ) [" + signature.name()
                + "]";
    }

    /**
     * Lookup by method name, returning {@code null} if it is not a
     * recognised name for any slot.
     *
     * @param name of a (possible) special method
     * @return the Slot corresponding, or {@code null}
     */
    public static Slot forMethodName(String name) { return Util.getMethodNameTable().get(name); }

    /**
     * Get the name of the method that, by convention, identifies the
     * corresponding operation in the implementing class. This is not
     * the same as the slot name.
     *
     * @return conventional special method name.
     */
    String getMethodName() { return methodName; }

    /**
     * Return the invocation type of slots of this name.
     *
     * @return the invocation type of slots of this name.
     */
    MethodType getType() { return signature.empty.type(); }

    /**
     * Get the default that fills the slot when it is "empty".
     *
     * @return empty method handle for this type of slot
     */
    MethodHandle getEmpty() { return signature.empty; }

    /**
     * Get a handle to throw a {@link TypeError} with a message
     * conventional for the slot. This handle has the same signature as
     * the slot, and some data specific to the slot. This is useful when
     * the target of a call site may have to raise a type error.
     *
     * @return throwing method handle for this type of slot
     */
    MethodHandle getOperandError() {
        // Not in the constructor so as not to provoke PyType
        if (operandError == null) {
            // Possibly racing, but that's harmless
            operandError = Util.operandError(this);
        }
        return operandError;
    }

    /**
     * Test whether this slot is non-empty in the given operations
     * object.
     *
     * @param ops to examine for this slot
     * @return true iff defined (non-empty)
     */
    boolean isDefinedFor(Operations ops) { return slotHandle.get(ops) != signature.empty; }

    /**
     * Get the {@code MethodHandle} of this slot's operation from the
     * given operations object. Each member of this {@code enum}
     * corresponds to a method handle of the same name, which must also
     * have the correct signature.
     *
     * @param ops target operations object
     * @return current contents of this slot in {@code ops}
     */
    MethodHandle getSlot(Operations ops) { return (MethodHandle)slotHandle.get(ops); }

    /**
     * Get the {@code MethodHandle} of this slot's "alternate" operation
     * from the given operations object. For a binary operation this is
     * the reflected operation.
     *
     * @param ops target operations object
     * @return current contents of the alternate slot in {@code t}
     * @throws NullPointerException if there is no alternate
     */
    MethodHandle getAltSlot(Operations ops) throws NullPointerException {
        return (MethodHandle)alt.slotHandle.get(ops);
    }

    /**
     * Set the {@code MethodHandle} of this slot's operation in the
     * given operations object.
     *
     * @param ops target type object
     * @param mh handle value to assign
     */
    void setHandle(Operations ops, MethodHandle mh) {
        if (mh == null || !mh.type().equals(getType()))
            throw slotTypeError(this, mh);
        slotHandle.set(ops, mh);
    }

    /**
     * Set the {@code MethodHandle} of this slot's operation in the
     * given operations object to one that calls the object given in a
     * manner appropriate to its type. This method is used when updating
     * setting the operation slots of a new type from the new type's
     * dictionary, and when updating them after a change. The object
     * argument is then the entry found by lookup of this slot's name.
     * It may be {@code null} if no entry was found.
     * <p>
     * Where the object is a {@link PyWrapperDescr}, the wrapped method
     * handle will be set as by
     * {@link #setHandle(Operations, MethodHandle)}. The
     * {@link PyWrapperDescr#slot} is not necessarily this slot: client
     * Python code can enter any wrapper descriptor against the name.
     *
     * @param ops target {@code Operations} (or {@code PyType}).
     * @param def object defining the handle to set (or {@code null})
     */
    // Compare CPython update_one_slot in typeobject.c
    void setDefinition(Operations ops, Object def) {
        MethodHandle mh;
        if (def == null) {
            // No definition available for the special method
            if (this == op_next) {
                // XXX We should special-case __next__
                /*
                 * In CPython, this slot is sometimes null=empty, and sometimes
                 * _PyObject_NextNotImplemented. PyIter_Check checks both, but
                 * PyIter_Next calls it without checking and a null would then cause
                 * a crash. We have EmptyException for a similar purpose.
                 */
            }
            mh = signature.empty;

        } else if (def instanceof PyWrapperDescr) {
            /*
             * When we invoke this slot in ops, the Java class of self will be
             * assignable to ops.getJavaClass(), since that class led us to ops.
             * It had better also be compatible with the method ultimately
             * invoked by the handle we install. We have no control over what
             * gets into the dictionary of a type, however, we do know that
             * method in a PyWrapperDescr are applicable to the accepted
             * implementations of classes of their defining class. We check here
             * that ops.getJavaClass() is assignable to an accepted
             * implementation of the defining type.
             */
            PyWrapperDescr wd = (PyWrapperDescr)def;
            mh = wd.getWrapped(ops.getJavaClass());
            if (wd.slot.signature != signature || mh == signature.empty) {
                /*
                 * wd is not compatible with objects of the type(s) that will show
                 * up at this slot: for example we have inserted float.__add__ into
                 * a sub-type of int. Python chooses to fail later, when the slot is
                 * bound or invoked, so insert something that checks.
                 */
                throw new MissingFeature("equivalent of the slot_* functions");
                // mh = signature.slotCalling(def);
            }

        } else if (def == Py.None && this == op_hash) {
            throw new MissingFeature("special case __hash__ == None");
            // mh = PyObject_HashNotImplemented

        } else {
            throw new MissingFeature("equivalent of the slot_* functions");
            // mh = makeSlotHandle(wd);
        }

        slotHandle.set(ops, mh);
    }

    /** The type of exception thrown by invoking an empty slot. */
    static class EmptyException extends Exception {
        private static final long serialVersionUID = 1L;

        // Suppression and stack trace disabled since singleton.
        EmptyException() { super(null, null, false, false); }
    }

    /**
     * An enumeration of the acceptable signatures for slots in an {link
     * Operations} object. For each {@code MethodHandle} we may place in
     * a slot of the {@code Operations} object, we must know in advance
     * the acceptable signature (the {@code MethodType}), and the slot
     * when empty must contain a handle with this signature to a method
     * that will raise {@link EmptyException}. Each {@code enum}
     * constant here gives a symbolic name to that {@code MethodType},
     * and provides the handle used when a slot of that type is empty.
     * <p>
     * Names are equivalent to {@code typedef}s provided in CPython
     * {@code Include/object.h}, but are not exactly the same. We do not
     * need quite the same signatures as CPython: we do not return
     * integer status, for example. Also, C-specifics like
     * {@code Py_ssize_t} are echoed in the C-API names but not here.
     * <p>
     * The shorthand notation we use to describe a signature, for
     * example {@code (O,O[],S[])O}, essentially specifies a
     * {@code MethodType}, and may be decoded as follows.
     * <table>
     * <tr>
     * <th>Shorthand</th>
     * <th>Java class</th>
     * </tr>
     * <tr>
     * <td>{@link ClassShorthand#B B}</td>
     * <td>{@code boolean.class}</td>
     * </tr>
     * <tr>
     * <td>{@link ClassShorthand#I I}</td>
     * <td>{@code int.class}</td>
     * </tr>
     * <tr>
     * <td>{@link ClassShorthand#O O}</td>
     * <td>{@code Object.class}</td>
     * </tr>
     * <tr>
     * <td>{@link ClassShorthand#S S}</td>
     * <td>{@code String.class}</td>
     * </tr>
     * <tr>
     * <td>{@link ClassShorthand#T T}</td>
     * <td>{@link PyType PyType.class}</td>
     * </tr>
     * <tr>
     * <td>{@link ClassShorthand#T V}</td>
     * <td>{@code void.class}</td>
     * </tr>
     * <tr>
     * <td>{@code []}</td>
     * <td>array of</td>
     * </tr>
     * <tr>
     * </tr>
     * <caption>Signature shorthands</caption>
     * </table>
     */
    enum Signature {

        /*
         * The makeDescriptor overrides returning anonymous sub-classes of
         * PyWrapperDescr are fairly ugly. However, sub-classes seem to be
         * the right solution, and defining them here keeps information
         * together that belongs together.
         */

        /**
         * The signature {@code (O)O}, for example {@link Slot#op_repr} or
         * {@link Slot#op_neg}.
         */
        // In CPython: unaryfunc
        UNARY(O, O) {

            @Override
            Object callWrapped(MethodHandle wrapped, Object self, Object[] args, String[] names)
                    throws ArgumentError, Throwable {
                checkArgs(args, 0, names);
                return wrapped.invokeExact(self);
            }
        },

        /**
         * The signature {@code (O,O)O}, for example {@link Slot#op_add} or
         * {@link Slot#op_getitem}.
         */
        // In CPython: binaryfunc
        BINARY(O, O, O) {

            @Override
            Object callWrapped(MethodHandle wrapped, Object self, Object[] args, String[] names)
                    throws ArgumentError, Throwable {
                checkArgs(args, 1, names);
                return wrapped.invokeExact(self, args[0]);
            }
        },
        /**
         * The signature {@code (O,O,O)O}, used for {@link Slot#op_pow}.
         */
        // In CPython: ternaryfunc
        TERNARY(O, O, O, O),

        /**
         * The signature {@code (O,O[],S[])O}, used for
         * {@link Slot#op_call}. Note that in Jython, standard calls are
         * what CPython refers to as vector calls (although they cannot use
         * a stack slice as the array).
         */
        // Not in CPython
        CALL(O, O, OA, SA) {

            @Override
            Object callWrapped(MethodHandle wrapped, Object self, Object[] args, String[] names)
                    throws ArgumentError, Throwable {
                return wrapped.invokeExact(self, args, names);
            }
        },

        /**
         * The signature {@code (O)B}, used for {@link Slot#op_bool}.
         */
        // In CPython: inquiry
        PREDICATE(B, O),

        /**
         * The signature {@code (O,O)B}, used for {@link Slot#op_contains}.
         * It is not used for comparisons, because they may return an
         * arbitrary object (e.g. in {@code numpy} array comparison).
         */
        BINARY_PREDICATE(B, O, O),

        /**
         * The signature {@code (O)I}, used for {@link Slot#op_hash} and
         * {@link Slot#op_len}.
         */
        // In CPython: lenfunc
        LEN(I, O) {

            @Override
            Object callWrapped(MethodHandle wrapped, Object self, Object[] args, String[] names)
                    throws ArgumentError, Throwable {
                checkArgs(args, 0, names);
                return (int)wrapped.invokeExact(self);
            }
        },

        /**
         * The signature {@code (O,O,O)V}, used for {@link Slot#op_setitem}
         * and {@link Slot#op_set}. The arguments have quite different
         * meanings in each.
         */
        // In CPython: objobjargproc
        SETITEM(V, O, O, O),

        /**
         * The signature {@code (O,O)V}, used for {@link Slot#op_delitem}
         * and {@link Slot#op_delete}. The arguments have quite different
         * meanings in each.
         */
        // Not in CPython
        DELITEM(V, O, O),

        /**
         * The signature {@code (O,O)S}, used for {@link Slot#op_getattr}.
         */
        // In CPython: getattrofunc
        GETATTR(O, O, S) {

            @Override
            Object callWrapped(MethodHandle wrapped, Object self, Object[] args, String[] names)
                    throws ArgumentError, Throwable {
                checkArgs(args, 1, names);
                String name = args[0].toString();
                return wrapped.invokeExact(self, name);
            }
        },

        /**
         * The signature {@code (O,S,O)V}, used for {@link Slot#op_setattr}.
         */
        // In CPython: setattrofunc
        SETATTR(V, O, S, O) {

            @Override
            Object callWrapped(MethodHandle wrapped, Object self, Object[] args, String[] names)
                    throws ArgumentError, Throwable {
                checkArgs(args, 2, names);
                String name = args[0].toString();
                wrapped.invokeExact(self, name, args[1]);
                return Py.None;
            }
        },

        /**
         * The signature {@code (O,S)V}, used for {@link Slot#op_delattr}.
         */
        // Not in CPython
        DELATTR(V, O, S) {

            @Override
            Object callWrapped(MethodHandle wrapped, Object self, Object[] args, String[] names)
                    throws ArgumentError, Throwable {
                checkArgs(args, 1, names);
                String name = args[0].toString();
                wrapped.invokeExact(self, name);
                return Py.None;
            }
        },

        /**
         * The signature {@code (O,O,T)O}, used for {@link Slot#op_get}.
         */
        // In CPython: descrgetfunc
        DESCRGET(O, O, O, T) {

            @Override
            Object callWrapped(MethodHandle wrapped, Object self, Object[] args, String[] names)
                    throws ArgumentError, Throwable {
                checkArgs(args, 1, 2, names);
                Object obj = args[0];
                if (obj == Py.None) {
                    obj = null;
                }
                Object type = null;
                if (type != Py.None) {
                    type = args[1];
                }
                if (type == null && obj == null) {
                    throw new TypeError("__get__(None, None) is invalid");
                }
                return wrapped.invokeExact(self, obj, (PyType)type);
            }
        },

        /**
         * The signature {@code (O,O,O[],S[])V}, used for
         * {@link Slot#op_init}. This is the same as {@link #CALL} except
         * with {@code void} return.
         */
        // In CPython: initproc
        INIT(V, O, OA, SA);

        /**
         * The signature was defined with this nominal method type.
         */
        final MethodType type;
        /**
         * When empty, the slot should hold this handle. The method type of
         * this handle also tells us the method type by which the slot must
         * always be invoked, see {@link Slot#getType()}.
         */
        final MethodHandle empty;

        /**
         * Constructor to which we specify the signature of the slot, with
         * the same semantics as {@code MethodType.methodType()}. Every
         * {@code MethodHandle} stored in the slot (including
         * {@link Signature#empty}) must be of this method type.
         *
         * @param returnType that the slot functions all return
         * @param ptypes types of parameters the slot function takes
         */
        Signature(Class<?> returnType, Class<?>... ptypes) {
            // The signature is recorded exactly as given
            this.type = MethodType.methodType(returnType, ptypes);
            // em = λ : throw Util.EMPTY
            // (with correct nominal return type for slot)
            MethodHandle em = MethodHandles.throwException(returnType, EmptyException.class)
                    .bindTo(Util.EMPTY);
            // empty = λ u v ... : throw Util.EMPTY
            // (with correct parameter types for slot)
            this.empty = MethodHandles.dropArguments(em, 0, this.type.parameterArray());

            // Prepare the kind of lookup we should do
            Class<?> p0 = ptypes.length > 0 ? ptypes[0] : null;
            if (p0 != O) {
                throw new InterpreterError("Special methods must be instance methods");
            }
        }

        /**
         * Check that no positional or keyword arguments are supplied. This
         * is for use when implementing
         * {@link #callWrapped(MethodHandle, Object, Object[], String[])}.
         *
         * @param args positional argument array to be checked
         * @param names to be checked
         * @throws ArgumentError if positional arguments are given or
         *     {@code names} is not {@code null} or empty
         */
        final protected void checkNoArgs(Object[] args, String[] names) throws ArgumentError {
            if (args.length != 0)
                throw new ArgumentError(Mode.NOARGS);
            else if (names != null && names.length != 0)
                throw new ArgumentError(Mode.NOKWARGS);
        }

        /**
         * Check the number of positional arguments and that no keywords are
         * supplied. This is for use when implementing
         * {@link #callWrapped(MethodHandle, Object, Object[], String[])}.
         *
         * @param args positional argument tuple to be checked
         * @param expArgs expected number of positional arguments
         * @param names to be checked
         * @throws ArgumentError if the wrong number of positional arguments
         *     are given or {@code kwargs} is not {@code null} or empty
         */
        final protected void checkArgs(Object[] args, int expArgs, String[] names)
                throws ArgumentError {
            if (args.length != expArgs)
                throw new ArgumentError(expArgs);
            else if (names != null && names.length != 0)
                throw new ArgumentError(Mode.NOKWARGS);
        }

        /**
         * Check the number of positional arguments and that no keywords are
         * supplied. This is for use when implementing
         * {@link #callWrapped(MethodHandle, Object, Object[], String[])}.
         *
         * @param args positional argument tuple to be checked
         * @param minArgs minimum number of positional arguments
         * @param maxArgs maximum number of positional arguments
         * @param names to be checked
         * @throws ArgumentError if the wrong number of positional arguments
         *     are given or {@code kwargs} is not {@code null} or empty
         */
        final protected void checkArgs(Object[] args, int minArgs, int maxArgs, String[] names)
                throws ArgumentError {
            int n = args.length;
            if (n < minArgs || n > maxArgs)
                throw new ArgumentError(minArgs, maxArgs);
            else if (names != null && names.length != 0)
                throw new ArgumentError(Mode.NOKWARGS);
        }

        /**
         * Invoke the given method handle for the given target {@code self},
         * having arranged the arguments as expected by a slot. We create
         * {@code enum} members of {@code Signature} to handle different
         * slot signatures, in which this method accepts arguments in a
         * generic way (from the interpreter, say) and adapts them to the
         * specific needs of a wrapped method. The caller guarantees that
         * the wrapped method has the {@code Signature} to which the call is
         * addressed.
         *
         * @param wrapped handle of the method to call
         * @param self target object of the method call
         * @param args of the method call
         * @param names of trailing arguments in {@code args}
         * @return result of the method call
         * @throws ArgumentError when the arguments ({@code args},
         *     {@code names}) are not correct for the {@code Signature}
         * @throws Throwable from the implementation of the special method
         */
        // Compare CPython wrap_* in typeobject.c
        // XXX should be abstract, but only when defined for each
        /* abstract */ Object callWrapped(MethodHandle wrapped, Object self, Object[] args,
                String[] names) throws ArgumentError, Throwable {
            checkNoArgs(args, names);
            return wrapped.invokeExact(self);
        }
    }

    /**
     * Helper for {@link Slot#setHandle(PyType, MethodHandle)}, when a
     * bad handle is presented.
     *
     * @param slot that the client attempted to set
     * @param mh offered value found unsuitable
     * @return exception with message filled in
     */
    private static InterpreterError slotTypeError(Slot slot, MethodHandle mh) {
        String fmt = "%s not of required type %s for slot %s";
        return new InterpreterError(fmt, mh, slot.getType(), slot);
    }

    /**
     * Helpers for {@link Slot} and {@link Signature} that can be used
     * in the constructors.
     */
    private static class Util {

        /*
         * This is a class separate from Slot to solve problems with the
         * order of static initialisation. The enum constants have to come
         * first, and their constructors are called as they are encountered.
         * This means that other constants in Slot are not initialised by
         * the time the constructors need them.
         */
        private static final Lookup LOOKUP = MethodHandles.lookup();

        /** Single re-used instance of {@code Slot.EmptyException} */
        static final EmptyException EMPTY = new EmptyException();

        private static Map<String, Slot> methodNameTable = null;

        static Map<String, Slot> getMethodNameTable() {
            if (methodNameTable == null) {
                Slot[] slots = Slot.values();
                methodNameTable = new HashMap<>(2 * slots.length);
                for (Slot s : slots) {
                    methodNameTable.put(s.methodName, s);
                }
            }
            return methodNameTable;
        }

        /**
         * Helper for {@link Slot} constructors at the point they need a
         * handle for their named field within an {@code Operations} class.
         */
        static VarHandle slotHandle(Slot slot) {
            Class<?> opsClass = Operations.class;
            try {
                // The field has the same name as the enum
                return LOOKUP.findVarHandle(opsClass, slot.name(), MethodHandle.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new InterpreterError(e, "creating handle for %s in %s", slot.name(),
                        opsClass.getSimpleName());
            }
        }

        /**
         * Helper for {@link Slot} and thereby for call sites providing a
         * method handle that throws a Python exception when invoked, with
         * an appropriate message for the operation.
         * <p>
         * To be concrete, if the slot is a binary operation, the returned
         * handle may throw something like {@code TypeError:
         * unsupported operand type(s) for -: 'str' and 'str'}.
         *
         * @param slot to mention in the error message
         * @return a handle that throws the exception
         */
        static MethodHandle operandError(Slot slot) {
            // The type of the method that creates the TypeError
            MethodType errorMT = slot.getType().insertParameterTypes(0, Slot.class)
                    .changeReturnType(PyException.class);
            // Exception thrower with nominal return type of the slot
            // thrower = λ(e): throw e
            MethodHandle thrower =
                    MethodHandles.throwException(slot.getType().returnType(), PyException.class);

            try {
                /*
                 * Look up a method f to create the exception, when applied the
                 * arguments v, w, ... (types matching the slot signature) prepended
                 * with this slot. We'll only call it if the handle is invoked.
                 */
                // error = λ(slot, v, w, ...): f(slot, v, w, ...)
                MethodHandle error;
                switch (slot.signature) {
                    case UNARY:
                        // Same name, although signature differs ...
                    case BINARY:
                        error = LOOKUP.findStatic(PyNumber.class, "operandError", errorMT);
                        break;
                    default:
                        // error = λ(slot): default(slot, v, w, ...)
                        error = LOOKUP.findStatic(Util.class, "defaultOperandError", errorMT);
                        // error = λ(slot, v, w, ...): default(slot)
                        error = MethodHandles.dropArguments(error, 0,
                                slot.getType().parameterArray());
                }

                // A handle that creates and throws the exception
                // λ(v, w, ...): throw f(slot, v, w, ...)
                return MethodHandles.collectArguments(thrower, 0, error.bindTo(slot));

            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new InterpreterError(e, "creating handle for type error", slot.name());
            }
        }

        /** Uninformative exception, mentioning the slot. */
        @SuppressWarnings("unused")  // reflected in operandError
        static PyException defaultOperandError(Slot op) {
            return new TypeError("bad operand type for %s", op.opName);
        }
    }
}
