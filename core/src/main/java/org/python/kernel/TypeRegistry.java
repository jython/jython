// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.kernel;

import java.util.Map;
import java.util.WeakHashMap;

import org.python.core.PyType;
import org.python.support.InterpreterError;
import org.python.types.TypeSpec;

/**
 * Mapping from Java class to the {@link Representation} that provides
 * instances of the class with Python semantics. We refer to this as a
 * "type registry" because the {@code Representation} retrieved contains
 * some type information and leads directly to more. Also
 * "representation registry" is just too much of a tongue twister.
 * <p>
 * In normal operation (outside test cases) there is only one instance
 * of this class, owned by a {@link TypeFactory}, in turn created by the
 * type system. Note that only the owning factory has a write interface
 * to the registry.
 */
public abstract class TypeRegistry extends ClassValue<Representation> {

    /**
     * The mapping from Java class to {@link Representation} that backs this
     * {@link TypeRegistry}, using entries published by the owning
     * {@link TypeFactory}.
     * <p>
     * The keys are weak to allow classes to be unloaded. (Concurrent GC is
     * not a threat to the consistency of the registry since a class we are
     * looking up cannot be unloaded.)
     * <p>
     * This map is protected from concurrent modification by synchronising
     * on the containing {@code Registry}, but that lock must not be held by
     * a thread waiting for the factory to avoid a deadlock when the factory
     * posts its answer.
     */
    protected final Map<Class<?>, Representation> map = new WeakHashMap<>();

    /**
     * Find a {@code Representation} to describe the given class in relation
     * to the Python type system. There are these broad cases. {@code c}
     * might be:
     * <ol>
     * <li>a crafted implementation of a Python type</li>
     * <li>an adopted implementation of some Python type</li>
     * <li>the Java representation class of multiple Python classes of
     * mutually replaceable {@code __class__}</li>
     * <li>a found Java type</li>
     * <li>the crafted base of Python sub-classes of a found Java type</li>
     * </ol>
     * Cases 1, 3 and 5 may be recognised by marker interfaces on {@code c}.
     * Case 2 may only be distinguished from case 4 only because classes
     * that are adopted representations will have been registered as such,
     * when their type was defined, before the question is first posed.
     */
    @Override
    protected Representation computeValue(Class<?> c) {

        /*
         * Representation.registry.get() (which is ClassValue.get()) contained
         * no cached mapping for c. We will either find an answer ready in the
         * public map, or try to construct one and post it to the public map.
         */
        Representation rep;

        if ((rep = lookup(c)) == null) {
            // We did not find c published, so we ask the type factory.
            rep = findOrCreate(c);
        }

        /*
         * It is possible that other threads are racing this one to define c.
         * This is not a problem: they all have the same answer, and one of them
         * will get to cache it.
         */
        return rep;
    }

    /**
     * Look up a class in the published registry map (only), and return the
     * {@code Representation} for it, or {@code null} if it was not found.
     *
     * @param c class to look up
     * @return representation object for {@code c} or {@code null}.
     */
    synchronized final Representation lookup(Class<?> c) { return map.get(c); }

    /**
     * Find (do not create) this class in the published registry map, or in
     * work-in-progress in the factory, and return the
     * {@code Representation} for it or {@code null} if it was not found.
     * <p>
     * This method is overridden by the registry implementation the
     * {@link TypeFactory} provides.
     *
     * @implNote This method should not be synchronised on the registry
     *     because it is likely to block waiting to use the factory.
     *
     * @param c class to resolve
     * @return representation object for {@code c} or {@code null}.
     */
    abstract Representation find(Class<?> c);

    /**
     * Find the given class in the published registry map, or in
     * work-in-progress in the factory, or create a type and return the
     * {@code Representation}. The {@link TypeFactory} creates a registry
     * that implements this method.
     *
     * @implNote This method must take the lock on the factory <b>before</b>
     *     it locks the registry because it is likely to wait for the
     *     factory.
     *
     * @param c class to represent
     * @return representation object for {@code c}.
     */
    abstract Representation findOrCreate(Class<?> c);

    /**
     * Ensure a class is statically initialised. Static initialisation of a
     * class that defines a Python type will normally create a
     * {@link PyType} and its representations through a call to
     * {@link PyType#fromSpec(TypeSpec)}, although a found Java class will
     * clearly need explicit actions in the caller.
     *
     * @param c to initialise
     */
    static void ensureInit(Class<?> c) {
        if (!c.isPrimitive()) {
            String name = c.getName();
            try {
                Class.forName(name, true, c.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new InterpreterError(e, "failed to initialise class %s", name);
            }
        }
    }

}
