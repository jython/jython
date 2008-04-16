"""Misc. import tests

Made for Jython.
"""
import imp
import os
import shutil
import sys
import tempfile
import unittest
from test import test_support
from test_chdir import read, safe_mktemp, COMPILED_SUFFIX

class MislabeledImportTestCase(unittest.TestCase):

    def setUp(self):
        self.dir = tempfile.mkdtemp()
        self.orig_cwd = os.getcwd()
        os.chdir(self.dir)
        self.orig_syspath = sys.path
        sys.path.append('')

    def tearDown(self):
        shutil.rmtree(self.dir)
        os.chdir(self.orig_cwd)
        sys.path = self.orig_syspath

    def test_renamed_bytecode(self):
        source = safe_mktemp(dir=self.dir, suffix='.py')
        fp = open(source, 'w')
        fp.write("test = 'imported'")
        fp.close()

        module = os.path.basename(source)[:-3]
        compiled = module + COMPILED_SUFFIX
        # Compile to bytecode
        module_obj = __import__(module)
        self.assertEquals(module_obj.test, 'imported')
        self.assert_(os.path.exists(compiled))

        # Rename the bytecode
        compiled_moved = safe_mktemp(dir=self.dir, suffix=COMPILED_SUFFIX)
        os.rename(compiled, compiled_moved)

        # Ensure we can still import the renamed bytecode
        moved_module = os.path.basename(compiled_moved)[:-len(COMPILED_SUFFIX)]
        module_obj = __import__(moved_module)
        self.assertEquals(module_obj.__file__, os.path.basename(compiled_moved))
        self.assertEquals(module_obj.test, 'imported')

    def test_dunder_init(self):
        os.mkdir('foo')

        # typical import: foo.__init__$py.class is actually compiled
        # with a class name of foo
        init = os.path.join('foo', '__init__.py')
        fp = open(init, 'w')
        fp.write("bar = 'test'")
        fp.close()
        module_obj = __import__('foo')
        self.assertEquals(module_obj.__file__, init)
        self.assertEquals(module_obj.bar, 'test')

        init_compiled = init[:-3] + COMPILED_SUFFIX
        self.assert_(os.path.exists(init_compiled))
        bytecode = read(init_compiled)

        # trigger an abnormal import of foo.__init__; ask for it by the
        # mismatched __init__ name
        fp = open(os.path.join('foo', 'test.py'), 'w')
        fp.write("import __init__; baz = __init__.bar + 'test'; "
                 "init_file = __init__.__file__")
        fp.close()
        module_obj = __import__('foo.test')
        self.assertEquals(module_obj.test.baz, 'testtest')
        # XXX: Jython's import has a bug where it doesn't use the
        # $py.class filename when it exists along with the .py file
        if sys.platform.startswith('java'):
            self.assertEqual(module_obj.test.init_file,
                             os.path.join('foo', '__init__.py'))
        else:
            self.assertEqual(module_obj.test.init_file,
                             os.path.join('foo', '__init__' + COMPILED_SUFFIX))

        # Ensure a recompile of __init__$py.class wasn't triggered to
        # satisfy the abnormal import
        self.assertEquals(bytecode, read(init_compiled),
                          'bytecode was recompiled')

        # Ensure load_module can still load it as foo (doesn't
        # recompile)
        module_obj = imp.load_module('foo', *imp.find_module('foo'))
        self.assertEquals(module_obj.bar, 'test')

        # Again ensure we didn't recompile
        self.assertEquals(bytecode, read(init_compiled),
                          'bytecode was recompiled')


def test_main():
    test_support.run_unittest(MislabeledImportTestCase)

if __name__ == '__main__':
    test_main()
