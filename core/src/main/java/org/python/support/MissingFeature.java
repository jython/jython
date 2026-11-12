// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.support;

/**
 * Thrown when we reach a combination of circumstances in the
 * interpreter that may arise from legitimate use, but we aren't ready
 * to implement it.
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
        super(String.format("Missing feature: " + msg, args));
    }
}
