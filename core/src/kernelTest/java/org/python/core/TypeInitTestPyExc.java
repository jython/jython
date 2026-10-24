// Copyright (c)20256 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test that the Python type system comes into operation in a consistent
 * state after referencing a type through {@link PyExc}. Most of the
 * tests are in the superclass.
 */
@DisplayName("After getting PyExc.UnboundLocalError ...")
@SuppressWarnings("static-method")
class TypeInitTestPyExc extends TypeInitTest {

    /** Initialised in {@link #setUpClass()}. */
    static PyType type;

    /** Start by touching the type object {@code UnboundLocalError}. */
    @BeforeAll
    static void setUpClass() { type = PyExc.UnboundLocalError; }

    @Test
    @DisplayName("The result is not null")
    void object_not_null() { assertNotNull(type); }

    @Test
    @DisplayName("PyExc.TypeError is not null")
    void typeerror_not_null() { assertNotNull(PyExc.TypeError); }

    @Test
    @DisplayName("PyExc.StopIteration is not null")
    void stopiter_not_null() { assertNotNull(PyExc.StopIteration); }

    @Test
    @DisplayName("PyExc.KeyError is not null")
    void keyerror_not_null() { assertNotNull(PyExc.KeyError); }
}
