// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigInteger;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.python.internal.Util;

/**
 * Tests of some basic mechanisms in {@link PyType} when exercised from
 * Java. We make a unit test of this because at the stage where these
 * don't work, we can't run tests in Python. It was created originally
 * to exercise the use of {@code type.__call__}, {@code type.__new__}
 * and {@code type.__init__} in a range of circumstances.
 */
@DisplayName("PyType implements")
class PyTypeTest extends UnitTestSupport {

    /** Base of tests that call {@code type}. */
    abstract static class AbstractCallTypeTest {

        /**
         * Provide a stream of types and selected characteristics as parameter
         * sets to the tests of types. Each example is essentially a tuple of a
         * type object, a value that should have that type, and text forms of
         * those two.
         *
         * @return the examples for search tests.
         */
        static Stream<Arguments> enquiryExamples() {
            return Stream.of(
                    // A type and a value that should have that type
                    enquiryExample(PyObject.TYPE, new Object()), //
                    enquiryExample(PyLong.TYPE, 1), //
                    enquiryExample(PyBool.TYPE, true), //
                    enquiryExample(PyType.TYPE(), PyLong.TYPE)  //
            );
        }

        /**
         * Construct a type enquiry problem and reference result.
         *
         * @param type concerned
         * @param obj an instance of that type
         * @return example data for a test
         */
        private static Arguments enquiryExample(PyType type, Object obj) {
            String ostr = obj.toString();
            String tname = type.getName();
            return arguments(type, obj, ostr, tname);
        }

        /**
         * Provide a stream of types and selected characteristics as parameter
         * sets to the tests of types. Each example is essentially a tuple of a
         * type object, a way to recognise the correct value has been
         * constructed, and arguments for the type object that would yield an
         * equal value when used as a constructor.
         *
         * @return the examples for search tests.
         */
        static Stream<Arguments> instanceExamples() {
            final BigInteger BIG = BigInteger.valueOf(10_000_000_000L);
            return Stream.of(
                    // A specification for each test
                    instanceExample(PyObject.TYPE, o -> PyType.of(o) == PyObject.TYPE,
                            "is an object"), //
                    instanceExample(PyLong.TYPE, 1, 1), //
                    instanceExample(PyLong.TYPE, 10, BigInteger.TEN), //
                    instanceExample(PyLong.TYPE, 42, 42), //
                    instanceExample(PyLong.TYPE, BIG, BIG), //
                    instanceExample(PyFloat.TYPE, 42.0, 42.0), //
                    instanceExample(PyFloat.TYPE, 42.0, 42), //
                    instanceExample(PyBool.TYPE, true, 1) //
            );
        }

        /**
         * Construct an instance construction problem with a test on the result
         * specified by the caller.
         *
         * @param type concerned
         * @param test to apply to the result as a predicate
         * @param strTest human readable statement of test (for display)
         * @param args to {@code type.__call__} to produce the value
         * @return example data for a test
         */
        private static Arguments instanceExample(PyType type, Predicate<Object> test,
                String strTest, Object... args) {
            String strType = type.getName();
            StringJoiner argJoiner = new StringJoiner(", ");
            for (Object v : args) { argJoiner.add(v.toString()); }
            return arguments(type, args, test, strType, argJoiner.toString(), strTest);
        }

        /**
         * Construct an instance construction problem and reference result.
         *
         * @param type concerned
         * @param value an instance of that type
         * @param args to {@code type.__call__} to produce the value
         * @return example data for a test
         */
        private static Arguments instanceExample(PyType type, Object value, Object... args) {
            Predicate<Object> test = o -> pythonEquals(value, o);
            String strTest = String.format("== %s", value);
            return instanceExample(type, test, strTest, args);
        }
    }

    @Nested
    @DisplayName("type.__call__")
    class CallTest extends AbstractCallTypeTest {
        /**
         * Test type enquiry.
         *
         * @param type expected as a result
         * @param obj on which to call {@code type()}
         * @param ostr string form of obj for display only
         * @param tname string form of type for display only
         * @throws Throwable unexpectedly
         */
        @DisplayName("enquiry type(obj)")
        @ParameterizedTest(name = "type({2}) is {3}")
        @MethodSource("enquiryExamples")
        void enquiry(PyType type, Object obj, String ostr, String tname) throws Throwable {
            Object t = Callables.call(PyType.TYPE(), obj);
            assertEquals(type, t);
            assertEquals(type, PyType.of(obj));
        }

        /**
         * Test instance construction.
         *
         * @param type to use as constructor
         * @param args to {@code __call__}
         * @param test whether result is as expected
         * @param strType string form of type for display only
         * @param strArgs string form of args for display only
         * @param strTest string form of test for display only
         * @throws Throwable unexpectedly
         */
        @DisplayName("construction <type>(args)")
        @ParameterizedTest(name = "{3}({4}) {5}")
        @MethodSource("instanceExamples")
        void instance(PyType type, Object[] args, Predicate<Object> test, String strType,
                String strArgs, String strTest) throws Throwable {
            Object r = Callables.call(type, args, null);
            assertTrue(test.test(r), strTest);
        }
    }

    /** Base of tests that create new types. */
    abstract static class AbstractNewTypeTest extends UnitTestSupport {

        private static final PyType INT = PyLong.TYPE;
        private static final PyType LIST = PyList.TYPE;
        private static final PyType TYPE = PyType.TYPE();
        private static final PyType OBJECT = PyObject.TYPE;

        /**
         * Provide a stream of metatypes and parameter sets to the tests of new
         * type creation. Each example is essentially a tuple of a metatype ,
         * bases of the new type, the namespace and a test of the new type.
         *
         * @return the examples for search tests.
         */
        static Stream<Arguments> newExamples() {

            // class L(list): pass
            PyType L = makeType(TYPE, "L", LIST);
            // class L1(list): pass
            // PyType L1 = makeType(TYPE, "L1", LIST);
            // class L2(list): pass
            PyType L2 = makeType(TYPE, "L2", LIST);
            // class L3(L): pass
            PyType L3 = makeType(TYPE, "L3", L);
            // class Meta(type): pass
            PyType META = makeType(TYPE, "Meta", new PyType[] {TYPE});

            return Stream.of(
                    // A specification for each test

                    // class A: pass
                    newExample(TYPE, "A", new PyType[] {}, new PyDict(),
                            t -> newTypeCheck(t, TYPE, "A", OBJECT),
                            // A()
                            t -> constructorCheck(t)),

                    // class A(int): pass
                    newExample(TYPE, "A", new PyType[] {INT}, new PyDict(),
                            t -> newTypeCheck(t, TYPE, "A", INT),
                            // A(42)
                            t -> constructorCheck(t, 42)),

                    // class L4A(L3, L2): pass
                    newExample(TYPE, "L4A", new PyType[] {L3, L2}, new PyDict(),
                            t -> newTypeCheck(t, TYPE, "L4A", L3),
                            // L4A()
                            t -> constructorCheck(t)),

                    // class L4(L3, L2, list): pass
                    newExample(TYPE, "L4", new PyType[] {L3, L2, LIST}, new PyDict(),
                            t -> newTypeCheck(t, TYPE, "L4", L3),
                            // L4()
                            t -> constructorCheck(t)),

                    // class Other(type): pass
                    // class MyOtherClass(list, metaclass=Other): pass
                    newExample(TYPE, "Other", new PyType[] {TYPE}, new PyDict(),
                            t -> newTypeCheck(t, TYPE, "Other", TYPE),
                            t -> constructorCheck(t, "MyOtherClass", Py.tuple(LIST), new PyDict())),

                    // class MyClass(metaclass=Meta): pass
                    newExample(META, "MyClass", new PyType[] {}, new PyDict(),
                            t -> newTypeCheck(t, META, "MyClass", OBJECT),
                            // MyClass()
                            t -> constructorCheck(t)),

                    // class MyClass(list, metaclass=Meta): pass
                    newExample(META, "MyClass", new PyType[] {LIST}, new PyDict(),
                            t -> newTypeCheck(t, META, "MyClass", LIST),
                            // MyClass()
                            t -> constructorCheck(t)) // ,
            );
        }

        /**
         * Create a new type for use in tests.
         *
         * @return new type or null on failure
         */
        private static PyType makeType(PyType metatype, String name, PyType... bases) {
            PyType type;
            try {
                type = (PyType)metatype.call(name, new PyTuple(bases), Py.dict());
            } catch (Throwable e) {
                type = null;
            }
            return type;
        }

        // TODO write a different stream of examples that throw errors

        /**
         * Construct an instance construction problem with a test on the result
         * specified by the caller.
         *
         * @param metatype to select the constructor
         * @param name of the new type
         * @param bases to the new type
         * @param namespace for the new type
         * @param test to apply to the result
         * @return example data for a test
         */
        @SafeVarargs
        private static Arguments newExample(PyType metatype, String name, PyType[] bases,
                PyDict namespace, Consumer<PyType>... tests) {
            assertNotNull(metatype, "check metatype exists");
            String strMetatype = metatype.getName();
            String strNamespace = namespace.toString();
            StringJoiner b = new StringJoiner(", ", "(", ")");
            for (PyType t : bases) { b.add(t.getName()); }
            return arguments(metatype, name, new PyTuple(bases), namespace, tests, strMetatype,
                    b.toString(), strNamespace);
        }

        /**
         * Check a new type for expected attributes.
         *
         * @param type the new type
         * @param cls type of the new type
         * @param name the type should bear
         * @param base of the new type
         */
        static void newTypeCheck(PyType type, PyType cls, String name, PyType base) {
            try {
                // Properties the type should have
                assertSame(cls, type.getType());
                assertEquals(name, type.getName());
                assertSame(base, type.getBase());
            } catch (Throwable t) {
                throw Util.asUnchecked(t);
            }
        }

        /**
         * Check result of constructing and object with the new type.
         *
         * @param type the new type
         * @param args arguments when making an instance
         */
        static void constructorCheck(PyType type, Object... args) {
            try {
                // Make an instance of the new type
                Object o = Callables.call(type, args, null);
                assertPythonType(type, o);
            } catch (Throwable t) {
                throw Util.asUnchecked(t);
            }
        }
    }

    @Nested
    @DisplayName("type.__new__")
    class NewTypeTest extends AbstractNewTypeTest {
        /**
         * Test type construction by the 3-argument call to type.
         *
         * @param metatype to select the constructor
         * @param name of the new type
         * @param bases to the new type
         * @param namespace for the new type
         * @param tests to apply to the result
         * @param strMetatype string form of {@code metatype}
         * @param strBases string form of {@code bases}
         * @param strNamespace string form of {@code namespace}
         * @throws Throwable unexpectedly
         */
        @DisplayName("definition <metatype>(name, bases, namespace)")
        @ParameterizedTest(name = "{5}(\"{1}\", {6}, {7}) has expected properties")
        @MethodSource("newExamples")
        void newType(PyType metatype, String name, PyTuple bases, PyDict namespace,
                Consumer<PyType>[] tests, String strMetatype, String strBases, String strNamespace)
                throws Throwable {
            PyType t = (PyType)Callables.call(metatype, name, bases, namespace);
            // Customised tests specified by caller
            for (Consumer<PyType> test : tests) { test.accept(t); }
        }

        /**
         * Test invalid arguments to the 3-argument call to type.
         *
         * @param metatype to select the constructor
         * @param name of the new type
         * @param bases to the new type
         * @param namespace for the new type
         * @param test whether result is as expected
         * @param strMetatype string form of {@code metatype}
         * @throws Throwable unexpectedly
         */
        @Disabled("ClassCastException in handle.invokeExact")
        // FIXME approach needed to reporting bad casts in invokeExact
        @DisplayName("<metatype>(name, bases, namespace) type error")
        @ParameterizedTest(name = "{5}(\"{1}\", ...)")
        @MethodSource("newExamples")
        void newTypeError(PyType metatype, String name, PyTuple bases, PyDict namespace,
                Consumer<PyType> test, String strMetatype) throws Throwable {

            // Supply a non-tuple as bases argument
            assertThrows(PyBaseException.class,
                    () -> Callables.call(metatype, name, NOT_TUPLE, namespace));

            // Corrupt the list of bases with a non-type
            int n = bases.size();
            Object[] not_bases = bases.toArray(new Object[n + 1]);
            not_bases[n] = 42;
            PyTuple NOT_BASES = new PyTuple(not_bases);
            assertThrows(PyBaseException.class,
                    () -> Callables.call(metatype, name, NOT_BASES, namespace));

            // Supply wrong kind of object as a namespace
            assertThrows(PyBaseException.class,
                    () -> Callables.call(metatype, name, bases, NOT_NAMESPACE));
        }

        static final Object NOT_TUPLE = 1;
        static final Object NOT_NAMESPACE = "not a namespace";
    }

    @Disabled("Implement more thorough test of a metatype")
    @Test
    void newTypeError() {

        // TODO creation of a metatype

        // TODO construction with a metatype
    }
}
