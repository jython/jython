"""
Tests Jython's capability to handle Python-functions and
methods that are so long that their JVM-bytecode exceeds
JVM method size restrictions.
The case that the main module code exceeds maximal length
is somewhat special, so it is explicitly tested.

Note: As of this writing, a CPython 2.7 bytecode-file (.pyc)
      is required for each module that contains an oversized
      function. The pyc-file is only required at compile-time
      in the sense that if you pre-compile py-files to classes,
      you won't need to distribute the pyc-file; it gets
      embedded into the class-file.
"""

import unittest
from test import test_support


class large_method_tests(unittest.TestCase):
    '''Tests some oversized functions and methods.
    '''

    @classmethod
    def setUpClass(cls):
        import large_methods as _large_methods
        global large_methods
        large_methods = _large_methods

    def test_large_func(self):
        '''Tests a function that slightly exceeds maximal JMV method
        length. It is internally represented as CPython bytecode.
        '''
        self.assertEqual(large_methods.large_function(), 'large 2300')

    def test_large_method(self):
        '''Tests a method that slightly exceeds maximal JMV method
        length. It is internally represented as CPython bytecode.
        '''
        cl = large_methods.OversizedMethodHolder()
        self.assertEqual(cl.large_function(), 'large_method 2300')

    def test_very_large_func(self):
        '''Here we test a function that is so large that its Python bytecode
        exceeds maximal String-literal length. It is automatically split up
        into several literals.
        '''
        self.assertEqual(large_methods.very_large_function(), 'very large 58900')

    def test_small_func(self):
        '''We assert that ordinary-sized, i.e. JVM-bytecode methods still work
        in context of PyBytecode.
        '''
        self.assertEqual(large_methods.small_function(), 'small 10')


class large_module_tests(unittest.TestCase):
    '''Tests a module with oversized main-code.
    So the whole module is represented as a single PyBytecode object.
    Additionally same tests as in large_method_tests are applied.
    '''

    @classmethod
    def setUpClass(cls):
        import large_module as _large_module
        global large_module
        large_module = _large_module

    def test_large_module_main(self):
        '''Tests the module's oversized main-code.
        '''
        self.assertEqual(large_module.count, 2310)
        self.assertEqual(large_module.msg, 'large_module 2310')

    def test_large_module_method(self):
        cl2 = large_module.OversizedMethodHolder()
        self.assertEqual(cl2.large_function(), 'large_method 2300')

    def test_large_module_large_func(self):
        self.assertEqual(large_module.large_function(), 'large 2300')

    def test_large_module_very_large_func(self):
        self.assertEqual(large_module.very_large_function(), 'very large 58900')

    def test_large_module_small_func(self):
        self.assertEqual(large_module.small_function(), 'small 10')


class large_method_back_compat_tests(unittest.TestCase):
    '''Tests backwards compatibility of the mechanism that handles oversized functions.
    '''

    @classmethod
    def setUpClass(cls):
        from org.python.core import imp as _imp
        from org.python.core import BytecodeLoader as _bcl
        global imp_class, bytecode_loader_class
        imp_class = _imp
        bytecode_loader_class = _bcl

    def test_backwards_compat(self):
        # To avoid unloading, we test everything in one go. This way, import failures are
        # tested before import succeeds for the first time. To verify, the tested resolution
        # is real, we assert import failures on large_methods_272$py.class and
        # large_module_272$py.class which were compiled by Jython 2.7.2.
        # We later turn on the mitigation to see that it restores compatibility.

        self.assertFalse(imp_class.getIgnoreAPIVersion())
        with self.assertRaises(ImportError) as cm_methods:
            import large_methods_272
        msg_start = 'compiled unit contains version 38 code (%s required): '
        msg_end = 'large_%s_272$py.class'
        api_v = imp_class.getAPIVersion()
        self.assertTrue(cm_methods.exception.message.startswith(msg_start % api_v))
        self.assertTrue(cm_methods.exception.message.endswith(msg_end % 'methods'))
        with self.assertRaises(ImportError) as cm_module:
            import large_module_272
        self.assertTrue(cm_module.exception.message.startswith(msg_start % api_v))
        self.assertTrue(cm_module.exception.message.endswith(msg_end % 'module'))

        # Now we bypass the API version check, but deliberately turn off the serialVersionUID
        # mismatch mitigation. This way we confirm, the issue is real and serialized objects
        # from older Jython might cause this import error.

        imp_class.setIgnoreAPIVersion(True)
        self.assertTrue(bytecode_loader_class.getIgnoreSerialVersionUID())
        bytecode_loader_class.setIgnoreSerialVersionUID(False)

        s = 'compiled unit contains incompatible serialized objects (for oversized function handling): '
        s2 = 'local class incompatible: stream classdesc serialVersionUID = '
        with self.assertRaises(ImportError) as cm2_methods:
            import large_methods_272
        m = str(cm2_methods.exception)
        self.assertEqual(m, cm2_methods.exception.message)
        self.assertTrue(m.startswith(s))
        self.assertTrue(s2 in m)

        with self.assertRaises(ImportError) as cm2_module:
            import large_module_272
        m = str(cm2_module.exception)
        self.assertEqual(m, cm2_module.exception.message)
        self.assertTrue(m.startswith(s))
        self.assertTrue(s2 in m)

        # Finally, we activate full mitigation of the issue and verify that everything passes now.

        bytecode_loader_class.setIgnoreSerialVersionUID(True)

        import large_methods_272
        self.assertEqual(large_methods_272.large_function(), 'large 2300')
        cl_272 = large_methods_272.OversizedMethodHolder()
        self.assertEqual(cl_272.large_function(), 'large_method 2300')
        self.assertEqual(large_methods_272.very_large_function(), 'very large 58900')
        self.assertEqual(large_methods_272.small_function(), 'small 10')

        import large_module_272
        self.assertEqual(large_module_272.count, 2310)
        self.assertEqual(large_module_272.msg, 'large_module 2310')
        cl2_272 = large_module_272.OversizedMethodHolder()
        self.assertEqual(cl2_272.large_function(), 'large_method 2300')
        self.assertEqual(large_module_272.large_function(), 'large 2300')
        self.assertEqual(large_module_272.very_large_function(), 'very large 58900')
        self.assertEqual(large_module_272.small_function(), 'small 10')

        # Restore default setting:
        imp_class.setIgnoreAPIVersion(False)
        self.assertFalse(imp_class.getIgnoreAPIVersion())


def test_main():
    test_support.run_unittest(
        large_method_tests,
        large_module_tests,
        large_method_back_compat_tests,
    )

if __name__ == "__main__":
    test_main()
