// Copyright (c) Corporation for National Research Initiatives
package org.python.util;

import org.python.core.*;

/**
 * This class provides the interface for compiling and running code that supports an interactive
 * interpreter.
 */
// Based on CPython-1.5.2's code module
public class InteractiveInterpreter extends PythonInterpreter {

    /**
     * Construct an InteractiveInterpreter with all default characteristics: default state (from
     * {@link Py#getSystemState()}), and a new empty dictionary of local variables.
     * */
    public InteractiveInterpreter() {
        this(null);
    }

    /**
     * Construct an InteractiveInterpreter with state (from {@link Py#getSystemState()}), and the
     * specified dictionary of local variables.
     *
     * @param locals dictionary to use, or if <code>null</code>, a new empty one will be created
     */
    public InteractiveInterpreter(PyObject locals) {
        this(locals, null);
    }

    /**
     * Construct an InteractiveInterpreter with, and system state the specified dictionary of local
     * variables.
     *
     * @param locals dictionary to use, or if <code>null</code>, a new empty one will be created
     * @param systemState interpreter state, or if <code>null</code> use {@link Py#getSystemState()}
     */
    public InteractiveInterpreter(PyObject locals, PySystemState systemState) {
        super(locals, systemState);
    }

    /**
     * Compile and run some source in the interpreter, in the mode {@link CompileMode#single} which
     * is used for incremental compilation at the interactive console, known as "<input>".
     *
     * @param source Python code
     * @return <code>true</code> to indicate a partial statement was entered
     */
    public boolean runsource(String source) {
        return runsource(source, "<input>", CompileMode.single);
    }

    /**
     * Compile and run some source in the interpreter, in the mode {@link CompileMode#single} which
     * is used for incremental compilation at the interactive console.
     *
     * @param source Python code
     * @param filename name with which to label this console input (e.g. in error messages).
     * @return <code>true</code> to indicate a partial statement was entered
     */
    public boolean runsource(String source, String filename) {
        return runsource(source, filename, CompileMode.single);
    }

    /**
     * Compile and run some source in the interpreter, according to the {@link CompileMode} given.
     * This method supports incremental compilation and interpretation through the return value,
     * where <code>true</code> signifies that more input is expected in order to complete the Python
     * statement. An interpreter can use this to decide whether to use <code>sys.ps1</code> ("
     * <code>>>> </code> ") or <code>sys.ps2</code> ("<code>... </code> ") to prompt the next line.
     * The arguments are the same as the mandatory ones in the Python <code>compile()</code>
     * command.
     * <p>
     * One the following can happen:
     * <ol>
     * <li>The input is incorrect; compilation raised an exception (SyntaxError or OverflowError). A
     * syntax traceback will be printed by calling {@link #showexception(PyException)}. Return is
     * <code>false</code>.</li>
     *
     * <li>The input is incomplete, and more input is required; compilation returned no code.
     * Nothing happens. Return is <code>true</code>.</li>
     *
     * <li>The input is complete; compilation returned a code object. The code is executed by
     * calling {@link #runcode(PyObject)} (which also handles run-time exceptions, except for
     * SystemExit). Return is <code>false</code>.</li>
     * </ol>
     *
     * @param source Python code
     * @param filename name with which to label this console input (e.g. in error messages).
     * @param kind of compilation required: {@link CompileMode#eval}, {@link CompileMode#exec} or
     *            {@link CompileMode#single}
     * @return <code>true</code> to indicate a partial statement was provided
     */
    public boolean runsource(String source, String filename, CompileMode kind) {
        PyObject code;
        try {
            code = Py.compile_command_flags(source, filename, kind, cflags, true);
        } catch (PyException exc) {
            if (exc.match(Py.SyntaxError)) {
                // Case 1
                showexception(exc);
                return false;
            } else if (exc.match(Py.ValueError) || exc.match(Py.OverflowError)) {
                // Should not print the stack trace, just the error.
                showexception(exc);
                return false;
            } else {
                throw exc;
            }
        }
        // Case 2
        if (code == Py.None) {
            return true;
        }
        // Case 3
        runcode(code);
        return false;
    }

    /**
     * Execute a code object. When an exception occurs, {@link #showexception(PyException)} is
     * called to display a stack trace, except in the case of SystemExit, which is re-raised.
     * <p>
     * A note about KeyboardInterrupt: this exception may occur elsewhere in this code, and may not
     * always be caught. The caller should be prepared to deal with it.
     **/

    // Make this run in another thread somehow????
    public void runcode(PyObject code) {
        try {
            exec(code);
        } catch (PyException exc) {
            if (exc.match(Py.SystemExit)) {
                throw exc;
            }
            showexception(exc);
        }
    }

    public void showexception(PyException exc) {
        // Should probably add code to handle skipping top stack frames
        // somehow...
        Py.printException(exc);
    }

    public void write(String data) {
        Py.stderr.write(data);
    }

    public StringBuilder buffer = new StringBuilder();
    public String filename = "<stdin>";

    public void resetbuffer() {
        buffer.setLength(0);
    }

    /**
     * Pause the current code, sneak an exception raiser into sys.trace_func, and then continue the
     * code hoping that Jython will get control to do the break;
     **/
    public void interrupt(ThreadState ts) {
        TraceFunction breaker = new BreakTraceFunction();
        TraceFunction oldTrace = ts.tracefunc;
        ts.tracefunc = breaker;
        if (ts.frame != null) {
            ts.frame.tracefunc = breaker;
        }
        ts.tracefunc = oldTrace;
        // ts.thread.join();
    }
}


class BreakTraceFunction extends TraceFunction {

    private void doBreak() {
        throw new Error("Python interrupt");
        // Thread.currentThread().interrupt();
    }

    @Override
    public TraceFunction traceCall(PyFrame frame) {
        doBreak();
        return null;
    }

    @Override
    public TraceFunction traceReturn(PyFrame frame, PyObject ret) {
        doBreak();
        return null;
    }

    @Override
    public TraceFunction traceLine(PyFrame frame, int line) {
        doBreak();
        return null;
    }

    @Override
    public TraceFunction traceException(PyFrame frame, PyException exc) {
        doBreak();
        return null;
    }
}
