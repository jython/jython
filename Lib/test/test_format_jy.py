"""String foramtting tests

Made for Jython.
"""
import test_support
import unittest

class FormatTestCase(unittest.TestCase):

    def test_overflow(self):
        # Ensure a float that would convert to a long isn't accepted
        for format in 'i', 'd', 'u', 'o', 'x', 'X':
            try:
                ('%%%s' % format) % 3.7517675036461267e17
            except TypeError, te:
                self.assertEqual(str(te), 'int argument required')
            else:
                self.fail('Expected TypeError')


def test_main():
    test_support.run_unittest(FormatTestCase)

if __name__ == '__main__':
    test_main()
