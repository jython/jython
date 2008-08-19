"Tests for cmp() compatibility with CPython"
import unittest
import os
import sys
from test import test_support
from subprocess import Popen, PIPE

class EnvironmentInheritanceTest(unittest.TestCase):
    def testDefaultEnvIsInherited(self):
        # Test for issue #1104
        os.environ['foo'] = 'something'
        p1 = Popen([sys.executable, "-c",
                    'import os, sys; sys.stdout.write(os.environ["foo"])'],
                   stdout=PIPE)
        self.assertEquals('something', p1.stdout.read())

def test_main():
    test_support.run_unittest(EnvironmentInheritanceTest)

if __name__ == '__main__':
    test_main()


