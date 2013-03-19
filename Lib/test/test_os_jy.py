"""Misc os module tests

Made for Jython.
"""
import os
import unittest
from test import test_support

class OSTestCase(unittest.TestCase):

    def setUp(self):
        open(test_support.TESTFN, 'w').close()

    def tearDown(self):
        if os.path.exists(test_support.TESTFN):
            os.remove(test_support.TESTFN)

    def test_issue1727(self):
        os.stat(*(test_support.TESTFN,))

    def test_issue1755(self):
        os.remove(test_support.TESTFN)
        self.assertRaises(OSError, os.utime, test_support.TESTFN, None)

    def test_issue1824(self):
        os.remove(test_support.TESTFN)
        self.assertRaises(OSError, os.link,
                          test_support.TESTFN, test_support.TESTFN)

    def test_issue1825(self):
        os.remove(test_support.TESTFN)
        testfnu = unicode(test_support.TESTFN)
        try:
            os.open(testfnu, os.O_RDONLY)
        except OSError, e:
            self.assertTrue(isinstance(e.filename, unicode))
            self.assertEqual(e.filename, testfnu)
        else:
            self.assertTrue(False)

        # XXX: currently fail
        #for fn in os.chdir, os.listdir, os.rmdir:
        for fn in (os.rmdir,):
            try:
                fn(testfnu)
            except OSError, e:
                self.assertTrue(isinstance(e.filename, unicode))
                self.assertEqual(e.filename, testfnu)
            else:
                self.assertTrue(False)


def test_main():
    test_support.run_unittest(OSTestCase)


if __name__ == '__main__':
    test_main()
