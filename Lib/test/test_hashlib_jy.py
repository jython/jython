# encoding: utf-8
"""Misc hashlib tests

Made for Jython.
"""
import hashlib
import unittest
from test import test_support

class HashlibTestCase(unittest.TestCase):

    def test_unicode(self):
        self.assertEqual(hashlib.md5(u'foo').hexdigest(),
                         'acbd18db4cc2f85cedef654fccc4a4d8')
        self.assertRaises(UnicodeEncodeError, hashlib.md5, u'Gráin amháiñ')


def test_main():
    test_support.run_unittest(HashlibTestCase)


if __name__ == '__main__':
    test_main()
