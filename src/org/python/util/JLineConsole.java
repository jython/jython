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

import jline.ConsoleReader;
import jline.Terminal;

import org.python.core.Py;
import org.python.core.PyObject;

/**
 * This class uses <a href="http://jline.sourceforge.net/">JLine</a> to provide
 * readline like functionality to its console without requiring native readline
 * support.
 */
public class JLineConsole extends InteractiveConsole {

    protected ConsoleReader reader;

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

    public String raw_input(PyObject prompt) {
        String line = null;
        try {
            line = reader.readLine(prompt.toString());
        } catch (IOException io) {
            throw Py.IOError(io);
        }
        if (line == null) {
            throw Py.EOFError("Ctrl-D exit");
        }
        return line.endsWith("\n") ? line.substring(0, line.length() - 1) : line;
    }
}
