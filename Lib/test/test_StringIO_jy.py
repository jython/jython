import unittest
import cStringIO
from test import test_support

class TestUnicodeInput(unittest.TestCase):
    def test_differences_handling_unicode(self):
        # Test for the "feature" described on #1089.
        #
        # Basically, StringIO returns unicode objects if you feed it unicode,
        # but cStringIO don't. This should change in future versions of
        # CPython and Jython.
        self.assertEqual(u'foo', cStringIO.StringIO(u'foo').read())
        self.assertEqual('foo', cStringIO.StringIO(u'foo').read())


def test_main():
    test_support.run_unittest(TestUnicodeInput)

if __name__ == '__main__':
    test_main()
