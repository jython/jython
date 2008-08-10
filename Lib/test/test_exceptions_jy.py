"""Misc. exception related tests

Made for Jython.
"""
from test import test_support
import unittest

class ExceptionsTestCase(unittest.TestCase):

    def test_keyerror_str(self):
        self.assertEquals(str(KeyError()), '')
        # Is actually repr(args[0])
        self.assertEquals(str(KeyError('')), "''")
        self.assertEquals(str(KeyError('', '')), "('', '')")


def test_main():
    test_support.run_unittest(ExceptionsTestCase)

if __name__ == '__main__':
    test_main()
