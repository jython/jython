# -*- coding: utf-8 -*-
"""Misc unicode tests

Made for Jython.
"""
import re
import unittest
from test import test_support

class UnicodeTestCase(unittest.TestCase):

    def test_simplejson_plane_bug(self):
        # a bug exposed by simplejson: unicode __add__ was always
        # forcing the basic plane
        chunker = re.compile(r'(.*?)(["\\\x00-\x1f])', re.VERBOSE | re.MULTILINE | re.DOTALL)
        orig = u'z\U0001d120x'
        quoted1 = u'"z\U0001d120x"'
        quoted2 = '"' + orig + '"'
        # chunker re gives different results depending on the plane
        self.assertEqual(chunker.match(quoted1, 1).groups(), (orig, u'"'))
        self.assertEqual(chunker.match(quoted2, 1).groups(), (orig, u'"'))

    def test_parse_unicode(self):
        foo = u'ą\n'
        self.assertEqual(len(foo), 2, repr(foo))
        self.assertEqual(repr(foo), "u'\\u0105\\n'")
        self.assertEqual(ord(foo[0]), 261)
        self.assertEqual(ord(foo[1]), 10)

        bar = foo.encode('utf-8')
        self.assertEqual(len(bar), 3)
        self.assertEqual(repr(bar), "'\\xc4\\x85\\n'")
        self.assertEqual(ord(bar[0]), 196)
        self.assertEqual(ord(bar[1]), 133)
        self.assertEqual(ord(bar[2]), 10)

    def test_parse_raw_unicode(self):
        foo = ur'ą\n'
        self.assertEqual(len(foo), 3, repr(foo))
        self.assertEqual(repr(foo), "u'\\u0105\\\\n'")
        self.assertEqual(ord(foo[0]), 261)
        self.assertEqual(ord(foo[1]), 92)
        self.assertEqual(ord(foo[2]), 110)

        bar = foo.encode('utf-8')
        self.assertEqual(len(bar), 4)
        self.assertEqual(repr(bar), "'\\xc4\\x85\\\\n'")
        self.assertEqual(ord(bar[0]), 196)
        self.assertEqual(ord(bar[1]), 133)
        self.assertEqual(ord(bar[2]), 92)
        self.assertEqual(ord(bar[3]), 110)


def test_main():
    test_support.run_unittest(UnicodeTestCase)


if __name__ == "__main__":
    test_main()
