"""Misc complex tests

Made for Jython.
"""
import unittest
from test import test_support

class ComplexTest(unittest.TestCase):

    def test_dunder_coerce(self):
        self.assertEqual(complex.__coerce__(1+1j, None), NotImplemented)
        self.assertRaises(TypeError, complex.__coerce__, None, 1+2j)


def test_main():
    test_support.run_unittest(ComplexTest)

if __name__ == "__main__":
    test_main()
