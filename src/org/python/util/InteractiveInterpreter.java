// Copyright (c) Corporation for National Research Initiatives
package org.python.util;
import org.python.core.*;

// Based on CPython-1.5.2's code module

public class InteractiveInterpreter extends PythonInterpreter {
    public InteractiveInterpreter() {
        this(null);
    }
    public InteractiveInterpreter(PyObject locals) {
        this(locals, null);

    }
        public InteractiveInterpreter(PyObject locals, PySystemState systemState) {
            super(locals, systemState);
            cflags = new CompilerFlags();
        }

    /**
     * Compile and run some source in the interpreter.
     *
     * Arguments are as for compile_command().
     *
     * One several things can happen:
     *
     * 1) The input is incorrect; compile_command() raised an exception
     * (SyntaxError or OverflowError).  A syntax traceback will be printed
     * by calling the showsyntaxerror() method.
     *
     * 2) The input is incomplete, and more input is required;
     * compile_command() returned None.  Nothing happens.
     *
     * 3) The input is complete; compile_command() returned a code object.
     * The code is executed by calling self.runcode() (which also handles
     * run-time exceptions, except for SystemExit).
     *
     * The return value is 1 in case 2, 0 in the other cases (unless an
     * exception is raised).  The return value can be used to decide
     * whether to use sys.ps1 or sys.ps2 to prompt the next line.
     **/
    public boolean runsource(String source) {
        return runsource(source, "<input>", "single");
    }

    public boolean runsource(String source, String filename) {
        return runsource(source, filename, "single");
    }

    public boolean runsource(String source, String filename, String kind) {
        PyObject code;
        try {
            code = Py.compile_command_flags(source, filename, kind, cflags, true);
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.SyntaxError)) {
                // Case 1
                showexception(exc);
                return false;
            } else if (Py.matchException(exc, Py.ValueError) ||
                       Py.matchException(exc, Py.OverflowError)) {
                // Should not print the stack trace, just the error.
                showexception(exc);
                return false;
            } else {
                throw exc;
            }
        }
        // Case 2
        if (code == Py.None)
            return true;
        // Case 3
        runcode(code);
        return false;
    }

    /**
     * execute a code object.
     *
     * When an exception occurs, self.showtraceback() is called to display
     * a traceback.  All exceptions are caught except SystemExit, which is
     * reraised.
     *
     * A note about KeyboardInterrupt: this exception may occur elsewhere
     * in this code, and may not always be caught.  The caller should be
     * prepared to deal with it.
     **/

    // Make this run in another thread somehow????
    public void runcode(PyObject code) {
        try {
            exec(code);
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.SystemExit)) throw exc;
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

    public StringBuffer buffer = new StringBuffer();
    public String filename="<stdin>";

    public void resetbuffer() {
        buffer.setLength(0);
    }

    /** Pause the current code, sneak an exception raiser into
     * sys.trace_func, and then continue the code hoping that Jython will
     * get control to do the break;
     **/
    public void interrupt(ThreadState ts) {
        TraceFunction breaker = new BreakTraceFunction();
        TraceFunction oldTrace = ts.tracefunc;
        ts.tracefunc = breaker;
        if (ts.frame != null)
            ts.frame.tracefunc = breaker;
        ts.tracefunc = oldTrace;
        //ts.thread.join();
    }
}

class BreakTraceFunction extends TraceFunction {
    private void doBreak() {
        throw new Error("Python interrupt");
        //Thread.currentThread().interrupt();
    }

    public TraceFunction traceCall(PyFrame frame) {
        doBreak();
        return null;
    }

    public TraceFunction traceReturn(PyFrame frame, PyObject ret) {
        doBreak();
        return null;
    }

    public TraceFunction traceLine(PyFrame frame, int line) {
        doBreak();
        return null;
    }

    public TraceFunction traceException(PyFrame frame, PyException exc) {
        doBreak();
        return null;
    }
}
