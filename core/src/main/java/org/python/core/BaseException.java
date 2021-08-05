// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

/** The Python {@code BaseException} exception. */
class BaseException extends RuntimeException implements CraftedPyObject {
    private static final long serialVersionUID = 1L;

    /** The type of Python object this class implements. */
    static final PyType TYPE = PyType.fromSpec( //
            new PyType.Spec("BaseException", MethodHandles.lookup()));
    private final PyType type;
    final Object[] args;

    @Override
    public PyType getType() { return type; }

    /**
     * Constructor for sub-class use specifying {@link #type}. The message
     * {@code msg} is a Java format string in which the constructor arguments
     * {@code args} are used to fill the place holders. The formatted message is the
     * exception message from the Java point of view.
     * <p>
     * From a Python perspective, the tuple ({@code exception.args}) has one
     * element, the formatted message, or zero elements if the message is zero
     * length.
     *
     * @param type object being constructed
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    protected BaseException(PyType type, String msg, Object... args) {
        super(String.format(msg, args));
        this.type = type;
        msg = this.getMessage();
        this.args = msg.length() > 0 ? new Object[] {msg} : Py.EMPTY_ARRAY;
    }

    /**
     * Constructor specifying a message.
     *
     * @param msg a Java format string for the message
     * @param args to insert in the format string
     */
    public BaseException(String msg, Object... args) { this(TYPE, msg, args); }

    @Override
    public String toString() {
        String msg = args.length > 0 ? args[0].toString() : "";
        return String.format("%s: %s", getType().name, msg);
    }

    // slot functions -------------------------------------------------

    protected Object __repr__() {
        // Somewhat simplified
        return getType().name + "('" + getMessage() + "')";
    }
}
