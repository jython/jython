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

    public void run() {
        Py.setSystemState(systemState);
        try {
            func.__call__(args);
        } catch (PyException exc) {
            Py.printException(exc);
        }
    }
}