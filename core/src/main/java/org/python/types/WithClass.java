// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.types;

import org.python.core.PyType;

/**
 * An instance of a class implementing {@code WithClass} reports an
 * explicit Python type, generally exposed as a {@code __class__}
 * attribute. Java classes that are the crafted representations of
 * Python types implement this interface.
 * <p>
 * The type may be the same for all instances of the same Java class or
 * be assignable within certain constraints (see
 * {@link WithClassAssignment}).
 */
public interface WithClass {
    /**
     * Return the actual Python type of the object.
     *
     * @return the actual type of the object
     */
    // @Exposed.Get(name="__class__")
    PyType getType();
}
