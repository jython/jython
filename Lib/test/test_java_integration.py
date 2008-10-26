import os
import unittest
import sys
import re

from test import test_support
from java.awt import (Dimension, Component, Rectangle, Button, Color,
                      HeadlessException)
from java.util import Vector, Hashtable
from java.io import FileOutputStream, FileWriter, OutputStreamWriter
                     
from java.lang import Runnable, Thread, ThreadGroup, System, Runtime, Math, Byte
from javax.swing.tree import TreePath
from java.math import BigDecimal

"""
public abstract class Abstract {
    public Abstract() {
        method();
    }

    public abstract void method();
}
"""
# The following is the correspoding bytecode for Abstract compiled with javac 1.5
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
                
class AutoSuperTest(unittest.TestCase):
        
    def test_auto_super(self):
        class R(Rectangle):
            def __init__(self):
                self.size = Dimension(6, 7)
        self.assert_("width=6,height=7" in  R().toString())

    def test_no_default_constructor(self):
        "Check autocreation when java superclass misses a default constructor."
        class A(ThreadGroup):
            def __init__(self):
                print self.name
        self.assertRaises(TypeError, A)
        
    def test_no_public_constructors(self):
        self.assertRaises(TypeError, Math)

class PyObjectCmpTest(unittest.TestCase):

    def test_vect_cmp(self):
        "Check comparing a PyJavaClass with a Object."
        class X(Runnable):
            pass
        v = Vector()
        v.addElement(1)
        v.indexOf(X())

class IOTest(unittest.TestCase):

    def test_io_errors(self):
        "Check that IOException isn't mangled into an IOError"
        from java.io import UnsupportedEncodingException
        self.assertRaises(UnsupportedEncodingException, OutputStreamWriter, 
                System.out, "garbage")
    
    def test_fileio_error(self):
        from java.io import FileInputStream, FileNotFoundException
        self.assertRaises(FileNotFoundException, FileInputStream, "garbage")

    def test_unsupported(self):
        from org.python.core.util import FileUtil
        fp = FileUtil.wrap(System.out)
        self.assertRaises(IOError, fp.tell)

class VectorTest(unittest.TestCase):

    def test_looping(self):
        for i in Vector(): pass

    def test_return_proxy(self):
        "Jython proxies properly return back from Java code"
        class FooVector(Vector):
            bar = 99

        ht = Hashtable()
        fv = FooVector()
        ht.put("a", fv)
        self.failUnless(fv is ht.get("a"))

class JavaReservedNamesTest(unittest.TestCase):
    "Access to reserved words"

    def test_system_in(self):
        s = System.in
        self.assert_("java.io.BufferedInputStream" in str(s))
             
    def test_runtime_exec(self):
        e = Runtime.getRuntime().exec
        self.assert_(re.search("method .*exec", str(e)) is not None)
                       
    def test_byte_class(self):
        b = Byte(10)
        self.assert_("java.lang.Byte" in str(b.class))

class Keywords(object):
    pass

Keywords.in = lambda self: "in"
Keywords.exec = lambda self: "exec"
Keywords.class = lambda self: "class"
Keywords.print = lambda self: "print"
Keywords.and = lambda self: "and"
Keywords.as = lambda self: "as"
Keywords.assert = lambda self: "assert"
Keywords.break = lambda self: "break"
Keywords.continue = lambda self: "continue"
Keywords.def = lambda self: "def"
Keywords.del = lambda self: "del"
Keywords.elif = lambda self: "elif"
Keywords.else = lambda self: "else"
Keywords.except = lambda self: "except"
Keywords.finally = lambda self: "finally"
Keywords.from = lambda self: "from"
Keywords.for = lambda self: "for"
Keywords.global = lambda self: "global"
Keywords.if = lambda self: "if"
Keywords.import = lambda self: "import"
Keywords.is = lambda self: "is"
Keywords.lambda = lambda self: "lambda"
Keywords.pass = lambda self: "pass"
Keywords.print = lambda self: "print"
Keywords.raise = lambda self: "raise"
Keywords.return = lambda self: "return"
Keywords.try = lambda self: "try"
Keywords.while = lambda self: "while"
Keywords.with = lambda self: "with"
Keywords.yield = lambda self: "yield"

class PyReservedNamesTest(unittest.TestCase):
    "Access to reserved words"

    def setUp(self):
        self.kws = Keywords()

    def test_in(self):
        self.assertEquals(self.kws.in(), "in")
    
    def test_exec(self):
        self.assertEquals(self.kws.exec(), "exec")

    def test_class(self):
        self.assertEquals(self.kws.class(), "class")

    def test_print(self):
        self.assertEquals(self.kws.print(), "print")

    def test_and(self):
        self.assertEquals(self.kws.and(), "and")

    def test_as(self):
        self.assertEquals(self.kws.as(), "as")

    def test_assert(self):
        self.assertEquals(self.kws.assert(), "assert")

    def test_break(self):
        self.assertEquals(self.kws.break(), "break")

    def test_continue(self):
        self.assertEquals(self.kws.continue(), "continue")

    def test_def(self):
        self.assertEquals(self.kws.def(), "def")

    def test_del(self):
        self.assertEquals(self.kws.del(), "del")

    def test_elif(self):
        self.assertEquals(self.kws.elif(), "elif")

    def test_else(self):
        self.assertEquals(self.kws.else(), "else")

    def test_except(self):
        self.assertEquals(self.kws.except(), "except")

    def test_finally(self):
        self.assertEquals(self.kws.finally(), "finally")

    def test_from(self):
        self.assertEquals(self.kws.from(), "from")

    def test_for(self):
        self.assertEquals(self.kws.for(), "for")

    def test_global(self):
        self.assertEquals(self.kws.global(), "global")

    def test_if(self):
        self.assertEquals(self.kws.if(), "if")

    def test_import(self):
        self.assertEquals(self.kws.import(), "import")

    def test_is(self):
        self.assertEquals(self.kws.is(), "is")

    def test_lambda(self):
        self.assertEquals(self.kws.lambda(), "lambda")

    def test_pass(self):
        self.assertEquals(self.kws.pass(), "pass")

    def test_print(self):
        self.assertEquals(self.kws.print(), "print")

    def test_raise(self):
        self.assertEquals(self.kws.raise(), "raise")

    def test_return(self):
        self.assertEquals(self.kws.return(), "return")

    def test_try(self):
        self.assertEquals(self.kws.try(), "try")

    def test_while(self):
        self.assertEquals(self.kws.while(), "while")

    def test_with(self):
        self.assertEquals(self.kws.with(), "with")

    def test_yield(self):
        self.assertEquals(self.kws.yield(), "yield")

class ImportTest(unittest.TestCase):
    
    def test_bad_input_exception(self):
        self.assertRaises(ValueError, __import__, '')

class ButtonTest(unittest.TestCase):

    def test_setLabel(self):
        try:
            b = Button()
        except HeadlessException:
            return # can't raise TestSkipped
        try:
            b.setLabel = 4
        except TypeError, e:
            self.failUnless("can't assign to this attribute in java instance: setLabel" in str(e))

class ColorTest(unittest.TestCase):

    def test_static_fields(self):
        Color.red
        Color.blue

    def test_is_operator(self):
        red = Color.red
        self.assert_(red is red)
        self.assert_(red is Color.red)

class TreePathTest(unittest.TestCase):
    
    def test_overloading(self):
        treePath = TreePath([1,2,3])
        self.assertEquals(len(treePath.path), 3, "Object[] not passed correctly")
        self.assertEquals(TreePath(treePath.path).path, treePath.path, "Object[] not passed and returned correctly")
            
class BigDecimalTest(unittest.TestCase):
    
    def test_coerced_bigdecimal(self):
        from javatests import BigDecimalTest
        
        x = BigDecimal("123.4321")
        y = BigDecimalTest().asBigDecimal()

        self.assertEqual(type(x), type(y), "BigDecimal coerced")
        self.assertEqual(x, y, "BigDecimal coerced")

class MethodInvTest(unittest.TestCase):
    
    def test_method_invokation(self):
        from javatests import MethodInvokationTest
        
        bar = MethodInvokationTest.foo1(Byte(10))

        self.assertEquals(bar, "foo1 with byte arg: 10", "Wrong method called")

class InterfaceTest(unittest.TestCase):
    
    def test_override(self):
        from java.lang import String
        class Foo(Runnable):
            def run(self): pass
            def toString(self): return "Foo!!!"
            
        foo = Foo()
        s = String.valueOf(foo)

        self.assertEquals(s, "Foo!!!", "toString not overridden in interface")

class JavaStringTest(unittest.TestCase):
    
    def test_string_not_iterable(self):
        from java import lang
        x = lang.String('test')
        self.assertRaises(TypeError, list, x)

def test_main():
    test_support.run_unittest(AbstractOnSyspathTest,
                              InstantiationTest, 
                              BeanTest, 
                              ExtendJavaTest, 
                              SysIntegrationTest,
                              AutoSuperTest,
                              PyObjectCmpTest,
                              IOTest,
                              VectorTest,
                              JavaReservedNamesTest,
                              PyReservedNamesTest,
                              ImportTest,
                              ButtonTest,
                              ColorTest,
                              TreePathTest,
                              BigDecimalTest,
                              MethodInvTest,
                              InterfaceTest,
                              JavaStringTest,
                              )

if __name__ == "__main__":
    test_main()
