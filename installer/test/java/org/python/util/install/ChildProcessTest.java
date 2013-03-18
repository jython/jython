package org.python.util.install;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import junit.framework.TestCase;

public class ChildProcessTest extends TestCase {

    private final static String CLASS_NAME = "org.python.util.install.ChildProcessExample";

    /**
     * test a default child process
     */
    public void testDefault() {
        ChildProcess childProcess = new ChildProcess();
        String command[] = buildJavaCommand(CLASS_NAME);
        childProcess.setCommand(command);
        childProcess.setDebug(true);
        int exitValue = childProcess.run();
        assertEquals("Expected child process to end normally instead of " + exitValue, 0, exitValue);
    }

    /**
     * test the child process with a timeout
     */
    public void testTimeout() {
        ChildProcess childProcess = new ChildProcess();
        String command[] = buildJavaCommand(CLASS_NAME);
        childProcess.setCommand(command);
        childProcess.setDebug(true);
        childProcess.setTimeout(2000); // timeout to 2 seconds
        int exitValue = childProcess.run();
        assertEquals("Expected child process to be destroyed instead of " + exitValue,
                     ChildProcess.DESTROYED_AFTER_TIMEOUT,
                     exitValue);
    }

    /**
     * test silent mode
     */
    public void testSilent() throws IOException {
        ChildProcess childProcess = new ChildProcess();
        String command[] = new String[] {"lwiklsl", "-siwK"};
        childProcess.setCommand(command);
        childProcess.setDebug(false);
        childProcess.setSilent(true);
        ByteArrayOutputStream redirectedErr = new ByteArrayOutputStream();
        ByteArrayOutputStream redirectedOut = new ByteArrayOutputStream();
        int exitValue = 0;
        PrintStream oldErr = System.err;
        PrintStream oldOut = System.out;
        try {
            System.setErr(new PrintStream(redirectedErr));
            System.setOut(new PrintStream(redirectedOut));
            exitValue = childProcess.run();
        } finally {
            System.setErr(oldErr);
            System.setOut(oldOut);
        }
        assertTrue(0 != exitValue);
        redirectedErr.flush();
        redirectedOut.flush();
        assertEquals(0, redirectedErr.size());
        assertEquals(0, redirectedOut.size());
    }

    //
    // private methods
    //
    private String[] buildJavaCommand(String classAndArguments) {
        String quote = "";
        if (System.getProperty("os.name", "unknown").toLowerCase().indexOf("windows") >= 0) {
            quote = "\"";
        }
        String classpath = System.getProperty("java.class.path");
        return new String[] {"java",  "-classpath",  quote.concat(classpath).concat(quote), classAndArguments};
    }
}