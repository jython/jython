package org.python.core;

import java.util.Iterator;
import java.util.Set;

public class PySetIterator extends PyObject {

    private Set set;

    private int size;

    private Iterator<PyObject> iterator;

    public PySetIterator(Set set) {
        super();
        this.set = set;
        size = set.size();
        iterator = set.iterator();
    }

    public PyObject __iter__() {
        return this;
    }

    /**
     * Returns the next item in the iteration or raises a StopIteration.
     *
     * @return the next item in the iteration
     */
    public PyObject next() {
        PyObject o = this.__iternext__();
        if (o == null) {
            throw Py.StopIteration("");
        }
        return o;
    }

    /**
     * Returns the next item in the iteration.
     *
     * @return the next item in the iteration or null to signal the end of the iteration
     */
    public PyObject __iternext__() {
        if (set.size() != size) {
            throw Py.RuntimeError("set changed size during iteration");
        }
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }
}
