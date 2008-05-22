"""Tests whether Python keywords that are *not* Java keywords can be used
   as method names (needed for Java integration -- for example, if a Java
   method is named "print" and we want to use it from Jython).

Made for Jython.
"""

import test_support
import unittest

class Keywords(object):
    def exec(self):
        exec "2+2"
        return "success"

    def as(self):
        import unittest as eggs
        return "success"

class GrammarTest(unittest.TestCase):
    def testKeywords(self):
        kws = Keywords()
        self.assertEquals("success", kws.exec())
        self.assertEquals("success", kws.as())

    def testStringPrefixes(self):
        self.assertEquals(u"spam",U"spam")
        self.assertEquals(r"spam", R"spam")
        self.assertEquals(uR"spam", Ur"spam")
        self.assertEquals(ur"spam", UR"spam")

def test_main():
    test_support.run_unittest(GrammarTest)

if __name__ == '__main__':
    test_main()
