"""
Tests using a path inside a zip file for zip imports
"""

import support
import zipfile, time

def addZipEntry(zip, name, data):
    entry = zipfile.ZipInfo()
    entry.filename = name
    entry.date_time = time.gmtime(time.time())
    zip.writestr(entry, data)


zip = zipfile.ZipFile("test323.zip", "w", zipfile.ZIP_DEFLATED)

addZipEntry(zip, "Lib/test323m.py", """
assert __name__ == 'test323m', " __name__ should've been test323m but was %s" % __name__
from java.io import File
expected = "test323.zip%sLib/test323m.py" % (File.separator)
assert expected in __file__, "%s should've been in __file__ but was %s" % (expected, __file__)
""")

zip.close()

import sys
sys.path.append("test323.zip/Lib")

import test323m
