"""
Test import of java class from sys.path zipfile.
"""

import support
import zipfile, time

support.compileJava("test308d/test308j.java")

def addZipEntry(zip, name, data):
    entry = zipfile.ZipInfo()
    entry.filename = name
    entry.date_time = time.gmtime(time.time())
    zip.writestr(entry, data)


zip = zipfile.ZipFile("test308.zip", "w", zipfile.ZIP_DEFLATED)

addZipEntry(zip, "test308m.py", """
def compare(str, v1, v2):
    assert v1 == v2, "%s '%s' '%s'" % (str, v1, v2)
compare("__name__", __name__, "test308m")
compare("__file__", __file__, "test308m.py")
import test308j
assert test308j().foo() == "bar"
""")

zip.write("test308d/test308j.class", "test308j.class")

zip.close()

import sys
sys.path.append("test308.zip")

import test308m

sys.path.pop()
del test308m
del sys.modules['test308m'], sys.modules['test308j']

import java
java.lang.System.gc()
time.sleep(4)

#raise support.TestWarning('A test of TestWarning. It is not an error')

