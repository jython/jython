package org.python.tests.identity;

import junit.framework.TestCase;

import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class IdentityTest extends TestCase {

    private PythonInterpreter interp;

    @Override
    protected void setUp() throws Exception {
        PySystemState sys = new PySystemState();
        sys.path.append(new PyString("dist/Lib"));
        sys.path.append(new PyString("dist/javalib/constantine.jar"));
        sys.path.append(new PyString("dist/javalib/jna.jar"));
        sys.path.append(new PyString("dist/javalib/jna-posix.jar"));
        interp = new PythonInterpreter(new PyStringMap(), sys);
    }

    public void testReadonly() {
        //This used to cause an NPE see http://bugs.jython.org/issue1295 
        interp.execfile("tests/python/identity_test.py");
    }
}
