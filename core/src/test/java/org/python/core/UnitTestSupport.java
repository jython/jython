// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.junit.jupiter.api.function.Executable;
import org.python.base.InterpreterError;

/**
 * A base class for unit tests that defines some common convenience
 * functions for which the need recurs. A unit test that extends
 * this base will initialise the type system before running.
 */
public class UnitTestSupport {

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
     * @param value to wrap
     * @return from this value.
     */
    static PyFloat newPyFloat(double value) { return new PyFloat(PyFloat.TYPE, value); }

    /**
     * Force creation of an actual {@link PyFloat} from Object
     *
     * @param value to wrap
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
     * @param value to wrap
     * @return from this value.
     */
    static PyUnicode newPyUnicode(String value) { return new PyUnicode(PyUnicode.TYPE, value); }

    /**
     * Force creation of an actual {@link PyUnicode} from an array of
     * code points, which could include surrogates, even in pairs.
     *
     * @param value the code points
     * @return from this value.
     */
    static PyUnicode newPyUnicode(int[] value) {
        return new PyUnicode(PyUnicode.TYPE, value);
    }

    /**
     * The object {@code o} is equal to the expected value according to
     * Python (e.g. {@code True == 1} and strings may be equal even if
     * one is {@code String} and the other {@link PyUnicode}). An
     * unchecked exception may be thrown if the comparison goes badly
     * enough.
     *
     * @param expected value
     * @param o to test
     */
    public static void assertPythonEquals(Object expected, Object o) {
        if (pythonEquals(expected, o)) {
            return;
        } else {
            // This saves making a message ourselves
            assertEquals(expected, o);
        }
    }

    /**
     * As {@link #assertPythonEquals(Object, Object)} but with a message
     * supplied by the caller.
     *
     * @param expected value
     * @param o to test
     * @param messageSupplier supplies the message seen in failures
     */
    public static void assertPythonEquals(Object expected, Object o,
            Supplier<String> messageSupplier) {
        if (pythonEquals(expected, o)) {
            return;
        } else {
            fail(messageSupplier);
        }
    }

    /**
     * Test whether the object {@code o} is equal to the expected value
     * according to Python (e.g. {@code True == 1} and strings may be
     * equal even if one is a {@link PyUnicode}. An unchecked exception
     * may be thrown if the comparison goes badly enough.
     *
     * @param x value
     * @param o to test
     */
    private static boolean pythonEquals(Object x, Object o) {
        try {
            return Abstract.richCompareBool(x, o, Comparison.EQ);
        } catch (RuntimeException | Error e) {
            // Let unchecked exception fly
            throw e;
        } catch (Throwable t) {
            // Wrap checked exception
            throw new InterpreterError(t);
        }
    }

    /**
     * The Python type of {@code o} is exactly the one expected.
     *
     * @param expected type
     * @param o to test
     */
    public static void assertPythonType(PyType expected, Object o) {
        assertTrue(expected.checkExact(o),
                () -> String.format("Java %s not a Python '%s'",
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
        assertTrue(actual.startsWith(expected),
                "should start with " + expected);
    }

    /**
     * Invoke an action expected to raise a Python exception and check
     * the message. The return value may be the subject of further
     * assertions.
     *
     * @param <E> type of exception
     * @param expected type of exception
     * @param action to invoke
     * @param expectedMessage expected message text
     * @return what was thrown
     */
    static <E extends PyException> E assertRaises(Class<E> expected,
            Executable action, String expectedMessage) {
        E t = assertThrows(expected, action);
        assertEquals(expectedMessage, t.getMessage());
        return t;
    }

    /**
     * Find the (Gradle) build directory by ascending the file structure
     * from the path to this class as a resource. Several files we need
     * in tests are to be found at a well-defined location relative to
     * the build directory.
     *
     * This may be used from classes build by the IDE, as long as a
     * Gradle build has been run too. *
     *
     * @return the build directory
     */
    public static Path buildDirectory() {
        // Start at the resources for this class
        Class<?> c = UnitTestSupport.class;
        try {
            URI rsc = c.getResource("").toURI();
            Path path = Path.of(rsc);
            // Navigate up by the length of the package name
            String pkg = c.getPackage().getName();
            int k = -1;
            do { path = path.getParent(); } while ((k = pkg.indexOf('.', k + 1)) >= 0);

            // path is now the folder that contains project classes
            // System.err.println(" ... contains classes");

            // Continue up until path/build exists
            while ((path = path.getParent()) != null) {
                Path buildPath = path.resolve("build");
                if (buildPath.toFile().isDirectory()) { return buildPath; }
            }

            // We reached the root: maybe we did a "clean"
            throw new InterpreterError("build directory not found from %s", rsc.toString());
        } catch (URISyntaxException e) {
            throw new InterpreterError(e);
        }
    }
}
