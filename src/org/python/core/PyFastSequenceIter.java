/* Copyright (c) Jython Developers */
package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

/**
 * Sequence iterator specialized for accessing the underlying sequence directly.
 */
@ExposedType(name = "fastsequenceiterator", base = PyObject.class, isBaseType = false)
public class PyFastSequenceIter extends PyIterator {
    //note: Already implements Traverseproc, inheriting it from PyIterator

    public static final PyType TYPE = PyType.fromClass(PyFastSequenceIter.class);

    private PySequence seq;

    private int index;

    public PyFastSequenceIter(PySequence seq) {
        super(TYPE);
        this.seq = seq;
    }

    @ExposedMethod(doc = "x.next() -> the next value, or raise StopIteration")
    final PyObject fastsequenceiterator_next() {
        return super.next();
    }

    @Override
    public PyObject __iternext__() {
        if (seq == null) {
            return null;
        }

        PyObject result;
        try {
            result = seq.seq___finditem__(index++);
        } catch (PyException pye) {
            if (pye.match(Py.StopIteration)) {
                seq = null;
                return null;
            }
            throw pye;
        }

        if (result == null) {
            seq = null;
        }
        return result;
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retValue = super.traverse(visit, arg);
        if (retValue != 0) {
            return retValue;
        }
        return seq == null ? 0 : visit.visit(seq, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (ob == seq || super.refersDirectlyTo(ob));
    }
}
