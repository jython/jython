"""Float tests

Made for Jython.
"""
import sys
import test_support
import unittest

jython = sys.platform.startswith('java')

class FloatTestCase(unittest.TestCase):

    def test_float_repr(self):
        self.assertEqual(repr(12345678.000000005), '12345678.000000006')
        self.assertEqual(repr(12345678.0000000005), '12345678.0')

    def test_float_str(self):
        self.assertEqual(str(12345678.000005), '12345678.0')
        self.assertEqual(str(12345678.00005), jython and '12345678.0' or '12345678.0001')
        self.assertEqual(str(12345678.0005), '12345678.0005')

    def test_float_str_formatting(self):
        self.assertEqual('%.13g' % 12345678.00005, '12345678.00005')
        self.assertEqual('%.12g' % 12345678.00005, jython and '12345678' or '12345678.0001')
        self.assertEqual('%.11g' % 12345678.00005, '12345678')


def test_main():
    test_support.run_unittest(FloatTestCase)

if __name__ == '__main__':
    test_main()
