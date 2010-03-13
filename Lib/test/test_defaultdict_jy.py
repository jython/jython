"""Misc defaultdict tests.

Made for Jython.
"""
import pickle
import unittest
from collections import defaultdict
from test import test_support

class PickleTestCase(unittest.TestCase):

    def test_pickle(self):
        d = defaultdict(str, a='foo', b='bar')
        for proto in range(pickle.HIGHEST_PROTOCOL + 1):
            self.assertEqual(pickle.loads(pickle.dumps(d, proto)), d)


def test_main():
    test_support.run_unittest(PickleTestCase)


if __name__ == '__main__':
    test_main()
