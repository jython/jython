// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.python.core.Exposed.PositionalOnly;
import org.python.core.Exposed.PythonMethod;
import org.python.core.Exposed.PythonStaticMethod;
import org.python.base.MethodKind;

/**
 * Test that functions exposed by a Python <b>module</b> defined in
 * Java, using the scheme of annotations defined in {@link Exposed},
 * result in {@link PyJavaFunction} objects with characteristics
 * that correspond to the definition.
 * <p>
 * The first test in each case is to examine the fields in the
 * parser that attaches to the {@link ModuleDef.MethodDef}. Then we
 * call the function using the {@code __call__} special method, and
 * using our "Java call" signatures.
 * <p>
 * There is a nested test suite for each signature pattern.
 */
@DisplayName("A method exposed by a module")
class ModuleExposerMethodTest {

    /**
     * Nested test classes implement these as standard. A base class
     * here is just a way to describe the tests once that we repeat in
     * each nested case.
     */
    abstract static class Standard {

        // Working variables for the tests
        /** The module we create. */
        final PyModule module;
        /** The function to examine or call. */
        PyJavaFunction func;
        /** The parser in the function we examine. */
        ArgParser ap;
        /** The expected result of calling the function */
        Object[] exp;

        Standard() {
            this.module = new ExampleModule();
            this.module.exec();
        }

        /**
         * A parser attached to the function object should have field values
         * that correctly reflect the signature and annotations in the
         * defining class.
         */
        abstract void has_expected_fields();

        /**
         * Call the function using the {@code __call__} special method with
         * arguments correct for the function's specification. The function
         * should obtain the correct result (and not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void supports__call__() throws Throwable;

        /**
         * Call the function using the {@code __call__} special method with
         * arguments correct for the function's specification, and
         * explicitly zero or more keywords. The function should obtain the
         * correct result (and not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void supports_keywords() throws Throwable;

        /**
         * Call the function using the {@code __call__} special method and
         * an unexpected keyword: where none is expected, for a positional
         * argument, or simply an unacceptable name. The function should
         * throw {@link TypeError}.
         *
         * @throws Throwable unexpectedly
         */
        abstract void raises_TypeError_on_unexpected_keyword() throws Throwable;

        /**
         * Call the function using the Java call interface with arguments
         * correct for the function's specification. The function should
         * obtain the correct result (and not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void supports_java_call() throws Throwable;

        /**
         * Check that the fields of the parser match expectations for a
         * method with no collector parameters and a certain number of
         * positional-only parameters.
         *
         * @param kind static or instance
         * @param name of method
         * @param count of parameters
         * @param posonlycount count of positional-only parameters
         */
        void no_collector(MethodKind kind, String name, int count, int posonlycount) {
            assertEquals(name, ap.name);
            assertEquals(kind, ap.methodKind);
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
         * static method with no collector parameters and a certain number
         * of positional-only parameters.
         *
         * @param name of method
         * @param count of parameters
         * @param posonly count of positional-only parameters
         */
        void no_collector_static(String name, int count, int posonly) {
            no_collector(MethodKind.STATIC, name, count, posonly);
        }

        /**
         * Check that the fields of the parser match expectations for a
         * instance method with no collector parameters and a certain number
         * of positional-only parameters.
         *
         * @param name of method
         * @param count of parameters
         * @param posonly count of positional-only parameters
         */
        void no_collector_instance(String name, int count, int posonly) {
            no_collector(MethodKind.INSTANCE, name, count, posonly);
        }

        /**
         * Check the result of a call against {@link #exp}. The reference
         * rtesult is the same throughout a given sub-class test.
         *
         * @param result of call
         */
        void check_result(PyTuple result) { assertArrayEquals(exp, result.value); }
    }

    /**
     * A Python module definition that exhibits a range of method
     * signatures explored in the tests.
     */
    static class ExampleModule extends JavaModule {

        static final ModuleDef DEF = new ModuleDef("example", MethodHandles.lookup());

        ExampleModule() { super(DEF); }

        /**
         * See {@link StaticNoParams}: no parameters are allowed.
         */
        @PythonStaticMethod
        static void f0() {}

        /**
         * See {@link NoParams}: no parameters are allowed.
         */
        @PythonMethod
        void m0() {}

        /**
         * See {@link StaticOneParam}: the parameter is positional-only as a
         * result of the default exposure.
         *
         * @param a positional arg
         * @return the arg (tuple)
         */
        @PythonStaticMethod
        static PyTuple f1(double a) { return Py.tuple(a); }

        /**
         * See {@link OneParam}: the parameter is positional-only as a
         * result of the default exposure.
         *
         * @param a positional arg
         * @return the arg (tuple)
         */
        @PythonMethod
        PyTuple m1(double a) { return Py.tuple(this, a); }

        /**
         * See {@link StaticDefaultPositionalParams}: the parameters are
         * positional-only as a result of the default exposure.
         *
         * @param a positional arg
         * @param b positional arg
         * @param c positional arg
         * @return the args
         */
        @PythonStaticMethod
        static PyTuple f3(int a, String b, Object c) { return Py.tuple(a, b, c); }

        /**
         * See {@link DefaultPositionalParams}: the parameters are
         * positional-only as a result of the default exposure.
         *
         * @param a positional arg
         * @param b positional arg
         * @param c positional arg
         * @return the args
         */
        @PythonMethod
        PyTuple m3(int a, String b, Object c) { return Py.tuple(this, a, b, c); }

        /**
         * See {@link StaticPositionalOrKeywordParams}: the parameters are
         * positional-or-keyword but none are positional-only.
         *
         * @param a positional-or-keyword arg
         * @param b positional-or-keyword arg
         * @param c positional-or-keyword arg
         * @return the args
         */
        @PythonStaticMethod(positionalOnly = false)
        static PyTuple f3pk(int a, String b, Object c) { return Py.tuple(a, b, c); }

        /**
         * See {@link PositionalOrKeywordParams}: the parameters are
         * positional-or-keyword but none are positional-only.
         *
         * @param a positional-or-keyword arg
         * @param b positional-or-keyword arg
         * @param c positional-or-keyword arg
         * @return the args
         */
        @PythonMethod(positionalOnly = false)
        PyTuple m3pk(int a, String b, Object c) { return Py.tuple(this, a, b, c); }

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

        /**
         * See {@link StaticSomePositionalOnlyParams}: two parameters are
         * positional-only as a result of an annotation.
         *
         * @param a positional arg
         * @param b positional arg
         * @param c positional-or-keyword arg
         * @return the args
         */
        @PythonMethod
        PyTuple m3p2(int a, @PositionalOnly String b, Object c) { return Py.tuple(this, a, b, c); }
    }

    /** {@link ExampleModule#m0()} accepts no arguments. */
    @Nested
    @DisplayName("with no parameters")
    class NoParams extends Standard {

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // func = module.m0
            func = (PyJavaFunction)Abstract.getAttr(module, "m0");
            ap = func.argParser;
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_instance("m0", 0, 0); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call func()
            Object[] args = {};

            // The method is declared void (which means return None)
            Object r = func.__call__(args, null);
            assertEquals(Py.None, r);
        }

        /** Keywords must be empty. */
        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call func()
            Object[] args = {};
            String[] names = {};

            // The method is declared void (which means return None)
            Object r = func.__call__(args, names);
            assertEquals(Py.None, r);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call func(c=3)
            Object[] args = {3};
            String[] names = {"c"}; // Nothing expected

            assertThrows(TypeError.class, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call func()
            // The method is declared void (which means return None)
            Object r = func.call();
            assertEquals(Py.None, r);
        }
    }

    /** {@link ExampleModule#f0()} accepts no arguments. */
    @Nested
    @DisplayName("static, with no parameters")
    class StaticNoParams extends NoParams {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // func = module.f0
            func = (PyJavaFunction)Abstract.getAttr(module, "f0");
            ap = func.argParser;
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_static("f0", 0, 0); }
    }

    /**
     * {@link ExampleModule#m1(double)} accepts one argument that
     * <b>must</b> be given by position.
     */
    @Nested
    @DisplayName("with one positional-only parameter")
    class OneParam extends Standard {

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // func = module.m1
            func = (PyJavaFunction)Abstract.getAttr(module, "m1");
            ap = func.argParser;
            exp = new Object[] {module, 42.0};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_instance("m1", 1, 1); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call func(42.0)
            Object[] args = {42.0};
            // The method reports its arguments as a tuple
            PyTuple r = (PyTuple)func.__call__(args, null);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call func(42.0)
            Object[] args = {42.0};
            String[] names = {};
            // The method reports its arguments as a tuple
            PyTuple r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call func(42.0, a=5)
            Object[] args = {42.0, 5};
            String[] names = {"a"};

            assertThrows(TypeError.class, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call func(42.0)
            PyTuple r = (PyTuple)func.call(42.0);
            check_result(r);
        }
    }

    /**
     * {@link ExampleModule#f1(double)} accepts one argument that
     * <b>must</b> be given by position.
     */
    @Nested
    @DisplayName("static, with one positional-only parameter")
    class StaticOneParam extends OneParam {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // func = module.f1
            func = (PyJavaFunction)Abstract.getAttr(module, "f1");
            ap = func.argParser;
            exp = new Object[] {42.0};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_static("f1", 1, 1); }
    }

    /**
     * {@link ExampleModule#m3(int, String, Object)} accepts 3 arguments
     * that <b>must</b> be given by position.
     */
    @Nested
    @DisplayName("with positional-only parameters by default")
    class DefaultPositionalParams extends Standard {

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // func = module.m3
            func = (PyJavaFunction)Abstract.getAttr(module, "m3");
            ap = func.argParser;
            exp = new Object[] {module, 1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_instance("m3", 3, 3); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call func(1, '2', 3)
            Object[] args = {1, "2", 3};
            // The method reports its arguments as a tuple
            PyTuple r = (PyTuple)func.__call__(args, null);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call func(1, '2', 3)
            Object[] args = {1, "2", 3};
            String[] names = {};
            // The method reports its arguments as a tuple
            PyTuple r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call func(1, '2', c=3)
            Object[] args = {1, "2", 3};
            String[] names = {"c"};

            assertThrows(TypeError.class, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call func(1, '2', 3)
            PyTuple r = (PyTuple)func.call(1, "2", 3);
            check_result(r);
        }
    }

    /**
     * {@link ExampleModule#f3(int, String, Object)} accepts 3 arguments
     * that <b>must</b> be given by position.
     */
    @Nested
    @DisplayName("static, with positional-only parameters by default")
    class StaticDefaultPositionalParams extends DefaultPositionalParams {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // func = module.f3
            func = (PyJavaFunction)Abstract.getAttr(module, "f3");
            ap = func.argParser;
            exp = new Object[] {1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_static("f3", 3, 3); }
    }

    /**
     * {@link ExampleModule#m3pk(int, String, Object)} accepts 3
     * arguments that may be given by position or keyword.
     */
    @Nested
    @DisplayName("with positional-or-keyword parameters")
    class PositionalOrKeywordParams extends Standard {

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // func = module.m3pk
            func = (PyJavaFunction)Abstract.getAttr(module, "m3pk");
            ap = func.argParser;
            exp = new Object[] {module, 1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_instance("m3pk", 3, 0); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call func(1, '2', 3)
            Object[] args = {1, "2", 3};
            String[] names = {};
            PyTuple r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        /** Supply second and third arguments by keyword. */
        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call func(1, c=3, b='2')
            Object[] args = {1, 3, "2"};
            String[] names = {"c", "b"};
            PyTuple r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        /** Get the wrong keyword. */
        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() throws Throwable {
            // We call func(1, c=3, b='2', x=4)
            Object[] args = {1, 3, "2", 4};
            String[] names = {"c", "b", /* unknown */"x"};
            assertThrows(TypeError.class, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            PyTuple r = (PyTuple)func.call(1, "2", 3);
            check_result(r);
        }
    }

    /**
     * {@link ExampleModule#f3pk(int, String, Object)} accepts 3
     * arguments that may be given by position or keyword.
     */
    @Nested
    @DisplayName("static, with positional-or-keyword parameters")
    class StaticPositionalOrKeywordParams extends PositionalOrKeywordParams {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // func = module.f3pk
            func = (PyJavaFunction)Abstract.getAttr(module, "f3pk");
            ap = func.argParser;
            exp = new Object[] {1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_static("f3pk", 3, 0); }

    }

    /**
     * {@link ExampleModule#m3p2(int, String, Object)} accepts 3
     * arguments, two of which may be given by position only, and the
     * last by either position or keyword.
     */
    @Nested
    @DisplayName("with two positional-only parameters")
    class SomePositionalOnlyParams extends Standard {

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // func = module.m3p2
            func = (PyJavaFunction)Abstract.getAttr(module, "m3p2");
            ap = func.argParser;
            exp = new Object[] {module, 1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_instance("m3p2", 3, 2); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call func(1, '2', 3)
            Object[] args = {1, "2", 3};
            String[] names = {};

            // The method just parrots its arguments as a tuple
            PyTuple r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        /** Supply third argument by keyword. */
        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call func(1, '2', c=3)
            Object[] args = {1, "2", 3};
            String[] names = {"c"};

            // The method reports its arguments as a tuple
            PyTuple r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() throws Throwable {
            // We call func(1, c=3, b='2')
            Object[] args = {1, 3, "2"};
            String[] names = {"c", /* positional */"b"};
            assertThrows(TypeError.class, () -> func.__call__(args, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // The method reports its arguments as a tuple
            PyTuple r = (PyTuple)func.call(1, "2", 3);
            check_result(r);
        }
    }

    /**
     * {@link ExampleModule#f3p2(int, String, Object)} accepts 3
     * arguments, two of which may be given by position only, and the
     * last by either position or keyword.
     */
    @Nested
    @DisplayName("static, with two positional-only parameters")
    class StaticSomePositionalOnlyParams extends SomePositionalOnlyParams {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // func = module.f3p2
            func = (PyJavaFunction)Abstract.getAttr(module, "f3p2");
            ap = func.argParser;
            exp = new Object[] {1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_static("f3p2", 3, 2); }
    }
}
