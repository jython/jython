// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.python.support.JavaClassShorthand.O;
import static org.python.support.JavaClassShorthand.V;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

import org.python.internal.EmptyException;
import org.python.kernel.BaseType;
import org.python.kernel.Representation;
import org.python.support.InterpreterError;
import org.python.types.Feature;
import org.python.types.TypeSpec;
import org.python.types.Exposed.Deleter;
import org.python.types.Exposed.Getter;
import org.python.types.Exposed.Setter;

/**
 * Descriptor for an attribute that has been defined by a series of
 * {@link Getter}, {@link Setter} and {@link Deleter} annotations on
 * access methods defined in the object implementation to get, set or
 * delete the value. {@code PyGetSetDescr} differs from
 * {@link PyMemberDescr} in giving the author of an implementation class
 * the power (and responsibility) entirely to define the behaviour
 * corresponding to these actions.
 */
// Compare CPython struct PyGetSetDef in descrobject.h,
// and PyGetSetDescrObject also in descrobject.h
public abstract class PyGetSetDescr extends DataDescriptor {

    /** Only referenced during bootstrap by {@link TypeSystem}. */
    static class Spec {
        /** @return the type specification. */
        static TypeSpec get() {
            return new TypeSystem.BootstrapSpec("getset_descriptor", MethodHandles.lookup(),
                    PyGetSetDescr.class).remove(Feature.INSTANTIABLE);
        }
    }

    /**
     * Return the Python type of {@code getset_descriptor} objects,
     * {@code types.GetSetDescriptorType}.
     *
     * @return {@code <class 'getset_descriptor'>}
     */
    public static final PyType TYPE() { return TypeSystem.TYPE_getset_descriptor; }

    /** The method handle type (O)O. */
    // CPython: PyObject *(*getter)(PyObject *, void *)
    static final MethodType GETTER = MethodType.methodType(O, O);
    /** The method handle type (O,O)V. */
    // CPython: int (*setter)(PyObject *, PyObject *, void *)
    static final MethodType SETTER = MethodType.methodType(V, O, O);
    /** The method handle type (O)V. */
    static final MethodType DELETER = MethodType.methodType(V, O);

    /** A handle on {@link #emptyGetter(PyObject)} */
    private static MethodHandle EMPTY_GETTER;
    /** A handle on {@link #emptySetter(PyObject, PyObject)} */
    private static MethodHandle EMPTY_SETTER;
    /** A handle on {@link #emptyDeleter(PyObject)} */
    private static MethodHandle EMPTY_DELETER;
    /** Empty array of method handles */
    private static MethodHandle[] EMPTY_MH_ARRAY = new MethodHandle[0];

    static {
        /*
         * Initialise the empty method handles in a block since it can fail (in
         * theory).
         */
        try {
            Lookup LOOKUP = MethodHandles.lookup();
            EMPTY_GETTER = LOOKUP.findStatic(PyGetSetDescr.class, "emptyGetter", GETTER);
            EMPTY_SETTER = LOOKUP.findStatic(PyGetSetDescr.class, "emptySetter", SETTER);
            EMPTY_DELETER = LOOKUP.findStatic(PyGetSetDescr.class, "emptyDeleter", DELETER);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // This should never happen.
            throw new InterpreterError(e, "cannot find get-set empty* functions");
        }
    }

    /** Java class of attribute accepted by set method. */
    final Class<?> klass;

    /*
     * CPython has a void * argument to [gs]etter but no uses are found in
     * the CPython code base. Sub-classing may be the Java way to provide a
     * closure.
     */
    // void *closure;

    /**
     * Construct a descriptor that calls the access methods for get, set and
     * delete operations specified as method handles.
     *
     * @param objclass to which descriptor applies
     * @param name of attribute
     * @param doc documentation string (or {@code null})
     * @param klass Java class of attribute accepted by set method
     */
    // Compare CPython PyDescr_NewGetSet
    PyGetSetDescr(BaseType objclass, String name, String doc, Class<?> klass) {
        super(objclass, name, doc);
        this.klass = klass;
    }

    @Override
    public PyType getType() { return TYPE(); }

    /**
     * Return a {@code MethodHandle} by which the Java implementation of the
     * getter, corresponding to the given index in
     * {@link PyType#selfClasses()}, may be invoked.
     *
     * @param selfClassIndex specifying the Java class of {@code self}
     * @return corresponding handle (or {@code slot.getEmpty()})
     */
    abstract MethodHandle getWrappedGet(int selfClassIndex);

    /**
     * Return a {@code MethodHandle} by which the Java implementation of the
     * setter, corresponding to the given index in
     * {@link PyType#selfClasses()}, may be invoked.
     *
     * @param selfClassIndex specifying the Java class of {@code self}
     * @return corresponding handle (or {@code slot.getEmpty()})
     */
    abstract MethodHandle getWrappedSet(int selfClassIndex);

    /**
     * Return a {@code MethodHandle} by which the Java implementation of the
     * deleter, corresponding to the given index in
     * {@link PyType#selfClasses()}, may be invoked.
     *
     * @param selfClassIndex specifying the Java class of {@code self}
     * @return corresponding handle (or {@code slot.getEmpty()})
     */
    abstract MethodHandle getWrappedDelete(int selfClassIndex);

    /**
     * Return a {@code MethodHandle} contained in this descriptor,
     * applicable to the Java class of {@code obj} argument during a get.
     * <p>
     * The implementation must check the Python type of {@code obj} is a
     * subclass of the the type that defined the descriptor. Where the
     * defining type allows multiple (Java) self-classes, the implementation
     * must find the correct one an array.
     *
     * @param obj from which to get this attribute
     * @return corresponding handle (or one that throws
     *     {@link EmptyException})
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if
     *     {@code obj} is of unacceptable type
     */
    abstract MethodHandle getWrappedGet(Object obj);

    /**
     * Return a {@code MethodHandle} contained in this descriptor,
     * applicable to the Java class of {@code obj} argument during a set.
     * <p>
     * The implementation must check the Python type of {@code obj} is a
     * subclass of the the type that defined the descriptor. Where the
     * defining type allows multiple (Java) self-classes, the implementation
     * must find the correct one an array.
     *
     * @param obj on which to set this attribute
     * @return corresponding handle (or one that throws
     *     {@link EmptyException})
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if
     *     {@code obj} is of unacceptable type
     */
    abstract MethodHandle getWrappedSet(Object obj);

    /**
     * Return a {@code MethodHandle} contained in this descriptor,
     * applicable to the Java class of a {@code obj} argument during a
     * delete.
     * <p>
     * The implementation must check the Python type of {@code obj} is a
     * subclass of the the type that defined the descriptor. Where the
     * defining type allows multiple (Java) self-classes, the implementation
     * must find the correct one an array.
     *
     * @param obj from which to delete this attribute
     * @return corresponding handle (or one that throws
     *     {@link EmptyException})
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if
     *     {@code obj} is of unacceptable type
     */
    abstract MethodHandle getWrappedDelete(Object obj);

    /**
     * The attribute may not be set or deleted.
     *
     * @return true if the attribute may not be set or deleted
     */
    abstract boolean readonly();

    /**
     * The attribute may be deleted.
     *
     * @return true if the attribute may be deleted.
     */
    abstract boolean optional();

    // Special methods ------------------------------------------------

    // Compare CPython getset_repr in descrobject.c
    @SuppressWarnings("unused")
    private Object __repr__() { return descrRepr("attribute"); }

    /**
     * {@inheritDoc}
     *
     * If {@code obj != null} invoke {@code get} on it to return a value.
     * {@code obj} must be of type {@link #objclass}. A call made with
     * {@code obj == null} returns {@code this} descriptor.
     *
     * @param type is ignored
     */
    // Compare CPython getset_get in descrobject.c
    @Override
    public Object __get__(Object obj, PyType type) throws Throwable {
        if (obj == null)
            /*
             * obj==null indicates the descriptor was found on the target object
             * itself (or a base), see CPython type_getattro in typeobject.c
             */
            return this;
        else {
            try {
                check(obj);
                MethodHandle mh = getWrappedGet(obj);
                return mh.invokeExact(obj);
            } catch (EmptyException e) {
                throw cannotReadAttr();
            }
        }
    }

    // Compare CPython getset_set in descrobject.c
    @Override
    void __set__(Object obj, Object value) throws PyBaseException, Throwable {
        if (value == null) {
            // This ought to be an error, but allow for CPython idiom.
            __delete__(obj);
        } else {
            try {
                checkSet(obj);
                MethodHandle mh = getWrappedSet(obj);
                try {
                    mh.invokeExact(obj, value);
                } catch (ClassCastException e) {
                    /*
                     * A cast of 'value' to the argument type of the set method has failed
                     * (so not Object). The required class is hidden in the handle, but we
                     * wrote it in this.klass during exposure.
                     */
                    throw attrMustBe(klass, value);
                }
            } catch (EmptyException e) {
                throw cannotWriteAttr();
            }
        }
    }

    // Compare CPython getset_set in descrobject.c with NULL value
    @Override
    void __delete__(Object obj) throws PyBaseException, Throwable {
        try {
            checkDelete(obj);
            MethodHandle mh = getWrappedDelete(obj);
            mh.invokeExact(obj);
        } catch (EmptyException e) {
            throw readonly() ? cannotWriteAttr() : cannotDeleteAttr();
        }
    }

    // Implementations -----------------------------------------------

    /**
     * A {@link PyGetSetDescr} to use for a get-set attribute when the
     * owning Python type has just one accepted implementation.
     */
    static class Single extends PyGetSetDescr {

        /**
         * A handle on the getter defined by the unique implementation of
         * {@link Descriptor#objclass} for this attribute. The method type is
         * {@link #GETTER} = {@code (O)O}.
         */
        // CPython: PyObject *(*getter)(PyObject *, void *)
        // Compare CPython PyGetSetDef::get
        final MethodHandle get;  // MT = GETTER

        /**
         * A handle on the setter defined by the unique implementation of
         * {@link Descriptor#objclass} for this attribute. The method type is
         * {@link #SETTER} = {@code (O,O)V}.
         */
        // CPython: int (*setter)(PyObject *, PyObject *, void *)
        // Compare CPython PyGetSetDef::set
        final MethodHandle set;  // MT = SETTER

        /**
         * A handle on the deleter defined by the unique implementation of
         * {@link Descriptor#objclass} for this attribute. The method type is
         * {@link #DELETER} = {@code (O)V}.
         */
        // Compare CPython PyGetSetDef::set with null
        final MethodHandle delete;  // MT = DELETER

        /**
         * Construct a get-set descriptor, identifying by a method handle each
         * implementation method applicable to {@code objclass}. These methods
         * will be identified in an implementation by annotations
         * {@link Getter}, {@link Setter}, {@link Deleter}.
         *
         * @param objclass to which descriptor applies
         * @param name of attribute
         * @param get handle on getter method (or {@code null})
         * @param set handle on setter method (or {@code null})
         * @param delete handle on deleter method (or {@code null})
         * @param doc documentation string
         * @param klass Java class of attribute accepted by set method
         */
        // Compare CPython PyDescr_NewGetSet
        Single(BaseType objclass, String name, MethodHandle get, MethodHandle set,
                MethodHandle delete, String doc, Class<?> klass) {
            super(objclass, name, doc, klass);
            this.get = get != null ? get : EMPTY_GETTER;
            this.set = set != null ? set : EMPTY_SETTER;
            this.delete = delete != null ? delete : EMPTY_DELETER;
        }

        @Override
        MethodHandle getWrappedGet(int selfClassIndex) {
            assert selfClassIndex == 0;
            return get;
        }

        @Override
        MethodHandle getWrappedSet(int selfClassIndex) {
            assert selfClassIndex == 0;
            return set;
        }

        @Override
        MethodHandle getWrappedDelete(int selfClassIndex) {
            assert selfClassIndex == 0;
            return delete;
        }

        @Override
        MethodHandle getWrappedGet(Object obj) {
            // All single class descriptors simply return the handle.
            check(obj);
            return get;
        }

        @Override
        MethodHandle getWrappedSet(Object obj) {
            // All single class descriptors simply return the handle.
            check(obj);
            return set;
        }

        @Override
        MethodHandle getWrappedDelete(Object obj) {
            // All single class descriptors simply return the handle.
            check(obj);
            return delete;
        }

        @Override
        boolean readonly() { return set == EMPTY_SETTER; }

        @Override
        boolean optional() { return delete != EMPTY_DELETER; }
    }

    /**
     * A {@link PyGetSetDescr} to use for a get-set attribute when the
     * owning Python type has multiple accepted implementations.
     */
    static class Multiple extends PyGetSetDescr {

        /**
         * Handles for the particular implementations of the getter. The method
         * type of each is {@code (O)O}.
         */
        // Compare CPython PyGetSetDef::get
        protected final MethodHandle[] get;

        /**
         * Handles for the particular implementations of the setter. The method
         * type of each is {@code (O,O)V}.
         */
        // Compare CPython PyGetSetDef::set
        protected final MethodHandle[] set;

        /**
         * Handles for the particular implementations of the deleter. The method
         * type of each is {@code (O)V}.
         */
        // CPython uses PyGetSetDef::set
        protected final MethodHandle[] delete;

        /**
         * Construct a get-set descriptor, identifying by an array of method
         * handles the implementation methods applicable to {@code objclass}.
         * These methods will be identified in an implementation by annotations
         * {@link Getter}, {@link Setter}, {@link Deleter}.
         *
         * @param objclass to which descriptor applies
         * @param name of attribute
         * @param get operation
         * @param set operation
         * @param delete operation
         * @param doc documentation string
         * @param klass Java class of attribute accepted by set method
         */
        // Compare CPython PyDescr_NewGetSet
        Multiple(BaseType objclass, String name, MethodHandle[] get, MethodHandle[] set,
                MethodHandle delete[], String doc, Class<?> klass) {
            super(objclass, name, doc, klass);
            this.get = get;
            this.set = set != null ? set : EMPTY_MH_ARRAY;
            this.delete = delete != null ? delete : EMPTY_MH_ARRAY;
        }

        @Override
        MethodHandle getWrappedGet(int selfClassIndex) {
            try {
                return get[selfClassIndex];
            } catch (ArrayIndexOutOfBoundsException iobe) {
                // This will behave as an empty slot
                return EMPTY_GETTER;
            }
        }

        @Override
        MethodHandle getWrappedSet(int selfClassIndex) {
            try {
                return set[selfClassIndex];
            } catch (ArrayIndexOutOfBoundsException iobe) {
                // This will behave as an empty slot
                return EMPTY_SETTER;
            }
        }

        @Override
        MethodHandle getWrappedDelete(int selfClassIndex) {
            try {
                return delete[selfClassIndex];
            } catch (ArrayIndexOutOfBoundsException iobe) {
                // This will behave as an empty slot
                return EMPTY_DELETER;
            }
        }

        @Override
        MethodHandle getWrappedGet(Object obj) {
            // Work out how to call this descriptor on that object
            Class<?> objClass = obj.getClass();
            Representation rep = TypeSystem.registry.get(objClass);
            PyType objType = rep.pythonType(obj);
            try {
                if (objType == objclass) {
                    // objType defined the method so it must be ok
                    return get[rep.getIndex()];
                } else {
                    // Check validity at the Python level
                    checkPythonType(objType);
                    // obj is an instance of a Python sub-class
                    return get[objclass.getSubclassIndex(objClass)];
                }
            } catch (ArrayIndexOutOfBoundsException iobe) {
                // This will behave as an empty slot
                return EMPTY_GETTER;
            }
        }

        @Override
        MethodHandle getWrappedSet(Object obj) {
            // Work out how to call this descriptor on that object
            Class<?> objClass = obj.getClass();
            Representation rep = TypeSystem.registry.get(objClass);
            PyType objType = rep.pythonType(obj);
            try {
                if (objType == objclass) {
                    // objType defined the method so it must be ok
                    return set[rep.getIndex()];
                } else {
                    // Check validity at the Python level
                    checkPythonType(objType);
                    // obj is an instance of a Python sub-class
                    return set[objclass.getSubclassIndex(objClass)];
                }
            } catch (ArrayIndexOutOfBoundsException iobe) {
                // This will behave as an empty slot
                return EMPTY_SETTER;
            }
        }

        @Override
        MethodHandle getWrappedDelete(Object obj) {
            // Work out how to call this descriptor on that object
            Class<?> objClass = obj.getClass();
            Representation rep = TypeSystem.registry.get(objClass);
            PyType objType = rep.pythonType(obj);
            try {
                if (objType == objclass) {
                    // objType defined the method so it must be ok
                    return delete[rep.getIndex()];
                } else {
                    // Check validity at the Python level
                    checkPythonType(objType);
                    // obj is an instance of a Python sub-class
                    return delete[objclass.getSubclassIndex(objClass)];
                }
            } catch (ArrayIndexOutOfBoundsException iobe) {
                // This will behave as an empty slot
                return EMPTY_DELETER;
            }
        }

        @Override
        boolean readonly() { return set.length == 0; }

        @Override
        boolean optional() { return delete.length != 0; }
    }

    /**
     * Single instance of {@link EmptyException} that we throw when the set
     * or delete cannot be performed (read only attributes), or conceivably
     * if get cannot.
     */
    private static final EmptyException EMPTY = new EmptyException();

    /**
     * This method fills {@link #get} when the implementation leaves it
     * blank.
     *
     * @param ignored object to operate on
     * @return never
     * @throws EmptyException always
     */
    @SuppressWarnings("unused") // used reflectively
    private static Object emptyGetter(Object ignored) throws EmptyException { throw EMPTY; }

    /**
     * This method fills {@link #set} when the implementation leaves it
     * blank.
     *
     * @param ignored object to operate on
     * @param v ignored too
     * @throws EmptyException always
     */
    @SuppressWarnings("unused") // used reflectively
    private static void emptySetter(Object ignored, Object v) throws EmptyException { throw EMPTY; }

    /**
     * This method fills {@link #delete} when the implementation leaves it
     * blank.
     *
     * @param ignored object to operate on
     * @throws EmptyException always
     */
    @SuppressWarnings("unused") // used reflectively
    private static void emptyDeleter(Object ignored) throws EmptyException { throw EMPTY; }

    /**
     * A mapping from symbolic names for the types of method handle in a
     * {@code PyGetSetDescr} to other properties like the method handle
     * type.
     */
    // TODO This is little used: consider another way.
    enum Type {
        /** An implementation of {@code __get__} */
        Getter(PyGetSetDescr.GETTER),
        /** An implementation of {@code __set__} */
        Setter(PyGetSetDescr.SETTER),
        /** An implementation of {@code __delete__} */
        Deleter(PyGetSetDescr.DELETER);

        /** The {@code MethodType} we should expect. */
        final MethodType methodType;

        private Type(MethodType mt) { this.methodType = mt; }

        /**
         * Map the method handle type back to the {@code PyGetSetDescr.Type}
         * that has it or {@code null}.
         *
         * @param mt to match
         * @return matching type or {@code null}
         */
        static Type fromMethodType(MethodType mt) {
            for (Type t : Type.values()) { if (mt == t.methodType) { return t; } }
            return null;
        }
    }
}
