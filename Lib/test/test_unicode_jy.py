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


def test_main():
    test_support.run_unittest(UnicodeTestCase)


if __name__ == "__main__":
    test_main()
