// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.types;

import java.util.Arrays;

import org.python.core.Abstract;
import org.python.core.ArgumentError;
import org.python.core.Callables;
import org.python.core.PyBaseException;
import org.python.core.PyErr;
import org.python.core.PyExc;
import org.python.core.PyUnicode;
import org.python.internal.Util;

/**
 * Support direct calls from Java to the function represented by this
 * object, potentially without constructing an argument array. Clients
 * that know the number of arguments they provide are able to call
 * {@code call(...)} with exactly that number. Callable objects that
 * implement this interface may override signatures of {@code call(...)}
 * that they implement most efficiently.
 */
/*
 * This is an efficiency mechanism similar to the "fast call" paths in
 * CPython. It may provide a basis for efficient call sites for function
 * and method calling when argument lists are simple.
 */
public interface FastCall {

    /**
     * Invoke the target object with standard arguments ({@code Object[]}
     * and {@code String[]}), providing all the argument values from the
     * caller and names for those given by keyword. If no other methods are
     * implemented, a call to any other interface method will land here with
     * an array of the arguments. This is to provide implementations of
     * {@code __call__} with a default when no more optimal call is
     * possible.
     * <p>
     * {@code np = args.length - names.length} arguments are given by
     * position, and the keyword arguments are
     * {{@code names[i]:args[np+i]}}.
     *
     * @implSpec An object that is a {@link FastCall} must support the
     *     standard call (and with the same result as the direct call). It
     *     must not call any other method in this interface, as that would
     *     risk creating a loop.
     * @implNote The reason we do not name this method {@code __call__} is
     *     because that may be called directly from Python, and an object
     *     should have the chance to choose amongst the optimised
     *     implementations specified by this interface, finally resorting to
     *     {@link #call(Object[], String[])} if necessary.
     *
     * @param args all arguments given, positional then keyword
     * @param names of keyword arguments or {@code null}
     * @return result of the invocation
     * @throws ArgumentError if the wrong number of arguments is given, or
     *     keywords where not expected.
     * @throws Throwable from the implementation
     */
    Object call(Object[] args, String[] names) throws ArgumentError, Throwable;

    /**
     * Call the object with arguments given by position only.
     *
     * @implSpec The default implementation calls
     *     {@link #call(Object[], String[])} with a null array of names.
     *
     * @param args arguments given by position
     * @return result of the invocation
     * @throws ArgumentError if the wrong number of arguments is given.
     * @throws Throwable from the implementation
     */
    default Object call(Object[] args) throws ArgumentError, Throwable {
        return call(args, null);
    }

    /* The idea is to provide a series of specialisations e.g. */
    // Object call(arg0, arg1, arg2) { ... }
    /*
     * Implementations then override __call__(Object[], String[]), and all
     * ones they can support efficiently, e.g. call(s), call(s, a), call(s,
     * a, b) for an instance method with up to two arguments. Anything else
     * is converted by a default implementation to call(Object[]).
     */
    /**
     * Call the object with arguments given by position only.
     *
     * @implSpec The default implementation calls {@link #call(Object[])}
     *     with an empty array.
     *
     * @return result of the invocation
     * @throws ArgumentError if zero arguments is the wrong number.
     * @throws Throwable from the implementation
     */
    default Object call() throws ArgumentError, Throwable { return call(Util.EMPTY_ARRAY); }

    /**
     * Call the object with arguments given by position only.
     *
     * @implSpec The default implementation calls {@link #call(Object[])}
     *     with an array the single argument.
     *
     * @param a0 single argument (may be {@code self})
     * @return result of the invocation
     * @throws ArgumentError if one argument is the wrong number.
     * @throws Throwable from the implementation
     */
    default Object call(Object a0) throws ArgumentError, Throwable {
        return call(new Object[] {a0});
    }

    /**
     * Call the object with arguments given by position only.
     *
     * @implSpec The default implementation calls {@link #call(Object[])}
     *     with an array of the arguments.
     *
     * @param a0 zeroth argument (may be {@code self})
     * @param a1 next argument
     * @return result of the invocation
     * @throws ArgumentError if two arguments is the wrong number.
     * @throws Throwable from the implementation
     */
    default Object call(Object a0, Object a1) throws ArgumentError, Throwable {
        return call(new Object[] {a0, a1});
    }

    /**
     * Call the object with arguments given by position only.
     *
     * @implSpec The default implementation calls {@link #call(Object[])}
     *     with an array of the arguments.
     *
     * @param a0 zeroth argument (may be {@code self})
     * @param a1 next argument
     * @param a2 next argument
     * @return result of the invocation
     * @throws ArgumentError if three arguments is the wrong number.
     * @throws Throwable from the implementation
     */
    default Object call(Object a0, Object a1, Object a2) throws ArgumentError, Throwable {
        return call(new Object[] {a0, a1, a2});
    }

    /**
     * Call the object with arguments given by position only.
     *
     * @implSpec The default implementation calls {@link #call(Object[])}
     *     with an array of the arguments.
     *
     * @param a0 zeroth argument (may be {@code self})
     * @param a1 next argument
     * @param a2 next argument
     * @param a3 next argument
     * @throws ArgumentError if four arguments is the wrong number.
     * @return result of the invocation
     * @throws Throwable from the implementation
     */
    default Object call(Object a0, Object a1, Object a2, Object a3)
            throws ArgumentError, Throwable {
        return call(new Object[] {a0, a1, a2, a3});
    }

    /**
     * There are fast signatures for up to {@code MAX_POSITIONAL} arguments,
     */
    // If we add call(a,b,c,d,e) etc., increase this.
    static final int MAX_POSITIONAL = 4;

    /**
     * Invoke the target object with standard arguments, but where the first
     * argument is provided "loose". This is a frequent need when that
     * argument is the {@code self} object in a method call. The call
     * effectively prepends {@code self} to {@code args}, although the aim
     * is usually to minimise data movement. It does no attribute binding.
     * <p>
     * {@code self} and another {@code np = args.length - names.length}
     * arguments are given by position, and the keyword arguments are
     * {{@code names[i]:args[np+i]}}. An incorrect number of arguments for
     * the slot will throw an {@link ArgumentError}, which the caller should
     * decode to a {@code TypeError} by a call to
     * {@link #typeError(ArgumentError, Object[], String[])} on this object.
     *
     * @param self first argument given
     * @param args other arguments given, positional then keyword
     * @param names of keyword arguments or {@code null}
     * @return result of the invocation
     * @throws ArgumentError if the wrong number of arguments is given, or
     *     keywords where not expected.
     * @throws Throwable from the implementation
     */
    default Object call(Object self, Object[] args, String[] names)
            throws ArgumentError, Throwable {
        return call(Util.prepend(self, args), names);
    }

    /**
     * Call this object with the vector call protocol. This supports CPython
     * byte code generated according to the conventions in PEP-590.
     * <p>
     * The {@code stack} argument (which is often the interpreter stack)
     * contains, at a given offset {@code start}, the {@code count}
     * arguments of which the last {@code len(kw)} are given by keyword (and
     * may therefore not be in the order expected by the called object).
     *
     * @param s positional and keyword arguments
     * @param p position of arguments in the array
     * @param n number of positional <b>and keyword</b> arguments
     * @param names of keyword arguments or {@code null}
     * @return the return from the call to the object
     * @throws ArgumentError if the wrong number of arguments is given, or
     *     keywords where not expected.
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if target
     *     is not callable
     * @throws Throwable for errors raised in the function
     */
    // Compare CPython _PyObject_Vectorcall in abstract.h
    // In CPython nargs counts only positional arguments
    default Object vectorcall(Object[] s, int p, int n, String[] names)
            throws ArgumentError, Throwable {
        if (names == null || names.length == 0)
            return vectorcall(s, p, n);
        else {
            Object[] args = Arrays.copyOfRange(s, p, p + n);
            return call(args, names);
        }
    }

    /**
     * Call this object with the vector call protocol, in the case where no
     * arguments were given by keyword. This supports CPython byte code
     * generated according to the conventions in PEP-590, but specialised
     * for this case.
     * <p>
     * The {@code stack} argument (which is often the interpreter stack)
     * contains, at a given offset {@code start}, the {@code count}
     * arguments given by position.
     *
     * @param s positional and keyword arguments
     * @param p position of arguments in the array
     * @param n number of <b>positional</b> arguments
     * @return the return from the call to the object
     * @throws ArgumentError if the wrong number of arguments is given.
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if target
     *     is not callable
     * @throws Throwable for errors raised in the function
     */
    // Compare CPython _PyObject_Vectorcall in abstract.h
    // In CPython nargs counts only positional arguments
    default Object vectorcall(Object[] s, int p, int n) throws ArgumentError, Throwable {
        switch (n) {
            case 0:
                return call();
            case 1:
                return call(s[p]);
            case 2:
                return call(s[p++], s[p]);
            case 3:
                return call(s[p++], s[p++], s[p]);
            case 4:
                return call(s[p++], s[p++], s[p++], s[p]);
            default:
                return call(Arrays.copyOfRange(s, p, p + n));
        }
    }

    /**
     * Translate an {@link ArgumentError} thrown inside a call to a Python
     * {@link PyBaseException TypeError} with proper context. Typically,
     * this adds a Python method name, owning type or module, and the number
     * of arguments given, to the basic information carried in the
     * {@code ArgumentError}.
     * <p>
     * Any of the optimised {@code call(...)}, or {@code vectorcall(...)}
     * methods in this interface may throw {@code ArgumentError} as a
     * shorthand. (This is to keep code short, especially when it is a
     * handle graph.) The caller should catch this close to the call and use
     * this method to swap the {@code ArgumentError} for a Python
     * {@code TypeError}.
     *
     * @param ae previously thrown by this object
     * @param args all arguments given, positional then keyword
     * @param names of keyword arguments or {@code null}
     * @return Python {@code TypeError} to throw
     */
    PyBaseException typeError(ArgumentError ae, Object[] args, String[] names);

    /**
     * Translate an {@link ArgumentError} thrown inside a call to a Python
     * {@link PyBaseException TypeError} with proper context. As
     * {@link #typeError(ArgumentError, Object[], String[])} when there were
     * no arguments by keyword.
     *
     * @param ae previously thrown by this object
     * @param args all arguments given, positional then keyword
     * @return Python {@code TypeError} to throw
     */
    default PyBaseException typeError(ArgumentError ae, Object[] args) {
        return typeError(ae, args, null);
    }

    /**
     * Translate an {@link ArgumentError} thrown inside a call to a Python
     * {@link PyBaseException TypeError} with proper context. As
     * {@link #typeError(ArgumentError, Object[], String[])} for
     * {@link #vectorcall(Object[], int, int, String[])} arguments.
     *
     * @param ae previously thrown by this object
     * @param s positional and keyword arguments
     * @param p position of arguments in the array
     * @param n number of positional <b>and keyword</b> arguments
     * @param names of keyword arguments or {@code null}
     * @return Python {@code TypeError} to throw
     */
    default PyBaseException typeError(ArgumentError ae, Object[] s, int p, int n, String[] names) {
        Object[] args = Arrays.copyOfRange(s, p, p + n);
        return typeError(ae, args, names);
    }

    /**
     * Translate an {@link ArgumentError} thrown inside a call to a Python
     * {@link PyBaseException TypeError} with proper context. As
     * {@link #typeError(ArgumentError, Object[], int, int, String[])} when
     * there were no arguments by keyword.
     *
     * @param ae previously thrown by this object
     * @param s positional and keyword arguments
     * @param p position of arguments in the array
     * @param n number of <b>positional</b> arguments
     * @return Python {@code TypeError} to throw
     */
    default PyBaseException typeError(ArgumentError ae, Object[] s, int p, int n) {
        return typeError(ae, s, p, p + n, null);
    }

    /**
     * Translate an {@link ArgumentError} to a Python {@link PyBaseException
     * TypeError} given the function name and the number and pattern of
     * arguments passed. This default implementation matches the needs of a
     * simple function-like callable identifiable by a {@code name} string.
     *
     * @param name of function
     * @param ae previously thrown by this object
     * @param args all arguments given, positional then keyword
     * @param names of keyword arguments or {@code null}
     * @return Python {@code TypeError} to throw
     */
    @SuppressWarnings("fallthrough")
    public static PyBaseException typeError(String name, ArgumentError ae, Object[] args,
            String[] names) {
        int n = args.length;
        switch (ae.mode) {
            case NOARGS:
            case NUMARGS:
            case MINMAXARGS:
                return PyErr.format(PyExc.TypeError, "%s() %s (%d given)", name, ae, n);
            case NOKWARGS:
                assert names != null && names.length > 0;
            default:
                return PyErr.format(PyExc.TypeError, "%s() %s", name, ae);
        }
    }

    /**
     * A class that wraps any {@code Object} so that it presents the
     * {@link FastCall} interface. Any type of {@code call} will eventually
     * try to invoke the {@code __call__} special method on the object
     * passed to the constructor.
     * <p>
     * This will not make a slow {@code __call__} fast, but it <i>will</i>
     * make a {@code FastCall} slow.
     */
    public static class Slow implements FastCall {

        private final Object callable;

        /**
         * Wraps any {@code Object} so that it presents the {@link FastCall}
         * interface.
         *
         * @param callable should implement {@code __call__}
         */
        public Slow(Object callable) { this.callable = callable; }

        @Override
        public Object call(Object[] args, String[] names) throws ArgumentError, Throwable {
            // Call it slowly.
            return Callables.standardCall(callable, args, names);
        }

        @Override
        public PyBaseException typeError(ArgumentError ae, Object[] args, String[] names) {
            /*
             * The underlying callable has thrown an ArgumentError, which is quite
             * unlikely unless it also implements FastCall.
             */
            if (callable instanceof FastCall fast) {
                return fast.typeError(ae, args, names);
            } else {
                // We'll do our best to make a message somehow.
                String name;
                try {
                    name = PyUnicode.asString(Abstract.repr(callable));
                } catch (Throwable e) {
                    name = "callable";
                }
                // Probably meaningful interpretation of the error
                return FastCall.typeError(name, ae, args, names);
            }
        }
    }
}
