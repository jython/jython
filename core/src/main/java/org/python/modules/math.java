// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.modules;

import org.python.core.PyTuple;

/**
 * Stop-gap {@code math} module holding just the things we use form
 * code at present. Not exposed to Python.
 */
public class math {

    public static PyTuple frexp(double x) {
        int exponent;
        double mantissa;

        switch (exponent = Math.getExponent(x)) {

            default:
                // x = m * 2**exponent and 1 <=abs(m) <2
                exponent = exponent + 1;
                // x = m * 2**exponent and 0.5 <=abs(m) <1
                mantissa = Math.scalb(x, -exponent);
                break;

            case 1024:  // nan or inf
                mantissa = x;
                exponent = 0;
                break;

            case -1023:
                if (x == 0.) { // , 0.0 or -0.0
                    mantissa = x;
                    exponent = 0;
                } else { // denormalised value
                    // x = m * 2**exponent but 0 < abs(m) < 1
                    exponent = Math.getExponent(x * 0x1p52) - 51;
                    mantissa = Math.scalb(x, -exponent);
                }
                break;
        }

        return new PyTuple(mantissa, exponent);
    }
}
