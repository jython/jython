# Copyright 2000 Samuele Pedroni
import sys

from java import io
from java.util import zip


class JarVFS:
    def __init__(self,fname):
        self.zipfile = zip.ZipFile(fname)

    def open(self,id):
        ent = self.zipfile.getEntry(id)
        if ent:
##            print "path-jar-open: %s!%s" % (self.zipfile.name,id) # ?? dbg
            return self.zipfile.getInputStream(ent)
        else:
            return None

    def __repr__(self):
        return "<jar-vfs '%s'>" % self.zipfile.name
        
class DirVFS:
    def __init__(self,dir):
        if dir == '':
            self.pfx = None
        else:
            self.pfx = dir

    def open(self,id):
        f = io.File(self.pfx, id.replace('/',io.File.separator))
        if f.file:
##            print "path-open:",f # ?? dbg
            return io.BufferedInputStream(io.FileInputStream(f))
        return None

    def __repr__(self):
        return "<dir-vfs '%s'>" % self.pfx

class PathVFS:

    def add_vfs(self,fname):
        if fname == '':
            if not self.once.has_key(''):
                self.once['']=1
                self.vfs.append(DirVFS(''));
            return        
        file=io.File(fname);
        canon = file.canonicalPath
        if not self.once.has_key(canon):
            self.once[canon]=1
            try:
                if file.directory:
                    self.vfs.append(DirVFS(fname));
                else:
                    if file.exists and (fname.endswith('.jar') or fname.endswith('.zip')):
                        self.vfs.append(JarVFS(fname))
            except:
                pass
        

    def __init__(self,registry):
        self.once = {}
        self.vfs = []
        paths = registry.getProperty("python.packages.paths","java.class.path")
        paths = paths.split(',')
        # opt
        if "sun.boot.class.path" in paths: # ??pending strip boot class paths of other jvms?
            paths.remove("sun.boot.class.path")

        #paths.append("python.packages.fakepath")

        for p in paths:
            e = registry.getProperty(p)
            if e != None:
                path = e.split(io.File.pathSeparator)
                for name in path:
                    self.add_vfs(name)

        for name in sys.path:
            self.add_vfs(name)
            
        del self.once

    def open(self,id):
        for v in self.vfs:
            stream = v.open(id)
            if stream:
                return stream
        return None
