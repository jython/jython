// Copyright (c) Corporation for National Research Initiatives
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

public class thread implements ClassDictInit
{
    public static PyString __doc__ = new PyString(
        "This module provides primitive operations to write multi-threaded "+
                "programs.\n" +
        "The 'threading' module provides a more convenient interface."
    );

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("LockType", PyType.fromClass(PyLock.class));
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

    public static long get_ident() {
        return Py.java_obj_id(Thread.currentThread());
    }
}
