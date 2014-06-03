"""String foramtting tests

Made for Jython.
"""
from test import test_support
import unittest

class FormatSubclass(unittest.TestCase):
    # Custom __int__ and __float__ should be respected by %-formatting

    def test_int_conversion_support(self):
        class Foo(object):
            def __init__(self, x): self.x = x
            def __int__(self): return self. x
        self.assertEqual('1', '%d' % Foo(1))
        self.assertEqual('1', '%d' % Foo(1L)) # __int__ can return a long, but
                                              # it should be accepted too

    def test_float_conversion_support(self):
        class Foo(object):
            def __init__(self, x): self.x = x
            def __float__(self): return self. x
        self.assertEqual('1.0', '%.1f' % Foo(1.0))

class FormatUnicodeBase(unittest.TestCase):

    # Test padding non-BMP result
    def test_pad_string(self):
        self.padcheck(u"architect")
        self.padcheck(u'a\U00010001cde')

class FormatUnicodeClassic(FormatUnicodeBase):
    # Check using %-formatting

    def padcheck(self, s):
        self.assertEqual(10, len('%10.4s' % s))
        self.assertEqual(u' '*6 + s[0:4], '%10.4s' % s)
        self.assertEqual(u' '*6 + s[0:4], '% 10.4s' % s)
        self.assertEqual(u' '*6 + s[0:4], '%010.4s' % s)
        self.assertEqual(s[0:3] + u' '*5, '%-8.3s' % s)

class FormatUnicodeModern(FormatUnicodeBase):
    # Check using __format__

    def padcheck(self, s):
        self.assertEqual(10, len(format(s, '10.4s')))
        self.assertEqual(s[0:3] + u' '*7, format(s, '10.3s'))
        self.assertEqual(s[0:3] + u'~'*7, format(s, '~<10.3s'))
        self.assertEqual(s[0:3] + u'~'*7, format(s, '~<10.3'))
        self.assertEqual(u' '*6 + s[0:4], format(s, '>10.4s'))
        self.assertEqual(u'*'*6 + s[0:4], format(s, '*>10.4s'))
        self.assertEqual(u'*'*6 + s[0:4], format(s, '*>10.4'))


class FormatMisc(unittest.TestCase):
    # Odd tests Jython used to fail

    def test_percent_padded(self) :
        self.assertEqual('%hello', '%%%s' % 'hello')
        self.assertEqual(u'     %hello', '%6%%s' % u'hello')
        self.assertEqual(u'%     hello', u'%-6%%s' % 'hello')

        self.assertEqual('     %', '%6%' % ())
        self.assertEqual('     %', '%06%' % ())
        self.assertEqual('   %', '%*%' % 4)
        self.assertEqual('%     ', '%-6%' % ())
        self.assertEqual('%     ', '%-06%' % ())
        self.assertEqual('%   ', '%*%' % -4)


def test_main():
    test_support.run_unittest(
            FormatSubclass,
            FormatUnicodeClassic,
            FormatUnicodeModern,
            FormatMisc,
    )

if __name__ == '__main__':
    test_main()
