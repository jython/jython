package org.python.tests.props;

import junit.framework.TestCase;

import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class BeanPropertyTest extends TestCase {

    private PythonInterpreter interp;

    @Override
    protected void setUp() throws Exception {
        interp = new PythonInterpreter(new PyStringMap(), new PySystemState());
    }

    public void testReadonly() {
        //This used to cause an NPE see http://bugs.jython.org/issue1295 
        interp.exec("from org.python.tests.props import Readonly;Readonly().a = 'test'");
    }

    public void testShadowing() {
        interp.execfile("tests/python/prop_test.py");
    }

}
