package org.python.core;
    
public class FunctionThread extends Thread
{
    private final PyObject func;
    private final PyObject[] args;
    private final PySystemState systemState;

    public FunctionThread(PyObject func, PyObject[] args) {
        super();
        this.func = func;
        this.args = args;
        this.systemState = Py.getSystemState();
    }
        
    public FunctionThread(PyObject func, PyObject[] args, long stack_size) {
        super(null, null, "Thread", stack_size);
        this.func = func;
        this.args = args;
        this.systemState = Py.getSystemState();
    }

    public void run() {
        Py.setSystemState(systemState);
        try {
            func.__call__(args);
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.SystemExit)) {
                return;
            }
            Py.stderr.println("Unhandled exception in thread started by " + func);
            Py.printException(exc);
        }
    }
}
