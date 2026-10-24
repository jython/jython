// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.python.support.InterpreterError;

/**
 * Some logic common to specifications for the Python types and Java
 * representation classes.
 */
public abstract class NamedSpec {

    /** Unmodifiable empty list (or sentinel for "never assigned"). */
    protected static final List<Class<?>> EMPTY =
            // Not List.of() because identity of object is significant.
            Collections.unmodifiableList(new ArrayList<>());
    /** Unmodifiable empty list (or sentinel for "never assigned"). */
    protected static final List<String> NONAMES =
            // Not List.of() because identity of object is significant.
            Collections.unmodifiableList(new ArrayList<>());

    /** Simple name of the representation class. */
    protected String name;

    /**
     * Prevent further change and make some validity checks.
     *
     * @return {@code this}
     */
    protected abstract NamedSpec freeze();

    /** Whether further change is allowed. */
    protected boolean frozen;

    /**
     * Begin specification with a name.
     *
     * @param name of specified object
     */
    protected NamedSpec(String name) { this.name = name; }

    /**
     * Return name specified to constructor.
     *
     * @return name of specified object
     */
    public String getName() { return name; }

    /** Check that {@link #freeze()} has not yet been called. */
    protected void checkNotFrozen() {
        if (frozen) { specError("specification changed after frozen"); }
    }

    /**
     * Construct an {@link InterpreterError} along the lines "[err] while
     * defining '[name]'."
     *
     * @param err qualifying the error (a format string)
     * @param args to formatted message
     * @return to throw
     */
    protected InterpreterError specError(String err, Object... args) {
        StringBuilder sb = new StringBuilder(100);
        sb.append(String.format(err, args)).append(" (defining '").append(name).append("').");
        return new InterpreterError(sb.toString());
    }

    /**
     * Construct an {@link InterpreterError} along the lines "repeat [err]
     * while defining '[name]'."
     *
     * @param thing qualifying the error
     * @return to throw
     */
    protected InterpreterError repeatError(String thing) {
        return specError("repeat " + thing + " specified");
    }

    /**
     * Construct an {@link InterpreterError} along the lines "repeat
     * [method]('[n]') while defining '[name]'."
     *
     * @param method naming the method called
     * @param name being added
     * @return to throw
     */
    protected InterpreterError repeatError(String method, String name) {
        return specError("repeat %s(\"%s\") specified", method, name);
    }

    /**
     * Construct an {@link InterpreterError} along the lines "repeat
     * [method]([c]) while defining '[name]'."
     *
     * @param method naming the method called
     * @param c the class being added
     * @return to throw
     */
    protected InterpreterError repeatError(String method, Class<?> c) {
        return specError("repeat %s(%s) specified", method, c.getTypeName());
    }
}
