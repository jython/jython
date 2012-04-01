package org.python.core;

import java.util.concurrent.atomic.AtomicInteger;

import org.python.modules._systemrestart;

public class FunctionThread extends Thread
{
    private final PyObject func;
    private final PyObject[] args;
    private final PySystemState systemState;
    private static AtomicInteger counter = new AtomicInteger();

    public FunctionThread(PyObject func, PyObject[] args, long stack_size, ThreadGroup group) {
        super(group, null, "Thread", stack_size);
        this.func = func;
        this.args = args;
        this.systemState = Py.getSystemState();
        this.setName("Thread-"+Integer.toString(counter.incrementAndGet()));
    }

    public void run() {
        Py.setSystemState(systemState);
        try {
            func.__call__(args);
        } catch (PyException exc) {
            if (exc.match(Py.SystemExit) || exc.match(_systemrestart.SystemRestart)) {
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
