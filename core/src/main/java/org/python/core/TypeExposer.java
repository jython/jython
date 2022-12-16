// Copyright (c)2022 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.python.base.InterpreterError;
import org.python.core.Exposed.Deleter;
import org.python.core.Exposed.DocString;
import org.python.core.Exposed.Getter;
import org.python.core.Exposed.Member;
import org.python.core.Exposed.PythonMethod;
import org.python.core.Exposed.PythonStaticMethod;
import org.python.core.Exposed.Setter;
import org.python.core.Operations.BinopGrid;
import org.python.core.PyMemberDescr.Flag;
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
     * The table of intermediate descriptions for members. They will
     * eventually become descriptors in a built-in object type. Every
     * entry here is also a value in {@link Exposer#specs}.
     */
    final Set<MemberSpec> memberSpecs;

    /**
     * The table of intermediate descriptions for get-sets. They will
     * eventually become descriptors in a built-in object type. Every
     * entry here is also a value in {@link Exposer#specs}.
     */
    final Set<GetSetSpec> getSetSpecs;

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
    TypeExposer(PyType type) {
        this.type = type;
        this.memberSpecs = new TreeSet<>();
        this.getSetSpecs = new TreeSet<>();
    }

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
        // ... and for fields.
        scanJavaFields(definingClass);
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
            spec.checkFormation();
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
    @Override
    void scanJavaMethods(Class<?> defsClass) throws InterpreterError {

        // Iterate over methods looking for those to expose
        for (Class<?> c : superClasses(defsClass)) {
            for (Method m : c.getDeclaredMethods()) {
                /*
                 * Note: method annotations (and special names) are not treated as
                 * alternatives, to catch exposure of methods by multiple routes.
                 */

                // Check for instance method
                PythonMethod pm = m.getDeclaredAnnotation(PythonMethod.class);
                if (pm != null) { addMethodSpec(m, pm); }

                // Check for static method
                PythonStaticMethod psm = m.getDeclaredAnnotation(PythonStaticMethod.class);
                if (psm != null) { addStaticMethodSpec(m, psm); }

                // Check for getter, setter, deleter methods
                Getter get = m.getAnnotation(Getter.class);
                if (get != null) { addGetter(m, get); }
                Setter set = m.getAnnotation(Setter.class);
                if (set != null) { addSetter(m, set); }
                Deleter del = m.getAnnotation(Deleter.class);
                if (del != null) { addDeleter(m, del); }

                // If it has a special method name record that definition.
                String name = m.getName();
                Slot slot = Slot.forMethodName(name);
                if (slot != null) { addWrapperSpec(m, slot); }
            }
        }
    }

    /**
     * Process a method annotated as an exposed attribute get method,
     * into a specification, and find a {@link GetSetSpec} to the table
     * of specifications by name (or add one) to hold it.
     *
     * @param m method annotated
     * @param anno annotation encountered
     * @throws InterpreterError on duplicates or unsupported types
     */
    private void addGetter(Method m, Getter anno) {
        addSpec(m, anno.value(), TypeExposer::castGetSet, GetSetSpec::new,
                ms -> getSetSpecs.add(ms), GetSetSpec::addGetter);
    }

    /**
     * Process a method annotated as an exposed attribute set method,
     * into a specification, and find a {@link GetSetSpec} to the table
     * of specifications by name (or add one) to hold it.
     *
     *
     * @param m method annotated
     * @param anno annotation encountered
     * @throws InterpreterError on duplicates or unsupported types
     */
    private void addSetter(Method m, Setter anno) {
        addSpec(m, anno.value(), TypeExposer::castGetSet, GetSetSpec::new,
                ms -> getSetSpecs.add(ms), GetSetSpec::addSetter);
    }

    /**
     * Process a method annotated as an exposed attribute get method,
     * into a specification, and find a {@link GetSetSpec} to the table
     * of specifications by name (or add one) to hold it.
     *
     *
     * @param m method annotated
     * @param anno annotation encountered
     * @throws InterpreterError on duplicates or unsupported types
     */
    private void addDeleter(Method m, Deleter anno) {
        addSpec(m, anno.value(), TypeExposer::castGetSet, GetSetSpec::new,
                ms -> getSetSpecs.add(ms), GetSetSpec::addDeleter);
    }

    /**
     * Cast an arbitrary {@link Spec} to a {@link GetSetSpec} or return
     * {@code null}.
     *
     * @param spec to cast
     * @return {@code spec} or {@code null}
     */
    private static GetSetSpec castGetSet(Spec spec) {
        return spec instanceof GetSetSpec ? (GetSetSpec)spec : null;
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

    /**
     * Add to {@link #specs}, definitions of fields found in the given
     * class and annotated for exposure.
     *
     * @param defsClass to introspect for field definitions
     * @throws InterpreterError on duplicates or unsupported types
     */
    void scanJavaFields(Class<?> defsClass) throws InterpreterError {
        // Iterate over fields looking for the relevant annotations
        for (Class<?> c : superClasses(defsClass)) {
            for (Field f : c.getDeclaredFields()) {
                Member m = f.getDeclaredAnnotation(Member.class);
                if (m != null) { addMemberSpec(f, m); }
            }
        }
    }

    /**
     * Process an annotated field, that describes an exposed attribute,
     * into a specification, and add it to the table of specifications
     * by name.
     *
     * @param f field annotated
     * @param anno annotation encountered
     * @throws InterpreterError on duplicates or unsupported types
     */
    void addMemberSpec(Field f, Member anno) throws InterpreterError {

        // The name is as annotated or the "natural" one
        String name = anno.value();
        if (name == null || name.length() == 0)
            name = f.getName();

        /*
         * XXX we follow the same pattern as with other spec types, in
         * accumulating multiple definitions in a list. Repeat definition is
         * almost certainly an error, and at this time, MemberSpec.add
         * treats it as such. This makes Member annotations incompatible
         * with the idea of multiple accepted implementations of a type.
         */
        // Find any existing definition
        Spec spec = specs.get(name);
        MemberSpec memberSpec;
        if (spec == null) {
            // A new entry is needed
            memberSpec = new MemberSpec(name);
            specs.put(memberSpec.name, memberSpec);
            memberSpecs.add(memberSpec);
        } else if (spec instanceof MemberSpec) {
            // Existing entry will be updated
            memberSpec = (MemberSpec)spec;
        } else {
            // Existing entry is not compatible
            memberSpec = new MemberSpec(name);
            throw duplicateError(name, f, memberSpec, spec);
        }
        // Add the field, processing the additional properties
        memberSpec.add(f, anno.optional(), anno.readonly());
    }

    @Override
    public String toString() { return "TypeExposer [type=" + type + "]"; }

    /**
     * A specialisation of {@link Exposer.Spec} to describe a named,
     * built-in data-like object, during the exposure process.
     */
    static class MemberSpec extends Exposer.Spec {

        /** Collects the fields declared (should be just one). */
        final List<Field> fields;

        /**
         * The member disappears when the field is {@code null}. This is
         * always {@code false} for primitive types.
         */
        boolean optional;
        /** The member may be read but not written or deleted. */
        boolean readonly;

        MemberSpec(String name) {
            super(name, ScopeKind.TYPE);
            this.fields = new ArrayList<>(1);
        }

        /**
         * Add a field implementing this member to the collection.
         *
         * @param field to add to {@link #fields}
         * @param optional member is optional
         * @param readonly member is read only
         */
        void add(Field field, boolean optional, boolean readonly) {

            // Final overrides readonly=false
            int modifiers = field.getModifiers();
            readonly |= (modifiers & Modifier.FINAL) != 0;

            // Disallow static (in Java)
            boolean javaStatic = (modifiers & Modifier.STATIC) != 0;
            if (javaStatic) { throw new InterpreterError(CANNOT_BE_JAVA_STATIC, getJavaName()); }

            // Disallow optional if primitive (in Java)
            if (optional) {
                if (field.getType().isPrimitive()) {
                    throw new InterpreterError(CANNOT_BE_OPTIONAL, "Primitive", getJavaName());
                } else if (readonly) {
                    throw new InterpreterError(CANNOT_BE_OPTIONAL, "Read-only", getJavaName());
                }
            }

            // Add the only definition (do we actually need a list?)
            fields.add(field);
            if (fields.size() != 1) { throw duplicateError(name, field, this, this); }

            // Finally insert the allowed combination
            this.optional = optional;
            this.readonly = readonly;

            // There may be a @DocString annotation
            DocString docAnno = field.getAnnotation(DocString.class);
            if (docAnno != null) { doc = docAnno.value(); }
        }

        private static final String CANNOT_BE_JAVA_STATIC =
                "The definition of '%s' cannot be Java static because it is a Python member";
        private static final String CANNOT_BE_OPTIONAL = "%s field '%s' cannot be optional";

        @Override
        Class<? extends Annotation> annoClass() { return Member.class; }

        /**
         * {@inheritDoc}
         * <p>
         * In a type, the attribute must be represented by a descriptor for
         * the Python member attribute from this specification.
         * <p>
         * Note that specification may have collected multiple Java
         * definitions of the same name. This method checks there is exactly
         * one.
         *
         * @return descriptor for access to the methods
         * @throws InterpreterError if the method type is not supported
         */
        @Override
        PyMemberDescr asAttribute(PyType objclass, Lookup lookup) {
            EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
            if (readonly) { flags.add(Flag.READONLY); }
            if (optional) { flags.add(Flag.OPTIONAL); }
            return PyMemberDescr.forField(objclass, name, fields.get(0), lookup, flags, doc);
        }

        @Override
        public void checkFormation() throws InterpreterError {}

        /** @return a name designating the field */
        @Override
        String getJavaName() {
            StringBuilder b = new StringBuilder(64);
            if (!fields.isEmpty()) {
                // It shouldn't matter, but take the last added
                Field field = fields.get(fields.size() - 1);
                b.append(field.getDeclaringClass().getSimpleName());
                b.append('.');
                b.append(field.getName());
            } else {
                // Take the name from the Spec instead
                b.append(name);
            }
            return b.toString();
        }

        /** @return the Java declaration for the field */
        String getJavaDeclaration() {
            StringBuilder b = new StringBuilder(64);
            if (!fields.isEmpty()) {
                // It shouldn't matter, but take the last added
                Field field = fields.get(fields.size() - 1);
                b.append(field.getType().getSimpleName());
                b.append(' ');
                b.append(field.getName());
            }
            return b.toString();
        }

        @Override
        public String toString() {
            return String.format("%s(%s [%s])", getClass().getSimpleName(), name,
                    getJavaDeclaration());
        }
    }

    /**
     * A specialisation of {@link Exposer.Spec} to describe a named,
     * built-in data-like object, during the exposure process.
     */
    static class GetSetSpec extends BaseMethodSpec {

        /** Collects the getters declared (often just one). */
        final List<Method> getters;
        /** Collects the setters declared (often just one). */
        final List<Method> setters;
        /** Collects the deleters declared (often just one). */
        final List<Method> deleters;
        /** Java class of attribute from setter parameter. */
        Class<?> klass = Object.class;

        GetSetSpec(String name) {
            super(name, ScopeKind.TYPE);
            this.getters = methods;
            this.setters = new ArrayList<>(1);
            this.deleters = new ArrayList<>(1);
        }

        /**
         * The attribute may not be set or deleted.
         *
         * @return true if set and delete are absent
         */
        boolean readonly() { return setters.isEmpty() && deleters.isEmpty(); }

        /**
         * The attribute may be deleted.
         *
         * @return true if delete is present
         */
        boolean optional() { return !deleters.isEmpty(); }

        /**
         * Add a getter to the collection.
         *
         * @param method to add to {@link #getters}
         */
        void addGetter(Method method) {
            // Add to list of methods
            getters.add(method);
            // There may be a @DocString annotation
            maybeAddDoc(method);
        }

        /**
         * Add a setter to the collection.
         *
         * @param method to add to {@link #setters}
         */
        void addSetter(Method method) {
            // Add to list of methods
            setters.add(method);
            // There may be a @DocString annotation
            maybeAddDoc(method);
            // Process parameters of the Setter
            determineAttrType(method);
        }

        /**
         * Add a deleter to the collection.
         *
         * @param method to add to {@link #deleters}
         */
        void addDeleter(Method method) {
            // Add to list of methods
            deleters.add(method);
            // There may be a @DocString annotation
            maybeAddDoc(method);
        }

        /**
         * Deduce the attribute type from the (raw) set method signature. We
         * do this in order to give a sensible {@link TypeError} when a cast
         * fails for the {@link PyGetSetDescr#__set__} operation.
         *
         * @param method annotated with a {@code Setter}
         */
        private void determineAttrType(Method method) {
            // Save class of value accepted (if signature is sensible)
            int modifiers = method.getModifiers();
            int v = (modifiers & Modifier.STATIC) != 0 ? 1 : 0;
            Class<?>[] paramClasses = method.getParameterTypes();
            if (paramClasses.length == v + 1) {
                Class<?> valueClass = paramClasses[v];
                if (valueClass == klass) {
                    // No change
                } else if (klass.isAssignableFrom(valueClass)) {
                    // The parameter is more specific than klass
                    klass = valueClass;
                }
            }
        }

        @Override
        Object asAttribute(PyType objclass, Lookup lookup) throws InterpreterError {
            if (objclass.acceptedCount == 1)
                return createDescrSingle(objclass, lookup);
            else
                return createDescrMultiple(objclass, lookup);
        }

        @Override
        public void checkFormation() throws InterpreterError {}

        private Object createDescrSingle(PyType objclass, Lookup lookup) {
            // TODO Stop-gap: do general case first
            return createDescrMultiple(objclass, lookup);
        }

        /**
         * Create a {@code PyGetSetDescr} from this specification. Note that
         * a specification collects all the methods as declared with this
         * name (in separate getter, setter and deleter lists). Normally
         * there is at most one of each.
         * <p>
         * Normally also, a Python type has just one Java implementation. If
         * a type has N accepted implementations, there should be
         * definitions of the getter, setter, and deleter methods, if
         * defined at all, applicable to each accepted implementation. This
         * method matches defined methods to the supported implementations.
         *
         * @param objclass Python type that owns the descriptor
         * @param lookup authorisation to access fields
         * @return descriptor for access to the field
         * @throws InterpreterError if the method type is not supported
         */
        private PyGetSetDescr createDescrMultiple(PyType objclass, Lookup lookup)
                throws InterpreterError {

            // Handles on implementation methods
            MethodHandle[] g, s = null, d = null;
            g = unreflect(objclass, lookup, PyGetSetDescr.GETTER, getters);
            if (!readonly()) {
                // We can set this attribute
                s = unreflect(objclass, lookup, PyGetSetDescr.SETTER, setters);
                if (optional()) {
                    // We can delete this attribute
                    d = unreflect(objclass, lookup, PyGetSetDescr.DELETER, deleters);
                }
            }

            return new PyGetSetDescr.Multiple(objclass, name, g, s, d, doc, klass);
        }

        private MethodHandle[] unreflect(PyType objclass, Lookup lookup, MethodType mt,
                List<Method> methods) throws InterpreterError {

            /*
             * In the first stage, translate each method to a handle. There
             * could be any number of candidates in the defining classes. There
             * may be a method for each accepted implementation of the type , or
             * a method may match more than one (e.g. Number matching Long and
             * Integer). We build a list with the more type-specific handles (in
             * the first argument) before the less type-specific.
             */
            LinkedList<MethodHandle> candidates = new LinkedList<>();
            for (Method m : methods) {
                // Convert m to a handle (if L args and accessible)
                try {
                    MethodHandle mh = lookup.unreflect(m);
                    addOrdered(candidates, mh);
                } catch (IllegalAccessException e) {
                    throw cannotGetHandle(m, e);
                }
            }

            /*
             *
             * We will try to create a handle for each implementation of an
             * instance method.
             */
            final int N = objclass.acceptedCount;
            MethodHandle[] method = new MethodHandle[N];

            // Fill the method array with matching method handles
            for (int i = 0; i < N; i++) {
                Class<?> acceptedClass = objclass.classes[i];
                /*
                 * Fill method[i] with the method handle where the first parameter
                 * is the most specific match for class accepted[i].
                 */
                // Try the candidate method until one matches
                for (MethodHandle mh : candidates) {
                    MethodType mt1 = mh.type();
                    if (mt1.parameterType(0).isAssignableFrom(acceptedClass)) {
                        /*
                         * Each sub-type of MethodDef handles callMethod(self, args, kwargs)
                         * in its own way, and must prepare the arguments of the generic
                         * method handle to match.
                         */
                        try {
                            // XXX not yet supporting Java args
                            method[i] = mh.asType(mt);
                        } catch (WrongMethodTypeException wmte) {
                            // Wrong number of args or cannot cast.
                            throw methodSignatureError(objclass, mh);
                        }
                        break;
                    }
                }

                // We should have a value in each of method[]
                if (method[i] == null) {
                    PyGetSetDescr.Type dt = PyGetSetDescr.Type.fromMethodType(mt);
                    throw new InterpreterError(ATTR_NOT_IMPL, dt, name, objclass.name,
                            objclass.classes[i]);
                }
            }

            /*
             * There are multiple definitions so use the array form of built-in
             * method. This is the case for types that have multiple accepted
             * implementations and methods on them that are not static or
             * "Object self".
             */
            return method;
        }

        private static String ATTR_NOT_IMPL =
                "%s of attribute '%s' of '%s' objects is not defined for implementation %s";

        @Override
        Class<? extends Annotation> annoClass() {
            // Try annotations in order of popularity
            if (getters.size() > 0)
                ; // -> Getter
            else if (setters.size() > 0)
                return Setter.class;
            else if (deleters.size() > 0)
                return Deleter.class;
            // Or by default, claim to have a Getter
            return Getter.class;
        }

        @Override
        public String toString() {
            return String.format("%s(%s[%d,%d,%d])", getClass().getSimpleName(), name,
                    getters.size(), setters.size(), deleters.size());
        }
    }

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
        public void checkFormation() throws InterpreterError {
            // XXX Check the signature instead of in createDescr?
        }

        @Override
        void add(Method method) { super.add(method); }

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
            assert L >= 1;

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
            if (!ok) { throw new WrongMethodTypeException(); }
            for (int i = 0; i < n; i++) {
                ok = slotType.parameterType(i).isAssignableFrom(mt.parameterType(i));
                if (!ok) { throw new WrongMethodTypeException(); }
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
        for (BinopGrid grid : defs.values()) { grid.checkFilled(); }

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
