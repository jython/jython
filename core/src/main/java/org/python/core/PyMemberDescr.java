// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.EnumSet;

import org.python.internal._PyUtil;
import org.python.kernel.BaseType;
import org.python.support.InterpreterError;
import org.python.types.Exposed;
import org.python.types.Feature;
import org.python.types.TypeSpec;

/**
 * Descriptor for an attribute that has been defined (by a
 * {@code @Member} annotations) to get and optionally set or delete the
 * value, with default type conversions.
 */
public abstract class PyMemberDescr extends DataDescriptor {

    /** Only referenced during bootstrap by {@link TypeSystem}. */
    static class Spec {
        /** @return the type specification. */
        static TypeSpec get() {
            return new TypeSystem.BootstrapSpec("member_descriptor", MethodHandles.lookup(),
                    PyMemberDescr.class).remove(Feature.INSTANTIABLE);
        }
    }

    /**
     * Return the Python type of {@code member_descriptor} objects,
     * {@code types.MemberDescriptorType}.
     *
     * @return {@code <class 'member_descriptor'>}
     */
    public static final PyType TYPE() { return TypeSystem.TYPE_member_descriptor; }

    /** Acceptable values in the {@link #flags}. */
    enum Flag {
        /**
         * This member may be read but not written. It may be declared so in the
         * decorator {@link Exposed.Member} or it may be inferred from the Java
         * {@code final} field flag.
         */
        READONLY,
        /**
         * This member may be absent (or deleted). Only a field implemented by a
         * reference type may be optional.
         */
        OPTIONAL,
        /**
         * In CPython, the RESTRICTED forms cause a call to {@code sys.audit}
         * and are here for compatibility with that eventual idea.
         */
        READ_RESTRICTED,
        /**
         * In CPython, the RESTRICTED forms cause a call to {@code sys.audit}
         * and are here for compatibility with that eventual idea.
         */
        WRITE_RESTRICTED
    }

    /**
     * Attributes controlling access and audit. (In CPython, the RESTRICTED
     * forms cause a call to {@code sys.audit} and are here for
     * compatibility with that eventual idea.) Note that {@code OPTIONAL},
     * although in the API, is represented as a sub-class attribute, to
     * minimise work in {@link #checkSet(Object)} and
     * {@link #checkDelete(Object)}
     */
    final EnumSet<Flag> flags;

    /** Reference to the field (offset) to access. */
    // Compare CPython PyMemberDef: int type; int offset;
    VarHandle field;

    /**
     * Construct a {@code PyMemberDescr} from a client-supplied handle. This
     * allows all JVM-supported access modes, but you have to make your own
     * handle.
     *
     * @param objclass Python type containing this member
     * @param name by which the member is known to Python
     * @param handle to the Java member
     * @param flags characteristics controlling access
     * @param doc documentation string (or {@code null})
     */
    PyMemberDescr(BaseType objclass, String name, VarHandle handle, EnumSet<Flag> flags,
            String doc) {
        super(objclass, name, doc);
        this.flags = flags;
        this.field = handle;
    }

    @Override
    public PyType getType() { return TYPE(); }

    private static VarHandle varHandle(Field f, Lookup lookup) {
        try {
            return lookup.unreflectVarHandle(f);
        } catch (IllegalAccessException e) {
            throw new InterpreterError(e, "cannot get handle for '%s'", f);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote Compare CPython {@code descr_check} in
     *     {@code descrobject.c}. We differ in that: (1) We throw directly
     *     on failure. (2) The condition {@code obj==null} (when found on a
     *     type) is the caller's job. (3) We fold the {@code sys.audit} call
     *     into this check.
     */
    @Override
    void check(Object obj) throws PyBaseException {
        PyType objType = PyType.of(obj);
        if (!objType.isSubTypeOf(objclass)) {
            throw selfTypeError(objType);
            /*
             * It is not sufficient to skip the test and catch the class cast from
             * VarHandle.get, because the wrong obj class is not necessarily the
             * wrong Java class.
             */
        } else if (flags.contains(Flag.READ_RESTRICTED)) {
            // Sys.audit("object.__getattr__", "Os",
            // obj != null ? obj : Py.None, name);
        }
    }

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
    @Override
    void checkSet(Object obj) throws PyBaseException {
        PyType objType = PyType.of(obj);
        if (!objType.isSubTypeOf(objclass)) { throw selfTypeError(objType); }
        if (!flags.isEmpty()) {
            if (flags.contains(Flag.READONLY)) {
                throw _PyUtil.readonlyAttributeOnType(objclass, name);
            } else if (flags.contains(Flag.WRITE_RESTRICTED)) {
                // Sys.audit("object.__setattr__", "Os",
                // obj != null ? obj : Py.None, name);
            }
        }
    }

    /**
     * {@code descr.__delete__(obj)} has been called on this descriptor. We
     * must check that the descriptor applies to the type of object supplied
     * as the {@code obj} argument. From Python, anything could be
     * presented, but when we operate on it, we'll be assuming the
     * particular {@link #objclass} type.
     */
    // Compare CPython descr_setcheck in descrobject.c
    @Override
    void checkDelete(Object obj) throws PyBaseException {
        PyType objType = PyType.of(obj);
        if (!objType.isSubTypeOf(objclass)) { throw selfTypeError(objType); }
        if (!flags.isEmpty()) {
            if (flags.contains(Flag.READONLY)) {
                throw _PyUtil.readonlyAttributeOnType(objclass, name);
            } else if (flags.contains(Flag.WRITE_RESTRICTED)) {
                // Sys.audit("object.__delattr__", "Os",
                // obj != null ? obj : Py.None, name);
            }
        }
    }

    /**
     * A method to get {@code o.name}, with conversion from the internal
     * field value if necessary (which will always succeed). This method is
     * called from {@link #__get__(Object, PyType)}, after checks, to
     * implement the type-specific conversion.
     *
     * @param obj object to access via {@link #field} (never null)
     * @return field value
     */
    // Compare CPython PyMember_GetOne in structmember.c
    abstract Object get(Object obj) throws PyAttributeError;

    /**
     * A method to set {@code o.name = v}, with conversion to the internal
     * field value if necessary. This method is called from
     * {@link #__set__(Object, Object)}, after checks, to implement the
     * type-specific conversion.
     *
     * @param obj object to access via {@link #field} (never null)
     * @param v value to assign: never null, may be {@code None}
     * @throws PyBaseException (TypeError) if v cannot be converted
     * @throws Throwable potentially from conversion
     */
    // Compare CPython PyMember_SetOne in structmember.c
    abstract void set(Object obj, Object v) throws PyAttributeError, PyBaseException, Throwable;

    /**
     * A method to delete {@code del o.name}. This method is called from
     * {@link #__delete__(Object)}, after checks, to implement the
     * type-specific delete.
     *
     * @implNote The default implementation is correct for primitive types
     *     (i.e. the majority) in raising {@link PyBaseException TypeError}
     *     with the message that the attribute cannot be deleted.
     * @param obj object to access via {@link #field} (never null)
     * @throws PyBaseException (TypeError) when not a type that can be
     *     deleted
     * @throws PyAttributeError when already deleted/undefined
     */
    // Compare CPython PyMember_SetOne in structmember.c with NULL
    void delete(Object obj) throws PyBaseException, PyAttributeError { throw cannotDeleteAttr(); }

    // Special methods ------------------------------------------------

    // Compare CPython member_repr in descrobject.c
    @SuppressWarnings("unused")
    private Object __repr__() { return descrRepr("member"); }

    /**
     * {@inheritDoc}
     *
     * If {@code obj != null} call {@link #get} on it to return a value.
     * {@code obj} must be of type {@link #objclass}. A call made with
     * {@code obj == null} returns {@code this} descriptor.
     *
     * @param type is ignored
     */
    @Override
    // Compare CPython member_get in descrobject.c
    public Object __get__(Object obj, PyType type) {
        if (obj == null)
            /*
             * obj==null indicates the descriptor was found on the target object
             * itself (or a base), see CPython type_getattro in typeobject.c
             */
            return this;
        else {
            check(obj);
            return get(obj);
        }
    }

    // Compare CPython member_set in descrobject.c
    @Override
    void __set__(Object obj, Object value) throws PyBaseException, Throwable {
        if (value == null) {
            // This ought to be an error, but allow for CPython idiom.
            __delete__(obj);
        } else {
            checkSet(obj);
            set(obj, value);
        }
    }

    // Compare CPython member_set in descrobject.c with NULL
    @Override
    void __delete__(Object obj) throws PyBaseException, Throwable {
        checkDelete(obj);
        delete(obj);
    }

    /**
     * Create a {@code PyMemberDescr} with behaviour specific to the class
     * of object being exposed.
     *
     * @param objclass Python type that owns the descriptor
     * @param name by which member known externally
     * @param field field to expose through this descriptor
     * @param lookup authorisation to access fields
     * @param flags supplying additional characteristics
     * @param doc documentation string (may be {@code null})
     * @return descriptor for access to the field
     * @throws InterpreterError if the field type is not supported
     */
    static PyMemberDescr forField(BaseType objclass, String name, Field field, Lookup lookup,
            EnumSet<Flag> flags, String doc) throws InterpreterError {
        Class<?> fieldType = field.getType();
        VarHandle vh = varHandle(field, lookup);
        // Note remove to minimise work in checkSet/checkDelete
        boolean opt = flags.remove(Flag.OPTIONAL);
        if (fieldType == int.class)
            return new _int(objclass, name, vh, flags, doc);
        else if (fieldType == double.class)
            return new _double(objclass, name, vh, flags, doc);
        else if (fieldType == String.class)
            return new _String(objclass, name, vh, flags, doc, opt);
        else if (fieldType.isPrimitive() || fieldType.isArray())
            // Identifiable exclusions as or-ed above. Enough?
            throw new InterpreterError(UNSUPPORTED_TYPE, name, field.getDeclaringClass().getName(),
                    fieldType.getSimpleName());
        else
            // Should be able to handle any object type not excluded.
            return new _Object(objclass, name, vh, flags, doc, opt);
    }

    private static final String UNSUPPORTED_TYPE =
            "@Member target %.50s in %.100s has unsupported type %.50s";

    private static class _int extends PyMemberDescr {

        _int(BaseType objclass, String name, VarHandle handle, EnumSet<Flag> flags, String doc) {
            super(objclass, name, handle, flags, doc);

        }

        @Override
        Object get(Object obj) { return (int)field.get(obj); }

        @Override
        void set(Object obj, Object value) throws PyBaseException, Throwable {
            int v = PyNumber.asSize(value, null);
            field.set(obj, v);
        }
    }

    private static class _double extends PyMemberDescr {

        _double(BaseType objclass, String name, VarHandle handle, EnumSet<Flag> flags, String doc) {
            super(objclass, name, handle, flags, doc);
        }

        @Override
        Object get(Object obj) { return (double)field.get(obj); }

        @Override
        void set(Object obj, Object value) throws PyBaseException, Throwable {
            double v = PyFloat.asDouble(value);
            field.set(obj, v);
        }
    }

    /** Behaviour for reference types. */
    private static abstract class Reference extends PyMemberDescr {

        /**
         * Controls what happens when the attribute implementation is
         * {@code null}, If {@code true}, {@link #get(Object)} will raise
         * {@link PyAttributeError AttributeError}. If {@code false},
         * {@link #get(Object)} will return {@code None}.
         * <p>
         * Delete sets the attribute implementation to {@code null}.
         */
        final boolean optional;

        Reference(BaseType objclass, String name, VarHandle handle, EnumSet<Flag> flags, String doc,
                boolean optional) {
            super(objclass, name, handle, flags, doc);
            this.optional = optional;
        }

        /**
         * {@inheritDoc}
         * <p>
         * If {@link #optional} and the attribute is {@code null}, reference
         * types raise an {@link PyAttributeError AttributeError}.
         */
        @Override
        void delete(Object obj) {
            if (optional && field.get(obj) == null)
                throw _PyUtil.noAttributeOnType(objclass, name);
            field.set(obj, null);
        }
    }

    /**
     * A string attribute that may be deleted (represented by {@code null}
     * in Java).
     */
    private static class _String extends Reference {

        _String(BaseType objclass, String name, VarHandle handle, EnumSet<Flag> flags, String doc,
                boolean optional) {
            super(objclass, name, handle, flags, doc, optional);
        }

        @Override
        Object get(Object obj) {
            String value = (String)field.get(obj);
            if (value == null) {
                if (optional)
                    throw _PyUtil.noAttributeOnType(objclass, name);
                else
                    return Py.None;
            }
            return value;
        }

        @Override
        void set(Object obj, Object value) throws PyBaseException, Throwable {
            // Special-case None if *not* an optional attribute
            if (value == Py.None && !optional) {
                delete(obj);
            } else {
                String v = PyUnicode.asString(value, (o) -> attrMustBe("a string", o));
                // String v = value.toString();
                field.set(obj, v);
            }
        }
    }

    /**
     * An {@code object} attribute that may be deleted (represented by
     * {@code null} in Java).
     */
    private static class _Object extends Reference {

        _Object(BaseType objclass, String name, VarHandle handle, EnumSet<Flag> flags, String doc,
                boolean optional) {
            super(objclass, name, handle, flags, doc, optional);
        }

        @Override
        Object get(Object obj) {
            Object value = field.get(obj);
            if (value == null) {
                if (optional)
                    throw _PyUtil.noAttributeOnType(objclass, name);
                else
                    return Py.None;
            }
            return value;
        }

        @Override
        void set(Object obj, Object value) throws PyBaseException, Throwable {
            // Special-case None if *not* an optional attribute
            if (value == Py.None && !optional) { delete(obj); return; }

            try {
                field.set(obj, value);
            } catch (ClassCastException cce) {
                // Raise TypeError mentioning Python type of field.
                throw attrMustBe(field.varType(), value);
            }
        }
    }
}
