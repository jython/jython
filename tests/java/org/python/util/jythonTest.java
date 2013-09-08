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
 * {@link PlainConsole} and this one that induces selection of a {@link JLineConsole}. Other
 * features of the JLine console (such as access history) could be tested here. But the test
 * Lib/test/test_readline.py does this fairly well, although it has to be run manually.
 * <p>
 * Automated testing of the console seems impossible since, in a scripted context (e.g. as a
 * subprocess or under Ant) Jython is no longer interactive. To run it at the prompt, suggested
 * idiom is (all one line):
 *
 * <pre>
 * java -cp build/exposed;build/classes;extlibs/* -Dpython.home=dist
 *              org.junit.runner.JUnitCore org.python.util.jythonTest
 * </pre>
 */
public class jythonTest {

    private static String[] commands = {"-c", "import sys; print type(sys._jy_console)"};

    /**
     * Test that the default behaviour is to provide a JLineConsole.
     */
    @Test
    public void testDefaultConsole() {
        jython.run(commands);
        Console console = Py.getConsole();
        assertEquals(JLineConsole.class, console.getClass());
    }
}
