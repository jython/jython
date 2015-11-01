import unittest
from test import test_support

import _io

class NameTest(unittest.TestCase):

    def test_names_available_in__io_module(self):
        # verifies fix for http://bugs.jython.org/issue2368
        self.assertGreaterEqual(
            set(dir(_io)),
            { 'BlockingIOError', 'BufferedRWPair', 'BufferedRandom',
              'BufferedReader', 'BufferedWriter', 'BytesIO',
              'DEFAULT_BUFFER_SIZE', 'FileIO', 'IncrementalNewlineDecoder',
              'TextIOWrapper', 'UnsupportedOperation',
              '_BufferedIOBase', '_IOBase', '_RawIOBase', '_TextIOBase'
            })

def test_main():
    test_support.run_unittest(NameTest)

if __name__ == "__main__":
    test_main()
