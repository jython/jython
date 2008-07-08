package org.python.core;

public class PyCallIter extends PyIterator {

    private PyObject callable;

    private PyObject sentinel;

    public PyCallIter(PyObject callable, PyObject sentinel) {
        if (!callable.isCallable()) {
            throw Py.TypeError("iter(v, w): v must be callable");
        }
        this.callable = callable;
        this.sentinel = sentinel;
    }

    public PyObject __iternext__() {
        if (callable == null) {
            return null;
        }

        PyObject result;
        try {
            result = callable.__call__();
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.StopIteration)) {
                callable = null;
                stopException = exc;
                return null;
            }
            throw exc;
        }
        if (result._eq(sentinel).__nonzero__()) {
            callable = null;
            return null;
        }
        return result;
    }
}
