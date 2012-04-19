/*
    Floating-point error function and complementary error function.

    This is a translation of the code in Google's go math/erf.go
*/

// The original C code and the long comment below are
// from FreeBSD's /usr/src/lib/msun/src/s_erf.c and
// came with this notice.  The go code is a simplified
// version of the original C.
//
// ====================================================
// Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
//
// Developed at SunPro, a Sun Microsystems, Inc. business.
// Permission to use, copy, modify, and distribute this
// software is freely granted, provided that this notice
// is preserved.
// ====================================================
package org.python.modules;

public class math_erf {
    final static double erx = 8.45062911510467529297e-01; // 0x3FEB0AC160000000
    // Coefficients for approximation to  erf in [0, 0.84375]
    final static double efx  = 1.28379167095512586316e-01; // 0x3FC06EBA8214DB69
    final static double efx8 = 1.02703333676410069053e+00; // 0x3FF06EBA8214DB69
    final static double pp0  = 1.28379167095512558561e-01; // 0x3FC06EBA8214DB68
    final static double pp1  = -3.25042107247001499370e-01; // 0xBFD4CD7D691CB913
    final static double pp2  = -2.84817495755985104766e-02; // 0xBF9D2A51DBD7194F
    final static double pp3  = -5.77027029648944159157e-03; // 0xBF77A291236668E4
    final static double pp4  = -2.37630166566501626084e-05; // 0xBEF8EAD6120016AC
    final static double qq1  = 3.97917223959155352819e-01; // 0x3FD97779CDDADC09
    final static double qq2  = 6.50222499887672944485e-02; // 0x3FB0A54C5536CEBA
    final static double qq3  = 5.08130628187576562776e-03; // 0x3F74D022C4D36B0F
    final static double qq4  = 1.32494738004321644526e-04; // 0x3F215DC9221C1A10
    final static double qq5  = -3.96022827877536812320e-06; // 0xBED09C4342A26120
    // Coefficients for approximation to  erf  in [0.84375, 1.25]
    final static double pa0 = -2.36211856075265944077e-03; // 0xBF6359B8BEF77538
    final static double pa1 = 4.14856118683748331666e-01; // 0x3FDA8D00AD92B34D
    final static double pa2 = -3.72207876035701323847e-01; // 0xBFD7D240FBB8C3F1
    final static double pa3 = 3.18346619901161753674e-01; // 0x3FD45FCA805120E4
    final static double pa4 = -1.10894694282396677476e-01; // 0xBFBC63983D3E28EC
    final static double pa5 = 3.54783043256182359371e-02; // 0x3FA22A36599795EB
    final static double pa6 = -2.16637559486879084300e-03; // 0xBF61BF380A96073F
    final static double qa1 = 1.06420880400844228286e-01; // 0x3FBB3E6618EEE323
    final static double qa2 = 5.40397917702171048937e-01; // 0x3FE14AF092EB6F33
    final static double qa3 = 7.18286544141962662868e-02; // 0x3FB2635CD99FE9A7
    final static double qa4 = 1.26171219808761642112e-01; // 0x3FC02660E763351F
    final static double qa5 = 1.36370839120290507362e-02; // 0x3F8BEDC26B51DD1C
    final static double qa6 = 1.19844998467991074170e-02; // 0x3F888B545735151D
    // Coefficients for approximation to  erfc in [1.25, 1/0.35]
    final static double ra0 = -9.86494403484714822705e-03; // 0xBF843412600D6435
    final static double ra1 = -6.93858572707181764372e-01; // 0xBFE63416E4BA7360
    final static double ra2 = -1.05586262253232909814e+01; // 0xC0251E0441B0E726
    final static double ra3 = -6.23753324503260060396e+01; // 0xC04F300AE4CBA38D
    final static double ra4 = -1.62396669462573470355e+02; // 0xC0644CB184282266
    final static double ra5 = -1.84605092906711035994e+02; // 0xC067135CEBCCABB2
    final static double ra6 = -8.12874355063065934246e+01; // 0xC054526557E4D2F2
    final static double ra7 = -9.81432934416914548592e+00; // 0xC023A0EFC69AC25C
    final static double sa1 = 1.96512716674392571292e+01; // 0x4033A6B9BD707687
    final static double sa2 = 1.37657754143519042600e+02; // 0x4061350C526AE721
    final static double sa3 = 4.34565877475229228821e+02; // 0x407B290DD58A1A71
    final static double sa4 = 6.45387271733267880336e+02; // 0x40842B1921EC2868
    final static double sa5 = 4.29008140027567833386e+02; // 0x407AD02157700314
    final static double sa6 = 1.08635005541779435134e+02; // 0x405B28A3EE48AE2C
    final static double sa7 = 6.57024977031928170135e+00; // 0x401A47EF8E484A93
    final static double sa8 = -6.04244152148580987438e-02; // 0xBFAEEFF2EE749A62
    // Coefficients for approximation to  erfc in [1/.35, 28]
    final static double rb0 = -9.86494292470009928597e-03; // 0xBF84341239E86F4A
    final static double rb1 = -7.99283237680523006574e-01; // 0xBFE993BA70C285DE
    final static double rb2 = -1.77579549177547519889e+01; // 0xC031C209555F995A
    final static double rb3 = -1.60636384855821916062e+02; // 0xC064145D43C5ED98
    final static double rb4 = -6.37566443368389627722e+02; // 0xC083EC881375F228
    final static double rb5 = -1.02509513161107724954e+03; // 0xC09004616A2E5992
    final static double rb6 = -4.83519191608651397019e+02; // 0xC07E384E9BDC383F
    final static double sb1 = 3.03380607434824582924e+01; // 0x403E568B261D5190
    final static double sb2 = 3.25792512996573918826e+02; // 0x40745CAE221B9F0A
    final static double sb3 = 1.53672958608443695994e+03; // 0x409802EB189D5118
    final static double sb4 = 3.19985821950859553908e+03; // 0x40A8FFB7688C246A
    final static double sb5 = 2.55305040643316442583e+03; // 0x40A3F219CEDF3BE6
    final static double sb6 = 4.74528541206955367215e+02; // 0x407DA874E79FE763
    final static double sb7 = -2.24409524465858183362e+01; // 0xC03670E242712D62

    public static double erf(double x) {
        if (Double.isNaN(x)) {
            return x;
        }
        if (Double.POSITIVE_INFINITY == x) {
            return 1;
        }
        if (Double.NEGATIVE_INFINITY == x) {
            return -1;
        }

        final double veryTiny = 2.848094538889218e-306; // 0x0080000000000000
        final double small = 1.0 / (1 << 28); // 2**-28
        boolean sign = false;

        if (x < 0) {
            x = -x;
            sign = true;
        }

        if (x < 0.84375) { // |x| < 0.84375
            double temp;
            if (x < small) { // |x| < 2**-28
                if (x < veryTiny) {
                    // avoid underflow
                    temp = 0.125 * (8.0*x + efx8*x); 
                } else {
                    temp = x + efx*x;
                }
            } else {
                double z = x * x;
                double r = pp0 + z*(pp1+z*(pp2+z*(pp3+z*pp4)));
                double s = 1 + z*(qq1+z*(qq2+z*(qq3+z*(qq4+z*qq5))));
                double y = r / s;
                temp = x + x*y;
            }
            if (sign) {
                return -temp;
            }
            return temp;
        }

        if (x < 1.25) { // 0.84375 <= |x| < 1.25
            double s = x - 1;
            double P = pa0 + s*(pa1+s*(pa2+s*(pa3+s*(pa4+s*(pa5+s*pa6)))));
            double Q = 1 + s*(qa1+s*(qa2+s*(qa3+s*(qa4+s*(qa5+s*qa6)))));
            if (sign) {
                return -erx - P/Q;
            }
            return erx + P/Q;
        }

        if (x >= 6) { // inf > |x| >= 6
            if (sign) {
                return -1;
            }
            return 1;
        }

        double s = 1 / (x * x);
        double R, S;
        if (x < 1/0.35) { // |x| < 1 / 0.35  ~ 2.857143
            R = ra0 + s*(ra1+s*(ra2+s*(ra3+s*(ra4+s*(ra5+s*(ra6+s*ra7))))));
            S = 1 + s*(sa1+s*(sa2+s*(sa3+s*(sa4+s*(sa5+s*(sa6+s*(sa7+s*sa8)))))));
        } else { // |x| >= 1 / 0.35  ~ 2.857143
            R = rb0 + s*(rb1+s*(rb2+s*(rb3+s*(rb4+s*(rb5+s*rb6)))));
            S = 1 + s*(sb1+s*(sb2+s*(sb3+s*(sb4+s*(sb5+s*(sb6+s*sb7))))));
        }

        // pseudo-single (20-bit) precision x
        long t20 = Double.doubleToLongBits(x) & 0xffffffff00000000L;
        double z = Double.longBitsToDouble(t20);
        double r = Math.exp(-z*z-0.5625) * Math.exp((z-x)*(z+x)+R/S);
        if (sign) {
            return r/x - 1;
        }
        return 1 - r/x;
    }


    public static double erfc(double x) {
        if (Double.isNaN(x)) {
            return x;
        }
        if (Double.POSITIVE_INFINITY == x) {
            return 0;
        }
        if (Double.NEGATIVE_INFINITY == x) {
            return 2;
        }

        final double tiny = 1.0 / (1 << 56); // 2**-56
        boolean sign = false;
        if (x < 0) {
            x = -x;
            sign = true;
        }
        if (x < 0.84375) { // |x| < 0.84375
            double temp;
            if (x < tiny) { // |x| < 2**-56
                temp = x;
            } else {
                double z = x * x;
                double r = pp0 + z*(pp1+z*(pp2+z*(pp3+z*pp4)));
                double s = 1 + z*(qq1+z*(qq2+z*(qq3+z*(qq4+z*qq5))));
                double y = r / s;
                if (x < 0.25) { // |x| < 1/4
                    temp = x + x*y;
                } else {
                    temp = 0.5 + (x*y + (x - 0.5));
                }
            }
            if (sign) {
                return 1 + temp;
            }
            return 1 - temp;
        }
        if (x < 1.25) { // 0.84375 <= |x| < 1.25
            double s = x - 1;
            double P = pa0 + s*(pa1+s*(pa2+s*(pa3+s*(pa4+s*(pa5+s*pa6)))));
            double Q = 1 + s*(qa1+s*(qa2+s*(qa3+s*(qa4+s*(qa5+s*qa6)))));
            if (sign) {
                return 1 + erx + P/Q;
            }
            return 1 - erx - P/Q;

        }
        if (x < 28) { // |x| < 28
            double s = 1 / (x * x);
            double R, S;
            if (x < 1/0.35) { // |x| < 1 / 0.35 ~ 2.857143
                R = ra0 + s*(ra1+s*(ra2+s*(ra3+s*(ra4+s*(ra5+s*(ra6+s*ra7))))));
                S = 1 + s*(sa1+s*(sa2+s*(sa3+s*(sa4+s*(sa5+s*(sa6+s*(sa7+s*sa8)))))));
            } else { // |x| >= 1 / 0.35 ~ 2.857143
                if (sign && (x > 6)) {
                    return 2; // x < -6
                }
                R = rb0 + s*(rb1+s*(rb2+s*(rb3+s*(rb4+s*(rb5+s*rb6)))));
                S = 1 + s*(sb1+s*(sb2+s*(sb3+s*(sb4+s*(sb5+s*(sb6+s*sb7))))));
            }
            // pseudo-single (20-bit) precision x
            long t20 = Double.doubleToLongBits(x) & 0xffffffff00000000L;
            double z = Double.longBitsToDouble(t20);
            double r = Math.exp(-z*z-0.5625) * Math.exp((z-x)*(z+x)+R/S);
            if (sign) {
                return 2 - r/x;
            }
            return r / x;
        }
        if (sign) {
            return 2;
        }
        return 0;
    }
}
