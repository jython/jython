"""Misc. type tests

Made for Jython
"""
import __builtin__
import unittest
from test import test_support

class TypesModuleTestCase(unittest.TestCase):

    def test_module(self):
        self.assertEqual(str.__module__, '__builtin__')
        class Foo:
            pass
        self.assertEqual(Foo.__module__, __name__)
        self.assertEqual(str(Foo), '%s.Foo' % __name__)
        self.assert_(repr(Foo).startswith('<class %s.Foo ' % __name__))
        foo = Foo()
        self.assert_(str(foo).startswith('<%s.Foo instance ' % __name__))

        class Bar(object):
            pass
        self.assertEqual(Bar.__module__, __name__)
        self.assertEqual(str(Bar), "<class '%s.Bar'>" % __name__)
        self.assertEqual(repr(Bar), "<class '%s.Bar'>" % __name__)
        bar = Bar()
        self.assert_(str(bar).startswith('<%s.Bar object ' % __name__))


class TypesNamelessModuleTestCase(unittest.TestCase):

    def setUp(self):
        global __name__
        self.name = __name__
        del __name__

    def tearDown(self):
        global __name__
        __name__ = self.name

    def test_nameless_module(self):
        class Foo:
            pass
        self.assertEqual(Foo.__module__, '__builtin__')
        self.assertEqual(str(Foo), '__builtin__.Foo')
        self.assert_(repr(Foo).startswith('<class __builtin__.Foo '))
        foo = Foo()
        self.assert_(str(foo).startswith('<__builtin__.Foo instance '))

        class Bar(object):
            pass
        self.assertEqual(Bar.__module__, '__builtin__')
        self.assertEqual(str(Bar), "<class 'Bar'>")
        self.assertEqual(repr(Bar), "<class 'Bar'>")
        bar = Bar()
        self.assert_(str(bar).startswith('<Bar '))
        self.assert_(str(bar).startswith('<Bar object '))


class BrokenNameTestCase(unittest.TestCase):

    def setUp(self):
        global __name__
        self.name = __name__
        self.builtin_name = __builtin__.__name__
        del __name__
        del __builtin__.__name__

    def tearDown(self):
        global __name__
        __builtin__.__name__ = self.builtin_name
        __name__ = self.name

    def test_broken_name(self):
        try:
            class Foobar:
                pass
        except NameError, ne:
            pass
        else:
            self.assert_(False, "Expected a NameError")


def test_main():
    test_support.run_unittest(TypesModuleTestCase,
                              TypesNamelessModuleTestCase,
                              BrokenNameTestCase)


if __name__ == "__main__":
    test_main()
