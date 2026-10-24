// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.python.core.Callables;
import org.python.core.PyDict;
import org.python.core.PyFloat;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.internal.Util;
import org.python.subclass.SubclassFactory;
import org.python.subclass.SubclassSpec;
import org.python.types.Feature;
import org.python.types.TypeSpec;
import org.python.types.WithClassAssignment;
import org.python.types.WithDict;
import org.python.types.WithDictAssignment;

/**
 * This is a test of a process behind class definition in Python, which
 * is founded on {@link SubclassSpec} and {@link SubclassFactory}. We
 * create a specification, as we might when interpreting a class
 * definition, and generate a Java class in byte code that will
 * represent instances of the new Python class. We then compare
 * properties of the generated class with a hand-crafted prototype.
 * <p>
 * The new Python class will be a subclass (in Python) of all the bases,
 * and will have {@code __dict__} or {@code __slots__} (or both).
 * However, we also avoid making identical classes. The new Java
 * representation class may only be a subclass (in Java) of one base. It
 * follows that the bases are only suitable if their representations lie
 * on a single inheritance hierarchy.
 * <p>
 * The test mainly checks that correct classes are created, and can be
 * found again when an identical need arises (i.e. a layout-compatible
 * Python class is defined).
 */
class SubclassCreationTest {

    // FIXME add test for subclass of subclass defined as in Python
    /*
     * SOLUTION IMPLEMENTED: in the subclass factory, we make an additional
     * cache entry to say that anything with the new class as base and
     * exactly the same managed dict/class has the same representation. This
     * turns up in BaseType.__new__.
     *
     * We do not test type.__new__ here, but lack a test for the kind of use
     * is makes of SubclassSpec.
     */

    /**
     * We create Java representations of classes directly, using only the
     * code generation aspects of defining a subclass. This is activity that
     * occurs when a class is defined in Python, using the {@code class}
     * keyword or the {@code type()} constructor (with 3 arguments).
     * <p>
     * This allows us to test code generation, while we simulate (in
     * {@link BareJavaRepresentation#bareExamples()}) actions that
     * {@code type.__new__} and the type factory should carry out when not
     * broken. Our reference result is one of the classes in this file named
     * {@code HCD_*} or {@code HCS_*}, where we express in Java, the code
     * equivalent to that which should be generated for the examples.
     * <p>
     * See {@link DynamicBase} for a parallel test going via capabilities in
     * the type object.
     */
    @Nested
    @DisplayName("A synthetic Java representation  ...")
    class BareJavaRepresentation {
        /**
         * Factory for the tests. Although this couples the tests through a
         * single factory, it contributes to the test to have it used multiple
         * times.
         */
        static final SubclassFactory factory = new SubclassFactory("BARE$%s$%d");

        /**
         * Provide a stream of examples as parameter sets to the tests.
         *
         * @return the examples for representation tests.
         */
        static Stream<Arguments> bareExamples() {
            return Stream.of(//
                    // No bases no slots
                    bareExample(HCD_O.class, "O"), //
                    // Simple base, no slots
                    bareExample(HCD_F.class, "F", PyFloat.TYPE), //
                    // No bases, slots
                    bareExample(HCS_Oa.class, "Oa", List.of(), List.of("a")), //
                    // Simple base, no dict, slots
                    bareExample(HCS_Fabc.class, "Fabc", List.of(PyFloat.TYPE),
                            List.of("c", "a", "b")), //
                    // Meta-type, inherit dict, no slots
                    bareExample(HCD_T.class, "T", List.of(PyType.TYPE()), null) //
            );
        }

        /**
         * Construct parameters for a test of subclass creation, with the
         * implied Python base {@code object}, no {@code __slots__} and an
         * instance dictionary. The reference result is expressed through a
         * hand-crafted class.
         *
         * @param refClass the reference result
         * @param name of the Python type to create
         * @return parameters for the test
         */
        private static Arguments bareExample(Class<?> refClass, String name) {
            return bareExample(refClass, name, List.of());
        }

        /**
         * Construct parameters for a test of subclass creation, with a single
         * Python base, {@code __slots__} and no instance dictionary. The
         * reference result is expressed through a hand-crafted class.
         *
         * @param refClass the reference result
         * @param name of the Python type to create
         * @param base (single) of the Python type or {@code null}
         * @param slots {@code __slots__} or {@code null}
         * @return parameters for the test
         */
        private static Arguments bareExample(Class<?> refClass, String name, PyType base,
                List<String> slots) {
            if (base == null) { base = PyObject.TYPE; }
            return bareExample(refClass, name, List.of(base), slots);
        }

        /**
         * Construct parameters for a test of subclass creation, with a single
         * Python base, no {@code __slots__} and an instance dictionary. The
         * reference result is expressed through a hand-crafted class.
         *
         * @param refClass the reference result
         * @param name of the Python type to create
         * @param base (single) of the Python type or {@code null}
         * @return parameters for the test
         */
        private static Arguments bareExample(Class<?> refClass, String name, PyType base) {
            if (base == null) { base = PyObject.TYPE; }
            return bareExample(refClass, name, List.of(base), null);
        }

        /**
         * Construct parameters for a test of subclass creation, with Python
         * bases, no {@code __slots__} and an instance dictionary. The reference
         * result is expressed through a hand-crafted class.
         *
         * @param refClass the reference result
         * @param name of the Python type to create
         * @param bases of the Python type (empty means {@code object})
         * @return parameters for the test
         */
        private static Arguments bareExample(Class<?> refClass, String name, List<PyType> bases) {
            return bareExample(refClass, name, bases, null);
        }

        /**
         * Construct parameters for a test of subclass creation, with Python
         * bases, optionally with {@code __slots__}, optionally with an instance
         * dictionary. The reference result is expressed through a hand-crafted
         * class.
         *
         * @param refClass the reference result
         * @param name of the Python type to create
         * @param addDict whether to add a dictionary
         * @param slots {@code __slots__} or {@code null}
         * @return parameters for the test
         */
        private static Arguments bareExample(Class<?> refClass, String name, List<PyType> bases,
                List<String> slots) {

            // Make a string title "class name(b0, b1, b2):"
            String title = generateClassDeclaration(name, bases, slots);

            /*
             * Find the most-derived representations of all the bases, i.e. b's
             * canonical class is assignable from baseClass for all b, and the
             * interfaces any of them asks. We're not as thorough here as we must be
             * in PyType, just aiming to catch mistakes building a test.
             */
            Set<Class<?>> interfaces = new HashSet<>();
            PyType baseType = findBaseClass(interfaces, bases, title);

            // Create a specification
            SubclassSpec spec = new SubclassSpec(name, baseType.canonicalClass());
            spec.addInterfaces(interfaces);
            if (slots != null) { spec.addSlots(slots); }

            return arguments(refClass, title.toString(), spec, name);
        }

        /**
         * We verify that when two specifications are equal, we do not create a
         * second the subclass representation. This is essential for the
         * operation of class assignment to be possible between sufficiently
         * similar types.
         *
         * @param refClass a local class like the one we need
         * @param title roughly equivalent Python class definition
         * @param spec we are trying to satisfy.
         */
        @DisplayName("is created without duplication")
        @ParameterizedTest(name = "when defined by {1}")
        @MethodSource("bareExamples")
        void withoutDuplication(Class<?> refClass, String title, SubclassSpec spec) {
            /*
             * When run as a single test in the IDE, this first call to
             * findOrCreateSubclass will create the class, and the second call
             * should return that class from the cache. When run as a batch, this
             * may not be the first test, so one of the other tests may already have
             * created the class and both calls will return the class from the
             * cache. This does not invalidate the test.
             */
            // Get a Java subclass representation for refspec
            SubclassSpec refspec = spec.clone();
            assertEquals(spec, refspec, "clones not equal");
            Lookup reflu = factory.findOrCreateSubclass(refspec);

            // Get a Java subclass representation for spec
            Lookup lu = factory.findOrCreateSubclass(spec);
            // It should be the same as for refspec
            assertSame(reflu, lu);
        }

        @DisplayName("has the expected superclass and modifiers")
        @ParameterizedTest(name = "when defined by {1}")
        @MethodSource("bareExamples")
        void createRep(Class<?> refClass, String title, SubclassSpec spec) {
            // Get subclass representation
            Lookup lu = factory.findOrCreateSubclass(spec);
            Class<?> c = lu.lookupClass();

            // c should have the same super class as the reference.
            Class<?> refSuper = refClass.getSuperclass();
            Class<?> cSuper = c.getSuperclass();
            assertSame(refSuper, cSuper);

            // c should have the same modifier bits as the reference
            int refModifiers = refClass.getModifiers();
            // except the reference class is static.
            refModifiers &= ~Modifier.STATIC;
            assertModifiersMatch(c.getName(), refModifiers, c.getModifiers());
        }

        @DisplayName("declares the expected fields")
        @ParameterizedTest(name = "when defined by {1}")
        @MethodSource("bareExamples")
        void expectedFields(Class<?> refClass, String title, SubclassSpec spec) {
            // Get subclass representation
            Lookup lu = factory.findOrCreateSubclass(spec);
            Class<?> c = lu.lookupClass();

            // Collect the fields declared by the reference.
            Map<String, Field> fields = new HashMap<>();
            for (Field f : refClass.getDeclaredFields()) { fields.put(f.getName(), f); }

            // c should have the same fields as the reference.
            List<Field> extras = new LinkedList<>();
            for (Field f : c.getDeclaredFields()) {
                // Compare the same-name reference field
                String name = f.getName();
                Field ref = fields.get(name);
                if (ref != null) {
                    assertSame(ref.getType(), f.getType(), name + ": type");
                    assertModifiersMatch(name, ref.getModifiers(), f.getModifiers());
                    fields.remove(name);
                } else {
                    extras.add(f);
                }
            }

            // What is left in fields was missing from c.
            if (!fields.isEmpty()) { fail(String.format("missing field(s): %s", fields.keySet())); }
            // What landed in extras was unexpected in c.
            if (!extras.isEmpty()) { fail(String.format("unexpected field(s): %s", extras)); }
        }

        @DisplayName("has the expected interfaces")
        @ParameterizedTest(name = "when defined by {1}")
        @MethodSource("bareExamples")
        void expectedInterfaces(Class<?> refClass, String title, SubclassSpec spec) {
            // Get subclass representation
            Lookup lu = factory.findOrCreateSubclass(spec);
            Class<?> c = lu.lookupClass();

            // Collect the interfaces declared by the reference.
            Map<String, Class<?>> interfaces = new HashMap<>();
            for (Class<?> i : refClass.getInterfaces()) { interfaces.put(i.getName(), i); }

            // c should have the same interfaces as the reference.
            Map<String, Class<?>> extras = new HashMap<>();
            for (Class<?> i : c.getInterfaces()) {
                // Compare the same-name reference interface
                String name = i.getName();
                Class<?> ref = interfaces.get(name);
                if (ref != null) {
                    assertSame(ref, i);
                    interfaces.remove(name);
                } else {
                    extras.put(name, i);
                }
            }

            // What is left in interfaces was missing from c.
            if (!interfaces.isEmpty()) {
                fail(String.format("missing interface(s): %s", interfaces.keySet()));
            }
            // What landed in extras was unexpected in c.
            if (!extras.isEmpty()) {
                fail(String.format("unexpected interface(s): %s", extras.keySet()));
            }
        }

        @DisplayName("declares the expected methods")
        @ParameterizedTest(name = "when defined by {1}")
        @MethodSource("bareExamples")
        void expectedMethods(Class<?> refClass, String title, SubclassSpec spec) {
            // Get subclass representation
            Lookup lu = factory.findOrCreateSubclass(spec);
            Class<?> c = lu.lookupClass();

            // Collect the methods declared by the reference.
            Map<String, Method> methods = new HashMap<>();
            for (Method m : refClass.getDeclaredMethods()) {
                // Assumes we wrote only one method with each name.
                if ((m.getModifiers() & SYNTHETIC) == 0) {
                    // Not a synthetic or bridge method.
                    String name = m.getName();
                    Method previous = methods.put(name, m);
                    assertNull(previous, name);
                }
            }

            // c should have the same methods as the reference.
            List<Method> extras = new LinkedList<>();
            for (Method m : c.getDeclaredMethods()) {
                // Compare the same-name reference methods
                // Assumes there is only one method with each name.
                String name = m.getName();
                Method ref = methods.get(name);
                if (ref != null) {
                    assertSame(ref.getReturnType(), m.getReturnType(), name + ": return type");
                    assertModifiersMatch(name, ref.getModifiers(), m.getModifiers());
                    // Parameters should match in number and type.
                    Class<?>[] mp = m.getParameterTypes();
                    Class<?>[] rp = ref.getParameterTypes();
                    int n = rp.length;
                    assertEquals(n, mp.length, name + ": parameters");
                    for (int i = 0; i < n; i++) { assertSame(rp[i], mp[i], name + ": param " + i); }
                    methods.remove(name);
                } else {
                    extras.add(m);
                }
            }

            // What is left in methods was missing from c.
            if (!methods.isEmpty()) {
                fail(String.format("missing method(s): %s", methods.keySet()));
            }
            // What landed in extras was unexpected in c.
            if (!extras.isEmpty()) {
                fail(String.format("unexpected method(s): %s", methods.keySet()));
            }
        }
    }

    /**
     * We create new types and instances of their Java representations going
     * via capabilities in the type object, to supply test cases with a
     * specific base.
     */
    static class DynamicBase {
        /**
         * {@link SubclassFactory} to use in the tests. Although this couples
         * the tests through a single factory, using multiple times implicitly
         * tests the caching and re-use of representations with the same
         * specification.
         */
        static final SubclassFactory factory = new SubclassFactory("DYNAMIC$%s$%d");

        /**
         * Construct parameters for a test of subclass creation and
         * instantiation,
         *
         * with the implied Python base {@code object},
         *
         * no {@code __slots__},
         *
         * an instance dictionary,
         *
         * and no arguments to the constructor.
         *
         * The reference result is expressed through a hand-crafted class.
         *
         * @param refClass the reference result
         * @param name of the Python type to create
         * @return parameters for the test
         */
        protected static Arguments dynamicExample(Class<?> refClass, String name) {
            return dynamicExample(refClass, name, List.of());
        }

        /**
         * Construct parameters for a test of subclass creation,
         *
         * with a single Python base,
         *
         * {@code __slots__},
         *
         * no instance dictionary,
         *
         * and no arguments to the constructor.
         *
         * The reference result is expressed through a hand-crafted class.
         *
         * @param refClass the reference result
         * @param name of the Python type to create
         * @param base (single) of the Python type or {@code null}
         * @param slots {@code __slots__} or {@code null}
         * @return parameters for the test
         */
        protected static Arguments dynamicExample(Class<?> refClass, String name, PyType base,
                List<String> slots) {
            if (base == null) { base = PyObject.TYPE; }
            return dynamicExample(refClass, name, List.of(base), slots);
        }

        /**
         * Construct parameters for a test of subclass creation,
         *
         * with a single Python base,
         *
         * no {@code __slots__},
         *
         * an instance dictionary,
         *
         * and no arguments to the constructor.
         *
         * The reference result is expressed through a hand-crafted class.
         *
         * @param refClass the reference result
         * @param name of the Python type to create
         * @param base (single) of the Python type or {@code null}
         * @return parameters for the test
         */
        protected static Arguments dynamicExample(Class<?> refClass, String name, PyType base) {
            if (base == null) { base = PyObject.TYPE; }
            return dynamicExample(refClass, name, List.of(base), null);
        }

        /**
         * Construct parameters for a test of subclass creation,
         *
         * with multiple Python bases,
         *
         * no {@code __slots__},
         *
         * an instance dictionary,
         *
         * and no arguments to the constructor.
         *
         * The reference result is expressed through a hand-crafted class.
         *
         * @param refClass the reference result
         * @param name of the Python type to create
         * @param bases of the Python type (empty means {@code object})
         * @return parameters for the test
         */
        protected static Arguments dynamicExample(Class<?> refClass, String name,
                List<PyType> bases) {
            return dynamicExample(refClass, name, bases, null, Util.EMPTY_ARRAY,
                    Util.EMPTY_STRING_ARRAY);
        }

        /**
         * Construct parameters for a test of subclass creation,
         *
         * with Python bases,
         *
         * {@code __slots__},
         *
         * no instance dictionary,
         *
         * and no arguments to the constructor.
         *
         * The reference result is expressed through a hand-crafted class.
         *
         * @param refClass the reference result
         * @param name of the Python type to create
         * @param bases of the Python type (empty means {@code object})
         * @return parameters for the test
         */
        protected static Arguments dynamicExample(Class<?> refClass, String name,
                List<PyType> bases, List<String> slots) {
            return dynamicExample(refClass, name, bases, slots, Util.EMPTY_ARRAY,
                    Util.EMPTY_STRING_ARRAY);
        }

        /**
         * Construct parameters for a test of subclass creation,
         *
         * with Python bases,
         *
         * either {@code __slots__}
         *
         * or an instance dictionary,
         *
         * and classic arguments to the constructor.
         *
         * The reference result is expressed through a hand-crafted class.
         *
         * @param refClass the reference result
         * @param name of the Python type to create
         * @param bases of the Python type (empty means {@code object})
         * @param slots {@code __slots__} or {@code null}
         * @return parameters for the test
         */
        protected static Arguments dynamicExample(Class<?> refClass, String name,
                List<PyType> bases, List<String> slots, Object[] args, String[] kwds) {

            // Make a string title "class name(b0, b1, b2):"
            String title = generateClassDeclaration(name, bases, slots);

            // XXX Shouldn't this be automatic?
            Set<Class<?>> interfaces = new HashSet<>();
            // interfaces.add(WithClassAssignment.class);

            /*
             * Find the most-derived representations of all the bases, i.e. b's
             * canonical class is assignable from baseClass for all b, and the
             * interfaces any of them asks. We're not as thorough here as we must be
             * in __build_class__, just aiming to catch mistakes building a test.
             */
            // XXX This should be in PyType or __build_class__
            PyType baseType = findBaseClass(interfaces, bases, title);

            // Create a specification from baseType Java information
            SubclassSpec spec = new SubclassSpec(name, baseType.javaClass());
            // spec.addInterfaces(interfaces);
            if (slots != null) { spec.addSlots(slots); }
            spec.addConstructors(baseType);

            // Get subclass representation
            Lookup lu = factory.findOrCreateSubclass(spec);
            PyType type = PyType.fromSpec(new TypeSpec(name, lu).add(Feature.REPLACEABLE)
                    .bases(bases.toArray(new PyType[bases.size()])));

            return arguments(refClass, title.toString(), type, args, kwds);
        }
    }

    /**
     * We create new types and instances of their Java representations going
     * via capabilities in the type object.
     */
    @Nested
    @DisplayName("A Python subclass of object ...")
    class DynamicObjectSubclass extends DynamicBase {
        /**
         * Provide a stream of examples as parameter sets to the tests.
         *
         * @return the examples for dynamic instance tests.
         */
        static Stream<Arguments> dynamicExamples() {
            return Stream.of(//
                    // No bases no slots
                    dynamicExample(HCD_O.class, "O"), //
                    // No bases, slots
                    dynamicExample(HCS_Oa.class, "Oa", List.of(), List.of("a")) //
            );
        }

        @DisplayName("is instantiated by calling its type")
        @ParameterizedTest(name = "when defined by {1}")
        @MethodSource("dynamicExamples")
        void newInstance(Class<?> refClass, String title, PyType type, Object[] args, String[] kwds)
                throws Throwable {
            // Instantiate by calling the type
            Object o = Callables.call(type, args, null);
            PyType t = PyType.of(o);
            PyType[] m = t.getMRO();
            assertSame(type, m[0]);
            assertSame(PyObject.TYPE, m[1]);
        }

        @Test
        @Disabled("Add tests of sub-sub-classes")
        void missingTests() {}
    }

    /**
     * We create new types and instances of their Java representations going
     * via capabilities in the type object.
     */
    @Nested
    @DisplayName("A Python subclass of float ...")
    class DynamicFloatSubclass extends DynamicBase {
        /**
         * Provide a stream of examples as parameter sets to the tests.
         *
         * @return the examples for dynamic instance tests.
         */
        static Stream<Arguments> dynamicExamples() {
            return Stream.of(//
                    // Simple base, no slots
                    dynamicExample(HCD_F.class, "F", List.of(PyFloat.TYPE), null,
                            new Object[] {42.0}, null), //
                    // Simple base, slots
                    dynamicExample(HCS_Fabc.class, "Fabc", List.of(PyFloat.TYPE),
                            List.of("c", "a", "b"), new Object[] {42.0}, null) //
            );
        }

        @DisplayName("is instantiated by calling its type")
        @ParameterizedTest(name = "when defined by {1}")
        @MethodSource("dynamicExamples")
        void newInstance(Class<?> refClass, String title, PyType type, Object[] args, String[] kwds)
                throws Throwable {
            // Instantiate by calling the type
            Object o = Callables.call(type, args, null);
            PyType t = PyType.of(o);
            PyType[] m = t.getMRO();
            assertSame(type, m[0]);
            assertSame(PyFloat.TYPE, m[1]);
        }

        @DisplayName("defines __neg__ by inheritance")
        @ParameterizedTest(name = "when defined by {1}")
        @MethodSource("dynamicExamples")
        void inheritsNeg(Class<?> refClass, String title, PyType type, Object[] args, String[] kwds)
                throws Throwable {
            // Lookup __neg__ on the type
            Object neg = type.lookup("__neg__");
            assertSame(PyFloat.TYPE.lookup("__neg__"), neg);
        }
    }

    // TODO: Test complex web of subclasses (of list?) as in narrative

    /**
     * Check expected value of integer as a set of class or member modifier
     * bits.
     *
     * @param name of class or member
     * @param expected modifier bits
     * @param actual modifier bits
     */
    static void assertModifiersMatch(String name, int expected, int actual) {
        int missing = expected & ~actual, extras = actual & ~expected;
        if (missing != 0) { fail(name + ": missing modifier(s): " + Modifier.toString(missing)); }
        if (extras != 0) { fail(name + ": unexpected modifier(s): " + Modifier.toString(extras)); }
    }

    /**
     * Find the most-derived representation class of all the bases, which
     * the generated class must extend, and the interfaces that class must
     * implement. That is, {@code b}'s canonical class is assignable from
     * the returned class for all {@code b} in {@code bases}.
     *
     * @param interfaces aggregated from the representation classes
     * @param bases to scan for canonical representations
     * @param title for error messages only
     * @return the most-derived representation class
     */
    private static PyType findBaseClass(Set<Class<?>> interfaces, List<PyType> bases,
            CharSequence title) {

        PyType baseType = PyObject.TYPE;
        Class<?> baseClass = baseType.canonicalClass();
        assert baseClass == Object.class;

        for (PyType b : bases) {
            Class<?> canonical = b.canonicalClass();
            if (canonical != baseClass) {
                if (isBaseLike(canonical)) {
                    if (baseClass.isAssignableFrom(canonical)) {
                        // b.canonical is the more derived: take it
                        baseType = b;
                        baseClass = canonical;
                    } else if (canonical.isAssignableFrom(baseClass)) {
                        // baseClass is the more derived: keep it
                    } else {
                        // Neither more nor less: that's bad
                        fail("cannot resolve bases " + title);
                    }
                } else if (isInterfaceLike(canonical)) {
                    // Collect interfaces
                    interfaces.add(canonical);
                } else {
                    fail(String.format("neither base nor interface %s in %s", b.getName(), title));
                }
            }
        }
        return baseType;
    }

    /**
     * Test that a class is suitable as a base (not final, private or an
     * interface).
     */
    private static boolean isBaseLike(Class<?> c) {
        int m = c.getModifiers();
        return !(Modifier.isInterface(m) || Modifier.isFinal(m) || Modifier.isPrivate(m));
    }

    /** Test that a class is an interface. */
    private static boolean isInterfaceLike(Class<?> c) {
        int m = c.getModifiers();
        return Modifier.isInterface(m);
    }
    // }

    /** A bridge method, generated by the compiler (JVMS 4.6). */
    static final int ACC_BRIDGE = 0x40;
    /** Declared synthetic (JVMS 4.6). */
    static final int ACC_SYNTHETIC = 0x1000;
    /** Not in our prototype source code if any of these modifiers. */
    static final int SYNTHETIC = ACC_SYNTHETIC | ACC_BRIDGE;

    // Hand-crafted models of the representation classes -------------

    /**
     * A hand-crafted model of the Java representation that should be formed
     * when a Python class is defined by:<pre>
     * class O1: pass
     * x = O1()
     * </pre>
     * <p>
     * {@code x} will be an instance of this class (but {@code O1} will be
     * an instance of {@code type}).
     */
    static class HCD_O implements WithDictAssignment, WithClassAssignment {

        /**
         * Type of this object is managed here since it is not assignable in
         * {@code object}.
         */
        private PyType $type;

        /** Instance dictionary. */
        private PyDict $dict;

        HCD_O(PyType actualType) { this.$type = checkClassAssignment(actualType); }

        @Override
        public PyType getType() { return $type; }

        @Override
        public void setType(Object replacementType) {
            $type = checkClassAssignment(replacementType);
        }

        @Override
        public PyDict getDict() { return $dict; }

        @Override
        public void setDict(Object replacementDict) {
            $dict = checkDictAssignment(replacementDict);
        }
    }

    /**
     * A hand-crafted model of the Java representation that should be formed
     * when a Python class is defined by:<pre>
     * class F1(float): pass
     * x = F1(42)
     * </pre>
     * <p>
     * {@code x} will be an instance of this class (but {@code F1} will be
     * an instance of {@code type}).
     */
    static class HCD_F extends PyFloat implements WithDictAssignment, WithClassAssignment {

        /**
         * Type of this object is managed here since it is not assignable in
         * {@code float}.
         */
        private PyType $type;

        /**
         * Add an instance dictionary because {@code float} does not have one.
         */
        private PyDict $dict;

        HCD_F(PyType actualType, double value) {
            super(value);
            this.$type = checkClassAssignment(actualType);
        }

        @Override
        public PyType getType() { return $type; }

        @Override
        public void setType(Object replacementType) {
            $type = checkClassAssignment(replacementType);
        }

        @Override
        public PyDict getDict() { return $dict; }

        @Override
        public void setDict(Object replacementDict) {
            $dict = checkDictAssignment(replacementDict);
        }
    }

    /**
     * {@code HCD_T} is a hand-crafted model of the Java representation that
     * should be formed when a Python class is defined by:<pre>
     * class Meta(type): pass
     * class M2o(metaclass=Meta): pass
     * x = M2o()
     * </pre> If we further define:<pre>
     * class Peta(type): pass
     * class P2o(metaclass=Peta): pass
     * </pre> then class assignment is possible on the types:<pre>
     * P2o.__class__ = Meta
     * </pre> This tells us that the objects {@code M2o} (an instance in
     * Python of {@code Meta}) and {@code P2o} (an instance in Python of
     * {@code Peta}, initially) must be instances in Java of the same class,
     * which is the shared representation that {@code Meta} and {@code Peta}
     * designate for their instances. It must have a modifiable
     * {@code __class__} attribute. The class {@code HCD_T} is a model for
     * that class.
     * <p>
     * The Python type of {@code Meta} and {@code Peta} is exactly
     * {@code type}, but in order to share a representation, these type
     * objects must be implemented as {@link ReplaceableType}.
     * <p>
     * Further class assignments are also possible on the instances of
     * {@code Meta} and {@code Peta}:<pre>
     * y = P2o()
     * y.__class__ = M2o
     * class O2: pass
     * x.__class__ = O2
     * </pre> We infer that {@code x} (an instance in Python of {@code M2o})
     * and {@code y} (an instance in Python of {@code P2o}, initially) must
     * also be instances in Java of the same class, which is the shared
     * representation designated by {@code M2o}, {@code P2o} and {@code O2}
     * for their instances. But this is just {@code HCD_O}.
     * <p>
     * The Python type of {@code M2o} is {@code Meta}, which is a subclass
     * in Python of {@code type}, therefore the Java class of the object
     * {@code M2o} subclasses a representation of {@code type}. As we have
     * seen, {@code P2o} is an instance in Java of the same class. These
     * type objects also designate the same Java class ({@code HCD_O}) to
     * represent their instances, so they must also subclass
     * {@link ReplaceableType}. Therefore {@code HCD_T} is a subclass of
     * {@code ReplaceableType}.
     * <p>
     * Note {@code HCD_T} inherits {@link WithDict} from
     * {@link ReplaceableType}, and allows Python class assignment, but does
     * not implement {@link WithDictAssignment}.
     */
    static class HCD_T extends ReplaceableType implements WithClassAssignment {

        /**
         * Type of this object is managed here since it is not assignable in
         * {@code type}.
         */
        private PyType $type;

        HCD_T(PyType metaclass, String name) {
            super(name, new SharedRepresentation(HCD_O.class, HCD_O.class),
                    new BaseType[] {(BaseType)PyType.TYPE()});
            this.$type = checkClassAssignment(metaclass);
        }

        @Override
        public PyType getType() { return $type; }

        @Override
        public void setType(Object replacementType) {
            // Maybe add mutability and other checks.
            $type = checkClassAssignment(replacementType);
        }
    }

    /**
     * A hand-crafted model of the Java representation that should be formed
     * when a Python class is defined by:<pre>
     * class Oa:
     *     __slots__ = ('a',)
     * </pre>
     */
    static class HCS_Oa implements WithClassAssignment {

        /**
         * Type of this object is managed here since it is not assignable in
         * {@code object}.
         */
        private PyType $type;

        /* Variables corresponding to {@code __slots__} names. */
        // @Exposed.Member
        Object a;

        HCS_Oa(ReplaceableType actualType) { this.$type = checkClassAssignment(actualType); }

        @Override
        public PyType getType() { return $type; }

        @Override
        public void setType(Object replacementType) {
            $type = checkClassAssignment(replacementType);
        }
    }

    /**
     * A hand-crafted model of the Java representation that should be formed
     * when a Python class is defined by:<pre>
     * class Fabc(float):
     *     __slots__ = ('a', 'b', 'c')
     * </pre>
     */
    static class HCS_Fabc extends PyFloat implements WithClassAssignment {

        /**
         * Type of this object is managed here since it is not assignable in
         * {@code float}.
         */
        private PyType $type;

        /* Variables corresponding to {@code __slots__} names. */
        // @Exposed.Member
        Object a;
        // @Exposed.Member
        Object b;
        // @Exposed.Member
        Object c;

        HCS_Fabc(ReplaceableType actualType, double value) {
            super(value);
            this.$type = checkClassAssignment(actualType);
        }

        @Override
        public PyType getType() { return $type; }

        @Override
        public void setType(Object replacementType) {
            $type = checkClassAssignment(replacementType);
        }
    }

    /**
     * Make a string title for the test of the form
     * {@code class name(b0, b1, b2):} and a {@code __slots__} declaration.
     *
     * @param name for the class
     * @param bases of the class
     * @param slots names of the slots
     * @return title for test
     */
    private static String generateClassDeclaration(String name, List<PyType> bases,
            List<String> slots) {

        StringBuilder sb = new StringBuilder(80);
        sb.append("class ").append(name);
        if (bases.size() > 0) {
            StringJoiner sj = new StringJoiner(", ", "(", ")");
            for (PyType b : bases) { sj.add(b.getName()); }
            sb.append(sj.toString());
        }
        sb.append(':');

        // We may have slots
        if (slots != null) {
            StringJoiner sj =
                    new StringJoiner("', '", " __slots__ = ('", slots.size() == 1 ? "',)" : "')");
            for (String s : slots) { sj.add(s); }
            sb.append(sj.toString());
        }

        return sb.toString();
    }
}
