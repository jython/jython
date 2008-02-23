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
        

def test_main():
    test.test_support.run_unittest(SysTest)

if __name__ == "__main__":
    test_main()
