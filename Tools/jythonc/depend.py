# Copyright © Corporation for National Research Initiatives

import sys
import os
from types import TypeType

from java import io
from java.util.zip import ZipFile



def fix(n):
    DOT = '.'
    return DOT.join(n.split('/'))

def unfix(n):
    SLASH = '/'
    return SLASH.join(n.split('.'))


def getClass(t, names):
    #print t, names
    if t[0] == '(':
        index = 1
        while index < len(t) and t[index] != ')':
            off, value = getClass(t[index:], names)
            index = index+off
        getClass(t[index+1:], names)
    elif t[0] == 'L':
        end = t.index(';')
        value = t[1:end]
        names[fix(value)] = 1
        return end, t[1:end]
    elif t[0] == '[':
        off, ret = getClass(t[1:], names)
        return off+1, ret
    else:
        return 1, None



def dependencies(file):
    data = io.DataInputStream(file)

    data.readInt()
    data.readShort()
    data.readShort()

    n = data.readShort()

    strings = [None]*n
    classes = []
    types = []

    i = 0
    while i < n-1:
        tag = data.readByte()
        if tag == 1:
            name = data.readUTF()
            strings[i] = name
        elif tag == 7:
            classes.append(data.readShort())
        elif tag == 9 or tag == 10 or tag == 11:
            data.readShort()
            data.readShort()
        elif tag == 8:
            data.readShort()
        elif tag == 3:
            data.readInt()
        elif tag == 4:
            data.readFloat()
        elif tag == 5:
            data.readLong()
            i = i+1
        elif tag == 6:
            data.readDouble()
            i = i+1
        elif tag == 12:
            data.readShort()
            types.append(data.readShort())
        i = i+1
    data.readShort() # access_flags
    data.readShort() # this_class
    data.readShort() # super_class

    interfaces_count = data.readShort()
    for i in range(interfaces_count):
        data.readShort()

    fields_count = data.readShort()
    for i in range(fields_count):
        data.readShort() # access_flags
    	data.readShort() # name_index
        types.append(data.readShort())
        attributes_count = data.readShort()
        for j in range(attributes_count):
            data.readShort() # attribute_name_index 
            attribute_length = data.readInt()
            data.skip(attribute_length)

    names = {}
    for c in classes:
        #print c, strings[c-1]
        name = fix(strings[c-1])
        if name[0] == '[':
            continue
        names[name]= 1

    for t in types:
        getClass(strings[t-1], names)

    return filter(defaultFilter, names.keys())



def defaultFilter(name):
    return not (name[:5] == 'java.' or name[:16] == 'org.python.core.')



from org.python.core import PyJavaPackage
from util import lookup, getzip
from util import openResource
def getFile(name):
    dot = name.rfind('.')
    if dot == -1:
        return PkgEntry(name)
##        return topFile(name)

    package = lookup(name[:dot])
    if isinstance(package, PyJavaPackage):
        return PkgEntry(name)
##    if hasattr(package, '__file__'):
##        return ZipEntry(package.__file__, name)
##    elif hasattr(package, '__path__') and len(package.__path__) == 1:
##        return DirEntry(package.__path__[0], name)
    elif isinstance(package, TypeType):
        # this 'package' is a java class
        f = getFile(name[:dot])
        if f:
            return f

class PkgEntry:
    def __init__(self, classname):
        self.classname = classname

    def __repr__(self):
        return "PkgEntry(%s)" % (self.classname)

    def getInputStream(self):
        res = unfix(self.classname) + '.class'
        return openResource(res)

    def getZipName(self):
        return '/'.join(self.classname.split('.')) + '.class'

class ResourceEntry:
    def __init__(self, name):
        self.name = name

    def __repr__(self):
        return "ResourceEntry(%s)" % (self.name)

    def getInputStream(self):
        import java
        return java.lang.Class.getResourceAsStream("".__class__, self.name)

    def getZipName(self):
        return self.name[1:]


##class ZipEntry:
##    def __init__(self, filename, classname):
##        self.filename = filename
##        self.classname = classname
##
##    def __repr__(self):
##        return "ZipEntry(%s, %s)" % (self.filename, self.classname)
##
##    def getInputStream(self):
##        zf = getzip(self.filename)
##        zfilename = unfix(self.classname) + '.class'
##        entry = zf.getEntry(zfilename)
##        return zf.getInputStream(entry)
##
##
##class DirEntry:
##    def __init__(self, dirname, classname):
##        self.dirname = dirname
##        self.classname = classname
##
##    def __repr__(self):
##        return "DirEntry(%s, %s)" % (self.dirname, self.classname)
##
##    def getInputStream(self):
##        lastname = self.classname.split('.')[-1]
##        fullname = os.path.join(self.dirname, lastname+'.class')
##        return io.FileInputStream(fullname)


def depends(name):
    if name[0] == '/':
        return ResourceEntry(name), []
    ze = getFile(name)
    ip = ze.getInputStream()
    ret = dependencies(ip)
    ip.close()
    return ze, ret


if __name__ == '__main__':
    #print getFile('org.python.modules.strop')
    #print getFile('java.lang.String')

    #print depend('com.oroinc.text.regex.Perl5Matcher')
    for name in ['javax.swing.JButton', 'java.lang.String']:
        print depends(name)
