# see org.python.core.SysPackageManager

import sys
import os
from string import strip

from java import io

from org.python.core import PathPackageManager
class YaPM(PathPackageManager):

    def __init__(self, registry):
        self.findAllPackages(registry)

    def findClass(self, pkg, name):
        return None

    def findAllPackages(self,registry):
        paths = registry.getProperty("python.packages.paths","java.class.path")
        paths = paths.split(',')
        # opt
        if "sun.boot.class.path" in paths: # ??pending strip boot class paths of other jvms?
            paths.remove("sun.boot.class.path")
        fakepath = registry.getProperty("python.packages.fakepath", None)

        for p in paths:
            e = registry.getProperty(p)
            if e != None:
                self.addClassPath(e)

        if fakepath != None:
            self.addClassPath(fakepath)
                    
    def filterByName(self,name,pkg):
        return 0

    def filterByAccess(self,name,acc):
        return not ((name.find('$') != -1) or (acc & 1 == 0))

    def doDir(self, jpkg, instantiate, exclpkgs):
        basic = self.basicDoDir(jpkg, 0, exclpkgs)
        ret = []

        self.super__doDir(self.searchPath,ret,jpkg,0,exclpkgs)
        self.super__doDir(sys.path,ret,jpkg,0,exclpkgs)
    
        return self.merge(basic,ret)
      
    def packageExists(self,pkg,name):
        return self.super__packageExists(self.searchPath,pkg,name) or self.super__packageExists(sys.path,pkg,name)
