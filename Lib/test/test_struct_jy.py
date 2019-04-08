import unittest
from test import test_support
import struct

import sys
ISBIGENDIAN = sys.byteorder == "big"

class StructTests(unittest.TestCase): # (format, argument, big-endian result, little-endian result, asymmetric)
    _tests = [
        ('c', 'a', 'a', 'a', 0),
        ('xc', 'a', '\0a', '\0a', 0),
        ('cx', 'a', 'a\0', 'a\0', 0),
        ('s', 'a', 'a', 'a', 0),
        ('0s', 'helloworld', '', '', 1),
        ('1s', 'helloworld', 'h', 'h', 1),
        ('9s', 'helloworld', 'helloworl', 'helloworl', 1),
        ('10s', 'helloworld', 'helloworld', 'helloworld', 0),
        ('11s', 'helloworld', 'helloworld\0', 'helloworld\0', 1),
        ('20s', 'helloworld', 'helloworld'+10*'\0', 'helloworld'+10*'\0', 1),
        ('b', 7, '\7', '\7', 0),
        ('b', -7, '\371', '\371', 0),
        ('B', 7, '\7', '\7', 0),
        ('B', 249, '\371', '\371', 0),
        ('h', 700, '\002\274', '\274\002', 0),
        ('h', -700, '\375D', 'D\375', 0),
        ('H', 700, '\002\274', '\274\002', 0),
        ('H', 0x10000-700, '\375D', 'D\375', 0),
        ('i', 70000000, '\004,\035\200', '\200\035,\004', 0),
        ('i', -70000000, '\373\323\342\200', '\200\342\323\373', 0),
        ('I', 70000000L, '\004,\035\200', '\200\035,\004', 0),
        ('I', 0x100000000L-70000000, '\373\323\342\200', '\200\342\323\373', 0),
        ('l', 70000000, '\004,\035\200', '\200\035,\004', 0),
        ('l', -70000000, '\373\323\342\200', '\200\342\323\373', 0),
        ('L', 70000000L, '\004,\035\200', '\200\035,\004', 0),
        ('L', 0x100000000L-70000000, '\373\323\342\200', '\200\342\323\373', 0),
        ('f', 2.0, '@\000\000\000', '\000\000\000@', 0),
        ('d', 2.0, '@\000\000\000\000\000\000\000',
                   '\000\000\000\000\000\000\000@', 0),
        ('f', -2.0, '\300\000\000\000', '\000\000\000\300', 0),
        ('d', -2.0, '\300\000\000\000\000\000\000\000',
                   '\000\000\000\000\000\000\000\300', 0),
    ]

    def test_struct(self):
        for fmt, arg, big, lil, asy in self._tests:
            for (xfmt, exp) in [('>'+fmt, big), ('!'+fmt, big), ('<'+fmt, lil),
                                ('='+fmt, ISBIGENDIAN and big or lil)]:
                res = struct.pack(xfmt, arg)
                self.assertEqual(res,exp,msg="pack(%r, %r) -> %r # expected %r" %
                                             (fmt, arg, res, exp))
                n=struct.calcsize(xfmt)
                self.assertEqual(n, len(res),msg="calcsize(%r) -> %d # expected %d" %
                                                                      (xfmt, n, len(res)))
                rev = struct.unpack(xfmt, res)[0]
                if asy:
                    self.assertNotEqual(arg,rev,msg="unpack(%r, %r) -> (%r,) # expected (%r,)" %
                                                    (fmt, res, rev, exp))
                else:
                    self.assertEqual(arg,rev,msg="unpack(%r, %r) -> (%r,) # expected (%r,)" %
                                                 (fmt, res, rev, arg))

    def test_struct_unpack_bytearray(self):
        for fmt, arg, big, lil, asy in self._tests:
            for (xfmt, exp) in [('>'+fmt, big), ('!'+fmt, big), ('<'+fmt, lil),
                                ('='+fmt, ISBIGENDIAN and big or lil)]:
                res = struct.pack(xfmt, arg)
                self.assertEqual(res,exp,msg="pack(%r, %r) -> %r # expected %r" %
                                             (fmt, arg, res, exp))
                n=struct.calcsize(xfmt)
                self.assertEqual(n, len(res),msg="calcsize(%r) -> %d # expected %d" %
                                                                      (xfmt, n, len(res)))
                rev = struct.unpack(xfmt, bytearray(res))[0]
                if asy:
                    self.assertNotEqual(arg,rev,msg="unpack(%r, %r) -> (%r,) # expected (%r,)" %
                                                    (fmt, res, rev, exp))
                else:
                    self.assertEqual(arg,rev,msg="unpack(%r, %r) -> (%r,) # expected (%r,)" %
                                                 (fmt, res, rev, arg))

    def test_struct_unpack_buffer(self):
        for fmt, arg, big, lil, asy in self._tests:
            for (xfmt, exp) in [('>'+fmt, big), ('!'+fmt, big), ('<'+fmt, lil),
                                ('='+fmt, ISBIGENDIAN and big or lil)]:
                res = struct.pack(xfmt, arg)
                self.assertEqual(res,exp,msg="pack(%r, %r) -> %r # expected %r" %
                                             (fmt, arg, res, exp))
                n=struct.calcsize(xfmt)
                self.assertEqual(n, len(res),msg="calcsize(%r) -> %d # expected %d" %
                                                                      (xfmt, n, len(res)))
                rev = struct.unpack(xfmt, buffer(res))[0]
                if asy:
                    self.assertNotEqual(arg,rev,msg="unpack(%r, %r) -> (%r,) # expected (%r,)" %
                                                    (fmt, res, rev, exp))
                else:
                    self.assertEqual(arg,rev,msg="unpack(%r, %r) -> (%r,) # expected (%r,)" %
                                                 (fmt, res, rev, arg))
    def test_struct_unpack_from(self):
        for fmt, arg, big, lil, asy in self._tests:
            for (xfmt, exp) in [('>'+fmt, big), ('!'+fmt, big), ('<'+fmt, lil),
                                ('='+fmt, ISBIGENDIAN and big or lil)]:
                res = struct.pack(xfmt, arg)
                self.assertEqual(res,exp,msg="pack(%r, %r) -> %r # expected %r" %
                                             (fmt, arg, res, exp))
                n=struct.calcsize(xfmt)
                self.assertEqual(n, len(res),msg="calcsize(%r) -> %d # expected %d" %
                                                                      (xfmt, n, len(res)))
                rev = struct.unpack_from(xfmt, res)[0]
                if asy:
                    self.assertNotEqual(arg,rev,msg="unpack(%r, %r) -> (%r,) # expected (%r,)" %
                                                    (fmt, res, rev, exp))
                else:
                    self.assertEqual(arg,rev,msg="unpack(%r, %r) -> (%r,) # expected (%r,)" %
                                                 (fmt, res, rev, arg))

    def test_struct_unpack_from_bytearray(self):
        for fmt, arg, big, lil, asy in self._tests:
            for (xfmt, exp) in [('>'+fmt, big), ('!'+fmt, big), ('<'+fmt, lil),
                                ('='+fmt, ISBIGENDIAN and big or lil)]:
                res = struct.pack(xfmt, arg)
                self.assertEqual(res,exp,msg="pack(%r, %r) -> %r # expected %r" %
                                             (fmt, arg, res, exp))
                n=struct.calcsize(xfmt)
                self.assertEqual(n, len(res),msg="calcsize(%r) -> %d # expected %d" %
                                                                      (xfmt, n, len(res)))
                rev = struct.unpack_from(xfmt, bytearray(res))[0]
                if asy:
                    self.assertNotEqual(arg,rev,msg="unpack(%r, %r) -> (%r,) # expected (%r,)" %
                                                    (fmt, res, rev, exp))
                else:
                    self.assertEqual(arg,rev,msg="unpack(%r, %r) -> (%r,) # expected (%r,)" %
                                                 (fmt, res, rev, arg))

    def test_struct_unpack_from_buffer(self):
        for fmt, arg, big, lil, asy in self._tests:
            for (xfmt, exp) in [('>'+fmt, big), ('!'+fmt, big), ('<'+fmt, lil),
                                ('='+fmt, ISBIGENDIAN and big or lil)]:
                res = struct.pack(xfmt, arg)
                self.assertEqual(res,exp,msg="pack(%r, %r) -> %r # expected %r" %
                                             (fmt, arg, res, exp))
                n=struct.calcsize(xfmt)
                self.assertEqual(n, len(res),msg="calcsize(%r) -> %d # expected %d" %
                                                                      (xfmt, n, len(res)))
                rev = struct.unpack_from(xfmt, buffer(res))[0]
                if asy:
                    self.assertNotEqual(arg,rev,msg="unpack(%r, %r) -> (%r,) # expected (%r,)" %
                                                    (fmt, res, rev, exp))
                else:
                    self.assertEqual(arg,rev,msg="unpack(%r, %r) -> (%r,) # expected (%r,)" %
                                                 (fmt, res, rev, arg))




def test_main():
    test_support.run_unittest(__name__)

if __name__ == "__main__":
    test_main()
