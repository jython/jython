"""Misc. class tests. These are more general class tests than CPython's
test_class which focuses on operators.

Made for Jython
"""
import __builtin__
import unittest
from test import test_support

class TypesGeneralTestCase(unittest.TestCase):

    TE_MSG = "can't set attributes of built-in/extension type 'str'"

    def test_dunder_module(self):
        self.assertEqual(str.__module__, '__builtin__')
        class Foo:
            pass
        self.assertEqual(Foo.__module__, __name__)
        self.assertEqual(str(Foo), '%s.Foo' % __name__)
        self.assert_(repr(Foo).startswith('<class %s.Foo at' % __name__))
        foo = Foo()
        self.assert_(str(foo).startswith('<%s.Foo instance at' % __name__))

        class Bar(object):
            pass
        self.assertEqual(Bar.__module__, __name__)
        self.assertEqual(str(Bar), "<class '%s.Bar'>" % __name__)
        self.assertEqual(repr(Bar), "<class '%s.Bar'>" % __name__)
        bar = Bar()
        self.assert_(str(bar).startswith('<%s.Bar object at' % __name__))


    def test_builtin_attributes(self):
        for attr, val in dict(__name__='foo', __module__='bar', __dict__={},
                              __flags__=1, __base__=object,
                              __bases__=(unicode, object),
                              __mro__=(unicode, object)).iteritems():
            try:
                setattr(str, attr, val)
            except TypeError, te:
                self.assertEqual(str(te), self.TE_MSG)
            else:
                self.assert_(False,
                             'setattr str.%s expected a TypeError' % attr)
            try:
                delattr(str, attr)
            except TypeError, te:
                self.assertEqual(str(te), self.TE_MSG)
            else:
                self.assert_(False,
                             'delattr str.%s expected a TypeError' % attr)


    def test_attributes(self):
        class Foo(object):
            pass

        Foo.__name__ = 'Bar'
        self.assertEqual(Foo.__name__, 'Bar')
        try:
            del Foo.__name__
        except TypeError, te:
            self.assertEqual(str(te), "can't delete Bar.__name__")
        else:
            self.assert_(False, 'Expected a TypeError')

        Foo.__module__ = 'baz'
        self.assertEqual(Foo.__module__, 'baz')
        try:
            del Foo.__module__
        except TypeError, te:
            self.assertEqual(str(te), "can't delete Bar.__module__")
        else:
            self.assert_(False, 'Expected a TypeError')

        try:
            Foo.__dict__ = {}
        except AttributeError, ae:
            self.assertEqual(str(ae),
                             "attribute '__dict__' of 'type' objects is not "
                             "writable")
        else:
            self.assert_(False, 'Expected an AttributeError')
        try:
            del Foo.__dict__
        except AttributeError, ae:
            self.assertEqual(str(ae),
                             "attribute '__dict__' of 'type' objects is not "
                             "writable")
        else:
            self.assert_(False, 'Expected an AttributeError')

        for attr, val in dict(__flags__=1, __base__=object,
                              __bases__=(unicode, object),
                              __mro__=(unicode, object)).iteritems():
            try:
                setattr(str, attr, val)
            except TypeError, te:
                self.assertEqual(str(te), self.TE_MSG)
            else:
                self.assert_(False,
                             'setattr Foo.%s expected a TypeError' % attr)
            try:
                delattr(str, attr)
            except TypeError, te:
                self.assertEqual(str(te), self.TE_MSG)
            else:
                self.assert_(False,
                             'delattr Foo.%s expected a TypeError' % attr)


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
        self.assert_(repr(Foo).startswith('<class __builtin__.Foo at'))
        foo = Foo()
        self.assert_(str(foo).startswith('<__builtin__.Foo instance at'))

        class Bar(object):
            pass
        self.assertEqual(Bar.__module__, '__builtin__')
        self.assertEqual(str(Bar), "<class 'Bar'>")
        self.assertEqual(repr(Bar), "<class 'Bar'>")
        bar = Bar()
        self.assert_(str(bar).startswith('<Bar '))
        self.assert_(repr(bar).startswith('<Bar object at'))


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
        except NameError:
            pass
        else:
            self.assert_(False, "Expected a NameError")


def test_main():
    test_support.run_unittest(TypesGeneralTestCase,
                              TypesNamelessModuleTestCase,
                              BrokenNameTestCase)


if __name__ == "__main__":
    test_main()
