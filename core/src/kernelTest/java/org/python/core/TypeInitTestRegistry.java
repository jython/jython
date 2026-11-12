// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

import org.python.kernel.TypeRegistry;

/**
 * Test that the Python type system comes into operation in a consistent
 * state after the registry is initialised explicitly. The tests are all
 * in the superclass: this class supplies only the initialising event in
 * {@link #setUpClass()}.
 */
@DisplayName("After TypeRegistry.getInstance() ...")
class TypeInitTestRegistry extends TypeInitTest {

    /** Initialised in {@link #setUpClass()}. */
    static TypeRegistry registry;

    /** Start by touching the type registry singleton itself. */
    @BeforeAll
    static void setUpClass() { registry = TypeSystem.registry; }
}
