package org.python.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public abstract class PySequenceList extends PySequence {

    public PySequenceList() {
    }

    protected PySequenceList(PyType type) {
        super(type);
    }

    public abstract void add(int index, Object element);

    public abstract boolean add(Object o);

    public abstract boolean addAll(int index, Collection c);

    public abstract boolean addAll(Collection c);

    public abstract void clear();

    public abstract boolean contains(Object o);

    public abstract boolean containsAll(Collection c);

    public abstract boolean equals(Object o);

    public abstract Object get(int index);

    /**
     * Get the backing array. The array should not be modified. To get a copy of the array, see
     * {@link #toArray()}.
     */
    public abstract PyObject[] getArray();

    public abstract int hashCode();

    public abstract int indexOf(Object o);

    public abstract boolean isEmpty();

    public abstract Iterator iterator();

    public abstract int lastIndexOf(Object o);

    public abstract ListIterator listIterator();

    public abstract ListIterator listIterator(int index);

    public abstract void pyadd(int index, PyObject element);

    public abstract boolean pyadd(PyObject o);

    public abstract PyObject pyget(int index);

    public abstract void pyset(int index, PyObject element);

    public abstract Object remove(int index);

    public abstract void remove(int start, int stop);

    public abstract boolean remove(Object o);

    public abstract boolean removeAll(Collection c);

    public abstract boolean retainAll(Collection c);

    public abstract Object set(int index, Object element);

    public abstract int size();

    public abstract List subList(int fromIndex, int toIndex);

    public abstract Object[] toArray();

    public abstract Object[] toArray(Object[] a);

    public abstract String toString();

}
