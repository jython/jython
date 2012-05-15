# Test packages (dotted-name import)

# XXX: This test is borrowed from CPython 2.7 as it tickles
# http://bugs.jython.org/issue1871 so it should be removed in Jython 2.7
import sys
import os
import tempfile
import textwrap
import unittest
from test import test_support


# Helpers to create and destroy hierarchies.

def cleanout(root):
    names = os.listdir(root)
    for name in names:
        fullname = os.path.join(root, name)
        if os.path.isdir(fullname) and not os.path.islink(fullname):
            cleanout(fullname)
        else:
            os.remove(fullname)
    os.rmdir(root)

def fixdir(lst):
    if "__builtins__" in lst:
        lst.remove("__builtins__")
    return lst


class Test(unittest.TestCase):

    def setUp(self):
        self.root = None
        self.pkgname = None
        self.syspath = list(sys.path)

    def tearDown(self):
        sys.path[:] = self.syspath
        if self.root: # Only clean if the test was actually run
            cleanout(self.root)

        # delete all modules concerning the tested hierarchy
        if self.pkgname:
            modules = [name for name in sys.modules
                       if self.pkgname in name.split('.')]
            for name in modules:
                del sys.modules[name]

    def run_code(self, code):
        exec(textwrap.dedent(code), globals(), {"self": self})

    def mkhier(self, descr):
        root = tempfile.mkdtemp()
        sys.path.insert(0, root)
        if not os.path.isdir(root):
            os.mkdir(root)
        for name, contents in descr:
            comps = name.split()
            fullname = root
            for c in comps:
                fullname = os.path.join(fullname, c)
            if contents is None:
                os.mkdir(fullname)
            else:
                f = open(fullname, "w")
                f.write(contents)
                if contents and contents[-1] != '\n':
                    f.write('\n')
                f.close()
        self.root = root
        # package name is the name of the first item
        self.pkgname = descr[0][0]

    def test_5(self):
        hier = [
        ("t5", None),
        ("t5 __init__"+os.extsep+"py", "import t5.foo"),
        ("t5 string"+os.extsep+"py", "spam = 1"),
        ("t5 foo"+os.extsep+"py",
         "from . import string; assert string.spam == 1"),
         ]
        self.mkhier(hier)

        import t5
        s = """
            from t5 import *
            self.assertEqual(dir(), ['foo', 'self', 'string', 't5'])
            """
        self.run_code(s)

        import t5
        self.assertEqual(fixdir(dir(t5)),
                         ['__doc__', '__file__', '__name__',
                          '__path__', 'foo', 'string', 't5'])
        self.assertEqual(fixdir(dir(t5.foo)),
                         ['__doc__', '__file__', '__name__',
                          'string'])
        self.assertEqual(fixdir(dir(t5.string)),
                         ['__doc__', '__file__', '__name__',
                          'spam'])

if __name__ == "__main__":
    unittest.main()
