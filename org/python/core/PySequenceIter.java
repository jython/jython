package org.python.core;

public class PySequenceIter extends PyIterator {
    private PyObject seq;
    private int idx;

    public PySequenceIter(PyObject seq) {
        this.seq = seq;
        this.idx = 0;
    }

    public PyObject __iternext__() {
        try {
            return seq.__finditem__(idx++);
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.StopIteration))
                return null;
            throw exc;
        }
    }

    // __class__ boilerplate -- see PyObject for details
    public static PyClass __class__;
    protected PyClass getPyClass() { return __class__; }
}

