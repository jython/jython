import unittest
from test import test_support
from java.util import HashMap
from org.python.tests import (InterfaceCombination, Invisible, OnlySubclassable, SubVisible,
        Visible, VisibleOverride)
from org.python.tests import VisibilityResults as Results

class VisibilityTest(unittest.TestCase):
    def test_invisible(self):
        for item in dir(Invisible):
            self.assert_(not item.startswith("package"))
            self.assert_(not item.startswith("private"))
            self.assert_(not item.startswith("protected"))
        self.assertRaises(TypeError, Invisible,
                "Calling a Java class with package protected constructors should raise a TypeError")

    def test_protected_from_python_subclass(self):
        class SubVisible(Visible):
            def __init__(self, publicValue=None):
                if publicValue is not None:
                    Visible.__init__(self, publicValue)
                else:
                    Visible.__init__(self)
        # TODO - protectedStaticMethod, protectedStaticField, StaticInner, and protectedField should
        # be here
        s = SubVisible()
        self.assertEquals(Results.PROTECTED_METHOD, s.protectedMethod(0))
        self.assertEquals(Results.OVERLOADED_PROTECTED_METHOD, s.protectedMethod('foo'))
        self.assertEquals(Results.UNUSED, SubVisible(Results.UNUSED).visibleField)
        self.assertRaises(TypeError, OnlySubclassable,
                "Calling a Java class with protected constructors should raise a TypeError")
        class SubSubclassable(OnlySubclassable):
            pass
        sub = SubSubclassable()
        self.assert_(not sub.filledInByConstructor == 0,
                '''Creating SubSubclassable should call OnlySubclassable's constructor to fill in
                filledInByConstructor''')



    def test_visible(self):
        v = Visible()
        self.assertEquals(Results.PUBLIC_FIELD, v.visibleField)
        self.assertEquals(Results.PUBLIC_STATIC_FIELD, Visible.visibleStaticField)
        self.assertEquals(Results.PUBLIC_METHOD, v.visibleInstance(0))
        self.assertEquals(Results.OVERLOADED_PUBLIC_METHOD, v.visibleInstance('a'))
        self.assertEquals(Results.EXTRA_ARG_PUBLIC_METHOD, v.visibleInstance(0, 'b'))
        self.assertEquals(Results.OVERLOADED_EXTRA_ARG_PUBLIC_METHOD,
                v.visibleInstance('a', 'b'))
        self.assertEquals(Results.PUBLIC_STATIC_METHOD, Visible.visibleStatic(0))
        self.assertEquals(Results.OVERLOADED_PUBLIC_STATIC_METHOD, 
                v.visibleStatic('a'))
        self.assertEquals(Results.EXTRA_ARG_PUBLIC_STATIC_METHOD,
                v.visibleStatic(0, 'a'))
        self.assertEquals(Results.PUBLIC_STATIC_FIELD, Visible.StaticInner.visibleStaticField)

        # Ensure that the visibleInstance method from SubVisible that takes a double doesn't
        # leak through to the parent
        self.assertRaises(TypeError, v.visibleInstance, 0.0, 'b')
        # TODO - no way to access a field with the same name as a method
        #self.assertEquals(Results.PUBLIC_METHOD_FIELD, v.visibleInstance)
        #self.assertEquals(Results.PUBLIC_STATIC_METHOD_FIELD, Visible.visibleStatic)

    def test_java_subclass(self):
        s = SubVisible()
        self.assertEquals(Results.PUBLIC_FIELD, s.visibleField)
        self.assertEquals(Results.PUBLIC_STATIC_FIELD, SubVisible.visibleStaticField)
        self.assertEquals(Results.SUBCLASS_STATIC_OVERRIDE, SubVisible.visibleStatic(3))
        self.assertEquals(Results.SUBCLASS_STATIC_OVERLOAD, SubVisible.visibleStatic(3.0, 'a'))
        self.assertEquals(Results.SUBCLASS_OVERRIDE, s.visibleInstance(3))
        self.assertEquals(Results.SUBCLASS_OVERLOAD, s.visibleInstance(3.0, 'a'))
        self.assertEquals(Results.PACKAGE_METHOD, s.packageMethod())
        # Java methods don't allow direct calling of the superclass method, so it should 
        # return the subclass value here.
        self.assertEquals(Results.SUBCLASS_OVERRIDE, Visible.visibleInstance(s, 3))
        self.assertEquals(Results.PUBLIC_STATIC_FIELD, SubVisible.StaticInner.visibleStaticField)


    def test_in_dict(self):
        for c in Visible, SubVisible, VisibleOverride:
            self.failUnless('visibleInstance' in c.__dict__,
                    'visibleInstance expected in %s __dict__' % c)

    def test_interface_combination(self):
        '''Checks that a private class that extends a public class and public interfaces has only the items
           from the public bases visible'''
        i = InterfaceCombination.newImplementation()
        self.assertEquals(InterfaceCombination.NO_ARG_RESULT, i.getValue(),
                "methods from IFace should be visible on Implementation")
        self.assertEquals(InterfaceCombination.ONE_ARG_RESULT, i.getValue("one arg"),
                "methods from IIFace should be visible on Implementation")
        self.assertEquals(InterfaceCombination.TWO_ARG_RESULT, i.getValue("one arg", "two arg"),
                "methods from Base should be visible on Implementation")
        self.assertRaises(TypeError, i.getValue, "one arg", "two arg", "three arg", 
                "methods defined solely on Implementation shouldn't be visible")
        self.assertFalse(hasattr(i, "internalMethod"),
                "methods from private interfaces shouldn't be visible on a private class")

class JavaClassTest(unittest.TestCase):
    def test_class_methods_visible(self):
        self.assertFalse(HashMap.isInterface(),
                'java.lang.Class methods should be visible on Class instances')
        self.assertFalse(HashMap.interface,
                'java.lang.Class bean methods should be visible on instances')
        self.assertEquals(3, len(HashMap.getInterfaces()))

class NumberCoercionTest(unittest.TestCase):
    def test_int_coercion(self):
        from org.python.tests import Coercions
        c = Coercions()
        self.assertEquals("5", c.takeInt(5))
        self.assertEquals("15", c.takeInteger(15))
        self.assertEquals("150", c.takeNumber(150))

def test_main():
    test_support.run_unittest(VisibilityTest,
            JavaClassTest,
            NumberCoercionTest)

if __name__ == "__main__":
    test_main()
