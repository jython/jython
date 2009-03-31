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

    //This test is for http://bugs.jython.org/issue1271
    public void testBaseProp() {
        /*
        interp.exec("from org.python.tests.props import PropShadow");
        interp.exec("a = PropShadow.Derived()");
        interp.exec("assert a.foo() == 1, 'a'");
        interp.exec("assert a.bar() == 2, 'b'");
        */
    }

    //This test is for http://bugs.jython.org/issue1271
    public void testDerivedProp() {
        /*
        interp.exec("from org.python.tests.props import PropShadow");
        interp.exec("b = PropShadow.Derived()");
        interp.exec("assert b.getBaz() == 4, 'c'");
        interp.exec("assert b.getFoo() == 3, 'd'");
        interp.exec("assert b.foo() == 1, 'e'");
        interp.exec("assert b.foo() == 1, 'f'");
        */
    }
}
