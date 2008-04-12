"""Test descriptors, binary ops, etc.

Made for Jython.
"""
import test_support
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


def test_main():
    test_support.run_unittest(TestDescrTestCase)

if __name__ == '__main__':
    test_main()
