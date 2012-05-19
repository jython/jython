package org.python.modules;
/* This is a translation for the code in Python's mathmodule.c */

import org.python.core.Py;
import java.lang.Math;


public class math_gamma {
/*
   sin(pi*x), giving accurate results for all finite x (especially x
   integral or close to an integer).  This is here for use in the
   reflection formula for the gamma function.  It conforms to IEEE
   754-2008 for finite arguments, but not for infinities or nans.
*/

static final double pi = 3.141592653589793238462643383279502884197;
static final double sqrtpi = 1.772453850905516027298167483341145182798;

static double 
sinpi(double x)
{
    double y, r;
    int n;

    y = Math.abs(x) % 2.0;
    n = (int)Math.round(2.0*y);

    assert((0 <= n) && (n <= 4));

    switch (n) {
    case 0:
        r = Math.sin(pi*y);
        break;
    case 1:
        r = Math.cos(pi*(y-0.5));
        break;
    case 2:
        /* N.B. -sin(pi*(y-1.0)) is *not* equivalent: it would give
           -0.0 instead of 0.0 when y == 1.0. */
        r = Math.sin(pi*(1.0-y));
        break;
    case 3:
        r = -Math.cos(pi*(y-1.5));
        break;
    case 4:
        r = Math.sin(pi*(y-2.0));
        break;
    default:
        assert(false);  /* should never get here */
        r = 3; // Make javac happy
    }

    return Math.copySign(1.0, x)*r;
}

/* Implementation of the real gamma function.  In extensive but non-exhaustive
   random tests, this function proved accurate to within <= 10 ulps across the
   entire float domain.  Note that accuracy may depend on the quality of the
   system math functions, the pow function in particular.  Special cases
   follow C99 annex F.  The parameters and method are tailored to platforms
   whose double format is the IEEE 754 binary64 format.

   Method: for x > 0.0 we use the Lanczos approximation with parameters N=13
   and g=6.024680040776729583740234375; these parameters are amongst those
   used by the Boost library.  Following Boost (again), we re-express the
   Lanczos sum as a rational function, and compute it that way.  The
   coefficients below were computed independently using MPFR, and have been
   double-checked against the coefficients in the Boost source code.

   For x < 0.0 we use the reflection formula.

   There's one minor tweak that deserves explanation: Lanczos' formula for
   Gamma(x) involves computing pow(x+g-0.5, x-0.5) / exp(x+g-0.5).  For many x
   values, x+g-0.5 can be represented exactly.  However, in cases where it
   can't be represented exactly the small error in x+g-0.5 can be magnified
   significantly by the pow and exp calls, especially for large x.  A cheap
   correction is to multiply by (1 + e*g/(x+g-0.5)), where e is the error
   involved in the computation of x+g-0.5 (that is, e = computed value of
   x+g-0.5 - exact value of x+g-0.5).  Here's the proof:

   Correction factor
   -----------------
   Write x+g-0.5 = y-e, where y is exactly representable as an IEEE 754
   double, and e is tiny.  Then:

     pow(x+g-0.5,x-0.5)/exp(x+g-0.5) = pow(y-e, x-0.5)/exp(y-e)
     = pow(y, x-0.5)/exp(y) * C,

   where the correction_factor C is given by

     C = pow(1-e/y, x-0.5) * exp(e)

   Since e is tiny, pow(1-e/y, x-0.5) ~ 1-(x-0.5)*e/y, and exp(x) ~ 1+e, so:

     C ~ (1-(x-0.5)*e/y) * (1+e) ~ 1 + e*(y-(x-0.5))/y

   But y-(x-0.5) = g+e, and g+e ~ g.  So we get C ~ 1 + e*g/y, and

     pow(x+g-0.5,x-0.5)/exp(x+g-0.5) ~ pow(y, x-0.5)/exp(y) * (1 + e*g/y),

   Note that for accuracy, when computing r*C it's better to do

     r + e*g/y*r;

   than

     r * (1 + e*g/y);

   since the addition in the latter throws away most of the bits of
   information in e*g/y.
*/

static final int LANCZOS_N = 13;
static final double lanczos_g = 6.024680040776729583740234375;
static final double lanczos_g_minus_half = 5.524680040776729583740234375;
static final double[] lanczos_num_coeffs = new double[]{
    23531376880.410759688572007674451636754734846804940,
    42919803642.649098768957899047001988850926355848959,
    35711959237.355668049440185451547166705960488635843,
    17921034426.037209699919755754458931112671403265390,
    6039542586.3520280050642916443072979210699388420708,
    1439720407.3117216736632230727949123939715485786772,
    248874557.86205415651146038641322942321632125127801,
    31426415.585400194380614231628318205362874684987640,
    2876370.6289353724412254090516208496135991145378768,
    186056.26539522349504029498971604569928220784236328,
    8071.6720023658162106380029022722506138218516325024,
    210.82427775157934587250973392071336271166969580291,
    2.5066282746310002701649081771338373386264310793408
};

/* denominator is x*(x+1)*...*(x+LANCZOS_N-2) */
static final double[] lanczos_den_coeffs = new double[]{
    0.0, 39916800.0, 120543840.0, 150917976.0, 105258076.0, 45995730.0,
    13339535.0, 2637558.0, 357423.0, 32670.0, 1925.0, 66.0, 1.0};

/* gamma values for small positive integers, 1 though NGAMMA_INTEGRAL */
static final int NGAMMA_INTEGRAL = 23;
static final double[] gamma_integral = new double[]{
    1.0, 1.0, 2.0, 6.0, 24.0, 120.0, 720.0, 5040.0, 40320.0, 362880.0,
    3628800.0, 39916800.0, 479001600.0, 6227020800.0, 87178291200.0,
    1307674368000.0, 20922789888000.0, 355687428096000.0,
    6402373705728000.0, 121645100408832000.0, 2432902008176640000.0,
    51090942171709440000.0, 1124000727777607680000.0,
};

/* Lanczos' sum L_g(x), for positive x */

static double
lanczos_sum(double x)
{
    double num = 0.0, den = 0.0;
    int i;

    assert(x > 0.0);
    
    /* evaluate the rational function lanczos_sum(x).  For large
       x, the obvious algorithm risks overflow, so we instead
       rescale the denominator and numerator of the rational
       function by x**(1-LANCZOS_N) and treat this as a
       rational function in 1/x.  This also reduces the error for
       larger x values.  The choice of cutoff point (5.0 below) is
       somewhat arbitrary; in tests, smaller cutoff values than
       this resulted in lower accuracy. */
    if (x < 5.0) {
        for (i = LANCZOS_N; --i >= 0; ) {
            num = num * x + lanczos_num_coeffs[i];
            den = den * x + lanczos_den_coeffs[i];
        }
    }
    else {
        for (i = 0; i < LANCZOS_N; i++) {
            num = num / x + lanczos_num_coeffs[i];
            den = den / x + lanczos_den_coeffs[i];
        }
    }
    return num/den;
}

public static double
gamma(double x)
{
    double absx, r, y, z, sqrtpow;

    if (Double.isNaN(x)) {
        return x;
    }

    /* special cases */
    if (Double.isInfinite(x)) {
        if (x > 0.0) {
            return x;
        }

        throw Py.ValueError("math domain error");
    }

    if (x == 0.0) {
        throw Py.ValueError("math domain error");
    }

    /* integer arguments */
    if (x == Math.floor(x)) {
        if (x < 0.0) {
            throw Py.ValueError("math domain error");
        }
        if (x <= NGAMMA_INTEGRAL) {
            return gamma_integral[(int)x - 1];
        }
    }
    absx = Math.abs(x);

    /* tiny arguments:  tgamma(x) ~ 1/x for x near 0 */
    if (absx < 1e-20) {
        r = 1.0/x;
        if (Double.isInfinite(r)) {
            throw Py.OverflowError("math range error");
        }
        return r;
    }

    /* large arguments: assuming IEEE 754 doubles, tgamma(x) overflows for
       x > 200, and underflows to +-0.0 for x < -200, not a negative
       integer. */
    if (absx > 200.0) {
        if (x < 0.0) {
            return 0.0/sinpi(x);
        }
        else {
            throw Py.OverflowError("math range error");
        }
    }

    y = absx + lanczos_g_minus_half;
    /* compute error in sum */
    if (absx > lanczos_g_minus_half) {
        /* note: the correction can be foiled by an optimizing
           compiler that (incorrectly) thinks that an expression like
           a + b - a - b can be optimized to 0.0.  This shouldn't
           happen in a standards-conforming compiler. */
        double q = y - absx;
        z = q - lanczos_g_minus_half;
    }
    else {
        double q = y - lanczos_g_minus_half;
        z = q - absx;
    }
    z = z * lanczos_g / y;
    if (x < 0.0) {
        r = -pi / sinpi(absx) / absx * Math.exp(y) / lanczos_sum(absx);
        r -= z * r;
        if (absx < 140.0) {
            r /= Math.pow(y, absx - 0.5);
        }
        else {
            sqrtpow = Math.pow(y, absx / 2.0 - 0.25);
            r /= sqrtpow;
            r /= sqrtpow;
        }
    }
    else {
        r = lanczos_sum(absx) / Math.exp(y);
        r += z * r;
        if (absx < 140.0) {
            r *= Math.pow(y, absx - 0.5);
        }
        else {
            sqrtpow = Math.pow(y, absx / 2.0 - 0.25);
            r *= sqrtpow;
            r *= sqrtpow;
        }
    }

    if (Double.isInfinite(r)) {
        throw Py.OverflowError("math range error");
    }

    return r;
}

/*
   lgamma:  natural log of the absolute value of the Gamma function.
   For large arguments, Lanczos' formula works extremely well here.
*/

public static double
lgamma(double x)
{
    double r, absx;

    /* special cases */
    if (Double.isNaN(x)) {
        return x;
    }
    if (Double.isInfinite(x)) {
        /* lgamma(+-inf) = +inf */
        return Double.POSITIVE_INFINITY;
    }

    /* integer arguments */
    if (x == Math.floor(x) && x <= 2.0) {
        if (x <= 0.0) {
            throw Py.ValueError("math domain error");
        }
        else {
            return 0.0; /* lgamma(1) = lgamma(2) = 0.0 */
        }
    }

    absx = Math.abs(x);
    /* tiny arguments: lgamma(x) ~ -log(fabs(x)) for small x */
    if (absx < 1e-20) {
        return -Math.log(absx);
    }

    /* Lanczos' formula */
    if (x > 0.0) {
        /* we could save a fraction of a ulp in accuracy by having a
           second set of numerator coefficients for lanczos_sum that
           absorbed the exp(-lanczos_g) term, and throwing out the
           lanczos_g subtraction below; it's probably not worth it. */
        r = Math.log(lanczos_sum(x)) - lanczos_g +
            (x-0.5)*(Math.log(x+lanczos_g-0.5)-1);
    }
    else {
        r = Math.log(pi) - Math.log(Math.abs(sinpi(absx))) - Math.log(absx) -
            (Math.log(lanczos_sum(absx)) - lanczos_g +
             (absx-0.5)*(Math.log(absx+lanczos_g-0.5)-1));
    }

    if (Double.isInfinite(r)) {
        throw Py.OverflowError("math range error");
    }

    return r;
}

}
