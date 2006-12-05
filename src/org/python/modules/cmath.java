package org.python.modules;

import org.python.core.Py;
import org.python.core.PyComplex;
import org.python.core.PyException;
import org.python.core.PyFloat;
import org.python.core.PyObject;
import org.python.modules.math;

public class cmath {
    public static PyFloat pi = new PyFloat(Math.PI);
    public static PyFloat e = new PyFloat(Math.E);

    private static PyComplex one = new PyComplex(1.0, 0.0);
    private static PyComplex half = new PyComplex(0.5, 0.0);
    private static PyComplex i = new PyComplex(0.0, 1.0);
    private static PyComplex half_i = new PyComplex(0.0, 0.5);

    private static PyComplex c_prodi(PyComplex x) {
        return (new PyComplex(-x.imag, x.real));
    }

    private static double hypot(double x, double y) {
        return (Math.sqrt(x * x + y * y));
    }

    private static PyComplex complexFromPyObject(PyObject in) {
        try{
            return(in.__complex__());
        } catch(PyException e){
            if(e.type == Py.AttributeError) {
                throw Py.TypeError("a float is required");
            }
            throw e;
        }
    }

    public static PyObject acos(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return (c_prodi(log(x.__add__(i
                .__mul__(sqrt(one.__sub__(x.__mul__(x))))))).__neg__());
    }

    public static PyComplex acosh(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex r = null;

        PyComplex a = sqrt(x.__sub__(one));
        PyComplex b = sqrt(x.__add__(one));
        PyComplex c = sqrt(half);
        r = log(c.__mul__(b.__add__(a)));

        return ((PyComplex) r.__add__(r));
    }

    public static PyComplex asin(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex r = null;

        PyComplex squared = (PyComplex) x.__mul__(x);
        PyComplex sq1_minus_xsq = sqrt(one.__sub__(squared));

        r = (PyComplex) c_prodi(log(sq1_minus_xsq.__add__(c_prodi(x))))
                .__neg__();
        return (r);
    }

    public static PyComplex asinh(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex r = null;

        PyComplex a = sqrt(x.__add__(i));
        PyComplex b = sqrt(x.__sub__(i));
        PyComplex z = sqrt(half);
        r = log(z.__mul__(a.__add__(b)));

        return ((PyComplex) r.__add__(r));
    }

    public static PyComplex atan(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex r = (PyComplex) half_i.__mul__(log(i.__add__(x).__div__(
                i.__sub__(x))));

        return (r);
    }

    public static PyComplex atanh(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex r = (PyComplex) half.__mul__(log(one.__add__(x).__div__(
                one.__sub__(x))));
        return (r);
    }

    public static PyComplex cos(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex r = new PyComplex(Math.cos(x.real) * math.cosh(x.imag), -Math
                .sin(x.real)
                * math.sinh(x.imag));
        return (r);
    }

    public static PyComplex cosh(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex r = new PyComplex(Math.cos(x.imag) * math.cosh(x.real), Math
                .sin(x.imag)
                * math.sinh(x.real));
        return (r);
    }

    public static PyComplex exp(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex r = new PyComplex(0.0, 0.0);
        double l = Math.exp(x.real);
        r.real = l * Math.cos(x.imag);
        r.imag = l * Math.sin(x.imag);
        return (r);
    }

    public static PyComplex log(PyObject in) {
        PyComplex r = new PyComplex(0.0, 0.0);
        PyComplex x = complexFromPyObject(in);
        r.imag = Math.atan2(x.imag, x.real);
        r.real = Math.log(hypot(x.real, x.imag));
        return (r);
    }

    public static PyComplex log10(PyObject in) {
        PyComplex r = new PyComplex(0.0, 0.0);
        PyComplex x = complexFromPyObject(in);
        double l = hypot(x.real, x.imag);
        r.imag = Math.atan2(x.imag, x.real) / Math.log(10.0);
        r.real = math.log10(new PyFloat(l));
        return (r);
    }

    public static PyComplex sin(PyObject in) {
        PyComplex r = new PyComplex(0.0, 0.0);
        PyComplex x = complexFromPyObject(in);
        r.real = Math.sin(x.real) * math.cosh(x.imag);
        r.imag = Math.cos(x.real) * math.sinh(x.imag);
        return (r);
    }

    public static PyComplex sinh(PyObject in) {
        PyComplex r = new PyComplex(0.0, 0.0);
        PyComplex x = complexFromPyObject(in);
        r.real = Math.cos(x.imag) * math.sinh(x.real);
        r.imag = Math.sin(x.imag) * math.cosh(x.real);
        return (r);
    }

    public static PyComplex sqrt(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex r = new PyComplex(0.0, 0.0);

        if ((x.real != 0.0) || (x.imag != 0.0)) {
            double s = Math
                    .sqrt(0.5 * (Math.abs(x.real) + hypot(x.real, x.imag)));
            double d = 0.5 * x.imag / s;

            if (x.real > 0) {
                r.real = s;
                r.imag = d;
            } else if (x.imag >= 0) {
                r.real = d;
                r.imag = s;
            } else {
                r.real = -d;
                r.imag = -s;
            }
        }
        return (r);
    }

    public static PyComplex tan(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex r = new PyComplex(0.0, 0.0);

        double sr = Math.sin(x.real);
        double cr = Math.cos(x.real);
        double shi = math.sinh(x.imag);
        double chi = math.cosh(x.imag);
        double rs = sr * chi;
        double is = cr * shi;
        double rc = cr * chi;
        double ic = -sr * shi;
        double d = rc * rc + ic * ic;
        r.real = ((rs * rc) + (is * ic)) / d;
        r.imag = ((is * rc) - (rs * ic)) / d;

        return (r);
    }

    public static PyComplex tanh(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex r = new PyComplex(0.0, 0.0);

        double si = Math.sin(x.imag);
        double ci = Math.cos(x.imag);
        double shr = math.sinh(x.real);
        double chr = math.cosh(x.real);
        double rs = ci * shr;
        double is = si * chr;
        double rc = ci * chr;
        double ic = si * shr;
        double d = rc * rc + ic * ic;
        r.real = ((rs * rc) + (is * ic)) / d;
        r.imag = ((is * rc) - (rs * ic)) / d;

        return (r);
    }
}
