package org.python.modules;

import org.python.core.Py;
import org.python.core.PyComplex;
import org.python.core.PyException;
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

    /** 2<sup>-&#189;</sup> (Ref: Abramowitz &amp; Stegun [1972], p2). */
    private static final double ROOT_HALF = 0.70710678118654752440;
    /** ln({@link Double#MAX_VALUE}) or a little less */
    private static final double NEARLY_LN_DBL_MAX = 709.4361393;

    private static PyComplex c_prodi(PyComplex x) {
        return (PyComplex)x.__mul__(i);
    }

    private static boolean isNaN(PyComplex x) {
        return Double.isNaN(x.real) || Double.isNaN(x.imag);
    }

    private static double abs(PyComplex x) {
        boolean isNaN = isNaN(x);
        boolean isInfinite = !isNaN && (Double.isInfinite(x.real) || Double.isInfinite(x.imag));
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
        return ((PyComplex)r.__add__(r));
    }

    public static PyComplex asin(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex squared = (PyComplex)x.__mul__(x);
        PyComplex sq1_minus_xsq = sqrt(one.__sub__(squared));
        return (PyComplex)c_prodi(log(sq1_minus_xsq.__add__(c_prodi(x)))).__neg__();
    }

    public static PyComplex asinh(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        PyComplex a = sqrt(x.__add__(i));
        PyComplex b = sqrt(x.__sub__(i));
        PyComplex z = sqrt(half);
        PyComplex r = log(z.__mul__(a.__add__(b)));
        return ((PyComplex)r.__add__(r));
    }

    public static PyComplex atan(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return (PyComplex)half_i.__mul__(log(i.__add__(x).__div__(i.__sub__(x))));
    }

    public static PyComplex atanh(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return (PyComplex)half.__mul__(log(one.__add__(x).__div__(one.__sub__(x))));
    }

    public static PyComplex cos(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return new PyComplex(Math.cos(x.real) * math.cosh(x.imag), -Math.sin(x.real)
                * math.sinh(x.imag));
    }

    public static PyComplex cosh(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return new PyComplex(Math.cos(x.imag) * math.cosh(x.real), Math.sin(x.imag)
                * math.sinh(x.real));
    }

    /**
     * Return the exponential value e<sup>z</sup>.
     *
     * @param z
     * @return e<sup>z</sup>
     */
    public static PyComplex exp(PyObject z) {
        PyComplex zz = complexFromPyObject(z);
        double x = zz.real, y = zz.imag, r, u, v;
        /*
         * This has a lot of corner-cases, and some of them make little sense sense, but it matches
         * CPython and passes the regression tests.
         */
        if (y == 0.) {
            // Real value: use a real solution. (This may raise a range error.)
            u = math.exp(x);
            // v follows sign of y.
            v = y;

        } else {
            // The trig calls will not throw, although if y is infinite, they return nan.
            double cosy = Math.cos(y), siny = Math.sin(y);

            if (x == Double.NEGATIVE_INFINITY) {
                // w = (0,0) but "signed" by the direction cosines (even in they are nan).
                u = Math.copySign(0., cosy);
                v = Math.copySign(0., siny);

            } else if (x == Double.POSITIVE_INFINITY) {
                if (!Double.isNaN(cosy)) {
                    // w = (inf,inf), but "signed" by the direction cosines.
                    u = Math.copySign(x, cosy);
                    v = Math.copySign(x, siny);
                } else {
                    // Provisionally w = (inf,nan), which will raise domain error if y!=nan.
                    u = x;
                    v = Double.NaN;
                }

            } else if (x > NEARLY_LN_DBL_MAX) {
                // r = e**x would overflow but maybe not r*cos(y) and r*sin(y).
                r = Math.exp(x - 1); // = r / e
                u = r * cosy * Math.E;
                v = r * siny * Math.E;
                if (Double.isInfinite(u) || Double.isInfinite(v)) {
                    // A finite x gave rise to an infinite u or v.
                    throw math.mathRangeError();
                }

            } else {
                // Normal case, without risk of overflow.
                // Compute r = exp(x), and return w = u + iv = r (cos(y) + i*sin(y))
                r = Math.exp(x);
                u = r * cosy;
                v = r * siny;
            }
        }
        // If that generated a nan, and there wasn't one in the argument, raise domain error.
        return exceptNaN(new PyComplex(u, v), zz);
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
        return new PyComplex(Math.log(abs(x)), Math.atan2(x.imag, x.real));
    }

    public static double phase(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return Math.atan2(x.imag, x.real);
    }

    public static PyTuple polar(PyObject in) {
        PyComplex z = complexFromPyObject(in);
        double phi = Math.atan2(z.imag, z.real);
        double r = math.hypot(z.real, z.imag);
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

        return new PyComplex(r * Math.cos(phi), r * Math.sin(phi));
    }

    /**
     * @param in
     *
     * @return <code>true</code> if in.real or in.imag is positive or negative infinity
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
        return new PyComplex(math.log10(new PyFloat(l)), Math.atan2(x.imag, x.real)
                / Math.log(10.0));
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
        return (PyComplex)new PyComplex(math.log(new PyFloat(l)), Math.atan2(x.imag, x.real))
                .__div__(log_base);
    }

    public static PyComplex sin(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return new PyComplex(Math.sin(x.real) * math.cosh(x.imag), Math.cos(x.real)
                * math.sinh(x.imag));
    }

    public static PyComplex sinh(PyObject in) {
        PyComplex x = complexFromPyObject(in);
        return new PyComplex(Math.cos(x.imag) * math.sinh(x.real), Math.sin(x.imag)
                * math.cosh(x.real));
    }

    /**
     * Calculate <i>z = x+iy</i>, such that <i>z<sup>2</sup> = w</i>. In taking the square roots to
     * get <i>x</i> and <i>y</i>, we choose to have <i>x&ge;0</i> always, and <i>y</i> the same sign
     * as <i>v</i>.
     *
     * @param w to square-root
     * @return <i>w<sup>&#189;</sup></i>
     */
    public static PyComplex sqrt(PyObject w) {
        /*
         * All the difficult parts are written for the first quadrant only (+,+), then the true sign
         * of the parts of w are factored in at the end, by flipping the result around the
         * diagonals.
         */
        PyComplex ww = complexFromPyObject(w);
        double u = Math.abs(ww.real), v = Math.abs(ww.imag), x, y;

        if (Double.isInfinite(u)) {
            // Special cases: u = inf
            x = Double.POSITIVE_INFINITY;
            y = (Double.isNaN(v) || Double.isInfinite(v)) ? v : 0.;

        } else if (Double.isInfinite(v)) {
            // Special cases: v = inf, u != inf
            x = y = Double.POSITIVE_INFINITY;

        } else if (Double.isNaN(u)) {
            // In the remaining cases, u == nan infects all.
            x = y = u;

        } else {

            if (v == 0.) {
                // Pure real (and positive since in first quadrant).
                x = (u == 0.) ? 0. : Math.sqrt(u);
                y = 0.;

            } else if (u == 0.) {
                // Pure imaginary, and v is positive.
                x = y = ROOT_HALF * Math.sqrt(v);

            } else {
                /*
                 * Let w = u + iv = 2a + 2ib, and define s**2 = a**2 + b**2. Then z = x + iy is
                 * computed as x**2 = s + a, and y = b /x. Most of the logic here is about managing
                 * the scaling.
                 */
                int ue = Math.getExponent(u), ve = Math.getExponent(v);
                int diff = ue - ve;

                if (diff > 27) {
                    // u is so much bigger than v we can ignore v in the square: s = u/2.
                    x = Math.sqrt(u);

                } else if (diff < -27) {
                    // v is so much bigger than u we can ignore u in the square: s = v/2.
                    if (ve >= Double.MAX_EXPONENT) {
                        x = Math.sqrt(0.5 * u + 0.5 * v); // Avoid overflow in u+v
                    } else {
                        x = Math.sqrt(0.5 * (u + v));
                    }

                } else {
                    /*
                     * Use the full-fat formula: s = Math.sqrt(a * a + b * b). During calculation,
                     * we will be squaring the components, so we scale by 2**n (small values up and
                     * large values down).
                     */
                    double s, a, b;
                    final int LARGE = 510;  // 1.999... * 2**LARGE is safe to square and double
                    final int SMALL = -510; // 1.0 * 2**(SMALL-1) may squared with full precision
                    final int SCALE = 600;  // EVEN and > (52+SMALL-Double.MIN_EXPONENT)
                    int n = 0;
                    if (ue > LARGE || ve > LARGE) {
                        // One of these is too big to square without overflow.
                        a = Math.scalb(u, -(SCALE + 1));   // a = (u/2) * 2**n
                        b = Math.scalb(v, -(SCALE + 1));
                        n = -SCALE;
                    } else if (ue < SMALL && ve < SMALL) {
                        // Both of these are too small to square without loss of bits.
                        a = Math.scalb(u, SCALE - 1);   // a = (u/2) * 2**n
                        b = Math.scalb(v, SCALE - 1);
                        n = SCALE;
                    } else {
                        a = 0.5 * u;                // a = u/2
                        b = 0.5 * v;
                    }

                    s = Math.sqrt(a * a + b * b);
                    x = Math.sqrt(s + a);

                    // Restore x through the square root of the scale 2**(-n/2)
                    if (n != 0) {
                        x = Math.scalb(x, -n / 2);
                    }
                }

                // Finally, use y = v/2x
                y = v / (x + x);
            }
        }

        // Flip according to the signs of the components of w.
        if (ww.real < 0.) {
            return new PyComplex(y, Math.copySign(x, ww.imag));
        } else {
            return new PyComplex(x, Math.copySign(y, ww.imag));
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

        return new PyComplex(((rs * rc) + (is * ic)) / d, ((is * rc) - (rs * ic)) / d);
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

        return new PyComplex(((rs * rc) + (is * ic)) / d, ((is * rc) - (rs * ic)) / d);
    }

    /**
     * Turn a <code>NaN</code> result into a thrown <code>ValueError</code>, a math domain error, if
     * the original argument was not itself <code>NaN</code>. A <code>PyComplex</code> is a
     * <code>NaN</code> if either component is a <code>NaN</code>.
     *
     * @param result to return (if we return)
     * @param arg to include in check
     * @return result if <code>arg</code> was <code>NaN</code> or <code>result</code> was not
     *         <code>NaN</code>
     * @throws PyException (ValueError) if <code>result</code> was <code>NaN</code> and
     *             <code>arg</code> was not <code>NaN</code>
     */
    private static PyComplex exceptNaN(PyComplex result, PyComplex arg) throws PyException {
        if ((Double.isNaN(result.real) || Double.isNaN(result.imag))
                && !(Double.isNaN(arg.real) || Double.isNaN(arg.imag))) {
            throw math.mathDomainError();
        } else {
            return result;
        }
    }

    /**
     * Turn an infinite result into a thrown <code>OverflowError</code>, a math range error, if the
     * original argument was not itself infinite. A <code>PyComplex</code> is infinite if either
     * component is infinite.
     *
     * @param result to return (if we return)
     * @param arg to include in check
     * @return result if <code>arg</code> was infinite or <code>result</code> was not infinite
     * @throws PyException (ValueError) if <code>result</code> was infinite and <code>arg</code> was
     *             not infinite
     */
    private static PyComplex exceptInf(PyComplex result, PyComplex arg) {
        if ((Double.isInfinite(result.real) || Double.isInfinite(result.imag))
                && !(Double.isInfinite(arg.real) || Double.isInfinite(arg.imag))) {
            throw math.mathRangeError();
        } else {
            return result;
        }
    }

}
