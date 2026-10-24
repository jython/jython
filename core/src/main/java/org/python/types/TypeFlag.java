// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.types;

import java.util.EnumSet;

import org.python.core.PyType;

/**
 * Enumeration of the overt features of a type, as used on the type
 * object itself. They may be tested with
 * {@link PyType#hasFeature(TypeFlag)}, but not set directly. Some have
 * equivalents that may appear in a {@link TypeSpec type specification},
 * while others are computed during construction. They are used
 * internally or by extensions to determine behaviour, including where a
 * quick answer is needed to frequent questions such as "is this a
 * subclass of {@code str}".
 */
/*
 * A few of these values may not be in use anywhere. We reproduce the
 * CPython flags here or in KernelTypeFlag, until we find we definitely
 * don't need them, and we add some of our own.
 */
public enum TypeFlag {

    /**
     * The type allows sub-classing (is acceptable as a base in Python). See
     * also {@link Feature#BASETYPE}.
     */
    // Compare CPython Py_TPFLAGS_BASETYPE
    BASETYPE,

    /**
     * The type object is immutable: type attributes may not be set or
     * deleted after creation of the type. This does not necessarily impede
     * {@code __class__} assignment. See {@link #REPLACEABLE}. See also
     * {@link Feature#IMMUTABLE}.
     */
    // Compare CPython Py_TPFLAGS_IMMUTABLETYPE
    IMMUTABLE,

    /**
     * An instance of the type may become an instance of another type (by
     * {@code __class__} assignment), if this flag is set, provided
     * instances of the two types have a common representation in Java, have
     * the same set of {@code __slots__}, and instances possess (or not) a
     * {@code __dict__} (equivalent to CPython's "layout" constraints). This
     * is separate from the {@link #IMMUTABLE immutability of the type}
     * itself. See also {@link Feature#REPLACEABLE}.
     */
    // No equivalent in CPython
    REPLACEABLE,

    /**
     * This type allows the creation of new instances (by {@code __call__},
     * say). This flag is set by default. A type for which this flag is
     * <b>not set</b> should <b>not define</b> a {@code __new__} method (and
     * it will not inherit one). This is commonly done for types such as
     * iterators that we want to expose as Python objects but cannot validly
     * be created "loose" by the user. See also
     * {@link Feature#INSTANTIABLE}.
     */
    // Compare CPython Py_TPFLAGS_DISALLOW_INSTANTIATION
    INSTANTIABLE,  // CPython equivalent has opposite (-ve) sense.

    /**
     * Instances of the type object are treated as sequences for <em>pattern
     * matching</em>. It does not define the applicability of sequence
     * operations to instances of the type. See also
     * {@link Feature#SEQUENCE}.
     */
    // Compare CPython Py_TPFLAGS_SEQUENCE
    SEQUENCE,

    /**
     * Instances of the type object are treated as mappings for <em>pattern
     * matching</em>. It does not define the applicability of map operations
     * to instances of the type. See also {@link Feature#MAPPING}.
     */
    // Compare CPython Py_TPFLAGS_MAPPING
    MAPPING,

    /**
     * An instance of this type is a method descriptor, that is, it supports
     * an optimised call pattern where a {@code self} argument may be
     * supplied "loose" when calling the method, with the same meaning as if
     * it were first bound and the bound object called. See also
     * {@link Feature#METHOD_DESCR}.
     */
    // Compare CPython Py_TPFLAGS_METHOD_DESCRIPTOR
    METHOD_DESCR,

    /**
     * An instance of the type will be treated as a sequence by the
     * interpreter. (It should define {@code __getitem__}.)
     * <p>
     * A type that defines {@code __getitem__} but does not add the feature
     * {@code SEQUENCE_PROTOCOL} causes its instances to be treated as a
     * mapping, in places where the distinction matters (e.g. in the
     * implementation of {@code exec}). See also
     * {@link Feature#SEQUENCE_PROTOCOL}.
     */
    // No equivalent flag in CPython
    // Compare CPython tp_as_sequence, tp_as_mapping in typestruct.h
    SEQUENCE_PROTOCOL,

    /** The type is a subclass of {@code int}. */
    // Compare CPython Py_TPFLAGS_LONG_SUBCLASS
    INT_SUBCLASS,
    /** The type is a subclass of {@code list}. */
    // Compare CPython Py_TPFLAGS_LIST_SUBCLASS
    LIST_SUBCLASS,
    /** The type is a subclass of {@code tuple}. */
    // Compare CPython Py_TPFLAGS_TUPLE_SUBCLASS
    TUPLE_SUBCLASS,
    /** The type is a subclass of {@code bytes}. */
    // Compare CPython Py_TPFLAGS_BYTES_SUBCLASS
    BYTES_SUBCLASS,
    /** The type is a subclass of {@code str}. */
    // Compare CPython Py_TPFLAGS_UNICODE_SUBCLASS
    STR_SUBCLASS,
    /** The type is a subclass of {@code dict}. */
    // Compare CPython Py_TPFLAGS_DICT_SUBCLASS
    DICT_SUBCLASS,
    /** The type is a subclass of {@code BaseException}. */
    // Compare CPython Py_TPFLAGS_BASE_EXC_SUBCLASS
    EXCEPTION_SUBCLASS,
    /** The type is a subclass of {@code type}. */
    // Compare CPython Py_TPFLAGS_TYPE_SUBCLASS
    TYPE_SUBCLASS;

    /**
     * {@code TypeFlag}s inherited from the base type when constructing a
     * new type.
     */
    public static final EnumSet<TypeFlag> HERITABLE =
            EnumSet.of(BASETYPE, REPLACEABLE, SEQUENCE, MAPPING, SEQUENCE_PROTOCOL);
}
