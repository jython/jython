// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.python.types.Exposed;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * Holder for objects appearing in the closure of a function. There is
 * only a default constructor {@code PyCell()} because cells always
 * start life empty.
 */
public class PyCell implements Supplier<Object>, WithClass {

    /** The Python type {@code cell}. */
    public static final PyType TYPE = PyType.fromSpec( //
            new TypeSpec("cell", MethodHandles.lookup()));

    /** The object currently held. */
    Object obj;

    /** Handy constant where no cells are needed in a frame. */
    static final PyCell[] EMPTY_ARRAY = new PyCell[0];

    /** Create an empty cell. */
    PyCell() {}

    /**
     * Create a cell wuthe the given initial value.
     *
     * @param initial value
     */
    PyCell(Object initial) { this.obj = initial; }

    @Override
    public PyType getType() { return TYPE; }

    // Java API -------------------------------------------------------

    /**
     * Get an array of {@code PyCell}s, some empty and the rest initialised
     * from an existing array, in that order. Create a new array if the
     * overall length will not be zero.
     *
     * @param n number of cells in array to be empty
     * @param closure to append (or {@code null})
     * @return the array
     */
    static PyCell[] array(int n, PyCell[] closure) {
        // We will copy m elements to location n, overall length is L
        int m = closure == null ? 0 : closure.length, L = m + n;
        if (L <= 0) {
            return EMPTY_ARRAY;
        } else {
            PyCell[] a = new PyCell[L];
            for (int i = 0; i < n; i++) { a[i] = new PyCell(); }
            if (m > 0) { System.arraycopy(closure, 0, a, n, m); }
            return a;
        }
    }

    /** Get the contents. */
    @Exposed.Getter
    private Object cell_contents() {
        return PyUtil.errorIfNull(obj, () -> PyErr.format(PyExc.ValueError, "Cell is empty"));
    }

    /**
     * @param v to set as new contents.
     */
    @Exposed.Setter("cell_contents")
    void set(Object v) { obj = v; }

    /** Delete the contents. */
    @Exposed.Deleter("cell_contents")
    void del() { obj = null; }

    @Override
    public Object get() { return obj; }

    // Compare CPython cell_repr in cellobject.c
    @Override
    public String toString() {
        if (obj == null) {
            return String.format("<cell at %#x: empty>", Py.id(this));
        } else {
            return String.format("<cell at %#x: [%.100s]>", Py.id(this), obj);
            // Or as in CPython, but less informative:
            // return String.format(
            // "<cell at %#x: %.80s object at %#x>",
            // Py.id(this), PyType.of(obj).getName(), Py.id(obj));
        }
    }

    // Special methods ------------------------------------------------

    @SuppressWarnings("unused")
    private Object __repr__() { return toString(); }

    @SuppressWarnings("unused")
    private Object __str__() { return toString(); }
}
