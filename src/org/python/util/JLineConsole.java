package org.python.util;

import java.io.IOException;
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

    public JLineConsole() {
        this(null);
    }

    public JLineConsole(PyObject locals) {
        this(locals, CONSOLE_FILENAME);
    }

    public JLineConsole(PyObject locals, String filename) {
        super(locals, filename, true);
        Terminal.setupTerminal();
        try {
            reader = new ConsoleReader();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String raw_input(PyObject prompt) {
        String line = null;
        try {
            line = reader.readLine(prompt.toString());
        } catch(IOException io) {
            throw Py.IOError(io);
        }
        if(line == null) {
            throw Py.EOFError("Ctrl-D exit");
        }
        return line.endsWith("\n") ? line.substring(0, line.length() - 1) : line;
    }

    private ConsoleReader reader;
}
