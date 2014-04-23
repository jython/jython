// Copyright (c) Corporation for National Research Initiatives
package org.python.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A static utility class with two additional math functions.
 */
public class ExtraMath {

    public static double EPSILON = Math.pow(2.0, -52.0);

    public static double CLOSE = EPSILON * 2.0;

    /**
     * Are v and w "close" to each other? Uses a scaled tolerance.
     */
    public static boolean close(double v, double w, double tol) {
        if (v == w) {
            return true;
        }
        double scaled = tol * (Math.abs(v) + Math.abs(w)) / 2.0;
        return Math.abs(w - v) < scaled;
    }

    public static boolean close(double v, double w) {
        return close(v, w, CLOSE);
    }

    /**
     * Returns floor(v) except when v is very close to the next number, when it returns ceil(v);
     */
    public static double closeFloor(double v) {
        double floor = Math.floor(v);
        return close(v, floor + 1.0) ? floor + 1.0 : floor;
    }

    /**
     * Round the argument x to n decimal places. (Rounding is half-up in Python 2.) The method uses
     * BigDecimal, to compute <i>r(x*10<sup>n</sup>)*10<sup>-n</sup></i>, where <i>r()</i> round to
     * the nearest integer. It takes some short-cuts for extreme values.
     * <p>
     * For sufficiently small <i>x*10<sup>n</sup></i>, the rounding is to zero, and the return value
     * is a signed zero (same sign as x). Suppose <i>x = a*2<sup>b</sup></i>, where the significand
     * we must have <i>a&lt;2</i>. Sufficiently small means such that <i>n log<sub>2</sub>10 <
     * -(b+2)</i>.
     * <p>
     * For sufficiently large <i>x*10<sup>n</sup></i>, the adjustment of rounding is too small to
     * affect the least significant bit. That is <i>a*2<sup>b</sup></i> represents an amount greater
     * than one, and rounding no longer affects the value, and the return is x. Since the matissa
     * has 52 fractional bits, sufficiently large means such that <i>n log<sub>2</sub>10 > 52-b</i>.
     *
     * @param x to round
     * @param n decimal places
     * @return x rounded.
     */
    public static double round(double x, int n) {

        if (Double.isNaN(x) || Double.isInfinite(x) || x == 0.0) {
            // nans, infinities and zeros round to themselves
            return x;

        } else {

            // (Slightly less than) n*log2(10).
            float nlog2_10 = 3.3219f * n;

            // x = a * 2^b and a<2.
            int b = Math.getExponent(x);

            if (nlog2_10 > 52 - b) {
                // When n*log2(10) > nmax, the lsb of abs(x) is >1, so x rounds to itself.
                return x;
            } else if (nlog2_10 < -(b + 2)) {
                // When n*log2(10) < -(b+2), abs(x)<0.5*10^n so x rounds to (signed) zero.
                return Math.copySign(0.0, x);
            } else {
                // We have to work it out properly.
                BigDecimal xx = new BigDecimal(x);
                BigDecimal rr = xx.setScale(n, RoundingMode.HALF_UP);
                return rr.doubleValue();
            }
        }
    }
}
