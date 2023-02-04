// Copyright (c)2022 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import org.python.core.ArgumentError.Mode;

/**
 * Abstract base class for the descriptor of a method defined in
 * Java. This class provides some common behaviour and support
 * methods that would otherwise be duplicated. This is also home to
 * some static methods in support of both sub-classes and other
 * callable objects (e.g. {@link PyJavaFunction}).
 */
abstract class MethodDescriptor extends Descriptor implements FastCall {

    MethodDescriptor(PyType descrtype, PyType objclass, String name) {
        super(descrtype, objclass, name);
    }

    @Override
    @SuppressWarnings("fallthrough")
    public TypeError typeError(ArgumentError ae, Object[] args, String[] names) {
        int n = args.length;
        switch (ae.mode) {
            case NOARGS:
            case NUMARGS:
            case MINMAXARGS:
                return new TypeError("%s() %s (%d given)", name, ae, n);
            case SELF:
                return new TypeError(DESCRIPTOR_NEEDS_ARGUMENT, name, objclass);
            case NOKWARGS:
                assert names != null && names.length > 0;
            default:
                return new TypeError("%s() %s", name, ae);
        }
    }

    /**
     * Check that no positional or keyword arguments are supplied. This
     * is for use when implementing {@code __call__} etc..
     *
     * @param args positional argument array to be checked
     * @param names to be checked
     * @throws ArgumentError if positional arguments are given or
     *     {@code names} is not {@code null} or empty
     */
    final static void checkNoArgs(Object[] args, String[] names) throws ArgumentError {
        if (args.length != 0)
            throw new ArgumentError(Mode.NOARGS);
        else if (names != null && names.length != 0)
            throw new ArgumentError(Mode.NOKWARGS);
    }

    /**
     * Check that no positional arguments are supplied, when no keyword
     * arguments have been. This is for use when implementing optimised
     * alternatives to {@code __call__}.
     *
     * @param args positional argument array to be checked
     * @throws ArgumentError if positional arguments are given
     */
    final static void checkNoArgs(Object[] args) throws ArgumentError {
        if (args.length != 0) { throw new ArgumentError(Mode.NOARGS); }
    }

    /**
     * Check the number of positional arguments and that no keywords are
     * supplied. This is for use when implementing {@code __call__}
     * etc..
     *
     * @param args positional argument array to be checked
     * @param expArgs expected number of positional arguments
     * @param names to be checked
     * @throws ArgumentError if the wrong number of positional arguments
     *     are given or {@code kwargs} is not {@code null} or empty
     */
    final static void checkArgs(Object[] args, int expArgs, String[] names) throws ArgumentError {
        if (args.length != expArgs)
            throw new ArgumentError(expArgs);
        else if (names != null && names.length != 0)
            throw new ArgumentError(Mode.NOKWARGS);
    }

    /**
     * Check the number of positional arguments and that no keywords are
     * supplied. This is for use when implementing {@code __call__}
     * etc..
     *
     * @param args positional argument array to be checked
     * @param minArgs minimum number of positional arguments
     * @param maxArgs maximum number of positional arguments
     * @param names to be checked
     * @throws ArgumentError if the wrong number of positional arguments
     *     are given or {@code kwargs} is not {@code null} or empty
     */
    final static void checkArgs(Object[] args, int minArgs, int maxArgs, String[] names)
            throws ArgumentError {
        int n = args.length;
        if (n < minArgs || n > maxArgs)
            throw new ArgumentError(minArgs, maxArgs);
        else if (names != null && names.length != 0)
            throw new ArgumentError(Mode.NOKWARGS);
    }

    /**
     * Check that no positional arguments are supplied, when no keyword
     * arguments have been. This is for use when implementing optimised
     * alternatives to {@code __call__}.
     *
     * @param args positional argument array to be checked
     * @param minArgs minimum number of positional arguments
     * @param maxArgs maximum number of positional arguments
     * @throws ArgumentError if the wrong number of positional arguments
     *     are given
     */
    final static void checkArgs(Object[] args, int minArgs, int maxArgs) throws ArgumentError {
        int n = args.length;
        if (n < minArgs || n > maxArgs) { throw new ArgumentError(minArgs, maxArgs); }
    }

    /**
     * Check that at least one argument {@code self} has been supplied.
     *
     * @param args positional argument array to be checked
     * @param names to be taken into account
     * @throws ArgumentError if {@code self} is missing
     */
    final static void checkHasSelf(Object[] args, String[] names) throws ArgumentError {
        int nkwds = names == null ? 0 : names.length;
        if (nkwds >= args.length) {
            // Not even one argument (self) given by position
            throw new ArgumentError(Mode.SELF);
        }
    }
}
