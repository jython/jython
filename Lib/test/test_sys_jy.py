import sys
import re
import unittest
import test.test_support

class SysTest(unittest.TestCase):

    def test_platform(self):
        self.assertEquals(sys.platform[:4], "java",
                          "sys.platform is not java")

    def test_exit_arg(self):
        "sys.exit can be called with args"
        try:
            sys.exit("leaving now")
        except SystemExit, e:
            self.assertEquals(str(e), "leaving now")

    def test_tuple_args(self):
        "Exceptions raised unpacking tuple args have right line number"
        def tuple_args( (x,y) ): pass
        try:
            tuple_args( 10 )
        except TypeError:
            tb = sys.exc_info()[2]
            if tb.tb_lineno == 0:
                self.fail("Traceback lineno was zero")

    def test_name(self):
        "sys.__name__ can be reassigned/deleted"
        self.assertEquals(sys.__name__, 'sys')
        sys.__name__ = 'foo'
        self.assert_('foo' in str(sys))
        del sys.__name__
        self.assert_('foo' not in str(sys))
        sys.__name__ = 'sys'

    def test_readonly(self):
        def deleteClass(): del sys.__class__
        self.assertRaises(TypeError, deleteClass)

        def deleteDict(): del sys.__dict__
        self.assertRaises(TypeError, deleteDict)

        def assignClass(): sys.__class__ = object
        self.assertRaises(TypeError, assignClass)

        def assignDict(): sys.__dict__ = {}
        self.assertRaises(TypeError, assignDict)

    def test_resetmethod(self):
        gde = sys.getdefaultencoding
        sys.getdefaultencoding = 5
        self.assertEquals(sys.getdefaultencoding, 5)
        del sys.getdefaultencoding
        self.assertRaises(AttributeError, getattr, sys, 'getdefaultencoding')
        sys.getdefaultencoding = gde

    def test_reload(self):
        gde = sys.getdefaultencoding
        del sys.getdefaultencoding
        reload(sys)
        self.assert_(type(sys.getdefaultencoding) == type(gde))

def test_main():
    test.test_support.run_unittest(SysTest)

if __name__ == "__main__":
    test_main()
