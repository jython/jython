"""Misc. exception related tests

Made for Jython.
"""
from test import test_support
import unittest
from javatests import StackOverflowErrorTest


class C:
    def __str__(self):
        raise Exception("E")
    def __repr__(self):
        raise Exception("S")

class ExceptionsTestCase(unittest.TestCase):

    def test_keyerror_str(self):
        self.assertEquals(str(KeyError()), '')
        # Is actually repr(args[0])
        self.assertEquals(str(KeyError('')), "''")
        self.assertEquals(str(KeyError('', '')), "('', '')")

    #From bugtests/test076.py
    def test_raise_no_arg(self):
        r = None
        try:
            try:
                raise RuntimeError("dummy")
            except RuntimeError:
                raise
        except RuntimeError, e:
            r = str(e)

        self.assertEquals(r, "dummy")

    def testBugFix1149372(self):
        try:
            c = C()
            str(c)
        except Exception, e:
            assert e.args[0] == "E"
            return
        unittest.fail("if __str__ raises an exception, re-raise")

    def test_wrap_StackOverflowError(self):
        with self.assertRaises(RuntimeError) as cm:
            StackOverflowErrorTest.throwStackOverflowError()
        self.assertEqual(
            cm.exception.message,
            "maximum recursion depth exceeded (Java StackOverflowError)")

        with self.assertRaises(RuntimeError) as cm:
            StackOverflowErrorTest.causeStackOverflowError()
        self.assertEqual(
            cm.exception.message,
            "maximum recursion depth exceeded (Java StackOverflowError)")


def test_main():
    test_support.run_unittest(ExceptionsTestCase)

if __name__ == '__main__':
    test_main()
