"""
Test import of modules and packages from sys.path zipfile.
"""

import support
import zipfile, time


def addZipEntry(zip, name, data):
    entry = zipfile.ZipInfo()
    entry.filename = name
    entry.date_time = time.gmtime(time.time())
    zip.writestr(entry, data)


zip = zipfile.ZipFile("test307.zip", "w")

addZipEntry(zip, "test307m.py", """
def compare(str, v1, v2):
    assert v1 == v2, "%s '%s' '%s'" % (str, v1, v2)
compare("__name__", __name__, "test307m")
compare("__file__", __file__, "test307m.py")
""")

addZipEntry(zip, "mod.py", """
def compare(str, v1, v2):
    assert v1 == v2, "%s '%s' '%s'" % (str, v1, v2)
compare("__name__", __name__, "mod")
compare("__file__", __file__, "mod.py")
""")

addZipEntry(zip, "test307p/__init__.py", """
def compare(str, v1, v2):
    assert v1 == v2, "%s '%s' '%s'" % (str, v1, v2)
compare("__name__", __name__, "test307p")
compare("__file__", __file__, "test307p/__init__.py")
compare("__path__", __path__, ["test307.zip!test307p"])
import mod
import submod
__path__.append("test307.zip!foo/bar")
import foobar
""")

addZipEntry(zip, "test307p/submod.py", """
def compare(str, v1, v2):
    assert v1 == v2, "%s '%s' '%s'" % (str, v1, v2)
compare("__name__", __name__, "test307p.submod")
compare("__file__", __file__, "test307p/submod.py")
#print "__path__", repr(__path__)
""")

addZipEntry(zip, "foo/bar/foobar.py", """
def compare(str, v1, v2):
    assert v1 == v2, "%s '%s' '%s'" % (str, v1, v2)
compare("__name__", __name__, "test307p.foobar")
compare("__file__", __file__, "foo/bar/foobar.py")
""")

zip.close()

import sys
sys.path.append("test307.zip")

import test307m
assert test307m.__name__ == "test307m"
assert test307m.__file__ == "test307m.py"

import test307p
assert test307p.__name__ == "test307p"
assert test307p.__file__ == "test307p/__init__.py"

sys.path.pop()
del test307p, test307m
del sys.modules['test307p'], sys.modules['test307m']

import java
java.lang.System.gc()
time.sleep(4)


