# -*- coding: utf-8 -*-

"""Misc os module tests

Made for Jython.
"""
import array
import glob
import os
import subprocess
import sys
import unittest
from test import test_support
from java.io import File


class OSFileTestCase(unittest.TestCase):

    def setUp(self):
        open(test_support.TESTFN, 'w').close()

    def tearDown(self):
        if os.path.exists(test_support.TESTFN):
            os.remove(test_support.TESTFN)

    def test_issue1727(self):
        os.stat(*(test_support.TESTFN,))

    def test_issue1755(self):
        os.remove(test_support.TESTFN)
        self.assertRaises(OSError, os.utime, test_support.TESTFN, None)

    @unittest.skipUnless(hasattr(os, 'link'), "os.link not available")
    def test_issue1824(self):
        os.remove(test_support.TESTFN)
        self.assertRaises(OSError, os.link,
                          test_support.TESTFN, test_support.TESTFN)

    def test_issue1825(self):
        os.remove(test_support.TESTFN)
        testfnu = unicode(test_support.TESTFN)
        try:
            os.open(testfnu, os.O_RDONLY)
        except OSError, e:
            self.assertTrue(isinstance(e.filename, unicode))
            self.assertEqual(e.filename, testfnu)
        else:
            self.assertTrue(False)

        # XXX: currently fail
        #for fn in os.chdir, os.listdir, os.rmdir:
        for fn in (os.rmdir,):
            try:
                fn(testfnu)
            except OSError, e:
                self.assertTrue(isinstance(e.filename, unicode))
                self.assertEqual(e.filename, testfnu)
            else:
                self.assertTrue(False)


class OSDirTestCase(unittest.TestCase):

    def setUp(self):
        self.base = test_support.TESTFN
        self.path = os.path.join(self.base, 'dir1', 'dir2', 'dir3')
        os.makedirs(self.path)

    def test_rmdir(self):
        # Remove end directory
        os.rmdir(self.path)
        # Fail to remove a chain of directories
        self.assertRaises(OSError, os.rmdir, self.base)

    def test_issue2083(self):
        # Should fail to remove/unlink directory
        self.assertRaises(OSError, os.remove, self.path)
        self.assertRaises(OSError, os.unlink, self.path)

    def tearDown(self):
        # Some dirs may have been deleted. Find the longest that exists.
        p = self.path
        while not os.path.exists(p) and p != self.base:
            p = os.path.dirname(p)
        os.removedirs(p)


class OSStatTestCase(unittest.TestCase):

    def setUp(self):
        open(test_support.TESTFN, 'w').close()

    def tearDown(self):
        if os.path.exists(test_support.TESTFN):
            os.remove(test_support.TESTFN)

    def test_stat_with_trailing_slash(self):
        self.assertRaises(OSError, os.stat, test_support.TESTFN + os.path.sep)
        self.assertRaises(OSError, os.lstat, test_support.TESTFN + os.path.sep)


class OSWriteTestCase(unittest.TestCase):

    def setUp(self):
        self.fd = os.open(test_support.TESTFN, os.O_WRONLY | os.O_CREAT)

    def tearDown(self):
        if self.fd :
            os.close(self.fd)
            if os.path.exists(test_support.TESTFN):
                os.remove(test_support.TESTFN)

    def do_write(self, b, nx=None):
        if nx is None : nx = len(b)
        n = os.write(self.fd, b)
        self.assertEqual(n, nx, "os.write length error: " + repr(b))

    def test_write_buffer(self): # Issue 2062
        s = b"Big Red Book"
        for type2test in (str, buffer, bytearray, (lambda x : array.array('b',x))) :
            self.do_write(type2test(s))

        with memoryview(s) as m :
            self.do_write(m)
            # not contiguous:
            self.assertRaises(BufferError, self.do_write, m[1::2])

        # lacks buffer api:
        self.assertRaises(TypeError, self.do_write, 1.5, 4)

class OSUnicodeTestCase(unittest.TestCase):

    def test_env(self):
        with test_support.temp_cwd(name=u"tempcwd-中文"):
            newenv = os.environ.copy()
            newenv["TEST_HOME"] = u"首页"
            p = subprocess.Popen([sys.executable, "-c",
                                  'import sys,os;' \
                                  'sys.stdout.write(os.getenv("TEST_HOME").encode("utf-8"))'],
                                 stdout=subprocess.PIPE,
                                 env=newenv)
            self.assertEqual(p.stdout.read().decode("utf-8"), u"首页")
    
    def test_getcwd(self):
        with test_support.temp_cwd(name=u"tempcwd-中文") as temp_cwd:
            p = subprocess.Popen([sys.executable, "-c",
                                  'import sys,os;' \
                                  'sys.stdout.write(os.getcwd().encode("utf-8"))'],
                                 stdout=subprocess.PIPE)
            self.assertEqual(p.stdout.read().decode("utf-8"), temp_cwd)

    def test_listdir(self):
        # It is hard to avoid Unicode paths on systems like OS X. Use
        # relative paths from a temp CWD to work around this
        with test_support.temp_cwd() as new_cwd:
            unicode_path = os.path.join(".", "unicode")
            self.assertIs(type(unicode_path), str)
            chinese_path = os.path.join(unicode_path, u"中文")
            self.assertIs(type(chinese_path), unicode)
            home_path = os.path.join(chinese_path, u"首页")
            os.makedirs(home_path)
            
            with open(os.path.join(home_path, "test.txt"), "w") as test_file:
                test_file.write("42\n")

            # Verify works with str paths, returning Unicode as necessary
            entries = os.listdir(unicode_path)
            self.assertIn(u"中文", entries)

            # Verify works with Unicode paths
            entries = os.listdir(chinese_path)
            self.assertIn(u"首页", entries)
           
            # glob.glob builds on os.listdir; note that we don't use
            # Unicode paths in the arg to glob
            self.assertEqual(glob.glob("unicode/*"), [u"unicode/中文"])
            self.assertEqual(glob.glob("unicode/*/*"), [u"unicode/中文/首页"])
            self.assertEqual(glob.glob("unicode/*/*/*"), [u"unicode/中文/首页/test.txt"])

            # Now use a Unicode path as well as the glob arg
            self.assertEqual(glob.glob(u"unicode/*"), [u"unicode/中文"])
            self.assertEqual(glob.glob(u"unicode/*/*"), [u"unicode/中文/首页"])
            self.assertEqual(glob.glob(u"unicode/*/*/*"), [u"unicode/中文/首页/test.txt"])
 
            # Verify Java integration. But we will need to construct
            # an absolute path since chdir doesn't work with Java
            # (except for subprocesses, like below in test_env)
            for entry in entries:
                entry_path = os.path.join(new_cwd, chinese_path, entry)
                f = File(entry_path)
                self.assertTrue(f.exists(), "File %r (%r) should be testable for existence" % (
                    f, entry_path))



def test_main():
    test_support.run_unittest(
        OSFileTestCase, 
        OSDirTestCase,
        OSStatTestCase,
        OSWriteTestCase,
        OSUnicodeTestCase
    )

if __name__ == '__main__':
    test_main()
