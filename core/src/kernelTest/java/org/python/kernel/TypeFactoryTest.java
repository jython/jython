// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.python.core.PyType;

/**
 * Test that the Python type system, {@link TypeFactory} and instances
 * of a few associated classes, may be brought into operation in a
 * consistent state. The {@code TypeFactory} is normally a singleton,
 * created during the static initialisation of the type system, but for
 * the purpose of testing, we make and discard instances repeatedly.
 * <p>
 * The design keeps the number of <i>materially</i> different
 * initialisation paths to a minimum. In practice, we funnel all actions
 * that cause initialisation into essentially the same bootstrap
 * process, in the static initialisation of {@code TypeSystem}. It is
 * too difficult to reason about otherwise. In this test we subvert that
 * to test copies of the parts separately.
 * <p>
 * When testing, we arrange to run each test involving initialisation in
 * a new JVM. (See the {@code kernelTest} target in the build.)
 */
@DisplayName("In a new TypeFactory")
class TypeFactoryTest {

    /** A type exists for {@code object}. */
    @Test
    @DisplayName("'object' exists")
    @SuppressWarnings({"deprecation", "static-method"})
    void object_exists() {
        TypeFactory factory = new TypeFactory();
        PyType object = factory.createTypeForType().getBase();
        assertNotNull(object);
        assertInstanceOf(SimpleType.class, object);
        assertEquals("object", object.getName());
    }

    /** A type exists for {@code type}. */
    @Test
    @DisplayName("'type' exists")
    @SuppressWarnings({"deprecation", "static-method"})
    void type_exists() {
        TypeFactory factory = new TypeFactory();
        PyType type = factory.createTypeForType();
        assertNotNull(type);
        assertInstanceOf(SimpleType.class, type);
        assertEquals("type", type.getName());
    }

    /** No type object is public for {@code PyType.class}. */
    @Test
    @DisplayName("PyType.class is not registered")
    @SuppressWarnings({"deprecation", "unused", "static-method"})
    void type_type_unpublished() {
        TypeFactory factory = new TypeFactory();
        PyType ignored = factory.createTypeForType();
        TypeRegistry registry = factory.getRegistry();
        Representation rep = registry.lookup(PyType.class);
        assertNull(rep);
    }

    /** No type object is public for {@code Object.class}. */
    @Test
    @DisplayName("Object.class is not registered")
    @SuppressWarnings({"deprecation", "unused", "static-method"})
    void object_type_unpublished() {
        TypeFactory factory = new TypeFactory();
        PyType ignored = factory.createTypeForType();
        TypeRegistry registry = factory.getRegistry();
        Representation rep = registry.lookup(Object.class);
        assertNull(rep);
    }
}
