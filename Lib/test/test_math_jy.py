"""Misc math module tests

Made for Jython.
"""
import math
import unittest
from test import test_support

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
        self.assertEqual(inf, math.hypot(inf, 4))
        self.assertEqual(inf, math.hypot(4, inf))
        self.assertEqual(inf, math.hypot(ninf, 4))
        self.assertEqual(inf, math.hypot(4, ninf))


def test_main():
    test_support.run_unittest(MathTestCase)


if __name__ == '__main__':
    test_main()
