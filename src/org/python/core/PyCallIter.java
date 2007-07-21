package org.python.core;

public class PyCallIter extends PyIterator {
    private PyObject callable;
    private PyObject sentinel;

    public PyCallIter(PyObject callable, PyObject sentinel) {
        if(!__builtin__.callable(callable)) {
            throw Py.TypeError("iter(v, w): v must be callable");
        }
        this.callable = callable;
        this.sentinel = sentinel;
    }

    public PyObject __iternext__() {
        PyObject val = null;
        try {
            val = callable.__call__();
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.StopIteration)){
                stopException = exc;
                return null;
            }
            throw exc;
        }
        if (val._eq(sentinel).__nonzero__())
            return null;
        return val;
    }

}

