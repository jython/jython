"""Test handling of newlines via file's read and readline

Made for Jython.
"""
import os
import tempfile
import test.test_support as test_support
import unittest

assert not os.linesep == '\r', ('os.linesep of  %r is not supported' %
                                os.linesep)

LF = os.linesep == '\n'
CRLF = os.linesep == '\r\n'

CRLF_TEST = 'CR\rLF\nCRLF\r\nEOF'

class BaseTestCase(unittest.TestCase):

    data = CRLF_TEST
    write_mode = 'wb'
    mode = 'r'

    def setUp(self):
        self.filename = tempfile.mktemp()
        fp = open(self.filename, self.write_mode)
        fp.write(self.data)
        fp.close()
        self.fp = open(self.filename, self.mode)

    def tearDown(self):
        if self.fp:
            self.fp.close()
        os.remove(self.filename)


class BinaryNewlinesTestCase(BaseTestCase):

    mode = 'rb'

    def test_binary_read(self):
        read(self.fp, CRLF_TEST)

        self.fp.seek(0)
        read(self.fp, CRLF_TEST, len(CRLF_TEST))

    def test_binary_readline(self):
        readline(self.fp, 'CR\rLF\n')
        readline(self.fp, 'CRLF\r\n')
        readline(self.fp, 'EOF')


class ReadTextNewlinesTestCase(BaseTestCase):

    def test_text_read(self):
        if LF:
            read(self.fp, 'CR\rLF\nCRLF\r\nEOF')
        elif CRLF:
            read(self.fp, 'CR\rLF\nCRLF\nEOF')

        self.fp.seek(0)
        read(self.fp, 'CR\r', 3)
        read(self.fp, 'LF\n', 3)
        if LF:
            read(self.fp, 'CRLF\r\n', 6)
        elif CRLF:
            read(self.fp, 'CRLF\n', 5)
        read(self.fp, 'EOF', 3)

    def _test_text_readline(self):
        readline(self.fp, 'CR\rLF\n')
        if LF:
            readline(self.fp, 'CRLF\r\n')
        elif CRLF:
            readline(self.fp, 'CRLF\n')
        readline(self.fp, 'EOF')

        self.fp.seek(0)
        readline(self.fp, 'CR\rLF\n')
        if LF:
            readline(self.fp, 'CRLF\r', 5)
            readline(self.fp, '\n')
        elif CRLF:
            readline(self.fp, 'CRLF\n', 5)
            readline(self.fp, 'EOF')


class ReadTextBoundaryTestCase(BaseTestCase):

    data = 'CR\r'

    def test_read_boundary(self):
        read(self.fp, 'CR\r')
        self.fp.seek(0)
        read(self.fp, 'CR\r', 3)

    def test_readline_boundary(self):
        readline(self.fp, 'CR\r')
        self.fp.seek(0)
        readline(self.fp, 'CR\r', 3)


class WriteTextNewlinesTestCase(BaseTestCase):

    write_mode = 'w'
    mode = 'rb'

    def test_text_written(self):
        if LF:
            readline(self.fp, 'CR\rLF\n')
            readline(self.fp, 'CRLF\r\n')
        elif CRLF:
            readline(self.fp, 'CR\rLF\r\n')
            readline(self.fp, 'CRLF\r\r\n')
        readline(self.fp, 'EOF')


class ReadUniversalNewlinesTestCase(BaseTestCase):

    mode = 'rU'

    def test_read(self):
        read(self.fp, 'CR\nLF\nCRLF\nEOF')

        self.fp.seek(0)
        read(self.fp, 'CR\nLF\nCRLF\nEOF', 14)

    def test_readline(self):
        readline(self.fp, 'CR\n')
        assert self.fp.newlines == None, repr(self.fp.newlines)
        readline(self.fp, 'LF\n')
        assert self.fp.newlines == ('\r', '\n'), repr(self.fp.newlines)
        readline(self.fp, 'CRLF\n')
        assert self.fp.newlines == ('\r', '\n'), repr(self.fp.newlines)
        readline(self.fp, 'EOF')
        assert self.fp.newlines == ('\r', '\n', '\r\n'), repr(self.fp.newlines)

        self.fp.seek(0)
        readline(self.fp, 'CR\n', 3)
        readline(self.fp, 'LF\n', 3)
        readline(self.fp, 'CRLF\n', 5)
        readline(self.fp, 'EOF', 3)

    def test_seek(self):
        # Ensure seek doesn't confuse CRLF newline identification
        self.fp.seek(6)
        readline(self.fp, 'CRLF\n')
        assert self.fp.newlines == None
        self.fp.seek(5)
        readline(self.fp, '\n')
        assert self.fp.newlines == '\n'

class WriteUniversalNewlinesTestCase(unittest.TestCase):

    def test_fails(self):
        try:
            open(tempfile.mktemp(), 'wU')
        except ValueError:
            pass
        else:
            raise AssertionError("file mode 'wU' did not raise a "
                                 "ValueError")
    

def read(fp, data, size=-1):
    line = fp.read(size)
    assert line == data, 'read: %r expected: %r' % (line, data)


def readline(fp, data, size=-1):
    line = fp.readline(size)
    assert line == data, 'readline: %r expected: %r' % (line, data)


def test_main():
    test_support.run_unittest(BinaryNewlinesTestCase,
                              ReadTextNewlinesTestCase,
                              ReadTextBoundaryTestCase,
                              WriteTextNewlinesTestCase,
                              ReadUniversalNewlinesTestCase,
                              WriteUniversalNewlinesTestCase)

if __name__ == '__main__':
    test_main()
