// Copyright © Corporation for National Research Initiatives

package org.python.core;

public class ExtraMath {
    static double LOG10 = Math.log(10.0);

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
        if (v == 0.0)
            return 0.0;
        else {
            double wv = w/v;
            return v * Math.sqrt(1.0 + wv*wv);
        }
    }
}
