// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandle;

import org.python.core.ArgumentError.Mode;
import org.python.kernel.BaseType;
import org.python.types.FastCall;

/**
 * Abstract base class for the descriptor of a method defined in Java.
 * This class provides some common behaviour and support methods that
 * would otherwise be duplicated. This is also home to some static
 * methods in support of both sub-classes and other callable objects
 * (e.g. {@link PyJavaFunction}).
 */
public abstract class MethodDescriptor extends Descriptor implements FastCall {

    /**
     * Create the common part of {@code MethodDescriptor} sub-classes.
     *
     * @param objclass that defines the attribute being described
     * @param name of the object described as {@code __name__}
     */
    MethodDescriptor(BaseType objclass, String name) { super(objclass, name); }

    /**
     * Return a {@code MethodHandle} by which the Java implementation of the
     * method, corresponding to the given index in
     * {@link PyType#selfClasses()}, may be invoked. The index must be in
     * range for that list. (Zero will always be in range.)
     * <p>
     * This method is intended for use in the construction of call sites in
     * code compiled from Python, and for some uses internal to the runtime.
     * The signature of the returned handle (its
     * {@link MethodHandle#type()}) will be determined by the declaration
     * (in Java) of the method that it wraps, and by the optimisations
     * available in the particular implementation.
     *
     * @param selfClassIndex specifying the Java class of {@code self}
     * @return corresponding handle (to call or generate error)
     * @throws IndexOutOfBoundsException if the index is in the range
     *     acceptable to {@link Descriptor#objclass}.
     */
    // public so that the kernel package may call it.
    public abstract MethodHandle getHandle(int selfClassIndex) throws IndexOutOfBoundsException;

    @Override
    @SuppressWarnings("fallthrough")
    public PyBaseException typeError(ArgumentError ae, Object[] args, String[] names) {
        int n = args.length;
        switch (ae.mode) {
            case NOARGS, NUMARGS, MINMAXARGS:
                return PyErr.format(PyExc.TypeError, "%s() %s (%d given)", name, ae, n);
            case SELF:
                return PyErr.format(PyExc.TypeError, DESCRIPTOR_NEEDS_ARGUMENT, name,
                        objclass.getName());
            case NOKWARGS:
                assert names != null && names.length > 0;
            default:
                return PyErr.format(PyExc.TypeError, "%s() %s", name, ae);
        }
    }

    /**
     * Check that no positional or keyword arguments are supplied. This is
     * for use when implementing {@code __call__} etc..
     *
     * @param args positional argument array to be checked
     * @param names to be checked
     * @throws ArgumentError if positional arguments are given or
     *     {@code names} is not {@code null} or empty
     */
    static void checkNoArgs(Object[] args, String[] names) throws ArgumentError {
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
    static void checkNoArgs(Object[] args) throws ArgumentError {
        if (args.length != 0) { throw new ArgumentError(Mode.NOARGS); }
    }

    /**
     * Check the number of positional arguments and that no keywords are
     * supplied. This is for use when implementing {@code __call__} etc..
     *
     * @param args positional argument array to be checked
     * @param expArgs expected number of positional arguments
     * @param names to be checked
     * @throws ArgumentError if the wrong number of positional arguments are
     *     given or {@code kwargs} is not {@code null} or empty
     */
    static void checkArgs(Object[] args, int expArgs, String[] names) throws ArgumentError {
        if (args.length != expArgs)
            throw new ArgumentError(expArgs);
        else if (names != null && names.length != 0)
            throw new ArgumentError(Mode.NOKWARGS);
    }

    /**
     * Check the number of positional arguments and that no keywords are
     * supplied. This is for use when implementing {@code __call__} etc..
     *
     * @param args positional argument array to be checked
     * @param minArgs minimum number of positional arguments
     * @param maxArgs maximum number of positional arguments
     * @param names to be checked
     * @throws ArgumentError if the wrong number of positional arguments are
     *     given or {@code kwargs} is not {@code null} or empty
     */
    static void checkArgs(Object[] args, int minArgs, int maxArgs, String[] names)
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
     * @throws ArgumentError if the wrong number of positional arguments are
     *     given
     */
    static void checkArgs(Object[] args, int minArgs, int maxArgs) throws ArgumentError {
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
    static void checkHasSelf(Object[] args, String[] names) throws ArgumentError {
        int nkwds = names == null ? 0 : names.length;
        if (nkwds >= args.length) {
            // Not even one argument (self) given by position
            throw new ArgumentError(Mode.SELF);
        }
    }

    private static final String DESCRIPTOR_NEEDS_ARGUMENT =
            "descriptor '%s' of '%.100s' object needs an argument";
}
