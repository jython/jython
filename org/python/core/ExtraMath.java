// Copyright (c) Corporation for National Research Initiatives

package org.python.core;

/**
 * A static utility class with two additional math functions.
 */
public class ExtraMath {
    public static double LOG10 = Math.log(10.0);

    public static double EPSILON = Math.pow(2.0, -52.0);

    public static double CLOSE = EPSILON * 2.0;

    public static double log10(double v) {
        return Math.log(v) / LOG10;
    }

    public static double hypot(double v, double w) {
        v = Math.abs(v);
        w = Math.abs(w);
        if (v < w) {
            double temp = v;
            v = w;
            w = temp;
        }
        if (v == 0.0) {
            return 0.0;
        } else {
            double wv = w / v;
            return v * Math.sqrt(1.0 + wv * wv);
        }
    }

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
     * Returns floor(v) except when v is very close to the next number, when it
     * returns ceil(v);
     */
    public static double closeFloor(double v) {
        double floor = Math.floor(v);
        return close(v, floor + 1.0) ? floor + 1.0 : floor;
    }
}
