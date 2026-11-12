// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import org.python.types.TypeSpec;

/**
 * Python type objects for exceptions. The built-in Python exception
 * types form an extensive hierarchy, but are represented in Java by a
 * much smaller set of of Java exception classes that all extend
 * {@link PyBaseException}.
 * <p>
 * Type objects for the built-in Python exceptions are defined in the
 * class files of their Java representations, and referenced from this
 * class for notational simplicity. When it becomes necessary to create
 * (or raise) an exception, code will usually refer to one of the type
 * objects here.
 *
 * @implNote An extension or new built-in Python exception type, it will
 *     necessarily extend (in Java) a built-in Python exception. When
 *     implementing one, do not make static reference (e.g. in
 *     {@link TypeSpec#base(PyType)}) to the Python base type through
 *     the shorthands in this class, as that may create loop in the
 *     static initialisation.
 *     <p>
 *     Instead, follow the pattern in {@link PyNameError}, which
 *     references {@link PyBaseException#Exception} as a base, and
 *     creates its Python subclass {@code UnboundLocalError} also
 *     avoiding {@code PyExc}. The pattern prevents a loop that may
 *     leave static final references unexpectedly {@code null}, or not,
 *     depending on which object is touched first by an application.
 */
// Compare CPython exceptions.c
/*
 * We choose the class name, and type object names somewhat against
 * convention, so that a reference to the type object looks like its
 * name in the CPython codebase. E.g. PyExc.TypeError looks like
 * PyExc_TypeError.
 */
public class PyExc {
    private PyExc() {}

    /**
     * {@code BaseException} is the base type in Python of all exceptions
     * and is implemented by {@link PyBaseException}.
     */
    public static PyType BaseException = PyBaseException.TYPE;
    /** Exception extends {@link PyBaseException}. */
    public static PyType Exception = PyBaseException.Exception;
    /** {@code TypeError} extends {@link Exception}. */
    public static PyType TypeError = PyBaseException.TypeError;

    /**
     * {@code StopIteration} extends {@code Exception} and is implemented by
     * {@link PyStopIteration}.
     */
    public static PyType StopIteration = PyStopIteration.TYPE;

    /**
     * {@code OSError} extends {@code Exception} and is implemented by
     * {@link PyOSError}.
     */
    public static PyType OSError = PyOSError.TYPE;

    /** {@code EOFError} extends {@link Exception}. */
    public static PyType EOFError = PyBaseException.EOFError;

    /**
     * {@code NameError} extends {@code Exception} and is implemented by
     * {@link PyNameError}.
     */
    public static PyType NameError = PyNameError.TYPE;

    /** {@code UnboundLocalError} extends {@link NameError}. */
    public static PyType UnboundLocalError = PyNameError.UnboundLocalError;

    /**
     * {@code AttributeError} extends {@code Exception} and is implemented
     * by {@link PyAttributeError}.
     */
    public static PyType AttributeError = PyAttributeError.TYPE;

    /** {@code LookupError} extends {@link Exception}. */
    public static PyType LookupError = PyBaseException.LookupError;
    /** {@code IndexError} extends {@link LookupError}. */
    public static PyType IndexError = PyBaseException.IndexError;

    /**
     * {@code KeyError} extends {@code LookupError} and is implemented by
     * {@link PyKeyError}.
     */
    public static PyType KeyError = PyKeyError.TYPE;

    /** {@code ValueError} extends {@link Exception}. */
    public static PyType ValueError = PyBaseException.ValueError;

    /** {@code ArithmeticError} extends {@link Exception}. */
    public static PyType ArithmeticError = PyBaseException.ArithmeticError;
    /** {@code FloatingPointError} extends {@link ArithmeticError}. */
    public static PyType FloatingPointError = PyBaseException.FloatingPointError;
    /** {@code OverflowError} extends {@link ArithmeticError}. */
    public static PyType OverflowError = PyBaseException.OverflowError;
    /** {@code ZeroDivisionError} extends {@link ArithmeticError}. */
    public static PyType ZeroDivisionError = PyBaseException.ZeroDivisionError;

    /** {@code Warning} extends {@link Exception}. */
    public static PyType SystemError = PyBaseException.SystemError;

    /** {@code Warning} extends {@link Exception}. */
    public static PyType Warning = PyBaseException.Exception;
    /** {@code DeprecationWarning} extends {@link Warning}. */
    public static PyType DeprecationWarning = PyBaseException.Warning;
    /** {@code RuntimeWarning} extends {@link Warning}. */
    public static PyType RuntimeWarning = PyBaseException.Warning;
}
