// Copyright (c)2013 Jython Developers
package javatests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Test;

/**
 * Tests investigating issues with readline() first raised in Jython Issue #1972. These involve
 * sub-process input and output through the console streams. Although the console streams are used,
 * the JLine console handler is not engaged, as the test {@link #jythonJLineConsole()} verifies. You
 * could run this as a straight JUnit test, or in various debugging configurations, including remote
 * debugging of the subprocess.
 * <p>
 * This test passes in Jython 2.5.2 and 2.5.4rc1. The test {@link #jythonReadline()} fails with
 * Jython 2.5.3.
 * <p>
 * The bulk of this program is designed to be run as JUnit tests, but it also has a
 * {@link #main(String[])} method that echoes <code>System.in</code> onto <code>System.out</code>
 * either as byte data (characters effectively) or as hexadecimal. The early tests run this as a
 * subprocess to establish exactly what bytes, in particular exactly what line endings, are received
 * and emitted by a simple Java subprocess. The later tests run Jython as the subprocess, executing
 * various command line programs and interactive console commands.
 * <p>
 * This was developed on Windows. It tries to abstract away the particular choice of line separator,
 * so it will run on other platforms, but it hasn't been tested that this was successful.
 */
public class Issue1972 {

    /** Set to non-zero port number to enable subprocess debugging in selected tests. */
    static int DEBUG_PORT = 0; // 8000 or 0

    /** Control the amount of output to the console: 0, 1 or 2. */
    static int VERBOSE = 0;

    /** Lines in stdout (as regular expressions) to ignore when checking subprocess output. */
    static String[] STDOUT_IGNORE = {"Listening for transport dt_socket"};

    /** Lines in stderr (as regular expressions) to ignore when checking subprocess output. */
    static String[] STDERR_IGNORE = {"Jython 2", "\\*sys-package-mgr"};

    /**
     * Extra JVM options used when debugging is enabled. <code>DEBUG_PORT</code> will be substituted
     * for the <code>%d</code> marker in a <code>String.format</code> call. The debugger must attach
     * to the application after it is launched by this test programme.
     * */
    static final String DEBUG_OPTS =
            "-agentlib:jdwp=transport=dt_socket,server=y,address=%d,suspend=y";

    /** Subprocess created by {@link #setProcJava(String...)} */
    private Process proc = null;

    /** The <code>stdin</code> of the subprocess as a writable stream. */
    private OutputStream toProc;

    /** A queue handling the <code>stdout</code> of the subprocess. */
    private LineQueue inFromProc;
    /** A queue handling the <code>stderr</code> of the subprocess. */
    private LineQueue errFromProc;

    @After
    public void afterEachTest() {
        if (proc != null) {
            proc.destroy();
        }
        inFromProc = errFromProc = null;
    }

    static final Properties props = System.getProperties();
    static final String lineSeparator = props.getProperty("line.separator");
    static final String pythonHome = props.getProperty("python.home");

    /**
     * Check that on this system we know how to launch and read the error output from a subprocess.
     *
     * @throws IOException
     */
    @Test
    public void readStderr() throws Exception {
        announceTest(VERBOSE, "readStderr");

        // Run java -version, which outputs on System.err
        setProcJava("-version");
        proc.waitFor();

        // Dump to console
        outputAsHexDump(VERBOSE, inFromProc, errFromProc);

        assertEquals("Unexpected text in stdout", 0, inFromProc.size());
        assertTrue("No text output to stderr", errFromProc.size() > 0);
        String res = errFromProc.asStrings().get(0);
        assertTrue("stderr should mention version", res.contains("version"));
    }

    /**
     * Check that on this system we know how to launch and read standard output from a subprocess.
     *
     * @throws IOException
     */
    @Test
    public void readStdout() throws Exception {
        announceTest(VERBOSE, "readStdout");

        // Run the main of this class
        setProcJava(this.getClass().getName());
        proc.waitFor();

        outputAsHexDump(VERBOSE, inFromProc, errFromProc);

        checkErrFromProc();
        checkInFromProc("Hello");
    }

    /**
     * Check that on this system we know how to launch, write to and read from a subprocess.
     *
     * @throws IOException
     */
    @Test
    public void echoStdin() throws Exception {
        announceTest(VERBOSE, "echoStdin");

        // Run the main of this class as an echo programme
        setProcJava(this.getClass().getName(), "echo");

        writeToProc("spam");
        writeToProc("spam\r");
        writeToProc("spam\r\n");
        toProc.close();
        proc.waitFor();

        outputAsHexDump(VERBOSE, inFromProc, errFromProc);

        checkErrFromProc();
        checkInFromProc(false, "Hello\\r\\n", "spamspam\\r", "spam\\r\\n");
    }

    /**
     * Check that on this system line endings are received as expected by a subprocess.
     * <p>
     * <b>Observation from Windows 7 x64:</b> data is written to the subprocess once
     * <code>flush()</code> is called on the output stream. It can be read from
     * <code>System.in</code> in the subprocess, which of course writes hex to
     * <code>System.out</code> but that data is not received back in the parent process until
     * <code>System.out.println()</code> is called in the subprocess.
     *
     * @throws IOException
     */
    @Test
    public void echoStdinAsHex() throws Exception {
        announceTest(VERBOSE, "echoStdinAsHex");

        // Run the main of this class as an echo programme
        setProcJava(this.getClass().getName(), "hex");

        writeToProc("a\r");
        writeToProc("b\n");
        writeToProc("c\r\n");
        toProc.close();
        proc.waitFor();

        outputAsStrings(VERBOSE, inFromProc, errFromProc);

        checkErrFromProc();
        checkInFromProc("Hello", " 61", " 0d", " 62", " 0a", " 63", " 0d", " 0a");
    }

    /**
     * Test reading back from Jython subprocess with program on command-line.
     *
     * @throws Exception
     */
    @Test
    public void jythonSubprocess() throws Exception {
        announceTest(VERBOSE, "jythonSubprocess");

        // Run Jython hello programme
        setProcJava("org.python.util.jython", "-c", "print 'Hello'");
        proc.waitFor();

        outputAsHexDump(VERBOSE, inFromProc, errFromProc);

        checkErrFromProc();
        checkInFromProc("Hello");
    }

    /**
     * Discover what is handling the "console" when the program is on the command line only.
     *
     * @throws Exception
     */
    @Test
    public void jythonNonInteractive() throws Exception {
        announceTest(VERBOSE, "jythonNonInteractiveConsole");

        // Run Jython enquiry about console as -c program
        setProcJava("org.python.util.jython", "-c",
                "import sys; print type(sys._jy_console).__name__; print sys.stdin.isatty()");
        proc.waitFor();

        outputAsStrings(VERBOSE, inFromProc, errFromProc);

        checkErrFromProc();
        checkInFromProc("PlainConsole", "False");
    }

    /**
     * Discover what is handling the "console" when the program is entered interactively at the
     * Jython prompt.
     *
     * @throws Exception
     */
    @Test
    public void jythonInteractive() throws Exception {
        announceTest(VERBOSE, "jythonInteractiveConsole");

        // Run Jython with simple actions at the command prompt
        setProcJava(    //
                "-Dpython.home=" + pythonHome, //
                "org.python.util.jython");

        writeToProc("12+3\n");
        writeToProc("import sys\n");
        writeToProc("print type(sys._jy_console).__name__\n");
        writeToProc("print sys.stdin.isatty()\n");
        toProc.close();
        proc.waitFor();

        outputAsStrings(VERBOSE, inFromProc, errFromProc);

        checkErrFromProc("");   // stderr produces one empty line. Why?
        checkInFromProc("15", "PlainConsole", "False");
    }

    /**
     * Discover what is handling the "console" when the program is entered interactively at the
     * Jython prompt, and we try to force use of JLine (which fails).
     *
     * @throws Exception
     */
    @Test
    public void jythonJLineConsole() throws Exception {
        announceTest(VERBOSE, "jythonJLineConsole");

        // Run Jython with simple actions at the command prompt
        setProcJava(    //
                "-Dpython.console=org.python.util.JLineConsole", //
                "-Dpython.home=" + pythonHome, //
                "org.python.util.jython");

        writeToProc("12+3\n");
        writeToProc("import sys\n");
        writeToProc("print type(sys._jy_console).__name__\n");
        writeToProc("print sys.stdin.isatty()\n");
        toProc.close();
        proc.waitFor();

        outputAsStrings(VERBOSE, inFromProc, errFromProc);

        checkErrFromProc("");   // stderr produces one empty line. Why?

        // We can specify JLineConsole, but isatty() is not fooled.
        checkInFromProc("15", "PlainConsole", "False");
    }

    /**
     * Test writing to and reading back from Jython subprocess with echo program on command-line.
     *
     * @throws Exception
     */
    @Test
    public void jythonReadline() throws Exception {
        announceTest(VERBOSE, "jythonReadline");

        // Run Jython simple readline programme
        setProcJava( //
                "-Dpython.console=org.python.util.JLineConsole", //
                // "-Dpython.console.interactive=True", //
                "-Dpython.home=" + pythonHome, //
                "org.python.util.jython", //
                "-c", //
                "import sys; sys.stdout.write(sys.stdin.readline()); sys.stdout.flush();" //
        );

        // Discard first output (banner or debugging sign-on)
        inFromProc.clear();
        errFromProc.clear();

        // Force lines in until something comes out or it breaks
        String spamString = "spam" + lineSeparator;
        byte[] spam = (spamString).getBytes();
        int count, limit = 9000;
        for (count = 0; count <= limit; count += spam.length) {
            toProc.write(spam);
            toProc.flush();
            // Give the sub-process a chance the first time and the last
            if (count == 0 || count + spam.length > limit) {
                Thread.sleep(10000);
            }
            // If anything came back, we're done
            if (inFromProc.size() > 0) {
                break;
            }
            if (errFromProc.size() > 0) {
                break;
            }
        }

        if (VERBOSE >= 1) {
            System.out.println(String.format("  count = %4d", count));
        }

        toProc.close();
        proc.waitFor();
        outputAsHexDump(VERBOSE, inFromProc, errFromProc);

        assertTrue("Subprocess did not respond promptly to first line", count == 0);
        checkInFromProc("spam");
    }

    /**
     * A main program that certain tests in the module will use as a subprocess. If an argument is
     * given it means:
     * <table>
     * <tr>
     * <td>"echo"</td>
     * <td>echo the characters received as text</td>
     * </tr>
     * <tr>
     * <td>"hex"</td>
     * <td>echo the characters as hexadecimal</td>
     * </tr>
     * </table>
     *
     * @param args
     * @throws IOException
     */
    public static void main(String args[]) throws IOException {

        System.out.println("Hello");

        if (args.length > 0) {
            String arg = args[0];

            if ("echo".equals(arg)) {
                int c;
                while ((c = System.in.read()) != -1) {
                    System.out.write(c);
                    System.out.flush();
                }

            } else if ("hex".equals(arg)) {
                int c;
                while ((c = System.in.read()) != -1) {
                    System.out.printf(" %02x", c);
                    System.out.println();
                }

            } else {
                System.err.println("Huh?");
            }
        }

    }

    /**
     * Invoke the java command with the given arguments. The class path will be the same as this
     * programme's class path (as in the property <code>java.class.path</code>).
     *
     * @param args further arguments to the program run
     * @return the running process
     * @throws IOException
     */
    static Process startJavaProcess(String... args) throws IOException {

        // Prepare arguments for the java command
        String javaClassPath = props.getProperty("java.class.path");

        List<String> cmd = new ArrayList<String>();
        cmd.add("java");
        cmd.add("-classpath");
        cmd.add(javaClassPath);
        if (DEBUG_PORT > 0) {
            cmd.add(String.format(DEBUG_OPTS, DEBUG_PORT));
        }
        for (String arg : args) {
            cmd.add(arg);
        }

        // Create the factory for the external process with the given command
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(cmd);

        // If you want to check environment variables, it looks like this:
        /*
         * Map<String, String> env = pb.environment(); for (Map.Entry<String, String> entry :
         * env.entrySet()) { System.out.println(entry); }
         */

        // Actually create the external process and return the Java object representing it
        return pb.start();
    }

    /**
     * Invoke the java command with the given arguments. The class path will be the same as this
     * programme's class path (as in the property <code>java.class.path</code>). After the call,
     * {@link #proc} references the running process and {@link #inFromProc} and {@link #errFromProc}
     * are handling the <code>stdout</code> and <code>stderr</code> of the subprocess.
     *
     * @param args further arguments to the program run
     * @throws IOException
     */
    private void setProcJava(String... args) throws IOException {
        proc = startJavaProcess(args);
        inFromProc = new LineQueue(proc.getInputStream());
        errFromProc = new LineQueue(proc.getErrorStream());
        toProc = proc.getOutputStream();
    }

    /**
     * Write this string into the <code>stdin</code> of the subprocess. The platform default
     * encoding will be used.
     *
     * @param s to write
     * @throws IOException
     */
    private void writeToProc(String s) throws IOException {
        toProc.write(s.getBytes());
        toProc.flush();
    }

    /**
     * Check lines of {@link #queue} against expected text. Lines from the subprocess, after the
     * {@link #escape(byte[])} transormation has been applied, are expected to be equal to the
     * strings supplied, optionally after {@link #escapedSeparator} has been added to the expected
     * strings.
     *
     * @param message identifies the queue in error message
     * @param addSeparator if true, system-defined line separator expected
     * @param queue to be compared
     * @param toIgnore patterns defining lines to ignore while processing
     * @param expected lines of text (given without line separators)
     */
    private void checkFromProc(String message, boolean addSeparator, LineQueue queue,
            List<Pattern> toIgnore, String... expected) {

        if (addSeparator) {
            // Each expected string must be treated as if the lineSeparator were appended
            String escapedSeparator = "";
            try {
                escapedSeparator = escape(lineSeparator.getBytes("US-ASCII"));
            } catch (UnsupportedEncodingException e) {
                fail("Could not encode line separator as ASCII"); // Never happens
            }
            // ... so append one
            for (int i = 0; i < expected.length; i++) {
                expected[i] += escapedSeparator;
            }
        }

        // Get the escaped form of the byte buffers in the queue
        List<String> results = queue.asStrings();

        // Count through the results, comparing what we can't ignore to what was expected
        int count = 0;
        for (String r : results) {
            if (!beginsWithAnyOf(r, toIgnore)) {
                if (count < expected.length) {
                    // Check the line against the expected text
                    assertEquals(message, expected[count++], r);
                } else {
                    // Extra line will be a failure but continue to count
                    count++;
                }
            }
        }
        // Check number of lines we can't ignore against the number expected
        assertEquals(message, expected.length, count);
    }

    /** Compiled regular expressions for the lines to ignore (on stdout). */
    private static List<Pattern> stdoutIgnore;

    /** Compiled regular expressions for the lines to ignore (on stderr). */
    private static List<Pattern> stderrIgnore;

    /** If not already done, compile the regular expressions we need. */
    private static void compileToIgnore() {
        if (stdoutIgnore == null || stderrIgnore == null) {
            // Compile the lines to ignore to Pattern objects
            stdoutIgnore = compileAll(STDOUT_IGNORE);
            stderrIgnore = compileAll(STDERR_IGNORE);
        }
    }

    /** If not already done, compile one set of regular expressions to patterns. */
    private static List<Pattern> compileAll(String[] regex) {
        List<Pattern> result = new LinkedList<Pattern>();
        if (regex != null) {
            for (String s : regex) {
                Pattern p = Pattern.compile(s);
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Compute whether a string begins with any of a set of strings.
     *
     * @param s the string in question
     * @param patterns to check against
     * @return
     */
    private static boolean beginsWithAnyOf(String s, List<Pattern> patterns) {
        for (Pattern p : patterns) {
            if (p.matcher(s).lookingAt()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check lines of {@link #inFromProc} against expected text.
     *
     * @param addSeparator if true, system-defined line separator expected
     * @param expected lines of text (given without line separators)
     */
    private void checkInFromProc(boolean addSeparator, String... expected) {
        compileToIgnore();        // Make sure we have the matcher patterns
        checkFromProc("subprocess stdout", addSeparator, inFromProc, stdoutIgnore, expected);
    }

    /**
     * Check lines of {@link #inFromProc} against expected text. Lines from the subprocess are
     * expected to be equal to those supplied after {@link #escapedSeparator} has been added.
     *
     * @param expected lines of text (given without line separators)
     */
    private void checkInFromProc(String... expected) {
        checkInFromProc(true, expected);
    }

    /**
     * Check lines of {@link #errFromProc} against expected text.
     *
     * @param addSeparator if true, system-defined line separator expected
     * @param expected lines of text (given without line separators)
     */
    private void checkErrFromProc(boolean addSeparator, String... expected) {
        compileToIgnore();        // Make sure we have the matcher patterns
        checkFromProc("subprocess stderr", addSeparator, errFromProc, stderrIgnore, expected);
    }

    /**
     * Check lines of {@link #errFromProc} against expected text. Lines from the subprocess are
     * expected to be equal to those supplied after {@link #escapedSeparator} has been added.
     *
     * @param expected lines of text (given without line separators)
     */
    private void checkErrFromProc(String... expected) {
        checkErrFromProc(true, expected);
    }

    /**
     * Brevity for announcing tests on the console when that is used to dump values.
     *
     * @param verbose if <1 suppress output
     * @param name of test
     */
    static void announceTest(int verbose, String name) {
        if (verbose >= 1) {
            System.out.println(String.format("------- Test: %-40s -------", name));
        }
    }

    /**
     * Output is System.out the formatted strings representing lines from a subprocess stdout.
     *
     * @param verbose if <2 suppress output
     * @param inFromProc lines received from the stdout of a subprocess
     */
    static void outputAsStrings(int verbose, LineQueue inFromProc) {
        if (verbose >= 2) {
            outputStreams(inFromProc.asStrings(), null);
        }
    }

    /**
     * Output is System.out the formatted strings representing lines from a subprocess stdout, and
     * if there are any, from stderr.
     *
     * @param verbose if <2 suppress output
     * @param inFromProc lines received from the stdout of a subprocess
     * @param errFromProc lines received from the stderr of a subprocess
     */
    static void outputAsStrings(int verbose, LineQueue inFromProc, LineQueue errFromProc) {
        if (verbose >= 2) {
            outputStreams(inFromProc.asStrings(), errFromProc.asStrings());
        }
    }

    /**
     * Output is System.out a hex dump of lines from a subprocess stdout.
     *
     * @param verbose if <2 suppress output
     * @param inFromProc lines received from the stdout of a subprocess
     */
    static void outputAsHexDump(int verbose, LineQueue inFromProc) {
        if (verbose >= 2) {
            outputStreams(inFromProc.asHexDump(), null);
        }
    }

    /**
     * Output is System.out a hex dump of lines from a subprocess stdout, and if there are any, from
     * stderr.
     *
     * @param verbose if <2 suppress output
     * @param inFromProc lines received from the stdout of a subprocess
     * @param errFromProc lines received from the stderr of a subprocess
     */
    static void outputAsHexDump(int verbose, LineQueue inFromProc, LineQueue errFromProc) {
        if (verbose >= 2) {
            outputStreams(inFromProc.asHexDump(), errFromProc.asHexDump());
        }
    }

    /**
     * Output is System.out the formatted strings representing lines from a subprocess stdout, and
     * if there are any, from stderr.
     *
     * @param stdout to output labelled "Output stream:"
     * @param stderr to output labelled "Error stream:" unless an empty list or null
     */
    private static void outputStreams(List<String> stdout, List<String> stderr) {

        PrintStream out = System.out;

        out.println("Output stream:");
        for (String line : stdout) {
            out.println(line);
        }

        if (stderr != null && stderr.size() > 0) {
            out.println("Error stream:");
            for (String line : stderr) {
                out.println(line);
            }
        }
    }

    private static final String ESC_CHARS = "\r\n\t\\\b\f";
    private static final String[] ESC_STRINGS = {"\\r", "\\n", "\\t", "\\\\", "\\b", "\\f"};

    /**
     * Helper to format one line of string output hex-escaping non-ASCII characters.
     *
     * @param sb to overwrite with the line of dump output
     * @param bb from which to take the bytes
     */
    private static void stringDump(StringBuilder sb, ByteBuffer bb) {

        // Reset the string buffer
        sb.setLength(0);
        int n = bb.remaining();

        for (int i = 0; i < n; i++) {
            // Read byte as character code (old-style ascii mindset at work here)
            char c = (char)(0xff & bb.get());

            // Check for C-style escape characters
            int j = ESC_CHARS.indexOf(c);
            if (j >= 0) {
                // Use replacement escape sequence
                sb.append(ESC_STRINGS[j]);

            } else if (c < ' ' || c > 126) {
                // Some non-printing character that doesn't have an escape
                sb.append(String.format("\\x%02x", c));

            } else {
                // A safe character
                sb.append(c);
            }
        }

    }

    /**
     * Convert bytes (interpreted as ASCII) to String where the non-ascii characters are escaped.
     *
     * @param b
     * @return
     */
    public static String escape(byte[] b) {
        StringBuilder sb = new StringBuilder(100);
        ByteBuffer bb = ByteBuffer.wrap(b);
        stringDump(sb, bb);
        return sb.toString();
    }

    /**
     * Wrapper for an InputStream that creates a thread to read it in the background into a queue of
     * ByteBuffer objects. Line endings (\r, \n or \r\n) are preserved. This is used in the tests to
     * see exactly what a subprocess produces, without blocking the subprocess as it writes. The
     * data are available as a hexadecimal dump (a bit like <code>od</code>) and as string, assuming
     * a UTF-8 encoding, or some subset like ASCII.
     */
    static class LineQueue extends LinkedBlockingQueue<ByteBuffer> {

        static final int BUFFER_SIZE = 1024;

        private InputStream in;
        ByteBuffer buf;
        boolean seenCR;

        Thread scribe;

        /**
         * Wrap a stream in the reader and immediately begin reading it.
         *
         * @param in
         */
        LineQueue(InputStream in) {
            this.in = in;

            scribe = new Thread() {

                @Override
                public void run() {
                    try {
                        runScribe();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            };
            // Set the scribe thread off filling buffers
            scribe.start();
        }

        /**
         * Scan every byte read from the input and squirrel them away in buffers, one per line,
         * where lines are delimited by \r, \n or \r\n..
         *
         * @throws IOException
         */
        private void runScribe() throws IOException {
            int c;
            newBuffer();

            while ((c = in.read()) != -1) {

                byte b = (byte)c;

                if (c == '\n') {
                    // This is always the end of a line
                    buf.put(b);
                    emitBuffer();
                    newBuffer();

                } else if (seenCR) {
                    // The line ended just before the new character
                    emitBuffer();
                    newBuffer();
                    buf.put(b);

                } else if (c == '\r') {
                    // This may be the end of a line (if next is not '\n')
                    buf.put(b);
                    seenCR = true;

                } else {
                    // Not the end of a line, just accumulate
                    buf.put(b);
                }
            }

            // Emit a partial line if there is one.
            if (buf.position() > 0) {
                emitBuffer();
            }
        }

        private void newBuffer() {
            buf = ByteBuffer.allocate(BUFFER_SIZE);
            seenCR = false;
        }

        private void emitBuffer() {
            buf.flip();
            add(buf);
        }

        /**
         * Return the contents of the queue as a list of escaped strings, interpreting the bytes as
         * ASCII.
         *
         * @return contents as strings
         */
        public List<String> asStrings() {

            // Make strings here:
            StringBuilder sb = new StringBuilder(100);

            // Build a list of decoded buffers
            List<String> list = new LinkedList<String>();
            for (ByteBuffer bb : this) {
                stringDump(sb, bb);
                list.add(sb.toString());
                bb.rewind();
            }
            return list;
        }

        /**
         * Return a hex dump the contents of the object as a list of strings
         *
         * @return dump as strings
         */
        public List<String> asHexDump() {
            final int LEN = 16;
            StringBuilder sb = new StringBuilder(4 * LEN + 20);
            // Build a list of dumped buffer rows
            List<String> list = new LinkedList<String>();
            for (ByteBuffer bb : this) {
                int n;
                while ((n = bb.remaining()) >= LEN) {
                    hexDump(sb, bb, n, LEN);
                    list.add(sb.toString());
                }
                if (n > 0) {
                    hexDump(sb, bb, n, LEN);
                    list.add(sb.toString());
                }
                bb.rewind();
            }
            return list;
        }

        /**
         * Helper to format one line of hex dump output up to a maximum number of bytes.
         *
         * @param sb to overwrite with the line of dump output
         * @param bb from which to take the bytes
         * @param n number of bytes to take (up to <code>len</code>)
         * @param len maximum number of bytes to take
         */
        private static void hexDump(StringBuilder sb, ByteBuffer bb, int n, int len) {

            // Reset the string buffer
            sb.setLength(0);

            // Impose the limit
            if (n > len) {
                n = len;
            }

            // The data on this row of output start here in the ByteBuffer
            bb.mark();
            sb.append(String.format("%4d: ", bb.position()));

            // Output n of them
            for (int i = 0; i < n; i++) {
                sb.append(String.format(" %02x", bb.get()));
            }

            // And make it up to the proper width
            for (int i = n; i < len; i++) {
                sb.append("   ");
            }

            // Now go back to the start of the row and output printable characters
            bb.reset();
            sb.append("|");
            for (int i = 0; i < n; i++) {
                char c = (char)(0xff & bb.get());
                if (c < ' ' || c > 126) {
                    c = '.';
                }
                sb.append(c);
            }
        }
    }

}
