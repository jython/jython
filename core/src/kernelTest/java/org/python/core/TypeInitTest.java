// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.python.kernel.AdoptiveType;
import org.python.kernel.ReplaceableType;
import org.python.kernel.Representation;
import org.python.kernel.SimpleType;
import org.python.kernel.TypeRegistry;

/**
 * Test that the Python type system comes into existence in a consistent
 * state, in response to use. We use subclasses of this test, each to be
 * run in a fresh JVM, to make the same set of tests after the type
 * system is woken in the several materially different ways possible:
 * <ol>
 * <li>A call to {@link TypeRegistry#get(Class)}, such as occurs during
 * a Python operation (or type enquiry) on any object at all.</li>
 * <li>Initialising the class of a Python type defined in Java that
 * entails a call to {@link PyType#fromSpec(org.python.runtime.TypeSpec)
 * PyType.fromSpec()}. Initialisation precedes any call or reference to
 * a member (such as a static {@code TYPE} member).</li>
 * <li>As a variant of 2, any reference to the type objects for
 * {@code type} or {@code object}, which have to be created without
 * using {@code PyType.fromSpec()}.</li>
 * </ol>
 * The design keeps the number of <i>materially</i> different ways to a
 * minimum, by funnelling all causes into essentially the same bootstrap
 * process, rooted in the static initialisation of {@link TypeSystem}.
 * <p>
 * When testing, we arrange to run each subclass of this test in a new
 * JVM. (See the {@code kernelTest} target in the build.)
 */
@DisplayName("Without any preparation")
@SuppressWarnings("static-method")
class TypeInitTest {

    /**
     * The action that triggers the type system. Override this to choose an
     * event that starts the system. It must be annotated in the subclass
     * with {@code @BeforeAll}.
     */
    @BeforeAll
    static void setUpClass() {}

    /** After setUp() a type exists for {@code object}. */
    @Test
    @DisplayName("PyObject.TYPE exists")
    void object_exists() {
        PyType object = PyObject.TYPE;
        assertNotNull(object);
        assertInstanceOf(SimpleType.class, object);
        assertEquals("object", object.getName());
    }

    /** After setUp() we can look up {@code Object.class}. */
    @Test
    @DisplayName("we can look up a type for Object.class")
    void lookup_object_type() {
        TypeRegistry registry = TypeSystem.registry;
        Representation rep = registry.get(Object.class);
        assertInstanceOf(SimpleType.class, rep);
        PyType type = rep.pythonType(new Object());
        assertSame(type, rep);
        assertEquals("object", type.getName());
    }

    /** After setUp() a type exists for {@code type}. */
    @Test
    @DisplayName("PyType.TYPE() returns the type object")
    void type_exists() {
        PyType type = PyType.TYPE();
        assertNotNull(type);
        assertInstanceOf(SimpleType.class, type);
        assertEquals("type", type.getName());
    }

    /** After setUp() we can look up {@code PyType.class}. */
    @Test
    @DisplayName("we can look up a type for PyType.class")
    void lookup_type_type() {
        TypeRegistry registry = TypeSystem.registry;
        Representation rep = registry.get(PyType.class);
        assertNotNull(rep);
        assertInstanceOf(SimpleType.class, rep);
        PyType type = rep.pythonType(rep);
        assertSame(rep, type);
        assertEquals("type", type.getName());
    }

    /** After setUp() all type implementations have the same type. */
    @Test
    @DisplayName("Subclasses of PyType share a type object")
    void type_subclasses_share_type() {
        // Lookup for the base type
        TypeRegistry registry = TypeSystem.registry;
        Representation repType = registry.get(PyType.class);
        assertSame(PyType.TYPE(), repType);
        // Lookup each Java subclass has the same type
        for (Class<? extends PyType> c : List.of(SimpleType.class, AdoptiveType.class,
                ReplaceableType.class)) {
            Representation rep = registry.get(c);
            assertSame(repType, rep);
        }
    }

    /**
     * After setUp() we can look up {@code Double.class} and get
     * {@code float}. The potential for failure consists in treating
     * {@code Double} as a found Java type before the Python type
     * {@code float} can adopt it.
     */
    @Test
    @DisplayName("we can look up a type for Double.class")
    void lookup_type_double() {
        TypeRegistry registry = TypeSystem.registry;
        Representation rep = registry.get(Double.class);
        assertNotNull(rep);
        PyType type = rep.pythonType(1.0);
        assertInstanceOf(AdoptiveType.class, type);
        assertNotSame(rep, type);
        assertEquals("float", type.getName());
        assertSame(PyFloat.TYPE, type);
    }
}
