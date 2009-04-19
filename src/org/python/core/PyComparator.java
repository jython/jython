package org.python.core;

import java.util.Comparator;

public class PyComparator implements Comparator<PyObject> {

    protected PyList list;
    protected PyObject cmp;
    protected PyObject key;
    protected boolean reverse = false;

    PyComparator(PyList list, PyObject cmp, PyObject key, boolean reverse) {
        this.list = list;
        this.cmp = cmp;
        this.key = key;
        this.reverse = reverse;
    }

    // First cut at an implementation.  FIXME: In CPython key is supposed to
    // make things fast, accessing each element once.  For this first cut I am
    // cheating and calling the key function on every pass to get something
    // that works right away.
    public int compare(PyObject o1, PyObject o2) {
        int result;
        if (key != null && key != Py.None) {
            o1 = key.__call__(o1);
            o2 = key.__call__(o2);
        }
        if (cmp != null && cmp != Py.None) {
            result = cmp.__call__(o1, o2).asInt();
        } else {
            result = o1._cmp(o2);
        }
        if (reverse) {
            return -result;
        }
        if (this.list.gListAllocatedStatus >= 0) {
            throw Py.ValueError("list modified during sort");
        }
        return result;
    }

    public boolean equals(Object o) {
        if(o == this) {
            return true;
        }

        if (o instanceof PyComparator) {
            return key.equals(((PyComparator)o).key) && cmp.equals(((PyComparator)o).cmp);
        }
        return false;
    }
}
