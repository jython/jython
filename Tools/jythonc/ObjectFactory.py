# Copyright © Corporation for National Research Initiatives
import jast
import org, java
from Object import Object, PyObject, Generic



def makeAnys(args):
    ret = []
    for arg in args:
        ret.append(arg.asAny())
    return ret



def PyObjectArray(args):
    aargs = makeAnys(args)
    return jast.FilledArray("PyObject", aargs)

import SimpleCompiler



class ObjectFactory:
    def __init__(self, parent=None):
        self.parent = parent
    #makeModule????

    def importName(self, name):
        ns = PyNamespace(self.parent, name)
        return Object(ns.getNew(), ns)

    def makeFunction(self, name, args, body, doc=None):
        func = PyFunction(self.parent, self, name, args, body, doc)
        return Object(func.getNew(), func)

    def makeClass(self, name, bases, body, doc=None):
        cls =  PyClass(self.parent, self, name, bases, body, doc)
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

    def makeSlice(self, items):
        code = jast.New("PySlice", 
            [items[0].asAny(), items[1].asAny(), items[2].asAny()])
        return Object(code, Generic)

    def makeFunctionFrame(self):
        return SimpleCompiler.LocalFrame(self.parent)

    def makeClassFrame(self):
        return SimpleCompiler.LocalFrame(self.parent, 
                                         SimpleCompiler.DynamicStringReference)

    def getCompiler(self, frame):
        return SimpleCompiler.SimpleCompiler(self.parent.module, self, frame,
                                             self.parent.options)


class FixedObject(PyObject):
    pass



class PyConstant(FixedObject):
    def __init__(self, value):
        self.value = value

    def getStatement(self, code=None):
        return jast.Comment(str(self.value))



class PyFunction(FixedObject):
    def __init__(self, parent, factory, name, args, body, doc=None):
        self.name = name
        self.factory = factory
        self.parent = parent
        self.args = args
        self.body = body
        self.doc = doc

    def getNew(self):
        globals = jast.GetInstanceAttribute(self.parent.frame.frame,
                                            "f_globals")
        pycode = self.makeCode()
        return jast.New("PyFunction",
                        [globals, PyObjectArray(self.args.defaults), pycode])

    def makeCode(self):
        # Don't handle a,b style args yet       
        init_code = []
        frame = self.parent.frame
        # Add args to funcframe
        funcframe = self.factory.makeFunctionFrame()
        for argname in self.args.names:
            funcframe.setname(argname, self.factory.makePyObject(None))
            #funcframe.addlocal(argname)
        # Parse the body
        comp = self.factory.getCompiler(funcframe)
        code = jast.Block([comp.parse(self.body)])
        # Set up a code object
        self.pycode = self.parent.module.getCodeConstant(
            self.name, self.args, funcframe.getlocals(), code)
        self.frame = funcframe
        return self.pycode



class PyClass(FixedObject):
    def __init__(self, parent, factory, name, bases, body, doc=None):
        self.name = name
        self.parent = parent
        self.factory = factory
        self.bases = bases
        self.body = body
        self.doc = doc

    def getNew(self):
        args = [jast.StringConstant(self.name),
                PyObjectArray(self.bases), self.pycode,
                jast.Null]

        if self.isSuperclassJava():
            args.append(jast.Identifier("%s.class" % self.proxyname))

        return jast.InvokeStatic("Py", "makeClass", args)

    def isSuperclassJava(self):
        if hasattr(self, 'javaclasses'):
            return len(self.javaclasses)

        self.javaclasses = []
        self.proxyname = None
        self.supername = None
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
                if jc is not None:
                    self.javaclasses.append(jc)
                    self.proxyname = self.name
                    if not jc.isInterface():
                        self.supername = jc.__name__
                    continue
            if isinstance(base, PyClass):
                if base.isSuperclassJava():
                    self.javaclasses.extend(base.javaclasses)
                    self.proxyname = self.name
                    self.supername = base.name
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
                                self.javaclasses.extend(cls.value.javaclasses)
                                self.proxyname = self.name
                                self.supername = cls.value.name
                                continue

        if len(self.javaclasses) and self.supername == None:
            self.supername = "java.lang.Object"
            self.proxyname = self.name
        return self.supername != None


    def makeCode(self):
        classframe = self.factory.makeClassFrame()
        comp = self.factory.getCompiler(classframe)
        code = jast.Block([comp.parse(self.body),
                           jast.Return(jast.Invoke(classframe.frame,
                                                   "getf_locals", []))])
        self.frame = classframe
        self.pycode = self.parent.module.getCodeConstant(
            self.name, None,
            classframe.getlocals(), code)
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
    mod = SimpleCompiler.BasicModule("foo")
    fact = ObjectFactory()
    pi = SimpleCompiler.SimpleCompiler(mod, fact)
    fact.parent = pi
    code = jast.Block(pi.execstring(data))
    mod.addMain(code, pi)

    print mod.attributes.keys()
    print mod.imports.keys()
    print mod
    mod.dump("c:\\jpython\\tools\\jpythonc2\\test")
