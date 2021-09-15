package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.DoubleBinaryOperator;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * These are unit tests of the methods that underlie floating point
 * division and modulus: {@code x//y} and {@code x%y}. The Java and
 * Python have differing semantics for integer division and remainder,
 * so it is not just a case of wrapping up the Java implementation, but
 * of carefully adjusting the arguments and result according to quadrant
 * in which {@code (x,y)} lies.
 * <p>
 * See also {@link IntegerDivisionTest}.
 */
class FloatDivisionTest extends UnitTestSupport {

    /** Test successful division. */
    @Test
    void testDivide() {
        for (Example example : FLOORDIV) {
            if (!example.expectThrow) {
                double x = example.x;
                double y = example.y;
                double r = PyFloat.floordiv(x, y);
                example.test(r);
            }
        }
    }

    /** Test raising {@link ZeroDivisionError} from division. */
    @Test
    void testDivideByZero() {
        // Wrap the method under test as operator
        DoubleBinaryOperator func = PyFloat::floordiv;
        for (Example example : FLOORDIV) {
            if (example.expectThrow) { example.testThrows(func); }
        }
    }

    /** Test remainder on division. */
    @Test
    void testModulo() {
        for (Example example : MODULO) {
            if (!example.expectThrow) {
                double x = example.x;
                double y = example.y;
                double r = PyFloat.mod(x, y);
                example.test(r);
            }
        }
    }

    /** Test raising {@link ZeroDivisionError} from remainder. */
    @Test
    void testModuloByZero() {
        // Wrap the method under test as operator
        DoubleBinaryOperator func = PyFloat::mod;
        for (Example example : MODULO) {
            if (example.expectThrow) { example.testThrows(func); }
        }
    }

    /** Test division and modulus combined "div" part. */
    @Test
    void testDivMod0() {
        for (Example example : FLOORDIV) {
            if (!example.expectThrow) {
                double x = example.x;
                double y = example.y;
                double r = PyFloat
                        .doubleValue(PyFloat.divmod(x, y).get(0));
                example.test(r);
            }
        }
    }

    /** Test division and modulus combined "mod" part. */
    @Test
    void testDivMod1() {
        for (Example example : MODULO) {
            if (!example.expectThrow) {
                double x = example.x;
                double y = example.y;
                double r = PyFloat
                        .doubleValue(PyFloat.divmod(x, y).get(1));
                example.test(r);
            }
        }
    }

    /**
     * Test raising {@link ZeroDivisionError} from division and modulus
     * combined.
     */
    @Test
    void testDivModByZero() {
        // Wrap the method under test as operator
        DoubleBinaryOperator func = (double x, double y) -> PyFloat
                .doubleValue(PyFloat.divmod(x, y).get(0));
        for (Example example : FLOORDIV) {
            if (example.expectThrow) { example.testThrows(func); }
        }
    }

    /**
     * Hold a set of values and the expected result of a test case and
     * scrupulously validate a result. The class is agnostic about the
     * actual computation, which is performed by the client.
     */
    private static class Example {
        final double x;
        final double y;
        final double expected;
        final boolean expectThrow;
        final double tolerance;

        private Example(double x, double y, double expected,
                boolean expectThrow) {
            this.x = x;
            this.y = y;
            this.expected = expected;
            this.expectThrow = expectThrow;
            if (!expectThrow && Double.isFinite(expected)) {
                this.tolerance = Math.abs(expected) * 1e-4;
            } else {
                this.tolerance = 0.0;
            }
        }

        /**
         * A case where a result should be returned.
         *
         * @param x dividend
         * @param y divisor
         * @param expected result
         */
        Example(double x, double y, double expected) {
            this(x, y, expected, false);
        }

        /**
         * A case where a {@link ZeroDivisionError} should be thrown.
         *
         * @param x dividend
         * @param y divisor
         **/
        Example(double x, double y) { this(x, y, 0.0, true); }

        /**
         * Test the given result against the expected answer in all
         * relevant detail.
         *
         * @param result of the invocation
         */
        void test(double result) {
            Supplier<String> msg = () -> this.toString();
            if (Double.isNaN(expected)) {
                // Expecting nan. All nans are equivalent.
                assertTrue(Double.isNaN(result), msg);
            } else {
                // Signs should match (even if zero or infinite)
                assertEquals(Math.copySign(1, expected),
                        Math.copySign(1, result), msg);
                if (Double.isInfinite(expected)) {
                    // Expecting infinity.
                    assertTrue(Double.isInfinite(result), msg);
                } else {
                    // Finite value, so check it normally.
                    assertEquals(expected, result, tolerance, msg);
                }
            }
        }

        /**
         * Test that a {@link ZeroDivisionError} is be thrown.
         *
         * @param result of the invocation
         */
        void testThrows(DoubleBinaryOperator func) {
            Supplier<String> xy = () -> this.toString();
            assertThrows(ZeroDivisionError.class,
                    () -> func.applyAsDouble(x, y), xy);
        }

        @Override
        public String toString() {
            return String.format(
                    "Example [x=% .4g, y=% .4g, expected=% .4g]", x, y,
                    expected);
        }
    }

    private static final double inf = Double.POSITIVE_INFINITY;
    private static final double nan = Double.NaN;

    private static final Example[] FLOORDIV = new Example[] {
            // @formatter:off
            // Reference material to test x//y
            new Example(      -inf,        nan,        nan), //
            new Example(-5.00e+300,        nan,        nan), //
            new Example(     -5.00,        nan,        nan), //
            new Example(     -0.00,        nan,        nan), //
            new Example(      0.00,        nan,        nan), //
            new Example(      5.00,        nan,        nan), //
            new Example( 5.00e+300,        nan,        nan), //
            new Example(       inf,        nan,        nan), //
            new Example(       nan,        nan,        nan), //
            new Example(      -inf,        inf,        nan), //
            new Example(-5.00e+300,        inf, -1.0000000), //
            new Example(     -5.00,        inf, -1.0000000), //
            new Example(     -0.00,        inf, -0.0000000), //
            new Example(      0.00,        inf,  0.0000000), //
            new Example(      5.00,        inf,  0.0000000), //
            new Example( 5.00e+300,        inf,  0.0000000), //
            new Example(       inf,        inf,        nan), //
            new Example(       nan,        inf,        nan), //
            new Example(      -inf,  3.00e+300,        nan), //
            new Example(-5.00e+300,  3.00e+300, -2.0000000), //
            new Example(     -5.00,  3.00e+300, -1.0000000), //
            new Example(     -0.00,  3.00e+300, -0.0000000), //
            new Example(      0.00,  3.00e+300,  0.0000000), //
            new Example(      5.00,  3.00e+300,  0.0000000), //
            new Example( 5.00e+300,  3.00e+300,  1.0000000), //
            new Example(       inf,  3.00e+300,        nan), //
            new Example(       nan,  3.00e+300,        nan), //
            new Example(      -inf,       3.00,        nan), //
            new Example(-5.00e+300,       3.00, -1.6666667e+300), //
            new Example(     -5.00,       3.00, -2.0000000), //
            new Example(     -0.00,       3.00, -0.0000000), //
            new Example(      0.00,       3.00,  0.0000000), //
            new Example(      5.00,       3.00,  1.0000000), //
            new Example( 5.00e+300,       3.00,  1.6666667e+300), //
            new Example(       inf,       3.00,        nan), //
            new Example(       nan,       3.00,        nan), //
            new Example(      -inf,       2.60,        nan), //
            new Example(-5.00e+300,       2.60, -1.9230769e+300), //
            new Example(     -5.00,       2.60, -2.0000000), //
            new Example(     -0.00,       2.60, -0.0000000), //
            new Example(      0.00,       2.60,  0.0000000), //
            new Example(      5.00,       2.60,  1.0000000), //
            new Example( 5.00e+300,       2.60,  1.9230769e+300), //
            new Example(       inf,       2.60,        nan), //
            new Example(       nan,       2.60,        nan), //
            new Example(      -inf,       2.50,        nan), //
            new Example(-5.00e+300,       2.50, -2.0000000e+300), //
            new Example(     -5.00,       2.50, -2.0000000), //
            new Example(     -0.00,       2.50, -0.0000000), //
            new Example(      0.00,       2.50,  0.0000000), //
            new Example(      5.00,       2.50,  2.0000000), //
            new Example( 5.00e+300,       2.50,  2.0000000e+300), //
            new Example(       inf,       2.50,        nan), //
            new Example(       nan,       2.50,        nan), //
            new Example(      -inf,       2.40,        nan), //
            new Example(-5.00e+300,       2.40, -2.0833333e+300), //
            new Example(     -5.00,       2.40, -3.0000000), //
            new Example(     -0.00,       2.40, -0.0000000), //
            new Example(      0.00,       2.40,  0.0000000), //
            new Example(      5.00,       2.40,  2.0000000), //
            new Example( 5.00e+300,       2.40,  2.0833333e+300), //
            new Example(       inf,       2.40,        nan), //
            new Example(       nan,       2.40,        nan), //
            new Example(      -inf,       0.00), // ZeroDivisionError
            new Example(-5.00e+300,       0.00), // ZeroDivisionError
            new Example(     -5.00,       0.00), // ZeroDivisionError
            new Example(     -0.00,       0.00), // ZeroDivisionError
            new Example(      0.00,       0.00), // ZeroDivisionError
            new Example(      5.00,       0.00), // ZeroDivisionError
            new Example( 5.00e+300,       0.00), // ZeroDivisionError
            new Example(       inf,       0.00), // ZeroDivisionError
            new Example(       nan,       0.00), // ZeroDivisionError
            new Example(      -inf,      -0.00), // ZeroDivisionError
            new Example(-5.00e+300,      -0.00), // ZeroDivisionError
            new Example(     -5.00,      -0.00), // ZeroDivisionError
            new Example(     -0.00,      -0.00), // ZeroDivisionError
            new Example(      0.00,      -0.00), // ZeroDivisionError
            new Example(      5.00,      -0.00), // ZeroDivisionError
            new Example( 5.00e+300,      -0.00), // ZeroDivisionError
            new Example(       inf,      -0.00), // ZeroDivisionError
            new Example(       nan,      -0.00), // ZeroDivisionError
            new Example(      -inf,      -2.40,        nan), //
            new Example(-5.00e+300,      -2.40,  2.0833333e+300), //
            new Example(     -5.00,      -2.40,  2.0000000), //
            new Example(     -0.00,      -2.40,  0.0000000), //
            new Example(      0.00,      -2.40, -0.0000000), //
            new Example(      5.00,      -2.40, -3.0000000), //
            new Example( 5.00e+300,      -2.40, -2.0833333e+300), //
            new Example(       inf,      -2.40,        nan), //
            new Example(       nan,      -2.40,        nan), //
            new Example(      -inf,      -2.50,        nan), //
            new Example(-5.00e+300,      -2.50,  2.0000000e+300), //
            new Example(     -5.00,      -2.50,  2.0000000), //
            new Example(     -0.00,      -2.50,  0.0000000), //
            new Example(      0.00,      -2.50, -0.0000000), //
            new Example(      5.00,      -2.50, -2.0000000), //
            new Example( 5.00e+300,      -2.50, -2.0000000e+300), //
            new Example(       inf,      -2.50,        nan), //
            new Example(       nan,      -2.50,        nan), //
            new Example(      -inf,      -2.60,        nan), //
            new Example(-5.00e+300,      -2.60,  1.9230769e+300), //
            new Example(     -5.00,      -2.60,  1.0000000), //
            new Example(     -0.00,      -2.60,  0.0000000), //
            new Example(      0.00,      -2.60, -0.0000000), //
            new Example(      5.00,      -2.60, -2.0000000), //
            new Example( 5.00e+300,      -2.60, -1.9230769e+300), //
            new Example(       inf,      -2.60,        nan), //
            new Example(       nan,      -2.60,        nan), //
            new Example(      -inf,      -3.00,        nan), //
            new Example(-5.00e+300,      -3.00,  1.6666667e+300), //
            new Example(     -5.00,      -3.00,  1.0000000), //
            new Example(     -0.00,      -3.00,  0.0000000), //
            new Example(      0.00,      -3.00, -0.0000000), //
            new Example(      5.00,      -3.00, -2.0000000), //
            new Example( 5.00e+300,      -3.00, -1.6666667e+300), //
            new Example(       inf,      -3.00,        nan), //
            new Example(       nan,      -3.00,        nan), //
            new Example(      -inf, -3.00e+300,        nan), //
            new Example(-5.00e+300, -3.00e+300,  1.0000000), //
            new Example(     -5.00, -3.00e+300,  0.0000000), //
            new Example(     -0.00, -3.00e+300,  0.0000000), //
            new Example(      0.00, -3.00e+300, -0.0000000), //
            new Example(      5.00, -3.00e+300, -1.0000000), //
            new Example( 5.00e+300, -3.00e+300, -2.0000000), //
            new Example(       inf, -3.00e+300,        nan), //
            new Example(       nan, -3.00e+300,        nan), //
            new Example(      -inf,       -inf,        nan), //
            new Example(-5.00e+300,       -inf,  0.0000000), //
            new Example(     -5.00,       -inf,  0.0000000), //
            new Example(     -0.00,       -inf,  0.0000000), //
            new Example(      0.00,       -inf, -0.0000000), //
            new Example(      5.00,       -inf, -1.0000000), //
            new Example( 5.00e+300,       -inf, -1.0000000), //
            new Example(       inf,       -inf,        nan), //
            new Example(       nan,       -inf,        nan), //
            // @formatter:on
    };

    private static final Example[] MODULO = new Example[] {
            // insert output of float_division.py
            // @formatter:off
            // Reference material to test x%y
            new Example(      -inf,        nan,        nan), //
            new Example(-5.00e+300,        nan,        nan), //
            new Example(     -5.00,        nan,        nan), //
            new Example(     -0.00,        nan,        nan), //
            new Example(      0.00,        nan,        nan), //
            new Example(      5.00,        nan,        nan), //
            new Example( 5.00e+300,        nan,        nan), //
            new Example(       inf,        nan,        nan), //
            new Example(       nan,        nan,        nan), //
            new Example(      -inf,        inf,        nan), //
            new Example(-5.00e+300,        inf,        inf), //
            new Example(     -5.00,        inf,        inf), //
            new Example(     -0.00,        inf,  0.0000000), //
            new Example(      0.00,        inf,  0.0000000), //
            new Example(      5.00,        inf,  5.0000000), //
            new Example( 5.00e+300,        inf,  5.0000000e+300), //
            new Example(       inf,        inf,        nan), //
            new Example(       nan,        inf,        nan), //
            new Example(      -inf,  3.00e+300,        nan), //
            new Example(-5.00e+300,  3.00e+300,  1.0000000e+300), //
            new Example(     -5.00,  3.00e+300,  3.0000000e+300), //
            new Example(     -0.00,  3.00e+300,  0.0000000), //
            new Example(      0.00,  3.00e+300,  0.0000000), //
            new Example(      5.00,  3.00e+300,  5.0000000), //
            new Example( 5.00e+300,  3.00e+300,  2.0000000e+300), //
            new Example(       inf,  3.00e+300,        nan), //
            new Example(       nan,  3.00e+300,        nan), //
            new Example(      -inf,       3.00,        nan), //
            new Example(-5.00e+300,       3.00,  0.0000000), //
            new Example(     -5.00,       3.00,  1.0000000), //
            new Example(     -0.00,       3.00,  0.0000000), //
            new Example(      0.00,       3.00,  0.0000000), //
            new Example(      5.00,       3.00,  2.0000000), //
            new Example( 5.00e+300,       3.00,  0.0000000), //
            new Example(       inf,       3.00,        nan), //
            new Example(       nan,       3.00,        nan), //
            new Example(      -inf,       2.60,        nan), //
            new Example(-5.00e+300,       2.60,  0.30303273), //
            new Example(     -5.00,       2.60,  0.20000000), //
            new Example(     -0.00,       2.60,  0.0000000), //
            new Example(      0.00,       2.60,  0.0000000), //
            new Example(      5.00,       2.60,  2.4000000), //
            new Example( 5.00e+300,       2.60,  2.2969673), //
            new Example(       inf,       2.60,        nan), //
            new Example(       nan,       2.60,        nan), //
            new Example(      -inf,       2.50,        nan), //
            new Example(-5.00e+300,       2.50,  0.0000000), //
            new Example(     -5.00,       2.50,  0.0000000), //
            new Example(     -0.00,       2.50,  0.0000000), //
            new Example(      0.00,       2.50,  0.0000000), //
            new Example(      5.00,       2.50,  0.0000000), //
            new Example( 5.00e+300,       2.50,  0.0000000), //
            new Example(       inf,       2.50,        nan), //
            new Example(       nan,       2.50,        nan), //
            new Example(      -inf,       2.40,        nan), //
            new Example(-5.00e+300,       2.40,  1.3339075), //
            new Example(     -5.00,       2.40,  2.2000000), //
            new Example(     -0.00,       2.40,  0.0000000), //
            new Example(      0.00,       2.40,  0.0000000), //
            new Example(      5.00,       2.40,  0.20000000), //
            new Example( 5.00e+300,       2.40,  1.0660925), //
            new Example(       inf,       2.40,        nan), //
            new Example(       nan,       2.40,        nan), //
            new Example(      -inf,       0.00), // ZeroDivisionError
            new Example(-5.00e+300,       0.00), // ZeroDivisionError
            new Example(     -5.00,       0.00), // ZeroDivisionError
            new Example(     -0.00,       0.00), // ZeroDivisionError
            new Example(      0.00,       0.00), // ZeroDivisionError
            new Example(      5.00,       0.00), // ZeroDivisionError
            new Example( 5.00e+300,       0.00), // ZeroDivisionError
            new Example(       inf,       0.00), // ZeroDivisionError
            new Example(       nan,       0.00), // ZeroDivisionError
            new Example(      -inf,      -0.00), // ZeroDivisionError
            new Example(-5.00e+300,      -0.00), // ZeroDivisionError
            new Example(     -5.00,      -0.00), // ZeroDivisionError
            new Example(     -0.00,      -0.00), // ZeroDivisionError
            new Example(      0.00,      -0.00), // ZeroDivisionError
            new Example(      5.00,      -0.00), // ZeroDivisionError
            new Example( 5.00e+300,      -0.00), // ZeroDivisionError
            new Example(       inf,      -0.00), // ZeroDivisionError
            new Example(       nan,      -0.00), // ZeroDivisionError
            new Example(      -inf,      -2.40,        nan), //
            new Example(-5.00e+300,      -2.40, -1.0660925), //
            new Example(     -5.00,      -2.40, -0.20000000), //
            new Example(     -0.00,      -2.40, -0.0000000), //
            new Example(      0.00,      -2.40, -0.0000000), //
            new Example(      5.00,      -2.40, -2.2000000), //
            new Example( 5.00e+300,      -2.40, -1.3339075), //
            new Example(       inf,      -2.40,        nan), //
            new Example(       nan,      -2.40,        nan), //
            new Example(      -inf,      -2.50,        nan), //
            new Example(-5.00e+300,      -2.50, -0.0000000), //
            new Example(     -5.00,      -2.50, -0.0000000), //
            new Example(     -0.00,      -2.50, -0.0000000), //
            new Example(      0.00,      -2.50, -0.0000000), //
            new Example(      5.00,      -2.50, -0.0000000), //
            new Example( 5.00e+300,      -2.50, -0.0000000), //
            new Example(       inf,      -2.50,        nan), //
            new Example(       nan,      -2.50,        nan), //
            new Example(      -inf,      -2.60,        nan), //
            new Example(-5.00e+300,      -2.60, -2.2969673), //
            new Example(     -5.00,      -2.60, -2.4000000), //
            new Example(     -0.00,      -2.60, -0.0000000), //
            new Example(      0.00,      -2.60, -0.0000000), //
            new Example(      5.00,      -2.60, -0.20000000), //
            new Example( 5.00e+300,      -2.60, -0.30303273), //
            new Example(       inf,      -2.60,        nan), //
            new Example(       nan,      -2.60,        nan), //
            new Example(      -inf,      -3.00,        nan), //
            new Example(-5.00e+300,      -3.00, -0.0000000), //
            new Example(     -5.00,      -3.00, -2.0000000), //
            new Example(     -0.00,      -3.00, -0.0000000), //
            new Example(      0.00,      -3.00, -0.0000000), //
            new Example(      5.00,      -3.00, -1.0000000), //
            new Example( 5.00e+300,      -3.00, -0.0000000), //
            new Example(       inf,      -3.00,        nan), //
            new Example(       nan,      -3.00,        nan), //
            new Example(      -inf, -3.00e+300,        nan), //
            new Example(-5.00e+300, -3.00e+300, -2.0000000e+300), //
            new Example(     -5.00, -3.00e+300, -5.0000000), //
            new Example(     -0.00, -3.00e+300, -0.0000000), //
            new Example(      0.00, -3.00e+300, -0.0000000), //
            new Example(      5.00, -3.00e+300, -3.0000000e+300), //
            new Example( 5.00e+300, -3.00e+300, -1.0000000e+300), //
            new Example(       inf, -3.00e+300,        nan), //
            new Example(       nan, -3.00e+300,        nan), //
            new Example(      -inf,       -inf,        nan), //
            new Example(-5.00e+300,       -inf, -5.0000000e+300), //
            new Example(     -5.00,       -inf, -5.0000000), //
            new Example(     -0.00,       -inf, -0.0000000), //
            new Example(      0.00,       -inf, -0.0000000), //
            new Example(      5.00,       -inf,       -inf), //
            new Example( 5.00e+300,       -inf,       -inf), //
            new Example(       inf,       -inf,        nan), //
            new Example(       nan,       -inf,        nan), //
            // @formatter:on
    };
}
