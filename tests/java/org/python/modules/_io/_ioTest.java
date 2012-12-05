/* Copyright (c)2012 Jython Developers */
package org.python.modules._io;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

/**
 * Tests of specific methods in the Python _io module (org.python.modules._io._io). There is an
 * extensive regression test in Lib/test/test_io.py, but that is quite complex. This test case
 * exists to exercise selected functionality in isolation.
 */
public class _ioTest {

    /** We need the interpreter to be initialised for these tests **/
    PythonInterpreter interp;

    /**
     * Initialisation called before each test.
     *
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        // Initialise a Jython interpreter
        interp = new PythonInterpreter();

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

}
