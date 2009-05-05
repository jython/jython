"""Misc subprocess tests"""
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


class JythonOptsTest(unittest.TestCase):

    """ Tests for (some parts of) issue #1187: JYTHON_OPTS should not be
    enriched by arguments
    """

    def testNoJythonOpts(self):
        os.environ['JYTHON_OPTS'] = ''
        p1 = Popen([sys.executable, "-c",
                    'import os, sys; sys.stdout.write(os.environ["JYTHON_OPTS"])'],
                   stdout=PIPE)
        self.assertEquals('', p1.stdout.read())

    def testExistingJythonOpts(self):
        options = '-Qold -Qwarn'
        os.environ['JYTHON_OPTS'] = options
        p1 = Popen([sys.executable, "-c",
                    'import os, sys; sys.stdout.write(os.environ["JYTHON_OPTS"])'],
                   stdout=PIPE)
        self.assertEquals(options, p1.stdout.read())


def test_main():
    test_support.run_unittest(EnvironmentInheritanceTest,
                              JythonOptsTest)


if __name__ == '__main__':
    test_main()
