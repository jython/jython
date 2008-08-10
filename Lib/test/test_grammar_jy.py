""" Extra grammar tests for Jython.
"""

from test import test_support
import unittest

class GrammarTest(unittest.TestCase):

    def testStringPrefixes(self):
        self.assertEquals(u"spam",U"spam")
        self.assertEquals(r"spam", R"spam")
        self.assertEquals(uR"spam", Ur"spam")
        self.assertEquals(ur"spam", UR"spam")

def test_main():
    test_support.run_unittest(GrammarTest)

if __name__ == '__main__':
    test_main()
