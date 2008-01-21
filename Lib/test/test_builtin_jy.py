import sys
import unittest
import test.test_support

class BuiltinTest(unittest.TestCase):
	
    def test_in_sys_modules(self):
        self.assert_("__builtin__" in sys.modules,
            "__builtin__ not found in sys.modules")

def test_main():
    test.test_support.run_unittest(BuiltinTest)

if __name__ == "__main__":
    test_main()
