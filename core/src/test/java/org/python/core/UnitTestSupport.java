// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigInteger;

/**
 * A base class for unit tests that defines some common convenience
 * functions for which the need recurs. A unit test that extends
 * this base will initialise the type system before running.
 */
class UnitTestSupport {

    /** The {@link PyType} {@code object}. */
    /*
     * This is needed to initialise the type system in a controlled way.
     * Java static initialisation of PyType brings into being the
     * critical built-in types in a carefully chosen order. If we use a
     * Python type out of the blue (e.g. call a PyLong static method),
     * initialising that class to use it causes the type system to
     * initialise, but the type that caused it will complete its
     * initialisation last. This subverts the careful ordering of the
     * Python types in PyType.
     */
    static PyType OBJECT = PyType.OBJECT_TYPE;

    /**
     * Convert test value to Java {@code int} (avoiding
     * {@link PyLong#asInt(Object)}).
     *
     * @param v to convert
     * @return converted value
     * @throws ArithmeticError if out of range
     * @throws IllegalArgumentException if wrong type
     */
    static int toInt(Object v) throws ArithmeticError, IllegalArgumentException {
        if (v instanceof Integer)
            return ((Integer)v).intValue();
        else if (v instanceof BigInteger)
            return ((BigInteger)v).intValueExact();
        else if (v instanceof PyLong)
            return ((PyLong)v).value.intValue();
        else if (v instanceof Boolean)
            return (Boolean)v ? 1 : 0;

        throw new IllegalArgumentException(String.format("cannot convert '%s' to int", v));
    }

    /**
     * Convert test value to Java {@code String} (avoiding
     * {@code __str__} for {@code PyUnicode} and non-crafted types).
     *
     * @param v to convert
     * @return converted value
     */
    static String toString(Object v) {
        if (v instanceof String)
            return (String)v;
        else if (v instanceof PyUnicode)
            return ((PyUnicode)v).toString();
        else
            return v.toString();
    }

    /**
     * Force creation of an actual {@link PyLong}
     *
     * @param value to assign
     * @return from this value.
     */
    static PyLong newPyLong(BigInteger value) { return new PyLong(PyLong.TYPE, value); }

    /**
     * Force creation of an actual {@link PyLong} from Object
     *
     * @param value to assign
     * @return from this value.
     */
    static PyLong newPyLong(Object value) {
        BigInteger vv = BigInteger.ZERO;
        try {
            vv = PyLong.asBigInteger(value);
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Failed to create a PyLong");
        }
        return newPyLong(vv);
    }

    /**
     * Convert test value to double (avoiding
     * {@link PyFloat#asDouble(Object)}).
     *
     * @param v to convert
     * @return converted value
     * @throws IllegalArgumentException if wrong type
     */
    static double toDouble(Object v) {
        if (v instanceof Double)
            return ((Double)v).doubleValue();
        else if (v instanceof PyFloat)
            return ((PyFloat)v).value;
        else if (v instanceof Integer)
            return ((Integer)v).intValue();
        else if (v instanceof BigInteger)
            return ((BigInteger)v).doubleValue();
        else if (v instanceof PyLong)
            return ((PyLong)v).value.doubleValue();
        else if (v instanceof Boolean)
            return (Boolean)v ? 1. : 0.;

        throw new IllegalArgumentException(String.format("cannot convert '%s' to double", v));
    }

    /**
     * Force creation of an actual {@link PyFloat}
     *
     * @return from this value.
     */
    static PyFloat newPyFloat(double value) { return new PyFloat(PyFloat.TYPE, value); }

    /**
     * Force creation of an actual {@link PyFloat} from Object
     *
     * @return from this value.
     */
    static PyFloat newPyFloat(Object value) {
        double vv = 0.0;
        try {
            vv = toDouble(value);
        } catch (Throwable e) {
            fail("Failed to create a PyFloat");
        }
        return newPyFloat(toDouble(vv));
    }

    /**
     * Force creation of an actual {@link PyUnicode} from a
     * {@code String} to be treated as in the usual Java encoding.
     * Surrogate pairs will be interpreted as their characters, unless
     * lone.
     *
     * @return from this value.
     */
    static PyUnicode newPyUnicode(String value) { return new PyUnicode(PyUnicode.TYPE, value); }

    /**
     * Force creation of an actual {@link PyUnicode} from an array of
     * code points, which could include surrogates, even in pairs.
     *
     * @return from this value.
     */
    static PyUnicode newPyUnicode(int[] value) { return new PyUnicode(PyUnicode.TYPE, value); }

    /**
     * The Python type of {@code o} is exactly the one expected.
     *
     * @param expected type
     * @param o to test
     */
    static void assertPythonType(PyType expected, Object o) {
        assertTrue(expected.checkExact(o), () -> String.format("Java %s not a Python '%s'",
                o.getClass().getSimpleName(), expected.name));
    }

    /**
     * Assertion for test that a result is a string beginning a certain
     * way.
     *
     * @param expected prefix
     * @param actual result to match
     */
    static void assertStartsWith(String expected, String actual) {
        assertTrue(actual.startsWith(expected), "should start with " + expected);
    }
}
