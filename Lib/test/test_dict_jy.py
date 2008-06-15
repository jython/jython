import test_support
import unittest

class DictInitTest(unittest.TestCase):
    def testInternalSetitemInInit(self):
        '''Test for http://jython.org/bugs/1816134

        CPython's dict uses an internal setitem method to initialize itself
        rather than the one on its subclasses, and this tests that Jython does
        as well.
        '''
        class Subdict(dict):
            def __init__(self):
                super(Subdict, self).__init__([('a',1)])
                self.createdInInit = 1

            def __setitem__(self, key, value):
                super(Subdict, self).__setitem__(key, value)
                assert hasattr(self, 'createdInInit')
                self.createdInInit = value

        s = Subdict()
        s[7] = 'called'
        self.assertEquals('called', s.createdInInit)

class DerivedDictTest(unittest.TestCase):
    "Tests for derived dict behaviour"
    def test_raising_custom_key_error(self):
        class CustomKeyError(KeyError):
            pass
        class DerivedDict(dict):
            def __getitem__(self, key):
                raise CustomKeyError("custom message")
        self.assertRaises(CustomKeyError, lambda: DerivedDict()['foo'])


def test_main():
    test_support.run_unittest(DictInitTest, DerivedDictTest)

if __name__ == '__main__':
    test_main()
