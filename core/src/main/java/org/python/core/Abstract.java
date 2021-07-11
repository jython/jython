package org.python.core;

/**
 * The "abstract interface" to operations on Python objects. Methods here
 * execute the slot functions of the type definition of the objects passed in. A
 * primary application is to the CPython byte code interpreter. (Methods here
 * often correspond closely to a CPython opcode.)
 * <p>
 * In CPython, the methods are found in {@code Objects/abstract.c}
 */
public class Abstract {

    /**
     * There are only static methods here, so no instances should be created.
     * Formally make the constructor {@code protected} so we can sub-class.
     * (Otherwise {@code private} would be the right choice.)
     */
    protected Abstract() {}

    // Convenience functions constructing errors --------------------

    /**
     * Create a {@link TypeError} with a message along the lines "can't set
     * attributes of X" giving str of {@code name}.
     *
     * @param obj actual object on which setting failed
     * @return exception to throw
     */
    static TypeError cantSetAttributeError(Object obj) {
        return new TypeError("can't set attributes of %.200s", obj);
    }

    /**
     * Create a {@link AttributeError} with a message along the lines "'T' object
     * has no attribute N", where T is the type of the object accessed.
     *
     * @param v object accessed
     * @param name of attribute
     * @return exception to throw
     */
    static AttributeError noAttributeError(Object v, Object name) {
        return noAttributeOnType(PyType.of(v), name);
    }

    /**
     * Create a {@link AttributeError} with a message along the lines "'T' object
     * has no attribute N", where T is the type given.
     *
     * @param type of object accessed
     * @param name of attribute
     * @return exception to throw
     */
    static AttributeError noAttributeOnType(PyType type, Object name) {
        String fmt = "'%.50s' object has no attribute '%.50s'";
        return new AttributeError(fmt, type.getName(), name);
    }

    /**
     * Create a {@link AttributeError} with a message along the lines "'T' object
     * attribute N is read-only", where T is the type of the object accessed.
     *
     * @param v object accessed
     * @param name of attribute
     * @return exception to throw
     */
    static AttributeError readonlyAttributeError(Object v, Object name) {
        return readonlyAttributeOnType(PyType.of(v), name);
    }

    /**
     * Create a {@link AttributeError} with a message along the lines "'T' object
     * attribute N is read-only", where T is the type given.
     *
     * @param type of object accessed
     * @param name of attribute
     * @return exception to throw
     */
    static AttributeError readonlyAttributeOnType(PyType type, Object name) {
        String fmt = "'%.50s' object attribute '%s' is read-only";
        return new AttributeError(fmt, type.getName(), name);
    }

    /**
     * Create a {@link AttributeError} with a message along the lines "'T' object
     * attribute N cannot be deleted", where T is the type of the object accessed.
     *
     * @param v object accessed
     * @param name of attribute
     * @return exception to throw
     */
    static AttributeError mandatoryAttributeError(Object v, Object name) {
        return mandatoryAttributeOnType(PyType.of(v), name);
    }

    /**
     * Create a {@link AttributeError} with a message along the lines "'T' object
     * attribute N cannot be deleted", where T is the type given.
     *
     * @param type of object accessed
     * @param name of attribute
     * @return exception to throw
     */
    static AttributeError mandatoryAttributeOnType(PyType type, Object name) {
        String fmt = "'%.50s' object attribute '%s' cannot be deleted";
        return new AttributeError(fmt, type.getName(), name);
    }

}
