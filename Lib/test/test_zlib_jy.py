"""Misc zlib tests

Made for Jython.
"""
import unittest
import zlib
from array import array
from test import test_support

class ArrayTestCase(unittest.TestCase):

    def test_array(self):
        self._test_array(zlib.compress, zlib.decompress)

    def test_array_compressobj(self):
        def compress(value):
            co = zlib.compressobj()
            return co.compress(value) + co.flush()
        def decompress(value):
            dco = zlib.decompressobj()
            return dco.decompress(value) +  dco.flush()
        self._test_array(compress, decompress)

    def _test_array(self, compress, decompress):
        self.assertEqual(compress(array('c', 'jython')), compress('jython'))
        intarray = array('i', range(5))
        self.assertEqual(compress(intarray), compress(intarray.tostring()))
        compressed = array('c', compress('jython'))
        self.assertEqual('jython', decompress(compressed))

    def test_decompress_gzip(self):
        co = zlib.compressobj(wbits=31)  # window 15 with gzip wrapper.
        c = co.compress("Jenny: 867-5309")
        c += co.flush()
        dco = zlib.decompressobj(wbits=31)
        d = dco.decompress(c)
        self.assertEqual(b'', dco.unused_data, msg="dco.unused_data not empty after decompress.")
        self.assertEqual(b'', dco.unconsumed_tail, msg="dco.unconsumed_tail not empty after decompress.")
        self.assertEqual("Jenny: 867-5309", d)

    def test_decompress_badlen(self):
        # Manipulating last two bytes to create invalid initial size check.
        # RFC-1952:
        #    0   1   2   3   4   5   6   7
        #  +---+---+---+---+---+---+---+---+
        #  |     CRC32     |     ISIZE     |
        #  +---+---+---+---+---+---+---+---+turn:
        #
        c=b'\x1f\x8b\x08\x00\x00\x00\x00\x00\x00\x03\x0bJ\xacT(O,V\xc8H-J\x05\x00\xc2\xb0\x1e\xe5\x0d\x00\x00\x00'
        dco = zlib.decompressobj(wbits=31)
        self.assertRaisesRegexp(zlib.error, 'Error -3 while decompressing data: incorrect length check',
                                dco.decompress, c)

    def test_decompress_badcrc(self):
        # Manipulating last crc bytes to create a crc check exception.
        # RFC-1952:
        #    0   1   2   3   4   5   6   7
        #  +---+---+---+---+---+---+---+---+
        #  |     CRC32     |     ISIZE     |
        #  +---+---+---+---+---+---+---+---+turn:
        #
        c=b'\x1f\x8b\x08\x00\x00\x00\x00\x00\x00\x03\x0bJ\xacT(O,V\xc8H-J\x05\x00\xc2\xb0\x1f\xe5\x0c\x00\x00\x00'
        dco = zlib.decompressobj(wbits=31)
        self.assertRaisesRegexp(zlib.error, 'Error -3 while decompressing data: incorrect data check',
                                dco.decompress, c)




def test_main():
    test_support.run_unittest(ArrayTestCase)


if __name__ == '__main__':
    test_main()
