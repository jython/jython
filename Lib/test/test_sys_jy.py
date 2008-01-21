import sys
import re
import unittest
import test.test_support

class SysTest(unittest.TestCase):
    
    def test_platform(self):
        self.assertEquals(sys.platform[:4], "java",
            "sys.platform is not java")
    
    def test_exit_arg(self):
        "sys.exit can be called with  args"
        try:
            sys.exit("leaving now")
        except SystemExit, e:
            self.assertEquals(str(e), "leaving now")

def test_main():
    test.test_support.run_unittest(SysTest)

if __name__ == "__main__":
    test_main()
