// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.support;

/**
 * Some shorthands used to construct method signatures,
 * {@code MethodType}s, etc.. Clients can access the constants by
 * implementing the interface (slightly frowned upon in public code), or
 * paste this into their imports section: <pre>
import static org.python.support.JavaClassShorthand.*;
 *</pre>
 */
public interface JavaClassShorthand {
    /** Shorthand for {@code Object.class}. */
    static final Class<Object> O = Object.class;
    /** Shorthand for {@code Class.class}. */
    static final Class<?> C = Class.class;
    /** Shorthand for {@code String.class}. */
    static final Class<String> S = String.class;
    /** Shorthand for {@code int.class}. */
    static final Class<?> I = int.class;
    /** Shorthand for {@code boolean.class}. */
    static final Class<?> B = boolean.class;
    /** Shorthand for {@code void.class}. */
    static final Class<?> V = void.class;
    /** Shorthand for {@code Object[].class}. */
    static final Class<Object[]> OA = Object[].class;
    /** Shorthand for {@code String[].class}. */
    static final Class<String[]> SA = String[].class;
}
