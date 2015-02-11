// Copyright (c) 2013 Jython Developers
package org.python.util;

import java.io.EOFException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import jline.console.ConsoleReader;
import jline.WindowsTerminal;
import jline.console.history.FileHistory;
import jnr.constants.platform.Errno;

import org.python.core.PlainConsole;
import org.python.core.PyObject;
import org.python.core.Py;

/**
 * This class uses <a href="http://jline.sourceforge.net/">JLine</a> to provide readline like
 * functionality to its console without requiring native readline support.
 */
public class JLineConsole extends PlainConsole {

    /** Main interface to JLine. */
    public ConsoleReader reader;
    /** Callable object set by <code>readline.set_startup_hook</code>. */
    protected PyObject startup_hook;
    /** <b>Not</b> currently set by <code>readline.set_pre_input_hook</code>. Why not? */
    protected PyObject pre_input_hook;
    /** Whether reader is a WindowsTerminal. */
    private boolean windows;
    /** The ctrl-z character String. */
    protected static final String CTRL_Z = "\u001a";
    /** Stream wrapping System.out in order to capture the last prompt. */
    private ConsoleOutputStream outWrapper;

    /**
     * Errno strerrors possibly caused by a SIGSTP (ctrl-z). They may propagate up to IOException
     * messages.
     */
    private static final List<String> SUSPENDED_STRERRORS = Arrays.asList(
            Errno.EINTR.description(), Errno.EIO.description());

    /**
     * Construct an instance of the console class specifying the character encoding. This encoding
     * must be one supported by the JVM.
     * <p>
     * Most of the initialisation is deferred to the {@link #install()} method so that any prior
     * console can uninstall itself before we change system console settings and
     * <code>System.in</code>.
     *
     * @param encoding name of a supported encoding or <code>null</code> for
     *            <code>Charset.defaultCharset()</code>
     */
    public JLineConsole(String encoding) {
        /*
         * Super-class needs the encoding in order to re-encode the characters that
         * jline.ConsoleReader.readLine() has decoded.
         */
        super(encoding);
        /*
         * Communicate the specified encoding to JLine. jline.ConsoleReader.readLine() edits a line
         * of characters, decoded from stdin.
         */
        System.setProperty("jline.WindowsTerminal.input.encoding", this.encoding);
        System.setProperty("input.encoding", this.encoding);
        // ... not "jline.UnixTerminal.input.encoding" as you might think, not even in JLine2
    }

    private static class HistoryCloser implements Runnable {
        FileHistory history;
        public HistoryCloser(FileHistory history) {
            this.history = history;
        }

        @Override
        public void run() {
            try {
                history.flush();
            } catch (IOException e) {
                // could not save console history, but quietly ignore in this case
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation overrides that by setting <code>System.in</code> to a
     * <code>FilterInputStream</code> object that wraps JLine, and <code>System.out</code>
     * with a layer that buffers incomplete lines for use as the console prompt.
     */
    @Override
    public void install() {
        String userHomeSpec = System.getProperty("user.home", ".");

        // Configure a ConsoleReader (the object that does most of the line editing).
        try {
            // Create the reader as unbuffered as possible
            InputStream in = new FileInputStream(FileDescriptor.in);
            reader = new ConsoleReader("jython", in, System.out, null, encoding);
            reader.setKeyMap("jython");
            reader.setHandleUserInterrupt(true);
            reader.setCopyPasteDetection(true);

            // We find the bell too noisy
            reader.setBellEnabled(false);

            // Do not attempt to expand ! in the input
            reader.setExpandEvents(false);

            /*
             * Everybody else, using sys.stdout or java.lang.System.out, gets to write on a special
             * PrintStream that keeps the last incomplete line in case it turns out to be a console
             * prompt.
             */
            outWrapper = new ConsoleOutputStream(System.out, reader.getTerminal().getWidth());
            System.setOut(new PrintStream(outWrapper, true, encoding));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Access and load (if possible) the line history.
        try {
            File historyFile = new File(userHomeSpec, ".jline-jython.history");
            FileHistory history = new FileHistory(historyFile);
            Runtime.getRuntime().addShutdownHook(new Thread(new HistoryCloser(history)));
            reader.setHistory(history);
        } catch (IOException e) {
            // oh well, no history from file
        }

        // Check for OS type
        windows = reader.getTerminal() instanceof WindowsTerminal;

        // Replace System.in
        FilterInputStream wrapper = new Stream();
        System.setIn(wrapper);
    }

    /**
     * Class to wrap the line-oriented interface to JLine with an InputStream that can replace
     * System.in.
     */
    private class Stream extends ConsoleInputStream {

        /** Create a System.in replacement with JLine that adds system-specific line endings */
        Stream() {
            super(System.in, encodingCharset, EOLPolicy.ADD, LINE_SEPARATOR);
        }

        @Override
        protected CharSequence getLine() throws IOException, EOFException {

            // Get a line and hope to be done. The prompt is the current partial output line.
            String prompt = outWrapper.getPrompt(encodingCharset).toString();
            String line = readerReadLine(prompt);

            if (!isEOF(line)) {
                return line;
            } else {
                // null or ctrl-z on Windows indicates EOF
                throw new EOFException();
            }
        }

    }

    /**
     * Wrapper on reader.readLine(prompt) that deals with retries (on Unix) when the user enters
     * ctrl-Z to background Jython, then brings it back to the foreground. The inherited
     * implementation says this is necessary and effective on BSD Unix.
     *
     * @param prompt to display
     * @return line of text read in
     * @throws IOException if an error occurs (other than an end of suspension)
     * @throws EOFException if an EOF is detected
     */
    private String readerReadLine(String prompt) throws IOException, EOFException {

        // We must be prepared to try repeatedly since the read may be interrupted.

        while (true) {

            try {
                // If there's a hook, call it
                if (startup_hook != null) {
                    startup_hook.__call__();
                }

                try {
                    // Resumption from control-Z suspension may occur without JLine telling us
                    // Work around by putting the terminal into a well-known state before
                    // each read line, if possible
                    reader.getTerminal().init();
                } catch (Exception exc) {}

                // Send the cursor to the start of the line (no prompt, empty buffer).
                reader.setPrompt(null);
                reader.redrawLine();

                // The prompt is whatever was already on the line.
                return reader.readLine(prompt);
            } catch (IOException ioe) {
                // Something went wrong, or we were interrupted (seems only BSD throws this)
                if (!fromSuspend(ioe)) {
                    // The interruption is not the result of (the end of) a ctrl-Z suspension
                    throw ioe;

                } else {
                    // The interruption seems to be (return from) a ctrl-Z suspension:
                    try {
                        // Must reset JLine and continue (not repeating the prompt)
                        reader.resetPromptLine (prompt, null, 0);
                        prompt = "";
                    } catch (Exception e) {
                        // Do our best to say what went wrong
                        throw new IOException("Failed to re-initialize JLine: " + e.getMessage());
                    }
                }
            }
        }

    }

    /**
     * Determine if the IOException was likely caused by a SIGSTP (ctrl-z). Seems only applicable to
     * BSD platforms.
     */
    private boolean fromSuspend(IOException ioe) {
        return !windows && SUSPENDED_STRERRORS.contains(ioe.getMessage());
    }

    /**
     * Determine if line denotes an EOF.
     */
    private boolean isEOF(String line) {
        return line == null || (windows && CTRL_Z.equals(line));
    }

    /**
     * @return the JLine console reader associated with this interpreter
     */
    public ConsoleReader getReader() {
        return reader;
    }

    /**
     * @return the startup hook (called prior to each readline)
     */
    public PyObject getStartupHook() {
        return startup_hook;
    }

    /**
     * Sets the startup hook (called prior to each readline)
     */
    public void setStartupHook(PyObject hook) {
        // Convert None to null here, so that readerReadLine can use only a null check
        if (hook == Py.None) {
            hook = null;
        }
        startup_hook = hook;
    }
}
