package org.python.tests.props;

import junit.framework.TestCase;

import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class ReadonlyTest extends TestCase {

    private PythonInterpreter interp;

    @Override
    protected void setUp() throws Exception {
        interp = new PythonInterpreter(new PyStringMap(), new PySystemState());
    }

    public void testReadonly() {
        interp.exec("from org.python.tests.props import Readonly;Readonly().a = 'test'");
    }
}
