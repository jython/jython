// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.io.IOException;

import org.python.internal.Util;

/**
 * Convenience methods for creating and manipulating Python exceptions.
 */
// Compare CPython errors.c
/*
 * The class name was chosen to resemble the prefix used by CPython for
 * similar API functions PyErr_*, although much of that API is better
 * represented by methods on PyBaseException.
 */
public class PyErr {

    /**
     * Create a Python exception for the caller to throw, specifying the
     * type of exception, a format string and arguments to compose the
     * message (as in Java {@code String.format}, not quite as in the
     * CPython API)). Note that the exception will be created using only the
     * resulting {@code String} message as the argument. If the Python
     * constructor (__new__ and __init__) of the exception takes other
     * arguments, this call cannot be used to provide them. The first
     * argument is usually one of the type constants found in {@link PyExc}.
     *
     * @param excType exception type
     * @param formatStr format string (Java conventions)
     * @param vals to substitute in the format string
     * @return an exception to throw
     */
    // Compare CPython PyErr_Format in errors.c
    public static PyBaseException format(PyType excType, String formatStr, Object... vals) {
        String msg = vals.length == 0 ? formatStr : String.format(formatStr, vals);
        // XXX Should construct via the type so as to set members.
        try {
            return (PyBaseException)Callables.call(excType, msg);
        } catch (Throwable e) {
            throw Util.asUnchecked(e);
        }
    }

    /**
     * Create a Python exception for the caller to throw, specifically a
     * Python {@code OSError} subclass and Java {@code IOException}.
     *
     * @param excType exception type
     * @param ioe Java exception
     * @return an exception to throw
     */
    // Compare CPython PyErr_Format in errors.c
    public static PyOSError format(PyType excType, IOException ioe) {
        // TODO implement OSError properly after exceptions.c
        if (!excType.isSubTypeOf(PyExc.OSError)) { throw Abstract.typeError(OSERROR_REQUIRED); }
        try {
            return (PyOSError)Callables.call(excType, ioe);
        } catch (Throwable e) {
            throw Util.asUnchecked(e);
        }
    }

    private static final String OSERROR_REQUIRED =
            "OSError or a sub-class must be specified " + "when raising from a Java IOException";
}
