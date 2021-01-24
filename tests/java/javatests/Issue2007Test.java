package javatests;

import org.junit.Test;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.util.InteractiveInterpreter;

import java.io.StringWriter;

import static org.junit.Assert.*;

public class Issue2007Test
{
    /**
     * Verify that print_function works in scripts.  This was always the case.
     */
    @Test
    public void testPrintFunctionInScript() {
        CapturingInterpreter interp = new CapturingInterpreter();
        interp.exec("from __future__ import print_function\n" +
                    "print(1, 2, 3)\n");
        assertEquals("1 2 3\n", interp.getOutput());
    }

    /**
     * Verify that print_function works when passing individual statements to
     * an interactive interpreter.  If issue 2007 is present, this will fail
     * as the second statement will print '(1, 2, 3)' instead of '1 2 3'.
     */
    @Test
    public void testPrintFunctionInInteractive() {
        CapturingInterpreter interp = new CapturingInterpreter();
        interp.runsource("from __future__ import print_function");
        interp.runsource("print(1, 2, 3)");
        assertEquals("1 2 3\n", interp.getOutput());
    }

    private static class CapturingInterpreter extends InteractiveInterpreter {
        private final StringWriter writer = new StringWriter();

        CapturingInterpreter()
        {
            super(new PyStringMap(), new PySystemState());
            setOut(writer);
        }

        String getOutput() {
            close();
            return writer.toString();
        }
    }
}
