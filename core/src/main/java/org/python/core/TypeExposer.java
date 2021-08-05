// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;

import org.python.base.InterpreterError;
import org.python.core.Operations.BinopGrid;
import org.python.core.Slot.Signature;

class TypeExposer extends Exposer {

    /**
     * Type for which attributes are to be exposed (or {@code null}
     * during certain tests). It is referenced (e.g. where we create a
     * descriptor), but is not otherwise accessed, since it is
     * (necessarily) incomplete at this time.
     */
    final PyType type;

    /**
     * Construct the {@code TypeExposer} instance for a particular
     * Python type. The {@code type} object is referenced (e.g. in
     * intermediate specification objects), but is not otherwise
     * accessed, since it is (necessarily) incomplete at this time. It
     * will be interrogated as to its implementing classes, where we
     * create descriptors, at the point {@link #expose(Class)} is
     * called.
     *
     * @param type being exposed
     */
    TypeExposer(PyType type) { this.type = type; }

    @Override
    ScopeKind kind() { return ScopeKind.TYPE; }

    /**
     * Build the result from the defining class.
     *
     * @param definingClass to scan for definitions
     */
    void expose(Class<?> definingClass) {
        // Scan the defining class for exposed and special methods
        scanJavaMethods(definingClass);
    }

    /**
     * For each name having a definition in {@link #specs}, construct
     * the attribute and add it to the map passed in. The map is
     * normally the dictionary of the type. Attributes may rely on a
     * {@code MethodHandle} or {@code VarHandle}, so a lookup object
     * must be provided that can create them.
     *
     * @param dict to which the attributes should be delivered
     * @param lookup authorisation to access members
     */
    void populate(Map<? super String, Object> dict, Lookup lookup) {
        if (type == null)
            // type may only properly be null during certain tests
            throw new InterpreterError("Cannot generate descriptors for type 'null'");
        for (Spec spec : specs.values()) {
            Object attr = spec.asAttribute(type, lookup);
            dict.put(spec.name, attr);
        }
    }

    /**
     * Add to {@link #specs}, definitions based on methods found in the
     * given class and either annotated for exposure or having the name
     * of a special method.
     *
     * @param defsClass to introspect for methods
     * @throws InterpreterError on duplicates or unsupported types
     */
    void scanJavaMethods(Class<?> defsClass) throws InterpreterError {

        // Iterate over methods looking for those to expose
        for (Method m : defsClass.getDeclaredMethods()) {
            // If it has a special method name record that definition.
            String name = m.getName();
            Slot slot = Slot.forMethodName(name);
            if (slot != null) {
                addWrapperSpec(m, slot);
            }
        }
    }

    /**
     * Process a method that matches a slot name to a descriptor
     * specification and add it to the table of specifications by name.
     *
     * @param meth method annotated
     * @param slot annotation encountered
     * @throws InterpreterError on duplicates or unsupported types
     */
    private void addWrapperSpec(Method meth, Slot slot) throws InterpreterError {

        // For clarity, name lambda expression for cast
        Function<Spec, WrapperSpec> cast =
                // Test and cast a found Spec to MethodSpec
                spec -> spec instanceof WrapperSpec ? (WrapperSpec)spec : null;
        // Now use the generic create/update
        addSpec(meth, slot.methodName, cast, (String ignored) -> new WrapperSpec(slot), ms -> {},
                WrapperSpec::add);
    }

    @Override
    public String toString() { return "TypeExposer [type=" + type + "]"; }

    /**
     * Specification in which we assemble information about a Python
     * special method in advance of creating a special method
     * descriptor.
     */
    static class WrapperSpec extends BaseMethodSpec {

        /** The special method being defined. */
        final Slot slot;

        WrapperSpec(Slot slot) {
            super(slot.methodName, ScopeKind.TYPE);
            this.slot = slot;
        }

        @Override
        Object asAttribute(PyType objclass, Lookup lookup) throws InterpreterError {
            /*
             * We will try to create a handle for each implementation of a
             * special (instance) method. See corresponding logic in
             * Slot.setSlot(Operations, Object)
             */
            return createDescrForInstanceMethod(objclass, lookup);
        }

        @Override
        void add(Method method) {
            super.add(method);
            // XXX Check the signature instead of in createDescr?
        }

        @Override
        Class<? extends Annotation> annoClass() {
            // Special methods recognised by name, so no annotation
            return Annotation.class;
        }

        /**
         * {@inheritDoc}
         * <p>
         * In this case, we name the slot function, as there is no
         * annotation.
         */
        @Override
        protected String annoClassName() { return slot.toString(); }

        /**
         * Create a {@code PyWrapperDescr} from this specification. Note
         * that a specification describes the methods as declared, and that
         * there may be any number. This method matches them to the
         * supported implementations.
         *
         * @param objclass Python type that owns the descriptor
         * @param lookup authorisation to access fields
         * @return descriptor for access to the field
         * @throws InterpreterError if the method type is not supported
         */
        private PyWrapperDescr createDescrForInstanceMethod(PyType objclass, Lookup lookup)
                throws InterpreterError {

            // Acceptable methods can be coerced to this signature
            MethodType slotType = slot.getType();
            final int L = slotType.parameterCount();
            assert (L >= 1);

            /*
             * There could be any number of candidates in the implementation. An
             * implementation method could match multiple accepted
             * implementations of the type (e.g. Number matching Long and
             * Integer).
             */
            LinkedList<MethodHandle> candidates = new LinkedList<>();
            for (Method m : methods) {
                // Convert m to a handle (if L args and accessible)
                try {
                    MethodHandle mh = lookup.unreflect(m);
                    if (mh.type().parameterCount() == L)
                        addOrdered(candidates, mh);
                } catch (IllegalAccessException e) {
                    throw cannotGetHandle(m, e);
                }
            }

            /*
             * We will try to create a handle for each implementation of an
             * instance method, but only one handle for static/class methods
             * (like __new__). See corresponding logic in
             * Slot.setSlot(Operations, Object)
             */
            final int N = objclass.acceptedCount;
            MethodHandle[] wrapped = new MethodHandle[N];

            // Fill the wrapped array with matching method handles
            for (int i = 0; i < N; i++) {
                Class<?> acceptedClass = objclass.classes[i];
                /*
                 * Fill wrapped[i] with the method handle where the first parameter
                 * is the most specific match for class accepted[i].
                 */
                // Try the candidate method until one matches
                for (MethodHandle mh : candidates) {
                    if (mh.type().parameterType(0).isAssignableFrom(acceptedClass)) {
                        try {
                            // must have the expected signature
                            checkCast(mh, slotType);
                            wrapped[i] = mh.asType(slotType);
                            break;
                        } catch (WrongMethodTypeException wmte) {
                            // Wrong number of args or cannot cast.
                            throw methodSignatureError(objclass, mh);
                        }
                    }
                }

                // We should have a value in each of wrapped[]
                if (wrapped[i] == null) {
                    throw new InterpreterError("'%s.%s' not defined for %s", objclass.name,
                            slot.methodName, objclass.classes[i]);
                }
            }

            if (N == 1)
                /*
                 * There is only one definition so use the simpler form of
                 * slot-wrapper. This is the frequent case.
                 */
                return new PyWrapperDescr.Single(objclass, slot, wrapped[0]);
            else
                /*
                 * There are multiple definitions so use the array form of
                 * slot-wrapper. This is the case for types that have multiple
                 * accepted implementations and methods on them that are not static
                 * or "Object self".
                 */
                return new PyWrapperDescr.Multiple(objclass, slot, wrapped);
        }

        /**
         * Throw a {@code WrongMethodTypeException} if the offered method
         * (e.g. a special method) cannot be called with arguments matching
         * the specified type. This makes up for the fact that
         * {@code MethodHandle.asType} does not do much checking. This way,
         * we get an error at specification time, not run-time.
         *
         * @param mh handle of method offered
         * @param slotType required type
         * @throws WrongMethodTypeException if cannot cast
         */
        private static void checkCast(MethodHandle mh, MethodType slotType)
                throws WrongMethodTypeException {
            MethodType mt = mh.type();
            int n = mt.parameterCount();
            if (n != slotType.parameterCount())
                throw new WrongMethodTypeException();
            boolean ok = slotType.returnType().isAssignableFrom(mt.returnType());
            if (!ok) {
                throw new WrongMethodTypeException();
            }
            for (int i = 0; i < n; i++) {
                ok = slotType.parameterType(i).isAssignableFrom(mt.parameterType(i));
                if (!ok) {
                    throw new WrongMethodTypeException();
                }
            }
        }
    }

    /**
     * Create a table of {@code MethodHandle}s from binary operations
     * defined in the given class, on behalf of the type given. This
     * table is 3-dimensional, being indexed by the slot of the method
     * being defined, which must be a binary operation, and the indices
     * of the operand classes in the type. These handles are used
     * privately by the type to create call sites. Although the process
     * of creating them is similar to making wrapper descriptors, these
     * structures do not become exposed as descriptors.
     *
     * @param lookup authorisation to access methods
     * @param binops to introspect for binary operations
     * @param type to which these descriptors apply
     * @return attributes defined (in the order first encountered)
     * @throws InterpreterError on duplicates or unsupported types
     */
    static Map<Slot, BinopGrid> binopTable(Lookup lookup, Class<?> binops, PyType type)
            throws InterpreterError {

        // Iterate over methods looking for the relevant annotations
        Map<Slot, BinopGrid> defs = new HashMap<>();

        for (Method m : binops.getDeclaredMethods()) {
            // If it is a special method, record the definition.
            String name = m.getName();
            Slot slot = Slot.forMethodName(name);
            if (slot != null && slot.signature == Signature.BINARY) {
                binopTableAdd(defs, slot, m, lookup, binops, type);
            }
        }

        // Check for nulls in the table.
        for (BinopGrid grid : defs.values()) {
            grid.checkFilled();
        }

        return defs;
    }

    /**
     * Add a method handle to the table, verifying that the method type
     * produced is compatible with the {@link #slot}.
     *
     * @param defs the method table to add to
     * @param slot being matched
     * @param m implementing method
     * @param lookup authorisation to access fields
     * @param binops class defining class-specific binary operations
     * @param type to which these belong
     */
    private static void binopTableAdd(Map<Slot, BinopGrid> defs, Slot slot, Method m, Lookup lookup,
            Class<?> binops, PyType type) {

        // Get (or create) the table for this slot
        BinopGrid def = defs.get(slot);
        if (def == null) {
            // A new special method has been encountered
            def = new BinopGrid(slot, type);
            defs.put(slot, def);
        }

        try {
            // Convert the method to a handle
            def.add(lookup.unreflect(m));
        } catch (IllegalAccessException | WrongMethodTypeException e) {
            throw new InterpreterError(e, "ill-formed or inaccessible binary op '%s'", m);
        }
    }

}
