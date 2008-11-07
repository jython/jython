# -*- coding: utf-8 -*-
import sys
import unittest
import test.test_support
from codecs import BOM_UTF8

class BuiltinTest(unittest.TestCase):
        
    def test_in_sys_modules(self):
        self.assert_("__builtin__" in sys.modules,
            "__builtin__ not found in sys.modules")

    def test_hasattr_swallows_exceptions(self):
        class Foo(object):
            def __getattr__(self, name):
                raise TypeError()
        self.assert_(not hasattr(Foo(), 'bar'))

class LoopTest(unittest.TestCase):

    def test_break(self):
        while 1:
            i = 0
            while i<10:
                i = i+1
            else:
                break

class DebugTest(unittest.TestCase):

    def test_debug(self):
        "__debug__ exists"
        try:
            foo = __debug__
        except NameError, e:
            self.assert_(False)

class GetSliceTest(unittest.TestCase):

    def test_getslice(self):
        class F:
            def __getitem__(self,*args): return '__getitem__ '+`args`
            def __getslice__(self,*args): return '__getslice__ '+`args`
        self.failUnless("__getslice__ (1, 1)" in F()[1:1])

class ChrTest(unittest.TestCase):

    def test_debug(self):
        "chr(None) throws TypeError"
        foo = False
        try:
            chr(None)
        except TypeError, e:
            foo = True
        self.assert_(foo)

class ReturnTest(unittest.TestCase):

    def test_finally(self):
        '''return in finally causes java.lang.VerifyError at compile time'''
        def timeit(f):
            t0 = time.clock()
            try:
                f()
            finally:
                t1 = time.clock()
                return t1 - t0

class ReprTest(unittest.TestCase):
    def test_unbound(self):
        "Unbound methods indicated properly in repr"
        class Foo:
            def bar(s): 
                pass
        self.failUnless(repr(Foo.bar).startswith('<unbound method'))

class CallableTest(unittest.TestCase):

    def test_callable_oldstyle(self):
        class Foo:
            pass
        self.assert_(callable(Foo))
        self.assert_(not callable(Foo()))
        class Bar:
            def __call__(self):
                return None
        self.assert_(callable(Bar()))
        class Baz:
            def __getattr__(self, name):
                return None
        self.assert_(callable(Baz()))

    def test_callable_newstyle(self):
        class Foo(object):
            pass
        self.assert_(callable(Foo))
        self.assert_(not callable(Foo()))
        class Bar(object):
            def __call__(self):
                return None
        self.assert_(callable(Bar()))
        class Baz(object):
            def __getattr__(self, name):
                return None
        self.assert_(not callable(Baz()))

class ConversionTest(unittest.TestCase):

    class Foo(object):
        def __int__(self):
            return 3
        def __float__(self):
            return 3.14
    foo = Foo()

    def test_range_non_int(self):
        self.assertEqual(range(self.foo), [0, 1, 2])

    def test_xrange_non_int(self):
        self.assertEqual(list(xrange(self.foo)), [0, 1, 2])

    def test_round_non_float(self):
        self.assertEqual(round(self.Foo(), 1), 3.1)

class ExecEvalTest(unittest.TestCase):

    def test_eval_bom(self):
        self.assertEqual(eval(BOM_UTF8 + '"foo"'), 'foo')
        # Actual BOM ignored, so causes a SyntaxError
        self.assertRaises(SyntaxError, eval,
                          BOM_UTF8.decode('iso-8859-1') + '"foo"')

    def test_parse_str_eval(self):
        foo = 'föö'
        for code in ("'%s'" % foo.decode('utf-8'),
                     "# coding: utf-8\n'%s'" % foo,
                     "%s'%s'" % (BOM_UTF8, foo)):
            mod = compile(code, 'foo.py', 'eval')
            bar = eval(mod)
            self.assertEqual(foo, bar)
            bar = eval(code)
            self.assertEqual(foo, bar)

    def test_parse_str_exec(self):
        foo = 'föö'
        for code in ("a = '%s'" % foo.decode('utf-8'),
                     "# coding: utf-8\na = '%s'" % foo,
                     "%sa = '%s'" % (BOM_UTF8, foo)):
            ns = {}
            exec code in ns
            self.assertEqual(foo, ns['a'])

def test_main():
    test.test_support.run_unittest(BuiltinTest,
                                   LoopTest,
                                   DebugTest,
                                   GetSliceTest,
                                   ChrTest,
                                   ReturnTest,
                                   ReprTest,
                                   CallableTest,
                                   ConversionTest,
                                   ExecEvalTest)

if __name__ == "__main__":
    test_main()
