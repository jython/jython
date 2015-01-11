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
    /**
     * For x larger than this, <i>e<sup>-x</sup></i> is negligible compared with
     * <i>e<sup>x</sup></i>, or equivalently 1 is negligible compared with <i>e<sup>2x</sup></i>, in
     * IEEE-754 floating point. Beyond this, sinh <i>x</i> and cosh <i>x</i> are adequately
     * approximated by 0.5<i>e<sup>x</sup></i>. The smallest theoretical value is 27 ln(2).
     */
    private static final double ATLEAST_27LN2 = 18.72;
    private static final double HALF_E2 = 0.5 * Math.E * Math.E;

    /** log<sub>10</sub>e (Ref: Abramowitz &amp; Stegun [1972], p3). */
    private static final double LOG10E = 0.43429448190325182765;

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

    /**
     * Return the cosine of z.
     *
     * @param z
     * @return cos <i>z</i>
     */
    public static PyComplex cos(PyObject z) {
        return cosOrCosh(complexFromPyObject(z), false);
    }

    /**
     * Return the hyperbolic cosine of z.
     *
     * @param z
     * @return cosh <i>z</i>
     */
    public static PyComplex cosh(PyObject z) {
        return cosOrCosh(complexFromPyObject(z), true);
    }

    /**
     * Helper to compute either cos <i>z</i> or cosh <i>z</i>.
     *
     * @param z
     * @param h <code>true</code> to compute cosh <i>z</i>, <code>false</code> to compute cos
     *            <i>z</i>.
     * @return
     */
    private static PyComplex cosOrCosh(PyComplex z, boolean h) {
        double x, y, u, v;
        PyComplex w;

        if (h) {
            // We compute w = cosh(z). Let w = u + iv and z = x + iy.
            x = z.real;
            y = z.imag;
            // Then the function body computes cosh(x+iy), according to:
            // u = cosh(x) cos(y),
            // v = sinh(x) sin(y),
            // And we return w = u + iv.
        } else {
            // We compute w = sin(z). Unusually, let z = y - ix, so x + iy = iz.
            y = z.real;
            x = -z.imag;
            // Then the function body computes cosh(x+iy) = cosh(iz) = cos(z) as before.
        }

        if (y == 0.) {
            // Real argument for cosh (or imaginary for cos): use real library.
            u = math.cosh(x);   // This will raise a range error on overflow.
            // v is zero but follows the sign of x*y (in which y could be -0.0).
            v = Math.copySign(1., x) * y;

        } else if (x == 0.) {
            // Imaginary argument for cosh (or real for cos): imaginary result at this point.
            u = Math.cos(y);
            // v is zero but follows the sign of x*y (in which x could be -0.0).
            v = x * Math.copySign(1., y);

        } else {

            // The trig calls will not throw, although if y is infinite, they return nan.
            double cosy = Math.cos(y), siny = Math.sin(y), absx = Math.abs(x);

            if (absx == Double.POSITIVE_INFINITY) {
                if (!Double.isNaN(cosy)) {
                    // w = (inf,inf), but "rotated" by the direction cosines.
                    u = absx * cosy;
                    v = x * siny;
                } else {
                    // Provisionally w = (inf,nan), which will raise domain error if y!=nan.
                    u = absx;
                    v = Double.NaN;
                }

            } else if (absx > ATLEAST_27LN2) {
                // Use 0.5*e**x approximation. This is also the region where we risk overflow.
                double r = Math.exp(absx - 2.);
                // r approximates 2cosh(x)/e**2: multiply in this order to avoid inf:
                u = r * cosy * HALF_E2;
                // r approximates 2sinh(|x|)/e**2: put back the proper sign of x in passing.
                v = Math.copySign(r, x) * siny * HALF_E2;
                if (Double.isInfinite(u) || Double.isInfinite(v)) {
                    // A finite x gave rise to an infinite u or v.
                    throw math.mathRangeError();
                }

            } else {
                // Normal case, without risk of overflow.
                u = Math.cosh(x) * cosy;
                v = Math.sinh(x) * siny;
            }
        }

        // Compose the result w = u + iv.
        w = new PyComplex(u, v);

        // If that generated a nan, and there wasn't one in the argument, raise a domain error.
        return exceptNaN(w, z);
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

    /**
     * Returns the natural logarithm of <i>w</i>.
     *
     * @param w
     * @return ln <i>w</i>
     */
    public static PyComplex log(PyObject w) {
        PyComplex ww = complexFromPyObject(w);
        double u = ww.real, v = ww.imag;
        // The real part of the result is the log of the magnitude.
        double lnr = logHypot(u, v);
        // The imaginary part of the result is the arg. This may result in a nan.
        double theta = Math.atan2(v, u);
        PyComplex z = new PyComplex(lnr, theta);
        return exceptNaN(z, ww);
    }

    /**
     * Returns the common logarithm of <i>w</i> (base 10 logarithm).
     *
     * @param w
     * @return log<sub>10</sub><i>w</i>
     */
    public static PyComplex log10(PyObject w) {
        PyComplex ww = complexFromPyObject(w);
        double u = ww.real, v = ww.imag;
        // The expression is the same as for base e, scaled in magnitude.
        double logr = logHypot(u, v) * LOG10E;
        double theta = Math.atan2(v, u) * LOG10E;
        PyComplex z = new PyComplex(logr, theta);
        return exceptNaN(z, ww);
    }

    /**
     * Returns the logarithm of <i>w</i> to the given base. If the base is not specified, returns
     * the natural logarithm of <i>w</i>. There is one branch cut, from 0 along the negative real
     * axis to -&infin;, continuous from above.
     *
     * @param w
     * @param b
     * @return log<sub>b</sub><i>w</i>
     */
    public static PyComplex log(PyObject w, PyObject b) {
        PyComplex ww = complexFromPyObject(w), bb = complexFromPyObject(b), z;
        double u = ww.real, v = ww.imag, br = bb.real, bi = bb.imag, x, y;
        // Natural log of w is (x,y)
        x = logHypot(u, v);
        y = Math.atan2(v, u);

        if (bi != 0. || br <= 0.) {
            // Complex or negative real base requires complex log: general case.
            PyComplex lnb = log(bb);
            z = (PyComplex)(new PyComplex(x, y)).__div__(lnb);

        } else {
            // Real positive base: frequent case. (b = inf or nan ends up here too.)
            double lnb = Math.log(br);
            z = new PyComplex(x / lnb, y / lnb);
        }

        return exceptNaN(z, ww);
    }

    /**
     * Helper function for the log of a complex number, dealing with the log magnitude, and without
     * intermediate overflow or underflow. It returns ln <i>r</i>, where <i>r<sup>2</sup> =
     * u<sup>2</sup>+v<sup>2</sup></i>. To do this it computes
     * &frac12;ln(u<sup>2</sup>+v<sup>2</sup>). Special cases are handled as follows:
     * <ul>
     * <li>if u or v is NaN, it returns NaN</li>
     * <li>if u or v is infinite, it returns positive infinity</li>
     * <li>if u and v are both zero, it raises a ValueError</li>
     * </ul>
     * We have this function instead of <code>Math.log(Math.hypot(u,v))</code> because a valid
     * result is still possible even when <code>hypot(u,v)</code> overflows, and because there's no
     * point in taking a square root when a log is to follow.
     *
     * @param u
     * @param v
     * @return &frac12;ln(u<sup>2</sup>+v<sup>2</sup>)
     */
    private static double logHypot(double u, double v) {

        if (Double.isInfinite(u) || Double.isInfinite(v)) {
            return Double.POSITIVE_INFINITY;

        } else {
            // Cannot overflow, but if u=v=0 will return -inf.
            int scale = 0, ue = Math.getExponent(u), ve = Math.getExponent(v);
            double lnr;

            if (ue < -511 && ve < -511) {
                // Both u and v are too small to square, or zero. (Just one would be ok.)
                scale = 600;
            } else if (ue > 510 || ve > 510) {
                // One of these is too big to square and double (or is nan or inf).
                scale = -600;
            }

            if (scale == 0) {
                // Normal case: there is no risk of overflow or log of zero.
                lnr = 0.5 * Math.log(u * u + v * v);
            } else {
                // We must work with scaled values, us = u * 2**n etc..
                double us = Math.scalb(u, scale);
                double vs = Math.scalb(v, scale);
                // rs**2 = r**2 * 2**2n
                double rs2 = us * us + vs * vs;
                // So ln(r) = ln(u**2+v**2)/2 = ln(us**2+vs**2)/2 - n ln(2)
                lnr = 0.5 * Math.log(rs2) - scale * math.LN2;
            }

            // (u,v) = 0 leads to ln(r) = -inf, but that's a domain error
            if (lnr == Double.NEGATIVE_INFINITY) {
                throw math.mathDomainError();
            } else {
                return lnr;
            }
        }
    }

    /**
     * Return the sine of z.
     *
     * @param z
     * @return sin <i>z</i>
     */
    public static PyComplex sin(PyObject z) {
        return sinOrSinh(complexFromPyObject(z), false);
    }

    /**
     * Return the hyperbolic sine of z.
     *
     * @param z
     * @return sinh <i>z</i>
     */
    public static PyComplex sinh(PyObject z) {
        return sinOrSinh(complexFromPyObject(z), true);
    }

    /**
     * Helper to compute either sin <i>z</i> or sinh <i>z</i>.
     *
     * @param z
     * @param h <code>true</code> to compute sinh <i>z</i>, <code>false</code> to compute sin
     *            <i>z</i>.
     * @return
     */
    private static PyComplex sinOrSinh(PyComplex z, boolean h) {
        double x, y, u, v;
        PyComplex w;

        if (h) {
            // We compute w = sinh(z). Let w = u + iv and z = x + iy.
            x = z.real;
            y = z.imag;
            // Then the function body computes sinh(x+iy), according to:
            // u = sinh(x) cos(y),
            // v = cosh(x) sin(y),
            // And we return w = u + iv.
        } else {
            // We compute w = sin(z). Unusually, let z = y - ix, so x + iy = iz.
            y = z.real;
            x = -z.imag;
            // Then the function body computes sinh(x+iy) = sinh(iz) = i sin(z) as before,
            // but we finally return w = v - iu = sin(z).
        }

        if (y == 0.) {
            // Real argument for sinh (or imaginary for sin): use real library.
            u = math.sinh(x);   // This will raise a range error on overflow.
            // v follows the sign of y (which could be -0.0).
            v = y;

        } else if (x == 0.) {
            // Imaginary argument for sinh (or real for sin): imaginary result at this point.
            v = Math.sin(y);
            // u follows sign of x (which could be -0.0).
            u = x;

        } else {

            // The trig calls will not throw, although if y is infinite, they return nan.
            double cosy = Math.cos(y), siny = Math.sin(y), absx = Math.abs(x);

            if (absx == Double.POSITIVE_INFINITY) {
                if (!Double.isNaN(cosy)) {
                    // w = (inf,inf), but "rotated" by the direction cosines.
                    u = x * cosy;
                    v = absx * siny;
                } else {
                    // Provisionally w = (inf,nan), which will raise domain error if y!=nan.
                    u = x;
                    v = Double.NaN;
                }

            } else if (absx > ATLEAST_27LN2) {
                // Use 0.5*e**x approximation. This is also the region where we risk overflow.
                double r = Math.exp(absx - 2.);
                // r approximates 2cosh(x)/e**2: multiply in this order to avoid inf:
                v = r * siny * HALF_E2;
                // r approximates 2sinh(|x|)/e**2: put back the proper sign of x in passing.
                u = Math.copySign(r, x) * cosy * HALF_E2;
                if (Double.isInfinite(u) || Double.isInfinite(v)) {
                    // A finite x gave rise to an infinite u or v.
                    throw math.mathRangeError();
                }

            } else {
                // Normal case, without risk of overflow.
                u = Math.sinh(x) * cosy;
                v = Math.cosh(x) * siny;
            }
        }

        // Compose the result w according to whether we're computing sin(z) or sinh(z).
        if (h) {
            w = new PyComplex(u, v);    // w = u + iv = sinh(x+iy).
        } else {
            w = new PyComplex(v, -u);   // w = v - iu = sin(y-ix) = sin(z)
        }

        // If that generated a nan, and there wasn't one in the argument, raise a domain error.
        return exceptNaN(w, z);
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

    /**
     * Return the tangent of z.
     *
     * @param z
     * @return tan <i>z</i>
     */
    public static PyComplex tan(PyObject z) {
        return tanOrTanh(complexFromPyObject(z), false);
    }

    /**
     * Return the hyperbolic tangent of z.
     *
     * @param z
     * @return tanh <i>z</i>
     */
    public static PyComplex tanh(PyObject z) {
        return tanOrTanh(complexFromPyObject(z), true);
    }

    /**
     * Helper to compute either tan <i>z</i> or tanh <i>z</i>. The expression used is:
     * <p>
     * tanh(<i>x+iy</i>) = (sinh <i>x</i> cosh <i>x + i</i> sin <i>y</i> cos <i>y</i>) /
     * (sinh<sup>2</sup><i>x +</i> cos<sup>2</sup><i>y</i>)
     * <p>
     * A simplification is made for x sufficiently large that <i>e<sup>2|x|</sup></i>&#x0226B;1 that
     * deals satisfactorily with large or infinite <i>x</i>. When computing tan, we evaluate
     * <i>i</i> tan <i>iz</i> instead, and the approximation applies to
     * <i>e<sup>2|y|</sup></i>&#x0226B;1.
     *
     * @param z
     * @param h <code>true</code> to compute tanh <i>z</i>, <code>false</code> to compute tan
     *            <i>z</i>.
     * @return tan or tanh <i>z</i>
     */
    private static PyComplex tanOrTanh(PyComplex z, boolean h) {
        double x, y, u, v, s;
        PyComplex w;

        if (h) {
            // We compute w = tanh(z). Let w = u + iv and z = x + iy.
            x = z.real;
            y = z.imag;
            // Then the function body computes tanh(x+iy), according to:
            // s = sinh**2 x + cos**2 y
            // u = sinh x cosh x / s,
            // v = sin y cos y / s,
            // And we return w = u + iv.
        } else {
            // We compute w = tan(z). Unusually, let z = y - ix, so x + iy = iz.
            y = z.real;
            x = -z.imag;
            // Then the function body computes tanh(x+iy) = tanh(iz) = i tan(z) as before,
            // but we finally return w = v - iu = tan(z).
        }

        if (y == 0.) {
            // Real argument for tanh (or imaginary for tan).
            u = Math.tanh(x);
            // v is zero but follows the sign of y (which could be -0.0).
            v = y;

        } else if (x == 0. && !Double.isNaN(y)) {
            // Imaginary argument for tanh (or real for tan): imaginary result at this point.
            v = Math.tan(y); // May raise domain error
            // u is zero but follows sign of x (which could be -0.0).
            u = x;

        } else {
            // The trig calls will not throw, although if y is infinite, they return nan.
            double cosy = Math.cos(y), siny = Math.sin(y), absx = Math.abs(x);

            if (absx > ATLEAST_27LN2) {
                // e**2x is much greater than 1: exponential approximation to sinh and cosh.
                s = 0.25 * Math.exp(2 * absx);
                u = Math.copySign(1., x);
                if (s == Double.POSITIVE_INFINITY) {
                    // Either x is inf or 2x is large enough to overflow exp(). v=0, but signed:
                    v = Math.copySign(0., siny * cosy);
                } else {
                    v = siny * cosy / s;
                }

            } else {
                // Normal case: possible overflow in s near (x,y) = (0,pi/2) is harmless.
                double sinhx = Math.sinh(x), coshx = Math.cosh(x);
                s = sinhx * sinhx + cosy * cosy;
                u = sinhx * coshx / s;
                v = siny * cosy / s;
            }
        }

        // Compose the result w according to whether we're computing tan(z) or tanh(z).
        if (h) {
            w = new PyComplex(u, v);    // w = u + iv = tanh(x+iy).
        } else {
            w = new PyComplex(v, -u);   // w = v - iu = tan(y-ix) = tan(z)
        }

        // If that generated a nan, and there wasn't one in the argument, raise a domain error.
        return exceptNaN(w, z);
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
