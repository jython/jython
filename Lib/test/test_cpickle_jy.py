"""Misc cPickle tests.

Made for Jython.
"""
import cPickle
import pickle
import unittest
from test import test_support

class CPickleTestCase(unittest.TestCase):

    def test_zero_long(self):
        self.assertEqual(cPickle.loads(cPickle.dumps(0L, 2)), 0L)
        self.assertEqual(cPickle.dumps(0L, 2), pickle.dumps(0L, 2))


def test_main():
    test_support.run_unittest(CPickleTestCase)


if __name__ == '__main__':
    test_main()
