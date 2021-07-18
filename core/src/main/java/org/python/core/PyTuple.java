package org.python.core;

import java.lang.invoke.MethodHandles;

import org.python.core.PyType.Spec;

/**
 * This is a placeholder to satisfy references in implementations o preserved
 * from Jython 2.
 */
/** The Python {@code tuple} object. */
public class PyTuple implements CraftedPyObject {

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

    @Override
    public PyType getType() { return type; }
}
