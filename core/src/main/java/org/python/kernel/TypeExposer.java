// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.kernel;

import java.lang.invoke.MethodHandles.Lookup;

/**
 * Interface expected of the type exposer, and used by the
 * {@link TypeFactory}.
 */
/*
 * We define this (in the kernel) to make it possible for the runtime
 * package to contain the implementation of the type exposer, while the
 * TypeFactory, safely locked away in the kernel, can request and use
 * instances. When we create a type factory, we provide it with a
 * factory object for TypeExposer instances.
 */
public interface TypeExposer {
    /**
     * Gather methods (including getters and setters of fields) from the
     * specified class. Definitions (a precursor of Python descriptors)
     * accumulate in the exposer implementation.
     *
     * @param methodClass to scan for definitions
     */
    void scanJavaMethods(Class<?> methodClass);

    /**
     * Gather members (fields exposed as Python attributes) from the
     * specified class. Definitions (a precursor of Python descriptors)
     * accumulate in the exposer.
     *
     * @param fieldClass to scan for definitions
     */
    void scanJavaFields(Class<?> fieldClass);

    /**
     * A name-value pair that holds one entry intended for the dictionary of
     * the type.
     */
    static record Entry(String name, Object value) {}

    /**
     * Get the definitions created in this {@code TypeExposer} as name and
     * value pairs. The client for this action is the type object itself,
     * and the values are attributes to be entered in its dictionary. These
     * are constructed as the iterator runs.
     * <p>
     * Attributes may rely on a {@code MethodHandle} or {@code VarHandle},
     * so a lookup object must be provided that can create them.
     *
     * @param lookup authorisation to access members
     * @return sequence of definitions found in the type
     */
    Iterable<Entry> entries(Lookup lookup);
}
