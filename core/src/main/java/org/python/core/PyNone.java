// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

import org.python.internal.Singleton;
import org.python.types.TypeSpec;

/** The Python {@code None} object. */
public final class PyNone extends Singleton {

    /** Only referenced during bootstrap by {@link TypeSystem}. */
    static class Spec {
        /** @return the type specification. */
        static TypeSpec get() {
            return new TypeSystem.BootstrapSpec("NoneType", MethodHandles.lookup(), PyNone.class);
        }
    }

    /** The Python type of {@code None}. */
    public static final PyType TYPE = TypeSystem.TYPE_NoneType;

    /** The only instance, published as {@link Py#None}. */
    public static final PyNone INSTANCE = new PyNone();

    private PyNone() { super(TYPE, "None"); }

    // Special methods -----------------------------------------------

    @SuppressWarnings({"static-method", "unused"})
    private boolean __bool__() { return false; }
}
