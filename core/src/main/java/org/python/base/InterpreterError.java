package org.python.base;

/**
 * Internal error thrown when the Python implementation cannot be relied on to
 * work. A Python exception (a {@code PyObject} that might be caught in Python
 * code) is not then appropriate. Typically thrown during initialisation or for
 * irrecoverable internal errors.
 */
public class InterpreterError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor specifying a message.
     *
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    public InterpreterError(String msg, Object... args) { super(String.format(msg, args)); }

    /**
     * Constructor specifying a cause and a message.
     *
     * @param cause a Java exception behind the interpreter error
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    public InterpreterError(Throwable cause, String msg, Object... args) {
        super(String.format(msg, args), cause);
    }

}
