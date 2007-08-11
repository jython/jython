package org.python.core;

/**
 * An iterator that yields the objects from a sequence-like object in reverse
 * order. 
 */
public class PyReversedIterator extends PyIterator {

    /**
     * Creates an iterator that first yields the item at __len__ - 1 on seq and
     * returns the objects in descending order from there down to 0.
     * 
     * @param seq -
     *            an object that supports __getitem__ and __len__
     */
    public PyReversedIterator(PyObject seq) {
        this.seq = seq;
        idx = seq.__len__();
        if(idx > 0) {
            idx = idx - 1;
        }
    }

    public PyObject __iternext__() {
        if(idx >= 0) {
            return seq.__finditem__(idx--);
        }
        return null;
    }

    private PyObject seq;

    private int idx;
}
