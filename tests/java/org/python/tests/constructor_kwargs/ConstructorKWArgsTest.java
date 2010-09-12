package org.python.tests.constructor_kwargs;

import junit.framework.TestCase;

import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class ConstructorKWArgsTest extends TestCase {

    private PythonInterpreter interp;

    @Override
    protected void setUp() throws Exception {
        PySystemState sys = new PySystemState();
        interp = new PythonInterpreter(new PyStringMap(), sys);
    }

    public void testConstructorKWArgs() {
        interp.execfile("tests/python/constructorkwargs_test.py");
    }
}
