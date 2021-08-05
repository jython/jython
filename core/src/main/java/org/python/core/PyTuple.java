// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.util.AbstractList;

import org.python.core.PyType.Spec;

/**
 * This is a minimal implementation to satisfy references elsewhere
 * in the code.
 */
/** The Python {@code tuple} object. */
public class PyTuple extends AbstractList<Object> implements CraftedPyObject {

    /** The Python type object for {@code tuple}. */
    static final PyType TYPE = PyType.fromSpec( //
            new Spec("tuple", MethodHandles.lookup()));

    /** The Python type of this instance. */
    protected final PyType type;

    /** The elements of the {@code tuple}. */
    final Object[] value;

    public <E> PyTuple(PyType type, E[] value) {
        this.type = type;
        // We make a new array .
        int n = value.length;
        this.value = new Object[n];
        // The copy may throw ArrayStoreException.
        System.arraycopy(value, 0, this.value, 0, n);
    }

    @SafeVarargs
    public <E> PyTuple(E... value) { this(TYPE, value); }

    // AbstractList methods ------------------------------------------

    @Override
    public Object get(int i) { return value[i]; }

    @Override
    public int size() { return value.length; }

    @Override
    public PyType getType() { return type; }
}
