import os
import unittest
import sys

from test import test_support
from java.awt import Component
from java.util import Vector
from java.io import FileOutputStream, FileWriter

# The following is the correspoding bytecode compiled with javac 1.5
"""
public abstract class Abstract {
    public Abstract() {
        method();
    }

    public abstract void method();
}
"""
ABSTRACT_CLASS = """\
eJw1TrsKwkAQnI1nEmMe/oKdSaHYiyCClWih2F+SQyOaQDz9LxsFCz/AjxL3Am6xw8zs7O7n+3oD
GKPnQcD30ELgIHQQEexJURZ6SmgN4h1BzKtcEaJlUarV9ZyqeivTEyv2WelDlRO8TXWtM7UojBrM
0ouuZaaHR3mTPtqwfXRgE9y/Q+gZb3SS5X60To8q06LPHwiYskAmxN1hFjMSYyd5gpIHrDsT3sU9
5IgZF4wuhCBzpnG9Ru/+AF4RJn8=
""".decode('base64').decode('zlib')

class AbstractOnSyspathTest(unittest.TestCase):
    '''Subclasses an abstract class that isn't on the startup classpath.
    
    Checks for http://jython.org/bugs/1861985
    '''
    def setUp(self):
        out = open('Abstract.class', 'w')
        out.write(ABSTRACT_CLASS)
        out.close()
        self.orig_syspath = sys.path[:]
        sys.path.append('')

    def tearDown(self):
        os.unlink('Abstract.class')
        sys.path = self.orig_syspath

    def test_can_subclass_abstract(self):
        import Abstract
        
        class A(Abstract):
            def method(self):
                pass
        A()

class InstantiationTest(unittest.TestCase):
    def test_cant_create_abstract(self):
        self.assertRaises(TypeError, Component)

    def test_can_subclass_abstract(self):
        class A(Component):
            pass
        A()

    def test_cant_instantiate_abstract(self):
        self.assertRaises(TypeError, Component)

    def test_str_doesnt_coerce_to_int(self):
        from java.util import Date
        self.assertRaises(TypeError, Date, '99-01-01', 1, 1)


class BeanTest(unittest.TestCase):
    def test_shared_names(self):
        self.failUnless(callable(Vector.size),
                'size method should be preferred to writeonly field')

    def test_multiple_listeners(self):
        '''Check that multiple BEP can be assigned to a single cast listener'''
        from org.python.tests import Listenable
        from java.awt.event import ComponentEvent
        from java.awt import Container
        m = Listenable()
        called = []
        def f(evt, called=called):
            called.append(0)

        m.componentShown = f
        m.componentHidden = f

        m.fireComponentShown(ComponentEvent(Container(), 0))
        self.assertEquals(1, len(called))
        m.fireComponentHidden(ComponentEvent(Container(), 0))
        self.assertEquals(2, len(called))

class MethodVisibilityTest(unittest.TestCase):
    def test_in_dict(self):
        from java.awt import Container, MenuContainer, Component
        for c in Container, MenuContainer, Component:
            self.failUnless('remove' in c.__dict__,
                    'remove expected in %s __dict__' % c)

    def test_parent_cant_call_subclass(self):
        '''
        Makes sure that Base doesn't pick up the getVal method from Sub that
        takes an int
        '''
        from org.python.tests import Base, Sub
        b = Base()
        s = Sub()
        self.assertRaises(TypeError, b.getVal, 8)

    def test_dir_on_java_class(self):
        '''Checks that basic fields and methods are in the dir of a regular Java class'''
        from org.python.core import Py
        self.failUnless('None' in dir(Py))
        self.failUnless('newString' in dir(Py))

class ExtendJavaTest(unittest.TestCase):
    def test_override_tostring(self):
        from java.lang import Object, String
        class A(Object):
            def toString(self):
                return 'name'
        self.assertEquals('name', String.valueOf(A()))

class SysIntegrationTest(unittest.TestCase):
    def test_stdout_outputstream(self):
        out = FileOutputStream(test_support.TESTFN)
        oldstdout = sys.stdout
        sys.stdout = out
        print 'hello',
        out.close()
        f = open(test_support.TESTFN)
        self.assertEquals('hello', f.read())
        f.close()
        sys.stdout = out

def test_main():
    test_support.run_unittest(AbstractOnSyspathTest,
            InstantiationTest, 
            BeanTest, 
            MethodVisibilityTest, 
            ExtendJavaTest, 
            SysIntegrationTest)

if __name__ == "__main__":
    test_main()
