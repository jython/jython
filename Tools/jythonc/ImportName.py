# Copyright © Corporation for National Research Initiatives

import sys
import os

from org.python.core import PyModule, PyJavaClass, PyClass, \
     PyJavaPackage, PyBeanEventProperty

from util import lookup



def wrapJava(mod):
    if isinstance(mod, PyModule):
        return Module(mod)
    elif isinstance(mod, PyJavaClass):
        return JavaClass(mod)
    elif isinstance(mod, PyClass):
        return Class(mod)
    elif isinstance(mod, PyJavaPackage):
        return Package(mod)
    else:
        return Namespace(mod)

def topImport(name):
    if not name in sys.builtin_module_names:
        top = findOnPath(name)
        if top is not None:
            return top
    try:
        return wrapJava(__import__(name))
    except ImportError:
        return None

def importName(name):
    if name[0] == "/":
        return Resource(name)
    try:
        names = name.split('.')
        top = topImport(names[0])
        for name in names[1:]:
            top = top.getattr(name)
        return top
    except (ImportError, AttributeError):
        return None

def findOnPath(name, path=sys.path):
    try:
        pyname = name+".py"
        for d in path:
            pyfile = os.path.join(d, pyname)
            if os.path.exists(pyfile):
                return Module(name=name, file=pyfile)
            initfile = os.path.join(os.path.join(d, name), "__init__.py")
            if os.path.exists(initfile):
                return Module(name=name, file=None,
                              path=[os.path.join(d, name)])
    # TBD: this is bogus
    except "fake":
        return None

def lookupName(name):
    return importName(name)

class Resource:
    def __init__(self, name):
        self.name = name
    def getDepends(self):
        return []
 

class Namespace:
    def __init__(self, mod):
        if hasattr(mod, '__name__'):
            self.name = mod.__name__
        else:
            self.name = "<unknown>"
        self.mod = mod

    def getattr(self, name):
        try:
            attr = getattr(self.mod, name)
        except:
            return None
        return wrapJava(attr)

    def addEvents(self, attrs, events):
        pass

    def getDepends(self):
        if not hasattr(self.mod, '__depends__'):
            return []
        return map(lookupName, self.mod.__depends__)



from util import reportPublicPlainClasses
class Package(Namespace):
    _classes = {}
    def __init__(self, mod, *args):
        apply(Namespace.__init__, (self, mod)+args)
        if isinstance(mod, PyJavaPackage):
##            classes = PyJavaPackage._unparsedAll._doget(self.mod)
            classes = reportPublicPlainClasses(self.mod)
            if classes:
                self._classes[self.name] = classes
    def getClasses(self):
        return self._classes.get(self.name, None)



class Class(Namespace):
    pass



class Module(Namespace):
    def __init__(self, mod=None, name=None, file=None, path=None):
        Namespace.__init__(self, mod)
        if file is None and hasattr(mod, '__file__'):
            file = mod.__file__
        if name is not None:
            self.name = name
        self.file = file
        if name is None and hasattr(mod, '__path__'):
            path = mod.__path__
        self.path = path

    def getattr(self, name):
        ret = None
        if self.path is not None:
            ret = findOnPath(name, self.path)
        if ret is not None:
            ret.name = self.name+'.'+ret.name
            return ret
        if self.mod is None:
            self.mod = lookup(self.name)
        return Namespace.getattr(self, name)

    def __repr__(self):
        return "Module(%s, %s, %s)" % (self.name, self.file, self.path)


class JavaClass(Namespace):
    def __init__(self, mod):
        Namespace.__init__(self, mod)   
        self.file = None
        self.bases = self.findBases(mod)
        self.eventProperties = self.findEventProperties(mod)

    def __repr__(self):
        return "JavaClass(%s)" % (self.name,)

    def findBases(self, c):
        bases = []
        for base in c.__bases__:
            bases.append(JavaClass(base))
        return bases

    def findEventProperties(self, c):
        eps = {}
        for name, value in c.__dict__.items():
            if isinstance(value, PyBeanEventProperty):
                eps[name] = value.eventClass
        return eps

    def addEvents(self, attrs, events, source=None):
        for name, value in self.eventProperties.items():
            if attrs.has_key(name):
                try:
                    events[value][source] = 1
                except KeyError:
                    d = {source:1}
                    events[value] = d
        for base in self.bases:
            base.addEvents(attrs, events, source)



if __name__ == '__main__':
    import sys
    print sys.path
    print importName("pawt.colors"), importName("SimpleCompiler"), \
          importName("pawt")
    print importName("os.path")
    print sys.modules
