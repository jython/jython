// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.kernel;

import java.util.List;

import org.python.types.TypeFlag;

/**
 * A Python type object used where multiple Python types share a single
 * representation in Java, making them all acceptable for assignment to
 * the {@code __class__} member of Python instances of any of them. The
 * common representation encapsulates what in Python terms the "layout
 * constraints". The Java implementation of Python instance methods in
 * such a type will have the common Java type (or a superclass) as their
 * {@code self} parameter.
 */
public non-sealed class ReplaceableType extends BaseType {

    /** The representation shared by this type and others. */
    final SharedRepresentation representation;

    /**
     * Construct one of several types that share a single representation in
     * Java.
     *
     * @param name of the type (fully qualified).
     * @param representation shared
     * @param bases of the new type
     */
    public ReplaceableType(String name, SharedRepresentation representation, BaseType[] bases) {
        super(name, representation.javaClass(), bases);
        this.representation = representation;
    }

    @Override
    public List<Representation> representations() { return List.of(representation); }

    @Override
    public List<Class<?>> selfClasses() { return List.of(javaClass); }

    @Override
    public Class<?> canonicalClass() { return representation.canonicalClass(); }

    @Override
    public BaseType pythonType(Object x) {
        // I don't *think* we should be asked this question unless:
        assert javaClass.isAssignableFrom(x.getClass());
        return this;
    }

    @Override
    public boolean isMutable() { return !features.contains(TypeFlag.IMMUTABLE); }

    @Override
    public boolean isIntExact() { return false; }

    @Override
    public boolean isFloatExact() { return false; }
}
