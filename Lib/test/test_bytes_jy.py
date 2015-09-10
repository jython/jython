# Tests against problems we have seen in Jython's implementation of
# buffer, bytes, bytearray, and memoryview to prevent possible
# regression as well as integration with Java.

import unittest
import test.test_support


class ByteArraySubclassTest(unittest.TestCase):

    def test_len(self):
        class Sub(bytearray): pass
        s = Sub("abc123")
        self.assertEqual(len(s), 6)


class SimpleOperationsTest(unittest.TestCase):
    # Things the CPython library did not test throughly enough

    def test_irepeat(self) :

        def check_irepeat(a, n) :
            # Check in-place multiplication (repeats)
            b = bytearray(a)
            b *= n
            self.assertEquals(b, bytearray(a*n))

        def irepeat_export(a, n) :
            # In-place multiplication with export mostly raises BufferError
            b = bytearray(a)
            with memoryview(b) as m:
                b *= n
            # If it doesn't raise, it gets the right answer
            self.assertEquals(b, bytearray(a*n))

        for a in [b'', b'a', b'hello'] :
            check_irepeat(a, 7)
            check_irepeat(a, 1)
            check_irepeat(a, 0)
            check_irepeat(a, -1) # -ve treated as 0

        # Resizing with exports should raise an exception
        self.assertRaises(BufferError, irepeat_export, b'a', 5)
        self.assertRaises(BufferError, irepeat_export, b'hello', 3)
        self.assertRaises(BufferError, irepeat_export, b'hello', 0)
        self.assertRaises(BufferError, irepeat_export, b'hello', -1)

        # These don't raise an exception (CPython 2.7.6, 3.4.1)
        irepeat_export(b'a', 1)
        irepeat_export(b'hello', 1)
        for n in range(-1, 3) :
            irepeat_export(b'', n)

    # The following test_is* tests supplement string_tests for non-ascii examples.
    # The principle is to choose some character codes that are letters, digits
    # or spaces in Unicode but not in ASCII and check they are *not* categorised
    # as such in a byte context.

    def checkequal(self, expected, obj, methodname, *args):
        "check that object.method() returns expected result"
        for B in (bytearray,): # (bytes, bytearray):
            obj = B(obj)
            realresult = getattr(obj, methodname)()
            grumble = "%r.%s() returned %s" % (obj, methodname, realresult)
            self.assertIs(expected, realresult, grumble)
            # print grumble, 'x' if realresult != expected else '.'

    LOWER = b'\xe0\xe7\xe9\xff' # Uppercase in Latin-1 but not ascii
    UPPER = b'\xc0\xc7\xc9\xdd' # Lowercase in Latin-1 but not ascii
    DIGIT = b'\xb9\xb2\xb3'     # sup 1, 2, 3: numeric in Python (not Java)
    SPACE = b'\x85\xa0'         # NEXT LINE, NBSP: space in Python (not Java)

    def test_isalpha(self):
        for c in self.UPPER + self.LOWER:
            self.checkequal(False, c, 'isalpha')
            self.checkequal(False, b'a' + c + b'Z', 'isalpha')

    def test_isdigit(self):
        for c in self.DIGIT:
            self.checkequal(False, c, 'isdigit')
            self.checkequal(False, b'1' + c + b'3', 'isdigit')

    def test_islower(self):
        for c in self.LOWER:
            self.checkequal(False, c, 'islower')
        for c in self.UPPER:
            self.checkequal(True, b'a' + c + b'z', 'islower')

    def test_isupper(self):
        for c in self.UPPER:
            self.checkequal(False, c, 'isupper')
        for c in self.LOWER:
            self.checkequal(True, b'A' + c + b'Z', 'isupper')

    def test_isspace(self):
        for c in self.SPACE:
            self.checkequal(False, c, 'isspace')
            self.checkequal(False, b'\t' + c + b' ', 'isspace')

    def test_isalnum(self):
        for c in self.UPPER + self.LOWER + self.DIGIT:
            self.checkequal(False, c, 'isalnum')
            self.checkequal(False, b'a' + c + b'3', 'isalnum')

    def test_istitle(self):
        for c in self.UPPER:
            # c should be an un-cased character (effectively a space)
            self.checkequal(False, c, 'istitle')
            self.checkequal(True, b'A' + c + b'Titlecased Line', 'istitle')
            self.checkequal(True, b'A' + c + b' Titlecased Line', 'istitle')
            self.checkequal(True, b'A ' + c + b'Titlecased Line', 'istitle')
        for c in self.LOWER:
            # c should be an un-cased character (effectively a space)
            self.checkequal(True, b'A' + c + b'Titlecased Line', 'istitle')
            self.checkequal(True, b'A ' + c + b' Titlecased Line', 'istitle')


def test_main():
    test.test_support.run_unittest(
            ByteArraySubclassTest,
            SimpleOperationsTest,
        )


if __name__ == "__main__":
    test_main()
