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
 

def test_main():
    test.test_support.run_unittest(
        ByteArraySubclassTest)


if __name__ == "__main__":
    test_main()
