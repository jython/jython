"""
Test import of java class from sys.path zipfile.
"""

import support
import zipfile, time

#support.compileJava("test323d/test323j.java")

def addZipEntry(zip, name, data):
    entry = zipfile.ZipInfo()
    entry.filename = name
    entry.date_time = time.gmtime(time.time())
    zip.writestr(entry, data)


zip = zipfile.ZipFile("test323.zip", "w", zipfile.ZIP_DEFLATED)

addZipEntry(zip, "Lib/test323m.py", """
def compare(str, v1, v2):
    #print str, v1, v2
    assert v1 == v2, "%s '%s' '%s'" % (str, v1, v2)
compare("__name__", __name__, "test323m")
compare("__file__", __file__, "Lib/test323m.py")
""")

#zip.write("test323d/test323j.class", "test323j.class")

zip.close()

import sys
sys.path.append("test323.zip/Lib")

import test323m

sys.path.pop()
del test323m
del sys.modules['test323m']

import java
java.lang.System.gc()
time.sleep(4)

#raise support.TestWarning('A test of TestWarning. It is not an error')

