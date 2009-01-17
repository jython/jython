package org.python.core;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Exposes a Python iter as a Java Iterator.
 */
public abstract class WrappedIterIterator<E> implements Iterator<E> {

    private final PyObject iter;

    private PyObject next;

    private boolean checkedForNext;

    public WrappedIterIterator(PyObject iter) {
        this.iter = iter;
    }

    public boolean hasNext() {
        if (!checkedForNext) {
            next = iter.__iternext__();
            checkedForNext = true;
        }
        return next != null;
    }

    /**
     * Subclasses must implement this to turn the type returned by the iter to the type expected by
     * Java.
     */
    public abstract E next();

    public PyObject getNext() {
        if (!hasNext()) {
            throw new NoSuchElementException("End of the line, bub");
        }
        PyObject toReturn = next;
        checkedForNext = false;
        next = null;
        return toReturn;
    }

    public void remove() {
        throw new UnsupportedOperationException("Can't remove from a Python iterator");
    }
}
