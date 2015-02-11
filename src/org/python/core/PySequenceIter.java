/* Copyright (c) Jython Developers */
package org.python.core;

/**
 * General sequence iterator.
 */
public class PySequenceIter extends PyIterator {

    private PyObject seq;

    private int index = 0;

    public PySequenceIter(PyObject seq) {
        this.seq = seq;
    }

    public PyObject __iternext__() {
        if (seq == null) {
            return null;
        }

        PyObject result;
        try {
            result = seq.__finditem__(index++);
        } catch (PyException exc) {
            if (exc.match(Py.StopIteration)) {
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


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal = super.traverse(visit, arg);
        if (retVal != 0) {
            return retVal;
        }
        return seq == null ? 0 : visit.visit(seq, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (ob == seq || super.refersDirectlyTo(ob));
    }
}
