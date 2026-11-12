// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

/**
 * Public static methods associated with the Python type {@code object}.
 * Instances of the Python {@code object} type are represented by
 * {@code java.lang.Object} and not by instances of this class.
 * <p>
 * The Java implementation class of a type defined in Python <i>will</i>
 * be derived from the canonical implementation class of the "solid
 * base" it inherits in Python. This may well be
 * {@code java.lang.Object}.
 */
// Compare CPython PyBaseObject_Type in typeobject.c
public final class PyObject {

    /** The type object {@code object}. */
    public static final PyType TYPE = TypeSystem.TYPE_type.getBase();

    /** One cannot make instances of {@code PyObject}. */
    private PyObject() {}
}
