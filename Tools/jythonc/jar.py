# Copyright © Corporation for National Research Initiatives

import os

import jarray
from java.util.zip import *
from java.io import *
from util import lookup
from util import listAllClasses, openResource

DOT = '.'
SLASH = '/'



def copy(instream, outstream):
    data = jarray.zeros(1024*4, 'b')
    while 1:
        if instream.available() == 0:
            break
        n = instream.read(data)
        if n == -1:
            break
        outstream.write(data, 0, n)



class JavaArchive:
    def __init__(self, packages=[]):
        self.files = []
        self.entries = []
        self.packages = packages
        self.manifest = []
        self.jar_entries = {}

    def addFile(self, rootdir, filename):
        self.files.append( (rootdir, filename) )

    def addClass(self, rootdir, classname, properties=None):
        filename = apply(os.path.join, classname.split('.'))+'.class'
        outfile = SLASH.join(filename.split(os.sep))
        if self.jar_entries.has_key(outfile):
            return
        self.jar_entries[outfile] = 1

        self.addFile(rootdir, filename)
        if properties is not None:
            self.manifest.append("Name: "+ SLASH.join(classname.split('.')))
            self.addToManifest(properties)

    def addEntry(self, entry):
        outfile = SLASH.join(entry.classname.split('.')) + '.class'
        if self.jar_entries.has_key(outfile):
            return
        self.jar_entries[outfile] = 1

        self.entries.append(entry)

    def addToManifest(self, properties=None, **kw):
        if properties is None:
            properties = {}
        properties.update(kw)
        for name, value in properties.items():
            self.manifest.append(name+": "+value)
        self.manifest.append("")

    def dumpManifest(self):
        if len(self.manifest) == 0:
            return
        outfile = "META-INF/MANIFEST.MF"
        NL = '\n'
        self.zipfile.putNextEntry(ZipEntry(outfile))
        self.zipfile.write(NL.join(self.manifest))

    def dumpFiles(self):
        for rootdir, filename in self.files:
            infile = os.path.join(rootdir, filename)
            outfile = SLASH.join(filename.split(os.sep))
            instream = FileInputStream(infile)
            self.zipfile.putNextEntry(ZipEntry(outfile))
            copy(instream, self.zipfile)
            instream.close()

        for entry in self.entries:
            outfile = SLASH.join(entry.classname.split('.')) + '.class'
            instream = entry.getInputStream()
            self.zipfile.putNextEntry(ZipEntry(outfile))
            copy(instream, self.zipfile)
            instream.close()                    

        for package, skiplist in self.packages:
            self.addPackage(package, skiplist)

        self.dumpManifest()

    def dump(self, filename):
        self.zipfile = ZipOutputStream(FileOutputStream(filename))
        self.dumpFiles()
        self.zipfile.close()

    # handle packages (typically org.python.core, ...)
    def addPackage(self, package, skiplist = []):
        pkg = lookup(package)
        base = package.replace(DOT,SLASH)
        for cl in listAllClasses(pkg):
            name = package+ '.' +cl
            if name in skiplist:
                # print 'skipping',name # ?? dbg
                continue
            entryname = base +'/' + cl + '.class'
            self.zipfile.putNextEntry(ZipEntry(entryname))
            instream = openResource(entryname)
            copy(instream, self.zipfile)
            instream.close()
            
##    # add just one class from a package
##    def addOneClass(self, pkgclass):
##        parts = pkgclass.split('.')
##        package = DOT.join(parts[:-1])
##        pkg = lookup(package)
##        filename = os.path.join(pkg.__path__[0], parts[-1]) + '.class'
##        entryname = '/'.join(parts) + '.class'
##        self.zipfile.putNextEntry(ZipEntry(entryname))
##        instream = FileInputStream(filename)
##        copy(instream, self.zipfile)
##        instream.close()
##
##    # The next three methods handle packages (typically org.python.core, ...)
##    def addPackage(self, package, skiplist=[]):
##        pkg = lookup(package)
##        if hasattr(pkg, '__file__'):
##            return self.addZipPackage(package+'.', pkg.__file__, skiplist)
##        elif hasattr(pkg, '__path__') and len(pkg.__path__) == 1:
##            return self.addDirectoryPackage(package+'.',
##                                            pkg.__path__[0], skiplist)
##        raise ValueError, "can't find package: "+repr(package)
##
##    def addZipPackage(self, package, zipfile, skiplist):
##        zf = ZipFile(zipfile)
##        for entry in zf.entries():
##            filename = entry.name
##            if filename[-6:] != '.class':
##                continue
##            name = filename[:-6].replace(SLASH, DOT)
##
##            if name[:len(package)] != package:
##                continue
##            self.zipfile.putNextEntry(ZipEntry(filename))
##            copy(zf.getInputStream(entry), self.zipfile)
##        zf.close()
##
##    def addDirectoryPackage(self, package, directory, skiplist):
##        for file in os.listdir(directory):
##            if file[-6:] != '.class':
##                continue
##            name = package+file[:-6]
##            if name in skiplist:
##                continue
##            entryname = SLASH.join(name.split('.')) + '.class'
##            self.zipfile.putNextEntry(ZipEntry(entryname))
##            instream = FileInputStream(os.path.join(directory, file))
##            copy(instream, self.zipfile)
##            instream.close()



if __name__ == '__main__':
    root = "c:\\jpython\\tools\\jpythonc2"
    ja = JavaArchive()
    ja.addFile(root, "jar.py")
    ja.addFile(root, "proxies.py")
    ja.addClass(root, "jast.Statement$py")
    print ja.files
    ja.dump(os.path.join(root, "test\\t.jar"))
