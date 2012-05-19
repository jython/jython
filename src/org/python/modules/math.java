// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import java.math.BigInteger;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.core.PyTuple;
import org.python.core.__builtin__;
import org.python.modules.math_erf;
import org.python.modules.math_gamma;

public class math implements ClassDictInit {
    public static PyFloat pi = new PyFloat(Math.PI);
    public static PyFloat e = new PyFloat(Math.E);
    
    private static final double ZERO = 0.0;
    private static final double MINUS_ZERO = -0.0;
    private static final double HALF = 0.5;
    private static final double ONE = 1.0;
    private static final double MINUS_ONE = -1.0;
    private static final double TWO = 2.0;
    private static final double EIGHT = 8.0;
    private static final double INF = Double.POSITIVE_INFINITY;
    private static final double NINF = Double.NEGATIVE_INFINITY;
    private static final double NAN = Double.NaN;
    private static final BigInteger MAX_LONG_BIGINTEGER = new BigInteger(String.valueOf(Long.MAX_VALUE));
    private static final BigInteger MIN_LONG_BIGINTEGER = new BigInteger(String.valueOf(Long.MIN_VALUE));

    public static void classDictInit(@SuppressWarnings("unused") PyObject dict) {
    }

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
        if (isinf(v)) {
            throwMathDomainValueError();
        }
        if (isnan(v)) {
            return v;
        }
        return Math.acos(v);
    }
    
    public static double acosh(double v) {
        final double ln2 = 6.93147180559945286227e-01;
        final double large = 1 << 28;

        if (isninf(v)) {
            throwMathDomainValueError();
        }
        if (v == ZERO || v == MINUS_ONE) {
            throwMathDomainValueError();
        }
        if (isnan(v) || isinf(v) || (v < 1.0)) {
            return v;
        }
        if (v == 1.0) {
            return 0;
        }
        if (v >= large) {
            return log(v) + ln2;
        }
        return log(v + sqrt(v * v - 1));
    }

    public static double asin(double v) {
        if (isinf(v)) {
            throwMathDomainValueError();
        }
        if (isnan(v)) {
            return v;
        }
        return Math.asin(v);
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
            temp = log(2*v + 1/(sqrt(v*v+1)+v));
        } else if (v < small) {
		temp = v;
        } else {
            temp = log1p(v + v*v/(1+sqrt(1+v*v)));
        }

        return sign ? -temp : temp;
    }

    public static double atan(double v) {
        if (isnan(v)) {
            return v;
        }
        return Math.atan(v);
    }
    
    public static double atanh(double v) {
        if (isnan(v)) {
            return v;
        }
        if (isinf(v) || Math.abs(v) == ONE) {
            throwMathDomainValueError();
        }
        return log((1 + v) / (1 - v)) / 2;
    }

    public static double atan2(double v, double w) {
        return Math.atan2(v, w);
    }

    public static double ceil(PyObject v) {
        return ceil(v.asDouble());
    }

    public static double ceil(double v) {
        if (isnan(v) || isinf(v)) {
            return v;
        }
        return Math.ceil(v);
    }

    public static double cos(double v) {
        if (isinf(v)) {
            throwMathDomainValueError();
        }
        if (isnan(v)) {
            return NAN;
        }
        return Math.cos(v);
    }

    public static double cosh(double v) {
        if (isinf(v)) {
            return INF;
        }
        if (isnan(v)) {
            return v;
        }
        return HALF * (Math.exp(v) + Math.exp(-v));
    }

    public static double exp(double v) {
        if (isninf(v)) {
            return ZERO;
        }
        if (isnan(v) || isinf(v)) {
            return v;
        }
        return check(Math.exp(v));
    }

    public static double floor(PyObject v) {
        return floor(v.asDouble());
    }

    public static double floor(double v) {
        if (isnan(v) || isinf(v)) {
            return v;
        }
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
        if (base != null) {
            return check(applyLoggedBase(doubleValue, base));
        }
        return doubleValue;
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
                throwMathDomainValueError();
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
            throwMathDomainValueError();
        }
        return Math.pow(v, w);
    }

    public static double sin(PyObject v) {
        return sin(v.asDouble());
    }

    public static double sin(double v) {
        if (isinf(v)) {
            throwMathDomainValueError();
        }
        if (isnan(v)) {
            return v;
        }
        return Math.sin(v);
    }

    public static double sqrt(PyObject v) {
        return sqrt(v.asDouble());
    }

    public static double sqrt(double v) {
        if (isnan(v)) {
            return v;
        }
        if (ispinf(v)) {
            return v;
        }
        if (isninf(v) || v == MINUS_ONE) {
            throwMathDomainValueError();
        }
        return Math.sqrt(v);
    }

    public static double tan(double v) {
        if (isnan(v)) {
            return NAN;
        }
        if (isinf(v)) {
            throw Py.ValueError("math domain error");
        }
        return Math.tan(v);
    }

    public static double log10(PyObject v) {
        if (v instanceof PyLong) {
            int exp[] = new int[1];
            double x = ((PyLong)v).scaledDoubleValue(exp);
            if (x <= ZERO) {
                throwMathDomainValueError();
            }
            return log10(x) + (exp[0] * EIGHT) * log10(TWO);
        }
        return log10(v.asDouble());
    }

    public static double sinh(double v) {
        if (isnan(v)) {
            return v;
        }
        if (isinf(v)) {
            return v;
        }
        return HALF * (Math.exp(v) - Math.exp(-v));
    }

    public static double tanh(double v) {
        if (isnan(v)) {
            return v;
        }
        if (isinf(v)) {
            if (isninf(v)) {
                return MINUS_ONE;
            }
            return ONE;
        }
        if (v == MINUS_ZERO) {
            return v;
        }
        return sinh(v) / cosh(v);
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
            throwMathDomainValueError();
        }
        if (isinf(v) && w == ONE) {
            throwMathDomainValueError();
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
        int exponent = 0;

        if (isnan(x) || isinf(x) || x == ZERO) {
            exponent = 0;
        } else {
            short sign = 1;

            if (x < ZERO) {
                x = -x;
                sign = -1;
            }
            
            for (; x < HALF; x *= TWO, exponent--); // needs an empty statement

            for (; x >= ONE; x *= HALF, exponent++); // needs an empty statement

            x *= sign;
        }
        return new PyTuple(new PyFloat(x), new PyInteger(exponent));
    }

    public static PyObject trunc(PyObject number) {
        return number.__getattr__("__trunc__").__call__();
    }

    public static double ldexp(double v, PyObject wObj) {
        if (ZERO == v) {
            return v; // can be negative zero
        }
        if (isinf(v)) {
            return v;
        }
        if (isnan(v)) {
            return v;
        }
        long w = getLong(wObj);
        if (w == Long.MIN_VALUE) {
            if (v > ZERO) {
                return ZERO;
            }
            return MINUS_ZERO;
        }
        return checkOverflow(v * Math.pow(TWO, w));
    }

    public static double hypot(double v, double w) {
        if (isinf(v) || isinf(w)) {
            return INF;
        }
        if (isnan(v) || isnan(w)) {
            return NAN;
        }
        return Math.hypot(v, w);
    }

    public static double radians(double v) {
        return check(Math.toRadians(v));
    }

    public static double degrees(double v) {
        return check(Math.toDegrees(v));
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
        if (isnan(v)) {
            return NAN;
        }
        if (signum(v) == signum(w)) {
            return v;
        }
        return v *= MINUS_ONE;
    }
    
    public static PyLong factorial(double v) {
        if (v == ZERO || v == ONE) {
            return new PyLong(1);
        }
        if (v < ZERO || isnan(v) || isinf(v)) {
            throwMathDomainValueError();
        }
        if (!isIntegral(v)) {
            throwMathDomainValueError();
        }
        // long input should be big enough :-)
        long value = (long)v;
        BigInteger bi = new BigInteger(Long.toString(value));
        for (long l = value - 1; l > 1; l--) {
            bi = bi.multiply(new BigInteger(Long.toString(l)));
        }
        return new PyLong(bi);
    }

    public static double log1p(double v) {
        return log(ONE + v);
    }
    
    public static double fsum(final PyObject iterable) {
        PyFloat result = (PyFloat)__builtin__.__import__("_fsum")
            .invoke("fsum", iterable);
        return result.asDouble();
    }


    private static double calculateLongLog(PyLong v) {
        int exp[] = new int[1];
        double x = v.scaledDoubleValue(exp);
        if (x <= ZERO) {
            throwMathDomainValueError();
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
        return check(loggedValue / loggedBase);
    }

    private static double log(double v) {
        if (isninf(v)) {
            throwMathDomainValueError();
        }
        if (isinf(v) || isnan(v)) {
            return v;
        }
        return Math.log(v);
    }

    private static double log10(double v) {
        if (isninf(v)) {
            throwMathDomainValueError();
        }
        if (isinf(v) || isnan(v)) {
            return v;
        }
        return Math.log10(v);
    }
    
    private static boolean isninf(double v) {
        return v == NINF;
    }

    private static boolean ispinf(double v) {
        return v == INF;
    }
    
    /**
     * work around special Math.signum() behaviour for positive and negative zero
     */
    private static double signum(double v) {
        double signum = ONE;
        if (v == ZERO) {
            if ('-' == Double.toString(v).charAt(0)) {
                signum = MINUS_ONE;
            }
        } else {
            signum = Math.signum(v);
        }
        return signum;
    }

    private static void throwMathDomainValueError() {
        throw Py.ValueError("math domain error");
    }

    private static double check(double v) {
        if (isnan(v))
            throwMathDomainValueError();
        if (isinf(v))
            throw Py.OverflowError("math range error");
        return v;
    }
    
    private static double checkOverflow(double v) {
        if (isinf(v)) {
            throw Py.OverflowError("math range error");
        }
        return v;
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
