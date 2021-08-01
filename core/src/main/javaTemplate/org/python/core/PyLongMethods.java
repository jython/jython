package org.python.core;

import java.math.BigInteger;
import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;

import org.python.core.PyObjectUtil.NoConversion;

// $OBJECT_GENERATOR$ PyLongGenerator

/**
 * This class contains static methods implementing operations on the
 * Python {@code int} object, supplementary to those defined in
 * {@link PyLong}.
 * <p>
 * These methods may cause creation of descriptors in the dictionary of
 * the type. Those with reserved names in the data model will also fill
 * slots in the {@code Operations} object for the type.
 * <p>
 * Implementations of binary operations defined here will have
 * {@code Object} as their second argument, and should return
 * {@link Py#NotImplemented} when the type in that position is not
 * supported.
 */
class PyLongMethods {

    private PyLongMethods() {}  // no instances

    // $SPECIAL_METHODS$ ---------------------------------------------

    // plumbing ------------------------------------------------------

    /**
     * Convert an {@code int} or its sub-class to a Java
     * {@code BigInteger}. Conversion may raise an exception that is
     * propagated to the caller. If the Java type of the {@code int} is
     * declared, generally there is a better option than this method. We
     * only use it for {@code Object} arguments. If the method throws
     * the special exception {@link NoConversion}, the caller must catch
     * it, and will normally return {@link Py#NotImplemented}.
     * 
     * @param v to convert
     * @return converted to {@code BigInteger}
     * @throws NoConversion v is not an {@code int}
     */
    private static BigInteger toBig(Object v) throws NoConversion {
        // Check against supported types, most likely first
        if (v instanceof Integer)
            return BigInteger.valueOf(((Integer) v).longValue());
        else if (v instanceof BigInteger)
            return (BigInteger) v;
        else if (v instanceof PyLong)
            return ((PyLong) v).value;
        else if (v instanceof Boolean)
            return (Boolean) v ? ONE : ZERO;

        throw PyObjectUtil.NO_CONVERSION;
    }

    /**
     * Reduce a {@code BigInteger} result to {@code Integer} if
     * possible. This makes it more likely the next operation will be
     * 32-bit.
     * 
     * @param r to reduce
     * @return equal value
     */
    static Object toInt(BigInteger r) {
        /*
         * Implementation note: r.intValueExact() is for exactly this
         * purpose, but building the ArithmeticException is a huge cost.
         * (2900ns is added to a 100ns __add__.) The compiler (as tested
         * in JDK 11.0.9) doesn't recognise that it can be optimised
         * to a jump. This version of toInt() adds around 5ns.
         */
        if (r.bitLength() < 32)
            return r.intValue();
        else
            return r;
    }

    /** 2**31 aka Integer.MIN_VALUE / -1, which Java can't do. */
    private static BigInteger MINUS_INT_MIN =
            BigInteger.valueOf(-(long)Integer.MIN_VALUE);

    /**
     * Convenience function to create a {@link ZeroDivisionError}.
     *
     * @return to throw
     */
    private static ZeroDivisionError zeroDivisionError() {
        return new ZeroDivisionError(
                "integer division or modulo by zero");
    }

    /**
     * Divide x by y with integer result, following the Python sign
     * convention. The convention makes sense taken together with that
     * for remainders (the modulo operation {@code %}). As would be
     * expected, Python guarantees that {@code x = (x//y)*y + (x%y)}. It
     * also chooses that the sign of {@code x%y}, if it is not zero,
     * should be the same as that of {@code y}. This causes both
     * {@code /} and {@code %} to differ from their semantics in Java.
     *
     * @param x dividend
     * @param y divisor
     * @return quotient
     */
    static Object divide(int x, int y) {
        /*
         * Differences from Java integer quotient require adjustments in
         * quadrants 2 and 4 (excluding axes). A branch-free formula for
         * the Python quotient in terms of the Java one Q is q(x,y) =
         * Q(x+a,y)-b where a=b=0 if x and y have the same sign, or x=0,
         * and otherwise b=-1 and a is +1 or -1 with the opposite sign
         * to x.
         */
        try {
            if (x << 1 != 0) { // x !=0 and x != Integer.MIN_VALUE
                // x>>31 is 0 or -1 according to the sign of x
                int u = x >> 31;
                // y>>31 is 0 or -1 according to the sign of y
                int v = y >> 31;
                int a = v - u; // -1, 0 or 1
                int b = v ^ u; // 0 or -1
                // Q(x+a,y) + b where a = 1, 0, -1 and b = 0, -1
                return (x + a) / y + b;
            } else {
                // Special cases where the formula above fails:
                // x ==0 or x == Integer.MIN_VALUE
                if (x == 0 || y < -1)
                    // Java and Python agree
                    return x / y;
                else if (y >= 0) // and x == Integer.MIN_VALUE
                    // Opposite signs: use adjusted formula
                    return (x + 1) / y - 1;
                else
                    // y == -1 and x == Integer.MIN_VALUE
                    return MINUS_INT_MIN;
            }
        } catch (ArithmeticException ae) {
            // This can only be because y==0
            throw zeroDivisionError();
        }
    }

    /**
     * {@code x mod y} with {@code int} arguments, following the Python
     * sign convention. The convention makes sense taken together with
     * that for floor division (the modulo operation {@code //}). As
     * would be expected, Python guarantees that
     * {@code x = (x//y)*y + (x%y)}. It also chooses that the sign of
     * {@code x%y}, if it is not zero, should be the same as that of
     * {@code y}. This causes both {@code /} and {@code %} to differ
     * from their semantics in Java.
     *
     * @param x dividend
     * @param y divisor
     * @return remainder
     */
    static int modulo(int x, int y) {
        /*
         * Differences from Java integer remainder require adjustments
         * in quadrants 2 and 4 (excluding axes). A branch-free formula
         * for the Python remainder in terms of the Java one R is r(x,y)
         * = R(x+a,y)-b*y-a where a=b=0 if x and y have the same sign,
         * or x=0, and otherwise b=-1 and a is +1 or -1 with the
         * opposite sign to x.
         */
        try {
            if (x << 1 != 0) { // x !=0 and x != Integer.MIN_VALUE
                // x>>31 is 0 or -1 according to the sign of x
                int u = x >> 31;
                // y>>31 is 0 or -1 according to the sign of y
                int v = y >> 31;
                int a = v - u; // -1, 0 or 1
                int b = v ^ u; // 0 or -1
                // R(x+a,y) - b*y - a where -b*y can be done with &
                return (x + a) % y + (b & y) - a;
            } else {
                // Special cases where the formula above fails:
                // x ==0 or x == Integer.MIN_VALUE
                if (y < -1 || x == 0)
                    // Java and Python agree
                    return x % y;
                else if (y >= 0) // and x == Integer.MIN_VALUE
                    // Opposite signs: use adjusted formula
                    return (x + 1) % y + y - 1;
                else
                    // y == -1 and x == Integer.MIN_VALUE
                    return 0;

            }
        } catch (ArithmeticException ae) {
            // This can only be because y==0
            throw zeroDivisionError();
        }
    }

    /**
     * Divide x by y with integer result, following the Python sign
     * convention. The convention makes sense taken together with that
     * for remainders (the modulo operation {@code %}). As would be
     * expected, Python guarantees that {@code x = (x//y)*y + (x%y)}. It
     * also chooses that the sign of {@code x%y}, if it is not zero,
     * should be the same as that of {@code y}. This causes both
     * {@code /} and {@code %} to differ from their semantics in Java.
     *
     * @param x dividend
     * @param y divisor
     * @return quotient
     */
    static BigInteger divide(BigInteger x, BigInteger y) {
        /*
         * Getting signs correct for integer division is accomplished by
         * adjusting x in the cases where the signs are opposite. This
         * convention makes sense when you consider it with modulo.
         */
        int ySign = y.signum();
        if (ySign == 0) {
            throw zeroDivisionError();
        } else if (ySign < 0) {
            if (x.signum() > 0) { x = x.subtract(y).subtract(ONE); }
        } else {
            if (x.signum() < 0) { x = x.subtract(y).add(ONE); }
        }
        return x.divide(y);
    }

    /**
     * {@code x mod y} with {@code BigInteger} arguments, following the
     * Python sign convention. The convention makes sense taken together
     * with that for floor division (the modulo operation {@code //}).
     * As would be expected, Python guarantees that
     * {@code x = (x//y)*y + (x%y)}. It also chooses that the sign of
     * {@code x%y}, if it is not zero, should be the same as that of
     * {@code y}. This causes both {@code /} and {@code %} to differ
     * from their semantics in Java.
     *
     * @param x dividend
     * @param y divisor
     * @return remainder
     */
    static BigInteger modulo(BigInteger x, BigInteger y) {
        BigInteger q = divide(x, y);
        return x.subtract(q.multiply(y));
    }

    /**
     * {@code divmod(x,y)} with {@code int} arguments, following the
     * Python sign convention.
     *
     * @param x dividend
     * @param y divisor
     * @return quotient and remainder
     */
    static PyTuple divmod(int x, int y) {
        try {
            /*
             * Compute the quotient as in divide(x,y), as an int (except
             * in a corner-case), and the remainder from it.
             */
            int q;
            if (x << 1 != 0) { // x !=0 and x != Integer.MIN_VALUE
                // x>>31 is 0 or -1 according to the sign of x
                int u = x >> 31;
                // y>>31 is 0 or -1 according to the sign of y
                int v = y >> 31;
                int a = v - u; // -1, 0 or 1
                int b = v ^ u; // 0 or -1
                // Q(x+a,y) + b where a = 1, 0, -1 and b = 0, -1
                q = (x + a) / y + b;
            } else {
                // Special cases where the formula above fails:
                // x ==0 or x == Integer.MIN_VALUE
                if (y < -1 || x == 0)
                    // Java and Python agree
                    q = x / y;
                else if (y >= 0)  // and x == Integer.MIN_VALUE
                    // Opposite signs: use adjusted formula
                    q = (x + 1) / y - 1;
                else
                    // y == -1 and x == Integer.MIN_VALUE
                    return new PyTuple(MINUS_INT_MIN, 0);
            }
            return new PyTuple(q, x - q * y);
        } catch (ArithmeticException ae) {
            // This can only be because y==0
            throw zeroDivisionError();
        }
    }

    /**
     * {@code divmod(x,y)} with {@code BigInteger} arguments, following
     * the Python sign convention.
     *
     * @param x dividend
     * @param y divisor
     * @return quotient and remainder
     */
    static PyTuple divmod(BigInteger x, BigInteger y) {
        BigInteger q = divide(x, y);
        return new PyTuple(q, x.subtract(q.multiply(y)));
    }

    /**
     * Python true-division of {@code BigInteger} arguments.
     *
     * @param x dividend
     * @param y divisor
     * @return quotient
     */
    static final double trueDivide(BigInteger x, BigInteger y) {
        int[] xe = new int[1];
        int[] ye = new int[1];
        double xd = scaledDoubleValue(x, xe);
        double yd = scaledDoubleValue(y, ye);

        if (yd == 0) { throw zeroDivisionError(); }

        double q = xd / yd;
        int exp = xe[0] - ye[0];

        if (exp > Integer.MAX_VALUE / 8) {
            throw PyLong.tooLarge("integer division result", "float");
        } else if (exp < -(Integer.MAX_VALUE / 8)) { return 0.0; }

        q = q * Math.pow(2.0, exp * 8);

        if (Double.isInfinite(q)) {
            throw PyLong.tooLarge("integer division result", "float");
        }

        return q;
    }

    // Helper for trueDivide (fresh from Jython 2, so no comments)
    private static final double scaledDoubleValue(BigInteger val,
            int[] exp) {
        double x = 0;
        int signum = val.signum();
        byte[] digits;

        if (signum >= 0) {
            digits = val.toByteArray();
        } else {
            digits = val.negate().toByteArray();
        }

        int count = 8;
        int i = 0;

        if (digits[0] == 0) { i++; count++; }
        count = count <= digits.length ? count : digits.length;

        while (i < count) { x = x * 256 + (digits[i] & 0xff); i++; }
        exp[0] = digits.length - i;
        return signum * x;
    }
}
