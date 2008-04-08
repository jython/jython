// Copyright (c) Corporation for National Research Initiatives
package org.python.util;

import org.python.core.Py;
import org.python.core.PyBuiltinFunctionSet;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.core.__builtin__;

// Based on CPython-1.5.2's code module
public class InteractiveConsole extends InteractiveInterpreter {

    public static final String CONSOLE_FILENAME = "<stdin>";

    public String filename;

    public InteractiveConsole() {
        this(null, CONSOLE_FILENAME);
    }

    public InteractiveConsole(PyObject locals) {
        this(locals, CONSOLE_FILENAME);
    }

    public InteractiveConsole(PyObject locals, String filename) {
        this(locals, filename, false);
    }

    /**
     * @param replaceRawInput -
     *            if true, we hook this Class's raw_input into the builtins
     *            table so that clients like cmd.Cmd use it.
     */
    public InteractiveConsole(PyObject locals, String filename, boolean replaceRawInput) {
        super(locals);
        this.filename = filename;
        if(replaceRawInput) {
            PyObject newRawInput = new PyBuiltinFunctionSet("raw_input", 0, 0, 1) {

                public PyObject __call__() {
                    return __call__(Py.EmptyString);
                }

                public PyObject __call__(PyObject prompt) {
                    return Py.newString(raw_input(prompt));
                }
            };
            PySystemState.builtins.__setitem__("raw_input", newRawInput);
        }
    }

    /**
     * Closely emulate the interactive Python console.
     * 
     * The optional banner argument specifies the banner to print before the
     * first interaction; by default it prints "Jython <version> on <platform>".
     */
    public void interact() {
        interact(getDefaultBanner());
    }

    public static String getDefaultBanner() {
        return "Jython " + PySystemState.version + " on " + PySystemState.platform;
    }

    public void interact(String banner) {
        if(banner != null) {
            write(banner);
            write("\n");
        }
        // Dummy exec in order to speed up response on first command
        exec("2");
        // System.err.println("interp2");
        boolean more = false;
        while(true) {
            PyObject prompt = more ? systemState.ps2 : systemState.ps1;
            String line;
            try {
                line = raw_input(prompt);
            } catch(PyException exc) {
                if(!Py.matchException(exc, Py.EOFError))
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
     * newlines. The line is appended to a buffer and the interpreter's
     * runsource() method is called with the concatenated contents of the buffer
     * as source. If this indicates that the command was executed or invalid,
     * the buffer is reset; otherwise, the command is incomplete, and the buffer
     * is left as it was after the line was appended. The return value is 1 if
     * more input is required, 0 if the line was dealt with in some way (this is
     * the same as runsource()).
     */
    public boolean push(String line) {
        if(buffer.length() > 0)
            buffer.append("\n");
        buffer.append(line);
        boolean more = runsource(buffer.toString(), filename);
        if(!more)
            resetbuffer();
        return more;
    }

    /**
     * Write a prompt and read a line.
     * 
     * The returned line does not include the trailing newline. When the user
     * enters the EOF key sequence, EOFError is raised.
     * 
     * The base implementation uses the built-in function raw_input(); a
     * subclass may replace this with a different implementation.
     */
    public String raw_input(PyObject prompt) {
        return __builtin__.raw_input(prompt);
    }
}
