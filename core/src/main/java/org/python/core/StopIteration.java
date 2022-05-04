package org.python.core;

import java.lang.invoke.MethodHandles;

/** The Python {@code StopIteration} exception. */
public class StopIteration extends PyException {
    private static final long serialVersionUID = 1L;

    /** The type of Python object this class implements. */
    public static final PyType TYPE = PyType.fromSpec(
            new PyType.Spec("StopIteration", MethodHandles.lookup())
                    .base(PyException.TYPE));

    /**
     * Constructor for sub-class use specifying {@link #type}.
     *
     * @param type object being constructed
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    protected StopIteration(PyType type, String msg, Object... args) {
        super(type, msg, args);
    }

    /**
     * Constructor specifying a message.
     *
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    public StopIteration(String msg, Object... args) {
        this(TYPE, msg, args);
    }

    /**
     * Constructor specifying no arguments.
     */
    public StopIteration() { this(TYPE, ""); }
}
