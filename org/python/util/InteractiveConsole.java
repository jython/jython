// Copyright (c) Corporation for National Research Initiatives
package org.python.util;
import org.python.core.*;

// Based on CPython-1.5.2's code module

public class InteractiveConsole extends InteractiveInterpreter {
    public String filename;

    public InteractiveConsole() {
        this(null, "<console>");
    }
    public InteractiveConsole(PyObject locals) {
        this(locals, "<console>");
    }
    public InteractiveConsole(PyObject locals, String filename) {
        super(locals);
        this.filename = filename;
    }

    /**
     * Closely emulate the interactive Python console.
     *
     * The optional banner argument specifies the banner to print before
     * the first interaction; by default it prints a banner similar to the
     * one printed by the real Python interpreter, followed by the current
     * class name in parentheses (so as not to confuse this with the real
     * interpreter -- since it's so close!).
     **/
    public void interact() {
        interact(getDefaultBanner());
    }

    public static String getDefaultBanner() {
        String compiler = System.getProperty("java.compiler");
        
        return "Jython " + PySystemState.version + " on " +
            PySystemState.platform + " (JIT: " +
            ((compiler == null) ? "null" : compiler) +  ")";
    }

    public void interact(String banner) {
        if (banner != null) {
            write(banner);
            write("\n");
        }
        // Dummy exec in order to speed up response on first command
        exec("2");
        //System.err.println("interp2");
        boolean more = false;
        while (true) {
            PyObject prompt = more ? systemState.ps2 : systemState.ps1;
            String line;
            try {
                line = raw_input(prompt);
            } catch (PyException exc) {
                if (!Py.matchException(exc, Py.EOFError))
                    throw exc;
                write("\n");
                break;
            }
            more = push(line);
        }
    }

    /**
     * Push a line to the interpreter.
     *
     * The line should not have a trailing newline; it may have internal
     * newlines.  The line is appended to a buffer and the interpreter's
     * runsource() method is called with the concatenated contents of the
     * buffer as source.  If this indicates that the command was executed
     * or invalid, the buffer is reset; otherwise, the command is
     * incomplete, and the buffer is left as it was after the line was
     * appended.  The return value is 1 if more input is required, 0 if the
     * line was dealt with in some way (this is the same as runsource()).
     **/

    public boolean push(String line) {
        if (buffer.length() > 0)
            buffer.append("\n");
        buffer.append(line);
        boolean more = runsource(buffer.toString(), filename);
        if (!more)
            resetbuffer();
        return more;
    }

    /**
     * Write a prompt and read a line.
     *
     * The returned line does not include the trailing newline.  When the
     * user enters the EOF key sequence, EOFError is raised.
     *
     * The base implementation uses the built-in function raw_input(); a
     * subclass may replace this with a different implementation.
     **/
    public String raw_input(PyObject prompt) {
        return __builtin__.raw_input(prompt);
    }
}
