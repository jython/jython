/* Copyright (c)2013 Jython Developers */
package org.python.modules._io;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFile;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.core.io.RawIOBase;
import org.python.util.PythonInterpreter;

/**
 * Tests of specific methods in the Python _io module (org.python.modules._io._io). There is an
 * extensive regression test in Lib/test/test_io.py, but that is quite complex. This test case
 * exists to exercise selected functionality in isolation.
 */
public class _ioTest {

    /** We need the interpreter to be initialised for these tests **/
    PySystemState systemState;
    PyStringMap dict;
    PythonInterpreter interp;

    /**
     * Initialisation called before each test.
     *
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Initialise a Jython interpreter
        systemState = Py.getSystemState();
        dict = new PyStringMap();
        interp = new PythonInterpreter(dict, systemState);
    }

    /**
     * Test importing the _io module into the global namespace of {@link #interp}.
     */
    @Test
    public void moduleImport() {
        interp.exec("import _io");
        PyObject _io = interp.get("_io");
        org.junit.Assert.assertNotNull(_io);
    }

    /**
     * Test raising a Python _io.UnsupportedOperation from Java code directly.
     */
    @Test
    public void javaRaiseUnsupportedOperation() {

        // Built-in modules seem not to initialise until we actually use an interpreter
        interp.exec("import io");

        // There should be a helper function
        PyException pye = _io.UnsupportedOperation("Message from _ioTest");
        PyObject type = pye.type;
        String repr = type.toString();
        assertEquals("Class name", "<class '_io.UnsupportedOperation'>", repr);

        // Raise from Java into Python and catch it in a variable: _IOBase.fileno() raises it
        interp.exec("try :\n    io.IOBase().fileno()\n" + "except Exception as e:\n    pass");
        PyObject e = interp.get("e");

        String m = e.toString();
        assertThat(m, both(containsString("UnsupportedOperation")).and(containsString("fileno")));

    }

    /**
     * Test raising a Python _io.UnsupportedOperation from Python code into Java.
     */
    @Test
    public void pythonRaiseUnsupportedOperation() {
        interp.exec("import _io");
        try {
            interp.exec("raise _io.UnsupportedOperation()");
            fail("_io.UnsupportedOperation not raised when expected");
        } catch (PyException e) {
            assertEquals(_io.UnsupportedOperation, e.type);
        }
    }

    /**
     * Test automatic closing of files when the interpreter finally exits. Done correctly, text
     * written to any kind of file-like object should be flushed to disk and the file closed when
     * the PySystemState is torn down, which happens during JVM exit. We don't here (can't?) test
     * through JVM shutdown, but we test it using the same mechanism that a JVM shutdown invokes.
     *
     * @throws IOException
     */
    @Test
    public void closeNeglectedFiles() throws IOException {

        // File names
        final String F = "$test_1_tmp";     // Raw file
        final String FB = "$test_2_tmp";    // Buffered file
        final String FT = "$test_3_tmp";    // Test file

        String expText = "Line 1\nLine 2\nLine 3.";     // Note: all ascii, but with new lines
        byte[] expBytes = expText.getBytes();
        String escapedText = expText.replace("\n", "\\n");

        // The approach is to open and write files in Python, then bin the interpreter
        interp.exec("import io\n" + //
                "u = u'" + escapedText + "'\n" +    //
                "b = b'" + escapedText + "'\n"      //
        );

        // This should get us an io.FileIO (unbuffered binary file) called f
        interp.exec("f = io.open('" + F + "', 'wb', 0)");
        PyIOBase pyf = (PyIOBase)interp.get("f");
        assertNotNull(pyf);

        // This should get us an io.BufferedWriter (buffered binary file) called fb
        interp.exec("fb = io.open('" + FB + "', 'wb')");
        PyIOBase pyfb = (PyIOBase)interp.get("fb");
        assertNotNull(pyfb);

        // This should get us an io.TextIOWrapper (buffered text file) called ft
        interp.exec("ft = io.open('" + FT + "', 'w', encoding='ascii')");
        PyIOBase pyft = (PyIOBase)interp.get("ft");
        assertNotNull(pyft);

        // Write the bytes test material to each file but don't close it
        interp.exec("f.write(b)");
        interp.exec("fb.write(b)");
        interp.exec("ft.write(u)");

        // Now bin the interpreter. (Is there a more realistic way?)
        interp.cleanup();

        // Check file itself for closure using package-visible attribute
        assertTrue(pyf.__closed);
        assertTrue(pyfb.__closed);
        assertTrue(pyft.__closed);

        // If they were not closed properly not all bytes will reach the files.
        checkFileContent(F, expBytes);
        checkFileContent(FB, expBytes);

        // Expect that TextIOWrapper should have adjusted the line separator
        checkFileContent(FT, newlineFix(expText));
    }

    /**
     * Test automatic closing of PyFiles when the interpreter finally exits. This repeats
     * {@link #closeNeglectedFiles()} but for the py2k flavour of file.
     *
     * @throws IOException
     */
    @Test
    public void closeNeglectedPyFiles() throws IOException {

        final String F = "$test_1_tmp";     // Raw file
        final String FB = "$test_2_tmp";    // Buffered file
        final String FT = "$test_3_tmp";    // Test file

        String expText = "Line 1\nLine 2\nLine 3.";
        byte[] expBytes = expText.getBytes();
        String escapedText = expText.replace("\n", "\\n");

        // The approach is to open and write files in Python, then bin the interpreter
        interp.exec("import io\n" + //
                "u = u'" + escapedText + "'\n" +    //
                "b = b'" + escapedText + "'\n"      //
        );

        // This should get us an unbuffered binary PyFile called f
        interp.exec("f = open('" + F + "', 'wb', 0)");
        PyFile pyf = (PyFile)interp.get("f");
        assertNotNull(pyf);
        RawIOBase r = (RawIOBase) pyf.fileno().__tojava__(RawIOBase.class);

        // This should get us a buffered binary PyFile called fb
        interp.exec("fb = open('" + FB + "', 'wb')");
        PyFile pyfb = (PyFile)interp.get("fb");
        assertNotNull(pyfb);
        RawIOBase rb = (RawIOBase) pyfb.fileno().__tojava__(RawIOBase.class);

        // This should get us an buffered text PyFile called ft
        interp.exec("ft = open('" + FT + "', 'w')");
        PyFile pyft = (PyFile)interp.get("ft");
        assertNotNull(pyft);
        RawIOBase rt = (RawIOBase) pyft.fileno().__tojava__(RawIOBase.class);

        // Write the bytes test material to each file but don't close it
        interp.exec("f.write(b)");
        interp.exec("fb.write(b)");
        interp.exec("ft.write(u)");

        // Now bin the interpreter. (Is there a more realistic way?)
        interp.cleanup();

        // The PyFile itself is not closed but the underlying stream should be
        assertTrue(r.closed());
        assertTrue(rb.closed());
        assertTrue(rt.closed());

        // If they were not closed properly not all bytes will reach the files.
        checkFileContent(F, expBytes);
        checkFileContent(FB, expBytes);

        // Expect that TextIOWrapper should have adjusted the line separator
        checkFileContent(FT, newlineFix(expText));
    }

    /**
     * Check the file contains the bytes we expect and <b>delete the file</b>. If it was not closed
     * properly (layers in the right order and a flush) not all bytes will have reached the files.
     *
     * @param name of file
     * @param expBytes expected
     * @throws IOException if cannot open/read
     */
    private static void checkFileContent(String name, byte[] expBytes) throws IOException {
        byte[] r = new byte[2 * expBytes.length];
        File f = new File(name);
        FileInputStream in = new FileInputStream(f);
        int n = in.read(r);
        in.close();

        String msg = "Bytes read from " + name;
        assertEquals(msg, expBytes.length, n);
        byte[] resBytes = Arrays.copyOf(r, n);
        assertArrayEquals(msg, expBytes, resBytes);

        f.delete();
    }


    /**
     * Replace "\n" characters by the system-defined newline sequence and return as bytes.
     * @param expText to translate
     * @return result as bytes
     */
    private static byte[] newlineFix(String expText) {
        String newline = System.getProperty("line.separator");
        return expText.replace("\n", newline).getBytes();
    }
}
