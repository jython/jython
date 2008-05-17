package org.python.core;

import java.util.Iterator;
import java.util.Set;
import java.util.ConcurrentModificationException;

public class PySetIterator extends PyObject {
    private Set _set;
    private int _count;
    private Iterator _iterator;

    public PySetIterator(Set set) {
        super();
        this._set = set;
        this._count = 0;
        this._iterator = set.iterator();
    }

    public PyObject __iter__() {
        return this;
    }

    /**
     * Returns the next item in the iteration or raises a StopIteration.
     * <p/>
     * <p/>
     * This differs from the core Jython Set iterator in that it checks if
     * the underlying Set changes in size during the course and upon completion
     * of the iteration.  A RuntimeError is raised if the Set ever changes size
     * or is concurrently modified.
     * </p>
     *
     * @return the next item in the iteration
     */
    public PyObject next() {
        PyObject o = this.__iternext__();
        if (o == null) {
            if (this._count != this._set.size()) {
                // CPython throws an exception even if you have iterated through the
                // entire set, this is not true for Java, so check by hand
                throw Py.RuntimeError("dictionary changed size during iteration");
            }
            throw Py.StopIteration("");
        }
        return o;
    }

    /**
     * Returns the next item in the iteration.
     *
     * @return the next item in the iteration
     *         or null to signal the end of the iteration
     */
    public PyObject __iternext__() {
        if (this._iterator.hasNext()) {
            this._count++;
            try {
                return Py.java2py(this._iterator.next());
            } catch (ConcurrentModificationException e) {
                throw Py.RuntimeError("dictionary changed size during iteration");
            }
        }
        return null;
    }
}
