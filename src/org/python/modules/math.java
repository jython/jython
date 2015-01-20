// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import java.math.BigInteger;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.__builtin__;

public class math implements ClassDictInit {

    public static PyFloat pi = new PyFloat(Math.PI);
    public static PyFloat e = new PyFloat(Math.E);

    private static final double ZERO = 0.0;
    private static final double MINUS_ZERO = -0.0;
    private static final double ONE = 1.0;
    private static final double MINUS_ONE = -1.0;
    private static final double TWO = 2.0;
    private static final double EIGHT = 8.0;
    static final double LN2 = 0.693147180559945309417232121458; // Ref OEIS A002162

    private static final double INF = Double.POSITIVE_INFINITY;
    private static final double NINF = Double.NEGATIVE_INFINITY;
    private static final double NAN = Double.NaN;
    private static final BigInteger MAX_LONG_BIGINTEGER = new BigInteger(
            String.valueOf(Long.MAX_VALUE));
    private static final BigInteger MIN_LONG_BIGINTEGER = new BigInteger(
            String.valueOf(Long.MIN_VALUE));

    public static void classDictInit(@SuppressWarnings("unused") PyObject dict) {}

    public static double gamma(double v) {
        return math_gamma.gamma(v);
    }

    public static double lgamma(double v) {
        return math_gamma.lgamma(v);
    }

    public static double erf(double v) {
        return math_erf.erf(v);
    }

    public static double erfc(double v) {
        return math_erf.erfc(v);
    }

    public static double expm1(double v) {
        if (Double.POSITIVE_INFINITY == v) {
            return v;
        }

        double result = Math.expm1(v);
        if (Double.isInfinite(result)) {
            throw Py.OverflowError(Double.toString(v));
        }

        return result;
    }

    public static double acos(double v) {
        return exceptNaN(Math.acos(v), v);
    }

    /**
     * Compute <i>cosh<sup>-1</sup>y</i>.
     *
     * @param y
     * @return x such that <i>cosh x = y</i>
     */
    public static double acosh(double y) {
        if (y < 1.0) {
            throw mathDomainError();

        } else {
            // acosh(y) = ln[y + sqrt(y**2 - 1)]
            if (y < 2.) {
                // Rearrange as acosh(1+u) = ln[1 + u + sqrt(u(2+u))]
                final double u = y - 1.;
                double s = Math.sqrt(u * (2. + u));
                return Math.log1p(u + s);

            } else if (y < 0x1p27) {
                // Rearrange as acosh(y) = ln[ y ( 1 + sqrt[1-(1/y)**2] )]
                final double u = 1. / y;
                double t = Math.sqrt((1. + u) * (1. - u));
                return Math.log(y * (1. + t));

            } else {
                // As above but t indistinguishable from 1.0 so ...
                return Math.log(y) + LN2;
            }
        }
    }

    public static double asin(double v) {
        return exceptNaN(Math.asin(v), v);
    }

    public static double asinh(double v) {
        if (isnan(v) || isinf(v)) {
            return v;
        }

        final double ln2 = 6.93147180559945286227e-01;
        final double large = 1 << 28;
        final double small = 1.0 / (1 << 28);
        boolean sign = false;

        if (v < 0) {
            v = -v;
            sign = true;
        }

        double temp;
        if (v > large) {
            temp = log(v) + ln2;
        } else if (v > 2) {
            temp = log(2 * v + 1 / (sqrt(v * v + 1) + v));
        } else if (v < small) {
            temp = v;
        } else {
            temp = log1p(v + v * v / (1 + sqrt(1 + v * v)));
        }

        return sign ? -temp : temp;
    }

    public static double atan(double v) {
        return exceptNaN(Math.atan(v), v);
    }

    /**
     * Compute <i>tanh<sup>-1</sup>y</i>.
     *
     * @param y
     * @return <i>x</i> such that <i>tanh x = y</i>
     */
    public static double atanh(double y) {
        double absy = Math.abs(y);
        if (absy >= 1.0) {
            throw mathDomainError();
        } else {
            // 2x = ln[(1+y)/(1-y)] = ln[1 + 2y/(1-y)]
            double u = (absy + absy) / (1. - absy);
            double x = 0.5 * Math.log1p(u);
            return Math.copySign(x, y);
        }
    }

    public static double atan2(double v, double w) {
        return Math.atan2(v, w);
    }

    public static double ceil(PyObject v) {
        return ceil(v.asDouble());
    }

    public static double ceil(double v) {
        return Math.ceil(v);
    }

    public static double cos(double v) {
        return exceptNaN(Math.cos(v), v);
    }

    public static double cosh(double v) {
        return exceptInf(Math.cosh(v), v);
    }

    public static double exp(double v) {
        return exceptInf(Math.exp(v), v);
    }

    public static double floor(PyObject v) {
        return floor(v.asDouble());
    }

    public static double floor(double v) {
        return Math.floor(v);
    }

    public static double log(PyObject v) {
        return log(v, null);
    }

    public static double log(PyObject v, PyObject base) {
        double doubleValue;
        if (v instanceof PyLong) {
            doubleValue = calculateLongLog((PyLong)v);
        } else {
            doubleValue = log(v.asDouble());
        }
        return (base == null) ? doubleValue : applyLoggedBase(doubleValue, base);
    }

    public static double pow(double v, double w) {
        if (w == ZERO) {
            return ONE;
        }
        if (v == ONE) {
            return v;
        }
        if (isnan(v) || isnan(w)) {
            return NAN;
        }
        if (v == ZERO) {
            if (w == ZERO) {
                return ONE;
            } else if (w > ZERO || ispinf(w)) {
                return ZERO;
            } else {
                throw mathDomainError();
            }
        }
        if (isninf(v)) {
            if (isninf(w)) {
                return ZERO;
            }
            if (isinf(w)) {
                return INF;
            }
            if (w == ZERO) {
                return ONE;
            }
            if (w > ZERO) {
                if (isOdd(w)) {
                    return NINF;
                }
                return INF;
            }
            if (isOdd(w)) {
                return MINUS_ZERO;
            }
            return ZERO;
        }
        if (isninf(w)) {
            if (v < ZERO) {
                if (v == MINUS_ONE) {
                    return ONE;
                }
                if (v < MINUS_ONE) {
                    return ZERO;
                }
                return INF;
            }
        }
        if (ispinf(w)) {
            if (v < ZERO) {
                if (v == MINUS_ONE) {
                    return ONE;
                }
                if (v < MINUS_ONE) {
                    return INF;
                }
                return ZERO;
            }
        }
        if (v < ZERO && !isIntegral(w)) {
            throw mathDomainError();
        }
        return Math.pow(v, w);
    }

    public static double sin(PyObject v) {
        return sin(v.asDouble());
    }

    public static double sin(double v) {
        return exceptNaN(Math.sin(v), v);
    }

    public static double sqrt(PyObject v) {
        return sqrt(v.asDouble());
    }

    public static double sqrt(double v) {
        return exceptNaN(Math.sqrt(v), v);
    }

    public static double tan(double v) {
        return exceptNaN(Math.tan(v), v);
    }

    public static double log10(PyObject v) {
        if (v instanceof PyLong) {
            int exp[] = new int[1];
            double x = ((PyLong)v).scaledDoubleValue(exp);
            if (x <= ZERO) {
                throw mathDomainError();
            }
            return log10(x) + (exp[0] * EIGHT) * log10(TWO);
        }
        return log10(v.asDouble());
    }

    public static double sinh(double v) {
        return exceptInf(Math.sinh(v), v);
    }

    public static double tanh(double v) {
        return exceptInf(Math.tanh(v), v);
    }

    public static double fabs(double v) {
        return Math.abs(v);
    }

    public static double fmod(double v, double w) {
        if (isnan(v) || isnan(w)) {
            return NAN;
        }
        if (isinf(w)) {
            return v;
        }
        if (w == ZERO) {
            throw mathDomainError();
        }
        if (isinf(v) && w == ONE) {
            throw mathDomainError();
        }
        return v % w;
    }

    public static PyTuple modf(double v) {
        if (isnan(v)) {
            return new PyTuple(new PyFloat(v), new PyFloat(v));
        }
        if (isinf(v)) {
            double first = ZERO;
            if (isninf(v)) {
                first = MINUS_ZERO;
            }
            return new PyTuple(new PyFloat(first), new PyFloat(v));
        }
        double w = v % ONE;
        v -= w;
        return new PyTuple(new PyFloat(w), new PyFloat(v));
    }

    public static PyTuple frexp(double x) {
        int exponent;
        double mantissa;

        switch (exponent = Math.getExponent(x)) {

            default:
                // x = m * 2**exponent and 1 <=abs(m) <2
                exponent = exponent + 1;
                // x = m * 2**exponent and 0.5 <=abs(m) <1
                mantissa = Math.scalb(x, -exponent);
                break;

            case 1024:  // nan or inf
                mantissa = x;
                exponent = 0;
                break;

            case -1023:
                if (x == 0.) { // , 0.0 or -0.0
                    mantissa = x;
                    exponent = 0;
                } else { // denormalised value
                    // x = m * 2**exponent but 0 < abs(m) < 1
                    exponent = Math.getExponent(x * 0x1p52) - 51;
                    mantissa = Math.scalb(x, -exponent);
                }
                break;
        }

        return new PyTuple(new PyFloat(mantissa), new PyInteger(exponent));
    }

    public static PyObject trunc(PyObject number) {
        return number.__getattr__("__trunc__").__call__();
    }

    public static double ldexp(double v, PyObject wObj) {
        long w = getLong(wObj);
        if (w < Integer.MIN_VALUE) {
            w = Integer.MIN_VALUE;
        } else if (w > Integer.MAX_VALUE) {
            w = Integer.MAX_VALUE;
        }
        return exceptInf(Math.scalb(v, (int)w), v);
    }

    /**
     * Returns (x<sup>2</sup> +y<sup>2</sup>)<sup>&#189;</sup> without intermediate overflow or
     * underflow. If either argument is infinite, the result is infinite, but overflow during the
     * calculation is detected as an error.
     *
     * @param x
     * @param y
     * @return (x<sup>2</sup> +y<sup>2</sup>)<sup>&#189;</sup>
     */
    public static double hypot(double x, double y) {
        double mag = Math.hypot(x, y);
        if (Double.isInfinite(mag) && !(Double.isInfinite(x) || Double.isInfinite(y))) {
            // In these circumstances Math.hypot quietly returns inf, but CPython should raise.
            throw mathRangeError();
        }
        return mag;
    }

    public static double radians(double v) {
        return Math.toRadians(v);
    }

    public static double degrees(double v) {
        // Note that this does not raise overflow in Python: 1e307 -> inf as in Java.
        return Math.toDegrees(v);
    }

    public static boolean isnan(double v) {
        return Double.isNaN(v);
    }

    /**
     * @param v
     *
     * @return <code>true</code> if v is positive or negative infinity
     */
    public static boolean isinf(double v) {
        return Double.isInfinite(v);
    }

    public static double copysign(double v, double w) {
        return Math.copySign(v, w);
    }

    public static PyLong factorial(double v) {
        if (v == ZERO || v == ONE) {
            return new PyLong(1);
        } else if (v < ZERO || isnan(v) || isinf(v)) {
            throw mathDomainError();
        } else if (!isIntegral(v)) {
            throw mathDomainError();
        } else {
            // long input should be big enough :-)
            long value = (long)v;
            BigInteger bi = new BigInteger(Long.toString(value));
            for (long l = value - 1; l > 1; l--) {
                bi = bi.multiply(new BigInteger(Long.toString(l)));
            }
            return new PyLong(bi);
        }
    }

    public static double log1p(double v) {
        if (v <= -1.) {
            throw mathDomainError();
        } else {
            return Math.log1p(v);
        }
    }

    public static double fsum(final PyObject iterable) {
        PyFloat result = (PyFloat)__builtin__.__import__("_fsum").invoke("fsum", iterable);
        return result.asDouble();
    }

    private static double calculateLongLog(PyLong v) {
        int exp[] = new int[1];
        double x = v.scaledDoubleValue(exp);
        if (x <= ZERO) {
            throw mathDomainError();
        }
        return log(x) + (exp[0] * EIGHT) * log(TWO);
    }

    private static double applyLoggedBase(double loggedValue, PyObject base) {
        double loggedBase;
        if (base instanceof PyLong) {
            loggedBase = calculateLongLog((PyLong)base);
        } else {
            loggedBase = log(base.asDouble());
        }
        return loggedValue / loggedBase;
    }

    private static double log(double v) {
        if (v <= 0.) {
            throw mathDomainError();
        } else {
            return Math.log(v);
        }
    }

    private static double log10(double v) {
        if (v <= 0.) {
            throw mathDomainError();
        } else {
            return Math.log10(v);
        }
    }

    private static boolean isninf(double v) {
        return v == NINF;
    }

    private static boolean ispinf(double v) {
        return v == INF;
    }

    /**
     * Returns a ValueError("math domain error"), ready to throw from the client code.
     *
     * @return ValueError("math domain error")
     */
    static PyException mathDomainError() {
        return Py.ValueError("math domain error");
    }

    /**
     * Returns a OverflowError("math range error"), ready to throw from the client code.
     *
     * @return OverflowError("math range error")
     */
    static PyException mathRangeError() {
        return Py.OverflowError("math range error");
    }

    /**
     * Turn a <code>NaN</code> result into a thrown <code>ValueError</code>, a math domain error, if
     * the original argument was not itself <code>NaN</code>. Use as:
     *
     * <pre>
     * public static double asin(double v) { return exceptNaN(Math.asin(v), v); }
     * </pre>
     *
     * Note that the original function argument is also supplied to this method. Most Java math
     * library methods do exactly what we need for Python, but some return {@value Double#NaN} when
     * Python should raise <code>ValueError</code>. This is a brief way to change that.
     *
     * @param result to return (if we return)
     * @param arg to include in check
     * @return result if <code>arg</code> was <code>NaN</code> or <code>result</code> was not
     *         <code>NaN</code>
     * @throws PyException (ValueError) if <code>result</code> was <code>NaN</code> and
     *             <code>arg</code> was not <code>NaN</code>
     */
    private static double exceptNaN(double result, double arg) throws PyException {
        if (Double.isNaN(result) && !Double.isNaN(arg)) {
            throw mathDomainError();
        } else {
            return result;
        }
    }

    /**
     * Turn an infinite result into a thrown <code>OverflowError</code>, a math range error, if the
     * original argument was not itself infinite. Use as:
     *
     * <pre>
     * public static double cosh(double v) { return exceptInf( Math.cosh(v), v); }
     * </pre>
     *
     * Note that the original function argument is also supplied to this method. Most Java math
     * library methods do exactly what we need for Python, but some return an infinity when Python
     * should raise <code>OverflowError</code>. This is a brief way to change that.
     *
     * @param result to return (if we return)
     * @param arg to include in check
     * @return result if <code>arg</code> was infinite or <code>result</code> was not infinite
     * @throws PyException (ValueError) if <code>result</code> was infinite and <code>arg</code> was
     *             not infinite
     */
    private static double exceptInf(double result, double arg) {
        if (Double.isInfinite(result) && !Double.isInfinite(arg)) {
            throw mathRangeError();
        } else {
            return result;
        }
    }

    /**
     * convert a PyObject into a long between Long.MIN_VALUE and Long.MAX_VALUE
     */
    private static long getLong(PyObject pyo) {
        if (pyo instanceof PyLong) {
            return getLong(((PyLong)pyo));
        }
        return pyo.asLong();
    }

    /**
     * convert a PyLong into a long between Long.MIN_VALUE and Long.MAX_VALUE
     */
    private static long getLong(PyLong pyLong) {
        BigInteger value = pyLong.getValue();
        if (value.compareTo(MAX_LONG_BIGINTEGER) > 0) {
            return Long.MAX_VALUE;
        }
        if (value.compareTo(MIN_LONG_BIGINTEGER) < 0) {
            return Long.MIN_VALUE;
        }
        return value.longValue();
    }

    private static boolean isIntegral(double v) {
        return ceil(v) - v == ZERO;
    }

    private static boolean isOdd(double v) {
        return isIntegral(v) && v % TWO != ZERO;
    }

}
