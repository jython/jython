package org.python.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.python.base.MethodKind;
import org.python.core.Exposed.Default;
import org.python.core.Exposed.PositionalOnly;
import org.python.core.Exposed.PythonMethod;
import org.python.core.PyType.Spec;

/**
 * Test that methods exposed by a Python <b>type</b> defined in
 * Java, using the scheme of annotations defined in {@link Exposed},
 * result in method descriptors with characteristics that correspond
 * to their definitions.
 * <p>
 * The first test in each case is to examine the fields in the
 * parser that attaches to the {@link ModuleDef.MethodDef}. Then we
 * call the function using the {@code __call__} special method, and
 * using our "fast call" signatures.
 * <p>
 * There is a nested test suite for each signature pattern.
 */
@DisplayName("A method exposed by a type")
class TypeExposerMethodTest {

    /**
     * Certain nested test classes implement these as standard. A base
     * class here is just a way to describe the tests once that reappear
     * in each nested case.
     */
    abstract static class Standard {

        // Working variables for the tests
        /** Unbound descriptor by type access to examine or call. */
        PyMethodDescr descr;
        /** The object on which to invoke the method. */
        Object obj;
        /** The function to examine or call (bound to {@code obj}). */
        PyJavaFunction func;
        /** The parser we examine. */
        ArgParser ap;
        /** The expected result of calling the method. */
        Object[] exp;

        /**
         * A parser attached to the method descriptor should have field
         * values that correctly reflect the signature and annotations in
         * the defining class.
         */
        abstract void has_expected_fields();

        /**
         * Call the function using the {@code __call__} special method with
         * arguments correct for the method's specification. The method
         * should obtain the correct result (and not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void supports__call__() throws Throwable;

        /**
         * Call the method using the {@code __call__} special method with
         * arguments correct for the method's specification, and explicitly
         * zero or more keywords. The method should obtain the correct
         * result (and not throw).
         *
         * @throws Throwable unexpectedly
         */
        abstract void supports_keywords() throws Throwable;

        /**
         * Call the method using the {@code __call__} special method and an
         * unexpected keyword: where none is expected, for a positional
         * argument, or simply an unacceptable name. The method should throw
         * {@link TypeError}.
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
         * Helper to set up each test.
         *
         * @param name of the method
         * @param o to use as the self argument
         * @throws AttributeError if method not found
         * @throws Throwable other errors
         */
        void setup(String name, Object o) throws AttributeError, Throwable {
            descr = (PyMethodDescr)PyType.of(o).lookup(name);
            ap = descr.argParser;
            obj = o;
            func = (PyJavaFunction)Abstract.getAttr(obj, name);
        }

        /**
         * Check the result of a call against {@link #exp}. The reference
         * result is the same throughout a given sub-class test.
         *
         * @param result of call
         */
        void check_result(PyTuple result) { assertArrayEquals(exp, result.value); }

    }

    /**
     * A Python type definition that exhibits a range of method
     * signatures explored in the tests. Methods named {@code m*()} are
     * instance methods to Python, declared to Java as either instance
     * methods ({@code this} is {@code self}) or as static methods
     * ({@code self} is the first parameter).
     */
    static class SimpleObject {

        static PyType TYPE = PyType.fromSpec(new Spec("Simple", MethodHandles.lookup()));

        /**
         * See {@link NoParams}: no parameters are allowed (after
         * {@code self}).
         */
        @PythonMethod
        void m0() {}

        /**
         * See {@link OnePos}: a single positional parameter
         *
         * @param a positional arg
         * @return the arg (tuple)
         */
        @PythonMethod
        PyTuple m1(double a) { return Py.tuple(this, a); }

        /**
         * See {@link PositionalByDefault}: the parameters are
         * positional-only as a result of the default exposure. Use static
         * style, arbitrarily.
         *
         * @param self target
         * @param a positional arg
         * @param b positional arg
         * @param c positional arg
         * @return the args
         */
        @PythonMethod
        static PyTuple m3(SimpleObject self, int a, String b, Object c) {
            return Py.tuple(self, a, b, c);
        }

        /**
         * See {@link PositionalWithDefaults}: the parameters are
         * positional-only as a result of the default exposure. Use static
         * style, arbitrarily.
         *
         * @param self target
         * @param a positional arg
         * @param b positional arg = 2
         * @param c positional arg = 3
         * @return the args
         */
        @PythonMethod
        static PyTuple m3pd(SimpleObject self, int a, @Default("2") String b,
                @Default("3") Object c) {
            return Py.tuple(self, a, b, c);
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
        @PythonMethod
        PyTuple m3p2(int a, @PositionalOnly String b, Object c) { return Py.tuple(this, a, b, c); }
    }

    /**
     * A Python type definition that exhibits a range of method
     * signatures explored in the tests, and has a an adopted
     * implementation {@link ExampleObject2}. Methods named {@code m*()}
     * are instance methods to Python, declared to Java as either
     * instance methods ({@code this} is {@code self}) or as static
     * methods ({@code self} is the first parameter).
     */
    static class ExampleObject {

        static PyType TYPE = PyType.fromSpec( //
                new Spec("Example", MethodHandles.lookup()) //
                        .adopt(ExampleObject2.class));

        /**
         * See {@link NoParams}: no parameters are allowed (after
         * {@code self}).
         */
        @PythonMethod(primary = false)
        void m0() {}

        @SuppressWarnings("unused")
        @PythonMethod
        static void m0(ExampleObject2 self) {}

        /**
         * See {@link OnePos}: a single positional parameter
         *
         * @param a positional arg
         * @return the args
         */
        @PythonMethod
        PyTuple m1(double a) { return Py.tuple(this, a); }

        @PythonMethod(primary = false)
        static PyTuple m1(ExampleObject2 self, double a) { return Py.tuple(self, a); }

        /**
         * See {@link PositionalByDefault}: the parameters are
         * positional-only as a result of the default exposure.
         *
         * @param a positional arg
         * @param b positional arg
         * @param c positional arg
         * @return the args
         */
        @PythonMethod
        PyTuple m3(int a, String b, Object c) { return Py.tuple(this, a, b, c); }

        @PythonMethod(primary = false)
        static PyTuple m3(ExampleObject2 self, int a, String b, Object c) {
            return Py.tuple(self, a, b, c);
        }

        /**
         * See {@link PositionalWithDefaults}: the parameters are
         * positional-only as a result of the default exposure. Use static
         * style, arbitrarily.
         *
         * @param a positional arg
         * @param b positional arg = 2
         * @param c positional arg = 3
         * @return the args
         */
        @PythonMethod
        PyTuple m3pd(int a, @Default("2") String b, @Default("3") Object c) {
            return Py.tuple(this, a, b, c);
        }

        /**
         * Secondary definition does not repeat annotations.
         *
         * @param self target
         * @param a positional arg
         * @param b positional arg = 2
         * @param c positional arg = 3
         * @return the args
         */
        @PythonMethod(primary = false)
        static PyTuple m3pd(ExampleObject2 self, int a, String b, Object c) {
            return Py.tuple(self, a, b, c);
        }

        /**
         * See {@link PositionalOrKeywordParams}: the parameters are
         * positional-or-keyword but none are positional-only.
         *
         * @param a positional arg
         * @param b positional arg
         * @param c positional-or-keyword arg
         * @return the args
         */
        @PythonMethod(positionalOnly = false)
        PyTuple m3pk(int a, String b, Object c) { return Py.tuple(this, a, b, c); }

        @PythonMethod(primary = false)
        static PyTuple m3pk(ExampleObject2 self, int a, String b, Object c) {
            return Py.tuple(self, a, b, c);
        }

        /**
         * See {@link SomePositionalOnlyParams}: two parameters are
         * positional-only as a result of an annotation.
         *
         * @param a positional arg
         * @param b positional arg
         * @param c positional-or-keyword arg
         * @return the args
         */
        @PythonMethod
        PyTuple m3p2(int a, @PositionalOnly String b, Object c) { return Py.tuple(this, a, b, c); }

        @PythonMethod(primary = false)
        static PyTuple m3p2(ExampleObject2 self, int a, String b, Object c) {
            return Py.tuple(self, a, b, c);
        }
    }

    /**
     * Class cited as an "adopted implementation" of
     * {@link ExampleObject}
     */
    static class ExampleObject2 {
        // Ensure canonical counterpart is initialised
        @SuppressWarnings("unused")
        private static PyType CANONICAL = ExampleObject.TYPE;
    }

    /** {@link SimpleObject#m0()} accepts no arguments. */
    @Nested
    @DisplayName("with no parameters")
    class NoParams extends Standard {

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Simple.m0
            setup("m0", new SimpleObject());
            // The method is declared void (which means return None)
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_instance("m0", 0, 0); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type(obj).m0(obj)
            Object[] args = {obj};
            Object r = descr.__call__(args, null);
            assertEquals(Py.None, r);

            // We call obj.m0()
            args = new Object[0];
            r = func.__call__(args, null);
            assertEquals(Py.None, r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type(obj).m0(obj)
            Object[] args = {obj};
            String[] names = {};
            Object r = descr.__call__(args, names);
            assertEquals(Py.None, r);

            // We call obj.m0()
            args = new Object[0];
            r = func.__call__(args, names);
            assertEquals(Py.None, r);
        }

        /** To set anything by keyword is a {@code TypeError}. */
        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call type(obj).m0(obj, c=3)
            Object[] args = {obj, 3};
            String[] names = {"c"};
            assertThrows(TypeError.class, () -> descr.__call__(args, names));

            // We call obj.m0(c=3)
            Object[] args2 = Arrays.copyOfRange(args, 1, args.length);
            assertThrows(TypeError.class, () -> func.__call__(args2, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call type(obj).m0(obj)
            Object r = descr.call(obj);
            assertEquals(Py.None, r);

            // We call obj.m0()
            r = func.call();
            assertEquals(Py.None, r);
        }
    }

    /**
     * {@link NoParams} with {@link ExampleObject} as the
     * implementation.
     */
    @Nested
    @DisplayName("with no parameters" + " (canonical)")
    class NoParams1 extends NoParams {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Example.m0
            setup("m0", new ExampleObject());
        }
    }

    /**
     * {@link NoParams} with {@link ExampleObject2} as the
     * implementation.
     */
    @Nested
    @DisplayName("with no parameters" + " (adopted)")
    class NoParams2 extends NoParams {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Example.m0
            setup("m0", new ExampleObject2());
        }
    }

    /**
     * {@link SimpleObject#m1(double)} accepts 1 argument that
     * <b>must</b> be given by position.
     */
    @Nested
    @DisplayName("with a single positional-only parameter by default")
    class OnePos extends Standard {

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Simple.m1
            setup("m1", new SimpleObject());
            exp = new Object[] {obj, 42.0};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_instance("m1", 1, 1); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type(obj).m1(obj, 42.0)
            Object[] args = {obj, 42.0};
            PyTuple r = (PyTuple)descr.__call__(args, null);
            check_result(r);

            // We call obj.m1(42.0)
            args = Arrays.copyOfRange(args, 1, args.length);
            r = (PyTuple)func.__call__(args, null);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type(obj).m1(obj, 42.0)
            Object[] args = {obj, 42.0};
            String[] names = {};
            PyTuple r = (PyTuple)descr.__call__(args, names);
            check_result(r);

            // We call obj.m1(42.0)
            args = Arrays.copyOfRange(args, 1, args.length);
            r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call type(obj).m1(obj, a=42.0)
            Object[] args = {obj, 42.0};
            String[] names = {"a"};
            assertThrows(TypeError.class, () -> descr.__call__(args, names));

            // We call obj.m1(a=42.0)
            Object[] args2 = Arrays.copyOfRange(args, 1, args.length);
            assertThrows(TypeError.class, () -> func.__call__(args2, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call type(obj).m1(obj, 42.0)
            PyTuple r = (PyTuple)descr.call(obj, 42.0);
            check_result(r);

            // We call obj.m1(obj, 42.0)
            r = (PyTuple)func.call(42.0);
            check_result(r);
        }
    }

    /**
     * {@link OnePos} with {@link ExampleObject} as the implementation.
     */
    @Nested
    @DisplayName("with a single positional-only parameter by default" + " (canonical)")
    class OnePos1 extends OnePos {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Example.m1
            setup("m1", new ExampleObject());
            exp = new Object[] {obj, 42.0};
        }
    }

    /**
     * {@link OnePos} with {@link ExampleObject2} as the implementation.
     */
    @Nested
    @DisplayName("with a single positional-only parameter by default" + " (adopted)")
    class OnePos2 extends OnePos {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Example.m1
            setup("m1", new ExampleObject2());
            exp = new Object[] {obj, 42.0};
        }
    }

    /**
     * {@link SimpleObject#m3(SimpleObject, int, String, Object)}
     * accepts 3 arguments that <b>must</b> be given by position.
     */
    @Nested
    @DisplayName("with positional-only parameters by default")
    class PositionalByDefault extends Standard {

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Simple.m3
            setup("m3", new SimpleObject());
            exp = new Object[] {obj, 1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_instance("m3", 3, 3); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type(obj).m3(obj, 1, '2', 3)
            Object[] args = {obj, 1, "2", 3};
            PyTuple r = (PyTuple)descr.__call__(args, null);
            check_result(r);

            // We call obj.m3(1, '2', 3)
            args = Arrays.copyOfRange(args, 1, args.length);
            r = (PyTuple)func.__call__(args, null);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type(obj).m3(obj, 1, '2', 3)
            Object[] args = {obj, 1, "2", 3};
            String[] names = {};
            PyTuple r = (PyTuple)descr.__call__(args, names);
            check_result(r);

            // We call obj.m3(1, '2', 3)
            args = Arrays.copyOfRange(args, 1, args.length);
            r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call type(obj).m3(obj, 1, '2', c=3)
            Object[] args = {obj, 1, "2", 3};
            String[] names = {"c"};
            assertThrows(TypeError.class, () -> descr.__call__(args, names));

            // We call obj.m3(1, '2', c=3)
            Object[] args2 = Arrays.copyOfRange(args, 1, args.length);
            assertThrows(TypeError.class, () -> func.__call__(args2, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call type(obj).m3(obj, 1, '2', 3)
            PyTuple r = (PyTuple)descr.call(obj, 1, "2", 3);
            check_result(r);

            // We call obj.m3(obj, 1, '2', 3)
            r = (PyTuple)func.call(1, "2", 3);
            check_result(r);
        }
    }

    /**
     * {@link PositionalByDefault} with {@link ExampleObject} as the
     * implementation.
     */
    @Nested
    @DisplayName("with positional-only parameters by default" + " (canonical)")
    class PositionalByDefault1 extends PositionalByDefault {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Example.m3
            setup("m3", new ExampleObject());
            exp = new Object[] {obj, 1, "2", 3};
        }
    }

    /**
     * {@link PositionalByDefault} with {@link ExampleObject2} as the
     * implementation.
     */
    @Nested
    @DisplayName("with positional-only parameters by default" + " (adopted)")
    class PositionalByDefault2 extends PositionalByDefault {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Example.m3
            setup("m3", new ExampleObject2());
            exp = new Object[] {obj, 1, "2", 3};
        }
    }

    /**
     * {@link SimpleObject#m3pd(SimpleObject, int, String, Object)}
     * accepts 3 arguments that <b>must</b> be given by position but two
     * have defaults.
     */
    @Nested
    @DisplayName("with positional-only parameters and default values")
    class PositionalWithDefaults extends Standard {

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Simple.m3pd
            setup("m3pd", new SimpleObject());
            exp = new Object[] {obj, 1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_instance("m3pd", 3, 3); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type(obj).m3pd(obj, 1)
            Object[] args = {obj, 1};
            PyTuple r = (PyTuple)descr.__call__(args, null);
            check_result(r);

            // We call obj.m3pd(1)
            args = Arrays.copyOfRange(args, 1, args.length);
            r = (PyTuple)func.__call__(args, null);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type(obj).m3pd(obj, 1)
            Object[] args = {obj, 1};
            String[] names = {};
            PyTuple r = (PyTuple)descr.__call__(args, names);
            check_result(r);

            // We call obj.m3pd(1)
            args = Arrays.copyOfRange(args, 1, args.length);
            r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() {
            // We call type(obj).m3pd(obj, 1, c=3)
            Object[] args = {obj, 1, 3};
            String[] names = {"c"};
            assertThrows(TypeError.class, () -> descr.__call__(args, names));

            // We call obj.m3pd(1, c=3)
            Object[] args2 = Arrays.copyOfRange(args, 1, args.length);
            assertThrows(TypeError.class, () -> func.__call__(args2, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call type(obj).m3pd(obj, 1)
            PyTuple r = (PyTuple)descr.call(obj, 1);
            check_result(r);

            // We call obj.m3pd(obj, 1)
            r = (PyTuple)func.call(1);
            check_result(r);
        }
    }

    /**
     * {@link PositionalWithDefaults} with {@link ExampleObject} as the
     * implementation.
     */
    @Nested
    @DisplayName("with positional-only parameters and default values" + " (canonical)")
    class PositionalWithDefaults1 extends PositionalWithDefaults {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Example.m3pd
            setup("m3pd", new ExampleObject());
            exp = new Object[] {obj, 1, "2", 3};
        }
    }

    /**
     * {@link PositionalWithDefaults} with {@link ExampleObject2} as the
     * implementation.
     */
    @Nested
    @DisplayName("with positional-only parameters and default values" + " (adopted)")
    class PositionalWithDefaults2 extends PositionalWithDefaults {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Example.m3pd
            setup("m3pd", new ExampleObject2());
            exp = new Object[] {obj, 1, "2", 3};
        }
    }

    /**
     * {@link SimpleObject#m3pk(int, String, Object)} accepts 3
     * arguments that may be given by position or keyword.
     */
    @Nested
    @DisplayName("with positional-or-keyword parameters")
    class PositionalOrKeywordParams extends Standard {

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Simple.m3pk
            setup("m3pk", new SimpleObject());
            exp = new Object[] {obj, 1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_instance("m3pk", 3, 0); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type(obj).m3pk(obj, 1, '2', 3)
            Object[] args = {obj, 1, "2", 3};
            String[] names = {};
            PyTuple r = (PyTuple)descr.__call__(args, names);
            check_result(r);

            // We call obj.m3pk(1, '2', 3)
            args = Arrays.copyOfRange(args, 1, args.length);
            r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type(obj).m3pk(obj, 1, c=3, b='2')
            Object[] args = {obj, 1, 3, "2"};
            String[] names = {"c", "b"};
            PyTuple r = (PyTuple)descr.__call__(args, names);
            check_result(r);

            // We call obj.m3pk(1, c=3, b='2')
            args = Arrays.copyOfRange(args, 1, args.length);
            r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() throws Throwable {
            // We call type(obj).m3pk(obj, 1, c=3, b='2', x=4)
            Object[] args = {obj, 1, 3, "2", 4};
            String[] names = {"c", "b", /* unknown */"x"};
            assertThrows(TypeError.class, () -> descr.__call__(args, names));

            // We call obj.m3pk(1, c=3, b='2', x=4)
            Object[] args2 = Arrays.copyOfRange(args, 1, args.length);
            assertThrows(TypeError.class, () -> func.__call__(args2, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call type(obj).m3pk(obj, 1, '2', 3)
            PyTuple r = (PyTuple)descr.call(obj, 1, "2", 3);
            check_result(r);

            // We call obj.m3pk(1, '2', 3)
            r = (PyTuple)func.call(1, "2", 3);
            check_result(r);
        }
    }

    /**
     * {@link PositionalOrKeywordParams} with {@link ExampleObject} as
     * the implementation.
     */
    @Nested
    @DisplayName("with positional-or-keyword parameters" + " (canonical)")
    class PositionalOrKeywordParams1 extends PositionalOrKeywordParams {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Example.m3pk
            setup("m3pk", new ExampleObject());
            exp = new Object[] {obj, 1, "2", 3};
        }
    }

    /**
     * {@link PositionalOrKeywordParams} with {@link ExampleObject2} as
     * the implementation.
     */
    @Nested
    @DisplayName("with positional-or-keyword parameters" + " (adopted)")
    class PositionalOrKeywordParams2 extends PositionalOrKeywordParams {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Example.m3pk
            setup("m3pk", new ExampleObject2());
            exp = new Object[] {obj, 1, "2", 3};
        }
    }

    /**
     * {@link SimpleObject#m3p2(int, String, Object)} accepts 3
     * arguments, two of which may be given by position only, and the
     * last by either position or keyword.
     */
    @Nested
    @DisplayName("with two positional-only parameters")
    class SomePositionalOnlyParams extends Standard {

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Simple.m3p2
            setup("m3p2", new SimpleObject());
            exp = new Object[] {obj, 1, "2", 3};
        }

        @Override
        @Test
        void has_expected_fields() { no_collector_instance("m3p2", 3, 2); }

        @Override
        @Test
        void supports__call__() throws Throwable {
            // We call type(obj).m3p2(obj, 1, '2', 3)
            Object[] args = {obj, 1, "2", 3};
            String[] names = {};
            PyTuple r = (PyTuple)descr.__call__(args, names);
            check_result(r);
        }

        /** To set {@code c} by keyword is a ok. */
        @Override
        @Test
        void supports_keywords() throws Throwable {
            // We call type(obj).m3p2(obj, 1, '2', c=3)
            Object[] args = {obj, 1, "2", 3};
            String[] names = {"c"};
            PyTuple r = (PyTuple)descr.__call__(args, names);
            check_result(r);

            // We call obj.m3p2(1, '2', c=3)
            args = Arrays.copyOfRange(args, 1, args.length);
            r = (PyTuple)func.__call__(args, names);
            check_result(r);
        }

        @Override
        @Test
        void raises_TypeError_on_unexpected_keyword() throws Throwable {
            // We call type(obj).m3p2(obj, 1, c=3, b='2')
            Object[] args = {obj, 1, 3, "2"};
            String[] names = {"c", /* positional */"b"};
            assertThrows(TypeError.class, () -> descr.__call__(args, names));

            // We call obj.m3p2(1, c=3, b='2')
            Object[] args2 = Arrays.copyOfRange(args, 1, args.length);
            assertThrows(TypeError.class, () -> func.__call__(args2, names));
        }

        @Override
        @Test
        void supports_java_call() throws Throwable {
            // We call type(obj).m3p2(obj, 1, '2', 3)
            PyTuple r = (PyTuple)descr.call(obj, 1, "2", 3);
            check_result(r);

            // We call obj.m3p2(1, '2', 3)
            r = (PyTuple)func.call(1, "2", 3);
            check_result(r);
        }
    }

    /**
     * {@link PositionalOrKeywordParams} with {@link ExampleObject} as
     * the implementation.
     */
    @Nested
    @DisplayName("with two positional-only parameters" + " (canonical)")
    class SomePositionalOnlyParams1 extends SomePositionalOnlyParams {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Example.m3p2
            setup("m3p2", new ExampleObject());
            exp = new Object[] {obj, 1, "2", 3};
        }
    }

    /**
     * {@link PositionalOrKeywordParams} with {@link ExampleObject2} as
     * the implementation.
     */
    @Nested
    @DisplayName("with two positional-only parameters" + " (adopted)")
    class SomePositionalOnlyParams2 extends SomePositionalOnlyParams {

        @Override
        @BeforeEach
        void setup() throws AttributeError, Throwable {
            // descr = Example.m3p2
            setup("m3p2", new ExampleObject2());
            exp = new Object[] {obj, 1, "2", 3};
        }
    }
}
