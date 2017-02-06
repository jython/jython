// Copyright (c) Corporation for National Research Initiatives
package org.python.util;

import org.python.core.Py;
import org.python.core.PyBuiltinFunctionSet;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.core.__builtin__;

/**
 * This class provides the read, execute, print loop needed by a Python console; it is not actually
 * a console itself. The primary capability is the {@link #interact()} method, which repeatedly
 * calls {@link #raw_input(PyObject)}, and hence {@link __builtin__#raw_input(PyObject)}, in order
 * to get lines, and {@link #push(String)} them into the interpreter. The built-in
 * <code>raw_input()</code> method prompts on <code>sys.stdout</code> and reads from
 * <code>sys.stdin</code>, the standard console. These may be redirected using
 * {@link #setOut(java.io.OutputStream)} and {@link #setIn(java.io.InputStream)}, as may also
 * <code>sys.stderr</code>.
 */
// Based on CPython-1.5.2's code module
public class InteractiveConsole extends InteractiveInterpreter {

    /**
     * Note: This field is actually final; don't modify.
     * <p>
     * To work around an issue in javadoc with Java 8 we cannot have it final for now, see
     * <a href="http://bugs.jython.org/issue2539" target="_blank"> issue 2539</a> for details.
     */
    public static String CONSOLE_FILENAME = "<stdin>";

    public String filename;

    /**
     * Construct an interactive console, which will "run" when {@link #interact()} is called. The
     * name of the console (e.g. in error messages) will be {@value #CONSOLE_FILENAME}.
     */
    public InteractiveConsole() {
        this(null, CONSOLE_FILENAME);
    }

    /**
     * Construct an interactive console, which will "run" when {@link #interact()} is called. The
     * name of the console (e.g. in error messages) will be {@value #CONSOLE_FILENAME}.
     *
     * @param locals dictionary to use, or if <code>null</code>, a new empty one will be created
     */
    public InteractiveConsole(PyObject locals) {
        this(locals, CONSOLE_FILENAME);
    }

    /**
     * Construct an interactive console, which will "run" when {@link #interact()} is called.
     *
     * @param locals dictionary to use, or if <code>null</code>, a new empty one will be created
     * @param filename name with which to label this console input (e.g. in error messages).
     */
    public InteractiveConsole(PyObject locals, String filename) {
        this(locals, filename, false);
    }

    /**
     * Full-feature constructor for an interactive console, which will "run" when
     * {@link #interact()} is called. This version allows the caller to replace the built-in
     * raw_input() methods with {@link #raw_input(PyObject)} and
     * {@link #raw_input(PyObject, PyObject)}, which may be overridden in a sub-class.
     *
     * @param locals dictionary to use, or if <code>null</code>, a new empty one will be created
     * @param filename name with which to label this console input
     * @param replaceRawInput if true, hook this class's <code>raw_input</code> into the built-ins.
     */
    public InteractiveConsole(PyObject locals, String filename, boolean replaceRawInput) {
        super(locals);
        this.filename = filename;
        if (replaceRawInput) {
            PyObject newRawInput = new PyBuiltinFunctionSet("raw_input", 0, 0, 1) {

                @Override
                public PyObject __call__() {
                    return __call__(Py.EmptyString);
                }

                @Override
                public PyObject __call__(PyObject prompt) {
                    return Py.newString(raw_input(prompt));
                }
            };
            Py.getSystemState().getBuiltins().__setitem__("raw_input", newRawInput);
        }
    }

    /**
     * Operate a Python console, as in {@link #interact(String, PyObject)}, on the standard input.
     * The standard input may have been redirected by {@link #setIn(java.io.InputStream)} or its
     * variants. The banner (printed before first input) is obtained by calling
     * {@link #getDefaultBanner()}.
     */
    public void interact() {
        interact(getDefaultBanner(), null);
    }

    /**
     * Returns the banner to print before the first interaction: "Jython <version> on <platform>".
     *
     * @return the banner.
     */
    public static String getDefaultBanner() {
        return String
                .format("Jython %s on %s", PySystemState.version, Py.getSystemState().platform);
    }

    /**
     * Operate a Python console by repeatedly calling {@link #raw_input(PyObject, PyObject)} and
     * interpreting the lines read. An end of file causes the method to return.
     *
     * @param banner to print before accepting input, or if <code>null</code>, no banner.
     * @param file from which to read commands, or if <code>null</code>, read the console.
     */
    public void interact(String banner, PyObject file) {
        PyObject old_ps1 = systemState.ps1;
        PyObject old_ps2 = systemState.ps2;
        systemState.ps1 = new PyString(">>> ");
        systemState.ps2 = new PyString("... ");
        try {
            _interact(banner, file);
        } finally {
            systemState.ps1 = old_ps1;
            systemState.ps2 = old_ps2;
        }
    }

    public void _interact(String banner, PyObject file) {
        if (banner != null) {
            write(banner);
            write("\n");
        }
        // Dummy exec in order to speed up response on first command
        exec("2");
        // System.err.println("interp2");
        boolean more = false;
        while (true) {
            PyObject prompt = more ? systemState.ps2 : systemState.ps1;
            String line;
            try {
                if (file == null) {
                    line = raw_input(prompt);
                } else {
                    line = raw_input(prompt, file);
                }
            } catch (PyException exc) {
                if (!exc.match(Py.EOFError)) {
                    throw exc;
                }
                if (banner != null) {
                    write("\n");
                }
                break;
            } catch (Throwable t) {
                // catch jline.console.UserInterruptException, rethrow as a KeyboardInterrupt
                throw Py.JavaError(t);
                // One would expect that it would be possible to then catch the KeyboardInterrupt at the
                // bottom of this loop, however, for some reason the control-C restores the input text,
                // so simply doing
                // resetbuffer(); more = false;
                // is not sufficient
            }
            more = push(line);
        }
    }

    /**
     * Push a line to the interpreter.
     *
     * The line should not have a trailing newline; it may have internal newlines. The line is
     * appended to a buffer and the interpreter's runsource() method is called with the concatenated
     * contents of the buffer as source. If this indicates that the command was executed or invalid,
     * the buffer is reset; otherwise, the command is incomplete, and the buffer is left as it was
     * after the line was appended. The return value is 1 if more input is required, 0 if the line
     * was dealt with in some way (this is the same as runsource()).
     */
    public boolean push(String line) {
        if (buffer.length() > 0) {
            buffer.append("\n");
        }
        buffer.append(line);
        boolean more = runsource(buffer.toString(), filename);
        if (!more) {
            resetbuffer();
        }
        return more;
    }

    /**
     * Write a prompt and read a line from standard input. The returned line does not include the
     * trailing newline. When the user enters the EOF key sequence, EOFError is raised. The base
     * implementation uses the built-in function raw_input(); a subclass may replace this with a
     * different implementation.
     */
    public String raw_input(PyObject prompt) {
        return __builtin__.raw_input(prompt);
    }

    /**
     * Write a prompt and read a line from a file.
     */
    public String raw_input(PyObject prompt, PyObject file) {
        return __builtin__.raw_input(prompt, file);
    }
}
