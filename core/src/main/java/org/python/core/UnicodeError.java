package org.python.core;

import java.lang.invoke.MethodHandles;

/** The Python {@code UnicodeError} exception. */
class UnicodeError extends ValueError {

    private static final long serialVersionUID = 1L;

    /** The type of Python object this class implements. */
    static final PyType TYPE = PyType.fromSpec(
            new PyType.Spec("UnicodeError", MethodHandles.lookup()).base(PyException.TYPE));

    /*
     * PyUnicodeError should have 5 exposed attributes, although they
     * are only set by its sub-classes. See where CPython exceptions.c
     * defines UnicodeEncodeError, UnicodeDecodeError and
     * UnicodeTranslateError.
     */

    /**
     * Constructor for sub-class use specifying {@link #type}.
     *
     * @param type object being constructed
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    protected UnicodeError(PyType type, String msg, Object... args) { super(type, msg, args); }

    /**
     * Constructor specifying a message.
     *
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    public UnicodeError(String msg, Object... args) { this(TYPE, msg, args); }
}
