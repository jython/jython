// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import org.python.core.Slot.EmptyException;

/**
 * The base class of many built-in descriptors. Descriptors are a
 * fundamental component of the Python type system, populating the
 * dictionary of every type.
 *
 * @implNote It must be possible to create an instance of any concrete
 *     descriptor (a sub-class of this one) in circumstances where the
 *     only types in existence are {@link PyType#TYPE} and
 *     {@link PyType#OBJECT_TYPE}, and where these have not yet been
 *     given their descriptor attributes or operation slots
 *     ({@code op_*} slots}.
 *     <p>
 *     In order to create a descriptor, the JVM need only complete the
 *     static initialisation of the Java class for that descriptor and
 *     be able to execute the constructor.
 */
abstract class Descriptor extends AbstractPyObject {

    protected static final String DESCRIPTOR_DOESNT_APPLY =
            "descriptor '%s' for '%.100s' objects doesn't apply to a '%.100s' object";
    protected static final String DESCRIPTOR_NEEDS_ARGUMENT =
            "descriptor '%s' of '%.100s' object needs an argument";
    protected static final String DESCRIPTOR_REQUIRES =
            "descriptor '%s' requires a '%.100s' object but received a '%.100s'";
    /** Single re-used instance of {@link Slot.EmptyException} */
    protected static final EmptyException EMPTY = new EmptyException();

    /**
     * Python {@code type} that defines the attribute being described
     * (e.g. for a method, the Python type of the object that will be
     * "self" in a call). This is exposed to Python as
     * {@code __objclass__}.
     */
    // In CPython, called d_type
    protected final PyType objclass;

    /**
     * Name of the object described, e.g. "__add__" or "to_bytes". This
     * is exposed to Python as {@code __name__}.
     */
    // In CPython, called d_name
    @Exposed.Member(value="__name__", readonly=true)
    protected final String name;

    /**
     * Qualified name of the object described, e.g. "float.__add__" or
     * "int.to_bytes". This is exposed to Python as
     * {@code __qualname__}.
     */
    // In CPython, called d_qualname. Where used? Better computed?
    protected String qualname = null;

    Descriptor(PyType descrtype, PyType objclass, String name) {
        super(descrtype);
        this.objclass = objclass;
        this.name = name;
    }

    /**
     * The {@code __get__} special method of the Python descriptor
     * protocol, implementing {@code obj.name} or possibly
     * {@code type.name}.
     *
     * @apiNote Different descriptor types may have quite different
     *     behaviour. In general, a call made with {@code obj == null}
     *     is seeking a result related to the {@code type}, while in one
     *     where {@code obj != null}, {@code obj} must be of type
     *     {@link #objclass} and {@code type} will be ignored.
     * @param obj object on which the attribute is sought or
     *     {@code null}
     * @param type on which this descriptor was found (may be ignored)
     * @return attribute value, bound object or this attribute
     * @throws Throwable from the implementation of the getter
     */
    // Compare CPython *_get methods in descrobject.c
    abstract Object __get__(Object obj, PyType type) throws Throwable;

    /**
     * Helper for {@code __repr__} implementation. It formats together
     * the {@code kind} argument ("member", "attribute", "method", or
     * "slot wrapper"), {@code this.name} and
     * {@code this.objclass.name}.
     *
     * @param kind description of type (first word in the repr)
     * @return repr as a {@code str}
     */
    protected String descrRepr(String kind) {
        return String.format("<%s '%.50s' of '%.100s' objects>", kind,
                name, objclass.name);
    }

    /**
     * {@code descr.__get__(obj, type)} has been called on this
     * descriptor. We must check that the descriptor applies to the type
     * of object supplied as the {@code obj} argument. From Python,
     * anything could be presented, but when we operate on it, we'll be
     * assuming the particular {@link #objclass} type.
     *
     * @param obj target object (non-null argument to {@code __get__})
     * @throws TypeError if descriptor doesn't apply to {@code obj}
     */
    // Compare CPython descr_check in descrobject.c
    /*
     * We differ from CPython in that: 1. We either throw or return
     * void: there is no FALSE->error or descriptor. 2. The test
     * obj==null (implying found on a type) is the caller's job. 3. In a
     * data descriptor, we fold the auditing into this check.
     */
    protected void check(Object obj) throws TypeError {
        PyType objType = PyType.of(obj);
        if (!objType.isSubTypeOf(objclass)) {
            throw new TypeError(DESCRIPTOR_DOESNT_APPLY, this.name,
                    objclass.name, objType.name);
        }
    }

    // Compare CPython calculate_qualname in descrobject.c
    private String calculate_qualname()
            throws AttributeError, Throwable {
        Object type_qualname =
                Abstract.getAttr(objclass, "__qualname__");
        if (type_qualname == null)
            return null;
        // XXX use PyUnicode.TYPE.check()
        if (!(PyType.of(type_qualname).isSubTypeOf(PyUnicode.TYPE))) {
            throw new TypeError(
                    "<descriptor>.__objclass__.__qualname__ is not a unicode object");
        }
        return String.format("%s.%s", type_qualname, name);
    }

    // Compare CPython descr_get_qualname in descrobject.c
    static Object descr_get_qualname(Descriptor descr, Object ignored)
            throws AttributeError, Throwable {
        if (descr.qualname == null)
            descr.qualname = descr.calculate_qualname();
        return descr.qualname;
    }

    @Override
    public String toString() { return Py.defaultToString(this); }
}
