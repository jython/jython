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
 * This {@code enum} provides a set of structured constants that are used to
 * refer to the special methods of the Python data model.
 * <p>
 * These are structured constants that provide not only the {@code String}
 * method name, but also a signature, and much information used internally by
 * the run-time system in the creation of type objects, the interpretation of
 * code and the creation of call sites.
 * <p>
 * In principle, any Python object may support all of the special methods,
 * through "slots" in the Python type object {@code PyType}. These slots have
 * identical names to the corresponding constant in this {@code enum}. The
 * "slots" in the Python type object hold pointers ({@code MethodHandle}s) to
 * their implementations in Java for that type, which of course define the
 * behaviour of instances in Python. Where a special method is absent from the
 * implementation of a type, a default "empty" handle is provided from the
 * {@code Slot} constant.
 */
// Compare CPython struct wrapperbase in descrobject.h
// also typedef slotdef and slotdefs[] table in typeobject.h
enum Slot {

    op_repr(Signature.UNARY), //
    op_hash(Signature.LEN), //
    op_call(Signature.CALL), //
    op_str(Signature.UNARY), //

    op_getattribute(Signature.GETATTR), //
    op_getattr(Signature.GETATTR), //
    op_setattr(Signature.SETATTR), //
    op_delattr(Signature.DELATTR), //

    op_lt(Signature.BINARY), //
    op_le(Signature.BINARY), //
    op_eq(Signature.BINARY), //
    op_ne(Signature.BINARY), //
    op_ge(Signature.BINARY), //
    op_gt(Signature.BINARY), //

    op_iter(Signature.UNARY), //
    op_next(Signature.UNARY), //

    op_get(Signature.DESCRGET), //
    op_set(Signature.SETITEM), //
    op_delete(Signature.DELITEM), //

    op_init(Signature.INIT), //

    op_neg(Signature.UNARY, "unary -"), //
    op_pos(Signature.UNARY, "unary +"), //
    op_abs(Signature.UNARY, "abs()"), //
    op_invert(Signature.UNARY, "unary ~"), //

    // Binary ops: reflected form comes first so we can reference it.
    op_radd(Signature.BINARY, "+"), //
    op_rsub(Signature.BINARY, "-"), //
    op_rmul(Signature.BINARY, "*"), //
    op_rand(Signature.BINARY, "&"), //
    op_rxor(Signature.BINARY, "^"), //
    op_ror(Signature.BINARY, "|"), //

    op_add(Signature.BINARY, "+", op_radd), //
    op_sub(Signature.BINARY, "-", op_rsub), //
    op_mul(Signature.BINARY, "*", op_rmul), //
    op_and(Signature.BINARY, "&", op_rand), //
    op_xor(Signature.BINARY, "^", op_rxor), //
    op_or(Signature.BINARY, "|", op_ror), //

    /** Handle to {@code __bool__} with {@link Signature#PREDICATE} */
    op_bool(Signature.PREDICATE), //
    op_int(Signature.UNARY), //
    op_float(Signature.UNARY), //
    op_index(Signature.UNARY), //

    op_len(Signature.LEN), //

    op_contains(Signature.BINARY_PREDICATE), //

    op_getitem(Signature.BINARY), //
    op_setitem(Signature.SETITEM), //
    op_delitem(Signature.DELITEM);

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
     * Lookup by method name, returning {@code null} if it is not a recognised name
     * for any slot.
     *
     * @param name of a (possible) special method
     * @return the Slot corresponding, or {@code null}
     */
    public static Slot forMethodName(String name) { return Util.getMethodNameTable().get(name); }

    /**
     * Get the name of the method that, by convention, identifies the corresponding
     * operation in the implementing class. This is not the same as the slot name.
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
     * Get a handle to throw a {@link TypeError} with a message conventional for the
     * slot. This handle has the same signature as the slot, and some data specific
     * to the slot. This is useful when the target of a call site may have to raise
     * a type error.
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
     * Test whether this slot is non-empty in the given operations object.
     *
     * @param ops to examine for this slot
     * @return true iff defined (non-empty)
     */
    boolean isDefinedFor(Operations ops) { return slotHandle.get(ops) != signature.empty; }

    /**
     * Get the {@code MethodHandle} of this slot's operation from the given
     * operations object. Each member of this {@code enum} corresponds to a method
     * handle of the same name, which must also have the correct signature.
     *
     * @param ops target operations object
     * @return current contents of this slot in {@code ops}
     */
    MethodHandle getSlot(Operations ops) { return (MethodHandle) slotHandle.get(ops); }

    /**
     * Get the {@code MethodHandle} of this slot's "alternate" operation from the
     * given operations object. For a binary operation this is the reflected
     * operation.
     *
     * @param ops target operations object
     * @return current contents of the alternate slot in {@code t}
     * @throws NullPointerException if there is no alternate
     */
    MethodHandle getAltSlot(Operations ops) throws NullPointerException {
        return (MethodHandle) alt.slotHandle.get(ops);
    }

    /**
     * Set the {@code MethodHandle} of this slot's operation in the given operations
     * object.
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
     * An enumeration of the acceptable signatures for slots in a {@code PyType}.
     * For each {@code MethodHandle} we may place in a slot, we must know in advance
     * the acceptable signature ({@code MethodType}), and the slot when empty must
     * contain a handle with this signature to a method that will raise
     * {@link EmptyException}, Each {@code enum} constant here gives a symbolic name
     * to that {@code MethodType}, and provides an {@code empty} handle.
     * <p>
     * Names are equivalent to {@code typedef}s provided in CPython
     * {@code Include/object.h}, but not the same. We do not need quite the same
     * signatures as CPython: we do not return integer status, for example. Also,
     * C-specifics like {@code Py_ssize_t} are echoed in the C-API names but not
     * here.
     */
    enum Signature {

        /*
         * The makeDescriptor overrides returning anonymous sub-classes of
         * PyWrapperDescr are fairly ugly. However, sub-classes seem to be the right
         * solution, and defining them here keeps information together that belongs
         * together.
         */

        /**
         * The signature {@code (O)O}, for example {@link Slot#op_repr} or
         * {@link Slot#op_neg}.
         */
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
        BINARY(O, O, O) {

            @Override
            Object callWrapped(MethodHandle wrapped, Object self, Object[] args, String[] names)
                    throws ArgumentError, Throwable {
                checkArgs(args, 1, names);
                return wrapped.invokeExact(self, args[0]);
            }
        },
        /**
         * The signature {@code (O,O,O)O}.
         */
        TERNARY(O, O, O, O),

        /**
         * The signature {@code (O,O[],S[])O}, used for {@link Slot#op_call}. Note that
         * in Jython, standard calls are what CPython refers to as vector calls
         * (although they cannot use a stack slice as the array).
         */
        CALL(O, O, OA, SA) {

            @Override
            Object callWrapped(MethodHandle wrapped, Object self, Object[] args, String[] names)
                    throws ArgumentError, Throwable {
                return wrapped.invokeExact(self, args, names);
            }
        },

        // Slot#op_bool
        PREDICATE(B, O),

        // Slot#op_contains
        BINARY_PREDICATE(B, O, O),

        // Slot#op_length, Slot#op_hash
        LEN(I, O) {

            @Override
            Object callWrapped(MethodHandle wrapped, Object self, Object[] args, String[] names)
                    throws ArgumentError, Throwable {
                checkArgs(args, 0, names);
                return (int) wrapped.invokeExact(self);
            }
        },

        // (objobjargproc) Slot#op_setitem, Slot#op_set
        SETITEM(V, O, O, O),

        // (not in CPython) Slot#op_delitem, Slot#op_delete
        DELITEM(V, O, O),

        // (getattrofunc) Slot#op_getattr
        GETATTR(O, O, S) {

            @Override
            Object callWrapped(MethodHandle wrapped, Object self, Object[] args, String[] names)
                    throws ArgumentError, Throwable {
                checkArgs(args, 1, names);
                String name = args[0].toString();
                return wrapped.invokeExact(self, name);
            }
        },

        // (setattrofunc) Slot#op_setattr
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

        // (not in CPython) Slot#op_delattr
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

        // (descrgetfunc) Slot#op_get
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
                return wrapped.invokeExact(self, obj, (PyType) type);
            }
        },

        /**
         * The signature {@code (O,O,O[],S[])V}, used for {@link Slot#op_init}. This is
         * the same as {@link #CALL} except with {@code void} return.
         */
        // (initproc) Slot#op_init
        INIT(V, O, OA, SA);

        /**
         * The signature was defined with this nominal method type.
         */
        final MethodType type;
        /**
         * When empty, the slot should hold this handle. The method type of this handle
         * also tells us the method type by which the slot must always be invoked, see
         * {@link Slot#getType()}.
         */
        final MethodHandle empty;

        /**
         * Constructor to which we specify the signature of the slot, with the same
         * semantics as {@code MethodType.methodType()}. Every {@code MethodHandle}
         * stored in the slot (including {@link Signature#empty}) must be of this method
         * type.
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
         * Check that no positional or keyword arguments are supplied. This is for use
         * when implementing
         * {@link #callWrapped(MethodHandle, Object, Object[], String[])}.
         *
         * @param args positional argument array to be checked
         * @param names to be checked
         * @throws ArgumentError if positional arguments are given or {@code names} is
         *     not {@code null} or empty
         */
        final protected void checkNoArgs(Object[] args, String[] names) throws ArgumentError {
            if (args.length != 0)
                throw new ArgumentError(Mode.NOARGS);
            else if (names != null && names.length != 0)
                throw new ArgumentError(Mode.NOKWARGS);
        }

        /**
         * Check the number of positional arguments and that no keywords are supplied.
         * This is for use when implementing
         * {@link #callWrapped(MethodHandle, Object, Object[], String[])}.
         *
         * @param args positional argument tuple to be checked
         * @param expArgs expected number of positional arguments
         * @param names to be checked
         * @throws ArgumentError if the wrong number of positional arguments are given
         *     or {@code kwargs} is not {@code null} or empty
         */
        final protected void checkArgs(Object[] args, int expArgs, String[] names)
                throws ArgumentError {
            if (args.length != expArgs)
                throw new ArgumentError(expArgs);
            else if (names != null && names.length != 0)
                throw new ArgumentError(Mode.NOKWARGS);
        }

        /**
         * Check the number of positional arguments and that no keywords are supplied.
         * This is for use when implementing
         * {@link #callWrapped(MethodHandle, Object, Object[], String[])}.
         *
         * @param args positional argument tuple to be checked
         * @param minArgs minimum number of positional arguments
         * @param maxArgs maximum number of positional arguments
         * @param names to be checked
         * @throws ArgumentError if the wrong number of positional arguments are given
         *     or {@code kwargs} is not {@code null} or empty
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
         * Invoke the given method handle for the given target {@code self}, having
         * arranged the arguments as expected by a slot. We create {@code enum} members
         * of {@code Signature} to handle different slot signatures, in which this
         * method accepts arguments in a generic way (from the interpreter, say) and
         * adapts them to the specific needs of a wrapped method. The caller guarantees
         * that the wrapped method has the {@code Signature} to which the call is
         * addressed.
         *
         * @param wrapped handle of the method to call
         * @param self target object of the method call
         * @param args of the method call
         * @param names of trailing arguments in {@code args}
         * @return result of the method call
         * @throws ArgumentError when the arguments ({@code args}, {@code names}) are
         *     not correct for the {@code Signature}
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
     * Helper for {@link Slot#setHandle(PyType, MethodHandle)}, when a bad handle is
     * presented.
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
     * Helpers for {@link Slot} and {@link Signature} that can be used in the
     * constructors.
     */
    private static class Util {

        /*
         * This is a class separate from Slot to solve problems with the order of static
         * initialisation. The enum constants have to come first, and their constructors
         * are called as they are encountered. This means that other constants in Slot
         * are not initialised by the time the constructors need them.
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
         * Helper for {@link Slot} constructors at the point they need a handle for
         * their named field within an {@code Operations} class.
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
         * Helper for {@link Slot} and thereby for call sites providing a method handle
         * that throws a Python exception when invoked, with an appropriate message for
         * the operation.
         * <p>
         * To be concrete, if the slot is a binary operation, the returned handle may
         * throw something like {@code TypeError:
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
                 * Look up a method f to create the exception, when applied the arguments v, w,
                 * ... (types matching the slot signature) prepended with this slot. We'll only
                 * call it if the handle is invoked.
                 */
                // error = λ(slot, v, w, ...): f(slot, v, w, ...)
                MethodHandle error;
                switch (slot.signature) {
                case UNARY:
                    // Same name, although signature differs ...
                case BINARY:
                    error = LOOKUP.findStatic(Number.class, "operandError", errorMT);
                    break;
                default:
                    // error = λ(slot): default(slot, v, w, ...)
                    error = LOOKUP.findStatic(Util.class, "defaultOperandError", errorMT);
                    // error = λ(slot, v, w, ...): default(slot)
                    error = MethodHandles.dropArguments(error, 0, slot.getType().parameterArray());
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
