// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;
import org.python.core.util.ExtraMath;
import java.lang.Math;

public class math implements ClassDictInit {
    public static PyFloat pi = new PyFloat(Math.PI);
    public static PyFloat e = new PyFloat(Math.E);

    public static void classDictInit(PyObject dict) {
    }

    private static double check(double v) {
        if (Double.isNaN(v))
            throw Py.ValueError("math domain error");
        if (Double.isInfinite(v))
            throw Py.OverflowError("math range error");
        return v;
    }

    public static double acos(double v) {
        return check(Math.acos(v));
    }

    public static double asin(double v) {
        return check(Math.asin(v));
    }

    public static double atan(double v) {
        return check(Math.atan(v));
    }

    public static double atan2(double v, double w) {
        return check(Math.atan2(v, w));
    }

    public static double ceil(double v) {
        return check(Math.ceil(v));
    }

    public static double cos(double v) {
        return check(Math.cos(v));
    }

    public static double exp(double v) {
        return check(Math.exp(v));
    }

    public static double floor(PyObject v) {
        return floor(v.__float__().getValue());
    }

    public static double floor(double v) {
        return check(Math.floor(v));
    }

    public static double log(PyObject v) {
        return log(v, null);
    }

    public static double log(PyObject v, PyObject base) {
        double doubleValue;
        if (v instanceof PyLong) {
            doubleValue = calculateLongLog((PyLong)v);
        } else {
            doubleValue = log(v.__float__().getValue());
        }
        if (base != null) {
            return check(applyLoggedBase(doubleValue, base));
        }
        return doubleValue;
    }

    private static double calculateLongLog(PyLong v) {
        int e[] = new int[1];
        double x = ((PyLong)v).scaledDoubleValue(e);
        if (x <= 0.0) throw Py.ValueError("math domain error");
        return log(x) + (e[0]*8.0)*log(2.0);
    }

    private static double applyLoggedBase(double loggedValue, PyObject base) {
        double loggedBase;
        if (base instanceof PyLong) {
            loggedBase = calculateLongLog((PyLong)base);
        } else {
            loggedBase = log(base.__float__().getValue());
        }
        return check(loggedValue / loggedBase);
    }

    private static double log(double v) {
        return check(Math.log(v));
    }

    public static double pow(double v, double w) {
        return check(Math.pow(v, w));
    }

    public static double sin(PyObject v) {
        return sin(v.__float__().getValue());
    }

    public static double sin(double v) {
        return check(Math.sin(v));
    }

    public static double sqrt(PyObject v) {
        return sqrt(v.__float__().getValue());
    }

    public static double sqrt(double v) {
        return check(Math.sqrt(v));
    }

    public static double tan(double v) {
        return check(Math.tan(v));
    }

    public static double log10(PyObject v) {
        if (v instanceof PyLong) {
            int e[] = new int[1];
            double x = ((PyLong)v).scaledDoubleValue(e);
            if (x <= 0.0) throw Py.ValueError("math domain error");
            return log10(x) + (e[0]*8.0)*log10(2.0);
        }
        return log10(v.__float__().getValue());
    }

    private static double log10(double v) {
        return check(ExtraMath.log10(v));
    }

    public static double sinh(double v) {
        return check(0.5 * (Math.exp(v) - Math.exp(-v)));
    }

    public static double cosh(double v) {
        return check(0.5 * (Math.exp(v) + Math.exp(-v)));
    }

    public static double tanh(double v) {
        return check(sinh(v) / cosh(v));
    }

    public static double fabs(double v) {
        return Math.abs(v);
    }

    public static double fmod(double v, double w) {
        return v % w;
    }

    public static PyTuple modf(double v) {
        double w = v % 1.0;
        v -= w;
        return new PyTuple(new PyFloat(w), new PyFloat(v));
    }

    public static PyTuple frexp(double v) {
        int i = 0;
        if (v != 0.0) {
            int sign = 1;
            if (v < 0) {
                sign = -1;
                v = -v;
            }
            // slow...
            while (v < 0.5) {
                v = v*2.0;
                i = i-1;
            }
            while (v >= 1.0) {
                v = v*0.5;
                i = i+1;
            }
            v = v*sign;
        }
        return new PyTuple(new PyFloat(v), new PyInteger(i));
    }

    public static double ldexp(double v, int w) {
        return check(v * Math.pow(2.0, w));
    }

    public static double hypot(double v, double w) {
        return check(ExtraMath.hypot(v, w));
    }

    public static double radians(double v) {
    	return check(Math.toRadians(v));
    }

    public static double degrees(double v) {
    	return check(Math.toDegrees(v));
    }
}
