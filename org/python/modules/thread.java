// Copyright © Corporation for National Research Initiatives
package org.python.modules;
import org.python.core.*;

class FunctionThread extends Thread
{
    PyObject func;
    PyObject[] args;
    PySystemState systemState;

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

public class thread implements InitModule
{
    public void initModule(PyObject dict) {
	dict.__setitem__("LockType", PyJavaClass.lookup(PyLock.class));
    }
	
    public static PyObject error = new PyString("thread.error");

    public static void start_new_thread(PyObject func, PyTuple args) {
        Thread pt = new FunctionThread(func, args.list);
        pt.start();
    }

    public static PyLock allocate_lock() {
        return new PyLock();
    }

    public static void exit() {
        exit_thread();
    }

    public static void exit_thread() {
        throw new PyException(Py.SystemExit, new PyInteger(0));
    }

    public static int get_ident() {
        return System.identityHashCode(Thread.currentThread());
    }
}
