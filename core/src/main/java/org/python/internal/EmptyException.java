// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.internal;

/**
 * The type of exception thrown by invoking an "empty"
 * {@code MethodHandle} in the runtime system. By convention, we
 * initialise undefined method handles to a handle that throws this
 * exception. We may then invoke them without checking for {@code null},
 * as long as we are prepared to catch {@code EmptyException}.
 * <p>
 * The exception is "lightweight" (it comes with no message or stack
 * trace) so it must be caught close enough to the invocation that we
 * can still identify the cause.
 */
public class EmptyException extends Exception {
    /**
     * Constructor for (a small number of) anonymous instances. Suppression
     * and stack trace are disabled since this is nearly a singleton.
     */
    public EmptyException() { super(null, null, false, false); }

    private static final long serialVersionUID = 1L;
}
