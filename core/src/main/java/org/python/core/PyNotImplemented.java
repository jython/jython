// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

import org.python.internal.Singleton;
import org.python.types.TypeSpec;

/** The Python {@code NotImplemented} object. */
public final class PyNotImplemented extends Singleton {

    /** The Python type of {@code NotImplemented}. */
    public static final PyType TYPE = PyType.fromSpec( //
            new TypeSpec("NotImplementedType", MethodHandles.lookup()));

    /** The only instance, published as {@link Py#NotImplemented}. */
    static final PyNotImplemented INSTANCE = new PyNotImplemented();

    private PyNotImplemented() { super(TYPE, "NotImplemented"); }

    // Special methods -----------------------------------------------

    @SuppressWarnings({"static-method", "unused"})
    private boolean __bool__() {
        Warnings.format(PyExc.DeprecationWarning, 1, BOOLEAN_CONTEXT);
        return true;
    }

    private static final String BOOLEAN_CONTEXT =
            "NotImplemented should not be used in a boolean context";
}
