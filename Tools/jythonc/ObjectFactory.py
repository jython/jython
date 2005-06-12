# Copyright (c) Corporation for National Research Initiatives
import jast
import org, java
from Object import Object, PyObject, Generic
from org.python.parser import ast


def makeAnys(args):
    ret = []
    for arg in args:
        ret.append(arg.asAny())
    return ret



def PyObjectArray(args):
    aargs = makeAnys(args)
    return jast.FilledArray("PyObject", aargs)

import SrcGenCompiler



class ObjectFactory:
    def __init__(self, parent=None):
        self.parent = parent
    #makeModule????

    def importName(self, name):
        ns = PyNamespace(self.parent, name)
        return Object(ns.getNew(), ns)

    def makeFunction(self, name, def_compiler, scope, body, doc=None):
        func = PyFunction(self, name, def_compiler, scope, body, doc)
        return Object(func.getNew(), func)

    def makeClass(self, name, bases, def_compiler, scope, body, doc=None):
        cls =  PyClass(self, name, bases, def_compiler, scope, body, doc)
        # Yuck! We can't call getNew() before all modules have been analyzed.
        class DelayGen:
            def __init__(self, cls):
                self.cls = cls
                self.cls.makeCode()
            def sourceString(self):
                return self.cls.getNew().sourceString()
        return Object(DelayGen(cls), cls)

    def makeList(self, items):
        code = jast.New("PyList", [PyObjectArray(items)])
        return Object(code, Generic)

    def makeTuple(self, items):
        code = jast.New("PyTuple", [PyObjectArray(items)])
        return Object(code, Generic)

    def makeDictionary(self, items):
        lst = []
        for key, value in items:
            lst.append(key); lst.append(value)
        code = jast.New("PyDictionary", [PyObjectArray(lst)])
        return Object(code, Generic)

    def makeInteger(self, value):
        return Object(self.parent.module.getIntegerConstant(value),
                      PyConstant(value)) 

    def makeLong(self, value):
        return Object(self.parent.module.getLongConstant(value),
                      PyConstant(value)) 

    def makeImaginary(self, value):
        return Object(self.parent.module.getImaginaryConstant(value),
                      PyConstant(value)) 

    def makeFloat(self, value):
        return Object(self.parent.module.getFloatConstant(value),
                      PyConstant(value)) 

    def makeString(self, value):
        return Object(self.parent.module.getStringConstant(value),
                      PyConstant(value)) 

    def makeEllipsis(self):
        code = jast.GetStaticAttribute("Py", "Ellipsis")
        return Object(code, Generic)

    def makeJavaInteger(self, code):
        return Object(code, IntValue)

    def makeJavaString(self, code):
        return Object(code, StringValue)

    def makeObject(self, code, value):
        return Object(code, value)

    def makePyObject(self, code):
        return Object(code, Generic)

    def makeNull(self):
        return Object(jast.Null, Generic)

    def makePyNone(self):
        return Object(jast.GetStaticAttribute("Py", "None"), Generic)

    def makeSlice(self, items):
        code = jast.New("PySlice", 
            [items[0].asAny(), items[1].asAny(), items[2].asAny()])
        return Object(code, Generic)

    def getCompiler(self, parent_compiler, frameCtr, scope, className):
        return SrcGenCompiler.SrcGenCompiler(self.parent.module, self,
                                             parent = parent_compiler,
                                             frameCtr = frameCtr,
                                             scope = scope,
                                             options = self.parent.options,
                                             className = className)


class FixedObject(PyObject):
    pass



class PyConstant(FixedObject):
    def __init__(self, value):
        self.value = value

    def getStatement(self, code=None):
        return jast.Comment(str(self.value))



class PyFunction(FixedObject):
    def __init__(self, factory, name, def_compiler,scope, body, doc=None):
        self.name = name
        self.factory = factory
        self.def_compiler = def_compiler
        self.scope = scope
        self.body = body
        self.doc = doc

    def getNew(self):
        globals = jast.GetInstanceAttribute(self.def_compiler.frame.frame,
                                            "f_globals")
        pycode = self.makeCode()
        defaults = [ self.def_compiler.visit(d) for d in self.scope.ac.defaults ]
        clos = self.def_compiler.frame.makeClosure(self.scope)
        ctrargs = [globals, PyObjectArray(defaults), pycode]
        if clos:
            ctrargs.append(PyObjectArray(clos))
        return jast.New("PyFunction", ctrargs)

    def makeCode(self): # now handles a,b style args too
        # Add args to funcframe
        ac = self.scope.ac
        # Parse the body
        comp = self.factory.getCompiler(self.def_compiler,
                    SrcGenCompiler.FunctionFrame, self.scope,
                    self.def_compiler.className)
        for argname in ac.names:
            comp.frame.setname(argname, self.factory.makePyObject(None))

        tree = ast.Suite(self.body)
        ac.appendInitCode(tree)
        code = jast.Block([comp.parse(tree)])
        # Set up a code object
        self.pycode = self.def_compiler.top_compiler.module.getCodeConstant(
            self.name, code, comp.frame)
        self.frame = comp.frame
        return self.pycode



class PyClass(FixedObject):
    def __init__(self, factory, name, bases, def_compiler, scope, body, doc=None):
        self.name = name
        self.factory = factory
        self.bases = bases
        self.def_compiler = def_compiler
        self.scope = scope
        self.body = body
        self.doc = doc

    def getNew(self):
        args = [jast.StringConstant(self.name),
                PyObjectArray(self.bases), self.pycode,
                jast.Null]

        if self.isSuperclassJava():
            args.append(jast.Identifier("%s.class" % self.proxyname))

        clos = self.def_compiler.frame.makeClosure(self.scope)
        if clos:
            args.append(PyObjectArray(clos))

        return jast.InvokeStatic("Py", "makeClass", args)

    # proper logic for retrieving superproxy name
    def _takeSupername(self,cls,mod,modname = None):
        if modname is None:
            modname = ""
            if mod.package: modname = mod.package+'.'
            modname += mod.name
        self.javaclasses.extend(cls.javaclasses)
        self.proxyname = self.name
        self.supername = None
        self.issuperproxy = 1
        full_py = modname + '.' + cls.name
        if cls.name != mod.name:
            self.pySupername = self.supername = full_py
        else:
            self.pySupername = full_py
            self.supername = modname

    def isSuperclassJava(self):
        if hasattr(self, 'javaclasses'):
            return len(self.javaclasses)

        self.javaclasses = []
        self.proxyname = None
        self.supername = None
        self.pySupername = None
        self.issuperproxy = 0
        import compile
        for base in self.bases:
            if hasattr(base, "javaclass"):
                self.javaclasses.append(base.javaclass)
                self.proxyname = self.name
                self.supername = base.javaclass.__name__
                continue
            base = base.value
            if hasattr(base, "name"):
                jc = compile.getJavaClass(base.name)
                if jc is None:
                    idx = base.name.rfind('.')
                    if idx > 0:
                        #crude support for innerclasses.
                        name = base.name[:idx] + '$' + base.name[idx+1:]
                        jc = compile.getJavaClass(name)
                if jc is not None:
                    self.javaclasses.append(jc)
                    self.proxyname = self.name
                    if not jc.isInterface():
                        self.supername = jc.__name__
                    continue
            if isinstance(base, PyClass):
                if base.isSuperclassJava():
                    self._takeSupername(base,base.def_compiler.module)
                    continue
            if isinstance(base, PyNamespace):
                names = base.name.split('.')
                if len(names) >= 2:
                    modname = '.'.join(names[:-1])
                    mod = compile.Compiler.allmodules.get(modname, None)
                    if mod:
                        cls = mod.classes.get(names[-1], None)
                        if cls:
                            if cls.value.isSuperclassJava():
                                # pass no modname but just mod,
                                # javapackage should be included
                                self._takeSupername(cls.value,mod)
                                continue

        if len(self.javaclasses) and self.supername == None:
            self.supername = "java.lang.Object"
            self.proxyname = self.name
        return self.supername != None


    def makeCode(self):
        comp = self.factory.getCompiler(self.def_compiler,
                                        SrcGenCompiler.ClassFrame, self.scope,
                                        self.name)
        code = jast.Block([comp.parse(ast.Suite(self.body)),
                           jast.Return(jast.Invoke(comp.frame.frame,
                                                   "getf_locals", []))])
        self.frame = comp.frame
        self.pycode = self.def_compiler.top_compiler.module.getCodeConstant(
            self.name, code, comp.frame)
        return self.pycode

class PyNamespace(FixedObject):
    def __init__(self, parent, name):
        self.name = name
        self.parent = parent
        self.parent.addModule(name)

    def getNew(self):
        return jast.InvokeStatic("imp", "load",
                                 [jast.StringConstant(self.name)])

    def getattr(self, code, name):
        newobj = FixedObject.getattr(self, code, name)
        newobj.value = PyNamespace(self.parent, self.name+'.'+name)
        return newobj

    #Use the base definition of invoke so that getattr will be properly handled
    def invoke(self, code, name, args, keyargs):
        return self.getattr(code, name).call(args, keyargs)



data = """
x=1+1
#import pawt
#print pawt.test

for i in [1,2,3]:
    print i

def bar(x):
    print x*10
    y = x+2
    print y

bar(42)

class Baz:
    def eggs(self, x, y, z):
        return x, y, z

b = Baz()
print b.eggs(1,2,3)
"""

if __name__ == '__main__':
    mod = SrcGenCompiler.BasicModule("foo")
    fact = ObjectFactory()
    pi = SrcGenCompiler.SrcGenCompiler(mod, fact)
    fact.parent = pi
    code = jast.Block(pi.execstring(data))
    mod.addMain(code, pi)

    print mod.attributes.keys()
    print mod.imports.keys()
    print mod
    mod.dump("c:\\jpython\\tools\\jpythonc2\\test")
