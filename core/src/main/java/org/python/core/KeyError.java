package org.python.core;

import java.lang.invoke.MethodHandles;

/** The Python {@code KeyError} exception. */
public class KeyError extends PyException {
    private static final long serialVersionUID = 1L;

    /** The type object of Python {@code KeyError} exceptions. */
    @SuppressWarnings("hiding")
    public static final PyType TYPE = PyType
            .fromSpec(new PyType.Spec("KeyError", MethodHandles.lookup()).base(LookupError.TYPE));

    /** The problematic key */
    final Object key;

    /**
     * Constructor for sub-class use specifying {@link #type}.
     *
     * @param key causing the problem
     * @param type of object being constructed
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    protected KeyError(Object key, PyType type, String msg, Object... args) {
        super(type, msg, args);
        this.key = key;
    }

    /**
     * Constructor specifying a key and a message. A Java String form of
     * the key will be the first argument formatted
     *
     * @param key causing the problem
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    public KeyError(Object key, String msg, Object... args) {
        this(key, TYPE, msg, key.toString(), args);
    }

    /**
     * A Python {@link KeyError} when the problem is a duplicate key.
     * (This is the same Python type, but Java can catch it as a
     * distinct type.)
     */
    public static class Duplicate extends KeyError {
        private static final long serialVersionUID = 1L;

        public Duplicate(Object key) { super(key, "duplicate key %s", key.toString()); }
    }
}
