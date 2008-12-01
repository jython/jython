// Copyright (c) Corporation for National Research Initiatives
package org.python.modules.thread;

import org.python.core.ClassDictInit;
import org.python.core.FunctionThread;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTableCode;
import org.python.core.PyType;
import org.python.core.PyTuple;

public class thread implements ClassDictInit {

    private static volatile long stack_size = 0; // XXX - can we figure out the current stack size?
    private static ThreadGroup group = new ThreadGroup("jython-threads");


    public static PyString __doc__ = new PyString(
        "This module provides primitive operations to write multi-threaded "+
                "programs.\n" +
        "The 'threading' module provides a more convenient interface."
    );

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("LockType", PyType.fromClass(PyLock.class));
        dict.__setitem__("_local", PyLocal.TYPE);
        dict.__setitem__("interruptAllThreads", null);
    }

    public static PyObject error = new PyString("thread.error");

    public static void start_new_thread(PyObject func, PyTuple args) {
        Thread pt = _newFunctionThread(func, args);
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

    /**
     * Initializes a {@link FunctionThread}, using the configured stack_size and
     * registering the thread in the @link {@link #group} of threads spawned by
     * the thread module.
     *
     * Also used from the threading.py module.
     */
    public static FunctionThread _newFunctionThread(PyObject func, PyTuple args) {
        return new FunctionThread(func, args.getArray(), stack_size, group);
    }

    /**
     * Interrupts all running threads spawned by the thread module.
     *
     * This works in conjuntion with:<ul>
     * <li>{@link PyTableCode#call(org.python.core.PyFrame, PyObject)}:
     * checks for the interrupted status of the current thread and raise
     * a SystemRestart exception if a interruption is detected.</li>
     * <li>{@link FunctionThread#run()}: exits the current thread when a
     * SystemRestart exception is not caught.</li>
     *
     * Thus, it is possible that this doesn't make all running threads to stop,
     * if SystemRestart exception is caught.
     */
    public static void interruptAllThreads() {
        group.interrupt();
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
    
    
    public static long stack_size(PyObject[] args) {
        switch (args.length) {
            case 0:
                return stack_size;
            case 1:
                long old_stack_size = stack_size;
                int proposed_stack_size = args[0].asInt();
                if (proposed_stack_size != 0 && proposed_stack_size < 32768) {
                    // as specified by Python, Java quietly ignores what
                    // it considers are too small
                    throw Py.ValueError("size not valid: " + proposed_stack_size + " bytes");
                }
                stack_size = proposed_stack_size;
                return old_stack_size;
            default:
                throw Py.TypeError("stack_size() takes at most 1 argument (" + args.length + "given)");
        }
    }
}
