package org.python.core;

import org.python.util.PythonInterpreter;

import junit.framework.TestCase;

public class PyJavaTypeExitTest extends TestCase {

    public void testNoJavaLangSystemExitPossible() {
        PySystemState.initialize();
        try (PythonInterpreter interpreter = new PythonInterpreter()) {
            interpreter.exec(callJavaLangSystemExit());
            fail("AttributeError expected");
        } catch (PyException se) {
            assertEquals("AttributeError: type object 'java.lang.System' has no attribute 'exit'", se.getMessage());
        }
    }

    private String callJavaLangSystemExit() {
        StringBuilder b = new StringBuilder(100);
        b.append("from java.lang import System\n");
        b.append("System.exit(0)\n");
        return b.toString();
    }

}
