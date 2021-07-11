package org.python.base;

/**
 * Thrown when we reach a combination of circumstances in the
 * interpreter that may arise from legitimate use, but we aren't ready
 * to implement it.
 * <p>
 * What does the reference implementation do at this point?
 */
public class MissingFeature extends InterpreterError {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor specifying a message.
     *
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    public MissingFeature(String msg, Object... args) {
        super(String.format(msg, args));
    }
}
