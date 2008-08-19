package org.python.core;

import org.python.modules._systemrestart;

public class FunctionThread extends Thread
{
    private final PyObject func;
    private final PyObject[] args;
    private final PySystemState systemState;

    public FunctionThread(PyObject func, PyObject[] args, long stack_size, ThreadGroup group) {
        super(group, null, "Thread", stack_size);
        this.func = func;
        this.args = args;
        this.systemState = Py.getSystemState();
    }

    public void run() {
        Py.setSystemState(systemState);
        try {
            func.__call__(args);
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.SystemExit) ||
                    Py.matchException(exc, _systemrestart.SystemRestart)) {
                return;
            }
            Py.stderr.println("Unhandled exception in thread started by " + func);
            Py.printException(exc);
        }
    }

    @Override
    public String toString() {
        ThreadGroup group = getThreadGroup();
        if (group != null) {
            return String.format("FunctionThread[%s,%s,%s]", getName(), getPriority(),
                                 group.getName());
        } else {
            return String.format("FunctionThread[%s,%s]", getName(), getPriority());
        }
    }
}
