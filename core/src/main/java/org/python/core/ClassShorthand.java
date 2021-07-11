package org.python.core;

/**
 * Some shorthands used to construct method signatures, {@code MethodType}s,
 * etc..
 */
interface ClassShorthand {
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
    /** Shorthand for {@code PyType.class}. */
    static final Class<PyType> T = PyType.class;
    /** Shorthand for {@code void.class}. */
    static final Class<?> V = void.class;
    // ** Shorthand for {@code Comparison.class}. */
    // static final Class<Comparison> CMP = Comparison.class;
    /// ** Shorthand for {@code PyTuple.class}. */
    // static final Class<PyTuple> TUPLE = PyTuple.class;
    /// ** Shorthand for {@code PyDict.class}. */
    // static final Class<PyDict> DICT = PyDict.class;
    /** Shorthand for {@code Object[].class}. */
    static final Class<Object[]> OA = Object[].class;
    /** Shorthand for {@code String[].class}. */
    static final Class<String[]> SA = String[].class;
}
