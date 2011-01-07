from __future__ import with_statement
import os
import re
import sys
import tempfile
import unittest
from test import test_support

class SysTest(unittest.TestCase):

    def test_platform(self):
        self.assertEquals(sys.platform[:4], "java",
                          "sys.platform is not java")

    def test_exit_arg(self):
        "sys.exit can be called with args"
        try:
            sys.exit("leaving now")
        except SystemExit, e:
            self.assertEquals(str(e), "leaving now")

    def test_tuple_args(self):
        "Exceptions raised unpacking tuple args have right line number"
        def tuple_args( (x,y) ): pass
        try:
            tuple_args( 10 )
        except TypeError:
            tb = sys.exc_info()[2]
            if tb.tb_lineno == 0:
                self.fail("Traceback lineno was zero")

    def test_name(self):
        "sys.__name__ can be reassigned/deleted"
        self.assertEquals(sys.__name__, 'sys')
        sys.__name__ = 'foo'
        self.assert_('foo' in str(sys))
        del sys.__name__
        self.assert_('foo' not in str(sys))
        sys.__name__ = 'sys'

    def test_readonly(self):
        def deleteClass(): del sys.__class__
        self.assertRaises(TypeError, deleteClass)

        def deleteDict(): del sys.__dict__
        self.assertRaises(TypeError, deleteDict)

        def assignClass(): sys.__class__ = object
        self.assertRaises(TypeError, assignClass)

        def assignDict(): sys.__dict__ = {}
        self.assertRaises(TypeError, assignDict)

    def test_resetmethod(self):
        gde = sys.getdefaultencoding
        sys.getdefaultencoding = 5
        self.assertEquals(sys.getdefaultencoding, 5)
        del sys.getdefaultencoding
        self.assertRaises(AttributeError, getattr, sys, 'getdefaultencoding')
        sys.getdefaultencoding = gde

    def test_reload(self):
        gde = sys.getdefaultencoding
        del sys.getdefaultencoding
        reload(sys)
        self.assert_(type(sys.getdefaultencoding) == type(gde))


def exec_code_separately(function, sharing=False):
    """Runs code in a separate context: (thread, PySystemState, PythonInterpreter)

    A PySystemState is used in conjunction with its thread
    context. This is not so desirable - at the very least it means
    that a thread pool cannot be shared. But this is not the place to
    revisit ancient design decisions."""

    def function_context():
        from org.python.core import Py
        from org.python.util import PythonInterpreter
        from org.python.core import PySystemState

        ps = PySystemState()
        pi = PythonInterpreter({}, ps)
        if not sharing:
            ps.shadow()
            ps.builtins = ps.builtins.copy()
        pi.exec(function.func_code)

    import threading
    context = threading.Thread(target=function_context)
    context.start()
    context.join()


def set_globally():
    import sys
    import test.sys_jy_test_module # used as a probe

    # can't use 'foo', test_with wants to have that undefined
    sys.builtins['test_sys_jy_foo'] = 42


def set_shadow():
    import sys
    sys.builtins['fum'] = 24

class ShadowingTest(unittest.TestCase):

    def setUp(self):
        exec_code_separately(set_globally, sharing=True)
        exec_code_separately(set_shadow)

    def test_super_globals(self):
        import sys, __builtin__

        def get_sym(sym):
            return sys.builtins.get(sym)
        def get_sym_attr(sym):
            return hasattr(__builtin__, sym)

        self.assertEqual(test_sys_jy_foo, 42, "should be able to install a new builtin ('super global')")
        self.assertEqual(get_sym('test_sys_jy_foo'), 42)
        self.assertTrue(get_sym_attr('test_sys_jy_foo'))

        def is_fum_there(): fum
        self.assertRaises(NameError, is_fum_there) # shadowed global ('fum') should not be visible
        self.assertEqual(get_sym('fum'), None)
        self.assertTrue(not(get_sym_attr('fum')))

    def test_sys_modules_per_instance(self):
        import sys
        self.assertTrue('sys_jy_test_module' not in sys.modules, "sys.modules should be per PySystemState instance")


class SyspathResourceTest(unittest.TestCase):
    def setUp(self):
        self.orig_path = sys.path
        sys.path.insert(0, test_support.findfile("bug1373.jar"))

    def tearDown(self):
        sys.path = self.orig_path

    def test_resource_stream_from_syspath(self):
        from pck import Main
        self.assert_(Main.getResourceAsStream('Main.txt'))

    def test_resource_url_from_syspath(self):
        from pck import Main
        self.assert_(Main.getResource('Main.txt'))


class SyspathUnicodeTest(unittest.TestCase):
    """bug 1693: importing from a unicode path threw a unicode encoding
    error"""

    def test_nonexisting_import_from_unicodepath(self):
        # \xf6 = german o umlaut
        sys.path.append(u'/home/tr\xf6\xf6t')
        self.assertRaises(ImportError, __import__, 'non_existing_module')

    def test_import_from_unicodepath(self):
        # \xf6 = german o umlaut
        moduleDir = tempfile.mkdtemp(suffix=u'tr\xf6\xf6t')
        try:
            self.assertTrue(os.path.exists(moduleDir))
            module = 'unicodetempmodule'
            moduleFile = '%s/%s.py' % (moduleDir, module)
            try:
                with open(moduleFile, 'w') as f:
                    f.write('# empty module')
                self.assertTrue(os.path.exists(moduleFile))
                sys.path.append(moduleDir)
                __import__(module)
                moduleClassFile = '%s/%s$py.class' % (moduleDir, module) 
                self.assertTrue(os.path.exists(moduleClassFile))
                os.remove(moduleClassFile)
            finally:
                os.remove(moduleFile)
        finally:
            os.rmdir(moduleDir)
        self.assertFalse(os.path.exists(moduleDir))        
        

def test_main():
    test_support.run_unittest(SysTest,
                              ShadowingTest,
                              SyspathResourceTest,
                              SyspathUnicodeTest)

if __name__ == "__main__":
    test_main()
