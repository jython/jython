package org.python.core;

import java.lang.invoke.MethodHandle;
import java.util.Map;

import org.python.core.Slot.Signature;

/**
 * The Python {@code object} object: all Python objects by default inherit its
 * Python method implementations. The canonical implementation of {@code object}
 * is {@code Object}. This class provides its Python behaviours.
 * <p>
 * Although all Python objects are sub-classes in Python of {@code object},
 * their implementation classes will not generally be sub-classes in Java of
 * this one. In particular, many built-in types are not.
 * <p>
 * The Java implementation class of a type defined in Python will be derived
 * from the canonical implementation class of the "solid base" it inherits in
 * Python. This <i>may</i> be {@code object}, in which case the implementation
 * class will be a sub-class in Java of this class.
 * <p>
 *
 * @implNote All exposed methods, special methods and attribute get, set and
 *     delete methods defined here must be declared {@code static} in Java, with
 *     an explicit {@code Object self} argument. ({@code __new__} is excepted
 *     from this rule as it is {@code static} anyway.) This is so that methods
 *     defined here on {@code object} operate correctly on receiving Python
 *     objects whatever their Java class. Methods and fields must be package
 *     visible so that {@link PyType} is able to form {@code MethodHandle}s to
 *     them using its default lookup object.
 */
class PyBaseObject extends AbstractPyObject {

    /** The type object of {@code object} objects. */
    static final PyType TYPE = PyType.OBJECT_TYPE;

    /**
     * Constructor for Python sub-class specifying {@code type}.
     *
     * @param type actual Python sub-class being created
     */
    protected PyBaseObject(PyType type) { super(type); }

    /** Constructor for {@code object}. */
    public PyBaseObject() { super(TYPE); }


    // Special methods ------------------------------------------------

    /*
     * Methods must be static with a "self" argument of type Object so that method
     * handles copied from the slots of "object" function correctly in the type
     * slots of Python objects.
     *
     * It follows that operations performed here must be feasible for any Python
     * object.
     */

    /**
     * {@link Slot#op_repr} has signature {@link Signature#UNARY} and sometimes
     * reproduces the source-code representation of the object.
     *
     * @param self target of the operation
     * @return string form
     */
    // Compare CPython object_repr in typeobject.c
    static Object __repr__(Object self) { return "<" + PyObjectUtil.toAt(self) + ">"; }

    /**
     * {@link Slot#op_str} has signature {@link Signature#UNARY} and returns a
     * human-readable presentation of the object. The default definition of the
     * {@code __str__} slot is to invoke the {@code __repr__} slot.
     *
     * @param self target of the operation
     * @return string form
     */
    // Compare CPython object_str in typeobject.c
    static Object __str__(Object self) {
        MethodHandle repr = Operations.of(self).op_repr;
        // Be more bullet-proof than usual
        try {
            if (repr != null)
                return repr.invokeExact(self);
        } catch (Throwable e) {}
        // Fall back on a direct call
        return __repr__(self);
    }

    /**
     * {@link Slot#op_getattribute} has signature {@link Signature#GETATTR} and
     * provides attribute read access on the object and its type. The default
     * instance {@code __getattribute__} slot implements dictionary look-up on the
     * type and the instance. It is the starting point for activating the descriptor
     * protocol. The following order of precedence applies when looking for the
     * value of an attribute:
     * <ol>
     * <li>a data descriptor from the dictionary of the type</li>
     * <li>a value in the instance dictionary of {@code obj}</li>
     * <li>a non-data descriptor from dictionary of the type</li>
     * <li>a value from the dictionary of the type</li>
     * </ol>
     * If a matching entry on the type is a data descriptor (case 1), but throws
     * {@link AttributeError}, the instance dictionary (if any) will be consulted,
     * and the subsequent cases (3 and 4) skipped. A non-data descriptor that throws
     * an {@link AttributeError} (case 3) causes case 4 to be skipped.
     *
     * @param obj the target of the get
     * @param name of the attribute
     * @return attribute value
     * @throws AttributeError if no such attribute
     * @throws Throwable on other errors, typically from the descriptor
     */
    // Compare CPython PyObject_GenericGetAttr in object.c
    // Also _PyObject_GenericGetAttrWithDict without the tricks.
    static Object __getattribute__(Object obj, String name) throws AttributeError, Throwable {

        PyType objType = PyType.of(obj);
        MethodHandle descrGet = null;

        // Look up the name in the type (null if not found).
        Object typeAttr = objType.lookup(name);
        if (typeAttr != null) {
            // Found in the type, it might be a descriptor
            Operations typeAttrOps = Operations.of(typeAttr);
            descrGet = typeAttrOps.op_get;
            if (typeAttrOps.isDataDescr()) {
                // typeAttr is a data descriptor so call its __get__.
                try {
                    return descrGet.invokeExact(typeAttr, obj, objType);
                } catch (Slot.EmptyException e) {
                    /*
                     * Only __set__ or __delete__ was defined. We do not catch AttributeError: it's
                     * definitive. Suppress trying __get__ again.
                     */
                    descrGet = null;
                }
            }
        }

        /*
         * At this stage: typeAttr is the value from the type, or a non-data descriptor,
         * or null if the attribute was not found. It's time to give the object instance
         * dictionary a chance.
         */
        if (obj instanceof DictPyObject) {
            Map<Object, Object> d = ((DictPyObject) obj).getDict();
            Object instanceAttr = d.get(name);
            if (instanceAttr != null) {
                // Found something
                return instanceAttr;
            }
        }

        /*
         * The name wasn't in the instance dictionary (or there wasn't an instance
         * dictionary). We are now left with the results of look-up on the type.
         */
        if (descrGet != null) {
            // typeAttr may be a non-data descriptor: call __get__.
            try {
                return descrGet.invokeExact(typeAttr, obj, objType);
            } catch (Slot.EmptyException e) {}
        }

        if (typeAttr != null) {
            /*
             * The attribute obtained from the meta-type, and that turned out not to be a
             * descriptor, is the return value.
             */
            return typeAttr;
        }

        // All the look-ups and descriptors came to nothing :(
        throw Abstract.noAttributeError(obj, name);
    }

    /**
     * {@link Slot#op_setattr} has signature {@link Signature#SETATTR} and provides
     * attribute write access on the object. The default instance
     * {@code __setattr__} slot implements dictionary look-up on the type and the
     * instance. It is the starting point for activating the descriptor protocol.
     * The following order of precedence applies when setting the value of an
     * attribute:
     * <ol>
     * <li>call a data descriptor from the dictionary of the type</li>
     * <li>place a value in the instance dictionary of {@code obj}</li>
     * </ol>
     * If a matching entry on the type is a data descriptor (case 1) , but it throws
     * {@link AttributeError}, this is definitive and the instance dictionary (if
     * any) will not be updated.
     *
     * @param obj the target of the set
     * @param name of the attribute
     * @param value to give the attribute
     * @throws AttributeError if no such attribute or it is read-only
     * @throws Throwable on other errors, typically from the descriptor
     */
    // Compare CPython PyObject_GenericSetAttr in object.c
    // Also _PyObject_GenericSetAttrWithDict without the extras.
    static void __setattr__(Object obj, String name, Object value)
            throws AttributeError, Throwable {

        // Accommodate CPython idiom that set null means delete.
        if (value == null) {
            // Do this to help porting. Really this is an error.
            __delattr__(obj, name);
            return;
        }

        // Look up the name in the type (null if not found).
        Object typeAttr = PyType.of(obj).lookup(name);
        if (typeAttr != null) {
            // Found in the type, it might be a descriptor.
            Operations typeAttrOps = Operations.of(typeAttr);
            if (typeAttrOps.isDataDescr()) {
                // Try descriptor __set__
                try {
                    typeAttrOps.op_set.invokeExact(typeAttr, obj, value);
                    return;
                } catch (Slot.EmptyException e) {
                    // We do not catch AttributeError: it's definitive.
                    // Descriptor but no __set__: do not fall through.
                    throw Abstract.readonlyAttributeError(obj, name);
                }
            }
        }

        /*
         * There was no data descriptor, so we will place the value in the object
         * instance dictionary directly.
         */
        if (obj instanceof DictPyObject) {
            Map<Object, Object> d = ((DictPyObject) obj).getDict();
            try {
                // There is a dictionary, and this is a put.
                d.put(name, value);
            } catch (UnsupportedOperationException e) {
                // But the dictionary is unmodifiable
                throw Abstract.cantSetAttributeError(obj);
            }
        } else {
            // Object has no dictionary (and won't support one).
            if (typeAttr == null) {
                // Neither had the type an entry for the name.
                throw Abstract.noAttributeError(obj, name);
            } else {
                /*
                 * The type had either a value for the attribute or a non-data descriptor.
                 * Either way, it's read-only when accessed via the instance.
                 */
                throw Abstract.readonlyAttributeError(obj, name);
            }
        }
    }

    /**
     * {@link Slot#op_delattr} has signature {@link Signature#DELATTR} and provides
     * attribute deletion on the object. The default instance {@code __delattr__}
     * slot implements dictionary look-up on the type and the instance. It is the
     * starting point for activating the descriptor protocol. The following order of
     * precedence applies when setting the value of an attribute:
     * <ol>
     * <li>call a data descriptor from the dictionary of the type</li>
     * <li>remove an entry from the instance dictionary of {@code obj}</li>
     * </ol>
     * If a matching entry on the type is a data descriptor (case 1) , but it throws
     * {@link AttributeError}, this is definitive and the instance dictionary (if
     * any) will not be updated.
     *
     * @param obj the target of the delete
     * @param name of the attribute
     * @throws AttributeError if no such attribute or it is read-only
     * @throws Throwable on other errors, typically from the descriptor
     */
    // Compare CPython PyObject_GenericSetAttr in object.c
    static void __delattr__(Object obj, String name) throws AttributeError, Throwable {

        // Look up the name in the type (null if not found).
        Object typeAttr = PyType.of(obj).lookup(name);
        if (typeAttr != null) {
            // Found in the type, it might be a descriptor.
            Operations typeAttrOps = Operations.of(typeAttr);
            if (typeAttrOps.isDataDescr()) {
                // Try descriptor __delete__
                try {
                    typeAttrOps.op_delete.invokeExact(typeAttr, obj);
                    return;
                } catch (Slot.EmptyException e) {
                    // We do not catch AttributeError: it's definitive.
                    // Data descriptor but no __delete__.
                    throw Abstract.mandatoryAttributeError(obj, name);
                }
            }
        }

        /*
         * There was no data descriptor, so we will remove the name from the object
         * instance dictionary directly.
         */
        if (obj instanceof DictPyObject) {
            Map<Object, Object> d = ((DictPyObject) obj).getDict();
            try {
                // There is a dictionary, and this is a delete.
                Object previous = d.remove(name);
                if (previous == null) {
                    // A null return implies it didn't exist
                    throw Abstract.noAttributeError(obj, name);
                }
            } catch (UnsupportedOperationException e) {
                // But the dictionary is unmodifiable
                throw Abstract.cantSetAttributeError(obj);
            }
        } else {
            // Object has no dictionary (and won't support one).
            if (typeAttr == null) {
                // Neither has the type an entry for the name.
                throw Abstract.noAttributeError(obj, name);
            } else {
                /*
                 * The type had either a value for the attribute or a non-data descriptor.
                 * Either way, it's read-only when accessed via the instance.
                 */
                throw Abstract.readonlyAttributeError(obj, name);
            }
        }
    }

}
