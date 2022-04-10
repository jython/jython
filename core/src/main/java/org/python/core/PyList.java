package org.python.core;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;

import org.python.core.PyType.Spec;

/** Stop-gap definition to satisfy references in the project. */
public class PyList extends ArrayList<Object> {
    private static final long serialVersionUID = 1L;

    public static PyType TYPE =
            PyType.fromSpec(new Spec("list", MethodHandles.lookup()));

    /** Construct an empty {@code list}. */
    PyList() {}

    /**
     * Construct an empty {@code list} with the specified initial
     * capacity.
     *
     * @param initialCapacity the initial capacity of the list
     */
    public PyList(int initialCapacity) { super(Math.max(0, initialCapacity)); }

    /**
     * Construct a {@code list} with initial contents.
     *
     * @param c initial contents
     */
    public PyList(Collection<?> c) { super(c); }

    /** Reverse this list in-place. */
    void reverse() {
        final int N = size(), M = N / 2;
        // We can accomplish the reversal in M swaps
        for (int i = 0, j = N; i < M; i++) {
            Object x = get(i);
            set(i, get(--j));
            set(j, x);
        }
    }

    int __len__() { return size(); }

    Object __getitem__(Object index) throws Throwable {
        return get(PyNumber.asSize(index, null));
    }

}
