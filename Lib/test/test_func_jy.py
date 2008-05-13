"""Misc func tests.

Made for Jython.
"""
import types
import unittest
from test import test_support

xyz = 123

def abc():
    return xyz

class FunctionTypeTestCase(unittest.TestCase):

    def test_func(self):
        self.assertEquals(abc(), 123)

    def test_functiontype(self):
        new_abc = types.FunctionType(abc.func_code, {'xyz': 456},
                                     abc.func_name, abc.func_defaults,
                                     abc.func_closure)
        self.assertEquals(new_abc(), 456)

    def test_functiontype_from_globals(self):
        sm = type(globals())()
        sm.update({'xyz': 789})
        sm_abc = types.FunctionType(abc.func_code, sm, abc.func_name,
                                    abc.func_defaults, abc.func_closure)
        self.assertEquals(sm_abc(), 789)


def test_main():
    test_support.run_unittest(FunctionTypeTestCase)

if __name__ == '__main__':
    test_main()
