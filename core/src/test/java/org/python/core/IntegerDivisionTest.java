package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.python.core.PyLongMethods.divide;
import static org.python.core.PyLongMethods.modulo;
import static org.python.core.PyLongMethods.divmod;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * These are unit tests of the methods that underlie integer division
 * and modulus: {@code x//y} and {@code x%y}. The Java and Python have
 * differing semantics for integer division and remainder, so it is not
 * just a case of wrapping up the Java implementation, but of carefully
 * adjusting the arguments and result according to quadrant in which
 * {@code (x,y)} lies.
 * <p>
 * Furthermore, we have made an attempt to optimise these
 * implementations by arcane logic. All things considered, a careful
 * test is called for.
 */
class IntegerDivisionTest extends UnitTestSupport {

    /**
     * We use these values as the dividend. The list includes zero and
     * {@code Integer.MIN_VALUE}, both of which have alternate paths in
     * the implementation. {@code -Integer.MIN_VALUE / -1} is important
     * because Java division silently overflows.
     */
    private static final int[] XVALUES = argValues(8191, true);
    /**
     * We use these values as the divisor. The list does not include
     * zero, since you can't divide by that.
     */
    private static final int[] YVALUES = argValues(8933, false);

    /**
     * 2**31 aka {@code -Integer.MIN_VALUE / -1}, which Java can't
     * represent, but is a reasonable request in Python.
     */
    private static BigInteger MINUS_INT_MIN =
            BigInteger.valueOf(-(long)Integer.MIN_VALUE);

    /**
     * Reference implementation of integer division.
     *
     * @param x dividend
     * @param y divisor
     * @return quotient
     */
    private static int refDivide(int x, int y) {
        // Python division rounds towards negative infinity
        double dq = (double)x / (double)y;
        long lq = Math.round(Math.floor(dq));
        int q = (int)lq;
        if (q != lq)
            throw new IllegalArgumentException(String
                    .format("Can't return an int from Q(%d,%d)", x, y));
        return q;
    }

    /**
     * Reference implementation of integer division returning
     * {@code Object} and correctly handling dividend of
     * {@link Integer#MIN_VALUE}.
     *
     * @param x dividend
     * @param y divisor
     * @return quotient
     */
    private static Object refDivideObject(int x, int y) {
        if (x == Integer.MIN_VALUE && y == -1)
            // Java division overflows on these values (only)
            return MINUS_INT_MIN;
        else
            return refDivide(x, y);
    }

    /**
     * Compare {@code x//y} computed by class under test and reference
     * means.
     *
     * @param x dividend
     * @param y divisor
     */
    private static void singleDivision(int x, int y) {
        // The result is sometimes an Object not an Integer
        Object ref = refDivideObject(x, y);
        Object q = divide(x, y);
        assertEquals(ref, q, () -> String.format("q(%d,%d)", x, y));
    }

    /**
     * Reference implementation of integer remainder on division.
     *
     * @param x dividend
     * @param y divisor
     * @return remainder
     */
    private static int refRemainder(int x, int y) {
        if (x == Integer.MIN_VALUE && y == -1)
            // Java division overflows on these values (only)
            return 0;
        int q = refDivide(x, y);
        return x - q * y;
    }

    /**
     * Compare {@code x%y} computed by class under test and reference
     * means.
     *
     * @param x dividend
     * @param y divisor
     */
    private static void singleModulo(int x, int y) {
        int ref = refRemainder(x, y);
        int r = PyLong.asInt(modulo(x, y));
        assertEquals(ref, r, () -> String.format("r(%d,%d)", x, y));
    }

    /**
     * Compare {@code divmod(x,y)} computed by class under test and
     * reference means.
     *
     * @param x dividend
     * @param y divisor
     */
    private static void singleDivMod(int x, int y) {
        Object ref_q = refDivideObject(x, y);
        int ref_r = refRemainder(x, y);

        PyTuple qr = divmod(x, y);
        Object q = qr.get(0);
        int r = PyLong.asInt(qr.get(1));

        assertEquals(ref_q, q, () -> String.format("q(%d,%d)", x, y));
        assertEquals(ref_r, r, () -> String.format("r(%d,%d)", x, y));
    }

    /**
     * Generate values across the range of integers, positive,
     * (optionally) zero and negative, with some at or near the
     * extremes. A good choice for the parameter {@code P} is a large
     * prime less than a million.
     *
     * @param P some values will be small multiples of this.
     * @param withZero include zero in the range
     * @return an array of the values
     */
    private static int[] argValues(final int P, boolean withZero) {
        final int N = 5, M = 3;
        List<Integer> values = new LinkedList<>();
        // Some values either side of zero
        for (int x = -N; x <= N; x++) {
            if (x != 0 || withZero) { values.add(x); }
        }
        // A few values at each extreme
        for (int i = 0; i < M; i++) {
            values.add(Integer.MIN_VALUE + i);
            values.add(Integer.MAX_VALUE - i);
        }
        // Some large-ish values from the middle of the range
        for (int i = 2; i <= N; i++) {
            values.add(P * i);
            values.add(-P * i);
        }

        final int L = values.size();
        int[] v = new int[L];
        for (int i = 0; i < L; i++) { v[i] = values.get(i); }
        return v;
    }

    /** Test division in 4 quadrants. */
    @Test
    void testDivide() {
        for (int y : YVALUES) {
            for (int x : XVALUES) { singleDivision(x, y); }
        }
    }

    /** Test remainder on division in 4 quadrants. */
    @Test
    void testModulo() {
        for (int y : YVALUES) {
            for (int x : XVALUES) { singleModulo(x, y); }
        }
    }

    /** Test division and modulus combined in 4 quadrants. */
    @Test
    void testDivMod() {
        for (int y : YVALUES) {
            for (int x : XVALUES) { singleDivMod(x, y); }
        }
    }
}
