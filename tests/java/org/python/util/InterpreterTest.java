package org.python.util;

import junit.framework.TestCase;

import org.python.core.*;
import org.python.util.*;

public class InterpreterTest extends TestCase {

    /**
     * Motivated by a NPE reported on http://bugs.jython.org/issue1174.
     */
    public void testBasicEval() throws Exception {
        PyDictionary test = new PyDictionary();
        test.__setitem__(new PyUnicode("one"), new PyUnicode("two"));
        PythonInterpreter.initialize(System.getProperties(), null, new
String[] {});
        PythonInterpreter interp = new PythonInterpreter();
        PyObject pyo = interp.eval("{u'one': u'two'}");
        assertEquals(test, pyo);
    }
}
