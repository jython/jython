// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * Test that the Python type system comes into operation in a consistent
 * state after creating a type with {@link PyType#fromSpec(TypeSpec)}.
 * The tests are all in the superclass: this class supplies only the
 * initialising event in {@link #setUpClass()}.
 */
@DisplayName("After creating a type with PyType.fromSpec() ...")
class TypeInitTestFromSpec extends TypeInitTest {

    /** Initialised in {@link #setUpClass()}. */
    static MyType myType;

    /** Example user-defined type. */
    static class MyType implements WithClass {
        /** Construct trivially. */
        MyType() {}

        /** The Python type. */
        PyType TYPE = PyType.fromSpec(new TypeSpec("MyType", MethodHandles.lookup()));

        @Override
        public PyType getType() { return TYPE; }
    }

    /** Start by using a Python type defined in Java. */
    @BeforeAll
    static void setUpClass() { myType = new MyType(); }
}
