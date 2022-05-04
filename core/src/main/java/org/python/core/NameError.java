package org.python.core;

import java.lang.invoke.MethodHandles;

/** The Python {@code NameError} exception. */
public class NameError extends PyException {
    private static final long serialVersionUID = 1L;

    /** The type object of Python {@code NameError} exceptions. */
    @SuppressWarnings("hiding")
    public static final PyType TYPE = PyType
            .fromSpec(new PyType.Spec("NameError", MethodHandles.lookup()).base(PyException.TYPE));

    /**
     * Constructor for sub-class use specifying {@link #type}.
     *
     * @param type object being constructed
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    protected NameError(PyType type, String msg, Object... args) { super(type, msg, args); }

    /**
     * Constructor specifying a message.
     *
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    public NameError(String msg, Object... args) { this(TYPE, msg, args); }
}
