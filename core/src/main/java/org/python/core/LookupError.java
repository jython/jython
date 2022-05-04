package org.python.core;

import java.lang.invoke.MethodHandles;

/** The Python {@code LookupError} exception. */
public class LookupError extends PyException {
    private static final long serialVersionUID = 1L;

    /** The type object of Python {@code LookupError} exceptions. */
    @SuppressWarnings("hiding")
    static final PyType TYPE = PyType.fromSpec(
            new PyType.Spec("LookupError", MethodHandles.lookup()).base(PyException.TYPE));

    /**
     * Constructor for sub-class use specifying {@link #type}.
     *
     * @param type object being constructed
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    protected LookupError(PyType type, String msg, Object... args) { super(type, msg, args); }

    /**
     * Constructor specifying a message.
     *
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    public LookupError(String msg, Object... args) { this(TYPE, msg, args); }
}
