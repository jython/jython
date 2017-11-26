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
#
# class StrTestCase( # Should pass all tests for str
#     string_tests.CommonTest,
#     string_tests.MixinStrUnicodeUserStringTest,
#     string_tests.MixinStrUserStringTest,
#     string_tests.MixinStrUnicodeTest,
#     ):
#
#     type2test = PyShadowString


class ShadowStrTestCase(unittest.TestCase):

    def setUp(self):
        self.ss = PyShadowString("hello", "bonjour")

    def check_first_eq(self):
        self.assertTrue(self.ss == "hello")
        self.assertFalse(self.ss == "bonjour")

    def check_both_eq(self):
        self.assertTrue(self.ss == "hello")
        self.assertTrue(self.ss == "bonjour")

    def test_eq(self):
        # Test recognition unconditionally
        self.check_first_eq()
        self.ss.addtarget(None) # match any
        self.check_both_eq()

    def test_eq_class(self):
        # Test recognition of class context only
        self.check_first_eq()
        # The Java class of a python module may be <module>$py
        self.ss.addtarget(r"test\.test_shadowstr_jy\$py") # class only
        # Or it may be org.python.pycode._pyx<n>
        self.ss.addtarget(r"org\.python\.pycode\._pyx\d+") # class only
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
        self.ss.addtarget(r"test\.test_shadowstr_jy\$py", # class
                          r"test_eq_class_method\$\d+") # method
        self.ss.addtarget(r"org\.python\.pycode\._pyx\d+", # class
                          r"test_eq_class_method\$\d+") # method
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
        # The Java class of a python module may be <module>$py
        self.ss.addtarget(r"test\.test_shadowstr_jy\$py") # class only
        # Or it may be org.python.pycode._pyx<n>
        self.ss.addtarget(r"org\.python\.pycode\._pyx\d+") # class only
        self.check_both_startswith()

    def test_startswith_method(self):
        # Test recognition of method context only
        self.check_first_startswith()
        # The Java method name of a python function is name$<n>
        self.ss.addtarget(None, r"test_startswith_method\$\d+") # method only
        self.check_both_startswith()

    def test_startswith_class_method(self):
        # Test recognition of class and method context
        self.check_first_startswith()
        # Match this method in this module
        self.ss.addtarget(r"test\.test_shadowstr_jy\$py", # class
                          r"test_startswith_class_method\$\d+") # method
        self.ss.addtarget(r"org\.python\.pycode\._pyx\d+", # class
                          r"test_startswith_class_method\$\d+") # method
        self.check_both_startswith()


def test_main():
    run_unittest(
            #StrTestCase,
            ShadowStrTestCase,
        )


if __name__ == "__main__":
    test_main()
