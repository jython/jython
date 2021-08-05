// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static java.math.BigInteger.TEN;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test the {@link PyNumber} API class on a variety of types. We are
 * looking for correct behaviour in the cases attempted but mostly
 * testing the invocation of special methods through the operations
 * objects of the particular implementation classes.
 * <p>
 * To reach our main goal, we need only try enough types to exercise
 * every abstract method once in some type.
 */
@DisplayName("The API class PyNumber")
class AbstractNumberAPITest extends UnitTestSupport {

    /** A shorthand for the Python {@code int} type. */
    private static PyType INT = PyLong.TYPE;

    private static final BigInteger INT_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);

    /**
     * This abstract base forms a check-list of methods we mean to test.
     */
    abstract static class Standard {

        abstract void supports_negative() throws Throwable;

        abstract void supports_absolute() throws Throwable;

        abstract void supports_add() throws Throwable;

        abstract void supports_subtract() throws Throwable;

        abstract void supports_multiply() throws Throwable;

        abstract void supports_or() throws Throwable;

        abstract void supports_and() throws Throwable;

        abstract void supports_xor() throws Throwable;

        abstract void supports_index() throws Throwable;

        abstract void supports_asSize() throws Throwable;

        abstract void supports_asLong() throws Throwable;
    }

    /** The {@code int} implementations all behave like this. */
    abstract static class IntLike extends Standard {

        // Working variables for the tests
        Object zero;
        Object small;
        Object large;
        Object negative;
        List<Object> all;
        List<Object> other;

        void setup(Object zero, Object small, Object large, Object negative) {
            this.zero = zero;
            this.small = small;
            this.large = large;
            this.negative = negative;
            this.all = List.of(zero, small, large, negative);
        }

        void other(Object... otherValues) { this.other = List.of(otherValues); }

        /**
         * Assert that the argument is an {@code Integer} if it could be so
         * represented. (Simple unary operations may not normalise this way,
         * and none of them need to, but it is desirable if it may be done
         * quickly.)
         *
         * @param result to test
         */
        static void assertRightSize(Object result) {
            boolean ok = result instanceof Integer;
            if (!ok && result instanceof BigInteger) {
                // Justify by being outside the Integer range
                BigInteger r = (BigInteger)result;
                ok = r.compareTo(INT_MIN) < 0 || r.compareTo(INT_MAX) > 0;
            }
            assertTrue(ok, () -> String.format("result %s should be an Integer", result));
        }

        @Override
        @Test
        void supports_negative() throws Throwable {
            for (Object v : all) {
                Object r = PyNumber.negative(v);
                BigInteger e = PyLong.asBigInteger(v).negate();
                assertEquals(e, PyLong.asBigInteger(r));
                if (v instanceof Integer && !v.equals(Integer.MIN_VALUE)) { assertRightSize(r); }
            }
        }

        @Override
        @Test
        void supports_absolute() throws Throwable {
            for (Object v : all) {
                Object r = PyNumber.absolute(v);
                BigInteger e = PyLong.asBigInteger(v).abs();
                assertEquals(e, PyLong.asBigInteger(r));
                if (v instanceof Integer) { assertRightSize(r); }
            }
        }

        @Override
        @Test
        void supports_add() throws Throwable {
            for (Object v : all) {
                for (Object w : other) {
                    Object r = PyNumber.add(v, w);
                    BigInteger vv = PyLong.asBigInteger(v);
                    BigInteger ww = PyLong.asBigInteger(w);
                    BigInteger e = vv.add(ww);
                    assertEquals(e, PyLong.asBigInteger(r));
                    assertRightSize(r);
                }
            }
        }

        @Override
        @Test
        void supports_subtract() throws Throwable {
            for (Object v : all) {
                for (Object w : other) {
                    Object r = PyNumber.subtract(v, w);
                    BigInteger vv = PyLong.asBigInteger(v);
                    BigInteger ww = PyLong.asBigInteger(w);
                    BigInteger e = vv.subtract(ww);
                    assertEquals(e, PyLong.asBigInteger(r));
                    assertRightSize(r);
                }
            }
        }

        @Override
        @Test
        void supports_multiply() throws Throwable {
            for (Object v : all) {
                for (Object w : other) {
                    Object r = PyNumber.multiply(v, w);
                    BigInteger vv = PyLong.asBigInteger(v);
                    BigInteger ww = PyLong.asBigInteger(w);
                    BigInteger e = vv.multiply(ww);
                    assertEquals(e, PyLong.asBigInteger(r));
                    assertRightSize(r);
                }
            }
        }

        @Override
        @Test
        void supports_or() throws Throwable {
            for (Object v : all) {
                for (Object w : other) {
                    Object r = PyNumber.or(v, w);
                    BigInteger vv = PyLong.asBigInteger(v);
                    BigInteger ww = PyLong.asBigInteger(w);
                    BigInteger e = vv.or(ww);
                    assertEquals(e, PyLong.asBigInteger(r));
                    assertRightSize(r);
                }
            }
        }

        @Override
        @Test
        void supports_and() throws Throwable {
            for (Object v : all) {
                for (Object w : other) {
                    Object r = PyNumber.and(v, w);
                    BigInteger vv = PyLong.asBigInteger(v);
                    BigInteger ww = PyLong.asBigInteger(w);
                    BigInteger e = vv.and(ww);
                    assertEquals(e, PyLong.asBigInteger(r));
                    assertRightSize(r);
                }
            }
        }

        @Override
        @Test
        void supports_xor() throws Throwable {
            for (Object v : all) {
                for (Object w : other) {
                    Object r = PyNumber.xor(v, w);
                    BigInteger vv = PyLong.asBigInteger(v);
                    BigInteger ww = PyLong.asBigInteger(w);
                    BigInteger e = vv.xor(ww);
                    assertEquals(e, PyLong.asBigInteger(r));
                    assertRightSize(r);
                }
            }
        }

        @Override
        @Test
        void supports_index() throws Throwable {
            for (Object v : all) {
                Object r = PyNumber.index(v);
                // For an int-like, it should be the same
                assertEquals(v, r);
            }
        }

        @Override
        @Test
        void supports_asSize() throws Throwable {
            for (Object v : all) {
                Object r = PyNumber.asSize(v, null);
                BigInteger e = PyLong.asBigInteger(v);
                // For an int-like, it should be the same, but clipped.
                if (e.compareTo(INT_MAX) > 0)
                    e = INT_MAX;
                else if (e.compareTo(INT_MIN) < 0)
                    e = INT_MIN;
                assertEquals(e, PyLong.asBigInteger(r));
                assertPythonType(INT, r);
            }
        }

        @Override
        @Test
        void supports_asLong() throws Throwable {
            for (Object v : all) {
                Object r = PyNumber.asLong(v);
                // For an int-like, it should be the same
                assertEquals(v, r);
            }
        }
    }

    @Nested
    @DisplayName("with Integer argument")
    class WithInteger extends IntLike {

        @BeforeEach
        void setup() throws Throwable {
            setup(0, 42, Integer.MAX_VALUE, Integer.MIN_VALUE);
            other(0, 932, ZERO, TEN);
        }
    }

    @Nested
    @DisplayName("with BigInteger argument")
    class WithBigInteger extends IntLike {

        @BeforeEach
        void setup() throws Throwable {
            setup(ZERO, BigInteger.valueOf(42), BigInteger.valueOf(Long.MAX_VALUE).multiply(TEN),
                    BigInteger.valueOf(Long.MIN_VALUE).multiply(TEN));
            other(0, 932, Integer.MIN_VALUE, ZERO, TEN.negate(), TEN.pow(10));
        }
    }
}
