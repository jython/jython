// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static java.lang.invoke.MethodHandles.exactInvoker;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodHandles.foldArguments;
import static org.python.core.ClassShorthand.O;
import static org.python.core.ClassShorthand.T;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle.AccessMode;
import java.lang.invoke.WrongMethodTypeException;
import java.util.Map;
import java.util.WeakHashMap;

import org.python.base.InterpreterError;
import org.python.core.Slot.Signature;

/**
 * An {@code Operations} object provides behaviour to a Java object
 * by defining, for its Java class, a {@code MethodHandle} on the
 * implementation of each special method required by the
 * implementation and enumerated in {@link Slot}. (This is almost
 * the same as the set of special methods defined by the Python data
 * model.)
 * <p>
 * The run-time system will form a mapping from each Java class to
 * an instance of (a specific sub-class of) {@code Operations}.
 * Apart from a small collection of bootstrap classes (all of them
 * built-in types), this mapping will be developed as the classes
 * are encountered through the use of instances of them in Python
 * code.
 * <p>
 * In cases where the behaviour depends on the Python type as well
 * as the Java class, this is taken care of within the handle
 * embedded in the {@code Operations} object for the Java class.
 */
abstract class Operations {

    /**
     * The {@code Operations} object of sub-classes of built-in types.
     * The slots of this (singleton) redirect through those on the
     * PyType of the object instance.
     */
    static final Operations DERIVED = Derived.getInstance();

    /**
     * There is only one instance of this class and it is
     * {@link Operations#registry}.
     */
    private static class Registry extends ClassValue<Operations> {

        /**
         * Mapping from Java class to {@link Operations} object. This is the
         * map that backs this {@link Registry}. This map is protected from
         * concurrent modification by synchronising on the containing
         * {@code Registry} object. The keys are weak to allow classes to be
         * unloaded. (Concurrent GC is not a threat to the consistency of
         * the registry since a class we are working on cannot be unloaded.)
         */
        private final Map<Class<?>, Operations> opsMap = new WeakHashMap<>();

        /**
         * Post an association from a Java class to an {@code Operations}
         * object, that will be bound into {@link Operations#registry} when
         * a look-up is made.
         *
         * @param c Java class
         * @param ops operations to bind to the class
         * @throws Clash when the class is already mapped
         */
        synchronized void set(Class<?> c, Operations ops) throws Clash {
            Operations old = opsMap.putIfAbsent(c, ops);
            if (old != null) { throw new Clash(c, old); }
        }

        /**
         * Post an association from multiple Java classes to corresponding
         * {@code Operations} objects, that will be bound into
         * {@link Operations#registry} when look-ups are made.
         *
         * @param c Java class
         * @param ops operations to bind to the class
         * @throws Clash when one of the classes is already mapped
         */
        synchronized void set(Class<?>[] c, Operations[] ops) throws Clash {
            int i, n = c.length;
            for (i = 0; i < n; i++) {
                Operations old = opsMap.putIfAbsent(c[i], ops[i]);
                if (old != null) {
                    // We failed to insert c[i]: erase what we did
                    for (int j = 0; j < i; j++) { opsMap.remove(c[j]); }
                    throw new Clash(c[i], old);
                }
            }
        }

        /**
         * Find an operations object for the given class. There are five
         * broad cases. {@code c} might be:
         * <ol>
         * <li>the crafted canonical implementation of a Python type</li>
         * <li>an adopted implementation of some Python type</li>
         * <li>the implementation of the base of Python sub-classes of a
         * Python type</li>
         * <li>a found Java type</li>
         * <li>the crafted base of Python sub-classes of a found Java
         * type</li>
         * </ol>
         * Cases 1, 3 and 5 may be recognised by marker interfaces on
         * {@code c}. Case 2 may only be distinguished from case 4 only
         * because classes that are adopted implementations will have been
         * posted to {@link #opsMap} before the first call, when their
         * {@link PyType}s were created.
         */
        @Override
        protected synchronized Operations computeValue(Class<?> c) {

            /*
             * Operations.registry contained no mapping (as a ClassValue) for c
             * at the time this thread called get(). We will either find an
             * answer ready in opsMap, or construct one and post it there.
             *
             * It is possible that other threads have already passed through
             * get() and blocked behind this thread at the entrance to
             * computeValue(). This synchronisation guarantees that this thread
             * completes the critical section before another thread enters.
             *
             * Threads entering subsequently, and needing a binding for the same
             * class c, will therefore find the same value found or constructed
             * by this thread. Even if the second thread overtakes this one
             * after the protected region, and returns first, the class value
             * will bind that same Operations object.
             */

            /*
             * XXX There is more to say about re-entrancy (this thread) and
             * concurrency. This design does not mean that another thread, or
             * even the current one, has not already produced a competing
             * Operations objects to post.
             */

            Operations ops = opsMap.get(c);

            if (ops != null) {
                /*
                 * An answer already exists, for example because a PyType was built
                 * (cases 1 & 2), but is not yet in the registry class value: our
                 * return through get() will bind it there for future use.
                 */
                return ops;

            } else if (DerivedPyObject.class.isAssignableFrom(c)) {
                // Case 3, 5: one of the derived cases
                // Ensure c and super-classes statically initialised.
                ensureInit(c);
                // Always the same
                return Derived.getInstance();

            } else if (CraftedPyObject.class.isAssignableFrom(c)) {
                // Case 1: one of the crafted cases
                // Ensure c and super-classes statically initialised.
                ensureInit(c);
                // PyType posts results via Operations.register
                return findOps(c);

            } else {
                // Case 4: found Java type
                // XXX Stop gap. Needs specialised exposure.
                /*
                 * A Lookup object cannot be provided from here. Access to members
                 * of c will be determined by package and class at he point of use,
                 * in relation to c, according to Java rules. It follows that
                 * descriptors in the PyType cannot build method handles in advance
                 * of constructing the call site.
                 */
                PyType.Spec spec =
                        new PyType.Spec(c.getSimpleName(), MethodHandles.publicLookup().in(c));
                ops = PyType.fromSpec(spec);
                // Must post answer to opsMap ourselves?
                return ops;
            }
        }

        /**
         * Ensure a class is statically initialised. Static initialisation
         * will normally create a {@link PyType} and call
         * {@link #set(Class, Operations)} to post a result to
         * {@link #opsMap}.
         *
         * @param c to initialise
         */
        private static void ensureInit(Class<?> c) {
            String name = c.getName();
            try {
                Class.forName(name, true, c.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new InterpreterError("failed to initialise class %s", name);
            }
        }

        /**
         * Find the {@code Operations} object for this class, trying
         * super-classes. {@code c} must be an initialised class. If it
         * posted an {@link Operations} object for itself, it will be found
         * immediately. Otherwise the method tries successive super-classes
         * until one is found that has already been posted.
         *
         * @param c class to resolve
         * @return operations object for {@code c}
         */
        private Operations findOps(Class<?> c) {
            Operations ops;
            Class<?> prev;
            while ((ops = opsMap.get(prev = c)) == null) {
                // c has not been posted, but perhaps its superclass?
                c = prev.getSuperclass();
                if (c == null) {
                    // prev was Object, or primitive or an interface
                    throw new InterpreterError("no operations defined by class %s",
                            prev.getSimpleName());
                }
            }
            return ops;
        }
    }

    /**
     * Mapping from Java class to the {@code Operations} object that
     * provides instances of the class with Python semantics.
     */
    static final Registry registry = new Registry();

    /**
     * Register the {@link Operations} object for a Java class.
     * Subsequent enquiries through {@link #of(Object)} and
     * {@link #fromClass(Class)} will yield this {@code Operations}
     * object. This is a one-time action on the JVM-wide registry,
     * affecting the state of the {@code Class} object: the association
     * cannot be changed, but the {@code Operations} object may be
     * mutated (where it allows that). It is an error to attempt to
     * associate different {@code Operations} with a class already
     * bound.
     *
     * @param c class with which associated
     * @param ops the operations object
     * @throws Clash when the class is already mapped
     */
    static void register(Class<?> c, Operations ops) throws Clash {
        Operations.registry.set(c, ops);
    }

    /**
     * Register the {@link Operations} objects for multiple Java
     * classes, as with {@link #register(Class, Operations)}. All
     * succeed or fail together.
     *
     * @param c classes with which associated
     * @param ops the operations objects
     * @throws Clash when one of the classes is already mapped
     */
    static void register(Class<?>[] c, Operations ops[]) throws Clash {
        Operations.registry.set(c, ops);
    }

    /**
     * Map a Java class to the {@code Operations} object that provides
     * Python semantics to instances of the class.
     *
     * @param c class on which operations are required
     * @return {@code Operations} providing Python semantics
     */
    static Operations fromClass(Class<?> c) {
        // Normally, this is completely straightforward
        // TODO deal with re-entrancy and concurrency
        return registry.get(c);
    }

    /**
     * Map an object to the {@code Operations} object that provides it
     * with Python semantics.
     *
     * @param obj on which operations are required
     * @return {@code Operations} providing Python semantics
     */
    static Operations of(Object obj) { return fromClass(obj.getClass()); }

    /**
     * Get the Python type of the object <i>given that</i> this is the
     * operations object for it.
     *
     * @param x subject of the enquiry
     * @return {@code type(x)}
     */
    abstract PyType type(Object x);

    /**
     * Get the unique Python type for which this is operations object.
     * This is not always a meaningful enquiry: if this Operations
     * object is able to serve multiple types, an error will be thrown.
     *
     * @return type represented
     */
    abstract PyType uniqueType() throws IllegalArgumentException;

    /**
     * Identify by index which Java implementation of the associated
     * type this {@code Operations} object is for. (Some types have
     * multiple acceptable implementations.)
     *
     * @return index in the type (0 if canonical)
     */
    int getIndex() { return 0; }

    /**
     * Get the Java implementation class this {@code Operations} object
     * is for.
     *
     * @return class of the implementation
     */
    abstract Class<?> getJavaClass();

    /**
     * Fast check that the target is exactly a Python {@code int}. We
     * can do this without reference to the object itself, since it is
     * deducible from the Java class.
     *
     * @return target is exactly a Python {@code int}
     */
    boolean isIntExact() { return this == PyLong.TYPE; }

    /**
     * Fast check that the target is a data descriptor.
     *
     * @return target is a data descriptor
     */
    boolean isDataDescr() { return false; }

    // ---------------------------------------------------------------

    /**
     * Operations for an accepted implementation (non-canonical
     * implementation) are represented by an instance of this class. The
     * operations of a canonical implementation are represented by the
     * {@link PyType} itself.
     */
    static class Accepted extends Operations {

        /** The type of which this is an accepted implementation. */
        final private PyType type;

        /**
         * Index of this implementation in the type (see
         * {@link PyType#indexAccepted(Class)}.
         */
        final private int index;

        /**
         * Create an operations object that is the {@code n}th
         * implementation of the given type. ({@code n>0} since the
         * implementation 0 is represented by the type itself.)
         *
         * @param type of which this is an accepted implementation
         * @param n index of this implementation in the type
         */
        Accepted(PyType type, int n) {
            this.type = type;
            this.index = n;
            setAllSlots();
        }

        @Override
        PyType type(Object x) { return type; }

        @Override
        PyType uniqueType() { return type; }

        @Override
        int getIndex() { return index; }

        @Override
        Class<?> getJavaClass() { return type.classes[index]; }

        /**
         * Set all the slots ({@code op_*}) from the entries in the
         * dictionaries of this type and its bases.
         */
        private void setAllSlots() {
            for (Slot s : Slot.values()) {
                Object def = type.lookup(s.methodName);
                s.setDefinition(this, def);
            }
        }

        @Override
        public String toString() {
            String javaName = getJavaClass().getSimpleName();
            return javaName + " as " + type.toString();
        }
    }

    /**
     * Operations for a Python class defined in Python are represented
     * by an instance of this class. Many Python classes may be
     * implemented by the same Java class, the actual type being The
     * canonical implementation is represented by the {@link PyType}
     * itself.
     */
    static class Derived extends Operations {

        /**
         * {@code MethodHandle} of type {@code (DerivedPyObject)PyType}, to
         * get the actual Python type of a {@link DerivedPyObject} object.
         */
        private static final MethodHandle getType;
        /**
         * The type {@code (PyType)MethodHandle} used to cast the method
         * handle getter in {@link #indirectSlot(Slot)}.
         */
        private static final MethodType MT_MH_FROM_TYPE;

        /** Rights to form method handles. */
        private static final Lookup LOOKUP = MethodHandles.lookup();

        static {
            try {
                // Used as a cast in the formation of getMHfromType
                // (PyType)MethodHandle
                MT_MH_FROM_TYPE = MethodType.methodType(MethodHandle.class, T);
                // Used as a cast in the formation of getType
                // (PyType)MethodHandle
                // getType = λ x : x.getType()
                // .type() = (Object)PyType
                getType = LOOKUP
                        .findVirtual(CraftedPyObject.class, "getType", MethodType.methodType(T))
                        .asType(MethodType.methodType(T, O));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new InterpreterError(e, "preparing handles in Operations.Derived");
            }
        }

        /**
         * Return a handle of the correct type for the slot, but that
         * indirects through the type object of the first argument.
         *
         * @param s
         * @return
         */
        private static MethodHandle indirectSlot(Slot s) {
            /*
             * We form a method handle that can take any Object, and if it is a
             * CraftedPyObject, navigate to its type object, and pick out the
             * method handle from Slot s.
             */
            // getOpFromType = λ t : s.getSlot(t)
            // .type() = (PyType)MethodHandle
            MethodHandle getMHfromType =
                    s.slotHandle.toMethodHandle(AccessMode.GET).asType(MT_MH_FROM_TYPE);
            // getMHfromObj = λ x : s.getSlot(x.getType())
            // .type() = (CraftedPyObject)MethodHandle
            MethodHandle getMHfromObj = filterReturnValue(getType, getMHfromType);
            /*
             * We create an exact invoker, that can take a handle with the
             * correct signature for Slot s, and invoke it on the corresponding
             * arguments.
             */
            // invoker = λ h x ... : h(x, ...)
            MethodType mt = s.signature.empty.type();
            MethodHandle invoker = exactInvoker(mt);
            /*
             * Finally we compose the invoker with getMHfromType, to make a new
             * handle, with the correct signature for Slot s, that when invoked
             * itself, indirects through the corresponding handle in the type
             * object.
             */
            // λ x ... : (s.getSlot(x.getType(x)) (x, ...)
            return foldArguments(invoker, getMHfromObj);
        }

        private static final Derived instance = new Derived();

        static Derived getInstance() { return instance; }

        /**
         * Create an operations object that is the implementation of
         * potentially many types defined in Python.
         */
        Derived() { setAllSlots(); }

        @Override
        PyType type(Object x) {
            if (x instanceof DerivedPyObject)
                return ((DerivedPyObject)x).getType();
            else
                throw new InterpreterError("object %.50s has wrong Operations type %s", x,
                        getClass().getSimpleName());
        }

        @Override
        PyType uniqueType() {
            throw new IllegalArgumentException("Python type not uniquely defined by Operations");
        }

        @Override
        Class<?> getJavaClass() { return null; }

        /**
         * Set all the slots ({@code op_*}) to entries that will interrogate
         * the actual type of their target object.
         */
        private void setAllSlots() {
            for (Slot s : Slot.values()) { s.setHandle(this, indirectSlot(s)); }
        }

        @Override
        public String toString() { return "Derived"; }
    }

    /**
     * A table of binary operations that may be indexed by a pair of
     * classes (or their {@code Operations} objects). Binary operations,
     * at the same time as appearing as the {@code op} and {@code rop}
     * slots, meaning for example {@link Operations#op_add} and
     * {@link Operations#op_radd}, are optionally given implementations
     * specialised for the Java classes of their arguments. A
     * {@code BinopGrid} describes the
     */
    static class BinopGrid {

        /** Handle that marks an empty binary operation slot. */
        protected static final MethodHandle BINARY_EMPTY = Slot.Signature.BINARY.empty;

        /** The (binary) slot for which this is an operation. */
        final Slot slot;
        /** the type on which we find this implemented. */
        final PyType type;
        /** All the implementations, arrayed by argument class. */
        final MethodHandle[][] mh;

        /**
         * Construct a grid for the given operation and type.
         *
         * @param slot of the binary operation
         * @param type in which the definition is being made
         */
        BinopGrid(Slot slot, PyType type) {
            assert slot.signature == Signature.BINARY;
            this.slot = slot;
            this.type = type;
            final int N = type.acceptedCount;
            final int M = type.classes.length;
            this.mh = new MethodHandle[N][M];
        }

        /**
         * Post the definition for the {@link #slot} applicable to the
         * classes in the method type. The handle must be the "raw" handle
         * to the class-specific implementation, while the posted value
         * (later returned by {@link #get(Class, Class)} will have the
         * signature {@link Signature#BINARY}.
         *
         * @param mh handle to post
         */
        void add(MethodHandle mh) throws WrongMethodTypeException, InterpreterError {
            MethodType mt = mh.type();
            // Cast fails if the signature is incorrect for the slot
            mh = mh.asType(slot.getType());
            // Find cell based on argument types
            int i = type.indexAccepted(mt.parameterType(0));
            int j = type.indexOperand(mt.parameterType(1));
            if (i >= 0 && j >= 0) {
                this.mh[i][j] = mh;
            } else {
                /*
                 * The arguments to m are not (respectively) an accepted class and
                 * an operand class for the type. Type spec and the declared binary
                 * ops disagree?
                 */
                throw new InterpreterError("unexpected signature of %s.%s: %s", type.name,
                        slot.methodName, mt);
            }
        }

        /**
         * Check that every valid combination of classes has been added
         * (therefore leads to a non-null method handle).
         *
         * @throws InterpreterError if a {@code null} was found
         */
        void checkFilled() throws InterpreterError {
            final int N = type.acceptedCount;
            final int M = type.classes.length;
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    if (mh[i][j] == null) {
                        /*
                         * There's a gap in the table. Type spec and the declared binary ops
                         * disagree?
                         */
                        throw new InterpreterError("binary op not defined: %s(%s, %s)",
                                slot.methodName, type.classes[i].getSimpleName(),
                                type.classes[j].getSimpleName());
                    }
                }
            }
        }

        /**
         * Get the method handle of an implementation
         * {@code Object op(V v, W w)} specialised to the given classes. If
         * {@code V} is an accepted implementation of this type, and
         * {@code W} is an operand class, the return will be a handle on an
         * implementation accepting those classes. If no implementation is
         * available for those classes (which means they are not accepted
         * and operand types for the Python type) an empty slot handle is
         * returned.
         *
         * @param accepted class of first argument to method
         * @param operand class of second argument to method
         * @return the special-to-class binary operation
         */
        MethodHandle get(Class<?> accepted, Class<?> operand) {
            // Find cell based on argument types
            int i = type.indexAccepted(accepted);
            int j = type.indexOperand(operand);
            if (i >= 0 && j >= 0) {
                return mh[i][j];
            } else {
                return BINARY_EMPTY;
            }
        }

        /**
         * Convenience method allowing look-up equivalent to
         * {@link #get(Class, Class)}, but using the {@code Operations}
         * objects as a proxy for the actual classes.
         *
         * @param accepted class of first argument to method
         * @param operand class of second argument to method
         * @return the special-to-class binary operation
         */
        MethodHandle get(Operations accepted, Operations operand) {
            return get(accepted.getJavaClass(), operand.getJavaClass());
        }
    }

    /**
     * Exception reporting that an attempt was made to register a second
     * {@link Operations} object against a class already in the
     * registry.
     */
    static class Clash extends Exception {
        private static final long serialVersionUID = 1L;
        /** Class being redefined. */
        final Class<?> klass;
        /**
         * The operations object already in the registry for {@link #klass}
         */
        final Operations existing;

        Clash(Class<?> klass, Operations existing) {
            // super("repeat type/operations definition for %s", klass);
            this.klass = klass;
            this.existing = existing;
        }

        @Override
        public String getMessage() {
            return String.format("repeat type/operations definition for %s", klass);
        }
    }

    // ---------------------------------------------------------------

    // Cache of the standard type slots. See CPython PyType.
    // Same order as enum Slot (although only names are important).

    /**
     * Handle on special method {@code __repr__}, described by
     * {@link Slot#op_repr}.
     */
    MethodHandle op_repr;
    /**
     * Handle on special method {@code __hash__}, described by
     * {@link Slot#op_hash}.
     */
    MethodHandle op_hash;
    /**
     * Handle on special method {@code __call__}, described by
     * {@link Slot#op_call}.
     */
    MethodHandle op_call;
    /**
     * Handle on special method {@code __str__}, described by
     * {@link Slot#op_str}.
     */
    MethodHandle op_str;

    /**
     * Handle on special method {@code __getattribute__}, described by
     * {@link Slot#op_getattribute}.
     */
    MethodHandle op_getattribute;
    /**
     * Handle on special method {@code __getattr__}, described by
     * {@link Slot#op_getattr}.
     */
    MethodHandle op_getattr;
    /**
     * Handle on special method {@code __setattr__}, described by
     * {@link Slot#op_setattr}.
     */
    MethodHandle op_setattr;
    /**
     * Handle on special method {@code __delattr__}, described by
     * {@link Slot#op_delattr}.
     */
    MethodHandle op_delattr;

    /**
     * Handle on special method {@code __lt__}, described by
     * {@link Slot#op_lt}.
     */
    MethodHandle op_lt;
    /**
     * Handle on special method {@code __le__}, described by
     * {@link Slot#op_le}.
     */
    MethodHandle op_le;
    /**
     * Handle on special method {@code __eq__}, described by
     * {@link Slot#op_eq}.
     */
    MethodHandle op_eq;
    /**
     * Handle on special method {@code __ne__}, described by
     * {@link Slot#op_ne}.
     */
    MethodHandle op_ne;
    /**
     * Handle on special method {@code __gt__}, described by
     * {@link Slot#op_gt}.
     */
    MethodHandle op_gt;
    /**
     * Handle on special method {@code __ge__}, described by
     * {@link Slot#op_ge}.
     */
    MethodHandle op_ge;

    /**
     * Handle on special method {@code __iter__}, described by
     * {@link Slot#op_iter}.
     */
    MethodHandle op_iter;
    /**
     * Handle on special method {@code __next__}, described by
     * {@link Slot#op_next}.
     */
    MethodHandle op_next;

    /**
     * Handle on special method {@code __get__}, described by
     * {@link Slot#op_get}.
     */
    MethodHandle op_get;
    /**
     * Handle on special method {@code __set__}, described by
     * {@link Slot#op_set}.
     */
    MethodHandle op_set;
    /**
     * Handle on special method {@code __delete__}, described by
     * {@link Slot#op_delete}.
     */
    MethodHandle op_delete;

    /**
     * Handle on special method {@code __init__}, described by
     * {@link Slot#op_init}.
     */
    MethodHandle op_init;

    /**
     * Handle on special method {@code __await__}, described by
     * {@link Slot#op_await}.
     */
    MethodHandle op_await;
    /**
     * Handle on special method {@code __aiter__}, described by
     * {@link Slot#op_aiter}.
     */
    MethodHandle op_aiter;
    /**
     * Handle on special method {@code __anext__}, described by
     * {@link Slot#op_anext}.
     */
    MethodHandle op_anext;

    /**
     * Handle on special method {@code __radd__}, described by
     * {@link Slot#op_radd}.
     */
    MethodHandle op_radd;
    /**
     * Handle on special method {@code __rsub__}, described by
     * {@link Slot#op_rsub}.
     */
    MethodHandle op_rsub;
    /**
     * Handle on special method {@code __rmul__}, described by
     * {@link Slot#op_rmul}.
     */
    MethodHandle op_rmul;
    /**
     * Handle on special method {@code __rmod__}, described by
     * {@link Slot#op_rmod}.
     */
    MethodHandle op_rmod;
    /**
     * Handle on special method {@code __rdivmod__}, described by
     * {@link Slot#op_rdivmod}.
     */
    MethodHandle op_rdivmod;
    /**
     * Handle on special method {@code __rpow__}, described by
     * {@link Slot#op_rpow}.
     */
    MethodHandle op_rpow;
    /**
     * Handle on special method {@code __rlshift__}, described by
     * {@link Slot#op_rlshift}.
     */
    MethodHandle op_rlshift;
    /**
     * Handle on special method {@code __rrshift__}, described by
     * {@link Slot#op_rrshift}.
     */
    MethodHandle op_rrshift;
    /**
     * Handle on special method {@code __rand__}, described by
     * {@link Slot#op_rand}.
     */
    MethodHandle op_rand;
    /**
     * Handle on special method {@code __rxor__}, described by
     * {@link Slot#op_rxor}.
     */
    MethodHandle op_rxor;
    /**
     * Handle on special method {@code __ror__}, described by
     * {@link Slot#op_ror}.
     */
    MethodHandle op_ror;
    /**
     * Handle on special method {@code __rfloordiv__}, described by
     * {@link Slot#op_rfloordiv}.
     */
    MethodHandle op_rfloordiv;
    /**
     * Handle on special method {@code __rtruediv__}, described by
     * {@link Slot#op_rtruediv}.
     */
    MethodHandle op_rtruediv;
    /**
     * Handle on special method {@code __rmatmul__}, described by
     * {@link Slot#op_rmatmul}.
     */
    MethodHandle op_rmatmul;

    /**
     * Handle on special method {@code __add__}, described by
     * {@link Slot#op_add}.
     */
    MethodHandle op_add;
    /**
     * Handle on special method {@code __sub__}, described by
     * {@link Slot#op_sub}.
     */
    MethodHandle op_sub;
    /**
     * Handle on special method {@code __mul__}, described by
     * {@link Slot#op_mul}.
     */
    MethodHandle op_mul;
    /**
     * Handle on special method {@code __mod__}, described by
     * {@link Slot#op_mod}.
     */
    MethodHandle op_mod;
    /**
     * Handle on special method {@code __divmod__}, described by
     * {@link Slot#op_divmod}.
     */
    MethodHandle op_divmod;
    /**
     * Handle on special method {@code __pow__}, described by
     * {@link Slot#op_pow}.
     */
    MethodHandle op_pow;

    /**
     * Handle on special method {@code __neg__}, described by
     * {@link Slot#op_neg}.
     */
    MethodHandle op_neg;
    /**
     * Handle on special method {@code __pos__}, described by
     * {@link Slot#op_pos}.
     */
    MethodHandle op_pos;
    /**
     * Handle on special method {@code __abs__}, described by
     * {@link Slot#op_abs}.
     */
    MethodHandle op_abs;
    /**
     * Handle on special method {@code __invert__}, described by
     * {@link Slot#op_invert}.
     */
    MethodHandle op_invert;

    /**
     * Handle on special method {@code __bool__}, described by
     * {@link Slot#op_bool}.
     */
    MethodHandle op_bool;

    /**
     * Handle on special method {@code __lshift__}, described by
     * {@link Slot#op_lshift}.
     */
    MethodHandle op_lshift;
    /**
     * Handle on special method {@code __rshift__}, described by
     * {@link Slot#op_rshift}.
     */
    MethodHandle op_rshift;
    /**
     * Handle on special method {@code __and__}, described by
     * {@link Slot#op_and}.
     */
    MethodHandle op_and;
    /**
     * Handle on special method {@code __xor__}, described by
     * {@link Slot#op_xor}.
     */
    MethodHandle op_xor;
    /**
     * Handle on special method {@code __or__}, described by
     * {@link Slot#op_or}.
     */
    MethodHandle op_or;

    /**
     * Handle on special method {@code __int__}, described by
     * {@link Slot#op_int}.
     */
    MethodHandle op_int;
    /**
     * Handle on special method {@code __float__}, described by
     * {@link Slot#op_float}.
     */
    MethodHandle op_float;

    /**
     * Handle on special method {@code __iadd__}, described by
     * {@link Slot#op_iadd}.
     */
    MethodHandle op_iadd;
    /**
     * Handle on special method {@code __isub__}, described by
     * {@link Slot#op_isub}.
     */
    MethodHandle op_isub;
    /**
     * Handle on special method {@code __imul__}, described by
     * {@link Slot#op_imul}.
     */
    MethodHandle op_imul;
    /**
     * Handle on special method {@code __imod__}, described by
     * {@link Slot#op_imod}.
     */
    MethodHandle op_imod;
    /**
     * Handle on special method {@code __iand__}, described by
     * {@link Slot#op_iand}.
     */
    MethodHandle op_iand;
    /**
     * Handle on special method {@code __ixor__}, described by
     * {@link Slot#op_ixor}.
     */
    MethodHandle op_ixor;
    /**
     * Handle on special method {@code __ior__}, described by
     * {@link Slot#op_ior}.
     */
    MethodHandle op_ior;

    /**
     * Handle on special method {@code __floordiv__}, described by
     * {@link Slot#op_floordiv}.
     */
    MethodHandle op_floordiv;
    /**
     * Handle on special method {@code __truediv__}, described by
     * {@link Slot#op_truediv}.
     */
    MethodHandle op_truediv;
    /**
     * Handle on special method {@code __ifloordiv__}, described by
     * {@link Slot#op_ifloordiv}.
     */
    MethodHandle op_ifloordiv;
    /**
     * Handle on special method {@code __itruediv__}, described by
     * {@link Slot#op_itruediv}.
     */
    MethodHandle op_itruediv;

    /**
     * Handle on special method {@code __index__}, described by
     * {@link Slot#op_index}.
     */
    MethodHandle op_index;

    /**
     * Handle on special method {@code __matmul__}, described by
     * {@link Slot#op_matmul}.
     */
    MethodHandle op_matmul;
    /**
     * Handle on special method {@code __imatmul__}, described by
     * {@link Slot#op_imatmul}.
     */
    MethodHandle op_imatmul;

    /**
     * Handle on special method {@code __len__}, described by
     * {@link Slot#op_len}.
     */
    MethodHandle op_len;
    /**
     * Handle on special method {@code __getitem__}, described by
     * {@link Slot#op_getitem}.
     */
    MethodHandle op_getitem;
    /**
     * Handle on special method {@code __setitem__}, described by
     * {@link Slot#op_setitem}.
     */
    MethodHandle op_setitem;
    /**
     * Handle on special method {@code __delitem__}, described by
     * {@link Slot#op_delitem}.
     */
    MethodHandle op_delitem;
    /**
     * Handle on special method {@code __contains__}, described by
     * {@link Slot#op_contains}.
     */
    MethodHandle op_contains;
}
