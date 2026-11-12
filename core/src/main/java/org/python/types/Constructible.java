// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.types;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * An interface that supports the implementation of {@code __new__}. The
 * implementing object must be a Python type object supplied by the run
 * time system.
 * <p>
 * A custom {@code __new__} method in the defining Java class of a type
 * generally has direct access to all the constructors it needs to
 * create instances of its own Python type (base constructors). However,
 * when asked for an instance of a different type, it must be able to
 * call the constructor of the Java class that represents instances of
 * that type.
 * <p>
 * Every Python type must therefore be able to supply a Java constructor
 * of its instances, and conversely its representation class must have a
 * constructor with a signature matching its Python base. Python classes
 * defined in Python are given a representation that satisfies this
 * criterion automatically.
 * <p>
 * This interface provides a way of looking up that matching
 * constructor, which we return as a {@code MethodHandle}.
 */
public interface Constructible {
    /**
     * The return from {@link Constructible#constructor(Class...)} holding a
     * reflective constructor definition and a {@code MethodHandle} by which
     * it may be called.
     */
    public static record ConstructorAndHandle(Constructor<?> constructor, MethodHandle handle) {}

    /**
     * Return the table holding constructors and their method handles for
     * instances of this type. This enables client code to iterate over
     * available constructors without any copying. The table and its
     * contents are immutable.
     * <p>
     * Note that in the key, the Java class of the return type is
     * {@code Object}.
     *
     * @return the lookup for constructors and handles
     */
    Map<MethodType, ConstructorAndHandle> constructorLookup();

    /**
     * Return a constructor of instances of this type, and its method
     * handle, that accepts arguments matching the given types. The Java
     * class of the return type of the handle is {@code Object}, since we
     * cannot rely on the caller to know the specific class.
     * <p>
     * The representation of a requested type (the {@code cls} argument to
     * {@code __new__}) will be a subclass in Java of the canonical
     * representation of the type for which {@code __new__} is being
     * defined. Invoking the specific constructor is necessary for the JVM
     * to allocate the correct class of object, and entails a call to a base
     * constructor.
     *
     * @param param the intended argument types
     * @return a constructor and a handle on it
     */
    // Compare CPython type slot tp_alloc (but only loosely).
    ConstructorAndHandle constructor(Class<?>... param);
}
