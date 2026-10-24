// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

/**
 * Test that the Python type system comes into operation in a consistent
 * state after getting {@link PyObject#TYPE} The tests are all in the
 * superclass: this class supplies only the initialising event in
 * {@link #setUpClass()}.
 */
@DisplayName("After getting PyObject.TYPE ...")
class TypeInitTestObject extends TypeInitTest {

    /** Initialised in {@link #setUpClass()}. */
    static PyType object;

    /** Start by touching the type object {@code object}. */
    @BeforeAll
    static void setUpClass() { object = PyObject.TYPE; }
}
