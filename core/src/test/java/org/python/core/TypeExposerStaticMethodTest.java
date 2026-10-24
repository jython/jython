// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.python.support.MethodKind;
import org.python.support.ScopeKind;
import org.python.types.Exposed;
import org.python.types.Exposed.Default;
import org.python.types.Exposed.PositionalOnly;
import org.python.types.Exposed.PythonStaticMethod;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * Test that static methods exposed by a Python <b>type</b> defined in
 * Java, using the scheme of annotations defined in {@link Exposed},
 * result in method descriptors with characteristics that correspond to
 * their definitions.
 * <p>
 * The first test in each case is to examine the fields in the parser
 * that attaches to the {@link PyJavaFunction}. Then we call the
 * function using the {@code __call__} special method, and using our
 * "fast call" signatures.
 * <p>
 * There is a nested test suite for each signature pattern.
 */
@DisplayName("A static method exposed by a type")
class TypeExposerStaticMethodTest extends UnitTestSupport {

    /**
     * Certain nested test classes implement these as standard. A base class
     * here is just a way to describe the tests once that reappear in each
     * nested case.
     */
    abstract static class Standard {

        // Working variables for the tests
        /** Descriptor by type access to examine or call. */
        PyStaticMethod descr;
        /** The object on which to invoke the method. */
        Object obj;
        /** The function to examine or call (bound to {@code obj}). */
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
            PyStaticMethod s = (PyStaticMethod)PyType.of(obj).lookup(ap.name);
            PyJavaFunction f = (PyJavaFunction)s.__get__(null, null);
            assertEquals(ap.name, f.__name__());
            assertNull(f.self);  // unbound
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
         * Check that the fields of the parser match expectations for a static
         * method with no collector parameters and a certain number of
         * positional-only parameters.
         *
         * @param name of method
         * @param count of parameters
         * @param posonly count of positional-only parameters
         */
        void no_collector_static(String name, int count, int posonly) {
            no_collector(MethodKind.STATIC, name, count, posonly);
        }

        /**
         * Check that the fields of the parser match expectations for a instance
         * method with no collector parameters and a certain number of
         * positional-only parameters.
         *
         * @param name of method
         * @param count of parameters
         * @param posonly count of positional-only parameters
         */
        void no_collector_instance(String name, int count, int posonly) {
            no_collector(MethodKind.INSTANCE, name, count, posonly);
        }

        /**
         * Helper to set up each test.
         *
         * @param name of the method
         * @param o to use as the self argument
         * @throws PyAttributeError if method not found
         * @throws Throwable other errors
         */
        void setup(String name, Object o) throws PyAttributeError, Throwable {
            // A static method should bind as a PyJavaFunction
            descr = (PyStaticMethod)PyType.of(o).lookup(name);
            obj = o;
            func = (PyJavaFunction)Abstract.getAttr(obj, name);
            ap = func.argParser;
        }

        /**
         * Check the result of a call against {@link #exp}. The reference result
         * is the same throughout a given sub-class test.
         *
         * @param result of call
         */
        void check_result(PyTuple result) { assertArrayEquals(exp, result.toArray()); }
    }

    /**
     * A Python type definition that exhibits a range of method signatures
     * explored in the tests. All the methods {@code f*()} are static
     * methods to Python.
     */
    static class SimpleObject implements WithClass {

        static PyType TYPE = PyType.fromSpec(new TypeSpec("Simple", MethodHandles.lookup()));

        @Override
        public PyType getType() { return TYPE; }

        /**
         * See {@link NoParams}: no parameters are allowed.
         */
        @PythonStaticMethod
        static void f0() {}

        /**
         * See {@link OnePos}: a single positional parameter
         *
         * @param a positional arg
         * @return the arg (tuple)
         */
        @PythonStaticMethod
        static PyTuple f1(double a) { return Py.tuple(a); }

        /**
         * See {@link PositionalByDefault}: the parameters are positional-only
         * as a result of the default exposure.
         *
         * @param a positional arg
         * @param b positional arg
         * @param c positional arg
         * @return the args
         */
        @PythonStaticMethod
        static PyTuple f3(int a, String b, Object c) { return Py.tuple(a, b, c); }

        /**
         * See {@link PositionalWithDefaults}: the parameters are
         * positional-only as a result of the default exposure.
         *
         * @param a positional arg
         * @param b positional arg = 2
         * @param c positional arg = 3
         * @return the args
         */
        @PythonStaticMethod
        static PyTuple f3pd(int a, @Default("2") String b, @Default("3") Object c) {
            return Py.tuple(a, b, c);
        }

        /**
         * See {@link PositionalOrKeywordParams}: the parameters are
         * positional-or-keyword but none is positional-only.
         *
         * @param a positional-or-keyword arg
         * @param b positional-or-keyword arg
         * @param c positional-or-keyword arg
         * @return the args
         */
        @PythonStaticMethod(positionalOnly = false)
        static PyTuple f3pk(int a, String b, Object c) { return Py.tuple(a, b, c); }

        /**
         * See {@link SomePositionalOnlyParams}: two parameters are
         * positional-only as a result of an annotation.
         *
         * @param a positional arg
         * @param b positional arg
         * @param c positional-or-keyword arg
         * @return the args
         */
        @PythonStaticMethod
        static PyTuple f3p2(int a, @PositionalOnly String b, Object c) { return Py.tuple(a, b, c); }
    }

    /** {@link SimpleObject#f0()} accepts no arguments. */
    @Nested
    @DisplayName("with no parameters")
    class NoParams extends Standard {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // descr = Simple.f0
            setup("f0", new SimpleObject());
            // The method is declared void (which means return None)
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_static("f0", 0, 0); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type(obj).f0()
            Object[] args = new Object[0];
            Object r = descr.__call__(args, null);
            assertEquals(Py.None, r);

            // We call obj.f0()
            args = new Object[0];
            r = func.__call__(args, null);
            assertEquals(Py.None, r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type(obj).f0()
            Object[] args = new Object[0];
            String[] names = {};
            Object r = descr.__call__(args, names);
            assertEquals(Py.None, r);

            // We call obj.f0()
            r = func.__call__(args, names);
            assertEquals(Py.None, r);
        }

        /** To set anything by keyword is a {@code TypeError}. */
        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call type(obj).f0(c=3)
            Object[] args = {3};
            String[] names = {"c"};
            assertRaises(PyExc.TypeError, () -> descr.__call__(args, names));

            // We call obj.f0(c=3)
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // descr is not expected to support call()
            // We call obj.f0()
            Object r = func.call();
            assertEquals(Py.None, r);
        }
    }

    /**
     * {@link SimpleObject#f1(double)} accepts 1 argument that <b>must</b>
     * be given by position.
     */
    @Nested
    @DisplayName("with a single positional-only parameter by default")
    class OnePos extends Standard {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // descr = Simple.f1
            setup("f1", new SimpleObject());
            exp = new Object[] {42.0};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_static("f1", 1, 1); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type(obj).f1(42.0)
            Object[] args = {42.0};
            PyTuple r = (PyTuple)descr.__call__(args, null);
            check_result(r);

            // We call obj.f1(42.0)
            r = (PyTuple)func.__call__(args, null);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type(obj).f1(42.0)
            Object[] args = {42.0};
            String[] names = {};
            PyTuple r = (PyTuple)descr.__call__(args, names);
            check_result(r);

            // We call obj.f1(42.0)
            r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call type(obj).f1(a=42.0)
            Object[] args = {42.0};
            String[] names = {"a"};
            assertRaises(PyExc.TypeError, () -> descr.__call__(args, names));

            // We call obj.f1(a=42.0)
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // descr is not expected to support call()
            // We call obj.f1(42.0)
            PyTuple r = (PyTuple)func.call(42.0);
            check_result(r);
        }
    }

    /**
     * {@link SimpleObject#f3(int, String, Object)} accepts 3 arguments that
     * <b>must</b> be given by position.
     */
    @Nested
    @DisplayName("with positional-only parameters by default")
    class PositionalByDefault extends Standard {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // descr = Simple.f3
            setup("f3", new SimpleObject());
            exp = new Object[] {1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_static("f3", 3, 3); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type(obj).f3(1, '2', 3)
            Object[] args = {1, "2", 3};
            PyTuple r = (PyTuple)descr.__call__(args, null);
            check_result(r);

            // We call obj.f3(1, '2', 3)
            r = (PyTuple)func.__call__(args, null);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type(obj).f3(1, '2', 3)
            Object[] args = {1, "2", 3};
            String[] names = {};
            PyTuple r = (PyTuple)descr.__call__(args, names);
            check_result(r);

            // We call obj.f3(1, '2', 3)
            r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call type(obj).f3(1, '2', c=3)
            Object[] args = {1, "2", 3};
            String[] names = {"c"};
            assertRaises(PyExc.TypeError, () -> descr.__call__(args, names));

            // We call obj.f3(1, '2', c=3)
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // descr is not expected to support call()
            // We call obj.f3(obj, 1, '2', 3)
            PyTuple r = (PyTuple)func.call(1, "2", 3);
            check_result(r);
        }
    }

    /**
     * {@link SimpleObject#f3pd(int, String, Object)} accepts 3 arguments
     * that <b>must</b> be given by position but two have defaults.
     */
    @Nested
    @DisplayName("with positional-only parameters and default values")
    class PositionalWithDefaults extends Standard {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // descr = Simple.f3pd
            setup("f3pd", new SimpleObject());
            exp = new Object[] {1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_static("f3pd", 3, 3); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type(obj).f3pd(1)
            Object[] args = {1};
            PyTuple r = (PyTuple)descr.__call__(args, null);
            check_result(r);

            // We call obj.f3pd(1)
            r = (PyTuple)func.__call__(args, null);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type(obj).f3pd(1)
            Object[] args = {1};
            String[] names = {};
            PyTuple r = (PyTuple)descr.__call__(args, names);
            check_result(r);

            // We call obj.f3pd(1)
            r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call type(obj).f3pd(1, c=3)
            Object[] args = {1, 3};
            String[] names = {"c"};
            assertRaises(PyExc.TypeError, () -> descr.__call__(args, names));

            // We call obj.f3pd(1, c=3)
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // descr is not expected to support call()
            // We call obj.f3pd(1)
            PyTuple r = (PyTuple)func.call(1);
            check_result(r);
        }
    }

    /**
     * {@link SimpleObject#f3pk(int, String, Object)} accepts 3 arguments
     * that may be given by position or keyword.
     */
    @Nested
    @DisplayName("with positional-or-keyword parameters")
    class PositionalOrKeywordParams extends Standard {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // descr = Simple.f3pk
            setup("f3pk", new SimpleObject());
            exp = new Object[] {1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_static("f3pk", 3, 0); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type(obj).f3pk(1, '2', 3)
            Object[] args = {1, "2", 3};
            String[] names = {};
            PyTuple r = (PyTuple)descr.__call__(args, names);
            check_result(r);

            // We call obj.f3pk(1, '2', 3)
            r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type(obj).f3pk(1, c=3, b='2')
            Object[] args = {1, 3, "2"};
            String[] names = {"c", "b"};
            PyTuple r = (PyTuple)descr.__call__(args, names);
            check_result(r);

            // We call obj.f3pk(1, c=3, b='2')
            r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() throws Throwable {
            // We call type(obj).f3pk(1, c=3, b='2', x=4)
            Object[] args = {1, 3, "2", 4};
            String[] names = {"c", "b", /* unknown */"x"};
            assertRaises(PyExc.TypeError, () -> descr.__call__(args, names));

            // We call obj.f3pk(1, c=3, b='2', x=4)
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // descr is not expected to support call()
            // We call obj.f3pk(1, '2', 3)
            PyTuple r = (PyTuple)func.call(1, "2", 3);
            check_result(r);
        }
    }

    /**
     * {@link SimpleObject#f3p2(int, String, Object)} accepts 3 arguments,
     * two of which may be given by position only, and the last by either
     * position or keyword.
     */
    @Nested
    @DisplayName("with two positional-only parameters")
    class SomePositionalOnlyParams extends Standard {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            // descr = Simple.f3p2
            setup("f3p2", new SimpleObject());
            exp = new Object[] {1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_static("f3p2", 3, 2); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type(obj).f3p2(1, '2', 3)
            Object[] args = {1, "2", 3};
            String[] names = {};
            PyTuple r = (PyTuple)descr.__call__(args, names);
            check_result(r);

            // We call obj.f3p2(1, '2', 3)
            r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        /** To set {@code c} by keyword is a ok. */
        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type(obj).f3p2(1, '2', c=3)
            Object[] args = {1, "2", 3};
            String[] names = {"c"};
            PyTuple r = (PyTuple)descr.__call__(args, names);
            check_result(r);

            // We call obj.f3p2(1, '2', c=3)
            r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() throws Throwable {
            // We call type(obj).f3p2(1, c=3, b='2')
            Object[] args = {1, 3, "2"};
            String[] names = {"c", /* positional */"b"};
            assertRaises(PyExc.TypeError, () -> descr.__call__(args, names));

            // We call obj.f3p2(1, c=3, b='2')
            assertRaises(PyExc.TypeError, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // descr is not expected to support call()
            // We call obj.f3p2(1, '2', 3)
            PyTuple r = (PyTuple)func.call(1, "2", 3);
            check_result(r);
        }
    }
}
