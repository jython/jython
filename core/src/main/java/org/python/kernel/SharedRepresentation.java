// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.kernel;

import org.python.core.PyType;
import org.python.support.InterpreterError;
import org.python.types.WithClass;

/**
 * The {@link Representation} for a Python class defined in Python. Many
 * Python classes may be represented by the same Java class, the actual
 * Python type being indicated by the instance.
 */
class SharedRepresentation extends Representation {

    /** To return as {@link #canonicalClass()}. */
    private final Class<?> canonicalClass;

    /**
     * Create a {@code Representation} object that is the class used to
     * represent instances of (potentially) many types defined in Python.
     *
     * @param javaClass Java representation class
     * @param canonical class on which subclasses are based
     */
    SharedRepresentation(Class<?> javaClass, Class<?> canonical) {
        super(javaClass);
        this.canonicalClass = canonical;
    }

    @Override
    public String toString() { return String.format("Shared[%s]", javaClass().getSimpleName()); }

    @Override
    public BaseType pythonType(Object x) {
        if (x instanceof WithClass wcx) {
            return BaseType.cast(wcx.getType());
        } else if (x == null) {
            return null;
        } else {
            throw notSharedError(x);
        }
    }

    @Override
    public boolean isIntExact() { return false; }

    @Override
    public boolean isFloatExact() { return false; }

    /**
     * The {@link PyType#canonicalClass()} of types that share this
     * representation (the "clique"). Subclasses in Python of those types
     * will (in general) not share this representation,
     *
     *
     * as it depends on whether {@code __dict__} is defined and on the
     * content of {@code __slots__}. However, they will all have the same
     * the canonical class, of which their Java representation class
     * {@link #javaClass()} is a proper subclass in Java. This design allows
     * us to re-use an existing representation, if that is possible, when
     * defining a subclass.
     *
     * @return the canonical Java representation class of types
     */
    public Class<?> canonicalClass() { return canonicalClass; }

    /**
     * Return an exception reporting that the given object was registered as
     * if implementing a {@link ReplaceableType}, but it cannot be inspected
     * for its type. The {@link TypeFactory} has a bug if it created this
     * {@code Representation}. Or the type system has a bug if it allowed
     * anything else to do so.
     *
     * @param x objectionable object
     * @return to throw
     */
    private InterpreterError notSharedError(Object x) {
        String msg = String.format("unsharable class %.100s registered as %s",
                x.getClass().getTypeName(), this.toString());
        return new InterpreterError(msg);
    }
}
