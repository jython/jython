import re
import unittest
import test.test_support

class ReTest(unittest.TestCase):

    def test_bug_1140_addendum(self):
        result = re.sub('', lambda match : None, 'foo')
        self.assertEqual(result, 'foo')
        self.assert_(isinstance(result, str))
        
    def test_unkown_groupname(self):
        self.assertRaises(IndexError,
                          re.match("(?P<int>\d+)\.(\d*)", '3.14').group,
                          "misspelled")

def test_main():
        test.test_support.run_unittest(ReTest)

if __name__ == "__main__":
        test_main()
