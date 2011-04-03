"""Misc os module tests

Made for Jython.
"""
import os
import unittest
from test import test_support

class OSTestCase(unittest.TestCase):

    def setUp(self):
        open(test_support.TESTFN, 'w').close()

    def tearDown(self):
        os.remove(test_support.TESTFN)

    def test_issue1727(self):
        os.stat(*(test_support.TESTFN,))


def test_main():
    test_support.run_unittest(OSTestCase)


if __name__ == '__main__':
    test_main()
