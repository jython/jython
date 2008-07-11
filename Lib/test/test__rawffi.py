import unittest
from test import test_support

class RawFFITestCase(unittest.TestCase):

    def setUp(self):
        self.libc_name = "c"

    def test_libload(self):
        import _rawffi
        _rawffi.CDLL(self.libc_name)

    def test_libc_load(self):
        import _rawffi
        _rawffi.get_libc()

    def test_getattr(self):
        import _rawffi
        libc = _rawffi.get_libc()
        func = libc.ptr('rand', [], 'i')
        assert libc.ptr('rand', [], 'i') is func # caching
        assert libc.ptr('rand', [], 'l') is not func
        assert isinstance(func, _rawffi.FuncPtr)
        self.assertRaises(AttributeError, getattr, libc, "xxx")

def test_main():
    tests = [RawFFITestCase,
             ]
    test_support.run_unittest(*tests)

if __name__ == '__main__':
    test_main()
