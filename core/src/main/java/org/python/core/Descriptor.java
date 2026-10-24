// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import org.python.kernel.BaseType;
import org.python.types.Exposed;
import org.python.types.WithClass;

/**
 * The base class of many built-in descriptors. Descriptors are a
 * fundamental component of the Python type system, populating the
 * dictionary of every type.
 *
 * @implNote It must be possible to create an instance of any concrete
 *     descriptor (a sub-class of this one) in circumstances where the
 *     only types in existence are {@link PyType} and {@link PyObject},
 *     and where these have not yet been given their descriptor
 *     attributes or operation slots ({@code op_*} slots}.
 *     <p>
 *     In order to create a descriptor, the JVM need only complete the
 *     static initialisation of the Java class for that descriptor and
 *     be able to execute the constructor.
 */
abstract class Descriptor implements WithClass {

    /**
     * Python {@code type} that defines the unbound member, attribute or
     * method being described. For a method, it is the Python type of the
     * object that will be "self" in a call. This is exposed to Python as
     * {@code __objclass__}.
     */
    // In CPython, called d_type
    @Exposed.Member("__objclass__")
    final BaseType objclass;

    /**
     * Name of the object described, for example "__add__" or "to_bytes".
     * This is exposed to Python as {@code __name__}.
     */
    @Exposed.Member(value = "__name__")
    // In CPython, called d_name
    final String name;

    /**
     * Qualified name of the object described, for example "float.__add__"
     * or "int.to_bytes". This is exposed to Python as {@code __qualname__}.
     */
    // In CPython, called d_qualname.
    // TODO: Where is this used? Is it better computed?
    private String qualname;

    /**
     * Create the common part of {@code Descriptor} sub-classes.
     *
     * @param objclass that defines the attribute being described
     * @param name of the object described as {@code __name__}
     */
    Descriptor(BaseType objclass, String name) {
        assert objclass != null;
        this.objclass = objclass;
        assert name != null;
        this.name = name;
    }

    // Exposed attributes ---------------------------------------------

    /**
     * Return the class where this member, attribute or method was defined.
     * This is exposed to Python as {@code __objclass__}. For a method
     * descriptor, it is the type of the object that is expected as the
     * first (that is, {@code self}) argument, and that is bound (as
     * {@code obj}) when {@code __get__} is called. For a data descriptor,
     * it is the type of the object that is expected as {@code obj}) when
     * {@code __get__}, {@code __set__} or {@code __delete__} is called.
     *
     * @return the class where this object was defined
     */
    // Do not apply @Exposed.Getter as __objclass__ is an Exposed.Member
    // TODO Do we need this function at all? As Java API only, maybe.
    // Compare CPython descr_objclass in descrobject.c
    public PyType __objclass__() { return objclass; }

    /**
     * Return the name of the member, attribute or method described. For
     * example it is {@code "__add__"} or {@code "to_bytes"}. This is
     * exposed to Python as {@code __name__}.
     *
     * @return the plain name of the member, attribute or method
     */
    // Do not apply @Exposed.Getter as __objclass__ is an Exposed.Member
    // TODO Do we need this function at all? As Java API only, maybe.
    // Compare CPython descr_name in descrobject.c
    String __name__() { return name; }

    /**
     * Return the qualified name of the member, attribute or method
     * described, which includes the (qualified) type name. For example it
     * is {@code "float.__add__"} or {@code "int.to_bytes"}. This is exposed
     * to Python as {@code __qualname__}.
     *
     * @return the name of the member, attribute or method
     * @throws PyAttributeError if the type does not have
     *     {@code __qualname__}
     * @throws PyBaseException if it is not a string
     * @throws Throwable from other causes
     */
    @Exposed.Getter("__qualname__")
    // Compare CPython descr_get_qualname in descrobject.c
    String __qualname__() throws PyAttributeError, PyBaseException, Throwable {
        if (qualname == null) { qualname = calculate_qualname(); }
        return qualname;
    }

    /**
     * The {@code __get__} special method of the Python descriptor protocol.
     *
     * @apiNote Different descriptor types may have quite different
     *     behaviour. In general, a call made with {@code obj == null} is
     *     seeking a result related to the {@code type}, while in one where
     *     {@code obj != null}, {@code obj} must be of type
     *     {@link #objclass} and {@code type} will be ignored.
     * @param obj object on which the attribute is sought or {@code null}
     * @param type on which this descriptor was found (may be ignored)
     * @return attribute value, bound object or this attribute
     * @throws Throwable from the implementation of the getter
     */
    // Compare CPython *_get methods in descrobject.c
    public abstract Object __get__(Object obj, PyType type) throws Throwable;

    // TODO Incompletely ported from CPython:
    // CPython method table (to convert to annotations):
    // private MethodDef descr_methods[] = {
    // {"__reduce__", (PyCFunction)descr_reduce, METH_NOARGS, null},
    // {null, null}
    // };

    // Plumbing ------------------------------------------------------

    /**
     * Helper for {@code __repr__} implementation. It formats together the
     * {@code kind} argument ("member", "attribute", "method", or "slot
     * wrapper"), {@code this.name} and {@code this.objclass.name}.
     *
     * @param kind description of type (first word in the repr)
     * @return repr as a {@code str}
     */
    String descrRepr(String kind) {
        return String.format("<%s '%.50s' of '%.100s' objects>", kind, name, objclass.getName());
    }

    /**
     * A check made when the descriptor is applied to an argument
     * {@code obj}. This descriptor is being applied to a target object
     * {@code obj}, maybe as a result of a call
     * {@code descr.__get__(obj, type)}. From Python, anything could be
     * presented, but when we operate on it, we'll be assuming (a Java class
     * accepted by) the particular {@link #objclass} that defined the
     * descriptor, or a subclass of it.
     * <p>
     * When the type of {@code obj} is not a subclass of {@code objclass}
     * this method raises a {@link PyBaseException TypeError} by calling
     * {@link #selfTypeError(PyType)}.
     *
     * @param obj target object (non-null argument to {@code __get__})
     * @throws PyBaseException (TypeError) if descriptor doesn't apply to
     *     {@code obj}
     */
    // Compare CPython descr_check in descrobject.c
    void check(Object obj) throws PyBaseException {
        PyType objType = PyType.of(obj);
        if (objType != objclass) { checkPythonType(objType); }
    }

    /**
     * A check made when the descriptor is applied to an argument
     * {@code obj}. This is equivalent to {@link #check(Object)}, but when
     * we have the {@code type(obj)} already.
     *
     * @param objType target object type
     * @throws PyBaseException (TypeError) if descriptor doesn't apply to
     *     {@code objType}
     */
    void checkPythonType(PyType objType) throws PyBaseException {
        // XXX Should this call Abstract.recursiveIsSubclass instead?
        if (objType.isSubTypeOf(objclass)) { return; }
        throw selfTypeError(objType);
    }

    /**
     * Create a {@link PyBaseException TypeError} with a message along the
     * lines "descriptor 'D' of 'T' objects doesn't apply to 'S' objects"
     * involving the name of this descriptor, {@link #objclass} and a type
     * which is usually the type of a {@code self} to which the descriptor
     * was erroneously applied.
     *
     * @param selfType the type of object actually received
     * @return exception to throw
     */
    PyBaseException selfTypeError(PyType selfType) {
        return PyErr.format(PyExc.TypeError, DESCRIPTOR_DOESNT_APPLY, name, objclass.getName(),
                selfType.getName());
    }

    private static final String DESCRIPTOR_DOESNT_APPLY =
            "descriptor '%s' of '%.100s' objects doesn't apply to '%.100s' objects";

    // Compare CPython calculate_qualname in descrobject.c
    private String calculate_qualname() throws PyAttributeError, PyBaseException, Throwable {
        Object type_qualname = Abstract.getAttr(objclass, "__qualname__");
        if (!PyUnicode.TYPE.check(type_qualname)) {
            // Unfortunately, clients can mess with __getattribute__.
            throw PyErr.format(PyExc.TypeError, QUALNAME_NOT_STRING);
        }
        return String.format("%s.%s", type_qualname, name);
    }

    private static final String QUALNAME_NOT_STRING =
            "<descriptor>.__objclass__.__qualname__ is not a string";

    // TODO Incompletely ported from CPython:
    // static Object descr_reduce(PyDescr descr, Object ignored)
    // {
    // return Py_BuildValue("N(OO)", _PyEval_GetBuiltinId(ID.getattr),
    // descr.objclass, PyDescr_NAME(descr));
    // }

    @Override
    public String toString() { return PyUtil.defaultToString(this); }

    /**
     * Method {@code __text_signature__} string based on internal
     * documentation.
     *
     * @param name of method, member or get-set
     * @param doc from special method enum or descriptor
     * @return appropriate {@code __text_signature__}
     */
    // Compare CPython: _PyType_GetTextSignatureFromInternalDoc
    // Used by: method_get_text_signature,
    // wrapperdescr_get_text_signature, wrapper_text_signature
    // For us: PyMethodDescr, PyWrapperDescr, PyMethodWrapper
    /*
     * In CPython, this picks apart the string in the MethodDef, that was
     * built by Argument Clinic or hand-crafted. We don't do that because we
     * have ArgParser (except we have mistakenly aped typeobject.c in
     * SpecialMethod).
     */
    // TODO Implement __text_signature__ from ArgParser instead.
    // In order to do that, SpecialMethod must contain a parser.
    static Object getTextSignatureFromInternalDoc(String name, String doc) {
        // TODO Auto-generated method stub
        return Py.None;
    }

    /**
     * Method {@code __doc__} string based on internal documentation.
     *
     * @param name of method, member or get-set
     * @param doc from special method or descriptor
     * @return appropriate value of {@code __doc__}
     */
    // Compare CPython _PyType_GetDocFromInternalDoc
    // Used by: method_get_doc, wrapperdescr_get_doc, wrapper_doc
    // For us: PyMethodDescr, PyWrapperDescr, PyMethodWrapper
    /*
     * In CPython, this picks apart the string in the MethodDef, that was
     * built by Argument Clinic or hand-crafted. We don't do that because we
     * have ArgParser (except we have mistakenly aped typeobject.c in
     * SpecialMethod).
     */
    // TODO Implement __doc__ from ArgParser instead
    // In order to do that, SpecialMethod must contain a parser.
    static Object getDocFromInternalDoc(String name, String doc) {
        // TODO Auto-generated method stub
        return Py.None;
    }
}
