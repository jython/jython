"""Test unicode handling in the binascii Java module."""

from test import test_support
from test.test_binascii import BinASCIITest
import unittest
import binascii


class UnicodeBinASCIITest(BinASCIITest):

    type2test = unicode

    # Create binary test data, but only 7-bit data to survive implicit unicode to str conversion.
    rawdata = "The quick brown fox jumps over the lazy dog.\r\n"
    rawdata += "".join(map(chr, xrange(128)))
    rawdata += "\r\nHello world.\n"

    def test_base64invalid(self):
        # Test base64 with random invalid characters sprinkled throughout.
        # This is a copy of BinASCIITest.test_base64invalid with 256 changed to 128 where we
        # generate "fillers".

        # Creating the modified test reveals a latent bug in the test as written, which is that the
        # padding character "=" is/was inserted as a filler. In the original test, the location of
        # that is harmless. With the change 256 to 128, it causes early termination of the
        # a2b_base64 conversion (both CPython and Jython). We therefore make padding a valid
        # character, excluding it from the fillers.

        MAX_BASE64 = 57
        lines = []
        for i in range(0, len(self.data), MAX_BASE64):
            b = self.type2test(self.rawdata[i:i+MAX_BASE64])
            a = binascii.b2a_base64(b)
            lines.append(a)

        fillers = ""
        valid = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/"
        valid += "=" # pad character also valid
        for i in xrange(128): # not 256 as in BinASCIITest.test_base64invalid
            c = chr(i)
            if c not in valid:
                fillers += c

        def addnoise(line):
            noise = fillers
            ratio = len(line) // len(noise)
            res = ""
            while line and noise:
                if len(line) // len(noise) > ratio:
                    c, line = line[0], line[1:]
                else:
                    c, noise = noise[0], noise[1:]
                res += c
            return res + noise + line

        res = ""
        for line in map(addnoise, lines):
            a = self.type2test(line)
            b = binascii.a2b_base64(a)
            res += b
        self.assertEqual(res, self.rawdata)

        # Test base64 with just invalid characters, which should return
        # empty strings. TBD: shouldn't it raise an exception instead ?
        self.assertEqual(binascii.a2b_base64(self.type2test(fillers)), '')


def test_main():
    test_support.run_unittest(UnicodeBinASCIITest)

if __name__ == "__main__":
    test_main()
