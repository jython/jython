"""
[ #495458 ] multi level import from .zip file
"""

import support
import zipfile, time

def addZipEntry(zip, name, data):
    entry = zipfile.ZipInfo()
    entry.filename = name
    entry.date_time = time.gmtime(time.time())
    zip.writestr(entry, data)

zip = zipfile.ZipFile("test350.zip", "w")

addZipEntry(zip, "Lib/aaa/__init__.py", "")
addZipEntry(zip, "Lib/aaa/bbb/__init__.py", "")
addZipEntry(zip, "Lib/aaa/bbb/ccc/__init__.py", "")
addZipEntry(zip, "Lib/aaa/bbb/ccc/yyy.py", "")
addZipEntry(zip, "Lib/aaa/bbb/xxx.py", "")

zip.close()

import sys
sys.path.append("test350.zip/Lib")

import aaa
import aaa.bbb
import aaa.bbb.ccc
import aaa.bbb.ccc.yyy
import aaa.bbb.xxx

sys.path.pop()
