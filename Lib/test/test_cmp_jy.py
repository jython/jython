"Tests for cmp() compatibility with CPython"
import unittest
from test import test_support

class UnicodeDerivedCmp(unittest.TestCase):
    "Test for http://bugs.jython.org/issue1889394"
    def testCompareWithString(self):
        class Test(unicode):
            pass
        test = Test('{1:1}')
        self.assertNotEqual(test, {1:1})
    def testCompareEmptyDerived(self):
        class A(unicode): pass
        class B(unicode): pass
        self.assertEqual(A(), B())

def test_main():
    test_support.run_unittest(UnicodeDerivedCmp)

if __name__ == '__main__':
    test_main()
