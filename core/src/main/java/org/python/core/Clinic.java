// Copyright (c)2022 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

import org.python.base.InterpreterError;

/**
 * A collection of methods and {@code MethodHandle}s for converting
 * arguments when calling a Java method from Python. The class enables
 * the implementation of built-in (or extension) Python types to be
 * written in a natural way using Java standard and primitive types.
 * <p>
 * The class name refers to the CPython Argument Clinic (by Larry
 * Hastings) which generates argument processing code for exposed
 * methods defined in C and by a textual header.
 */
// Compare CPython *.c.h wrappers
class Clinic {

    /** Lookup for resolving handles throughout the class. */
    private static final Lookup LOOKUP = MethodHandles.lookup();

    private static final Class<?> O = Object.class;

    // Handles for converters from Python to Java types for args
    private static final MethodHandle intArgMH;
    private static final MethodHandle doubleArgMH;
    private static final MethodHandle stringArgMH;

    // Handles for converters from Java types to Python for returns
    private static final MethodHandle voidValueMH;

    private static final MethodHandle intValueMH;
    private static final MethodHandle doubleValueMH;
    private static final MethodHandle booleanValueMH;

    /**
     * Helpers used to construct {@code MethodHandle}s for type
     * conversion.
     */
    static {
        try {
            intArgMH = LOOKUP.findStatic(PyLong.class, "asInt",
                    MethodType.methodType(int.class, O));
            doubleArgMH = LOOKUP.findStatic(PyFloat.class, "asDouble",
                    MethodType.methodType(double.class, O));
            stringArgMH = LOOKUP.findStatic(Clinic.class, "stringArg",
                    MethodType.methodType(String.class, O));

            voidValueMH = MethodHandles.constant(O, Py.None);

            intValueMH = LOOKUP.findStatic(Integer.class, "valueOf",
                    MethodType.methodType(Integer.class, int.class));
            doubleValueMH = LOOKUP.findStatic(Double.class, "valueOf",
                    MethodType.methodType(Double.class, double.class));
            booleanValueMH = LOOKUP.findStatic(Boolean.class, "valueOf",
                    MethodType.methodType(Boolean.class,
                            boolean.class));

        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new InterpreterError(e, "during handle lookup");
        }
    }

    private Clinic() {} // Oh no you don't

    /**
     * Create an array of filters to convert an existing method handle,
     * with the given type, to one that expects arguments (starting at a
     * given index) that are all {@code Object} in the converted
     * locations. The returned array is suitable as an argument to
     * {@code MethodHandles.filterArguments}. (Some elements may be
     * {@code null}, meaning no adapter is applied.)
     * <p>
     * Where the existing method (described by {@code MethodType mt})
     * expects a primitive type, or a supported reference type (such as
     * {@code BigInteger}), the handle to a standard conversion
     * accepting an {@code Object} argument will be supplied. These
     * conversions will throw a Python exception (often
     * {@link TypeError}), when invoked on objects they cannot convert,
     * according to the usual behaviour of Python.
     * <p>
     * Where the existing method expects some other reference type, a
     * {@code null} conversion will be supplied. If the reference type
     * is {@code Object}, no problem arises.
     * <p>
     * When using this filter to adapt a handle on a purported
     * implementation of an exposed Python method, types {@code Clinic}
     * cannot convert will remain unchanged in the {@code MethodType} of
     * the adapted handle. Such a handle must be invoked with arguments
     * of exactly matching static type. If (as is likely), in the
     * invocation context, the arguments will all be statically
     * {@code Object}, the adapted handle would lead to a Java
     * {@code WrongMethodTypeException}.
     *
     * @param mt type to adapt.
     * @param pos index in the type at which to start.
     * @return array of filter-adaptors to expect {@code Object}.
     */
    static MethodHandle[] argumentFilter(MethodType mt, int pos) {
        final int n = mt.parameterCount() - pos;
        MethodHandle[] filter = new MethodHandle[n];
        for (int p = 0; p < n; p++) {
            Class<?> pt = mt.parameterType(pos + p);
            filter[p] = adaptParameterToObject(pt);
        }
        return filter;
    }

    /**
     * Equivalent to {@code argumentFilter(mt, 0)}
     *
     * @param mt type to adapt.
     * @return array of filter-adaptors to expect {@code Object}.
     */
    static MethodHandle[] argumentFilter(MethodType mt) {
        return argumentFilter(mt, 0);
    }

    /**
     * Return a filter that will adapt an existing method handle with
     * the given type, to one that the returns {@code Object} or a
     * sub-class. If not {@code null}, The handle produced is suitable
     * as an argument to {@code MethodHandle.filterReturnValue}.
     * <p>
     * This adapter will often be a constructor for the implementation
     * type or equivalent convenience method. If the return type of is
     * {@code void.class}, the adapter takes no arguments and produces
     * {@link Py#None}. .
     * <p>
     * If the return type is already {@code Object} or a sub-class, this
     * method returns {@code null} (which is not suitable as an argument
     * to {@code MethodHandle.filterReturnValue}). Client code must test
     * for this.
     *
     * @param mt type to adapt.
     * @return {@code null} or a filter-adapter to return
     *     {@code Object}.
     */
    static MethodHandle returnFilter(MethodType mt) {
        return adaptReturnToObject(mt.returnType());
    }

    // Conversions to Java -------------------------------------------

    /**
     * The logic of this method defines the standard for converting
     * Python types to a specified Java type.
     *
     * @param c Java type
     * @return filter converting Python object to {@code c}.
     */
    private static MethodHandle adaptParameterToObject(Class<?> c) {
        if (c.isPrimitive()) {
            if (c == int.class) {
                return Clinic.intArgMH;
            } else if (c == double.class) { return Clinic.doubleArgMH; }
        } else {
            if (c == String.class) {
                return Clinic.stringArgMH;
            } else if (c == O) {
                // The method expects a Object
                return null;
            } else if (O.isAssignableFrom(c)) {
                // The method expects some sub-class of Object
                return null;
            }
        }
        throw new InterpreterError(
                "Cannot convert Python object to Java %s",
                c.getSimpleName());
    }

    /**
     * @param o to convert
     * @return Java {@code String} value of an object
     * @throws TypeError if not interpretable as a string
     * @throws Throwable from {@code o.__str__} if called
     */
    @SuppressWarnings("unused")
    private static String stringArg(Object o)
            throws TypeError, Throwable {
        Class<? extends Object> c = o.getClass();
        if (c == PyUnicode.class) {
            return ((PyUnicode) o).toString();
        } else {
            return Abstract.str(o).toString();
        }
    }

    // Conversions from Java -----------------------------------------

    /**
     * The logic of this method defines the standard for converting
     * specified Java types to Python.
     *
     * @param c Java type
     * @return filter converting {@code c} to a Python object.
     */
    private static MethodHandle adaptReturnToObject(Class<?> c) {
        if (c.isPrimitive()) {
            if (c == void.class) {
                return Clinic.voidValueMH;
            } else if (c == int.class) {
                return Clinic.intValueMH;
            } else if (c == double.class) {
                return Clinic.doubleValueMH;
            } else if (c == boolean.class) {
                return Clinic.booleanValueMH;
            }
        } else {
            // XXX Possibly special-case certain Java types
            // The value is already some kind of Object
            return null;
        }
        throw new InterpreterError(
                "Cannot convert Java %s to Python object",
                c.getSimpleName());
    }

}
