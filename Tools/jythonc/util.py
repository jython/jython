# Copyright © Corporation for National Research Initiatives

def lookup(name):
    names = name.split('.')
    top = __import__(names[0])
    for name in names[1:]:
        top = getattr(top, name)
    return top


from java.util.zip import ZipFile

zipfiles = {}

def getzip(filename):
    if zipfiles.has_key(filename):
        return zipfiles[filename]
    zipfile = ZipFile(filename)
    zipfiles[filename] = zipfile
    return zipfile


def closezips():
    for zf in zipfiles.values():
        zf.close()
    zipfiles.clear()

# "tentative"

import sys
import string

from org.python.core import imp,Py

from yapm import YaPM

def findClass(c):
    return Py.findClassEx(c)

def reportPublicPlainClasses(jpkg):
    classes = sys.packageManager.doDir(jpkg,0,1)
    classes.remove('__name__')
    return string.join(classes,',')

def openResource(res):
    return imp.getSyspathJavaLoader().getResourceAsStream(res)

_ypm = None

def listAllClasses(jpkg):
    global _ypm
    classes = sys.packageManager.doDir(jpkg,0,1)
    classes.remove('__name__')
    if _ypm is None:
        _ypm = YaPM(sys.registry)
    pkg2 = _ypm.lookupName(jpkg.__name__)
    classes2 = _ypm.doDir(pkg2,0,1)
    classes2.remove('__name__')
    classes.extend(classes2)
    return classes
