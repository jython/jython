package org.python.core;

public class PySequenceIter extends PyObject {
    private PyObject seq;
    private int idx;

    public PySequenceIter(PyObject seq) {
        this.seq = seq;
        this.idx = 0;
    }

    public PyObject __iter__() {
        return this;
    }

    public PyObject __iternext__() {
        return seq.__finditem__(idx++);
    }

    public PyObject next() {
        return __iternext__();
    }

    // __class__ boilerplate -- see PyObject for details
    public static PyClass __class__;
    protected PyClass getPyClass() { return __class__; }
}

