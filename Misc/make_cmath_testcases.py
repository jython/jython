# This work is based on test_math.py in the Python test set.
# It a provides a tool to generate additional real test cases.

import math
import mpmath


# Table of additional real test cases. The layout is
#
# function : ( starting_number [ x1, x2, x3, ... ] ),
#
# Where tests will be numbered functionNNNN and each xn
# generates a new test with (complex) argument xn + 0j.

cases_to_generate = {

    'atan' : ( 400, [
                float.fromhex('0x1.fffffffffffffp1023'),
                float.fromhex('-0x1.fffffffffffffp1023'),
                1e-17, -1e-17, 1e-4, -1e-4,
                1 - 1e-15, 1 + 1e-15,
                14.101419947171719, # tan(1.5)
                1255.7655915007896, # tan(1.57)
             ]),

    'cos' : ( 50, [
                1e-150, 1e-18, 1e-9, 0.0003, 0.2, 1.0, 
                -1e-18, -0.0003, -1.0,
                1.0471975511965977, # -> 0.5
                2.5707963267948966,
                -2.5707963267948966,
                18, 18.0
             ]),

    'cosh' : ( 50, [
                1e-150, 1e-18, 1e-9, 0.0003, 0.2, 1.0, 
                -1e-18, -0.0003, -1.0,
                1.3169578969248167086, # -> 2.
                -1.3169578969248167086,
                25*math.log(2), # cosh != exp at 52 bits
                27*math.log(2), # cosh == exp at 52 bits
                709.7827, # not quite overflow
                -709.7827, # not quite overflow
             ]),

    'exp' : ( 70, [
                1e-8, 0.0003, 0.2, 1.0, 
                -1e-8, -0.0003, -1.0,
                2**-52, -2**-53,    # exp != 1 (just)
                2.3025850929940457, # -> 10
                -2.3025850929940457,
                709.7827, # not quite overflow
             ]),

    'sin' : ( 50, [
                1e-100, 3.7e-8, 0.001, 0.2, 1.0, 
                -3.7e-8, -0.001, -1.0, 
                0.5235987755982989, # -> 0.5
                -0.5235987755982989,
                2.617993877991494365,
                -2.617993877991494365,
             ]),

    'sinh' : ( 50, [
                1e-100, 5e-17, 1e-16, 3.7e-8, 0.001, 0.2, 1.0, 
                -3.7e-8, -0.001, -1.0, 
                1.44363547517881034, # -> 2.
                -1.44363547517881034,
                25*math.log(2), # sinh != exp at 52 bits
                27*math.log(2), # sinh == exp at 52 bits
                709.7827, # not quite overflow
                -709.7827, # not quite overflow
             ]),

    'tan' : ( 50, [
                1e-100, 3.7e-8, 0.001, 0.2, 1.0, 
                -3.7e-8, -0.001, -1.0, 
                0.463647609000806116,   # -> 0.5
                -0.463647609000806116,
                1.1071487177940905,     # -> 0.5
                -1.1071487177940905,
                1.5,
                1.57,
                math.pi/2 - 2**-51,
             ]),

    'tanh' : ( 50, [
                1e-100, 5e-17, 1e-16, 3.7e-8, 0.001, 0.2, 1.0, 
                -3.7e-8, -0.001, -1.0, 
                0.54930614433405484, # -> 0.5
                -0.54930614433405484,
                25*math.log(2), # sinh != cosh at 52 bits
                27*math.log(2), # sinh == cosh at 52 bits
                711,            # oveflow cosh in naive impl
                1.797e+308,     # risk overflow
             ]),

    'sqrt' : ( 150, [
                float.fromhex('0x1.fffffffffffffp1023'),
                float.fromhex('0x1.0p-1022'),
                float.fromhex('0x0.0000000000001p-1022'),
             ]),
    }

def generate_cases() :
    fmt = "{}{:04d} {} {!r} 0.0 -> {} {!r}"
    for fn in sorted(cases_to_generate.keys()):
        print "-- Additional real values (Jython)"
        count, xlist = cases_to_generate[fn]
        for x in xlist:
            # Compute the function (in the reference library)
            func = getattr(mpmath, fn)
            y = func(x)
            # For the benefit of cmath tests, get the sign of imaginary zero right
            zero = 0.0
            if math.copysign(1., x) > 0.:
                if fn=='cos' :
                    zero = -0.0
            else :
                if fn=='cosh' :
                    zero = -0.0
            # Output one test case at sufficient precision
            print fmt.format(fn, count, fn, x, mpmath.nstr(y, 20), zero )
            count += 1

def test_main():
    with mpmath.workprec(100):
        generate_cases()

if __name__ == '__main__':
    test_main()

    # Conveniences for interactive use
    from mpmath import mp, mpf, workprec, workdps, nstr
