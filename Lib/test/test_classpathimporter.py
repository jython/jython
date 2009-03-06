import unittest
import sys
from test import test_support
from java.lang import Thread

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

    def setClassLoaderAndCheck(self, jar, prefix):
        Thread.currentThread().contextClassLoader = test_support.make_jar_classloader(jar)
        import flat_in_jar
        self.assertEquals(flat_in_jar.value, 7)
        import jar_pkg
        self.assertEquals(prefix + '/jar_pkg/__init__$py.class', jar_pkg.__file__)
        from jar_pkg import prefer_compiled
        self.assertEquals(prefix + '/jar_pkg/prefer_compiled$py.class', prefer_compiled.__file__)
        self.assert_(prefer_compiled.compiled)
        self.assertRaises(NameError, __import__, 'flat_bad')
        self.assertRaises(NameError, __import__, 'jar_pkg.bad')

    def test_default_pyclasspath(self):
        self.setClassLoaderAndCheck("classimport.jar", "__pyclasspath__")

    def test_path_in_pyclasspath(self):
        sys.path = ['__pyclasspath__/Lib']
        self.setClassLoaderAndCheck("classimport_Lib.jar", "__pyclasspath__/Lib")

    # I don't like the checked in jar file bug1239.jar.  The *one* thing I
    # liked about the tests in bugtests/ is that you could create a java file,
    # compile it, jar it and destroy the jar when done.  Maybe when we move to
    # JDK 6 and can use JSR-199 to do runtime compiling, we can go back to
    # that.  Anyway, see http://bugs.jython.org/issue1239. In short, jars added
    # with sys.path.append where not getting scanned if they start with a top
    # level package we already have, like the "org" in org.python.*
    def test_bug1239(self):
        sys.path.append("Lib/test/bug1239.jar")
        import org.test403javapackage.test403

def test_main():
    test_support.run_unittest(ClasspathImporterTestCase)

if __name__ == '__main__':
    test_main()
