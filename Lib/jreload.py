# java classes reload support (experimental)
# Copyright 2000 Samuele Pedroni
# ?? doc pending

__version__ = "0.1"

import sys
from org.python.core import imp,PyJavaPackage,PyJavaClass

import jxxload_help


class _Unload:

    def __init__(self,ls):
        self.ls = ls
        self.loader = ls._mgr.loader

    def do_unload(self,pkg):
        for n in pkg.__dict__.keys():
            e = pkg.__dict__[n]
            if isinstance(e,PyJavaClass):
                if PyJavaClass.isLazy(e): continue
                if e.classLoader is self.loader:
                    del pkg.__dict__[n]
            elif isinstance(e,PyJavaPackage):
                self.do_unload(e)

    def __call__(self):
        if self.loader:
            self.do_unload(self.ls._top)
            loader = self.loader
            jxxload_help.DiscardHelp.discard(loader,loader.interfaces)
            if self.ls._mgr.loader is self.loader:
                self.ls._mgr.resetLoader()
            self.loader = None

class LoadSet:
# ?? for the moment from import * and dir do not work for LoadSet, but work for
# contained pkgs
# need java impl as PyObject

    def __init__(self,name,path):
        mgr = jxxload_help.PackageManager(path,imp.getSyspathJavaLoader())
        self._name = name
        self._mgr = mgr
        self._top = mgr.topLevelPackage

    def __getattr__(self,name):
        if name == 'unload':
            return _Unload(self)
        else:
            return getattr(self._top,name)

    def __repr__(self):
        return "<java load-set %s>" % self._name

def makeLoadSet(name,path):
    if sys.modules.has_key('name'): return sys.modules[name]
    sys.modules[name] = ls = LoadSet(name,path)
    return ls

_reload = reload

def _do_reload(mgr,pkg):
    pkg_name = pkg.__name__
    for n in pkg.__dict__.keys():
        e = pkg.__dict__[n]
        if isinstance(e,PyJavaClass):
            if PyJavaClass.isLazy(e): continue
            del pkg.__dict__[n]
            try :
                c = mgr.findClass(pkg_name,n);
                if c:
                    pkg.__dict__[n] = c
            except:
                pass
        elif isinstance(e,PyJavaPackage):
            _do_reload(mgr,e)

def reload(ls):
    if isinstance(ls,LoadSet):
        ls._mgr.resetLoader()
        _do_reload(ls._mgr,ls._top)      
    else:
        return _reload(ls)
