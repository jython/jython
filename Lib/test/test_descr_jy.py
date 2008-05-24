"""Test descriptors, binary ops, etc.

Made for Jython.
"""
import test_support
import types
import unittest

class TestDescrTestCase(unittest.TestCase):

    def test_class_dict_is_copy(self):
        class FooMeta(type):
            def __new__(meta, name, bases, class_dict):
                cls = type.__new__(meta, name, bases, class_dict)
                self.assert_('foo' not in class_dict)
                cls.foo = 'bar'
                self.assert_('foo' not in class_dict)
                return cls

        class Foo(object):
            __metaclass__ = FooMeta

    def test_descr___get__(self):
        class Foo(object):
            __slots__ = 'bar'
            def hello(self):
                pass
            def hi(self):
                pass
            hi = staticmethod(hi)
        foo = Foo()
        foo.bar = 'baz'

        self.assertEqual(Foo.bar.__get__(foo), 'baz')
        self.assertEqual(Foo.bar.__get__(None, Foo), Foo.bar)

        bound = Foo.hello.__get__(foo)
        self.assert_(isinstance(bound, types.MethodType))
        self.assert_(bound.im_self is foo)
        self.assertEqual(Foo.hello.__get__(None, Foo), Foo.hello)

        bound = Foo.hi.__get__(foo)
        self.assert_(isinstance(bound, types.MethodType))
        self.assert_(bound.im_self is foo)
        unbound = Foo.hi.__get__(None, foo)
        self.assert_(isinstance(unbound, types.MethodType))
        self.assert_(unbound.im_self is None)


def test_main():
    test_support.run_unittest(TestDescrTestCase)

if __name__ == '__main__':
    test_main()
