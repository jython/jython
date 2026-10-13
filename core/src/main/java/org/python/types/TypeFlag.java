package org.python.types;

import org.python.core.PyType;

/**
 * Enumeration of the characteristics of a type. These are the
 * members that appear appear in the {@link PyType#flags} to
 * determine behaviours or provide quick answers to frequent
 * questions such as "are you a data descriptor".
 */
public enum TypeFlag {
    /**
     * Special methods may be assigned new meanings in the {@code type},
     * after creation.
     */
    MUTABLE,
    /**
     * An object of this type can change to another type (within
     * "layout" constraints).
     */
    VARIABLE,
    /**
     * This type the type allows sub-classing (is acceptable as a base).
     */
    BASETYPE,
    /**
     * An object of this type is a descriptor (defines {@code __get__}).
     */
    IS_DESCR,
    /**
     * An object of this type is a data descriptor (defines
     * {@code __get__} and at least one of {@code __set__} or
     * {@code __delete__}).
     */
    IS_DATA_DESCR,
    /**
     * An object of this type is a method descriptor (participates in an
     * optimised call pattern).
     */
    IS_METHOD_DESCR,
}