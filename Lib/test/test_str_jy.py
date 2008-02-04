import test_support
import unittest

class WrappedStrCmpTest(unittest.TestCase):

    def testWrappedWorksAsKey(self):
        '''Test for http://jython.org/bugs/1816134

        PyString's equal used to check for str explicitly, so Wrapper's __cmp__ wasn't used
        and a KeyError would be raised by the lookup on ABC.
        '''
        class Wrapper(object):
            def __init__(self, content):
                self.content = content
            def __hash__(self):
                return hash(self.content)
            def __cmp__(self, other):
                if isinstance(other, Wrapper):
                    return cmp(self.content, other.content)
                return cmp(self.content, other)
        d = {'ABC' : 1}
        ABC = Wrapper('ABC')
        self.assertEquals(1, d[ABC])

class IntToStrTest(unittest.TestCase):
    
    def test_int_to_string_format(self):
        # 0.001 comes out as 0.0010
        self.assertEquals(str(0.001), "0.001")

class StringSlicingTest(unittest.TestCase):
    
    def test_out_of_bounds(self):
        try:
            "a"[10:]
        except StringOutOfBoundsError:
            self.fail("str slice threw StringOutOfBoundsError")

class FormatTest(unittest.TestCase):
    
    def test_add_zeros(self):
        # 2 "%012d" % -4 displays '0000000000-4'
        s = "%012d" % -4
        self.assertEquals(s, "-00000000004")

    def test_format(self):
        "%#.0f, %e and %+f w/ negative numbers print correctly."
        self.assertEquals("%.1f" % 5, "5.0")
        self.assertEquals("%e" % -1e-6, "-1.000000e-006")
        self.assertEquals("%e" % 0, "0.000000e+000")
        self.assertEquals("%e" % 1e-6, "1.000000e-006")
        self.assertEquals("%+f" % -5, "-5.000000")
        self.assertEquals("%+f" % 5, "+5.000000")
 

    def test_argument_count_exception(self):
        "exception thrown when too many or too few arguments for format string"
        foo = False
        try:
            r = '%d' % (1, 2)
        except TypeError, e:
            self.failUnless("not all arguments converted" in str(e))

        try:
            r = '%d%d' % 1
        except TypeError, e:
            self.failUnless("not enough arguments for format string" in str(e))
        try:
            s = '%d%d' % (1,)
        except TypeError, e:
            self.failUnless("not enough arguments for format string" in str(e))


def test_main():
    test_support.run_unittest(WrappedStrCmpTest,
        IntToStrTest,
        StringSlicingTest,
        FormatTest)

if __name__ == '__main__':
    test_main()
