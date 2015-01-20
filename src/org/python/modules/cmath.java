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

    /**
     * Return the arc cosine of w. There are two branch cuts. One extends right from 1 along the
     * real axis to &infin;, continuous from below. The other extends left from -1 along the real
     * axis to -&infin;, continuous from above.
     *
     * @param w
     * @return cos<sup>-1</sup><i>w</i>
     */
    public static PyComplex acos(PyObject w) {
        return _acos(complexFromPyObject(w));
    }

    /**
     * Helper to compute cos<sup>-1</sup><i>w</i>. The method used is as in CPython:
     * <p>
     * <i>a = (1-w)<sup>&frac12;</sup> = &radic;2</i> sin <i>z/2</i> <br>
     * <i>b = (1+w)<sup>&frac12;</sup> = &radic;2</i> cos <i>z/2</i>
     * <p>
     * Then, with <i>z = x+iy</i>, <i>a = a<sub>1</sub>+ia<sub>2</sub></i>, and <i>b =
     * b<sub>1</sub>+ib<sub>2</sub></i>,
     * <p>
     * a<sub>1</sub> / b<sub>1</sub> = tan <i>x/2</i> <br>
     * a<sub>2</sub>b<sub>1</sub> - a<sub>1</sub>b<sub>2</sub> = sinh <i>y</i>
     * <p>
     * and we use {@link Math#atan2(double, double)} and {@link math#asinh(double)} to obtain
     * <i>x</i> and <i>y</i>.
     * <p>
     * For <i>w</i> sufficiently large that <i>w<sup>2</sup></i>&#x0226B;1, cos<sup>-1</sup><i>w</i>
     * &asymp; -i ln(<i>2w</i>).
     *
     * @param w
     * @return cos<sup>-1</sup><i>w</i>
     */
    private static PyComplex _acos(PyComplex w) {

        // Let z = x + iy and w = u + iv.
        double x, y, u = w.real, v = w.imag;

        if (Math.abs(u) > 0x1p27 || Math.abs(v) > 0x1p27) {
            /*
             * w is large: approximate 2cos(z) by exp(i(x+iy)) or exp(-i(x+iy)), whichever
             * dominates. Hence, z = x+iy = i ln(2(u+iv)) or -i ln(2(u+iv))
             */
            x = Math.atan2(Math.abs(v), u);
            y = Math.copySign(logHypot(u, v) + math.LN2, -v);

        } else if (Double.isNaN(v)) {
            // Special cases
            x = (u == 0.) ? Math.PI / 2. : v;
            y = v;

        } else {
            // Normal case, without risk of overflow.
            PyComplex a = sqrt(new PyComplex(1. - u, -v)); // a = sqrt(1-w) = sqrt(2) sin(z/2)
            PyComplex b = sqrt(new PyComplex(1 + u, v));   // b = sqrt(1+w) = sqrt(2) cos(z/2)
            // Arguments here are sin(x/2)cosh(y/2), cos(x/2)cosh(y/2) giving tan(x/2)
            x = 2. * Math.atan2(a.real, b.real);
            // 2 (cos(x/2)**2+sin(x/2)**2) sinh(y/2)cosh(y/2) = sinh y
            y = math.asinh(a.imag * b.real - a.real * b.imag);
        }

        // If that generated a nan, and there wasn't one in the argument, raise a domain error.
        return exceptNaN(new PyComplex(x, y), w);
    }

    /**
     * Return the hyperbolic arc cosine of w. There is one branch cut, extending left from 1 along
     * the real axis to -&infin;, continuous from above.
     *
     * @param w
     * @return cosh<sup>-1</sup><i>w</i>
     */
    public static PyComplex acosh(PyObject w) {
        return _acosh(complexFromPyObject(w));
    }

    /**
     * Helper to compute z = cosh<sup>-1</sup><i>w</i>. The method used is as in CPython:
     * <p>
     * <i>a = (w-1)<sup>&frac12;</sup> = &radic;2</i> sinh <i>z/2</i> <br>
     * <i>b = (w+1)<sup>&frac12;</sup> = &radic;2</i> cosh <i>z/2</i>
     * <p>
     * Then, with <i>z = x+iy</i>, <i>a = a<sub>1</sub>+ia<sub>2</sub></i>, and <i>b =
     * b<sub>1</sub>+ib<sub>2</sub></i>,
     * <p>
     * a<sub>1</sub>b<sub>1</sub> + a<sub>2</sub>b<sub>2</sub> = sinh <i>x</i> <br>
     * a<sub>2</sub> / b<sub>1</sub> = tan <i>y/2</i>
     * <p>
     * and we use {@link math#asinh(double)} and {@link Math#atan2(double, double)} to obtain
     * <i>x</i> and <i>y</i>.
     * <p>
     * For <i>w</i> sufficiently large that <i>w<sup>2</sup></i>&#x0226B;1,
     * cosh<sup>-1</sup><i>w</i> &asymp; ln(<i>2w</i>). We do not use this method also to compute
     * cos<sup>-1</sup><i>w</i>, because the branch cuts do not correspond.
     *
     * @param w
     * @return cosh<sup>-1</sup><i>w</i>
     */
    private static PyComplex _acosh(PyComplex w) {

        // Let z = x + iy and w = u + iv.
        double x, y, u = w.real, v = w.imag;

        if (Math.abs(u) > 0x1p27 || Math.abs(v) > 0x1p27) {
            /*
             * w is large: approximate 2cosh(z) by exp(x+iy) or exp(-x-iy), whichever dominates.
             * Hence, z = x+iy = ln(2(u+iv)) or -ln(2(u+iv))
             */
            x = logHypot(u, v) + math.LN2;
            y = Math.atan2(v, u);

        } else if (v == 0. && !Double.isNaN(u)) {
            /*
             * We're on the real axis (and maybe the branch cut). u = cosh x cos y. In all cases,
             * the sign of y follows v.
             */
            if (u >= 1.) {
                // As real library, cos y = 1, u = cosh x.
                x = math.acosh(u);
                y = v;
            } else if (u < -1.) {
                // Left part of cut: cos y = -1, u = -cosh x
                x = math.acosh(-u);
                y = Math.copySign(Math.PI, v);
            } else {
                // -1 <= u <= 1: cosh x = 1, u = cos y.
                x = 0.;
                y = Math.copySign(Math.acos(u), v);
            }

        } else {
            // Normal case, without risk of overflow.
            PyComplex a = sqrt(new PyComplex(u - 1., v)); // a = sqrt(w-1) = sqrt(2) sinh(z/2)
            PyComplex b = sqrt(new PyComplex(u + 1., v)); // b = sqrt(w+1) = sqrt(2) cosh(z/2)
            // 2 sinh(x/2)cosh(x/2) (cos(y/2)**2+sin(y/2)**2) = sinh x
            x = math.asinh(a.real * b.real + a.imag * b.imag);
            // Arguments here are cosh(x/2)sin(y/2) and cosh(x/2)cos(y/2) giving tan y/2
            y = 2. * Math.atan2(a.imag, b.real);
        }

        // If that generated a nan, and there wasn't one in the argument, raise a domain error.
        return exceptNaN(new PyComplex(x, y), w);
    }

    /**
     * Return the arc sine of w. There are two branch cuts. One extends right from 1 along the real
     * axis to &infin;, continuous from below. The other extends left from -1 along the real axis to
     * -&infin;, continuous from above.
     *
     * @param w
     * @return sin<sup>-1</sup><i>w</i>
     */
    public static PyComplex asin(PyObject w) {
        return asinOrAsinh(complexFromPyObject(w), false);
    }

    /**
     * Return the hyperbolic arc sine of w. There are two branch cuts. One extends from 1j along the
     * imaginary axis to &infin;j, continuous from the right. The other extends from -1j along the
     * imaginary axis to -&infin;j, continuous from the left.
     *
     * @param w
     * @return sinh<sup>-1</sup><i>w</i>
     */
    public static PyComplex asinh(PyObject w) {
        return asinOrAsinh(complexFromPyObject(w), true);
    }

    /**
     * Helper to compute either sin<sup>-1</sup><i>w</i> or sinh<sup>-1</sup><i>w</i>. The method
     * used is as in CPython:
     * <p>
     * <i>a = (1-iw)<sup>&frac12;</sup> = &radic;2 </i>sin(<i>&pi;/4-iz/2</i>) <br>
     * <i>b = (1+iw)<sup>&frac12;</sup> = &radic;2 </i>cos(<i>&pi;/4-iz/2</i>)
     * <p>
     * Then, with <i>w = u+iv</i>, <i>z = x+iy</i>, <i>a = a<sub>1</sub>+ia<sub>2</sub></i>, and
     * <i>b = b<sub>1</sub>+ib<sub>2</sub></i>,
     * <p>
     * a<sub>1</sub>b<sub>2</sub> - a<sub>2</sub>b<sub>2</sub> = sinh <i>x</i> <br>
     * v / (a<sub>2</sub>b<sub>1</sub> - a<sub>1</sub>b<sub>2</sub>) = tan <i>y</i>
     * <p>
     * and we use {@link math#asinh(double)} and {@link Math#atan2(double, double)} to obtain
     * <i>x</i> and <i>y</i>.
     * <p>
     * For <i>w</i> sufficiently large that <i>w<sup>2</sup></i>&#x0226B;1,
     * sinh<sup>-1</sup><i>w</i> &asymp; ln(<i>2w</i>). When computing sin<sup>-1</sup><i>w</i>, we
     * evaluate <i>-i</i> sinh<sup>-1</sup><i>iw</i> instead.
     *
     * @param w
     * @param h <code>true</code> to compute sinh<sup>-1</sup><i>w</i>, <code>false</code> to
     *            compute sin<sup>-1</sup><i>w</i>.
     * @return sinh<sup>-1</sup><i>w</i> or sin<sup>-1</sup><i>w</i>
     */
    private static PyComplex asinOrAsinh(PyComplex w, boolean h) {
        double u, v, x, y;
        PyComplex z;

        if (h) {
            // We compute z = asinh(w). Let z = x + iy and w = u + iv.
            u = w.real;
            v = w.imag;
            // Then the function body computes x + iy = asinh(w).
        } else {
            // We compute w = asin(z). Unusually, let w = u - iv, so u + iv = iw.
            v = w.real;
            u = -w.imag;
            // Then as before, the function body computes asinh(u+iv) = asinh(iw) = i asin(w),
            // but we finally return z = y - ix = -i asinh(iw) = asin(w).
        }

        if (Double.isNaN(u)) {
            // Special case for nan in real part. Default clause deals naturally with v=nan.
            if (v == 0.) {
                x = u;
                y = v;
            } else if (Double.isInfinite(v)) {
                x = Double.POSITIVE_INFINITY;
                y = u;
            } else { // Any other value of v -> nan+nanj
                x = y = u;
            }

        } else if (Math.abs(u) > 0x1p27 || Math.abs(v) > 0x1p27) {
            /*
             * w is large: approximate 2sinh(z) by exp(x+iy) or -exp(-x-iy), whichever dominates.
             * Hence, z = x+iy = ln(2(u+iv)) or -ln(-2(u+iv))
             */
            x = logHypot(u, v) + math.LN2;
            if (Math.copySign(1., u) > 0.) {
                y = Math.atan2(v, u);
            } else {
                // Adjust for sign, choosing the angle so that -pi/2 < y < pi/2
                x = -x;
                y = Math.atan2(v, -u);
            }

        } else {
            // Normal case, without risk of overflow.
            PyComplex a = sqrt(new PyComplex(1. + v, -u)); // a = sqrt(1-iw)
            PyComplex b = sqrt(new PyComplex(1. - v, u));  // b = sqrt(1+iw)
            // Combine the parts so as that terms in y cancel, leaving us with sinh x:
            x = math.asinh(a.real * b.imag - a.imag * b.real);
            // The arguments are v = cosh x sin y, and cosh x cos y
            y = Math.atan2(v, a.real * b.real - a.imag * b.imag);
        }

        // Compose the result w according to whether we're computing asin(w) or asinh(w).
        if (h) {
            z = new PyComplex(x, y);    // z = x + iy = asinh(u+iv).
        } else {
            z = new PyComplex(y, -x);   // z = y - ix = -i asinh(v-iu) = asin(w)
        }

        // If that generated a nan, and there wasn't one in the argument, raise a domain error.
        return exceptNaN(z, w);
    }

    /**
     * Return the arc tangent of w. There are two branch cuts. One extends from 1j along the
     * imaginary axis to &infin;j, continuous from the right. The other extends from -1j along the
     * imaginary axis to -&infin;j, continuous from the left.
     *
     * @param w
     * @return tan<sup>-1</sup><i>w</i>
     */
    public static PyComplex atan(PyObject w) {
        return atanOrAtanh(complexFromPyObject(w), false);
    }

    /**
     * Return the hyperbolic arc tangent of w. There are two branch cuts. One extends from 1 along
     * the real axis to &infin;, continuous from below. The other extends from -1 along the real
     * axis to -&infin;, continuous from above.
     *
     * @param w
     * @return tanh<sup>-1</sup><i>w</i>
     */
    public static PyComplex atanh(PyObject w) {
        return atanOrAtanh(complexFromPyObject(w), true);
    }

    /**
     * Helper to compute either tan<sup>-1</sup><i>w</i> or tanh<sup>-1</sup><i>w</i>. The method
     * used is close to that used in CPython. For <i>z</i> = tanh<sup>-1</sup><i>w</i>:
     * <p>
     * <i>z = </i>&frac12;ln(<i>1 + 2w/(1-w)</i>)
     * <p>
     * Then, letting <i>z = x+iy</i>, and <i>w = u+iv</i>,
     * <p>
     * <i>x = </i>&frac14;ln(<i>1 + 4u/((1-u)<sup>2</sup>+v<sup>2</sup>)</i>) <i> =
     * -</i>&frac14;ln(<i>1 - 4u/((1+u)<sup>2</sup>+v<sup>2</sup>)</i>)<br>
     * <i>y = </i>&frac12;tan<sup>-1</sup>(<i>2v / ((1+u)(1-u)-v<sup>2</sup>)</i>)<br>
     * <p>
     * We use {@link math#log1p(double)} and {@link Math#atan2(double, double)} to obtain <i>x</i>
     * and <i>y</i>. The second expression for <code>x</code> is used when <i>u&lt;0</i>. For
     * <i>w</i> sufficiently large that <i>w<sup>2</sup></i>&#x0226B;1, tanh<sup>-1</sup><i>w</i>
     * &asymp; 1/w &plusmn; <i>i&pi;/2</i>). For small <i>w</i>, tanh<sup>-1</sup><i>w</i> &asymp;
     * <i>w</i>. When computing tan<sup>-1</sup><i>w</i>, we evaluate <i>-i</i>
     * tanh<sup>-1</sup><i>iw</i> instead.
     *
     * @param w
     * @param h <code>true</code> to compute tanh<sup>-1</sup><i>w</i>, <code>false</code> to
     *            compute tan<sup>-1</sup><i>w</i>.
     * @return tanh<sup>-1</sup><i>w</i> or tan<sup>-1</sup><i>w</i>
     */
    private static PyComplex atanOrAtanh(PyComplex w, boolean h) {
        double u, v, x, y;
        PyComplex z;

        if (h) {
            // We compute z = atanh(w). Let z = x + iy and w = u + iv.
            u = w.real;
            v = w.imag;
            // Then the function body computes x + iy = atanh(w).
        } else {
            // We compute w = atan(z). Unusually, let w = u - iv, so u + iv = iw.
            v = w.real;
            u = -w.imag;
            // Then as before, the function body computes atanh(u+iv) = atanh(iw) = i atan(w),
            // but we finally return z = y - ix = -i atanh(iw) = atan(w).
        }

        double absu = Math.abs(u), absv = Math.abs(v);

        if (absu >= 0x1p511 || absv >= 0x1p511) {
            // w is large: approximate atanh(w) by 1/w + i pi/2. 1/w = conjg(w)/|w|**2.
            if (Double.isInfinite(absu) || Double.isInfinite(absv)) {
                x = Math.copySign(0., u);
            } else {
                // w is also too big to square, carry a 2**-N scaling factor.
                int N = 520;
                double uu = Math.scalb(u, -N), vv = Math.scalb(v, -N);
                double mod2w = uu * uu + vv * vv;
                x = Math.scalb(uu / mod2w, -N);
            }
            // We don't need the imaginary part of 1/z. Just pi/2 with the sign of v. (If not nan.)
            if (Double.isNaN(v)) {
                y = v;
            } else {
                y = Math.copySign(Math.PI / 2., v);
            }

        } else if (absu < 0x1p-53) {
            // u is small enough that u**2 may be neglected relative to 1.
            if (absv > 0x1p-27) {
                // v is not small, but is not near overflow either.
                double v2 = v * v;
                double d = 1. + v2;
                x = Math.copySign(Math.log1p(4. * absu / d), u) * 0.25;
                y = Math.atan2(2. * v, 1. - v2) * 0.5;
            } else {
                // v is also small enough that v**2 may be neglected (or is nan). So z = w.
                x = u;
                y = v;
            }

        } else if (absu == 1. && absv < 0x1p-27) {
            // w is close to +1 or -1: needs a different expression, good as v->0
            x = Math.copySign(Math.log(absv) - math.LN2, u) * 0.5;
            if (v == 0.) {
                y = Double.NaN;
            } else {
                y = Math.copySign(Math.atan2(2., absv), v) * 0.5;
            }

        } else {
            /*
             * Normal case, without risk of overflow. The basic expression is z =
             * 0.5*ln((1+w)/(1-w)), which for positive u we rearrange as 0.5*ln(1+2w/(1-w)) and for
             * negative u as -0.5*ln(1-2w/(1+w)). By use of absu, we reduce the difference between
             * the expressions fo u>=0 and u<0 to a sign transfer.
             */
            double lmu = (1. - absu), lpu = (1. + absu), v2 = v * v;
            double d = lmu * lmu + v2;
            x = Math.copySign(Math.log1p(4. * absu / d), u) * 0.25;
            y = Math.atan2(2. * v, lmu * lpu - v2) * 0.5;
        }

        // Compose the result w according to whether we're computing atan(w) or atanh(w).
        if (h) {
            z = new PyComplex(x, y);    // z = x + iy = atanh(u+iv).
        } else {
            z = new PyComplex(y, -x);   // z = y - ix = -i atanh(v-iu) = atan(w)
        }

        // If that generated a nan, and there wasn't one in the argument, raise a domain error.
        return exceptNaN(z, w);
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

    /**
     * Return the complex number x with polar coordinates r and phi. Equivalent to
     * <code>r * (math.cos(phi) + math.sin(phi)*1j)</code>.
     *
     * @param r radius
     * @param phi angle
     * @return
     */
    public static PyComplex rect(double r, double phi) {
        double x, y;

        if (Double.isInfinite(r) && (Double.isInfinite(phi) || Double.isNaN(phi))) {
            x = Double.POSITIVE_INFINITY;
            y = Double.NaN;

        } else if (phi == 0.0) {
            // cos(phi)=1, sin(phi)=phi: finesse oddball r in computing y, but not x.
            x = r;
            if (Double.isNaN(r)) {
                y = phi;
            } else if (Double.isInfinite(r)) {
                y = phi * Math.copySign(1., r);
            } else {
                y = phi * r;
            }

        } else if (r == 0.0 && (Double.isInfinite(phi) || Double.isNaN(phi))) {
            // Ignore any problems (inf, nan) with phi
            x = y = 0.;

        } else {
            // Text-book case, using the trig functions.
            x = r * Math.cos(phi);
            y = r * Math.sin(phi);
        }
        return exceptNaN(new PyComplex(x, y), r, phi);
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
            // Then as before, the function body computes sinh(x+iy) = sinh(iz) = i sin(z),
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
     * Raise <code>ValueError</code> if <code>result</code> is a <code>NaN</code>, but neither
     * <code>a</code> nor <code>b</code> is <code>NaN</code>. Same as
     * {@link #exceptNaN(PyComplex, PyComplex)}.
     */
    private static PyComplex exceptNaN(PyComplex result, double a, double b) throws PyException {
        if ((Double.isNaN(result.real) || Double.isNaN(result.imag))
                && !(Double.isNaN(a) || Double.isNaN(b))) {
            throw math.mathDomainError();
        } else {
            return result;
        }
    }

}
