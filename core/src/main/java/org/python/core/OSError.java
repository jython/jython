package org.python.core;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** The Python {@code OSError} exception. */
public class OSError extends PyException {
    private static final long serialVersionUID = 1L;

    /** The type object of Python {@code OSError} exceptions. */
    @SuppressWarnings("hiding")
    public static final PyType TYPE = PyType.fromSpec(
            new PyType.Spec("OSError", MethodHandles.lookup())
                    .base(PyException.TYPE));

    /**
     * Constructor for sub-class use specifying {@link #type}.
     *
     * @param type object being constructed
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    protected OSError(PyType type, String msg, Object... args) {
        super(type, msg, args);
    }

    /**
     * Constructor specifying a message.
     *
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    public OSError(String msg, Object... args) {
        this(TYPE, msg, args);
    }

    /**
     * Constructor based on the Java exception.
     *
     * @param ioe the Java exception
     */
    public OSError(IOException ioe) {
        this(TYPE, ioe.getMessage());
    }


    // Full fat constructor from *Python* is:
    // OSError(errno, strerror[, filename[, winerror[, filename2]]])
    // producing:
    // OSError: [WinError 999] strerror: 'filename' -> 'filename2'
}
