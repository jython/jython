import unittest
import subprocess
import sys
from test import test_support

class TestUsingInitializer(unittest.TestCase):
    def test_syspath_initializer(self):
        fn = test_support.findfile("check_for_initializer_in_syspath.py")
        ret = subprocess.Popen([sys.executable, fn],
                env={"CLASSPATH":"tests/data/initializer"}).wait()
        self.assertEquals(0, ret)

def test_main():
    test_support.run_unittest(TestUsingInitializer)

if __name__ == "__main__":
    test_main()
