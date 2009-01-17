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

class TestWrite(unittest.TestCase):
    def test_write_seek_write(self):
        f = cStringIO.StringIO()
        f.write('hello')
        f.seek(2)
        f.write('hi')
        self.assertEquals(f.getvalue(), 'hehio')

def test_main():
    test_support.run_unittest(TestUnicodeInput)
    test_support.run_unittest(TestWrite)

if __name__ == '__main__':
    test_main()
