"""Misc math module tests

Made for Jython.
"""
import math
import unittest
from test import test_support
from java.lang import Math

inf = float('inf')
ninf = float('-inf')
nan = float('nan')

class MathTestCase(unittest.TestCase):

    def test_frexp(self):
        self.assertEqual(math.frexp(inf), (inf, 0))
        mantissa, exponent = math.frexp(nan)
        self.assertNotEqual(mantissa, mantissa)
        self.assertEqual(exponent, 0)

    def test_fmod(self):
        self.assertEqual(-1e-100, math.fmod(-1e-100, 1e100))

    def test_hypot(self):
        self.assert_(math.isnan(math.hypot(nan, nan)))
        self.assert_(math.isnan(math.hypot(4, nan)))
        self.assert_(math.isnan(math.hypot(nan, 4)))
        self.assertEqual(inf, math.hypot(inf, 4))
        self.assertEqual(inf, math.hypot(4, inf))
        self.assertEqual(inf, math.hypot(ninf, 4))
        self.assertEqual(inf, math.hypot(4, ninf))

    def test_math_domain_error(self):
        self.assertRaises(ValueError, math.sqrt, -1)
        self.assertRaises(ValueError, math.sqrt, -1.5)
        self.assertRaises(ValueError, math.sqrt, -0.5)
        self.assertRaises(ValueError, math.log, 0)
        self.assertRaises(ValueError, math.log, -1)
        self.assertRaises(ValueError, math.log, -1.5)
        self.assertRaises(ValueError, math.log, -0.5)

from test.test_math import MathTests

class MathAccuracy(MathTests):
    # Run the CPython tests but expect accurate results

    def ftest(self, name, value, expected):
        if expected != 0. :
            # Tolerate small deviation in proportion to expected
            tol = Math.ulp(expected)
        else :
            # On zero, allow 2**-52. Maybe allow different slack based on name
            tol = Math.ulp(1.)

        if abs(value-expected) > tol:
            # Use %r to display full precision.
            message = '%s returned %r, expected %r' % (name, value, expected)
            self.fail(message)

    def testConstants(self):
        self.ftest('pi', math.pi, Math.PI) # 3.141592653589793238462643
        self.ftest('e', math.e, Math.E)   # 2.718281828459045235360287



def test_main():
    test_support.run_unittest(
            MathTestCase,
            MathAccuracy,
        )


if __name__ == '__main__':
    test_main()
