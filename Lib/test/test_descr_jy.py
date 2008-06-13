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

    def test_ints(self):
        class C(int):
            pass
        try:
            foo = int(None)
        except TypeError:
            pass
        else:
            self.assert_(False, "should have raised TypeError")
        try:
            foo = C(None)
        except TypeError:
            pass
        else:
            self.assert_(False, "should have raised TypeError")

class SubclassDescrTestCase(unittest.TestCase):

    def test_subclass_cmp_right_op(self):
        # Case 1: subclass of int

        class B(int):
            def __ge__(self, other):
                return "B.__ge__"
            def __le__(self, other):
                return "B.__le__"

        self.assertEqual(B(1) >= 1, "B.__ge__")
        self.assertEqual(1 >= B(1), "B.__le__")

        # Case 2: subclass of object

        class C(object):
            def __ge__(self, other):
                return "C.__ge__"
            def __le__(self, other):
                return "C.__le__"

        self.assertEqual(C() >= 1, "C.__ge__")
        self.assertEqual(1 >= C(), "C.__le__")

        # Case 3: subclass of new-style class; here it gets interesting

        class D(C):
            def __ge__(self, other):
                return "D.__ge__"
            def __le__(self, other):
                return "D.__le__"

        self.assertEqual(D() >= C(), "D.__ge__")
        self.assertEqual(C() >= D(), "D.__le__")

        # Case 4: comparison is different than other binops

        class E(C):
            pass

        self.assertEqual(E.__le__, C.__le__)

        self.assertEqual(E() >= 1, "C.__ge__")
        self.assertEqual(1 >= E(), "C.__le__")
        self.assertEqual(E() >= C(), "C.__ge__")
        self.assertEqual(C() >= E(), "C.__le__") # different

    def test_subclass_binop(self):
        def raises(exc, expected, callable, *args):
            try:
                callable(*args)
            except exc, msg:
                if str(msg) != expected:
                    self.assert_(False, "Message %r, expected %r" % (str(msg),
                                                                     expected))
            else:
                self.assert_(False, "Expected %s" % exc)

        class B(object):
            pass

        class C(object):
            def __radd__(self, o):
                return '%r + C()' % (o,)

            def __rmul__(self, o):
                return '%r * C()' % (o,)

        # Test strs, unicode, lists and tuples
        mapping = []
        
        # + binop
        mapping.append((lambda o: 'foo' + o,
                        TypeError, "cannot concatenate 'str' and 'B' objects",
                        "'foo' + C()"))
        # XXX: There's probably work to be done here besides just emulating this
        # message
        #mapping.append((lambda o: u'foo' + o,<>
        #                TypeError,
        #                'coercing to Unicode: need string or buffer, B found',
        #                "u'foo' + C()"))
        mapping.append((lambda o: u'foo' + o,
                        TypeError, "cannot concatenate 'unicode' and 'B' objects",
                        "u'foo' + C()"))
        mapping.append((lambda o: [1, 2] + o,
                        TypeError, 'can only concatenate list (not "B") to list',
                        '[1, 2] + C()'))
        mapping.append((lambda o: ('foo', 'bar') + o,
                        TypeError, 'can only concatenate tuple (not "B") to tuple',
                        "('foo', 'bar') + C()"))

        # * binop
        mapping.append((lambda o: 'foo' * o,
                        TypeError, "can't multiply sequence by non-int of type 'B'",
                        "'foo' * C()"))
        mapping.append((lambda o: u'foo' * o,
                        TypeError, "can't multiply sequence by non-int of type 'B'",
                        "u'foo' * C()"))
        mapping.append((lambda o: [1, 2] * o,
                        TypeError, "can't multiply sequence by non-int of type 'B'",
                        '[1, 2] * C()'))
        mapping.append((lambda o: ('foo', 'bar') * o,
                        TypeError, "can't multiply sequence by non-int of type 'B'",
                        "('foo', 'bar') * C()"))

        for func, bexc, bexc_msg, cresult in mapping:
            raises(bexc, bexc_msg, lambda : func(B()))
            self.assertEqual(func(C()), cresult)


def test_main():
    test_support.run_unittest(TestDescrTestCase,
                              SubclassDescrTestCase)

if __name__ == '__main__':
    test_main()
