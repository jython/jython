// Copyright 2000 Finn Bock

package org.python.core;

/**
 * An abstract helper class usefull when implementing an iterator object. This
 * implementation supply a correct __iter__() and a next() method based on the
 * __iternext__() implementation. The __iternext__() method must be supplied by
 * the subclass.
 * 
 * If the implementation raises a StopIteration exception, it should be stored
 * in stopException so the correct exception can be thrown to preserve the line
 * numbers in the traceback.
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
        if(ret == null) {
            if(stopException != null) {
                PyException toThrow = stopException;
                stopException = null;
                throw toThrow;
            }
            throw Py.StopIteration("");
        }
        return ret;
    }
    
    public abstract PyObject __iternext__();
    
    protected PyException stopException;
}    
