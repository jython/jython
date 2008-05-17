import re
import unittest
import test.test_support

class ReTest(unittest.TestCase):

    def test_unkown_groupname(self):
        self.assertRaises(IndexError,
                          re.match("(?P<int>\d+)\.(\d*)", '3.14').group,
                          "misspelled")

    def test_no_empty_string_no_match(self):
        # must return pattern[0:0] here
        result = re.sub('^foo', 'bar', u'')
        self.assert_(isinstance(result, str))
        self.assertEqual(result, '')

        result = re.sub(u'^foo', 'bar', '')
        self.assert_(isinstance(result, unicode))
        self.assertEqual(result, u'')

def test_main():
        test.test_support.run_unittest(ReTest)

if __name__ == "__main__":
        test_main()
