# -*- coding: utf-8 -*-
"""Misc unicode tests

Made for Jython. (But it will run for CPython.)
"""
import itertools
import random
import re
import string
import sys
import unittest
from StringIO import StringIO
from test import test_support

class UnicodeTestCase(unittest.TestCase):

    def test_simplejson_plane_bug(self):
        # a bug exposed by simplejson: unicode __add__ was always
        # forcing the basic plane
        chunker = re.compile(r'(.*?)(["\\\x00-\x1f])', re.VERBOSE | re.MULTILINE | re.DOTALL)
        orig = u'z\U0001d120x'
        quoted1 = u'"z\U0001d120x"'
        quoted2 = '"' + orig + '"'
        # chunker re gives different results depending on the plane
        self.assertEqual(chunker.match(quoted1, 1).groups(), (orig, u'"'))
        self.assertEqual(chunker.match(quoted2, 1).groups(), (orig, u'"'))

    def test_parse_unicode(self):
        foo = u'ą\n'
        self.assertEqual(len(foo), 2, repr(foo))
        self.assertEqual(repr(foo), "u'\\u0105\\n'")
        self.assertEqual(ord(foo[0]), 261)
        self.assertEqual(ord(foo[1]), 10)

        bar = foo.encode('utf-8')
        self.assertEqual(len(bar), 3)
        self.assertEqual(repr(bar), "'\\xc4\\x85\\n'")
        self.assertEqual(ord(bar[0]), 196)
        self.assertEqual(ord(bar[1]), 133)
        self.assertEqual(ord(bar[2]), 10)

    def test_parse_raw_unicode(self):
        foo = ur'ą\n'
        self.assertEqual(len(foo), 3, repr(foo))
        self.assertEqual(repr(foo), "u'\\u0105\\\\n'")
        self.assertEqual(ord(foo[0]), 261)
        self.assertEqual(ord(foo[1]), 92)
        self.assertEqual(ord(foo[2]), 110)

        bar = foo.encode('utf-8')
        self.assertEqual(len(bar), 4)
        self.assertEqual(repr(bar), "'\\xc4\\x85\\\\n'")
        self.assertEqual(ord(bar[0]), 196)
        self.assertEqual(ord(bar[1]), 133)
        self.assertEqual(ord(bar[2]), 92)
        self.assertEqual(ord(bar[3]), 110)

        for baz in ur'Hello\u0020World !', ur'Hello\U00000020World !':
            self.assertEqual(len(baz), 13, repr(baz))
            self.assertEqual(repr(baz), "u'Hello World !'")
            self.assertEqual(ord(baz[5]), 32)

        quux = ur'\U00100000'
        self.assertEqual(repr(quux), "u'\\U00100000'")
        if sys.maxunicode == 0xffff:
            self.assertEqual(len(quux), 2)
            self.assertEqual(ord(quux[0]), 56256)
            self.assertEqual(ord(quux[1]), 56320)
        else:
            self.assertEqual(len(quux), 1)
            self.assertEqual(ord(quux), 1048576)

    def test_raw_unicode_escape(self):
        foo = u'\U00100000'
        self.assertEqual(foo.encode('raw_unicode_escape'), '\\U00100000')
        self.assertEqual(foo.encode('raw_unicode_escape').decode('raw_unicode_escape'),
                         foo)
        for bar in '\\u', '\\u000', '\\U00000':
            self.assertRaises(UnicodeDecodeError, bar.decode, 'raw_unicode_escape')

    def test_encode_decimal(self):
        self.assertEqual(int(u'\u0039\u0032'), 92)
        self.assertEqual(int(u'\u0660'), 0)
        self.assertEqual(int(u' \u001F\u0966\u096F\u0039'), 99)
        self.assertEqual(long(u'\u0663'), 3)
        self.assertEqual(float(u'\u0663.\u0661'), 3.1)
        self.assertEqual(complex(u'\u0663.\u0661'), 3.1+0j)

    def test_unstateful_end_of_data(self):
        # http://bugs.jython.org/issue1368
        for encoding in 'utf-8', 'utf-16', 'utf-16-be', 'utf-16-le':
            self.assertRaises(UnicodeDecodeError, '\xe4'.decode, encoding)

    def test_formatchar(self):
        self.assertEqual('%c' % 255, '\xff')
        self.assertRaises(OverflowError, '%c'.__mod__, 256)

        result = u'%c' % 256
        self.assert_(isinstance(result, unicode))
        self.assertEqual(result, u'\u0100')
        if sys.maxunicode == 0xffff:
            self.assertEqual(u'%c' % sys.maxunicode, u'\uffff')
        else:
            self.assertEqual(u'%c' % sys.maxunicode, u'\U0010ffff')
        self.assertRaises(OverflowError, '%c'.__mod__, sys.maxunicode + 1)

    def test_repr(self):
        self.assert_(isinstance('%r' % u'foo', str))

    @unittest.skipUnless(test_support.is_jython, "Specific to Jython")
    def test_unicode_lone_surrogate(self):
        # http://bugs.jython.org/issue2190
        self.assertRaises(ValueError, unichr, 0xd800)
        self.assertRaises(ValueError, unichr, 0xdfff)

    def test_concat(self):
        self.assertRaises(UnicodeDecodeError, lambda : u'' + '毛泽东')
        self.assertRaises(UnicodeDecodeError, lambda : '毛泽东' + u'')

    def test_join(self):
        self.assertRaises(UnicodeDecodeError, u''.join, ['foo', '毛泽东'])
        self.assertRaises(UnicodeDecodeError, '毛泽东'.join, [u'foo', u'bar'])

    def test_file_encoding(self):
        # Ensure file writing doesn't attempt to encode things by default and reading doesn't
        # decode things by default.  This was jython's behavior prior to 2.2.1'''
        EURO_SIGN = u"\u20ac"
        try:
            EURO_SIGN.encode()
        except UnicodeEncodeError:
            # This default encoding can't handle the encoding the Euro sign.  Skip the test
            return

        f = open(test_support.TESTFN, "w")
        self.assertRaises(UnicodeEncodeError, f, write, EURO_SIGN,
                "Shouldn't be able to write out a Euro sign without first encoding")
        f.close()

        f = open(test_support.TESTFN, "w")
        f.write(EURO_SIGN.encode('utf-8'))
        f.close()

        f = open(test_support.TESTFN, "r")
        encoded_euro = f.read()
        f.close()
        os.remove(test_support.TESTFN)
        self.assertEquals('\xe2\x82\xac', encoded_euro)
        self.assertEquals(EURO_SIGN, encoded_euro.decode('utf-8'))

    def test_translate(self):
        # http://bugs.jython.org/issue1483
        self.assertEqual(
            u'\u0443\u043a\u0430\u0437\u0430\u0442\u044c'.translate({}),
            u'\u0443\u043a\u0430\u0437\u0430\u0442\u044c')
        self.assertEqual(u'\u0443oo'.translate({0x443: 102}), u'foo')
        self.assertEqual(
            unichr(sys.maxunicode).translate({sys.maxunicode: 102}),
            u'f')


class UnicodeMaterial(object):
    ''' Object holding a list of single characters and a unicode string
        that is their concatenation. The sequence is created from a
        background sequence of basic plane characters and random
        replacement with supplementary plane characters (those with
        point code>0xffff).
    '''

    base = tuple(u'abcdefghijklmnopqrstuvwxyz')
    if sys.maxunicode < 0x10000:
        # This is here to prevent error messages on a narrow CPython build.
        supp = (u'NOT SUPPORTED',)
    else:
        supp = tuple(map(unichr, range(0x10000, 0x1000c)))
    used = sorted(set(base+supp))

    def __init__(self, size=20, pred=None, ran=None):
        ''' Create size chars choosing an SP char at i where
            pred(ran, i)==True where ran is an instance of
            random.Random supplied in the constructor or created
            locally (if ran==None).
        '''

        # Generators for the BMP and SP characters
        base = itertools.cycle(UnicodeMaterial.base)
        supp = itertools.cycle(UnicodeMaterial.supp)

        # Each instance gets a random generator
        if ran is None:
            ran = random.Random()
        self.random = ran

        if pred is None:
            pred = lambda ran, j : ran.random() < DEFAULT_RATE

        # Generate the list
        r = list()
        for i in range(size):
            if pred(self.random, i):
                c = supp.next()
            else:
                c = base.next()
            r.append(c)

        # The list and its concatenation are our material
        self.ref = r
        self.size = len(r)
        self.text = u''.join(r)
        self.target = u''

    def __len__(self):
        return self.size

    def insert(self, target, p=None):
        ''' Insert target string at position p (or middle), truncating if
            that would make the material any longer
        '''
        if p is None:
            p = max(0, (self.size-len(target)) // 2)

        n = 0
        for t in target:
            if p+n >= self.size:
                break;
            self.ref[p+n] = t
            n += 1

        self.target = target[:n]
        self.text = u''.join(self.ref)


@unittest.skipUnless(test_support.is_jython, "Specific to Jython")
class UnicodeIndexMixTest(unittest.TestCase):
    # Test indexing where there may be more than one code unit per code point.
    # See Jython Issue #2100.

    # Functions defining particular distributions of SP codes
    #
    def evenly(self, rate=0.2):
        'Evenly distributed at given rate'
        def f(ran, i):
            return ran.random() < rate
        return f

    def evenly_before(self, k, rate=0.2):
        'Evenly distributed on i<k at given rate'
        def f(ran, i):
            return i < k and ran.random() < rate
        return f

    def evenly_from(self, k, rate=0.2):
        'Evenly distributed on i>=k at given rate'
        def f(ran, i):
            return i >= k and ran.random() < rate
        return f

    def at(self, places):
        'Only at specified places'
        def f(ran, i):
            return i in places
        return f

    def setUp(self):
        ran = random.Random(1234)  # ensure repeatable
        mat = list()
        mat.append(UnicodeMaterial(10, self.at([2]), ran))
        mat.append(UnicodeMaterial(10, self.at([2, 5]), ran))
        mat.append(UnicodeMaterial(50, self.evenly(), ran))
        mat.append(UnicodeMaterial(200, self.evenly_before(70), ran))
        mat.append(UnicodeMaterial(200, self.evenly_from(130), ran))
        mat.append(UnicodeMaterial(1000, self.evenly(), ran))

        self.material = mat


    def test_getitem(self):
        # Test map from to code point index to internal representation
        # Fails in Jython 2.7b3

        def check_getitem(m):
            # Check indexing the string returns the expected point code
            for i in xrange(m.size):
                self.assertEqual(m.text[i], m.ref[i])

        for m in self.material:
            check_getitem(m)

    def test_slice(self):
        # Test indexing gets the slice ends correct.
        # Passes in Jython 2.7b3, but may be touched by #2100 changes.

        def check_slice(m):
            # Check a range of slices against slices of the reference.
            n = 1
            while n <= m.size:
                for i in range(m.size - n):
                    exp = u''.join(m.ref[i:i+n])
                    self.assertEqual(m.text[i:i+n], exp)
                n *= 3

        for m in self.material:
            check_slice(m)

    def test_find(self):
        # Test map from internal find result to code point index
        # Fails in Jython 2.7b3

        def check_find(ref):
            # Check find returns indexes for single point codes
            for c in set(m.used):
                start = 0
                u = m.text
                while start < m.size:
                    i = u.find(c, start)
                    if i < 0: break
                    self.assertEqual(u[i], c)
                    self.assertGreaterEqual(i, start)
                    start = i + 1

        def check_find_str(m, t):
            # Check find returns correct index for string target
            i = m.text.find(t)
            self.assertEqual(list(t), m.ref[i:i+len(t)])

        targets = [
            u"this",
            u"ab\U00010041de", 
            u"\U00010041\U00010042\U00010042xx",
            u"xx\U00010041\U00010042\U00010043yy",
        ]

        for m in self.material:
            check_find(m)
            for t in targets:
                # Insert in middle then try to find it
                m.insert(t)
                check_find_str(m, t)

    def test_rfind(self):
        # Test map from internal rfind result to code point index
        # Fails in Jython 2.7b3

        def check_rfind(ref):
            # Check rfind returns indexes for single point codes
            for c in set(m.used):
                end = m.size
                u = m.text
                while True:
                    i = u.rfind(c, 0, end)
                    if i < 0: break
                    self.assertLess(i, end)
                    self.assertEqual(u[i], c)
                    end = i

        def check_rfind_str(m, t):
            # Check rfind returns correct index for string target
            i = m.text.rfind(t)
            self.assertEqual(list(t), m.ref[i:i+len(t)])

        targets = [
            u"this",
            u"ab\U00010041de", 
            u"\U00010041\U00010042\U00010042xx",
            u"xx\U00010041\U00010042\U00010043yy",
        ]

        for m in self.material:
            check_rfind(m)
            for t in targets:
                # Insert in middle then try to find it
                m.insert(t)
                check_rfind_str(m, t)

    def test_surrogate_validation(self):

        from java.lang import StringBuilder

        def insert_sb(text, c1, c2):
            # Insert code points c1, c2 in the text, as a Java StringBuilder
            sb = StringBuilder()
            # c1 at the quarter point
            p1 = len(mat) // 4
            for c in mat.text[:p1]:
                sb.appendCodePoint(ord(c))
            sb.appendCodePoint(c1)
            # c2 at the three-quarter point
            p2 = 3 * p1
            for c in mat.text[p1:p2]:
                sb.appendCodePoint(ord(c))
            sb.appendCodePoint(c2)
            # Rest of text
            for c in mat.text[p2:]:
                sb.appendCodePoint(ord(c))
            return sb

        # Test that lone surrogates are rejected
        for surr in [0xdc81, 0xdc00, 0xdfff, 0xd800, 0xdbff]:
            for mat in self.material:

                # Java StringBuilder with two private-use characters:
                sb = insert_sb(mat.text, 0xe000, 0xf000)
                # Check this is acceptable
                #print repr(unicode(sb))
                self.assertEqual(len(unicode(sb)), len(mat)+2)

                # Java StringBuilder with private-use and lone surrogate:
                sb = insert_sb(mat.text, 0xe000, surr)
                # Check this is detected
                #print repr(unicode(sb))
                self.assertRaises(ValueError, unicode, sb)


class UnicodeFormatTestCase(unittest.TestCase):

    def test_unicode_mapping(self):
        assertTrue = self.assertTrue
        class EnsureUnicode(dict):
            def __missing__(self, key):
                assertTrue(isinstance(key, unicode))
                return key
        u'%(foo)s' % EnsureUnicode()

    def test_non_ascii_unicode_mod_str(self):
        # Regression test for a problem on the formatting logic: when no unicode
        # args were found, Jython stored the resulting buffer on a PyString,
        # decoding it later to make a PyUnicode. That crashed when the left side
        # of % was a unicode containing non-ascii chars
        self.assertEquals(u"\u00e7%s" % "foo", u"\u00e7foo")


class UnicodeStdIOTestCase(unittest.TestCase):

    def setUp(self):
        self.stdout = sys.stdout

    def tearDown(self):
        sys.stdout = self.stdout

    def test_intercepted_stdout(self):
        msg = u'Circle is 360\u00B0'
        sys.stdout = StringIO()
        print msg,
        self.assertEqual(sys.stdout.getvalue(), msg)


class UnicodeFormatStrTest(unittest.TestCase):
    # Adapted from test_str StrTest by liberally adding u-prefixes.

    def test__format__(self):
        def test(value, format, expected):
            r = value.__format__(format)
            self.assertEqual(r, expected)
            # note 'xyz'==u'xyz', so must check type separately
            self.assertIsInstance(r, unicode)
            # also test both with the trailing 's'
            r = value.__format__(format + u's')
            self.assertEqual(r, expected)
            self.assertIsInstance(r, unicode)

        test(u'', '', '')
        test(u'abc', '', 'abc')
        test(u'abc', '.3', 'abc')
        test(u'ab', '.3', 'ab')
        test(u'abcdef', '.3', 'abc')
        test(u'abcdef', '.0', '')
        test(u'abc', '3.3', 'abc')
        test(u'abc', '2.3', 'abc')
        test(u'abc', '2.2', 'ab')
        test(u'abc', '3.2', 'ab ')
        test(u'result', 'x<0', 'result')
        test(u'result', 'x<5', 'result')
        test(u'result', 'x<6', 'result')
        test(u'result', 'x<7', 'resultx')
        test(u'result', 'x<8', 'resultxx')
        test(u'result', ' <7', 'result ')
        test(u'result', '<7', 'result ')
        test(u'result', '>7', ' result')
        test(u'result', '>8', '  result')
        test(u'result', '^8', ' result ')
        test(u'result', '^9', ' result  ')
        test(u'result', '^10', '  result  ')
        test(u'a', '10000', 'a' + ' ' * 9999)
        test(u'', '10000', ' ' * 10000)
        test(u'', '10000000', ' ' * 10000000)

    def test_format(self):
        self.assertEqual(u''.format(), '')
        self.assertEqual(u'a'.format(), 'a')
        self.assertEqual(u'ab'.format(), 'ab')
        self.assertEqual(u'a{{'.format(), 'a{')
        self.assertEqual(u'a}}'.format(), 'a}')
        self.assertEqual(u'{{b'.format(), '{b')
        self.assertEqual(u'}}b'.format(), '}b')
        self.assertEqual(u'a{{b'.format(), 'a{b')

        # examples from the PEP:
        import datetime
        self.assertEqual(u"My name is {0}".format('Fred'), "My name is Fred")
        self.assertIsInstance(u"My name is {0}".format('Fred'), unicode)
        self.assertEqual(u"My name is {0[name]}".format(dict(name='Fred')),
                         "My name is Fred")
        self.assertEqual(u"My name is {0} :-{{}}".format('Fred'),
                         "My name is Fred :-{}")

        d = datetime.date(2007, 8, 18)
        self.assertEqual(u"The year is {0.year}".format(d),
                         "The year is 2007")

        # classes we'll use for testing
        class C:
            def __init__(self, x=100):
                self._x = x
            def __format__(self, spec):
                return spec

        class D:
            def __init__(self, x):
                self.x = x
            def __format__(self, spec):
                return str(self.x)

        # class with __str__, but no __format__
        class E:
            def __init__(self, x):
                self.x = x
            def __str__(self):
                return 'E(' + self.x + ')'

        # class with __repr__, but no __format__ or __str__
        class F:
            def __init__(self, x):
                self.x = x
            def __repr__(self):
                return 'F(' + self.x + ')'

        # class with __format__ that forwards to string, for some format_spec's
        class G:
            def __init__(self, x):
                self.x = x
            def __str__(self):
                return "string is " + self.x
            def __format__(self, format_spec):
                if format_spec == 'd':
                    return 'G(' + self.x + ')'
                return object.__format__(self, format_spec)

        # class that returns a bad type from __format__
        class H:
            def __format__(self, format_spec):
                return 1.0

        class I(datetime.date):
            def __format__(self, format_spec):
                return self.strftime(format_spec)

        class J(int):
            def __format__(self, format_spec):
                return int.__format__(self * 2, format_spec)


        self.assertEqual(u''.format(), '')
        self.assertEqual(u'abc'.format(), 'abc')
        self.assertEqual(u'{0}'.format('abc'), 'abc')
        self.assertEqual(u'{0:}'.format('abc'), 'abc')
        self.assertEqual(u'X{0}'.format('abc'), 'Xabc')
        self.assertEqual(u'{0}X'.format('abc'), 'abcX')
        self.assertEqual(u'X{0}Y'.format('abc'), 'XabcY')
        self.assertEqual(u'{1}'.format(1, 'abc'), 'abc')
        self.assertEqual(u'X{1}'.format(1, 'abc'), 'Xabc')
        self.assertEqual(u'{1}X'.format(1, 'abc'), 'abcX')
        self.assertEqual(u'X{1}Y'.format(1, 'abc'), 'XabcY')
        self.assertEqual(u'{0}'.format(-15), '-15')
        self.assertEqual(u'{0}{1}'.format(-15, 'abc'), '-15abc')
        self.assertEqual(u'{0}X{1}'.format(-15, 'abc'), '-15Xabc')
        self.assertEqual(u'{{'.format(), '{')
        self.assertEqual(u'}}'.format(), '}')
        self.assertEqual(u'{{}}'.format(), '{}')
        self.assertEqual(u'{{x}}'.format(), '{x}')
        self.assertEqual(u'{{{0}}}'.format(123), '{123}')
        self.assertEqual(u'{{{{0}}}}'.format(), '{{0}}')
        self.assertEqual(u'}}{{'.format(), '}{')
        self.assertEqual(u'}}x{{'.format(), '}x{')

        # weird field names
        self.assertEqual(u"{0[foo-bar]}".format({'foo-bar':'baz'}), 'baz')
        self.assertEqual(u"{0[foo bar]}".format({'foo bar':'baz'}), 'baz')
        self.assertEqual(u"{0[ ]}".format({' ':3}), '3')

        self.assertEqual(u'{foo._x}'.format(foo=C(20)), '20')
        self.assertEqual(u'{1}{0}'.format(D(10), D(20)), '2010')
        self.assertEqual(u'{0._x.x}'.format(C(D('abc'))), 'abc')
        self.assertEqual(u'{0[0]}'.format(['abc', 'def']), 'abc')
        self.assertEqual(u'{0[1]}'.format(['abc', 'def']), 'def')
        self.assertEqual(u'{0[1][0]}'.format(['abc', ['def']]), 'def')
        self.assertEqual(u'{0[1][0].x}'.format(['abc', [D('def')]]), 'def')

        self.assertIsInstance(u'{0[1][0].x}'.format(['abc', [D('def')]]), unicode)

        # strings
        self.assertEqual(u'{0:.3s}'.format('abc'), 'abc')
        self.assertEqual(u'{0:.3s}'.format('ab'), 'ab')
        self.assertEqual(u'{0:.3s}'.format('abcdef'), 'abc')
        self.assertEqual(u'{0:.0s}'.format('abcdef'), '')
        self.assertEqual(u'{0:3.3s}'.format('abc'), 'abc')
        self.assertEqual(u'{0:2.3s}'.format('abc'), 'abc')
        self.assertEqual(u'{0:2.2s}'.format('abc'), 'ab')
        self.assertEqual(u'{0:3.2s}'.format('abc'), 'ab ')
        self.assertEqual(u'{0:x<0s}'.format('result'), 'result')
        self.assertEqual(u'{0:x<5s}'.format('result'), 'result')
        self.assertEqual(u'{0:x<6s}'.format('result'), 'result')
        self.assertEqual(u'{0:x<7s}'.format('result'), 'resultx')
        self.assertEqual(u'{0:x<8s}'.format('result'), 'resultxx')
        self.assertEqual(u'{0: <7s}'.format('result'), 'result ')
        self.assertEqual(u'{0:<7s}'.format('result'), 'result ')
        self.assertEqual(u'{0:>7s}'.format('result'), ' result')
        self.assertEqual(u'{0:>8s}'.format('result'), '  result')
        self.assertEqual(u'{0:^8s}'.format('result'), ' result ')
        self.assertEqual(u'{0:^9s}'.format('result'), ' result  ')
        self.assertEqual(u'{0:^10s}'.format('result'), '  result  ')
        self.assertEqual(u'{0:10000}'.format('a'), 'a' + ' ' * 9999)
        self.assertEqual(u'{0:10000}'.format(''), ' ' * 10000)
        self.assertEqual(u'{0:10000000}'.format(''), ' ' * 10000000)

        # format specifiers for user defined type
        self.assertEqual(u'{0:abc}'.format(C()), 'abc')

        # !r and !s coercions
        self.assertEqual(u'{0!s}'.format('Hello'), 'Hello')
        self.assertEqual(u'{0!s:}'.format('Hello'), 'Hello')
        self.assertEqual(u'{0!s:15}'.format('Hello'), 'Hello          ')
        self.assertEqual(u'{0!s:15s}'.format('Hello'), 'Hello          ')
        self.assertEqual(u'{0!r}'.format('Hello'), "'Hello'")
        self.assertEqual(u'{0!r:}'.format('Hello'), "'Hello'")
        self.assertEqual(u'{0!r}'.format(F('Hello')), 'F(Hello)')

        # test fallback to object.__format__
        self.assertEqual(u'{0}'.format({}), '{}')
        self.assertEqual(u'{0}'.format([]), '[]')
        self.assertEqual(u'{0}'.format([1]), '[1]')
        self.assertEqual(u'{0}'.format(E('data')), 'E(data)')
        self.assertEqual(u'{0:d}'.format(G('data')), 'G(data)')
        self.assertEqual(u'{0!s}'.format(G('data')), 'string is data')

        msg = 'object.__format__ with a non-empty format string is deprecated'
        with test_support.check_warnings((msg, PendingDeprecationWarning)):
            self.assertEqual(u'{0:^10}'.format(E('data')), ' E(data)  ')
            self.assertEqual(u'{0:^10s}'.format(E('data')), ' E(data)  ')
            self.assertEqual(u'{0:>15s}'.format(G('data')), ' string is data')

        #FIXME: not supported in Jython yet:
        if not test_support.is_jython:
            self.assertEqual(u"{0:date: %Y-%m-%d}".format(I(year=2007,
                                                           month=8,
                                                           day=27)),
                             "date: 2007-08-27")

            # test deriving from a builtin type and overriding __format__
            self.assertEqual(u"{0}".format(J(10)), "20")


        # string format specifiers
        self.assertEqual(u'{0:}'.format('a'), 'a')

        # computed format specifiers
        self.assertEqual(u"{0:.{1}}".format('hello world', 5), 'hello')
        self.assertEqual(u"{0:.{1}s}".format('hello world', 5), 'hello')
        self.assertEqual(u"{0:.{precision}s}".format('hello world', precision=5), 'hello')
        self.assertEqual(u"{0:{width}.{precision}s}".format('hello world', width=10, precision=5), 'hello     ')
        self.assertEqual(u"{0:{width}.{precision}s}".format('hello world', width='10', precision='5'), 'hello     ')

        self.assertIsInstance(u"{0:{width}.{precision}s}".format('hello world', width='10', precision='5'), unicode)

        # test various errors
        self.assertRaises(ValueError, u'{'.format)
        self.assertRaises(ValueError, u'}'.format)
        self.assertRaises(ValueError, u'a{'.format)
        self.assertRaises(ValueError, u'a}'.format)
        self.assertRaises(ValueError, u'{a'.format)
        self.assertRaises(ValueError, u'}a'.format)
        self.assertRaises(IndexError, u'{0}'.format)
        self.assertRaises(IndexError, u'{1}'.format, u'abc')
        self.assertRaises(KeyError,   u'{x}'.format)
        self.assertRaises(ValueError, u"}{".format)
        self.assertRaises(ValueError, u"{".format)
        self.assertRaises(ValueError, u"}".format)
        self.assertRaises(ValueError, u"abc{0:{}".format)
        self.assertRaises(ValueError, u"{0".format)
        self.assertRaises(IndexError, u"{0.}".format)
        self.assertRaises(ValueError, u"{0.}".format, 0)
        self.assertRaises(IndexError, u"{0[}".format)
        self.assertRaises(ValueError, u"{0[}".format, [])
        self.assertRaises(KeyError,   u"{0]}".format)
        self.assertRaises(ValueError, u"{0.[]}".format, 0)
        self.assertRaises(ValueError, u"{0..foo}".format, 0)
        self.assertRaises(ValueError, u"{0[0}".format, 0)
        self.assertRaises(ValueError, u"{0[0:foo}".format, 0)
        self.assertRaises(KeyError,   u"{c]}".format)
        self.assertRaises(ValueError, u"{{ {{{0}}".format, 0)
        self.assertRaises(ValueError, u"{0}}".format, 0)
        self.assertRaises(KeyError,   u"{foo}".format, bar=3)
        self.assertRaises(ValueError, u"{0!x}".format, 3)
        self.assertRaises(ValueError, u"{0!}".format, 0)
        self.assertRaises(ValueError, u"{0!rs}".format, 0)
        self.assertRaises(ValueError, u"{!}".format)
        self.assertRaises(IndexError, u"{:}".format)
        self.assertRaises(IndexError, u"{:s}".format)
        self.assertRaises(IndexError, u"{}".format)

        # issue 6089
        self.assertRaises(ValueError, u"{0[0]x}".format, [None])
        self.assertRaises(ValueError, u"{0[0](10)}".format, [None])

        # can't have a replacement on the field name portion
        self.assertRaises(TypeError, u'{0[{1}]}'.format, 'abcdefg', 4)

        # exceed maximum recursion depth
        self.assertRaises(ValueError, u"{0:{1:{2}}}".format, 'abc', 's', '')
        self.assertRaises(ValueError, u"{0:{1:{2:{3:{4:{5:{6}}}}}}}".format,
                          0, 1, 2, 3, 4, 5, 6, 7)

        # string format spec errors
        self.assertRaises(ValueError, u"{0:-s}".format, '')
        self.assertRaises(ValueError, format, "", u"-")
        self.assertRaises(ValueError, u"{0:=s}".format, '')

    def test_format_auto_numbering(self):
        class C:
            def __init__(self, x=100):
                self._x = x
            def __format__(self, spec):
                return spec

        self.assertEqual(u'{}'.format(10), '10')
        self.assertEqual(u'{:5}'.format('s'), 's    ')
        self.assertEqual(u'{!r}'.format('s'), "'s'")
        self.assertEqual(u'{._x}'.format(C(10)), '10')
        self.assertEqual(u'{[1]}'.format([1, 2]), '2')
        self.assertEqual(u'{[a]}'.format({'a':4, 'b':2}), '4')
        self.assertEqual(u'a{}b{}c'.format(0, 1), 'a0b1c')

        self.assertEqual(u'a{:{}}b'.format('x', '^10'), 'a    x     b')
        self.assertEqual(u'a{:{}x}b'.format(20, '#'), 'a0x14b')

        # can't mix and match numbering and auto-numbering
        self.assertRaises(ValueError, u'{}{1}'.format, 1, 2)
        self.assertRaises(ValueError, u'{1}{}'.format, 1, 2)
        self.assertRaises(ValueError, u'{:{1}}'.format, 1, 2)
        self.assertRaises(ValueError, u'{0:{}}'.format, 1, 2)

        # can mix and match auto-numbering and named
        self.assertEqual(u'{f}{}'.format(4, f='test'), 'test4')
        self.assertEqual(u'{}{f}'.format(4, f='test'), '4test')
        self.assertEqual(u'{:{f}}{g}{}'.format(1, 3, g='g', f=2), ' 1g3')
        self.assertEqual(u'{f:{}}{}{g}'.format(2, 4, f=1, g='g'), ' 14g')


class StringModuleUnicodeTest(unittest.TestCase):
    # Taken from test_string ModuleTest and converted for unicode

    def test_formatter(self):

        def assertEqualAndUnicode(r, exp):
            self.assertEqual(r, exp)
            self.assertIsInstance(r, unicode)

        fmt = string.Formatter()
        assertEqualAndUnicode(fmt.format(u"foo"), "foo")
        assertEqualAndUnicode(fmt.format(u"foo{0}", "bar"), "foobar")
        assertEqualAndUnicode(fmt.format(u"foo{1}{0}-{1}", "bar", 6), "foo6bar-6")
        assertEqualAndUnicode(fmt.format(u"-{arg!r}-", arg='test'), "-'test'-")

        # override get_value ############################################
        class NamespaceFormatter(string.Formatter):
            def __init__(self, namespace={}):
                string.Formatter.__init__(self)
                self.namespace = namespace

            def get_value(self, key, args, kwds):
                if isinstance(key, (str, unicode)):
                    try:
                        # Check explicitly passed arguments first
                        return kwds[key]
                    except KeyError:
                        return self.namespace[key]
                else:
                    string.Formatter.get_value(key, args, kwds)

        fmt = NamespaceFormatter({'greeting':'hello'})
        assertEqualAndUnicode(fmt.format(u"{greeting}, world!"), 'hello, world!')


        # override format_field #########################################
        class CallFormatter(string.Formatter):
            def format_field(self, value, format_spec):
                return format(value(), format_spec)

        fmt = CallFormatter()
        assertEqualAndUnicode(fmt.format(u'*{0}*', lambda : 'result'), '*result*')


        # override convert_field ########################################
        class XFormatter(string.Formatter):
            def convert_field(self, value, conversion):
                if conversion == 'x':
                    return None
                return super(XFormatter, self).convert_field(value, conversion)

        fmt = XFormatter()
        assertEqualAndUnicode(fmt.format(u"{0!r}:{0!x}", 'foo', 'foo'), "'foo':None")


        # override parse ################################################
        class BarFormatter(string.Formatter):
            # returns an iterable that contains tuples of the form:
            # (literal_text, field_name, format_spec, conversion)
            def parse(self, format_string):
                for field in format_string.split('|'):
                    if field[0] == '+':
                        # it's markup
                        field_name, _, format_spec = field[1:].partition(':')
                        yield '', field_name, format_spec, None
                    else:
                        yield field, None, None, None

        fmt = BarFormatter()
        assertEqualAndUnicode(fmt.format(u'*|+0:^10s|*', 'foo'), '*   foo    *')

        # test all parameters used
        class CheckAllUsedFormatter(string.Formatter):
            def check_unused_args(self, used_args, args, kwargs):
                # Track which arguments actually got used
                unused_args = set(kwargs.keys())
                unused_args.update(range(0, len(args)))

                for arg in used_args:
                    unused_args.remove(arg)

                if unused_args:
                    raise ValueError("unused arguments")

        fmt = CheckAllUsedFormatter()
        # The next series should maybe also call assertEqualAndUnicode:
        #assertEqualAndUnicode(fmt.format(u"{0}", 10), "10")
        #assertEqualAndUnicode(fmt.format(u"{0}{i}", 10, i=100), "10100")
        #assertEqualAndUnicode(fmt.format(u"{0}{i}{1}", 10, 20, i=100), "1010020")
        # But string.Formatter.format returns bytes. See CPython Issue 15951.
        self.assertEqual(fmt.format(u"{0}", 10), "10")
        self.assertEqual(fmt.format(u"{0}{i}", 10, i=100), "10100")
        self.assertEqual(fmt.format(u"{0}{i}{1}", 10, 20, i=100), "1010020")
        self.assertRaises(ValueError, fmt.format, u"{0}{i}{1}", 10, 20, i=100, j=0)
        self.assertRaises(ValueError, fmt.format, u"{0}", 10, 20)
        self.assertRaises(ValueError, fmt.format, u"{0}", 10, 20, i=100)
        self.assertRaises(ValueError, fmt.format, u"{i}", 10, 20, i=100)


class UnicodeSpaceTest(unittest.TestCase):
    # Test classification of characters as whitespace (strictly as observed in CPython)

    def checkequal(self, expected, obj, methodname, *args):
        "check that object.method() returns expected result"
        realresult = getattr(obj, methodname)()
        grumble = "%r.%s() returned %r" % (obj, methodname, realresult)
        self.assertEqual(expected, realresult, grumble)
        # print grumble, 'x' if realresult != expected else '.'

    # The set of Unicode characters that are spaces according to CPython 2.7.15
    SPACE = u'\t\n\x0b\x0c\r\x1c\x1d\x1e\x1f\x20\x85\xa0\u1680\u180e' \
            u'\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200a' \
            u'\u2028\u2029\u202f\u205f\u3000'

    def test_isspace(self):
        for c in self.SPACE:
            self.checkequal(True, c, 'isspace')
            self.checkequal(True, u'\t' + c + u' ', 'isspace')

    # *strip() tests to supplement string_tests with non-ascii examples,
    # using characters that are spaces in latin-1 but not in ascii.

    def test_strip(self):
        for c in self.SPACE:
            # These should be stripped of c at left or right
            sp = u" " + c + u" "
            h = u"hello"
            s = sp + h + sp
            self.checkequal( h, s, 'strip')
            self.checkequal( h, c + s + c, 'strip')
            self.checkequal( sp + h, s, 'rstrip')
            self.checkequal( sp + h, s + c, 'rstrip')
            self.checkequal( h + sp, s, 'lstrip')
            self.checkequal( h + sp, c + s, 'lstrip')

    def test_split(self):
        for c in self.SPACE:
            # These should be split at c
            s = u"AAA" + c + u"BBB"
            self.assertEqual(2, len(s.split()), "no split made in " + repr(s))
            self.assertEqual(2, len(s.rsplit()), "no rsplit made in " + repr(s))


class EncodingContext(object):
    """Context manager to save and restore the encoding.

    Use like this:

        with EncodingContext("utf-8"):
            self.assertEqual("'caf\xc3\xa9'", u"'caf\xe9'")
    """

    def __init__(self, encoding):
        if not hasattr(sys, "setdefaultencoding"):
            reload(sys)
        self.original_encoding = sys.getdefaultencoding()
        sys.setdefaultencoding(encoding)

    def __enter__(self):
        return self

    def __exit__(self, *ignore_exc):
        sys.setdefaultencoding(self.original_encoding)


class DefaultDecodingTestCase(unittest.TestCase):
    # Test use of default encoding to coerce byte-like data to unicode

    BYTE_TYPES = (str, buffer, bytearray, memoryview)
    BYTE_TYPES_COMPARE = (str, buffer) # Restricted as for CPython __eq__ etc.

    if not test_support.is_jython:
        # CPython restricts the acceptable the byte-like types by context
        BYTE_TYPES = (str, buffer)
        BYTE_TYPES_COMPARE = (str, buffer)

    # Operators

    def test_add(self):
        cs = self.encoding
        ref = u'café crème'
        s1 = ref[:4].encode(cs)
        s2 = ref[4:].encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                b2 = B(s2)
                self.assertEqual( ref[:4] + b2, ref)
            # Really we're testing that str promotes. Other Bs may not.
            self.assertEqual( s1 + ref[4:], ref)

    def test_in(self):
        cs = self.encoding
        ref = u'café crème'
        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                self.assertTrue(B(u'é'.encode(cs)) in ref)
                self.assertTrue(B(u'fé'.encode(cs)) in ref)
                # Fails if the string is interpreted as code points.
                if cs !=  'latin-1':
                    self.assertFalse(B('\xc3\xa9') in u'caf\xc3\xa9')

    def test_eq(self):
        cs = self.encoding
        u = u"Un caf\xe9 cr\xe8me."
        # Derive a string such that u1 != u and the encoded versions s, s1
        u1 = u.replace('cr', 'm')
        s, s1 = u.encode(cs), u1.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES_COMPARE:
                #print B,
                b, b1 = B(s), B(s1)
                self.assertTrue (u == b )
                self.assertTrue (b == u )
                self.assertFalse(u == b1)
                self.assertFalse(b1== u )
                # Check not implicitly comparing as latin-1.
                if cs !=  'latin-1':
                    b = B('caf\xc3\xa9')
                    self.assertFalse(u'caf\xc3\xa9'== b)
                    self.assertFalse(b == u'caf\xc3\xa9')

    def test_ne(self):
        cs = self.encoding
        u = u"Un caf\xe9 cr\xe8me."
        # Derive a string such that u1 != u and the encoded versions s, s1
        u1 = u.replace('cr', 'm')
        s, s1 = u.encode(cs), u1.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES_COMPARE:
                #print B,
                b, b1 = B(s), B(s1)
                self.assertTrue (u != b1)
                self.assertTrue (b != u1)
                self.assertFalse(u != b )
                self.assertFalse(b != u )
                # Check not implicitly comparing as latin-1.
                if cs !=  'latin-1':
                    b = B('caf\xc3\xa9')
                    self.assertTrue(u'caf\xc3\xa9'!= b)
                    self.assertTrue(b != u'caf\xc3\xa9')

    def test_lt(self):
        cs = self.encoding
        u = u"Un caf\xe9 cr\xe8me."
        # Derive strings such that u0 < u < u1 and their encodings
        u0 = u.replace('cr', 'Cr')
        u1 = u.replace('.', '?')
        s0, s, s1 = u0.encode(cs), u.encode(cs), u1.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES_COMPARE:
                #print B,
                b0, b, b1 = B(s0), B(s), B(s1)
                self.assertTrue (b0 < u )
                self.assertFalse(b  < u )
                self.assertFalse(b1 < u )
                self.assertFalse(u  < b0)
                self.assertFalse(u  < b )
                self.assertTrue (u  < b1)

    def test_le(self):
        cs = self.encoding
        u = u"Un caf\xe9 cr\xe8me."
        # Derive strings such that u0 < u < u1 and their encodings
        u0 = u.replace('cr', 'Cr')
        u1 = u.replace('.', '?')
        s0, s, s1 = u0.encode(cs), u.encode(cs), u1.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES_COMPARE:
                #print B,
                b0, b, b1 = B(s0), B(s), B(s1)
                self.assertTrue (b0 <= u )
                self.assertTrue (b  <= u )
                self.assertFalse(b1 <= u )
                self.assertFalse(u  <= b0)
                self.assertTrue (u  <= b )
                self.assertTrue (u  <= b1)

    def test_gt(self):
        cs = self.encoding
        u = u"Un caf\xe9 cr\xe8me."
        # Derive strings such that u0 < u < u1 and their encodings
        u0 = u.replace('cr', 'Cr')
        u1 = u.replace('.', '?')
        s0, s, s1 = u0.encode(cs), u.encode(cs), u1.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES_COMPARE:
                #print B,
                b0, b, b1 = B(s0), B(s), B(s1)
                self.assertTrue (b1 > u )
                self.assertFalse(b  > u )
                self.assertFalse(b0 > u )
                self.assertFalse(u  > b1)
                self.assertFalse(u  > b )
                self.assertTrue (u  > b0)

    def test_ge(self):
        cs = self.encoding
        u = u"Un caf\xe9 cr\xe8me."
        # Derive strings such that u0 < u < u1 and their encodings
        u0 = u.replace('cr', 'Cr')
        u1 = u.replace('.', '?')
        s0, s, s1 = u0.encode(cs), u.encode(cs), u1.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES_COMPARE:
                #print B,
                b0, b, b1 = B(s0), B(s), B(s1)
                self.assertTrue (b1 >= u )
                self.assertTrue (b  >= u )
                self.assertFalse(b0 >= u )
                self.assertFalse(u  >= b1)
                self.assertTrue (u  >= b )
                self.assertTrue (u  >= b0)


    # Methods

    def test_count(self):
        cs = self.encoding
        ref = u'Le café des fées égarées'
        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                self.assertEqual(ref.count(B(u'é'.encode(cs))), 4)
                self.assertEqual(ref.count(B(u'fé'.encode(cs))), 2)

    def test_endswith(self):
        cs = self.encoding
        # Set up the test using unicode values and indices
        ref = u'café crème'
        s, u, v = ref[-4:], u'èm£', u'èµe'
        # Encode all this
        enc = ref.encode(cs)
        s1, u1, v1 = s.encode(cs), u.encode(cs), v.encode(cs)

        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                sb, ub, vb = B(s1), B(u1), B(v1)
                # Test with single argument
                self.assertFalse(ref.endswith(vb))
                self.assertTrue(ref.endswith(sb))
                # Test with a mixed tuple as the argument
                self.assertFalse(ref.endswith((ub, u, vb, v)))
                self.assertTrue(ref.endswith((ub, sb, vb)))
                self.assertTrue(ref.endswith((ub, u, sb, vb, v)))
                self.assertFalse(enc.endswith((ub, vb, u, v)))
                self.assertTrue(enc.endswith((u, s, v)))
                self.assertTrue(enc.endswith((ub, u, s, vb, v)))

    def test_endswith_slice(self):
        cs = self.encoding
        # Set up the test using unicode values and indices
        ref = u'«Un café crème?»'
        if len(u'«»'.encode(cs))!=2 and not test_support.is_jython:
            # CPython fails on str.startswith(unicode, int, int) as it passes
            # byte indices to unicode.startswith(unicode, int, int) unchanged.
            # It only works if « and » encode to single bytes. Easier test:
            ref = u'"Un café crème?"'
        a, b = 4, -2
        s, u, v = ref[b-4:b], u'èm£', u'èµe'
        # Encode all this, including the indices
        enc = ref.encode(cs)
        u1, v1 = u.encode(cs), v.encode(cs)
        a1 = len(ref[:a].encode(cs))
        b1 = - len(ref[b:].encode(cs))
        s1 = s.encode(cs)

        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                sb, ub, vb = B(s1), B(u1), B(v1)
                # Test the assumption on which the test is based
                self.assertEqual(ref[a:b], enc[a1:b1])
                # Test slice with single argument
                self.assertFalse(ref.endswith(vb, a, b))
                self.assertTrue(ref.endswith(sb, a, b))
                self.assertFalse(enc.endswith(vb, a1, b1))
                self.assertTrue(enc.endswith(s, a1, b1))
                # CPython would pass:
                #self.assertTrue(enc.endswith(s, a, b))
                # Test slice with a mixed tuple as the argument
                self.assertFalse(ref.endswith((ub, u, vb, v), a, b))
                self.assertTrue(ref.endswith((ub, sb, vb), a, b))
                self.assertTrue(ref.endswith((ub, u, sb, vb, v), a, b))
                self.assertFalse(enc.endswith((ub, vb, u, v), a1, b1))
                self.assertTrue(enc.endswith((u, s, v), a1, b1))
                self.assertTrue(enc.endswith((ub, u, s, vb, v), a1, b1))
                # CPython would pass:
                #self.assertTrue(enc.endswith((u, s, v), a, b))
                #self.assertTrue(enc.endswith((ub, u, s, vb, v), a, b))

    def test_find(self):
        cs = self.encoding
        ref = u'café crème'
        sub = u'è'.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                self.assertEqual(ref.find(B(sub)), 7)

    def test_index(self):
        cs = self.encoding
        ref = u'café crème'
        sub = u'è'.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                self.assertEqual(ref.index(B(sub)), 7)

    def test_lstrip(self):
        cs = self.encoding
        ref = u"¤£¥¥£¤du blé £"
        sep = u'¥£¤'.encode(cs)
        with EncodingContext(cs):
            self.assertEqual(ref.lstrip(sep), u"du blé £")

    def test_partition(self):
        cs = self.encoding
        ref = u"Des fées hébétées."
        sep1 = u'é'.encode(cs)
        sep2 = u'ées'.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                self.assertEqual(ref.partition(B(sep1)), (u"Des f", u"é", u"es hébétées."))
                self.assertEqual(ref.partition(B(sep2)), (u"Des f", u"ées", u" hébétées."))

    def test_replace(self):
        cs = self.encoding
        ref = u"Été."
        a = u'É'.encode(cs)
        b = u'é'.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                self.assertEqual(ref.replace(B(a), B(b)), u"été.")
                self.assertEqual(ref.replace(B(b), B(a)), u"ÉtÉ.")

    def test_rfind(self):
        cs = self.encoding
        ref = u'café crème'
        sub = u'é'.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                self.assertEqual(ref.rfind(B(sub)), 3)

    def test_rindex(self):
        cs = self.encoding
        ref = u'café crème'
        sub = u'é'.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                self.assertEqual(ref.index(B(sub)), 3)

    def test_rpartition(self):
        cs = self.encoding
        ref = u"Des fées hébétées."
        sep1 = u'é'.encode(cs)
        sep2 = u'ées'.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                self.assertEqual(ref.rpartition(B(sep1)), (u"Des fées hébét", u"é", u"es."))
                self.assertEqual(ref.rpartition(B(sep2)), (u"Des fées hébét", u"ées", u"."))

    def test_rsplit(self):
        cs = self.encoding
        ref = u"Des fées hébétées."
        sep1 = u'é'.encode(cs)
        sep2 = u'ées'.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                self.assertEqual(ref.rsplit(B(sep1), 3), [u"Des fées h", u"b", u"t", u"es."])
                self.assertEqual(ref.rsplit(B(sep2)), [u"Des f", u" hébét", u"."])

    def test_rstrip(self):
        cs = self.encoding
        ref = u"£ du blé¥£¤¤£¥"
        sep = u'¥£¤'.encode(cs)
        with EncodingContext(cs):
            self.assertEqual(ref.rstrip(sep), u"£ du blé")

    def test_split(self):
        cs = self.encoding
        ref = u"Des fées hébétées."
        sep1 = u'é'.encode(cs)
        sep2 = u'ées'.encode(cs)
        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                self.assertEqual(ref.split(B(sep1), 3), [u"Des f", u"es h", u"b", u"tées."])
                self.assertEqual(ref.split(B(sep2)), [u"Des f", u" hébét", u"."])

    def test_startsswith(self):
        cs = self.encoding
        # Set up the test using unicode values and indices
        ref = u'café crème'
        s, u, v = ref[:4], u'©af', u'caf£'
        # Encode all this
        enc = ref.encode(cs)
        u1, v1 = u.encode(cs), v.encode(cs)
        s1 = s.encode(cs)

        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                sb, ub, vb, b5 = B(s1), B(u1), B(v1), B(enc[:5])
                self.assertFalse(ref.startswith(vb))
                self.assertTrue(ref.startswith(b5))
                # Test with a mixed tuple as the argument
                self.assertFalse(ref.startswith((ub, u, vb, v)))
                self.assertTrue(ref.startswith((ub, b5, vb)))
                self.assertTrue(ref.startswith((ub, u, b5, vb, v)))
                self.assertFalse(enc.startswith((ub, vb, u, v)))
                self.assertTrue(enc.startswith((u, ref[:4], v)))
                self.assertTrue(enc.startswith((ub, u, ref[:4], vb, v)))

    def test_startsswith_slice(self):
        cs = self.encoding
        # Set up the test using unicode values and indices
        ref = u'«Un café crème?»'
        if len(u'«»'.encode(cs))!=2 and not test_support.is_jython:
            # CPython fails on str.startswith(unicode, int, int) as it passes
            # byte indices to unicode.startswith(unicode, int, int) unchanged.
            # It only works if « and » encode to single bytes. Easier test:
            ref = u'"Un café crème?"'
        a, b = 4, -2
        s, u, v = ref[a:a+4], u'©af', u'caf£'
        # Encode all this, including the indices
        enc = ref.encode(cs)
        u1, v1 = u.encode(cs), v.encode(cs)
        a1 = len(ref[:a].encode(cs))
        b1 = - len(ref[b:].encode(cs))
        s1 = s.encode(cs)

        with EncodingContext(cs):
            for B in self.BYTE_TYPES:
                #print B,
                sb, ub, vb = B(s1), B(u1), B(v1)
                # Test the assumption on which the test is based
                self.assertEqual(ref[a:b], enc[a1:b1])
                # Test slice with single argument
                self.assertFalse(ref.startswith(v, a, b))
                self.assertTrue(ref.startswith(sb, a, b))
                self.assertFalse(enc.startswith(vb, a1, b1))
                self.assertTrue(enc.startswith(s, a1, b1))
                # CPython would pass:
                #self.assertTrue(enc.startswith(s, a, b))
                # Test slice with a mixed tuple as the argument
                self.assertFalse(ref.startswith((ub, u, vb, v), a, b))
                self.assertTrue(ref.startswith((ub, sb, vb), a, b))
                self.assertTrue(ref.startswith((ub, u, sb, vb, v), a, b))
                self.assertFalse(enc.startswith((ub, vb, u, v), a1, b1))
                self.assertTrue(enc.startswith((u, s, v), a1, b1))
                self.assertTrue(enc.startswith((ub, u, s, vb, v), a1, b1))
                # CPython would pass:
                #self.assertTrue(enc.startswith((u, s, v), a, b))
                #self.assertTrue(enc.startswith((ub, u, s, vb, v), a, b))

    def test_strip(self):
        cs = self.encoding
        ref = u"¤£¥¥£¤du blé¥£¤¤£¥"
        sep = u'¥£¤'.encode(cs)
        with EncodingContext(cs):
            self.assertEqual(ref.strip(sep), u"du blé")

class DefaultDecodingLatin1(DefaultDecodingTestCase):
    encoding = "latin-1"

class DefaultDecodingUTF8(DefaultDecodingTestCase):
    encoding = "utf-8"

class DefaultDecodingCp850(DefaultDecodingTestCase):
    encoding = "cp850"


def test_main():
    # We'll be enabling sys.setdefaultencoding so remember to disable
    had_set = hasattr(sys, "setdefaultencoding")
    try:
        test_support.run_unittest(
                UnicodeTestCase,
                UnicodeIndexMixTest,
                UnicodeFormatTestCase,
                UnicodeStdIOTestCase,
                UnicodeFormatStrTest,
                StringModuleUnicodeTest,
                UnicodeSpaceTest,
                DefaultDecodingLatin1,
                DefaultDecodingUTF8,
                DefaultDecodingCp850,
            )
    finally:
        if not had_set:
            delattr(sys, "setdefaultencoding")


if __name__ == "__main__":
    test_main()
