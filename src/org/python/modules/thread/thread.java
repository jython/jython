// Copyright (c) Corporation for National Research Initiatives
package org.python.modules.thread;

import org.python.core.ClassDictInit;
import org.python.core.FunctionThread;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.core.PyTuple;

public class thread implements ClassDictInit {

    public static PyString __doc__ = new PyString(
        "This module provides primitive operations to write multi-threaded "+
                "programs.\n" +
        "The 'threading' module provides a more convenient interface."
    );

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("LockType", PyType.fromClass(PyLock.class));
        dict.__setitem__("_local", PyLocal.TYPE);
    }

    public static PyObject error = new PyString("thread.error");

    public static void start_new_thread(PyObject func, PyTuple args) {
        Thread pt = new FunctionThread(func, args.getArray());
        PyObject currentThread = func.__findattr__("im_self");
        if (currentThread != null) {
            PyObject isDaemon = currentThread.__findattr__("isDaemon");
            if (isDaemon != null && isDaemon.isCallable()) {
                PyObject po = isDaemon.__call__();
                pt.setDaemon(po.__nonzero__());
            }
            PyObject getName = currentThread.__findattr__("getName");
            if (getName != null && getName.isCallable()) {
                PyObject pname = getName.__call__();
                pt.setName(String.valueOf(pname));
            }
        }
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
