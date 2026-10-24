// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * When a Python class defined in Java names another as a member, and
 * neither type has yet been created for Python, reentrant calls are
 * made to {@link PyType#fromSpec(TypeSpec)}. This test exercises that
 * process (in the {@code @BeforeAll} set up), and in its cases verifies
 * invariants we claim for the {@code TypeFactory}.
 * <p>
 * This test is inadequate as it stands to drive the type system into
 * reentrant behaviour as we'd like.
 */
@DisplayName("On Java-defining a Python class referring to another")
class TypeInitTestReentrant extends TypeInitTest {

    /** Result of trigger action (if any). */
    protected static MyOther object;

    /** Start by using a complex Python type defined in Java. */
    @BeforeAll
    static void setUpClass() { object = new MyOther(); }

    /** A simple type defined in Java. */
    static class MyClass implements WithClass {
        /** Python type of MyClass. */
        static final PyType TYPE = PyType.fromSpec(new TypeSpec("MyClass", MethodHandles.lookup()));

        @Override
        public PyType getType() { return TYPE; }
    }

    // XXX Add some specific tests.

    /**
     * A type defined in Java that references another type.
     * <p>
     * This doesn't currently cause any reentrant behaviour. What would? How
     * about an exposed method taking or returning a {@code MyClass}?
     */
    static class MyOther implements WithClass {
        /** Static init of this class will require MyClass to init. */
        static MyClass friend = new MyClass();
        /** Python type of MyOther. */
        static final PyType TYPE = PyType.fromSpec(new TypeSpec("MyOther", MethodHandles.lookup()));

        @Override
        public PyType getType() { return TYPE; }
    }
}
