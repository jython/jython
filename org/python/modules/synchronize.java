// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;
import org.python.core.*;

class SynchronizedCallable extends PyObject
{
    PyObject callable;

    public SynchronizedCallable(PyObject callable) {
        this.callable = callable;
    }

    public PyObject _doget(PyObject container) {
        // TBD: third arg == null?  Hmm...
        return new PyMethod(container, this, null);
    }

    public PyObject __call__() {
        throw Py.TypeError("synchronized callable called with 0 args");
    }

    public PyObject __call__(PyObject arg) {
        synchronized(synchronize._getSync(arg)) {
            return callable.__call__(arg);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2) {
        synchronized(synchronize._getSync(arg1)) {
            return callable.__call__(arg1, arg2);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        synchronized(synchronize._getSync(arg1)) {
            return callable.__call__(arg1, arg2, arg3);
        }
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        if (args.length == 0) {
            throw Py.TypeError("synchronized callable called with 0 args");
        }
        synchronized(synchronize._getSync(args[0])) {
            return callable.__call__(args, keywords);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject[] args,
                             String[] keywords)
    {
        synchronized(synchronize._getSync(arg1)) {
            return callable.__call__(arg1, args, keywords);
        }
    }


}

public class synchronize
{
    public static Object _getSync(PyObject obj) {
        return Py.tojava(obj, Object.class);
    }

    public static PyObject apply_synchronized(PyObject sync_object,
                                              PyObject callable,
                                              PyObject args)
    {
        synchronized (_getSync(sync_object)) {
            return __builtin__.apply(callable, args);
        }
    }
    public static PyObject apply_synchronized(PyObject sync_object,
                                              PyObject callable,
                                              PyObject args,
                                              PyDictionary kws)
    {
        synchronized (_getSync(sync_object)) {
            return __builtin__.apply(callable, args, kws);
        }
    }

    public static PyObject make_synchronized(PyObject callable) {
        return new SynchronizedCallable(callable);
    }
}
