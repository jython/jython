// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal error thrown when the Python implementation cannot be relied
 * on to work. A Python {@code exception} (that might be caught in
 * Python code) is not then appropriate. An {@code InterpreterError} is
 * typically thrown during initialisation or for irrecoverable internal
 * errors.
 */
public class InterpreterError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Logger for interpreter errors. It may seem odd to log the raising of
     * a low-level exception, but a proportion of these are thrown during
     * the initialisation of the run-time system. Somehow they are swallowed
     * without trace: this gives us a second chance to notice.
     */
    static final Logger logger = LoggerFactory.getLogger(InterpreterError.class);

    /**
     * Constructor specifying a message.
     *
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    public InterpreterError(String msg, Object... args) {
        super(String.format(msg, args));
        logger.atInfo().log(getMessage());
    }

    /**
     * Constructor specifying a cause and a message.
     *
     * @param cause a Java exception behind the interpreter error
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    public InterpreterError(Throwable cause, String msg, Object... args) {
        super(String.format(msg, args), cause);
        logger.atInfo().log(getMessage());
        logger.atInfo().log(cause.getMessage());
    }

    /**
     * Constructor specifying a cause.
     *
     * @param cause a Java exception behind the interpreter error
     */
    public InterpreterError(Throwable cause) {
        this(cause, notNull(cause.getMessage(), "(no message)"));
    }

    /**
     * @param msg a string or {@code null}
     * @param defaultMsg a string or {@code null}
     * @return non-{@code null} {@code msg} or ""
     */
    private static String notNull(String msg, String defaultMsg) {
        return msg != null ? msg : defaultMsg;
    }
}
