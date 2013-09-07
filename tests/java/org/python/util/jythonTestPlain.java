package org.python.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.python.core.Console;
import org.python.core.PlainConsole;
import org.python.core.Py;

/**
 * Tests of creating and getting the right interactive console.
 * <p>
 * System initialisation is a one-time thing normally, and the embedding of a console handler
 * similarly, so it is difficult to test more than one console choice in a single executable. For
 * this reason, there are two programs like this one: one that follows the native preference for a
 * {@link JLineConsole} and this one that induces selection of a {@link PlainConsole}
 * <p>
 * <p>
 * Automated testing of the console seems impossible since, in a scripted context (e.g. as a
 * subprocess or under Ant) Jython is no longer interactive. To run it at the prompt, suggested
 * idiom is (all one line):
 *
 * <pre>
 * java -cp build/exposed;build/classes;extlibs/* -Dpython.home=dist
 *              org.junit.runner.JUnitCore org.python.util.jythonTestPlain
 * </pre>
 */
public class jythonTestPlain {

    private static final String PYTHON_CONSOLE = "python.console";
    private static String[] commands = {"-c", "import sys; print type(sys._jy_console)"};

    /**
     * Test that specifying an invalid console class falls back to a plain console. No, really,
     * "org.python.util.InteractiveConsole" is not a {@link Console}! A warning to that effect will
     * be issued when the test runs.
     */
    @Test
    public void testFallbackConsole() {
        System.out.println("testFallbackConsole");
        System.getProperties().setProperty(PYTHON_CONSOLE, "org.python.util.InteractiveConsole");
        jython.run(commands);
        Console console = Py.getConsole();
        assertEquals(PlainConsole.class, console.getClass());
    }

    /**
     * Show that a {@link PlainConsole} may be replaced with a {@link JLineConsole}.
     */
    @Test
    public void testChangeConsole() throws Exception {
        System.out.println("testChangeConsole");
        // In the case where this test is run in isolation, cause initialisation with PlainConsole
        System.getProperties().setProperty(PYTHON_CONSOLE, "org.python.core.PlainConsole");
        PythonInterpreter interp = new PythonInterpreter();
        // Now replace it
        Py.installConsole(new JLineConsole(null));
        jython.run(commands);
        Console console = Py.getConsole();
        assertEquals(JLineConsole.class, console.getClass());
        interp.cleanup();
    }

}
