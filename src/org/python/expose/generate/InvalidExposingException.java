package org.python.expose.generate;

/**
 * Indicates that something is invalid in an exposed type be it a conflict of
 * names, a missing annotation or some other such problem. The message on this
 * exception should be sufficient for an end user to track down where the
 * problem is occuring and fix it.
 */
public class InvalidExposingException extends RuntimeException {

    public InvalidExposingException(String msg) {
        super(msg);
    }

    public InvalidExposingException(String message, String method) {
        this(message + "[method=" + method + "]");
    }
}
