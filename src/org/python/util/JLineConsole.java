/* Copyright (c) Jython Developers */
package org.python.util;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import com.kenai.constantine.platform.Errno;

import jline.ConsoleReader;
import jline.Terminal;
import jline.WindowsTerminal;

import org.python.core.Py;
import org.python.core.PyObject;

/**
 * This class uses <a href="http://jline.sourceforge.net/">JLine</a> to provide
 * readline like functionality to its console without requiring native readline
 * support.
 */
public class JLineConsole extends InteractiveConsole {

    /** Main interface to JLine. */
    protected ConsoleReader reader;

    /** Whether reader is a WindowsTerminal. */
    private boolean windows;

    /** The ctrl-z character String. */
    protected static final String CTRL_Z = "\u001a";

    /**
     * Errno strerrors possibly caused by a SIGSTP (ctrl-z). They may propagate up to
     * IOException messages.
     */
    private static final List<String> SUSPENDED_STRERRORS =
            Arrays.asList(Errno.EINTR.description(), Errno.EIO.description());

    public JLineConsole() {
        this(null);
    }

    public JLineConsole(PyObject locals) {
        this(locals, CONSOLE_FILENAME);
        try {
            File historyFile = new File(System.getProperty("user.home"), ".jline-jython.history");
            reader.getHistory().setHistoryFile(historyFile);
        } catch (IOException e) {
            // oh well, no history from file
        }
    }

    public JLineConsole(PyObject locals, String filename) {
        super(locals, filename, true);

        // Disable JLine's unicode handling so it yields raw bytes
        System.setProperty("jline.UnixTerminal.input.encoding", "ISO-8859-1");
        System.setProperty("jline.WindowsTerminal.input.encoding", "ISO-8859-1");

        Terminal.setupTerminal();
        try {
            InputStream input = new FileInputStream(FileDescriptor.in);
            // Raw bytes in, so raw bytes out
            Writer output = new OutputStreamWriter(new FileOutputStream(FileDescriptor.out),
                                                   "ISO-8859-1");
            reader = new ConsoleReader(input, output, getBindings());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        windows = reader.getTerminal() instanceof WindowsTerminal;
    }

    /**
     * Return the JLine bindings file.
     *
     * This handles loading the user's custom keybindings (normally JLine does) so it can
     * fallback to Jython's (which disable tab completition) when the user's are not
     * available.
     *
     * @return an InputStream of the JLine bindings file.
     */
    protected InputStream getBindings() {
        String userBindings = new File(System.getProperty("user.home"),
                                       ".jlinebindings.properties").getAbsolutePath();
        File bindingsFile = new File(System.getProperty("jline.keybindings", userBindings));

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
        return getClass().getResourceAsStream("jline-keybindings.properties");
    }

    @Override
    public String raw_input(PyObject prompt) {
        String line = null;
        String promptString = prompt.toString();

        while (true) {
            try {
                line = reader.readLine(promptString);
                break;
            } catch (IOException ioe) {
                if (!fromSuspend(ioe)) {
                    throw Py.IOError(ioe);
                }

                // Hopefully an IOException caused by ctrl-z (seems only BSD throws this).
                // Must reset jline to continue
                try {
                    reader.getTerminal().initializeTerminal();
                } catch (Exception e) {
                    throw Py.IOError(e.getMessage());
                }
                // Don't redisplay the prompt
                promptString = "";
            }
        }

        if (isEOF(line)) {
            throw Py.EOFError("");
        }

        return line;
    }

    /**
     * Determine if the IOException was likely caused by a SIGSTP (ctrl-z). Seems only
     * applicable to BSD platforms.
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
}
