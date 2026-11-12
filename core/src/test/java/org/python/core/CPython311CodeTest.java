// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.python.core.PyDict.Key;
import org.python.core.PyDict.KeyHolder;
import org.python.modules.MarshalModule;
import org.python.support.InterpreterError;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * Test that we can read code objects from prepared {@code .pyc} files
 * and execute the byte code.
 *
 * These files are prepared in the Gradle build using a compatible
 * version of CPython, from Python source in
 * {@code core/src/test/pythonExample}. To run these in the IDE, first
 * execute the task:<pre>
 * .\gradlew --console=plain core:compileTestPythonExamples
 * </pre>
 */
@DisplayName("Given programs compiled by CPython 3.11 ...")
class CPython311CodeTest extends UnitTestSupport {

    /** Instance of the {@code marshal} module. */
    static final MarshalModule MARSHAL;
    static {
        MARSHAL = new MarshalModule();
        MARSHAL.exec();
    }

    @SuppressWarnings("static-method")
    @DisplayName("marshal can read a code object")
    @ParameterizedTest(name = "from {0}")
    @ValueSource(strings = {"load_store_name", "unary_op", "binary_op", "bool_left_arith",
            "bool_right_arith", "comparison", "tuple_index", "tuple_dot_product", "list_index",
            "simple_if", "multi_if", "simple_loop", "list_dot_product"})
    void loadCodeObject(String name) {
        PyCode code = readCode(name);
        assertPythonType(PyCode.TYPE, code);
    }

    @SuppressWarnings("static-method")
    @DisplayName("marshal can read nested code objects")
    @ParameterizedTest(name = "from {0}")
    @ValueSource(strings = {"function_def", "function_call", "function_closure"})
    void loadNestedCodeObjects(String name) {
        PyCode code = readCode(name);
        assertPythonType(PyCode.TYPE, code);
    }

    @SuppressWarnings("static-method")
    @DisplayName("marshal can read a result object")
    @ParameterizedTest(name = "from {0}")
    @ValueSource(strings = {"load_store_name", "unary_op", "binary_op", "bool_left_arith",
            "bool_right_arith", "comparison", "tuple_index", "tuple_dot_product", "list_index",
            "simple_if", "multi_if", "simple_loop", "list_dot_product"})
    void loadResultDict(String name) {
        PyDict dict = readResultDict(name);
        assertPythonType(PyDict.TYPE, dict);
    }

    @DisplayNameGeneration(DisplayNameGenerator.Simple.class)
    static abstract class CodeAttributes {
        final String name;
        final PyCode code;

        CodeAttributes(String name, String... fnames) {
            // The code object of the module
            this.name = name;
            PyCode code = readCode(name);
            // For each function name given, follow the nesting
            for (String fn : fnames) {
                // Function code objects are amongst the constants
                boolean found = false;
                for (Object o : code.consts) {
                    if (o instanceof PyCode c) {
                        if (c.name.equals(fn)) { found = true; code = c; break; }
                    }
                }
                assert found;
            }
            this.code = code;
        }

        @Test
        void co_cellvars() { assertEquals(0, code.co_cellvars().size()); }

        @Test
        void co_code() {
            // Can't predict, but not zero for CPython examples
            assertNotEquals(0, code.co_code().size());
        }

        @Test
        void co_freevars() { assertEquals(0, code.co_freevars().size()); }

        @Test
        void co_filename() {
            assertTrue(code.filename.contains(name), "file name");
            assertTrue(code.filename.contains(".py"), "file name");
        }

        @Test
        void co_name() { assertEquals("<module>", code.name); }

        void co_names() { checkNames(code.co_names(), EMPTY_STRINGS); }

        @Test
        void co_varnames() { checkNames(code.co_varnames(), EMPTY_STRINGS); }

        /**
         * Check {@code code} name enquiry against the expected list.
         *
         * @param names result from code object
         * @param exp expected names in expected order
         */
        static void checkNames(PyTuple names, String... exp) {
            assertEquals(exp.length, names.size());
            for (int i = 0; i < exp.length; i++) { assertPythonEquals(exp[i], names.get(i)); }
        }

        /**
         * Check {@code code} values enquiry against the expected list.
         *
         * @param values result from code object
         * @param exp expected values in expected order
         */
        static void checkValues(PyTuple values, Object... exp) {
            assertEquals(exp.length, values.size());
            for (int i = 0; i < exp.length; i++) { assertPythonEquals(exp[i], values.get(i)); }
        }
    }

    @Nested
    @DisplayName("A simple code object has expected ...")
    class SimpleCodeAttributes extends CodeAttributes {

        SimpleCodeAttributes() { super("load_store_name"); }

        @Test
        @Override
        void co_names() {
            // Names in order encountered
            assertPythonEquals("a", code.names[0]);
            assertPythonEquals("β", code.names[1]);
            assertPythonEquals("c", code.names[2]);
            assertPythonEquals("ਛਲ", code.names[3]);
        }

        @Test
        void co_consts() {
            // Fairly reliably 3 consts and a None to return
            assertEquals(4, code.consts.length);
        }
    }

    @Nested
    @DisplayName("The code for function_call.g has expected ...")
    class FunctionCodeAttributes extends CodeAttributes {

        FunctionCodeAttributes() { super("function_call", "g"); }

        @Test
        @Override
        void co_name() { assertPythonEquals("g", code.name); }

        @Test
        @Override
        void co_names() { checkNames(code.co_names(), "fmt", "join"); }

        @Test
        @Override
        void co_varnames() {
            checkNames(code.co_varnames(), "a", "b", "c", "d", "r", "s", "t", "args", "kwargs",
                    "parts");
        }

        @Test
        void co_consts() {
            // Sensitive to CPython compiler changes but ...
            checkValues(code.co_consts(), Py.None, "g: a", "b", "c", "d", "args", "r", "s", "t",
                    "kwargs", ", ");
        }
    }

    @Nested
    @DisplayName("The code for function_closure.f1 has expected ...")
    class FunctionCodeAttributes_f1 extends CodeAttributes {

        FunctionCodeAttributes_f1() { super("function_closure", "f1"); }

        @Override
        @Test
        void co_cellvars() { checkNames(code.co_cellvars(), "a", "b", "z"); }

        @Test
        @Override
        void co_name() { assertPythonEquals("f1", code.name); }

        @Test
        @Override
        void co_varnames() { checkNames(code.co_varnames(), "a", "b", "u", "f2"); }
    }

    @Nested
    @DisplayName("The code for function_closure.f2 has expected ...")
    class FunctionCodeAttributes_f2 extends CodeAttributes {

        FunctionCodeAttributes_f2() { super("function_closure", "f1", "f2"); }

        @Override
        @Test
        void co_cellvars() { checkNames(code.co_cellvars(), "c", "y"); }

        @Override
        @Test
        void co_freevars() { checkNames(code.co_freevars(), "a", "b", "z"); }

        @Test
        @Override
        void co_name() { assertPythonEquals("f2", code.name); }

        @Test
        @Override
        void co_varnames() { checkNames(code.co_varnames(), "c", "v", "f3"); }
    }

    @Nested
    @DisplayName("The code for function_closure.f3 has expected ...")
    class FunctionCodeAttributes_f3 extends CodeAttributes {

        FunctionCodeAttributes_f3() { super("function_closure", "f1", "f2", "f3"); }

        @Override
        @Test
        void co_freevars() { checkNames(code.co_freevars(), "a", "b", "c", "y", "z"); }

        @Test
        @Override
        void co_name() { assertPythonEquals("f3", code.name); }

        @Test
        @Override
        void co_varnames() { checkNames(code.co_varnames(), "d", "e", "w", "x"); }
    }

    /**
     * Tests of individual operations up to calling a built-in method,
     * without control structures in Python.
     *
     * @param name of the Python example
     */
    @SuppressWarnings("static-method")
    @DisplayName("We can execute simple ...")
    @ParameterizedTest(name = "{0}.py")
    @ValueSource(strings = {"load_store_name", "unary_op", "binary_op", "bool_left_arith",
            "bool_right_arith", "comparison", "iterables", "tuple_index", "list_index",
            "attr_access_builtin", "call_method_builtin", "builtins_module"})
    void executeSimple(String name) {
        CPython311Code code = readCode(name);
        PyDict globals = new PyDict();
        Interpreter interp = new Interpreter();
        Object r = interp.eval(code, globals);
        assertEquals(Py.None, r);
        assertExpectedVariables(readResultDict(name), globals);
    }

    /**
     * Tests involving transfer of control including {@code for} loops.
     *
     * @param name of the Python example
     */
    @SuppressWarnings("static-method")
    @DisplayName("We can execute branches and loops ...")
    @ParameterizedTest(name = "{0}.py")
    @ValueSource(strings = {"simple_if", "multi_if", "simple_loop", "tuple_dot_product",
            "list_dot_product", "for_loop"})
    void executeBranchAndLoop(String name) {
        CPython311Code code = readCode(name);
        PyDict globals = new PyDict();
        Interpreter interp = new Interpreter();
        Object r = interp.eval(code, globals);
        assertEquals(Py.None, r);
        assertExpectedVariables(readResultDict(name), globals);
    }

    /**
     * Tests involving multiple code objects, such as function definition.
     * and call.
     *
     * @param name of the Python example
     */
    @SuppressWarnings("static-method")
    @DisplayName("We can execute complex ...")
    @ParameterizedTest(name = "{0}.py")
    @ValueSource(strings = {"function_def", "function_call", "function_closure", "function_locals"})
    void executeComplex(String name) {
        CPython311Code code = readCode(name);
        PyDict globals = new PyDict();
        Interpreter interp = new Interpreter();
        Object r = interp.eval(code, globals);
        assertEquals(Py.None, r);
        assertExpectedVariables(readResultDict(name), globals);
    }

    /**
     * A selection of other tests repeated with locals namespace implemented
     * as a custom type with {@code __setitem__} and {@code __getitem__}.
     *
     * @param name of the Python example
     */
    @SuppressWarnings("static-method")
    @DisplayName("We can execute with custom locals ...")
    @ParameterizedTest(name = "{0}.py")
    @ValueSource(strings = {"load_store_name", "attr_access_builtin", "call_method_builtin",
            "builtins_module", "function_locals"})
    void executeCustomLocals(String name) {
        CPython311Code code = readCode(name);
        PyDict globals = new PyDict();
        Interpreter interp = new Interpreter();
        // locals is a custom type with __setitem__ and __getitem__
        CustomMap locals = new CustomMap();
        Object r = interp.eval(code, globals, locals);
        assertEquals(Py.None, r);
        // Merge results in locals to globals for test
        for (Entry<Object, Object> e : locals) { globals.put(e.getKey(), e.getValue()); }
        assertExpectedVariables(readResultDict(name), globals);
    }

    /**
     * A selection of other tests repeated with the {@code __builtins__}
     * namespace implemented as a custom type with {@code __setitem__} and
     * {@code __getitem__}.
     *
     * @param name of the Python example
     */
    @SuppressWarnings("static-method")
    @DisplayName("We can execute with custom builtins ...")
    @ParameterizedTest(name = "{0}.py")
    @ValueSource(strings = {"builtins_module", "function_locals"})
    void executeCustomBuiltins(String name) {
        CPython311Code code = readCode(name);
        PyDict globals = new PyDict();
        Interpreter interp = new Interpreter();
        // builtins is a custom type with __setitem__ and __getitem__,
        CustomMap builtins = new CustomMap();
        // ... but containing all the expected members.
        for (Map.Entry<Object, Object> e : interp.builtinsModule.getDict().entrySet()) {
            builtins.__setitem__(e.getKey(), e.getValue());
        }
        // Add custom builtins to globals for createFunction to find
        globals.put("__builtins__", builtins);
        PyFunction<?> fn = code.createFunction(interp, globals);
        PyFrame<?> f = fn.createFrame(globals);
        f.eval();
        assertExpectedVariables(readResultDict(name), globals);
    }

    // Supporting constants and methods -------------------------------

    /** The Gradle build directory. */
    private static final Path BUILD = buildDirectory();

    /**
     * Python source of the examples for test. This must be consistent with
     * the definition of {@code testPythonExampleOutputDir} in the project
     * Gradle build, and below "test", with any sub-directory structure
     * leading to the Python source files.
     */
    private static final Path PYTHON_DIR = BUILD //
            .resolve("generated/sources/pythonExample") //
            .resolve("test");

    /** Where compiled files are placed by CPython. */
    private static final Path PYC_DIR = PYTHON_DIR.resolve("__pycache__");

    /**
     * The name fragment used by the compiler in the supported version of
     * CPython, e.g. {@code "cpython-311"}.
     */
    private static final String CPYTHON_VER = "cpython-311";

    /**
     * The magic number placed by the supported version of CPython, in the
     * header of compiled files. The table of these is found in CPython
     * source {@code Lib/importlib/_bootstrap_external.py}.
     */
    private static final int MAGIC_NUMBER = 3495;

    private static final String PYC_SUFFIX = "pyc";
    private static final String VAR_SUFFIX = "var";
    private static final String[] EMPTY_STRINGS = {};

    /**
     * Read a {@code code} object with {@code marshal}. The method looks for
     * compiled examples in the customary directory ({@link #PYC_DIR}},
     * being provided only the base name of the program. So for example,
     * {@code "unary_op"} will retrieve a code object from
     * {@code unary_op.cpython-311.pyc} in
     * {@code generated/sources/pythonExample/test/__pycache__}.
     *
     * @param progName base name of program
     * @return {@code code} object read in
     */
    static CPython311Code readCode(String progName) {
        String name = progName + "." + CPYTHON_VER + "." + PYC_SUFFIX;
        File f = PYC_DIR.resolve(name).toFile();
        try (
                FileInputStream fs = new FileInputStream(f);
                BufferedInputStream s = new BufferedInputStream(fs);) {

            // Wrap a marshal reader around the input stream
            MarshalModule.Reader reader = MARSHAL.new StreamReader(s);

            // First 4 bytes is a magic header
            int magic = reader.readShort();
            int magic2 = reader.readShort();
            boolean good = magic == MAGIC_NUMBER && magic2 == 0x0a0d;

            // Undocumented
            for (int i = 0; i < 3; i++) { reader.readInt(); }

            // Next should be a code object
            if (good) {
                Object o = reader.readObject();
                if (o instanceof PyCode) { return (CPython311Code)o; }
            }

            // Didn't return a code object
            throw new InterpreterError("Not a CPython code object: %s", name);

        } catch (IOException ioe) {
            throw new InterpreterError(ioe);
        }
    }

    /**
     * Read a {@code dict} object with {@code marshal}. The method looks for
     * the saved results of compiled examples in the customary directory
     * ({@link #PYC_DIR}}, being provided only the base name of the program.
     * So for example, {@code "unary_op"} will retrieve a code object from
     * {@code unary_op.cpython-311.var} in
     * {@code generated/sources/pythonExample/test/__pycache__}.
     *
     * @param progName base name of program
     * @return {@code dict} object read in
     */
    static PyDict readResultDict(String progName) {
        String name = progName + "." + CPYTHON_VER + "." + VAR_SUFFIX;
        File f = PYC_DIR.resolve(name).toFile();
        try (
                FileInputStream fs = new FileInputStream(f);
                BufferedInputStream s = new BufferedInputStream(fs);) {

            // Wrap a marshal reader around the input stream
            MarshalModule.Reader reader = MARSHAL.new StreamReader(s);

            // Should be a dict object
            Object o = reader.readObject();
            if (o instanceof PyDict) {
                return (PyDict)o;
            } else {
                throw new InterpreterError("Not a dict object: %s", name);
            }

        } catch (IOException ioe) {
            throw new InterpreterError(ioe);
        }
    }

    /**
     * Assert that all the keys of a reference dictionary are present in the
     * test dictionary, and with the same value according to
     * {@link #assertPythonEquals(Object, Object) Python equality}
     *
     * @param ref dictionary of reference results
     * @param test dictionary of results to test
     */
    static void assertExpectedVariables(Map<Object, Object> ref, Map<Object, Object> test) {
        for (Map.Entry<Object, Object> e : ref.entrySet()) {
            Object k = e.getKey();
            Object x = e.getValue();
            Object v = test.get(k);
            assertNotNull(v, () -> String.format("variable '%s' missing from result", k));
            assertPythonEquals(x, v, () -> String.format("%s = %s (expected %s)", k, v, x));
        }
    }

    /**
     * A built-in that supports the Python mapping protocol but isn't a
     * {@code Map<Object,Object>}, which is a type that gets privileged
     * treatment at multiple points in the interpreter. We can give this to
     * the {@code PyFrame} as {@link PyFrame#locals} and it is supposed to
     * work. See test
     * {@link CPython311CodeTest#executeCustomLocals(String)}.
     */
    static class CustomMap implements WithClass, Iterable<Map.Entry<Object, Object>> {

        /** The type of Python object this class implements. */
        public static final PyType TYPE = PyType.fromSpec( //
                new TypeSpec("CustomMapping", MethodHandles.lookup()));

        /** The dictionary as a hash map preserving insertion order. */
        private final LinkedHashMap<Key, Object> map = new LinkedHashMap<Key, Object>();

        @Override
        public PyType getType() { return TYPE; }

        @Override
        public Iterator<Entry<Object, Object>> iterator() {
            /*
             * Return a custom iterator that understands that the keys of the
             * internal map are just holders of the actual key.
             */
            return new Iterator<Entry<Object, Object>>() {
                Iterator<Entry<Key, Object>> mapIter = map.entrySet().iterator();

                @Override
                public boolean hasNext() { return mapIter.hasNext(); }

                @Override
                public Entry<Object, Object> next() {
                    Entry<Key, Object> mapNext = mapIter.next();
                    Object key = mapNext.getKey().get();
                    return Map.entry(key, mapNext.getValue());
                }
            };
        }

        // Special methods ------------------------------------------------

        @SuppressWarnings("unused")
        private Object __getitem__(Object key) { return map.get(toKey(key)); }

        private void __setitem__(Object key, Object value) { map.put(toKey(key), value); }

        @SuppressWarnings("unused")
        private void __delitem__(Object key) { map.remove(toKey(key)); }

        @Override
        public String toString() { return map.toString(); }

        // Plumbing --------------------------------------------------

        /**
         * Turn an object into a {@link Key} suitable for lookup in
         * {@link #map}.
         *
         * @param key to return or wrap
         */
        private static Key toKey(Object key) {
            if (key instanceof Key)
                return (Key)key;
            else
                return new KeyHolder(key);
        }

    }
}
