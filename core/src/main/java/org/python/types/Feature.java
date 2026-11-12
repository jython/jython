// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.types;

/**
 * Enumeration of the features of a type that may be specified in a
 * {@link TypeSpec}.
 */
public enum Feature {

    /**
     * The type allows sub-classing (is acceptable as a base in Python).
     */
    // Compare CPython Py_TPFLAGS_BASETYPE
    BASETYPE(TypeFlag.BASETYPE),

    /**
     * The type object is immutable: type attributes may not be set or
     * deleted after creation of the type.
     */
    // Compare CPython Py_TPFLAGS_IMMUTABLETYPE
    IMMUTABLE(TypeFlag.IMMUTABLE),

    /**
     * An instance of the type may become an instance of another type (by
     * {@code __class__} assignment), if this flag is set, provided
     * instances of the two types have a common representation in Java,
     * __slots__, and instances possess (or not) a __dict__ (equivalent to
     * CPython's "layout" constraints). This is separate from the
     * {@link #IMMUTABLE immutability of the type} itself.
     */
    // No equivalent in CPython
    REPLACEABLE(TypeFlag.REPLACEABLE),

    /**
     * This type allows the creation of new instances (by {@code __call__},
     * say). This flag is set by default. A type for which this flag is
     * <b>not set</b> should <b>not define</b> a {@code __new__} method (and
     * it will not inherit one). This is commonly done for types such as
     * iterators that we want to expose as Python objects but cannot validly
     * be created "loose" by the user.
     */
    // Compare CPython Py_TPFLAGS_DISALLOW_INSTANTIATION
    INSTANTIABLE(TypeFlag.INSTANTIABLE),

    /**
     * Instances of the type object are treated as sequences for pattern
     * matching. This does not affect whether Python treats instances as a
     * sequence functionally, which depends instead on whether a type
     * defines the required special methods.
     */
    // Compare CPython Py_TPFLAGS_SEQUENCE
    SEQUENCE(TypeFlag.SEQUENCE),
    /**
     * Instances of the type object are treated as mappings for pattern
     * matching. This does not affect whether Python treats instances as a
     * mapping functionally, which depends instead on whether a type defines
     * the required special methods.
     */
    // Compare CPython Py_TPFLAGS_MAPPING
    MAPPING(TypeFlag.MAPPING),

    /**
     * An instance of the type is a method descriptor, that is, it supports
     * an optimised call pattern where a {@code self} argument may be
     * supplied "loose" when calling the method, with the same meaning as if
     * it were first bound and the bound object called.
     * <p>
     * This is equivalent to setting the flag
     * {@code Py_TPFLAGS_METHOD_DESCRIPTOR} described in <a
     * href=https://peps.python.org/pep-0590/#descriptor-behavior>
     * PEP-590</a>. If {@code isMethodDescr()} returns {@code true} for
     * {@code type(func)}, then:
     * <ul>
     * <li>{@code func.__get__(obj, cls)(*args, **kwds)} (with {@code {@code
     * obj}} not None) must be equivalent to
     * {@code func(obj, *args, **kwds)}.</li>
     * <li>{@code func.__get__(None, cls)(*args, **kwds)} must be equivalent
     * to {@code func(*args, **kwds)}.</li>
     * </ul>
     */
    // Compare CPython Py_TPFLAGS_METHOD_DESCRIPTOR
    METHOD_DESCR(TypeFlag.METHOD_DESCR),

    /**
     * An instance of the type, which must define {@code __getitem__}, will
     * be treated as a sequence by the interpreter and not as a mapping. If
     * a type defines {@code __getitem__} but does not add the feature
     * {@code SEQUENCE_PROTOCOL}, its instances will be treated as a mapping
     * and not a sequence.
     * <p>
     * Only a type defined in Java has this possibility. A type defined in
     * Python will be accepted as a mapping if it defines
     * {@code __getitem__}.
     * <p>
     * In general, an indexing operation in Python leads to a call to
     * implementation of {@code __getitem__}, which will treat an index,
     * slice or key object (like a name) appropriately to its type. If the
     * receiving object is a sequence, only a numerical index or slice will
     * be valid. In a few places (e.g. in the implementation of
     * {@code exec}) the interpreter is required to check before use that an
     * object will accept object keys (is a mapping).
     */
    /*
     * The sense of this Feature reflects the default of CPython, which
     * accepts any type defined in Python with a __getitem__ is a mapping,
     * that is, it creates the tp_as_mapping->mp_subscript slot.
     */
    // No equivalent flag in CPython
    // Compare CPython tp_as_sequence, tp_as_mapping in typestruct.h
    SEQUENCE_PROTOCOL(TypeFlag.SEQUENCE_PROTOCOL);

    /** Navigate from feature to corresponding type flag. */
    public final TypeFlag flag;

    private Feature(TypeFlag flag) { this.flag = flag; }
}
