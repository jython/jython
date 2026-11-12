// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.internal;

import java.lang.reflect.Array;

import org.python.support.InterpreterError;

/**
 * Convenient constants etc. for use across the implementation and not
 * needing the type system to be working.
 */
public class Util {
    /** An empty array of objects. */
    public static final Object[] EMPTY_ARRAY = new Object[0];
    /** An empty array of String. */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    /** Single re-used instance of {@code EmptyException}. */
    public static final EmptyException EMPTY_EXCEPTION = new EmptyException();

    /**
     * Return a new array in which a given element is placed first, followed
     * by the elements of an existing array.
     *
     * @param <T> Element type of the arrays
     * @param a0 first element of new array
     * @param a1plus rest of elements in new array
     * @return new copy array with first element inserted
     */
    public static <T> T[] prepend(T a0, T[] a1plus) {
        int n = a1plus.length;
        Class<?> elemType = a1plus.getClass().getComponentType();
        @SuppressWarnings("unchecked")
        T[] a = elemType == Object.class ? (T[])new Object[1 + n]
                : (T[])Array.newInstance(elemType, 1 + n);
        a[0] = a0;
        System.arraycopy(a1plus, 0, a, 1, n);
        return a;
    }

    /**
     * Convert any {@code Throwable} except an {@code Error} to a
     * {@code RuntimeException}, as by {@link asUnchecked asUnchecked(t,
     * ...)} with a default message.
     *
     * @param t to propagate or encapsulate
     * @return run-time exception to throw
     */
    public static RuntimeException asUnchecked(Throwable t) {
        return Util.asUnchecked(t, "non-Python Exception");
    }

    /**
     * Convert any {@code Throwable} except an {@code Error} to a
     * {@code RuntimeException}, so that (if not already) it becomes an
     * unchecked exception. An {@code Error} is re-thrown directly. We use
     * this in circumstances where a method cannot be declared to throw the
     * exceptions that methods within it are declared to throw, and no
     * specific handling is available locally.
     * <p>
     * In particular, we use it where a call is made to
     * {@code MethodHandle.invokeExact}. That is declared to throw
     * {@code Throwable}, but we know that the {@code Throwable} will either
     * be a {@code PyException} or will signify an interpreter error that
     * the local code cannot be expected to handle.
     *
     * @param t to propagate or encapsulate
     * @param during format string for detail message, typically like
     *     "during map.get(%.50s)" where {@code args} contains the key.
     * @param args to insert into format string.
     * @return run-time exception to throw
     */
    public static RuntimeException asUnchecked(Throwable t, String during, Object... args) {
        if (t instanceof RuntimeException)
            return (RuntimeException)t;
        else if (t instanceof Error)
            throw (Error)t;
        else
            return new InterpreterError(t, during, args);
    }
}
