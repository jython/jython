package org.python.core;

public class PySequenceIter extends PyIterator {
    private PyObject seq;
    private int idx;

    public PySequenceIter(PyObject seq) {
        this.seq = seq;
        this.idx = 0;
    }

    public PyObject __iternext__() {
        if (seq == null) {
            return null;
        }

        PyObject result;
        try {
            result = seq.__finditem__(idx++);
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.StopIteration)) {
                seq = null;
                return null;
            }
            throw exc;
        }
        if (result == null) {
            seq = null;
        }
        return result;
    }
}

