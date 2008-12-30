import unittest
import sys
from test import test_support
from java.io import File
from java.lang import Thread
from java.net import URLClassLoader

class ClasspathImporterTestCase(unittest.TestCase):
    def setUp(self):
        self.orig_context = Thread.currentThread().contextClassLoader
        self.orig_path = sys.path

    def tearDown(self):
        Thread.currentThread().contextClassLoader = self.orig_context
        sys.path = self.orig_path
        try:
            del sys.modules['flat_in_jar']
            del sys.modules['jar_pkg']
            del sys.modules['jar_pkg.prefer_compiled']
        except KeyError:
            pass

    def setClassLoaderAndCheck(self, jar):
        url = File(test_support.findfile(jar)).toURL()
        Thread.currentThread().contextClassLoader = URLClassLoader([url])
        import flat_in_jar
        self.assertEquals(flat_in_jar.value, 7)
        import jar_pkg
        from jar_pkg import prefer_compiled
        self.assert_(prefer_compiled.compiled)
        self.assertRaises(NameError, __import__, 'flat_bad')
        self.assertRaises(NameError, __import__, 'jar_pkg.bad')

    def test_default_pyclasspath(self):
        self.setClassLoaderAndCheck("classimport.jar")

    def test_path_in_pyclasspath(self):
        sys.path = ['__pyclasspath__/Lib']
        self.setClassLoaderAndCheck("classimport_Lib.jar")

def test_main():
    test_support.run_unittest(ClasspathImporterTestCase)

if __name__ == '__main__':
    test_main()
