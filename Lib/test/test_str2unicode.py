import unittest

class TestStrReturnsUnicode(unittest.TestCase):
    def test_join(self):
	self.assertEquals(unicode, type(''.join([u'blah'])))

    def test_replace(self):
	self.assertEquals(unicode, type('hello'.replace('o', u'o')))


class TestStrReturnsStr(unittest.TestCase):
    def test_join(self):
	self.assertEquals(str, type(''.join(['blah'])))

    def test_replace(self):
	self.assertEquals(str, type('hello'.replace('o', 'oo')))

if __name__ == '__main__':
    unittest.main()
