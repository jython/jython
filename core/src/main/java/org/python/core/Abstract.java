package org.python.core;

import org.python.base.InterpreterError;

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

    private static final String IS_REQUIRED_NOT =
            "%.200s is required, not '%.100s'";
    private static final String RETURNED_NON_TYPE =
            "%.200s returned non-%.200s (type %.200s)";
    private static final String ARGUMENT_MUST_BE =
            "%s()%s argument must be %s, not '%.200s'";

    /**
     * Create a {@link TypeError} with a message involving the type of
     * {@code o} and optionally other arguments.
     *
     * @param fmt format for message with at least one {@code %s}
     * @param o object whose type name will fill the first {@code %s}
     * @param args extra arguments to the formatted message
     * @return exception to throw
     */
    static TypeError typeError(String fmt, Object o, Object... args) {
        return new TypeError(fmt, PyType.of(o).getName(), args);
    }

    /**
     * Create a {@link TypeError} with a message along the lines "T is
     * required, not X" involving any descriptive phrase T and the type
     * X of {@code o}, e.g. "<u>a bytes-like object</u> is required, not
     * '<u>str</u>'".
     *
     * @param t expected kind of thing
     * @param o actual object involved
     * @return exception to throw
     */
    static TypeError requiredTypeError(String t, Object o) {
        return new TypeError(IS_REQUIRED_NOT, t,
                PyType.of(o).getName());
    }

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
     * Create a {@link TypeError} with a message along the lines "F()
     * [nth] argument must be T, not X", involving a function name,
     * optionally an ordinal n, an expected type description T and the type X of
     * {@code o}, e.g. "int() argument must be a string, a bytes-like
     * object or a number, not 'list'" or "complex() second argument
     * must be a number, not 'type'".
     *
     * @param f name of function or operation
     * @param n ordinal of argument: 1 for "first", etc., 0 for ""
     * @param t describing the expected kind of argument
     * @param o actual argument (not its type)
     * @return exception to throw
     */
    static TypeError argumentTypeError(String f, int n, String t,
            Object o) {
        return new TypeError(ARGUMENT_MUST_BE, f, ordinal(n), t,
                PyType.of(o).getName());
    }

    // Helper for argumentTypeError
    private static String ordinal(int n) {
        switch (n) {
            case 0:
                return "";
            case 1:
                return " first";
            case 2:
                return " second";
            case 3:
                return " third";
            default:
                return String.format(" %dth", n);
        }
    }

    /**
     * Create a {@link TypeError} with a message along the lines "F
     * returned non-T (type X)" involving a function name, an expected
     * type T and the type X of {@code o}, e.g. "__int__ returned
     * non-int (type str)".
     *
     * @param f name of function or operation
     * @param t expected type of return
     * @param o actual object returned
     * @return exception to throw
     */
    static TypeError returnTypeError(String f, String t, Object o) {
        return new TypeError(RETURNED_NON_TYPE, f, t,
                PyType.of(o).getName());
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

    /**
     * Submit a {@link DeprecationWarning} call (which may result in an
     * exception) with the same message as
     * {@link #returnTypeError(String, String, Object)}, the whole
     * followed by one about deprecation of the facility.
     *
     * @param f name of function or operation
     * @param t expected type of return
     * @param o actual object returned
     * @return {@code o}
     */
    static Object returnDeprecation(String f, String t, Object o) {
        // Warnings.format(DeprecationWarning.TYPE, 1,
        // RETURNED_NON_TYPE_DEPRECATION, f, t,
        // PyType.of(o).getName(), t);
        return o;
    }

    private static final String RETURNED_NON_TYPE_DEPRECATION =
            RETURNED_NON_TYPE + ".  "
                    + "The ability to return an instance of a strict "
                    + "subclass of %s is deprecated, and may be "
                    + "removed in a future version of Python.";

    /**
     * Create an {@link InterpreterError} for use where a Python method
     * (or special method) implementation receives an argument that
     * should be impossible in a correct interpreter. This is a sort of
     * {@link TypeError} against the {@code self} argument, but
     * occurring where no programming error should be able to induce it
     * (e.g. coercion fails after we have passed the check that
     * descriptors make on their {@code obj}, or when invoking a special
     * method found via an {@link Operations} object.
     *
     * @param d expected kind of argument
     * @param o actual argument (not its type)
     * @return exception to throw
     */
    static InterpreterError impossibleArgumentError(String d, Object o) {
        return new InterpreterError(IMPOSSIBLE_CLASS, d, o.getClass().getName());
    }

    private static final String IMPOSSIBLE_CLASS =
            "expected %.50s argument but found impossible Java class %s";
}
