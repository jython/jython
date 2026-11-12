// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.python.core.ArgumentError.Mode;
import org.python.internal.Util;
import org.python.kernel.BaseType;
import org.python.kernel.Representation;
import org.python.kernel.SpecialMethod;
import org.python.types.Exposed;
import org.python.types.Feature;
import org.python.types.TypeSpec;

/**
 * A {@link Descriptor} for a particular definition <b>in Java</b> of
 * one of the special methods of the Python data model (such as
 * {@code __sub__}). The type appears as
 * {@code <class 'wrapper_descriptor'>} but instances may appear as
 * {@code <slot wrapper '...' of '...' objects>}.
 * <p>
 * The owner of the descriptor is the Python type providing the
 * definition. Type construction places a {@code PyWrapperDescr} in the
 * dictionary of the defining {@link PyType}, against a key that is the
 * "dunder name" of the special method it wraps. (This does not preclude
 * client code moving it around afterwards!)
 * <p>
 * The {@code PyWrapperDescr} provides a {@code MethodHandle} for the
 * defining method. In every Python type where a {@code PyWrapperDescr}
 * appears as the attribute value corresponding to a special method,
 * type construction will give it the special significance that name has
 * in the Python data model, provided the signature of the wrapped
 * method allows. This may be because the type is the defining type, by
 * inheritance, or by insertion of the {@code PyWrapperDescr} as an
 * attribute of the type.
 */
// Compare CPython: PyWrapperDescrObject in descrobject.h
// and functions wrapperdescr_* in descrobject.c.
/*
 * Difference from CPython: In CPython, a PyWrapperDescr is created
 * because the slot at the corresponding offset in the PyTypeObject of
 * the owning Python type is filled, statically or by PyType_FromSpec.
 * It is then made an attribute in the dictionary of the type.
 *
 * In this implementation, we create a PyWrapperDescr as an attribute
 * because the Java implementation of the owning type defines a method
 * with the corresponding "dunder name". Then we (may) fill a slot in
 * the Representation because the type has an attribute with this name.
 * This second step is the same process that covers special methods
 * defined in Python.
 */
public abstract class PyWrapperDescr extends MethodDescriptor {

    /** Only referenced during bootstrap by {@link TypeSystem}. */
    static class Spec {
        /** @return the type specification. */
        static TypeSpec get() {
            return new TypeSystem.BootstrapSpec("wrapper_descriptor", MethodHandles.lookup(),
                    PyWrapperDescr.class) //
                            .add(Feature.METHOD_DESCR) //
                            .remove(Feature.INSTANTIABLE);
        }
    }

    /**
     * Return the Python type of {@code wrapper_descriptor} objects,
     * {@code types.WrapperDescriptorType}.
     *
     * @return {@code <class 'wrapper_descriptor'>}
     */
    public static final PyType TYPE() { return TypeSystem.TYPE_wrapper_descriptor; }

    /**
     * The {@link SpecialMethod} ({@code enum}) describing the generic
     * characteristics of the special method, of which
     * {@link Descriptor#objclass} provides a particular implementation.
     */
    final SpecialMethod sm;

    /**
     * Construct a slot wrapper descriptor for the {@code slot} in
     * {@code objclass}.
     *
     * @param objclass the class declaring the special method
     * @param slot for the generic special method
     */
    // Compare CPython PyDescr_NewWrapper in descrobject.c
    PyWrapperDescr(BaseType objclass, SpecialMethod slot) {
        super(objclass, slot.methodName);
        this.sm = slot;
    }

    @Override
    public PyType getType() { return TYPE(); }

    // Exposed attributes --------------------------------------------

    /**
     * Return the documentation string from the descriptor in an external
     * format.
     *
     * @return the documentation string
     */
    @Exposed.Getter
    // Compare CPython wrapperdescr_get_doc in descrobject.c
    private Object __doc__() { return Descriptor.getDocFromInternalDoc(sm.methodName, sm.doc); }

    /**
     * Return the method signature string from the descriptor in an external
     * format.
     *
     * @return the method signature
     */
    @Exposed.Getter
    // Compare CPython wrapperdescr_get_text_signature in descrobject.c
    private Object __text_signature__() {
        return Descriptor.getTextSignatureFromInternalDoc(sm.methodName, sm.doc);
    }

    // Special methods ------------------------------------------------

    // Compare CPython wrapperdescr_repr in descrobject.c
    @SuppressWarnings("unused")
    private Object __repr__() { return descrRepr("slot wrapper"); }

    /**
     * Return the wrapped special method, bound to {@code obj} as its
     * {@code self} argument, or if {@code obj==null}, return this
     * descriptor. In the non-{@code null} case, {@code __get__} returns a
     * {@link PyMethodWrapper}. Calling the returned object invokes the same
     * special method as this descriptor, with {@code obj} as first
     * argument, and other arguments to the call appended.
     *
     * @param obj target ({@code self}) of the method, or {@code null}
     * @param type ignored
     * @return method bound to {@code obj} or this descriptor.
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if
     *     {@code obj!=null} is not compatible
     */
    // Compare CPython wrapperdescr_get in descrobject.c
    @Override
    public Object __get__(Object obj, PyType type) {
        if (obj == null)
            /*
             * obj==null indicates the descriptor was found on the target object
             * itself (or a base), see CPython type_getattro in typeobject.c
             */
            return this;
        else {
            // Return callable binding this and obj
            check(obj);
            return new PyMethodWrapper(this, obj);
        }
    }

    /**
     * Call the wrapped method with positional arguments (the first being
     * the target object) and optionally keywords arguments. The arguments,
     * in type and number, must match the signature of the special function
     * slot.
     *
     * @param args positional arguments beginning with {@code self}
     * @param names of keywords in the method call
     * @return result of calling the wrapped method
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if
     *     {@code args[0]} is the wrong type
     * @throws Throwable from the implementation of the special method
     */
    // Compare CPython wrapperdescr_call in descrobject.c
    Object __call__(Object[] args, String[] names) throws PyBaseException, Throwable {
        try {
            return call(args, names);
        } catch (ArgumentError ae) {
            /*
             * FastCall.call() methods may encode a TypeError as an ArgumentError
             * with limited context.
             */
            assert args.length > 0;
            Object[] rest = Arrays.copyOfRange(args, 1, args.length);
            // Add local context to make a TypeError.
            throw typeError(ae, rest, names);
        }
    }

    @Override
    public Object call(Object[] args, String[] names) throws ArgumentError, Throwable {

        int n = args.length, m = n - 1;

        if (m < 0) {
            // Not even one argument
            throw new ArgumentError(Mode.SELF);

        } else {
            // Split the leading element self from rest of args
            Object self = args[0], rest[];
            if (m == 0) {
                rest = Util.EMPTY_ARRAY;
            } else {
                rest = new Object[m];
                System.arraycopy(args, 1, rest, 0, m);
            }

            return call(self, rest, names);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The method arranges {@code self} and the other arguments according to
     * the signature of the wrapped special method, avoiding an array copy.
     *
     * @param self target object of the method call
     * @param args of the method call
     * @param names of keywords in the method call
     * @return result of the method call
     * @throws ArgumentError if the number of arguments is incorrect
     * @throws Throwable from the implementation of the special method
     */
    // Compare CPython wrapperdescr_raw_call in descrobject.c
    // Also, CPython wrap_* functions in typeobject.c
    @Override
    public Object call(Object self, Object[] args, String[] names) throws ArgumentError, Throwable {
        // Call through the correct wrapped handle for self
        MethodHandle mh = getHandle(self);
        String name;
        switch (sm.signature) {
            case UNARY:
                checkNoArgs(args, names);
                return mh.invokeExact(self);
            case BINARY:
                checkArgs(args, 1, names);
                return mh.invokeExact(self, args[0]);
            case TERNARY:
                checkArgs(args, 2, names);
                return mh.invokeExact(self, args[0], args[1]);
            case CALL:
                return mh.invokeExact(self, args, names);
            case PREDICATE:
                checkNoArgs(args, names);
                return (boolean)mh.invokeExact(self);
            case BINARY_PREDICATE:
                checkArgs(args, 1, names);
                return (boolean)mh.invokeExact(self, args[0]);
            case LEN:
                checkNoArgs(args, names);
                return (int)mh.invokeExact(self);
            case SETITEM:
                checkArgs(args, 2, names);
                mh.invokeExact(self, args[0], args[1]);
                return Py.None;
            case DELITEM:
                checkArgs(args, 1, names);
                mh.invokeExact(self, args[0]);
                return Py.None;
            case GETATTR:
                checkArgs(args, 1, names);
                name = PyUnicode.asString(args[0], Abstract::attributeNameTypeError);
                return mh.invokeExact(self, name);
            case SETATTR:
                checkArgs(args, 2, names);
                name = PyUnicode.asString(args[0], Abstract::attributeNameTypeError);
                mh.invokeExact(self, name, args[1]);
                return Py.None;
            case DELATTR:
                checkArgs(args, 1, names);
                name = PyUnicode.asString(args[0], Abstract::attributeNameTypeError);
                mh.invokeExact(self, name);
                return Py.None;
            case DESCRGET:
                checkArgs(args, 1, 2, names);
                Object obj = args[0];
                if (obj == Py.None) { obj = null; }
                PyType type = null;
                if (args.length == 2) {
                    if (args[1] instanceof PyType t) { type = t; }
                    // Following CPython in ignoring other types.
                }
                if (type == null && obj == null) { throw PyErr.format(PyExc.TypeError, GET_NONE); }
                return mh.invokeExact(self, obj, type);
            case INIT:
                mh.invokeExact(self, args, names);
                return Py.None;
            default:
                assert false;  // switch should be exhaustive
                return Py.None;
        }
    }

    @Override
    public Object call(Object self) throws ArgumentError, Throwable {
        MethodHandle mh = getHandle(self);
        return switch (sm.signature) {
            case UNARY -> mh.invokeExact(self);
            case PREDICATE -> (boolean)mh.invokeExact(self);
            case LEN -> (int)mh.invokeExact(self);
            default -> super.call(self);
        };
    }

    @Override
    public Object call(Object self, Object a1) throws ArgumentError, Throwable {
        MethodHandle mh = getHandle(self);
        String name;
        switch (sm.signature) {
            case BINARY:
                return mh.invokeExact(self, a1);
            case BINARY_PREDICATE:
                return (boolean)mh.invokeExact(self, a1);
            case DELITEM:
                mh.invokeExact(self, a1);
                return Py.None;
            case GETATTR:
                name = PyUnicode.asString(a1, Abstract::attributeNameTypeError);
                return mh.invokeExact(self, name);
            case DELATTR:
                name = PyUnicode.asString(a1, Abstract::attributeNameTypeError);
                mh.invokeExact(self, name);
                return Py.None;
            case DESCRGET:
                if (a1 == Py.None || a1 == null) { throw PyErr.format(PyExc.TypeError, GET_NONE); }
                return mh.invokeExact(self, a1, (PyType)null);
            default:
                return super.call(self, a1);
        }
    }

    @Override
    public Object call(Object self, Object a1, Object a2) throws ArgumentError, Throwable {
        MethodHandle mh = getHandle(self);
        String name;
        switch (sm.signature) {
            case TERNARY:
                return mh.invokeExact(self, a1, a2);
            case SETITEM:
                mh.invokeExact(self, a1, a2);
                return Py.None;
            case SETATTR:
                name = PyUnicode.asString(a1, Abstract::attributeNameTypeError);
                mh.invokeExact(self, name, a2);
                return Py.None;
            case DESCRGET:
                if (a1 == Py.None) { a1 = null; }
                if (a2 instanceof PyType type) {
                    return mh.invokeExact(self, a1, type);
                } else if (a1 == null) {
                    // Following CPython in treating non-type as null.
                    throw PyErr.format(PyExc.TypeError, GET_NONE);
                }
                return mh.invokeExact(self, a1, null);
            default:
                return super.call(self, a1, a2);
        }
    }

    /**
     * Return the correct handle for the {@code self} argument, taking into
     * account the possibility that the type of {@code self} is a proper
     * sub-type of {@code __objclass__} or not a sub-type at all.
     *
     * @param self by which to select the handle
     * @return the handle to call with {@code self} as first argument
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if the
     *     type of {@code self} does not match the wrapped handle.
     * @throws Throwable propagated from subclass check
     */
    abstract MethodHandle getHandle(Object self) throws PyBaseException, Throwable;

    private static final String GET_NONE = "__get__(None, None) is invalid";

    /**
     * Create a {@link PyBaseException TypeError} with a message along the
     * lines "descriptor 'D' requires a 'T' object but received a 'S'"
     * involving the name of this descriptor, {@link #objclass} and a type
     * which is usually the type of a {@code self} to which the descriptor
     * was erroneously applied.
     *
     * @param selfType the type of object actually received
     * @return exception to throw
     */
    @Override
    PyBaseException selfTypeError(PyType selfType) {
        /*
         * For some reason, a wrapper descriptor has a slightly different
         * message from other descriptors.
         */
        return PyErr.format(PyExc.TypeError, DESCRIPTOR_REQUIRES, name, objclass.getName(),
                selfType.getName());
    }

    private static final String DESCRIPTOR_REQUIRES =
            "descriptor '%s' requires a '%.100s' object but received a '%.100s'";

    /**
     * A {@link PyWrapperDescr} for use when the owning Python type has just
     * one accepted implementation.
     */
    static class Single extends PyWrapperDescr {

        /**
         * A handle for the particular implementation of a special method being
         * wrapped. The method type is that of {@link #sm}{@code .signature}.
         */
        protected final MethodHandle wrapped;

        /**
         * Construct a slot wrapper descriptor, identifying by a method handle
         * the implementation method for the special method in
         * {@link Descriptor#objclass}.
         *
         * @param objclass the class declaring the special method
         * @param sm the generic special method
         * @param wrapped a handle to an implementation of that slot
         */
        // Compare CPython PyDescr_NewWrapper in descrobject.c
        Single(BaseType objclass, SpecialMethod sm, MethodHandle wrapped) {
            super(objclass, sm);
            this.wrapped = wrapped;
        }

        @Override
        public MethodHandle getHandle(int selfClassIndex) {
            assert selfClassIndex == 0;
            return wrapped;
        }

        @Override
        MethodHandle getHandle(Object self) throws PyBaseException, Throwable {
            // Check acceptability at the Python level
            check(self);
            return wrapped;
        }
    }

    /**
     * A {@link PyWrapperDescr} for use when the owning Python type has
     * multiple accepted implementations.
     */
    static class Multiple extends PyWrapperDescr {

        /**
         * Handles for the particular implementations of a special method being
         * wrapped. The method type of each is that of
         * {@link #sm}{@code .signature}.
         */
        protected final MethodHandle[] wrapped;

        /**
         * Construct a slot wrapper descriptor, identifying by an array of
         * method handles the implementation methods for the special method in
         * {@link Descriptor#objclass}.
         *
         * @param objclass the class declaring the special method
         * @param sm the generic special method
         * @param wrapped handles to the implementation of that slot
         */
        // Compare CPython PyDescr_NewWrapper in descrobject.c
        Multiple(BaseType objclass, SpecialMethod sm, MethodHandle[] wrapped) {
            super(objclass, sm);
            assert wrapped.length == objclass.selfClasses().size();
            this.wrapped = wrapped;
        }

        @Override
        public MethodHandle getHandle(int selfClassIndex) { return wrapped[selfClassIndex]; }

        @Override
        MethodHandle getHandle(Object self) throws PyBaseException, Throwable {
            Representation rep = TypeSystem.registry.get(self.getClass());
            PyType selfType = rep.pythonType(self);
            if (selfType == objclass) {
                // selfType defined the method so it must be ok
                int index = rep.getIndex();
                return wrapped[index];
            } else {
                // Check validity at the Python level
                checkPythonType(selfType);
                // self is an instance of a Python sub-class
                int index = objclass.getSubclassIndex(self.getClass());
                return wrapped[index];
            }
        }
    }
}
