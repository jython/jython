"""This test validates the fix for 511493 (jreload truncates large class files).

We do this by loading a large class file Blob.class from blob.jar (source inside the jar).  If the load failes with ClassFormatError,
the bug is present, if the load succeeds, the bug is fixed, if something else occurs, we don't know.
"""

import unittest
from test import test_support

from jreload import makeLoadSet
from java.lang import System, ClassFormatError

import os

class JreloadTestCase(unittest.TestCase):

    blobjar = test_support.findfile('blob.jar')

    def test( self ):
        myls = makeLoadSet('myls', [self.blobjar])

        try:
            from myls import Blob
        except ClassFormatError:
            print "Reload Error is present"
            raise
        
def test_main():
    test_support.run_unittest(JreloadTestCase)

if __name__ == "__main__":
    test_main()

