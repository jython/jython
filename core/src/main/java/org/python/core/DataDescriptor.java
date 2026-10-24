// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import org.python.internal._PyUtil;
import org.python.kernel.BaseType;
import org.python.types.Exposed;

/** Base class of built-in data descriptors. */
public abstract class DataDescriptor extends Descriptor {

    /**
     * Create the common part of {@code DataDescriptor} sub-classes.
     *
     * @param objclass to which the descriptor applies
     * @param name of the attribute
     * @param doc documentation string (or {@code null})
     */
    DataDescriptor(BaseType objclass, String name, String doc) {
        super(objclass, name);
        // Allow null to represent empty doc
        this.doc = doc != null && doc.length() > 0 ? doc : null;
    }

    /** Documentation string for the attribute (or {@code null}). */
    // Compare CPython member_get_doc in descrobject.c
    @Exposed.Member(value = "__doc__", optional = true)
    final String doc;

    /**
     * The {@code __set__} special method of the Python descriptor protocol,
     * implementing {@code obj.name = value}. In general, {@code obj} must
     * be of type {@link #objclass}.
     *
     * @param obj object on which the attribute is sought
     * @param value to assign (not {@code null})
     * @throws Throwable from the implementation of the setter
     */
    // Compare CPython *_set methods in descrobject.c
    abstract void __set__(Object obj, Object value) throws PyBaseException, Throwable;

    /**
     * The {@code __delete__} special method of the Python descriptor
     * protocol, implementing {@code del obj.name}. In general, {@code obj}
     * must be of type {@link #objclass}.
     *
     * @param obj object on which the attribute is sought
     * @throws Throwable from the implementation of the deleter
     */
    // Compare CPython *_set in descrobject.c with NULL
    abstract void __delete__(Object obj) throws PyBaseException, Throwable;

    /**
     * {@code descr.__set__(obj, value)} has been called on this descriptor.
     * We must check that the descriptor applies to the type of object
     * supplied as the {@code obj} argument. From Python, anything could be
     * presented, but when we operate on it, we'll be assuming the
     * particular {@link #objclass} type.
     *
     * @param obj target object (argument to {@code __set__})
     * @throws PyBaseException (TypeError) if descriptor doesn't apply to
     *     {@code obj}
     */
    // Compare CPython descr_setcheck in descrobject.c
    void checkSet(Object obj) throws PyBaseException {
        PyType objType = PyType.of(obj);
        if (!objType.isSubTypeOf(objclass)) { throw selfTypeError(objType); }
    }

    /**
     * {@code descr.__delete__(obj)} has been called on this descriptor. We
     * must check that the descriptor applies to the type of object supplied
     * as the {@code obj} argument. From Python, anything could be
     * presented, but when we operate on it, we'll be assuming the
     * particular {@link #objclass} type.
     *
     * @param obj target object (argument to {@code __delete__})
     */
    // Compare CPython descr_setcheck in descrobject.c
    void checkDelete(Object obj) throws PyBaseException {
        PyType objType = PyType.of(obj);
        if (!objType.isSubTypeOf(objclass)) { throw selfTypeError(objType); }
    }

    /**
     * Create an {@link PyAttributeError AttributeError} with a message
     * along the lines "attribute 'N' of 'T' objects is not readable"
     * involving the name N of this attribute and the type T which is
     * {@link Descriptor#objclass}.
     *
     * @return exception to throw
     */
    PyBaseException cannotReadAttr() {
        String msg = "attribute '%.50s' of '%.100s' objects is not readable";
        return PyErr.format(PyExc.AttributeError, msg, name, objclass.getName());
    }

    /**
     * Create an {@link PyAttributeError AttributeError} with a message
     * along the lines "attribute 'N' of 'T' objects is not writable"
     * involving the name N of this attribute and the type T which is
     * {@link Descriptor#objclass}.
     *
     * @return exception to throw
     */
    PyBaseException cannotWriteAttr() {
        String msg = "attribute '%.50s' of '%.100s' objects is not writable";
        return PyErr.format(PyExc.AttributeError, msg, name, objclass.getName());
    }

    /**
     * Create a {@link PyBaseException TypeError} with a message along the
     * lines "cannot delete attribute N from 'T' objects" involving the name
     * N of this attribute and the type T which is
     * {@link Descriptor#objclass}, e.g. "cannot delete attribute
     * <u>f_trace_lines</u> from '<u>frame</u>' objects".
     *
     * @return exception to throw
     */
    PyBaseException cannotDeleteAttr() {
        String msg = "cannot delete attribute %.50s from '%.100s' objects";
        return PyErr.format(PyExc.TypeError, msg, name, objclass.getName());
    }

    /**
     * Create a {@link PyBaseException TypeError} with a message along the
     * lines "'N' must be T, not 'X' as received" involving the name N of
     * the attribute, any descriptive phrase T and the type X of
     * {@code value}, e.g. "'<u>__dict__</u>' must be <u>a dictionary</u>,
     * not '<u>list</u>' as received".
     *
     * @param kind expected kind of thing
     * @param value provided to set this attribute in some object
     * @return exception to throw
     */
    PyBaseException attrMustBe(String kind, Object value) {
        return _PyUtil.attrMustBe(name, kind, value);
    }

    /**
     * Create a {@link PyBaseException TypeError} with a message along the
     * lines "'N' must be T, not 'X' as received" involving the name N of
     * the attribute, a description T based on the expected Java class
     * {@code attrClass}, and the type X of {@code value}, e.g.
     * "'<u>__dict__</u>' must be <u>a dictionary</u>, not '<u>list</u>' as
     * received".
     *
     * @param attrClass expected kind of thing
     * @param value provided to set this attribute in some object
     * @return exception to throw
     */
    PyBaseException attrMustBe(Class<?> attrClass, Object value) {
        String kind;
        PyType pyType = PyType.of(attrClass);
        if (pyType.selfClasses().size() == 1) {
            kind = String.format("'%.50s'", pyType.getName());
        } else {
            kind = String.format("'%.50s' (as %.50s)", attrClass.getSimpleName());
        }
        return _PyUtil.attrMustBe(name, kind, value);
    }
}
