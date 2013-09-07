// Copyright (c) 2013 Jython Developers
package org.python.util;

import java.io.EOFException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import jline.ConsoleReader;
import jline.Terminal;
import jline.WindowsTerminal;
import jnr.constants.platform.Errno;

import org.python.core.PlainConsole;
import org.python.core.PyObject;

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

    /**
     * {@inheritDoc}
     * <p>
     * This implementation overrides that by setting <code>System.in</code> to a
     * <code>FilterInputStream</code> object that wraps JLine.
     */
    @Override
    public void install() {
        Terminal.setupTerminal();

        String userHomeSpec = System.getProperty("user.home", ".");

        // Configure a ConsoleReader (the object that does most of the line editing).
        try {
            /*
             * Wrap System.out in the specified encoding. jline.ConsoleReader.readLine() echoes the
             * line through this Writer.
             */
            Writer out = new PrintWriter(new OutputStreamWriter(System.out, encoding));

            // Get the key bindings (built in ones treat TAB Pythonically).
            InputStream bindings = getBindings(userHomeSpec, getClass().getClassLoader());

            // Create the reader as unbuffered as possible
            InputStream in = new FileInputStream(FileDescriptor.in);
            reader = new ConsoleReader(in, out, bindings);

            // We find the bell too noisy
            reader.setBellEnabled(false);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Access and load (if possible) the line history.
        try {
            File historyFile = new File(userHomeSpec, ".jline-jython.history");
            reader.getHistory().setHistoryFile(historyFile);
        } catch (IOException e) {
            // oh well, no history from file
        }

        // Check for OS type
        windows = reader.getTerminal() instanceof WindowsTerminal;

        // Replace System.in
        FilterInputStream wrapper = new Stream();
        System.setIn(wrapper);
    }

    // Inherited raw_input() is adequate: calls input()

    /**
     * {@inheritDoc}
     * <p>
     * This console implements <code>input</code> using JLine to handle the prompt and data entry,
     * so that the cursor may be correctly handled in relation to the prompt string.
     */
    @Override
    public CharSequence input(CharSequence prompt) throws IOException, EOFException {
        // Get the line from the console via the library
        String line = readerReadLine(prompt.toString());
        if (line == null) {
            throw new EOFException();
        } else {
            return line;
        }
    }

    /**
     * Class to wrap the line-oriented interface to JLine with an InputStream that can replace
     * System.in.
     */
    private class Stream extends ConsoleStream {

        /** Create a System.in replacement with JLine that adds system-specific line endings */
        Stream() {
            super(encodingCharset, EOLPolicy.ADD, LINE_SEPARATOR);
        }

        @Override
        protected CharSequence getLine() throws IOException, EOFException {

            // Get a line and hope to be done. Suppress any remembered prompt.
            String line = readerReadLine("");

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
     * cvtrl-Z to background Jython, the brings it back to the foreground. The inherited
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
                // Get a line and hope to be done.
                String line = reader.readLine(prompt);
                return line;

            } catch (IOException ioe) {
                // Something went wrong, or we were interrupted (seems only BSD throws this)
                if (!fromSuspend(ioe)) {
                    // The interruption is not the result of (the end of) a ctrl-Z suspension
                    throw ioe;

                } else {
                    // The interruption seems to be (return from) a ctrl-Z suspension:
                    try {
                        // Must reset JLine and continue (not repeating the prompt)
                        reader.getTerminal().initializeTerminal();
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
     * Return the JLine bindings file.
     * 
     * This handles loading the user's custom key bindings (normally JLine does) so it can fall back
     * to Jython's (which disable tab completion) when the user's are not available.
     * 
     * @return an InputStream of the JLine bindings file.
     */
    protected static InputStream getBindings(String userHomeSpec, ClassLoader loader) {

        // The key bindings file may be specified explicitly
        String bindingsFileSpec = System.getProperty("jline.keybindings");
        File bindingsFile;

        if (bindingsFileSpec != null) {
            // Bindings file explicitly specified
            bindingsFile = new File(bindingsFileSpec);
        } else {
            // Otherwise try ~/.jlinebindings.properties
            bindingsFile = new File(userHomeSpec, ".jlinebindings.properties");
        }

        // See if that file really exists (and can be read)
        try {
            if (bindingsFile.isFile()) {
                try {
                    return new FileInputStream(bindingsFile);
                } catch (FileNotFoundException fnfe) {
                    // Shouldn't really ever happen
                    fnfe.printStackTrace();
                }
            }
        } catch (SecurityException se) {
            // continue
        }

        // User/specific key bindings could not be read: use the ones from the class path or jar.
        return loader.getResourceAsStream("org/python/util/jline-keybindings.properties");
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
        startup_hook = hook;
    }
}
