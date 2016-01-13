package javatests;


import org.junit.Test;
import org.python.util.PythonInterpreter;

import static org.junit.Assert.assertEquals;

public class Issue2391AttrOrderTest {

    @Test
    public void testAttribute() {
        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.exec("from " + getClass().getPackage().getName() + " import DoubleHolder");
        interpreter.exec("d = DoubleHolder()");
        assertEquals("0.0", interpreter.eval("d.number").toString());
        interpreter.exec("d.number = 3.0");
        assertEquals("3.0", interpreter.eval("d.number").toString());
    }
}
