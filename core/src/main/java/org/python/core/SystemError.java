package org.python.core;

import java.lang.invoke.MethodHandles;

/** The Python {@code SystemError} exception. */
public class SystemError extends PyException {
    private static final long serialVersionUID = 1L;

    /** The type object of Python {@code SystemError} exceptions. */
    @SuppressWarnings("hiding")
    public static final PyType TYPE = PyType.fromSpec(
            new PyType.Spec("SystemError", MethodHandles.lookup()).base(PyException.TYPE));

    /**
     * Constructor for sub-class use specifying {@link #type}.
     *
     * @param type object being constructed
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    protected SystemError(PyType type, String msg, Object... args) { super(type, msg, args); }

    /**
     * Constructor specifying a message.
     *
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    public SystemError(String msg, Object... args) { this(TYPE, msg, args); }
}
