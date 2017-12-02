# Made for Jython

# Tests of built-in type shadowstr

import os
import sys
from test import string_tests
from test.test_support import run_unittest, is_jython
from test.test_str import StrTest
import unittest

from org.python.core import PyShadowString

# Ideally we would test shadowstr is a str but the tests need to sub-class it

class StrTestCase(
    string_tests.CommonTest,
    string_tests.MixinStrUnicodeUserStringTest,
    string_tests.MixinStrUserStringTest,
    ):
    # A PyShadowString should pass the tests for str too.
    type2test = PyShadowString


class ShadowStrTestCase(unittest.TestCase):

    def setUp(self):
        self.ss = PyShadowString("hello", "bonjour")

    # The Java class of a python module may be <module>$py
    CCLASS = r"test\.test_shadowstr_jy\$py"     # compiled (e.g. regrtest)
    # Or it may be org.python.pycode._pyx<n>
    PCLASS = r"org\.python\.pycode\._pyx\d+"    # .py at the prompt

    def check_first_eq(self):
        self.assertTrue(self.ss == "hello")
        self.assertFalse(self.ss == "bonjour")
        self.assertTrue("hello" == self.ss)
        self.assertFalse("bonjour" == self.ss)
        # shadowstring-shadowstring comparisons
        tt = PyShadowString("hello", "goodbye")
        self.assertTrue(self.ss == tt) # primary==primary
        tt = PyShadowString("adieu", "hello")
        self.assertFalse(self.ss == tt) # primary==shadow
        self.assertFalse(tt == self.ss) # shadow==primary
        tt = PyShadowString("adieu", "bonjour")
        self.assertFalse(self.ss == tt) # shadow==shadow

    def check_both_eq(self):
        self.assertTrue(self.ss == "hello")
        self.assertTrue(self.ss == "bonjour")
        self.assertTrue("hello" == self.ss)
        self.assertTrue("bonjour" == self.ss)
        # shadowstring-shadowstring comparisons
        tt = PyShadowString("hello", "goodbye")
        for c, m in self.ss.gettargets(): tt.addtarget(c, m)
        self.assertTrue(self.ss == tt) # primary==primary
        tt = PyShadowString("goodbye", "hello")
        for c, m in self.ss.gettargets(): tt.addtarget(c, m)
        self.assertTrue(self.ss == tt) # primary==shadow
        self.assertTrue(tt == self.ss) # shadow==primary
        tt = PyShadowString("adieu", "bonjour")
        for c, m in self.ss.gettargets(): tt.addtarget(c, m)
        self.assertTrue(self.ss == tt) # shadow==shadow

    def test_eq(self):
        # Test recognition unconditionally
        self.check_first_eq()
        self.ss.addtarget(None) # match any
        self.check_both_eq()

    def test_eq_class(self):
        # Test recognition of class context only
        self.check_first_eq()
        self.ss.addtarget(self.CCLASS)
        self.ss.addtarget(self.PCLASS)
        self.check_both_eq()

    def test_eq_method(self):
        # Test recognition of method context only
        self.check_first_eq()
        # The Java method name of a python function is name$<n>
        self.ss.addtarget(None, r"test_eq_method\$\d+") # method only
        self.check_both_eq()

    def test_eq_class_method(self):
        # Test recognition of class and method context
        self.check_first_eq()
        # Match this method in this module
        method = r"test_eq_class_method\$\d+"
        self.ss.addtarget(self.CCLASS, method)
        self.ss.addtarget(self.PCLASS, method)
        self.check_both_eq()

    def check_first_startswith(self):
        self.assertTrue(self.ss.startswith("hel"))
        self.assertFalse(self.ss.startswith("bon"))

    def check_both_startswith(self):
        self.assertTrue(self.ss.startswith("hel"))
        self.assertTrue(self.ss.startswith("bon"))

    def test_startswith(self):
        # Test recognition unconditionally
        self.check_first_startswith()
        self.ss.addtarget(None) # match any
        self.check_both_startswith()

    def test_startswith_class(self):
        # Test recognition of class context only
        self.check_first_startswith()
        self.ss.addtarget(self.CCLASS) # class only
        self.ss.addtarget(self.PCLASS) # class only
        self.check_both_startswith()

    def test_startswith_method(self):
        # Test recognition of method context only
        self.check_first_startswith()
        self.ss.addtarget(None, r"test_startswith_method\$\d+") # method only
        self.check_both_startswith()

    def test_startswith_class_method(self):
        # Test recognition of class and method context
        self.check_first_startswith()
        # Match this method in this module
        method = r"test_startswith_class_method\$\d+"
        self.ss.addtarget(self.CCLASS, method)
        self.ss.addtarget(self.PCLASS, method)
        self.check_both_startswith()

    def test_slice(self):
        # Test slicing goes through to the constituent strings consistently
        def check(m, n):
            tt = self.ss[m:n]
            self.assertEqual(tt, "hello"[m:n])
            self.assertEqual(tt, "bonjour"[m:n])
            self.assertEqual(self.ss.gettargets(), tt.gettargets())

        # Match this method in this module
        method = r"test_slice\$\d+"
        self.ss.addtarget(self.CCLASS, method)
        self.ss.addtarget(self.PCLASS, method)
        check(None, 3)
        check(1, 5)
        check(-3, None)
        check(None, None)

def test_main():
    run_unittest(
            StrTestCase,
            ShadowStrTestCase,
        )


if __name__ == "__main__":
    test_main()
