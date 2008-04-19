// Copyright (c) Corporation for National Research Initiatives
package org.python.core.util;

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
     * Returns floor(v) except when v is very close to the next number, when it
     * returns ceil(v);
     */
    public static double closeFloor(double v) {
        double floor = Math.floor(v);
        return close(v, floor + 1.0) ? floor + 1.0 : floor;
    }
}
