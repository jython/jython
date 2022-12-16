package org.python.core;

/** Base class of built-in data descriptors. */
abstract class DataDescriptor extends Descriptor {

    /**
     * Create the common part of {@code DataDescriptor} sub-classes.
     *
     * @param descrtype actual Python type of descriptor
     * @param objclass to which the descriptor applies
     * @param name of the attribute
     */
    DataDescriptor(PyType descrtype, PyType objclass, String name) {
        super(descrtype, objclass, name);
    }

    /**
     * The {@code __set__} special method of the Python descriptor
     * protocol, implementing {@code obj.name = value}. In general,
     * {@code obj} must be of type {@link #objclass}.
     *
     * @param obj object on which the attribute is sought
     * @param value to assign (not {@code null})
     * @throws Throwable from the implementation of the setter
     */
    // Compare CPython *_set methods in descrobject.c
    abstract void __set__(Object obj, Object value) throws TypeError, Throwable;

    /**
     * The {@code __delete__} special method of the Python descriptor
     * protocol, implementing {@code del obj.name}. In general,
     * {@code obj} must be of type {@link #objclass}.
     *
     * @param obj object on which the attribute is sought
     * @throws Throwable from the implementation of the deleter
     */
    // Compare CPython *_set in descrobject.c with NULL
    abstract void __delete__(Object obj) throws TypeError, Throwable;

    /**
     * {@code descr.__set__(obj, value)} has been called on this
     * descriptor. We must check that the descriptor applies to the type
     * of object supplied as the {@code obj} argument. From Python,
     * anything could be presented, but when we operate on it, we'll be
     * assuming the particular {@link #objclass} type.
     *
     * @param obj target object (argument to {@code __set__})
     * @throws TypeError if descriptor doesn't apply to {@code obj}
     */
    // Compare CPython descr_setcheck in descrobject.c
    protected void checkSet(Object obj) throws TypeError {
        PyType objType = PyType.of(obj);
        if (!objType.isSubTypeOf(objclass)) {
            throw new TypeError(DESCRIPTOR_DOESNT_APPLY, name, objclass.name, objType.name);
        }
    }

    /**
     * {@code descr.__delete__(obj)} has been called on this descriptor.
     * We must check that the descriptor applies to the type of object
     * supplied as the {@code obj} argument. From Python, anything could
     * be presented, but when we operate on it, we'll be assuming the
     * particular {@link #objclass} type.
     *
     * @param obj target object (argument to {@code __delete__})
     */
    // Compare CPython descr_setcheck in descrobject.c
    protected void checkDelete(Object obj) throws TypeError {
        PyType objType = PyType.of(obj);
        if (!objType.isSubTypeOf(objclass)) {
            throw new TypeError(DESCRIPTOR_DOESNT_APPLY, name, objclass.name, objType.name);
        }
    }

    /**
     * Create an {@link AttributeError} with a message along the lines
     * "attribute 'N' of 'T' objects is not readable" involving the name
     * N of this attribute and the type T which is
     * {@link Descriptor#objclass}.
     *
     * @return exception to throw
     */
    protected AttributeError cannotReadAttr() {
        String msg = "attribute '%.50s' of '%.100s' objects is not readable";
        return new AttributeError(msg, name, objclass.getName());
    }

    /**
     * Create an {@link AttributeError} with a message along the lines
     * "attribute 'N' of 'T' objects is not writable" involving the name
     * N of this attribute and the type T which is
     * {@link Descriptor#objclass}.
     *
     * @return exception to throw
     */
    protected AttributeError cannotWriteAttr() {
        String msg = "attribute '%.50s' of '%.100s' objects is not writable";
        return new AttributeError(msg, name, objclass.getName());
    }

    /**
     * Create a {@link TypeError} with a message along the lines "cannot
     * delete attribute N from 'T' objects" involving the name N of this
     * attribute and the type T which is {@link Descriptor#objclass},
     * e.g. "cannot delete attribute <u>f_trace_lines</u> from
     * '<u>frame</u>' objects".
     *
     * @return exception to throw
     */
    protected TypeError cannotDeleteAttr() {
        String msg = "cannot delete attribute %.50s from '%.100s' objects";
        return new TypeError(msg, name, objclass.getName());
    }

    /**
     * Create a {@link TypeError} with a message along the lines "'N'
     * must be T, not 'X' as received" involving the name N of the
     * attribute, any descriptive phrase T and the type X of
     * {@code value}, e.g. "'<u>__dict__</u>' must be <u>a
     * dictionary</u>, not '<u>list</u>' as received".
     *
     * @param kind expected kind of thing
     * @param value provided to set this attribute in some object
     * @return exception to throw
     */
    protected TypeError attrMustBe(String kind, Object value) {
        return Abstract.attrMustBe(name, kind, value);
    }

    /**
     * Create a {@link TypeError} with a message along the lines "'N'
     * must be T, not 'X' as received" involving the name N of the
     * attribute, a description T based on the expected Java class
     * {@code attrClass}, and the type X of {@code value}, e.g.
     * "'<u>__dict__</u>' must be <u>a dictionary</u>, not '<u>list</u>'
     * as received".
     *
     * @param attrClass expected kind of thing
     * @param value provided to set this attribute in some object
     * @return exception to throw
     */
    protected TypeError attrMustBe(Class<?> attrClass, Object value) {
        String kind;
        PyType pyType = PyType.fromClass(attrClass);
        if (pyType.acceptedCount == 1) {
            kind = String.format("'%.50s'", pyType.getName());
        } else {
            kind = String.format("'%.50s' (as %.50s)", attrClass.getSimpleName());
        }
        return Abstract.attrMustBe(name, kind, value);
    }
}
