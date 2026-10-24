// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

import org.python.types.Exposed;
import org.python.types.Feature;
import org.python.types.TypeSpec;

/**
 * The Python {@code OSError} exception, and the same class also
 * represents a plethora of its Python subclasses.
 */
public class PyOSError extends PyBaseException {
    private static final long serialVersionUID = 1L;

    /** The type object of Python {@code OSError} exceptions. */
    public static final PyType TYPE =
            PyType.fromSpec(new TypeSpec("OSError", MethodHandles.lookup()) //
                    .base(Exception) //
                    .add(Feature.REPLACEABLE, Feature.IMMUTABLE) //
                    .doc("Base class for I/O related errors."));

    /** OS-specific error code. */
    @Exposed.Member
    private Object errno;

    /**
     * Constructor specifying Python type.
     *
     * @param type Python type of the exception
     * @param args positional arguments
     */
    public PyOSError(PyType type, PyTuple args) { super(type, args); }

    // Special methods -----------------------------------------------

    // TODO implement OSError properly after exceptions.c

    // Full fat constructor from *Python* is:
    // OSError(errno, strerror[, filename[, winerror[, filename2]]])
    // producing:
    // OSError: [WinError 999] strerror: 'filename' -> 'filename2'

    private static final ArgParser INIT_PARSER = ArgParser.fromSignature("__init__", "errno",
            "strerror", "filename", "winerror", "filename2", "*args");

    @Override
    void __init__(Object[] args, String[] kwds) {
        int nargs = args.length;
        if (nargs >= 2 && nargs <= 5) {
            Object[] frame = INIT_PARSER.parse(args, kwds);
            // frame =
            // [errno, strerror, filename, winerror, filename2, *args]
            errno = frame[0];
        }

    }
}
