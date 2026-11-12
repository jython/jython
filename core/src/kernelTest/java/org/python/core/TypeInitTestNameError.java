// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test that the Python type system comes into operation in a consistent
 * state after referencing the class {@link PyNameError}. Most of the
 * tests are in the superclass.
 */
@DisplayName("After getting PyNameError.TYPE ...")
@SuppressWarnings("static-method")
class TypeInitTestNameError extends TypeInitTest {

    /** Initialised in {@link #setUpClass()}. */
    static PyType type;

    /** Start by creating an instance of {@code PyNameError}. */
    @BeforeAll
    static void setUpClass() { type = PyNameError.TYPE; }

    @Test
    @DisplayName("PyNameError.TYPE is not null")
    void type_not_null() { assertNotNull(type); }

    @Test
    @DisplayName("PyExc.NameError is not null")
    void name_error_not_null() { assertNotNull(PyExc.NameError); }

    @Test
    @DisplayName("PyExc.KeyError is not null")
    void key_error_not_null() { assertNotNull(PyExc.KeyError); }

    @Test
    @DisplayName("PyExc.UnboundLocalError is not null")
    void unbound_local_error_not_null() { assertNotNull(PyExc.UnboundLocalError); }
}
