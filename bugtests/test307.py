"""
Test import of modules and packages from sys.path zipfile.
"""

import support
import zipfile, time
from java.io import File


def addZipEntry(zip, name, templateName):
    template = open('test307%s.template' % templateName)
    entry = zipfile.ZipInfo()
    entry.filename = name
    entry.date_time = time.gmtime(time.time())
    zip.writestr(entry, template.read())
    template.close()

zip = zipfile.ZipFile("test307.zip", "w")

addZipEntry(zip, 'test307m.py', 'm')
addZipEntry(zip, 'test307p/__init__.py', 'p')
addZipEntry(zip, "foo/bar/foobar.py", "foobar")

zip.close()

import sys
sys.path.append("test307.zip")

import test307m
assert test307m.__name__ == "test307m"
assert "test307.zip" in test307m.__file__
import test307p

sys.path.pop()
del test307p, test307m
del sys.modules['test307p'], sys.modules['test307m']

import java
java.lang.System.gc()
time.sleep(4)


