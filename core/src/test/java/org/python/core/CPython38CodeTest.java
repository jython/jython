package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.python.base.InterpreterError;
import org.python.modules.marshal;

/**
 * Tests that read code objects from prepared {@code .pyc} files and
 * execute the byte code.
 *
 * These files are prepared in the Gradle build using a compatible
 * version of CPython, from Python source in
 * {@code core/src/test/pythonExample}. To run these in the IDE,
 * first execute the task:<pre>
 * .\gradlew --console=plain core:compileTestPythonExamples
 * </pre>
 */
@DisplayName("Given programs compiled by CPython 3.8 ...")
class CPython38CodeTest extends UnitTestSupport {

    @SuppressWarnings("static-method")
    @DisplayName("marshal can read a code object")
    @ParameterizedTest(name = "from {0}")
    @ValueSource(strings = {"load_store_name", "unary_op", "binary_op", "bool_left_arith",
            "bool_right_arith", "simple_if", "multi_if"})
    void loadCodeObject(String name) {
        PyCode code = readCode(name);
        assertPythonType(PyCode.TYPE, code);
    }

    @SuppressWarnings("static-method")
    @DisplayName("marshal can read a result object")
    @ParameterizedTest(name = "from {0}")
    @ValueSource(strings = {"load_store_name", "unary_op", "binary_op", "bool_left_arith",
            "bool_right_arith", "simple_if", "multi_if"})
    void loadResultDict(String name) {
        PyDict dict = readResultDict(name);
        assertPythonType(PyDict.TYPE, dict);
    }

    @DisplayNameGeneration(DisplayNameGenerator.Simple.class)
    static abstract class CodeAttributes {
        final String name;
        final PyCode code;

        CodeAttributes(String name) {
            this.name = name;
            this.code = readCode(name);
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
        protected void co_name() { assertEquals("<module>", code.name); }

        abstract void co_names();

        @Test
        void co_varnames() { assertEquals(0, code.co_varnames().size()); }
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
            assertEquals(4, code.co_consts().size());
        }
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
            "bool_right_arith", "tuple_index", "list_index", "call_method_builtin"})
    void executeSimple(String name) {
        CPython38Code code = readCode(name);
        PyDict globals = new PyDict();
        code.createFrame(null, globals, globals).eval();
        assertExpectedVariables(readResultDict(name), globals);
    }

    /**
     * Tests involving transfer of control.
     *
     * @param name of the Python example
     */
    @SuppressWarnings("static-method")
    @DisplayName("We can execute branches ...")
    @ParameterizedTest(name = "{0}.py")
    @ValueSource(strings = {"simple_if", "multi_if"})
    void executeBranchAndLoop(String name) {
        CPython38Code code = readCode(name);
        PyDict globals = new PyDict();
        code.createFrame(null, globals, globals).eval();
        assertExpectedVariables(readResultDict(name), globals);
    }

    // Supporting constants and methods -------------------------------

    /** The Gradle build directory. */
    private static final Path BUILD = buildDirectory();

    /**
     * Python source of the examples for test. This must be consistent
     * with the definition of {@code testPythonExampleOutputDir} in the
     * project Gradle build, and below "test", with any sub-directory
     * structure leading to the Python source files.
     */
    private static final Path PYTHON_DIR = BUILD //
            .resolve("generated/sources/pythonExample") //
            .resolve("test");

    /** Where compiled files are placed by CPython. */
    private static final Path PYC_DIR = PYTHON_DIR.resolve("__pycache__");

    /**
     * The name fragment used by the compiler in the supported version
     * of CPython, e.g. {@code "cpython-38"}.
     */
    private static final String CPYTHON_VER = "cpython-38";
    /**
     * The magic number placed by the supported version of CPython, in
     * the header of compiled files.
     */
    private static final int MAGIC_NUMBER = 3413;

    private static final String PYC_SUFFIX = "pyc";
    private static final String VAR_SUFFIX = "var";

    /**
     * Read a {@code code} object with {@code marshal}. The method looks
     * for compiled examples in the customary directory
     * ({@link #PYC_DIR}}, being provided only the base name of the
     * program. So for example, {@code "unary_op"} will retrieve a code
     * object from {@code unary_op.cpython-38.pyc} in
     * {@code generated/sources/pythonExample/test/__pycache__}.
     *
     * @param progName base name of program
     * @return {@code code} object read in
     */
    static CPython38Code readCode(String progName) {
        String name = progName + "." + CPYTHON_VER + "." + PYC_SUFFIX;
        File f = PYC_DIR.resolve(name).toFile();
        try (FileInputStream fs = new FileInputStream(f);
                BufferedInputStream s = new BufferedInputStream(fs);) {

            // Wrap a marshal reader around the input stream
            marshal.Reader reader = new marshal.StreamReader(s);

            // First 16 bytes is a header
            int magic = reader.readShort();
            assert magic == MAGIC_NUMBER;
            int magic2 = reader.readShort();
            assert magic2 == 0x0a0d;
            for (int i = 0; i < 3; i++) { reader.readInt(); }

            // Next should be a code object
            Object o = reader.readObject();
            if (o instanceof PyCode) {
                return (CPython38Code)o;
            } else {
                throw new InterpreterError("Not a CPython code object: %s", name);
            }

        } catch (IOException ioe) {
            throw new InterpreterError(ioe);
        }
    }

    /**
     * Read a {@code dict} object with {@code marshal}. The method looks
     * for the saved results of compiled examples in the customary
     * directory ({@link #PYC_DIR}}, being provided only the base name
     * of the program. So for example, {@code "unary_op"} will retrieve
     * a code object from {@code unary_op.cpython-38.var} in
     * {@code generated/sources/pythonExample/test/vsj3/evo1/__pycache__}.
     *
     * @param progName base name of program
     * @return {@code dict} object read in
     */
    static PyDict readResultDict(String progName) {
        String name = progName + "." + CPYTHON_VER + "." + VAR_SUFFIX;
        File f = PYC_DIR.resolve(name).toFile();
        try (FileInputStream fs = new FileInputStream(f);
                BufferedInputStream s = new BufferedInputStream(fs);) {

            // Wrap a marshal reader around the input stream
            marshal.Reader reader = new marshal.StreamReader(s);

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
     * Assert that all the keys of a reference dictionary are present in
     * the test dictionary, and with the same value according to
     * {@link #assertPythonEquals(Object, Object) Python equality}
     *
     * @param ref dictionary of reference results
     * @param test dictionary of results to test
     */
    private static void assertExpectedVariables(Map<Object, Object> ref, Map<Object, Object> test) {
        for (Map.Entry<Object, Object> e : ref.entrySet()) {
            Object k = e.getKey();
            Object x = ref.get(k);
            Object v = test.get(k);
            assertNotNull(v, () -> String.format("variable %s missing from result", k));
            assertPythonEquals(x, v, () -> String.format("%s = %s (not %s)", k, v, x));
        }
    }
}
