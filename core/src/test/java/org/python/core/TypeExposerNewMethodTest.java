// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.python.support.MethodKind;
import org.python.support.ScopeKind;
import org.python.types.Exposed;
import org.python.types.TypeSpec;
import org.python.types.WithClass;
import org.python.types.Exposed.Default;
import org.python.types.Exposed.PositionalOnly;
import org.python.types.Exposed.PythonNewMethod;

/**
 * Test that {@code __new__} methods exposed by Python <b>types</b>
 * defined in Java, using the scheme of annotations defined in
 * {@link Exposed}, result in method objects that correspond to their
 * definitions.
 * <p>
 * The first test in each case is to examine the fields in the parser
 * that attaches to the {@link PyJavaFunction}. Then we call the
 * function using the {@code __call__} special method, and using our
 * "fast call" signatures.
 * <p>
 * There is a nested test suite for each signature pattern. Since we are
 * dealing with {@code __new__}, each fresh signature needs a fresh
 * class to define it.
 */
@DisplayName("A __new__ method exposed by a type")
class TypeExposerNewMethodTest extends UnitTestSupport {

    /**
     * Certain nested test classes implement these as standard. A base class
     * here is just a way to describe the tests once that reappear in each
     * nested case.
     */
    abstract static class Standard {

        /** We frequently need to pass no keyword arguments. */
        static final String[] NONAMES = new String[0];

        // Working variables for the tests
        /** Descriptor by type access to examine or call. */
        @Deprecated
        PyStaticMethod descr;
        /** The type defining the method. */
        PyType type;
        /** The function to examine or call. */
        PyJavaFunction func;
        /** The parser we examine. */
        ArgParser ap;
        /** The expected result of calling the method. */
        Object[] exp;

        /**
         * A parser attached to the method descriptor should have field values
         * that correctly reflect the signature and annotations in the defining
         * class.
         */
        abstract void has_expected_fields();

        /**
         * The type dictionary entry has the expected content.
         */
        @Test
        void wraps_expected_function() {
            assertEquals("__new__", func.__name__());
            assertEquals(type, func.self);  // bound to defining type
        }

        /**
         * Call the function using the {@code __call__} special method with
         * arguments correct for the method's specification. The method should
         * obtain the correct result (and not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void supports__call__() throws Throwable;

        /**
         * Call the method using the {@code __call__} special method with
         * arguments correct for the method's specification, and explicitly zero
         * or more keywords. The method should obtain the correct result (and
         * not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void supports_keywords() throws Throwable;

        /**
         * Call the method using the {@code __call__} special method but not
         * even the first argument is provided. The method should throw
         * {@link PyExc#TypeError TypeError}.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void raises_TypeError_if_no_args() throws Throwable {
            // We call type.__new__()
            final Object[] args = new Object[0];
            String[] names = {};
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
        }

        /**
         * Call the method using the {@code __call__} special method but the
         * first argument is not a {@code PyType} or that {@code PyType} is not
         * a sub-type of the defining type. The method should throw
         * {@link PyExc#TypeError TypeError}.
         *
         * @throws Throwable unexpectedly
         */
        abstract void raises_TypeError_if_not_subtype() throws Throwable;

        /**
         * Call the method using the {@code __call__} special method and an
         * unexpected keyword: where none is expected, for a positional
         * argument, or simply an unacceptable name. The method should throw
         * {@link PyExc#TypeError TypeError}.
         *
         * @throws Throwable unexpectedly
         */
        abstract void raises_TypeError_on_unexpected_keyword() throws Throwable;

        /**
         * Call the function using the Java call interface with arguments
         * correct for the function's specification. The function should obtain
         * the correct result (and not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void supports_java_call() throws Throwable;

        /**
         * Check that the fields of the parser match expectations for a method
         * with no collector parameters and a certain number of positional-only
         * parameters.
         *
         * @param kind static or instance
         * @param name of method
         * @param count of parameters
         * @param posonlycount count of positional-only parameters
         */
        void no_collector(MethodKind kind, String name, int count, int posonlycount) {
            assertEquals(name, ap.name);
            assertEquals(kind, ap.methodKind);
            assertEquals(ScopeKind.TYPE, ap.scopeKind);
            assertEquals(count, ap.argnames.length);
            assertEquals(count, ap.argcount);
            assertEquals(posonlycount, ap.posonlyargcount);
            assertEquals(0, ap.kwonlyargcount);
            assertEquals(count, ap.regargcount);
            assertEquals(-1, ap.varArgsIndex);
            assertEquals(-1, ap.varKeywordsIndex);
        }

        /**
         * Check that the fields of the parser match expectations for a
         * {@code __new__} method with no collector parameters and a certain
         * number of positional-only parameters after type.
         *
         * @param count of parameters after type
         * @param posonly count of positional-only parameters
         */
        void no_collector_new(int count, int posonly) {
            no_collector(MethodKind.NEW, "__new__", count + 1, posonly + 1);
        }

        /**
         * Helper to set up each test.
         *
         * @param t to use as the self argument
         * @throws PyAttributeError if method not found
         * @throws Throwable other errors
         */
        void setup(PyType t) throws PyAttributeError, Throwable {
            // _new__ appears as a bound PyJavaFunction
            type = t;
            func = (PyJavaFunction)t.lookup("__new__");
            ap = func.argParser;
        }

        /**
         * Check the result of a call against {@link #exp}. The reference result
         * is the same throughout a given sub-class test.
         *
         * @param result of call
         */
        void check_result(Object result) {
            assertPythonType(type, result);
            TestTypeBase obj = (TestTypeBase)result;
            assertArrayEquals(exp, obj.constructorArgs);
        }

        /**
         * Check that TypeError is thrown when the first argument to a working
         * call has been replaced with either a non-type or a type that is not a
         * sub-type.
         *
         * @param args values to method call
         * @param names keyword names
         * @throws Throwable unexpectedly
         */
        void check_not_subtype(Object[] args, String[] names) throws Throwable {
            // Prove correct by repeating supports_keywords()
            Object r = func.__call__(args, names);
            check_result(r);
            // Repeat with args[0] not a type at all
            args[0] = "incorrect";
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
            // Repeat with args[0] not sub-type
            args[0] = PyLong.TYPE;
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
        }
    }

    static abstract class TestTypeBase implements WithClass {
        final Object[] constructorArgs;

        TestTypeBase(Object... constructorArgs) { this.constructorArgs = constructorArgs; }
    }

    /**
     * A Python type definition where {@code __new__} requires the sub-type
     * and has no further parameters. Tests are in {@link NoParams}.
     */
    static class TT0 extends TestTypeBase {
        static PyType TYPE = PyType.fromSpec(new TypeSpec("TT0", MethodHandles.lookup()));

        private TT0() {}

        @Override
        public PyType getType() { return TYPE; }

        @PythonNewMethod
        static TT0 __new__(PyType type) {
            if (type == TYPE)
                return new TT0();
            else
                return new Derived(type);
        }

        // FIXME Old inadequate concept of Python subclass
        static class Derived extends TT0 {
            private PyType type;

            @Override
            public PyType getType() { return type; }

            protected Derived(PyType type) {
                super();
                this.type = type;
            }
        }
    }

    /**
     * {@link TT0#__new__(PyType) TT0.__new__} requires the sub-type and has
     * no further parameters.
     */
    @Nested
    @DisplayName("with no parameters")
    class NoParams extends Standard {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // Find TT0 and its __new__
            setup(TT0.TYPE);
            exp = new Object[0];
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_new(0, 0); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type.__new__(type)
            Object[] args = {type};
            Object r = func.__call__(args, null);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type.__new__(type)
            Object[] args = {type};
            Object r = func.__call__(args, NONAMES);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_if_not_subtype() throws Throwable {
            Object[] args = {type};
            check_not_subtype(args, NONAMES);
        }

        /** To set anything by keyword is a {@code TypeError}. */
        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call type.__new__(type, c=3)
            Object[] args = {type, 3};
            String[] names = {"c"};
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call type.__new__(type)
            Object r = func.call(type);
            check_result(r);
        }
    }

    /**
     * A Python type definition where {@code __new__} requires the sub-type
     * and has a further single positional parameter. Tests are in
     * {@link OnePos}.
     */
    static class TT1 extends TestTypeBase {
        static PyType TYPE = PyType.fromSpec(new TypeSpec("TT1", MethodHandles.lookup()));

        private TT1(double a) { super(a); }

        @Override
        public PyType getType() { return TYPE; }

        @PythonNewMethod
        static TT1 __new__(PyType type, double a) {
            if (type == TYPE)
                return new TT1(a);
            else
                return new Derived(type, a);
        }

        static class Derived extends TT1 {
            private PyType type;

            @Override
            public PyType getType() { return type; }

            protected Derived(PyType type, double a) {
                super(a);
                this.type = type;
            }
        }
    }

    /**
     * {@link TT1#__new__(PyType, double) TT1.__new__} requires the sub-type
     * and 1 argument that <b>must</b> be given by position.
     */
    @Nested
    @DisplayName("with a single positional-only parameter by default")
    class OnePos extends Standard {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // Find TT1 and its __new__
            setup(TT1.TYPE);
            exp = new Object[] {42.0};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_new(1, 1); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type.__new__(type, 42.0)
            Object[] args = {type, 42.0};
            Object r = func.__call__(args, null);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type.__new__(type, 42.0)
            Object[] args = {type, 42.0};
            Object r = func.__call__(args, NONAMES);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_if_not_subtype() throws Throwable {
            Object[] args = {type, 42.0};
            check_not_subtype(args, NONAMES);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call type.__new__(type, a=42.0)
            Object[] args = {type, 42.0};
            String[] names = {"a"};
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call type.__new__(42.0)
            Object r = func.call(type, 42.0);
            check_result(r);
        }
    }

    /**
     * A Python type definition where {@code __new__} requires the sub-type
     * and has a further 3 positional parameters. Tests are in
     * {@link PositionalByDefault}.
     */
    static class TT3 extends TestTypeBase {
        static PyType TYPE = PyType.fromSpec(new TypeSpec("TT3", MethodHandles.lookup()));

        private TT3(int a, String b, Object c) { super(a, b, c); }

        @Override
        public PyType getType() { return TYPE; }

        @PythonNewMethod
        static TT3 __new__(PyType type, int a, String b, Object c) {
            if (type == TYPE)
                return new TT3(a, b, c);
            else
                return new Derived(type, a, b, c);
        }

        static class Derived extends TT3 {
            private PyType type;

            @Override
            public PyType getType() { return type; }

            protected Derived(PyType type, int a, String b, Object c) {
                super(a, b, c);
                this.type = type;
            }
        }
    }

    /**
     * {@link TT3#__new__(PyType, int, String, Object) TT3.__new__} accepts
     * 3 arguments that <b>must</b> be given by position.
     */
    @Nested
    @DisplayName("with positional-only parameters by default")
    class PositionalByDefault extends Standard {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // Find TT3 and its __new__
            setup(TT3.TYPE);
            exp = new Object[] {1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_new(3, 3); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type.__new__(type, 1, '2', 3)
            Object[] args = {type, 1, "2", 3};
            Object r = func.__call__(args, null);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type.__new__(type, 1, '2', 3)
            Object[] args = {type, 1, "2", 3};
            Object r = func.__call__(args, NONAMES);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_if_not_subtype() throws Throwable {
            Object[] args = {type, 1, "2", 3};
            check_not_subtype(args, NONAMES);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call type.__new__(type, 1, '2', c=3)
            Object[] args = {type, 1, "2", 3};
            String[] names = {"c"};
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call type.__new__(type, 1, '2', 3)
            Object r = func.call(type, 1, "2", 3);
            check_result(r);
        }
    }

    /**
     * A Python type definition where {@code __new__} requires the sub-type
     * and has a further 2 positional parameters and 1 that may be given by
     * position or keyword. Tests are in {@link PositionalWithDefaults}.
     */
    static class TT3pd extends TestTypeBase {
        static PyType TYPE = PyType.fromSpec(new TypeSpec("TT3pd", MethodHandles.lookup()));

        private TT3pd(int a, String b, Object c) { super(a, b, c); }

        @Override
        public PyType getType() { return TYPE; }

        @PythonNewMethod
        static TT3pd __new__(PyType type, int a, @Default("2") String b, @Default("3") Object c) {
            if (type == TYPE)
                return new TT3pd(a, b, c);
            else
                return new Derived(type, a, b, c);
        }

        static class Derived extends TT3pd {
            private PyType type;

            @Override
            public PyType getType() { return type; }

            protected Derived(PyType type, int a, String b, Object c) {
                super(a, b, c);
                this.type = type;
            }
        }
    }

    /**
     * {@link TT3pd#__new__(PyType, int, String, Object) TT3pd.__new__}
     * accepts 3 arguments that <b>must</b> be given by position but two
     * have defaults.
     */
    @Nested
    @DisplayName("with positional-only parameters and default values")
    class PositionalWithDefaults extends Standard {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // Find TT3pd and its __new__
            setup(TT3pd.TYPE);
            exp = new Object[] {1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_new(3, 3); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type.__new__(type, 1)
            Object[] args = {type, 1};
            Object r = func.__call__(args, null);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type.__new__(type, 1)
            Object[] args = {type, 1};
            Object r = func.__call__(args, NONAMES);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_if_not_subtype() throws Throwable {
            Object[] args = {type, 1};
            check_not_subtype(args, NONAMES);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call type.__new__(type, 1, c=3)
            Object[] args = {type, 1, 3};
            String[] names = {"c"};
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call type.__new__(type, 1)
            Object r = func.call(type, 1);
            check_result(r);
        }
    }

    /**
     * A Python type definition where {@code __new__} requires the sub-type
     * and has a further 3 positional parameters that may be given by
     * position or keyword. Tests are in {@link PositionalOrKeywordParams}.
     */
    static class TT3pk extends TestTypeBase {
        static PyType TYPE = PyType.fromSpec(new TypeSpec("TT3pk", MethodHandles.lookup()));

        private TT3pk(int a, String b, Object c) { super(a, b, c); }

        @Override
        public PyType getType() { return TYPE; }

        @PythonNewMethod // not positionalOnly=false to spare type
        static TT3pk __new__(@PositionalOnly PyType type, int a, @Default("2") String b,
                @Default("3") Object c) {
            if (type == TYPE)
                return new TT3pk(a, b, c);
            else
                return new Derived(type, a, b, c);
        }

        static class Derived extends TT3pk {
            private PyType type;

            @Override
            public PyType getType() { return type; }

            protected Derived(PyType type, int a, String b, Object c) {
                super(a, b, c);
                this.type = type;
            }
        }
    }

    /**
     * {@link TT3pk#__new__(PyType, int, String, Object) TT3pk.__new__}
     * accepts 3 arguments that may be given by position or keyword.
     */
    @Nested
    @DisplayName("with positional-or-keyword parameters")
    class PositionalOrKeywordParams extends Standard {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // Find TT3pk and its __new__
            setup(TT3pk.TYPE);
            exp = new Object[] {1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_new(3, 0); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type.__new__(type, 1, '2', 3)
            Object[] args = {type, 1, "2", 3};
            Object r = func.__call__(args, null);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type.__new__(type, 1, c=3, b='2')
            Object[] args = {type, 1, 3, "2"};
            String[] names = {"c", "b"};
            Object r = func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_if_not_subtype() throws Throwable {
            Object[] args = {type, 1, 3, "2"};
            String[] names = {"c", "b"};
            check_not_subtype(args, names);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() throws Throwable {
            // We call type.__new__(type, 1, c=3, b='2', x=4)
            Object[] args = {type, 1, 3, "2", 4};
            String[] names = {"c", "b", /* unknown */"x"};
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call type.__new__(type, 1, '2', 3)
            Object r = func.call(type, 1, "2", 3);
            check_result(r);
        }
    }

    /**
     * A Python type definition where {@code __new__} requires the sub-type
     * and has a further 2 positional parameters and 1 that may be given by
     * position or keyword. Tests are in {@link SomePositionalOnlyParams}.
     */
    static class TT3p2 extends TestTypeBase {
        static PyType TYPE = PyType.fromSpec(new TypeSpec("TT3p2", MethodHandles.lookup()));

        private TT3p2(int a, String b, Object c) { super(a, b, c); }

        @Override
        public PyType getType() { return TYPE; }

        @PythonNewMethod
        static TT3p2 __new__(PyType type, int a, @PositionalOnly String b, Object c) {
            if (type == TYPE)
                return new TT3p2(a, b, c);
            else
                return new Derived(type, a, b, c);
        }

        static class Derived extends TT3p2 {
            private PyType type;

            @Override
            public PyType getType() { return type; }

            protected Derived(PyType type, int a, String b, Object c) {
                super(a, b, c);
                this.type = type;
            }
        }
    }

    /**
     * {@link TT3p2#__new__(PyType, int, String, Object) TT3p2.__new__}
     * accepts 3 arguments, two of which may be given by position only, and
     * the last by either position or keyword.
     */
    @Nested
    @DisplayName("with two positional-only parameters")
    class SomePositionalOnlyParams extends Standard {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // Find TT3p2 and its __new__
            setup(TT3p2.TYPE);
            exp = new Object[] {1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_new(3, 2); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type.__new__(type, 1, '2', 3)
            Object[] args = {type, 1, "2", 3};
            Object r = func.__call__(args, null);
            check_result(r);
        }

        /** To set {@code c} by keyword is a ok. */
        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type.__new__(type, 1, '2', c=3)
            Object[] args = {type, 1, "2", 3};
            String[] names = {"c"};
            Object r = func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_if_not_subtype() throws Throwable {
            Object[] args = {type, 1, "2", 3};
            String[] names = {"c"};
            check_not_subtype(args, names);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() throws Throwable {
            // We call type.__new__(type, 1, c=3, b='2')
            Object[] args = {1, 3, "2"};
            String[] names = {"c", /* positional */"b"};
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call type.__new__(type, 1, '2', 3)
            Object r = func.call(type, 1, "2", 3);
            check_result(r);
        }
    }
}
