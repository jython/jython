// Copyright 2000 Finn Bock

package org.python.core;

/**
 * An abstract helper class usefull when implementing an iterator object.
 * This implementation supply a correct __iter__() and a next() method
 * based on the __iternext__() implementation.
 * The __iternext__() method must be supplied by the subclass.
 */
public abstract class PyIterator extends PyObject {
    public PyObject __iter__() {
        return this;
    }

    public static PyString __doc__next = new PyString(
        "x.next() -> the next value, or raise StopIteration"
    );

    public PyObject next() {
        PyObject ret = __iternext__();
        if (ret == null)
            throw Py.StopIteration("");
        return ret;
    }

    public abstract PyObject __iternext__();
}    
