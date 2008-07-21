/* Copyright (c) Jython Developers */
package org.python.core;

/**
 * Sequence iterator specialized for accessing the underlying sequence directly.
 */
public class PyFastSequenceIter extends PyIterator {

    private PySequence seq;

    private int index = 0;

    public PyFastSequenceIter(PySequence seq) {
        this.seq = seq;
    }

    public PyObject __iternext__() {
        if (seq == null) {
            return null;
        }

        PyObject result;
        try {
            result = seq.seq___finditem__(index++);
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
