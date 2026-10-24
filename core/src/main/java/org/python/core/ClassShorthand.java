// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import org.python.support.JavaClassShorthand;

/**
 * Some shorthands used to construct method signatures of runtime
 * methods, {@code MethodType}s, etc.. Clients can access the constants
 * by implementing the interface (slightly frowned upon in public code),
 * or paste this into their imports section: <pre>
import static org.python.runtime.ClassShorthand.*;
 *</pre>
 */
public interface ClassShorthand extends JavaClassShorthand {
    /** Shorthand for {@code PyType.class}. */
    public static final Class<PyType> T = PyType.class;
    /** Shorthand for {@code Comparison.class}. */
    // static final Class<Comparison> CMP = Comparison.class;
    /** Shorthand for {@code PyTuple.class}. */
    static final Class<PyTuple> TUPLE = PyTuple.class;
    /** Shorthand for {@code PyDict.class}. */
    static final Class<PyDict> DICT = PyDict.class;
}
