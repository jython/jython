package org.python.modules;

import org.python.core.Py;
import org.python.core.PyComplex;
import org.python.core.PyFloat;
import org.python.core.PyInstance;
import org.python.core.PyObject;
import org.python.core.PyTuple;

public class cmath {
    public static final PyFloat pi = new PyFloat(Math.PI);
    public static final PyFloat e = new PyFloat(Math.E);

    private static final PyComplex one = new PyComplex(1.0, 0.0);
    private static final PyComplex half = new PyComplex(0.5, 0.0);
    private static final PyComplex i = new PyComplex(0.0, 1.0);
    private static final PyComplex half_i = new PyComplex(0.0, 0.5);

//    private static PyComplex c_prodi(PyComplex x) {
//        return (new PyComplex(-x.imag, x.real));
//    }

    private static PyComplex c_prodi(PyComplex x) {
        return (PyComplex) x.__mul__(i);
    }


    private static boolean isNaN(PyComplex x) {
        return Double.isNaN(x.real) || Double.isNaN(x.imag);
    }

    private static double abs(PyComplex x) {
        boolean isNaN = isNaN(x);
        boolean isInfinite = !isNaN &&
                (Double.isInfinite(x.real) || Double.isInfinite(x.imag));
        if (isNaN) {
            return Double.NaN;
        }
        if (isInfinite) {
            return Double.POSITIVE_INFINITY;
        }
        double real_abs = Math.abs(x.real);
        double imag_abs = Math.abs(x.imag);

        if (real_abs < imag_abs) {
            if (x.imag == 0.0) {
                return real_abs;
            }
            double q = x.real / x.imag;
            return imag_abs * Math.sqrt(1 + q * q);
        } else {
            if (x.real == 0.0) {
                return imag_abs;
            }
            double q = x.imag / x.real;
            return real_abs * Math.sqrt(1 + q * q);
        }
    }

    private static PyComplex complexFromPyObject(PyObject obj) {
        // If op is already of type PyComplex_Type, return its value
        if (obj instanceof PyComplex) {
            return (PyComplex)obj;
        }

        // If not, use op's __complex__ method, if it exists
        PyObject newObj = null;
        if (obj instanceof PyInstance) {
            // this can go away in python 3000
            if (obj.__findattr__("__complex__") != null) {
                newObj = obj.invoke("__complex__");
            }
            // else try __float__
        } else {
            PyObject complexFunc = obj.getType().lookup("__complex__");
            if (complexFunc != null) {
                newObj = complexFunc.__call__(obj);
            }
        }

        if (newObj != null) {
            if (!(newObj instanceof PyComplex)) {
                throw Py.TypeError("__complex__ should return a complex object");
            }
            return (PyComplex)newObj;
        }

        // If neither of the above works, interpret op as a float giving the real part of
        // the result, and fill in the imaginary part as 0
        return new PyComplex(obj.asDouble(), 0);
    }
    
    public static PyObject acos(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return c_prodi(log(x.__add__(i.__mul__(sqrt(one.__sub__(x.__mul__(x))))))).__neg__();
    }

    public static PyComplex acosh(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex a = sqrt(x.__sub__(one));
        PyComplex b = sqrt(x.__add__(one));
        PyComplex c = sqrt(half);
        PyComplex r = log(c.__mul__(b.__add__(a)));
        return ((PyComplex) r.__add__(r));
    }

    public static PyComplex asin(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex squared = (PyComplex) x.__mul__(x);
        PyComplex sq1_minus_xsq = sqrt(one.__sub__(squared));
        return (PyComplex) c_prodi(log(sq1_minus_xsq.__add__(c_prodi(x)))).__neg__();
    }

    public static PyComplex asinh(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex a = sqrt(x.__add__(i));
        PyComplex b = sqrt(x.__sub__(i));
        PyComplex z = sqrt(half);
        PyComplex r = log(z.__mul__(a.__add__(b)));
        return ((PyComplex) r.__add__(r));
    }

    public static PyComplex atan(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return (PyComplex) half_i.__mul__(log(i.__add__(x).__div__(
                i.__sub__(x))));
    }

    public static PyComplex atanh(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return (PyComplex) half.__mul__(log(one.__add__(x).__div__(
                one.__sub__(x))));
    }

    public static PyComplex cos(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return new PyComplex(
                Math.cos(x.real) * math.cosh(x.imag),
                -Math.sin(x.real) * math.sinh(x.imag));
    }

    public static PyComplex cosh(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return new PyComplex(
                Math.cos(x.imag) * math.cosh(x.real),
                Math.sin(x.imag) * math.sinh(x.real));
    }

    public static PyComplex exp(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        double l = Math.exp(x.real);
        return new PyComplex(
                l * Math.cos(x.imag),
                l * Math.sin(x.imag));
    }

    public static PyComplex log(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        if (isNaN(x)) {
            if (Double.isInfinite(x.real) || Double.isInfinite(x.imag)) {
                return new PyComplex(Double.POSITIVE_INFINITY, Double.NaN);
            } else {
                return PyComplex.NaN;
            }
        }
        return new PyComplex(
                Math.log(abs(x)),
                Math.atan2(x.imag, x.real));
    }

    public static double phase(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return Math.atan2(x.imag, x.real);
    }

    public static PyTuple polar(PyObject in) {
        PyComplex z = complexFromPyObject(in);
        if ((Double.isInfinite(z.real) && Double.isNaN(z.imag)) ||
            (Double.isInfinite(z.imag) && Double.isNaN(z.real))) {
            return new PyTuple(Py.newFloat(Double.POSITIVE_INFINITY), Py.newFloat(Double.NaN));
        }
        double phi = Math.atan2(z.imag, z.real);
        double r = Math.sqrt(z.real*z.real + z.imag*z.imag);
        return new PyTuple(new PyFloat(r), new PyFloat(phi));
    }

    public static PyComplex rect(double r, double phi) {
        // Handle various edge cases
        if (Double.isInfinite(r) && (Double.isInfinite(phi) || Double.isNaN(phi))) {
            return new PyComplex(Double.POSITIVE_INFINITY, Double.NaN);
        }
        if (phi == 0.0) { // NB this test will succeed if phi is 0.0 or -0.0
            if (Double.isNaN(r)) {
                return new PyComplex(Double.NaN, 0.0);
            } else if (r == Double.POSITIVE_INFINITY) {
                return new PyComplex(r, phi);
            } else if (r == Double.NEGATIVE_INFINITY) {
                return new PyComplex(r, -phi);
            }
        }
        if (r == 0.0 && (Double.isInfinite(phi) || Double.isNaN(phi))) {
            return new PyComplex(0.0, 0.0);
        }

        return new PyComplex(
                r * Math.cos(phi),
                r * Math.sin(phi));
    }

    /**
     * @param in 
     * 
     * @return <code>true</code> if in.real or in.imag is positive or negative
     *         infinity
     */
    public static boolean isinf(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return Double.isInfinite(x.real) || Double.isInfinite(x.imag);
    }

    /**
     * @param in 
     * 
     * @return <code>true</code> if in.real or in.imag is nan.
     */
    public static boolean isnan(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return Double.isNaN(x.real) || Double.isNaN(x.imag);
    }

    public static PyComplex log10(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        if (isNaN(x)) {
            if (Double.isInfinite(x.real) || Double.isInfinite(x.imag)) {
                return new PyComplex(Double.POSITIVE_INFINITY, Double.NaN);
            } else {
                return PyComplex.NaN;
            }
        }
        double l = abs(x);
        return new PyComplex(
                math.log10(new PyFloat(l)),
                Math.atan2(x.imag, x.real) / Math.log(10.0));
    }
                    
    public static PyComplex log(PyObject in, PyObject base) {
        return log(complexFromPyObject(in), complexFromPyObject(base));
    }

    public static PyComplex log(PyComplex x, PyComplex base) {
        if (isNaN(x)) {
            if (Double.isInfinite(x.real) || Double.isInfinite(x.imag)) {
                return new PyComplex(Double.POSITIVE_INFINITY, Double.NaN);
            } else {
                return PyComplex.NaN;
            }
        }
        double l = abs(x);
        PyComplex log_base = log(base);
        return (PyComplex) new PyComplex(
                math.log(new PyFloat(l)),
                Math.atan2(x.imag, x.real)).
                __div__(log_base);
    }

    public static PyComplex sin(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return new PyComplex(
                Math.sin(x.real) * math.cosh(x.imag),
                Math.cos(x.real) * math.sinh(x.imag));
    }

    public static PyComplex sinh(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return new PyComplex(
                Math.cos(x.imag) * math.sinh(x.real),
                Math.sin(x.imag) * math.cosh(x.real));
    }

    public static PyComplex sqrt(PyObject in) {
        PyComplex x = complexFromPyObject(in);

        if (Double.isInfinite(x.real) && Double.isNaN(x.imag)) {
            if (x.real == Double.NEGATIVE_INFINITY) {
                return new PyComplex(Double.NaN, Double.POSITIVE_INFINITY);
            } else {
                return new PyComplex(Double.POSITIVE_INFINITY, Double.NaN);
            }
        }

        if (x.imag == Double.POSITIVE_INFINITY) {
            return new PyComplex(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        } else if (x.imag == Double.NEGATIVE_INFINITY) {
            return new PyComplex(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        }

        if (x.real == 0.0 && x.imag == 0.0) {
            return new PyComplex(0.0, Math.copySign(0.0, x.imag));
        }

        double t = Math.sqrt((Math.abs(x.real) + abs(x)) / 2.0);
        if (x.real >= 0.0) {
            return new PyComplex(t, x.imag / (2.0 * t));
        } else {
            return new PyComplex(
                    Math.abs(x.imag) / (2.0 * t),
                    Math.copySign(1d, x.imag) * t);
        }
    }

    public static PyComplex tan(PyObject in) {
        PyComplex x = complexFromPyObject(in);

        double sr = Math.sin(x.real);
        double cr = Math.cos(x.real);
        double shi = math.sinh(x.imag);
        double chi = math.cosh(x.imag);
        double rs = sr * chi;
        double is = cr * shi;
        double rc = cr * chi;
        double ic = -sr * shi;
        double d = rc * rc + ic * ic;

        return new PyComplex(
                ((rs * rc) + (is * ic)) / d,
                ((is * rc) - (rs * ic)) / d);
    }

    public static PyComplex tanh(PyObject in) {
        PyComplex x = complexFromPyObject(in);

        double si = Math.sin(x.imag);
        double ci = Math.cos(x.imag);
        double shr = math.sinh(x.real);
        double chr = math.cosh(x.real);
        double rs = ci * shr;
        double is = si * chr;
        double rc = ci * chr;
        double ic = si * shr;
        double d = rc * rc + ic * ic;

        return new PyComplex(
                ((rs * rc) + (is * ic)) / d,
                ((is * rc) - (rs * ic)) / d);
    }
}
