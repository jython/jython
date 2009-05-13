"""Misc threading module tests

Made for Jython.
"""
import unittest
from test import test_support
from threading import Thread

class ThreadingTestCase(unittest.TestCase):

    def test_str_name(self):
        t = Thread(name=1)
        self.assertEqual(t.getName(), '1')
        t.setName(2)
        self.assertEqual(t.getName(), '2')


def test_main():
    test_support.run_unittest(ThreadingTestCase)


if __name__ == "__main__":
    test_main()
