// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test that the Python type system comes into operation in a consistent
 * state after creating an instance of {@link PyStopIteration}, which
 * references a type through {@link PyExc} in its static initialisation.
 * Most of the tests are in the superclass.
 */
@DisplayName("After constructing a PyStopIteration ...")
@SuppressWarnings("static-method")
class TypeInitTestStopIteration extends TypeInitTest {

    /** Initialised in {@link #setUpClass()}. */
    static Object object;

    /** Start by creating an instance of {@code PyStopIteration}. */
    @BeforeAll
    static void setUpClass() { object = new PyStopIteration(); }

    @Test
    @DisplayName("PyStopIteration.TYPE is not null")
    void type_not_null() { assertNotNull(PyStopIteration.TYPE); }

    @Test
    @DisplayName("PyExc.TypeError is not null")
    void typeerror_not_null() { assertNotNull(PyExc.TypeError); }

    @Test
    @DisplayName("PyExc.StopIteration is not null")
    void stopiter_not_null() { assertNotNull(PyExc.StopIteration); }
}
