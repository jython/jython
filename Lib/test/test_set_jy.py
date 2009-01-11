from test import test_support
import unittest

from java.util import Random
from javatests import PySetInJavaTest

class SetInJavaTest(unittest.TestCase):
    "Tests for derived dict behaviour"
    def test_using_PySet_as_Java_Set(self):
        PySetInJavaTest.testPySetAsJavaSet()

    def test_accessing_items_added_in_java(self):
        s = PySetInJavaTest.createPySetContainingJavaObjects()
        for v in s:
            self.assert_(v in s)
            if isinstance(v, unicode):
                self.assertEquals("value", v)
            else:
                v.nextInt()#Should be a java.util.Random; ensure we can call it 

    def test_java_accessing_items_added_in_python(self):
        # Test a type that should be coerced into a Java type, a Java instance
        # that should be wrapped, and a Python instance that should pass
        # through as itself with str, Random and tuple respectively.
        s = set(["value", Random(), ("tuple", "of", "stuff")])
        PySetInJavaTest.accessAndRemovePySetItems(s)
        self.assertEquals(0, len(s))# Check that the Java removal affected the underlying set
        


def test_main():
    test_support.run_unittest(SetInJavaTest)

if __name__ == '__main__':
    test_main()
