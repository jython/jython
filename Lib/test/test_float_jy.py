"""Float tests

Made for Jython.
"""
import math
import sys
import test_support
import unittest

jython = sys.platform.startswith('java')

class FloatTestCase(unittest.TestCase):

    def test_float_repr(self):
        self.assertEqual(repr(12345678.000000005), '12345678.000000006')
        self.assertEqual(repr(12345678.0000000005), '12345678.0')
        self.assertEqual(repr(math.pi**-100),
                         jython and '1.9275814160560203e-50' or '1.9275814160560206e-50')
        self.assertEqual(repr(-1.0), '-1.0')
        self.assertEqual(repr(-9876.543210),
                         jython and '-9876.54321' or '-9876.5432099999998')
        self.assertEqual(repr(0.123456789e+35), '1.23456789e+34')

    def test_float_str(self):
        self.assertEqual(str(12345678.000005), '12345678.0')
        self.assertEqual(str(12345678.00005),
                         jython and '12345678.0' or '12345678.0001')
        self.assertEqual(str(12345678.00005),
                         jython and '12345678.0' or '12345678.0001')
        self.assertEqual(str(12345678.0005), '12345678.0005')
        self.assertEqual(str(math.pi**-100),
                         jython and '1.927581416056e-50' or '1.92758141606e-50')
        self.assertEqual(str(0.0), '0.0')
        self.assertEqual(str(-1.0), '-1.0')
        self.assertEqual(str(-9876.543210), '-9876.54321')
        self.assertEqual(str(23456789012E666), 'inf')
        self.assertEqual(str(-23456789012E666), '-inf')

    def test_float_str_formatting(self):
        self.assertEqual('%.13g' % 12345678.00005, '12345678.00005')
        self.assertEqual('%.12g' % 12345678.00005,
                         jython and '12345678' or '12345678.0001')
        self.assertEqual('%.11g' % 12345678.00005, '12345678')
        # XXX: The exponential formatter isn't totally correct, e.g. our
        # output here is really .13g
        self.assertEqual('%.12g' % math.pi**-100,
                         jython and '1.927581416056e-50' or '1.92758141606e-50')
        self.assertEqual('%.5g' % 123.005, '123')
        self.assertEqual('%#.5g' % 123.005, '123.00')
        self.assertEqual('%#g' % 0.001, '0.00100000')
        self.assertEqual('%#.5g' % 0.001, '0.0010000')
        self.assertEqual('%#.1g' % 0.0001, '0.0001')
        self.assertEqual('%#.4g' % 100, '100.0')
        self.assertEqual('%#.4g' % 100.25, '100.2')
        self.assertEqual('%g' % 0.00001, '1e-05')
        self.assertEqual('%#g' % 0.00001,
                         jython and '1.000000e-05' or '1.00000e-05')
        self.assertEqual('%e' % -400.0, '-4.000000e+02')
        self.assertEqual('%.2g' % 99, '99')
        self.assertEqual('%.2g' % 100, '1e+02')

    def test_overflow(self):
        shuge = '12345' * 120
        shuge_float = float(shuge)
        shuge_int = int(shuge)
        self.assertRaises(OverflowError, float, shuge_int)
        self.assertRaises(OverflowError, int, shuge_float)
        # and cmp should not overflow
        self.assert_(0.1 != shuge_int)


def test_main():
    test_support.run_unittest(FloatTestCase)

if __name__ == '__main__':
    test_main()
