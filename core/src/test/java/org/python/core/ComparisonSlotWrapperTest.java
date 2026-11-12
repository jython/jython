// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Test the {@link PyWrapperDescr}s for comparison special functions on
 * a variety of types.
 */
class ComparisonSlotWrapperTest extends UnitTestSupport {

    /**
     * Test invocation of the {@code float.__lt__} descriptor on accepted
     * {@code float} classes in all combinations with {@code float} and
     * {@code int} operand types.
     *
     * @throws Throwable unexpectedly
     */
    @SuppressWarnings("static-method")
    @Test
    void float_lt() throws Throwable {

        PyWrapperDescr lt = (PyWrapperDescr)PyFloat.TYPE.lookup("__lt__");

        Double dv = 7.0, dw = 6.0;
        PyFloat pv = newPyFloat(dv), pw = newPyFloat(dw);
        Integer iw = 6;

        List<Object> wList =
                List.of(pw, dw, newPyLong(iw), iw, BigInteger.valueOf(iw), false, true);

        // v is Double, PyFloat.
        for (Object v : List.of(dv, pv)) {
            // w is PyFloat, Double, and int types
            for (Object w : wList) {
                Object r = lt.__call__(new Object[] {v, w}, null);
                assertEquals(Boolean.class, r.getClass());
                assertEquals(false, r);
            }
        }

        dv = -0.1;  // less than everything in wList
        pv = newPyFloat(dv);

        // v is Double, PyFloat.
        for (Object v : List.of(dv, pv)) {
            // w is PyFloat, Double, and int types
            for (Object w : wList) {
                Object r = lt.__call__(new Object[] {v, w}, null);
                assertEquals(Boolean.class, r.getClass());
                assertEquals(true, r);
            }
        }
    }

    /**
     * Test invocation of the {@code float.__eq__} descriptor on accepted
     * {@code float} classes in all combinations with {@code float} and
     * {@code int} operand types.
     *
     * @throws Throwable unexpectedly
     */
    @SuppressWarnings("static-method")
    @Test
    void float_eq() throws Throwable {

        PyWrapperDescr eq = (PyWrapperDescr)PyFloat.TYPE.lookup("__eq__");

        Double dv = 2.0, dw = 1.0;
        PyFloat pv = newPyFloat(dv), pw = newPyFloat(dw);
        Integer iw = 1;

        List<Object> wList = List.of(pw, dw, newPyLong(iw), iw, BigInteger.valueOf(iw), true);

        // v is Double, PyFloat.
        for (Object v : List.of(dv, pv)) {
            // w is PyFloat, Double, and int types
            for (Object w : wList) {
                Object r = eq.__call__(new Object[] {v, w}, null);
                assertEquals(Boolean.class, r.getClass());
                assertEquals(false, r);
            }
        }

        dv = dw;  // equal to everything in wList
        pv = newPyFloat(dv);

        // v is Double, PyFloat.
        for (Object v : List.of(dv, pv)) {
            // w is PyFloat, Double, and int types
            for (Object w : wList) {
                Object r = eq.__call__(new Object[] {v, w}, null);
                assertEquals(Boolean.class, r.getClass());
                assertEquals(true, r);
            }
        }
    }

    /**
     * Test invocation of the {@code int.__lt__} descriptor on accepted
     * {@code int} classes in all combinations.
     *
     * @throws Throwable unexpectedly
     */
    @SuppressWarnings("static-method")
    @Test
    void int_lt() throws Throwable {

        PyWrapperDescr lt = (PyWrapperDescr)PyLong.TYPE.lookup("__lt__");

        Integer iv = 4, iw = -1;
        BigInteger bv = BigInteger.valueOf(iv), bw = BigInteger.valueOf(iw);
        PyLong pv = newPyLong(iv), pw = newPyLong(iw);

        // v is Integer, BigInteger, PyLong, Boolean
        for (Object v : List.of(iv, bv, pv, true)) {
            // w is Integer, BigInteger, PyLong, Boolean
            for (Object w : List.of(iw, bw, pw, false)) {
                Object r = lt.__call__(new Object[] {v, w}, null);
                assertEquals(Boolean.class, r.getClass());
                assertEquals(false, r);
            }
        }

        bv = BigInteger.valueOf(iv = -2);
        pv = newPyLong(iv);
        bw = BigInteger.valueOf(iw = 3);
        pw = newPyLong(iw);

        // v is Integer, BigInteger, PyLong, Boolean
        for (Object v : List.of(iv, bv, pv, false)) {
            // w is Integer, BigInteger, PyLong, Boolean
            for (Object w : List.of(iw, bw, pw, true)) {
                Object r = lt.__call__(new Object[] {v, w}, null);
                assertEquals(Boolean.class, r.getClass());
                assertEquals(true, r);
            }
        }
    }

    /**
     * Test invocation of the {@code int.__eq__} descriptor on accepted
     * {@code int} classes in all combinations.
     *
     * @throws Throwable unexpectedly
     */
    @SuppressWarnings("static-method")
    @Test
    void int_eq() throws Throwable {

        PyWrapperDescr eq = (PyWrapperDescr)PyLong.TYPE.lookup("__eq__");

        Integer iv = 5, iw = 7;
        BigInteger bv = BigInteger.valueOf(iv), bw = BigInteger.valueOf(iw);
        PyLong pv = newPyLong(iv), pw = newPyLong(iw);

        // v is Integer, BigInteger, PyLong, Boolean
        for (Object v : List.of(iv, bv, pv, true)) {
            // w is Integer, BigInteger, PyLong, Boolean
            for (Object w : List.of(iw, bw, pw, false)) {
                Object r = eq.__call__(new Object[] {v, w}, null);
                assertEquals(Boolean.class, r.getClass());
                assertEquals(false, r);
            }
        }

        iv = iw = 1;
        bv = BigInteger.valueOf(iv);
        pv = newPyLong(iv);
        bw = BigInteger.valueOf(iw);
        pw = newPyLong(iw);

        // v is Integer, BigInteger, PyLong, Boolean
        for (Object v : List.of(iv, bv, pv, true)) {
            // w is Integer, BigInteger, PyLong, Boolean
            for (Object w : List.of(iw, bw, pw, true)) {
                Object r = eq.__call__(new Object[] {v, w}, null);
                assertEquals(Boolean.class, r.getClass());
                assertEquals(true, r);
            }
        }
    }

    /**
     * Test invocation of the {@code str.__lt__} descriptor on accepted
     * {@code str} classes in all combinations.
     *
     * @throws Throwable unexpectedly
     */
    @Test
    void str_lt() throws Throwable {
        str_lt_impl("", "anything", true);
        str_lt_impl("cat", "dog", true);
        str_lt_impl("cat", "cats", true);
        str_lt_impl("dogs", "dog", false);
        str_lt_impl("", new String(), false);
    }

    /**
     * Test invoke of the {@code str.__lt__} descriptor on accepted
     * {@code str} classes for the given pair of arguments supplied as
     * {@code String}s.
     *
     * @throws Throwable unexpectedly
     */
    @SuppressWarnings("static-method")
    private void str_lt_impl(String sv, String sw, boolean expected) throws Throwable {

        PyWrapperDescr lt = (PyWrapperDescr)PyUnicode.TYPE.lookup("__lt__");

        // Make PyUnicodes equal to the strings
        PyUnicode pv = newPyUnicode(sv);
        // If object-identical strings, do the same with PyUnicode
        PyUnicode pw = sv == sw ? pv : newPyUnicode(sw);
        List<Object> wList = List.of(sw, pw);

        for (Object v : List.of(sv, pv)) {
            // v is less than (or not) everything on wList as expected
            for (Object w : wList) {
                Object r = lt.__call__(new Object[] {v, w}, null);
                assertEquals(Boolean.class, r.getClass());
                assertEquals(expected, r);
            }
        }

        if (expected) {
            // Repeat with the args reversed, expecting false
            for (Object v : List.of(sv, pv)) {
                for (Object w : wList) {
                    Object r = lt.__call__(new Object[] {w, v}, null);
                    assertEquals(false, r);
                }
            }
        }
    }

    /**
     * Test invocation of the {@code str.__eq__} descriptor on accepted
     * {@code str} classes in all combinations with {@code str} and
     * {@code int} operand types.
     *
     * @throws Throwable unexpectedly
     */
    @SuppressWarnings("static-method")
    @Test
    void str_eq() throws Throwable {

        String sv = "mouse", sw = "mound";

        str_eq_impl(sv, sw, false);
        str_eq_impl(sv, "house", false);
        // Lengths differ
        str_eq_impl(sv, "mouser", false);
        str_eq_impl("mounds", sw, false);
        // Zero length
        str_eq_impl(sv, "", false);
        str_eq_impl("", sw, false);
        str_eq_impl("", "", true);
        str_eq_impl("", new String(), true);
        // Same object
        str_eq_impl(sv, sv, true);
        // Different objects, equal in value
        str_eq_impl(sv, "house".replace('h', 'm'), true);
    }

    /**
     * Test invoke of the {@code str.__eq__} descriptor on accepted
     * {@code str} classes for the given pair of arguments supplied as
     * {@code String}s.
     */
    private static void str_eq_impl(String sv, String sw, boolean expected) throws Throwable {

        PyWrapperDescr eq = (PyWrapperDescr)PyUnicode.TYPE.lookup("__eq__");

        // Make PyUnicodes equal to the strings
        PyUnicode pv = newPyUnicode(sv);
        // If object-identical strings, do the same with PyUnicode
        PyUnicode pw = sv == sw ? pv : newPyUnicode(sw);
        List<Object> wList = List.of(sw, pw);

        for (Object v : List.of(sv, pv)) {
            // v is equal (or not) to everything on wList as expected
            for (Object w : wList) {
                Object r = eq.__call__(new Object[] {v, w}, null);
                assertEquals(Boolean.class, r.getClass());
                assertEquals(expected, r);
            }
        }
    }

}
