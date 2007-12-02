import doctest
import test_support
import unittest

from encodings import shift_jis, euc_jp, iso2022_jp, ms932

illegalencoded = '\x80\x00'
unmappablechar = u'\u1234'
class TestErrorHandling(unittest.TestCase):
    '''Checks that the Java error handling is correcly mapped into Python'''

    def testStrictRaisesUnicodeError(self):
        self.assertRaises(UnicodeError, illegalencoded.decode, 'Shift_JIS')
        self.assertRaises(UnicodeError, 
                illegalencoded.decode, 'Shift_JIS', 'strict')
        self.assertRaises(UnicodeError, unmappablechar.encode, 'Shift_JIS')
        self.assertRaises(UnicodeError, unmappablechar.encode, 'Shift_JIS',
                'strict')

    def testIgnoreRaisesNoError(self):
        self.assertEquals(u'\x00', 
            illegalencoded.decode('Shift_JIS', 'ignore'))
        self.assertEquals('', unmappablechar.encode('Shift_JIS', 'ignore'))
        self.assertEquals('', unmappablechar.encode('Shift_JIS', 'ignore'))

    def testReplaceReplaces(self):
        self.assertEquals(u'\uFFFD\x00', 
            illegalencoded.decode('Shift_JIS', 'replace'))
        self.assertEquals('?', unmappablechar.encode('Shift_JIS', 'replace'))

def test_main():
    test_support.run_unittest(TestErrorHandling)

    for mod in shift_jis, euc_jp, iso2022_jp, ms932:
        test_support.run_doctest(mod)

if __name__ == "__main__":
    test_main()
