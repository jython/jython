# test overloaded java methods dispatch logic in PyReflectedFunction
# needs to grow more tests. Uses javatests.JOverload as a bag of overloaded methods.
# (can be adapted to test alternative re-implemations even while they are developed
# write a *Envl class and change/add to to_test for that)

import sys
import unittest

import java
from java.lang import Float, Double, Integer, Long, Boolean, Exception
from java.util import ArrayList
from javatests import JOverload, Reflection
from org.python.core import PyReflectedFunction

class PyReflFuncEnvl:

    def __init__(self,name,meths):
        self.reflfunc = PyReflectedFunction(meths)

    def __call__(self,inst,args):
        return self.reflfunc(inst,*args)

def extract_ov_meths(jcl,envl_class):
    meths = java.lang.Class.getDeclaredMethods(jcl)
    names = [ m.name for m in meths]
    meth_dict = {}
    for name in names:
        if name.startswith('ov_') and not meth_dict.has_key(name):
            meth_dict[name] = envl_class(name,[ m for m in meths if m.name == name ])
    return meth_dict

jo = JOverload()

to_test = [extract_ov_meths(JOverload,PyReflFuncEnvl)]

class OverloadedDispatchTests(unittest.TestCase):

    def check(self,lbl,rng,args,expected):
        expected = expected.split()
        for meth_dict in to_test:
            for i,expect in zip(rng,expected):
                self.assertEqual(meth_dict['ov_%s%s' % (lbl,i)](jo,args),expect)

    def test_posprec(self):
        self.check('posprec',[1,2],[0,0],
                   "(int,long) (long,int)")

    def test_scal_int_zero(self):
        self.check('scal',xrange(1,15),[0],
                   """
(long)
(int)
(short)
(byte)
(byte)
(double)
(float)
(boolean)
(java.io.Serializable)
(java.io.Serializable)
(java.io.Serializable)
(java.io.Serializable)
(java.io.Serializable)
(java.lang.Object)
                   """)

    def test_scal_string(self):
        self.check('scal',xrange(1,15),['str'],
                   """
(java.lang.String)
(java.lang.String)
(java.lang.String)
(java.lang.String)
(java.lang.String)
(java.lang.String)
(java.lang.String)
(java.lang.String)
(java.lang.String)
(java.io.Serializable)
(java.io.Serializable)
(java.io.Serializable)
(java.io.Serializable)
(java.lang.Object)
                   """)

    def test_scal_char(self):
        self.check('scal',xrange(1,15),['c'],
                   """
(char)
(char)
(char)
(char)
(java.lang.String)
(java.lang.String)
(java.lang.String)
(java.lang.String)
(java.lang.String)
(java.io.Serializable)
(java.io.Serializable)
(java.io.Serializable)
(java.io.Serializable)
(java.lang.Object)
                   """)

    def test_scal_float_one(self):
        self.check('scal',xrange(1,15),[1.0],
                   """
(double)
(double)
(double)
(double)
(double)
(double)
(float)
(java.io.Serializable)
(java.io.Serializable)
(java.io.Serializable)
(java.io.Serializable)
(java.io.Serializable)
(java.io.Serializable)
(java.lang.Object)
                   """)


class VarargsDispatchTests(unittest.TestCase):

    def test_strings(self):
        t = Reflection.StringVarargs()
        self.assertEqual(t.test("abc", "xyz"),
                         "String...:[abc, xyz]")
        self.assertEqual(t.test("abc"),
                         "String...:[abc]")
        self.assertEqual(t.test(),
                         "String...:[]")

        self.assertEqual(t.test(["abc", "xyz"]),
                         "String...:[abc, xyz]")
        self.assertEqual(t.test(["abc"]),
                         "String...:[abc]")
        self.assertEqual(t.test([]),
                         "String...:[]")

        self.assertEqual(t.testOneFixedArg("abc"),
                         "String arg1:abc String...:[]");
        self.assertEqual(t.testOneFixedArg("abc", "xyz"),
                         "String arg1:abc String...:[xyz]");
        self.assertEqual(t.testOneFixedArg("abc", "xyz", "123"),
                         "String arg1:abc String...:[xyz, 123]");

        self.assertEqual(t.testTwoFixedArg("fix1", "fix2"),
                         "String arg1:fix1 String arg2:fix2 String...:[]");
        self.assertEqual(t.testTwoFixedArg("fix1", "fix2", "var1"),
                         "String arg1:fix1 String arg2:fix2 String...:[var1]");
        self.assertEqual(t.testTwoFixedArg("fix1", "fix2", "var1", "var2"),
                         "String arg1:fix1 String arg2:fix2 String...:[var1, var2]");


    def test_lists(self):
        t = Reflection.ListVarargs()
        self.assertEqual(t.test(ArrayList([1,2,3]), ArrayList([4,5,6])),
                         "List...:[[1, 2, 3], [4, 5, 6]]")
        self.assertEqual(t.test(ArrayList([1,2,3])),
                         "List...:[[1, 2, 3]]")
        self.assertEqual(t.test(),
                         "List...:[]")

        self.assertEqual(t.test([ArrayList([1,2,3]), ArrayList([4,5,6])]),
                         "List...:[[1, 2, 3], [4, 5, 6]]")
        self.assertEqual(t.test([ArrayList([1,2,3])]),
                         "List...:[[1, 2, 3]]")
        self.assertEqual(t.test([]),
                         "List...:[]")

    def test_booleans(self):
        t = Reflection.BooleanVarargs()


        self.assertEqual(t.test(True, False),
                         "booleans...:[true, false]")
        self.assertEqual(t.test(True),
                         "booleans...:[true]")
        self.assertEqual(t.test(),
                         "booleans...:[]")

        self.assertEqual(t.testOneFixedArg(True),
                         "boolean arg1:true booleans...:[]");
        self.assertEqual(t.testOneFixedArg(True, False),
                         "boolean arg1:true booleans...:[false]");
        self.assertEqual(t.testOneFixedArg(True, False, True),
                         "boolean arg1:true booleans...:[false, true]");

        self.assertEqual(t.testTwoFixedArg(True, False),
                         "boolean arg1:true boolean arg2:false booleans...:[]");
        self.assertEqual(t.testTwoFixedArg(True, False, True),
                         "boolean arg1:true boolean arg2:false booleans...:[true]");
        self.assertEqual(t.testTwoFixedArg(True, False, True, False),
                         "boolean arg1:true boolean arg2:false booleans...:[true, false]");


class ComplexOverloadingTests(unittest.TestCase):
    def test_constructor_overloading(self):
        self.assertEqual(Reflection.Overloaded().constructorVersion, '')
        self.assertEqual(Reflection.Overloaded(1).constructorVersion, 'int')
        self.assertEqual(Reflection.Overloaded(1, 1).constructorVersion, 'int, int')
        self.assertEqual(Reflection.Overloaded(1, 1, 1).constructorVersion, 'int, int, Object')
        self.assertEqual(Reflection.Overloaded(1, 1, 1, 1).constructorVersion, 'int, int...')

        self.assertEqual(Reflection.Overloaded(1, [2,3,4]).constructorVersion, 'int, int...')

        b = Exception("Oops")
        self.assertEqual(Reflection.Overloaded("aa").constructorVersion, "String")
        self.assertEqual(Reflection.Overloaded("aa", 2).constructorVersion, "String, Object...")
        self.assertEqual(Reflection.Overloaded("aa", b).constructorVersion, "String, Throwable")
        self.assertEqual(Reflection.Overloaded("aa", b, 3).constructorVersion, "String, Throwable, Object...")

    def test_method_overloading(self):
        over = Reflection.Overloaded()
        self.assertEqual(over.foo(), "int...")
        self.assertEqual(over.foo(1, 2), "int, int")
        self.assertEqual(over.foo(1, 2, 3), "int, int, Object")
        self.assertEqual(over.foo(1, [2, 3, 4]), 'int, int...')
        # Note in Java these match both foo(int,int...) and foo(int...):
        self.assertEqual(over.foo(1), "int, int...")
        self.assertEqual(over.foo(1, 2, 3, 4), "int, int...")

    def test_method_most_specific(self):
        over = Reflection.Overloaded()
        # Java constructors may be used to specify argument types
        self.assertEqual(over.bar(Integer(1)), "int")
        self.assertEqual(over.bar(Long(1)), "long")
        self.assertEqual(over.bar(Boolean(True)), "boolean")
        self.assertEqual(over.bar(Float(1.)), "float")
        self.assertEqual(over.bar(Double(1.)), "Number")
        # For better or worse (for 25yrs), function returns are coerced to Python:
        self.assertEqual(over.bar(Integer.valueOf(1)), "long")
        self.assertEqual(over.bar(Boolean.valueOf(True)), "long")
        self.assertEqual(over.bar(Float.valueOf(1.)), "float")
        self.assertEqual(over.bar(Double(1.).valueOf(1.)), "float")

    def test_complex(self):
        o = Reflection.Overloaded()
        self.assertEqual(o(2.), "class java.lang.Double=2.0")
        self.assertEqual(o(1+2j), "class org.python.core.PyComplex=(1+2j)")



def printout(meth_dict,lbl,rng,args):
    for i in rng:
        print meth_dict['ov_%s%s' % (lbl,i)](jo,args)


def test_main():
    from test import test_support
    test_support.run_unittest(
        OverloadedDispatchTests,
        VarargsDispatchTests,
        ComplexOverloadingTests,
    )

if __name__ == '__main__':
    test_main()
