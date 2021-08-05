package org.python.core;

import org.python.core.PyObjectUtil.NoConversion;

/**
 * This is a placeholder to satisfy references in the implementation
 * before we have a proper {@code str} type.
 */
class PyFloat {

    // special methods ------------------------------------------------

    static Object __pow__(Object left, Object right, Object modulus) {
        try {
            if (modulus == null || modulus == Py.None) {
                return pow(convert(left), convert(right));
            } else {
                // Note that we also call __pow__ from PyLong.__pow__
                throw new TypeError(POW_3RD_ARGUMENT);
            }
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    private static final String POW_3RD_ARGUMENT =
            "pow() 3rd argument not allowed unless all arguments are integers";

    static Object __rpow__(Object right, Object left) {
        try {
            return pow(convert(left), convert(right));
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }


    // plumbing ------------------------------------------------------

    private static double convert(Object v) throws NoConversion, OverflowError {
        // Check against supported types, most likely first
        if (v instanceof Double)
            return ((Double)v).doubleValue();
        else
            // BigInteger, PyLong, Boolean, etc.
            // or throw PyObjectUtil.NO_CONVERSION;
            return PyLong.convertToDouble(v);
    }

    /**
     * Exponentiation with Python semantics.
     *
     * @param v base value
     * @param w exponent
     * @return {@code v ** w}
     */
    static double pow(double v, double w) {
        /*
         * This code was translated from the CPython implementation at
         * v2.7.8 by progressively removing cases that could be delegated to
         * Java. Jython differs from CPython in that where C pow()
         * overflows, Java pow() returns inf (observed on Windows). This is
         * not subject to regression tests, so we take it as an allowable
         * platform dependency. All other differences in Java Math.pow() are
         * trapped below and Python behaviour is enforced.
         */
        if (w == 0) {
            // v**0 is 1, even 0**0
            return 1.0;

        } else if (Double.isNaN(v)) {
            // nan**w = nan, unless w == 0
            return Double.NaN;

        } else if (Double.isNaN(w)) {
            // v**nan = nan, unless v == 1; 1**nan = 1
            return v == 1.0 ? v : Double.NaN;

        } else if (Double.isInfinite(w)) {
            /*
             * In Java Math pow(1,inf) = pow(-1,inf) = pow(1,-inf) =
             * pow(-1,-inf) = nan, but in Python they are all 1.
             */
            if (v == 1.0 || v == -1.0) {
                return 1.0;
            }

        } else if (v == 0.0) {
            // 0**w is an error if w is negative.
            if (w < 0.0) {
                throw new ZeroDivisionError("0.0 cannot be raised to a negative power");
            }

        } else if (!Double.isInfinite(v) && v < 0.0) {
            if (w != Math.floor(w)) {
                throw new ValueError("negative number cannot be raised to a fractional power");
            }
        }

        // In all other cases we can entrust the calculation to Java.
        return Math.pow(v, w);
    }
}
