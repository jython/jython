"""Misc io tests.

Made for Jython.
"""
import unittest

from org.python.core.util import FileUtil
from org.python.core.io import StreamIO

from java.io import InputStream
from java.nio import ByteBuffer;

class InfiniteInputStream(InputStream):

    def read(self, *args):
        if len(args) == 0:
            return ord('x')
        elif len(args) == 1:
            return InputStream.read(self, args[0])
        else:
            return self.read_buffer(*args)

    def read_buffer(self, buf, off, length):
        if length > 0:
            buf[off] = ord('x')
            return 1
        return 0


class IoTestCase(unittest.TestCase):
    """
    Jython was failing to read all available content when an InputStream
    returns early. Java's InputStream.read() is allowed to return less than the
    requested # of bytes under non-exceptional/EOF conditions, whereas
    (for example) wsgi.input requires the file.read() method to block until the
    requested # of bytes are available (except for exceptional/EOF conditions).

    See http://bugs.jython.org/issue1754 for more discussion.
    """
    def test_infinite_input(self):
        iis = InfiniteInputStream()
        f = FileUtil.wrap(iis, 'rb')
        size = 10000
        self.assertEqual(len(f.read(size)), size)
        self.assertEqual(len(f.read(size)), size)
        self.assertEqual(len(f.read(size)), size)

    def test_buffer_no_array(self):
        """
        Directly tests StreamIO with and without a backing array and an
        InputStream that returns early.
        """
        size = 10000
        without_array = ByteBuffer.allocateDirect(size)
        self.assertFalse(without_array.hasArray())
        with_array = ByteBuffer.allocate(size)
        self.assertTrue(with_array.hasArray())
        bbs = [with_array, without_array]
        for bb in bbs:
            iis = InfiniteInputStream()
            io = StreamIO(iis, True)
            self.assertEqual(io.readinto(bb), size)


if __name__ == '__main__':
    unittest.main()
