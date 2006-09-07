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
import test308j
assert test308j().foo() == "bar"
""")

zip.write("test308d/test308j.class", "test308j.class")

zip.close()

import sys
sys.path.append("test308.zip")

import test308m

