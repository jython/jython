// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.internal;

import org.python.core.PyType;
import org.python.types.WithClass;

/**
 * A base class for Python singletons {@code None},
 * {@code NotImplemented}, {@code ...}.
 */
public abstract class Singleton implements WithClass {

    /** The Python type of the object implemented. */
    private final PyType type;

    /**
     * The name in Python of the object implemented as returned by
     * {@code repr()}.
     */
    private final String name;

    /**
     * Specify the Python type and repr-name.
     *
     * @param type of the single instance
     * @param name that {@code repr()} should produce.
     */
    protected Singleton(PyType type, String name) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() { return name; }

    @Override
    public PyType getType() { return type; }

    // Special methods -----------------------------------------------

    /*
     * Special methods are public, so as to be visible from runtime package
     * (not API).
     */

    /**
     * {@code __repr__}
     *
     * @return {@code repr(this)} as defined in constructor.
     */
    public Object __repr__() { return name; }
}
