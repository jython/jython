package org.python.core;

/** Straightens the call path for some common cases
 */

// XXX - add support for generators in conjunction w/ contextlib.contextmanager

public class ContextGuard implements ContextManager {

    private final PyObject __enter__method;
    private final PyObject __exit__method;

    private ContextGuard(PyObject manager) {
        __enter__method = manager.__getattr__("__enter__");
        __exit__method = manager.__getattr__("__exit__");
    }

    public PyObject __enter__(ThreadState ts) {
        return __enter__method.__call__(ts);
    }

    public boolean __exit__(ThreadState ts, PyObject type, PyObject value, PyObject traceback) {
        return __exit__method.__call__(ts, type, value, traceback).__nonzero__();
    }

    public static ContextManager getManager(PyObject manager) {
        if (manager instanceof ContextManager) {
            return (ContextManager) manager;
        } else {
            return new ContextGuard(manager);
        }
    }
}
