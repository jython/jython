# java classes reload support (experimental)
# Copyright 2000 Samuele Pedroni
# ?? doc + examples pending

# ?? could have problem with import pkg.jclass.inner (this should not be used in any case)
# ?? using import * with a load-set together with reloading can be confusing
#    cannot be fixed => anyway import * is not for production code

__version__ = "0.2"

import sys
from org.python.core import imp,PyJavaPackage,PyJavaClass
from _jython import is_lazy as _is_lazy

import jxxload_help


class _LoaderFactory(jxxload_help.JavaLoaderFactory):
    def __init__(self,path):
        vfs = jxxload_help.PathVFS()
        for fname in path:
            vfs.addVFS(fname)
        self.vfs = vfs

    def makeLoader(self):
        return jxxload_help.PathVFSJavaLoader(self.vfs,imp.getSyspathJavaLoader())

class _Unload:

    def __init__(self,ls):
        self.ls = ls
        self.ls_name = ls._name
        self.loader = ls._mgr.loader

    def do_unload(self,pkg):
        for n in pkg.__dict__.keys():
            e = pkg.__dict__[n]
            if isinstance(e,PyJavaClass):
                if _is_lazy(e): continue
                if e.classLoader is self.loader:
                    del pkg.__dict__[n]
                    if pkg.__name__:
                        n = self.ls_name + '.' + pkg.__name__ + '.' +n
                    else:
                        n = self.ls_name + '.' + n
                    if sys.modules.has_key(n): del sys.modules[n]

            elif isinstance(e,PyJavaPackage):
                self.do_unload(e)

    def __call__(self):
        if self.loader:
            if self.ls._mgr.checkLoader() is self.loader:
                self.do_unload(self.ls._top)
                self.ls._mgr.resetLoader()
            loader = self.loader
            jxxload_help.DiscardHelp.discard(loader,loader.interfaces)
            self.loader = None

class LoadSet:
# ?? for the moment from import * and dir do not work for LoadSet, but work for
# contained pkgs
# need java impl as PyObject

    def __init__(self,name,path):
        mgr = jxxload_help.PackageManager(path,_LoaderFactory(path))
        self._name = name
        self._mgr = mgr
        self._top = mgr.topLevelPackage

    def __getattr__(self,name):
        try:
            return getattr(self._top,name)
        except:
            if name == 'unload': return _Unload(self)
            raise
            

    def __repr__(self):
        return "<java load-set %s>" % self._name

def unloadf(ls):
    if not isinstance(ls,LoadSet): raise TypeError,"unloadf(): arg is not a load-set"
    return _Unload(ls)

def makeLoadSet(name,path):
    if sys.modules.has_key('name'): return sys.modules[name]
    sys.modules[name] = ls = LoadSet(name,path)
    return ls

_reload = reload

def _do_reload(ls_name,mgr,pkg):
    pkg_name = pkg.__name__
    for n in pkg.__dict__.keys():
        e = pkg.__dict__[n]
        if isinstance(e,PyJavaClass):
            if _is_lazy(e): continue
            del pkg.__dict__[n]
            try :
                c = mgr.findClass(pkg_name,n);
                if c:
                    pkg.__dict__[n] = c
                    if pkg_name:
                        n = ls_name + '.' + pkg_name + '.' + n
                    else:
                        n = ls_name + '.' + n
                    if sys.modules.has_key(n): sys.modules[n] = c
            except:
                pass
        elif isinstance(e,PyJavaPackage):
            _do_reload(ls_name,mgr,e)

def reload(ls):
    if isinstance(ls,LoadSet):
        ls._mgr.resetLoader()
        _do_reload(ls._name,ls._mgr,ls._top)
        return ls
    else:
        return _reload(ls)
