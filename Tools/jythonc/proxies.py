# Copyright © Corporation for National Research Initiatives

from types import TupleType, IntType

import jast

from java.lang.reflect.Modifier import *
from java.lang.reflect import Modifier
from java.lang import *
import org, java



def aget(dict, key, default):
    if dict.has_key(key):
        return dict[key]
    else:
        ret = {}
        dict[key] = ret
        return ret


#Utility functions for converting between java and python types
def typeName(cls):
    if Class.isArray(cls):
        return typeName(Class.getComponentType(cls))+"[]"
    else:
        if '$' in cls.__name__:
            outer = cls.getDeclaringClass()
            if outer:
                l = len(outer.__name__)
                return "%s.%s" % (typeName(outer), cls.__name__[l+1:])
        return cls.__name__



def nullReturn(ret):
    if ret == Void.TYPE:
        return jast.Return()

    if Class.isPrimitive(ret):
        if ret.__name__ == 'boolean':
            value = jast.False
        elif ret.__name__ == 'char':
            value = jast.CharacterConstant('x')
        else:
            value = jast.IntegerConstant(0)
    else:
        value = jast.Null
    return jast.Return(value)



def makeReturn(code, ret):
    if ret == Void.TYPE:
        return code

    if Class.isPrimitive(ret):
        r = jast.InvokeStatic("Py", "py2"+ret.__name__, [code])
    else:
        r = jast.InvokeStatic("Py", "tojava",
                              [code, jast.StringConstant(ret.__name__)])
        r = jast.Cast(typeName(ret), r)
    return jast.Return(r)



def makeObject(code, c):
    if c in [Integer.TYPE, Byte.TYPE, Short.TYPE, Long.TYPE]:
        mname = "newInteger"
    elif c in [Character.TYPE]:
        mname = "newString"
    elif c in [Float.TYPE, Double.TYPE]:
        mname = "newFloat"
    elif c in [Boolean.TYPE]:
        mname = "newBoolean"
    else:
        return code
    return jast.InvokeStatic("Py", mname, [code])



def filterThrows(throws):
    ret = []
    for throwc in throws:
        #might want to remove subclasses of Error and RuntimeException here
        ret.append(throwc.__name__)
    return ret



def wrapThrows(stmt, throws, retType):
    if len(throws) == 0: return stmt
    catches = []
    throwableFound = 0
    for i in range(len(throws)):
        throw = throws[i]
        exctype = throw
        excname = jast.Identifier("exc%d" % i)
        body = jast.Block([jast.Throw(excname)])
        catches.append( (exctype, excname, body) )
        if throw == "java.lang.Throwable":
            throwableFound = 1

    if not throwableFound:
        body = jast.Block([jast.Invoke(jast.Identifier("inst"),
                                   "_jthrow", [jast.Identifier("t")]),
                       nullReturn(retType)])
        catches.append( ("java.lang.Throwable", jast.Identifier("t"), body) )
    return jast.TryCatches(jast.Block([stmt]), catches)



class JavaProxy:
    def __init__(self, name, supername, bases, methods, module=None,
                       issuperproxy = 1):
        self.bases = bases
        self.name = name
        self.supername = supername
        self.methods = methods
        self.issuperproxy = 0 #issuperproxy

        self.packages = self.properties = jast.Null
        self.modname = "foo"

        self.modifier = "public"
        self.initsigs = None

        self.package = None
        self.module = module
        if module is not None:
            self.packages = module.getPackages()
            self.properties = module.getProperties()
            self.modname = module.name
            if module.package is not None:
                self.modname = module.package+'.'+self.modname
            self.modules = module.modules.keys()

        self.isAdapter = 0
        self.frozen = 1

        self.superclass = Object
        self.interfaces = []

        self.jmethods = {}
        self.supermethods = {}
        for base in bases:
            self.addMethods(base)
            if base.isInterface():
                self.interfaces.append(base)
            else:
                self.superclass = base
        self.cleanMethods()

        if self.supername is None:
            self.supername = self.superclass.__name__

        self.jconstructors = []
        self.addConstructors(self.superclass)

        self.innerClasses = []

    def dumpAll(self):
        self.statements = []
        self.dumpInnerClasses()
        self.dumpMethods()
        self.dumpConstructors()
        self.addPyProxyInterface()
        self.addClassDictInit()
        return self.statements

    def dumpInnerClasses(self):
        for ic in self.innerClasses:
            self.statements.append(ic)

    def dumpMethods(self):
        names = self.jmethods.keys()
        names.sort()
        #print 'adding methods', self.name, names
        pymethods = {}
        for name, args, sigs in self.methods:
            pymethods[name] = 1
            #print name, args, sig
            if sigs is not None:
                if name == "__init__":
                    self.initsigs = sigs
                    continue

                #print sigs
                for access, ret, sig, throws in sigs:
                    self.callMethod(name, access, ret, sig, throws, 0)
                    if self.jmethods.has_key(name):
                        x = filter(lambda c: isinstance(c, TupleType), sig)
                        x = tuple(map(lambda c: c[0], x))
                        if self.jmethods[name].has_key(x):
                            del self.jmethods[name][x]

        for name in names:
            for sig, (access, ret, throws) in self.jmethods[name].items():
                #print name, access, isProtected(access), isFinal(access)
                if isProtected(access):
                    supername = name
                    if isFinal(access):
                     	access = access & ~FINAL
                        access = access & ~PROTECTED | PUBLIC
                        self.callSuperMethod(name, "super__" + name, 
                              access, ret, sig, throws)
		    	continue
                elif isFinal(access):
                    continue

                if isAbstract(access):
                    access = access & ~PROTECTED | PUBLIC
                    self.callMethod(name, access, ret, sig, throws, 0)
                elif pymethods.has_key(name):
                    access = access & ~PROTECTED | PUBLIC
                    self.callMethod(name, access, ret, sig, throws, 1)
                elif isProtected(access):
                    access = access & ~PROTECTED | PUBLIC
                    self.callSuperMethod(name, name, access, ret, sig, throws)

    def dumpConstructors(self):
        if self.initsigs is not None:
            #print self.initsigs
            for access, ret, sig, throws in self.initsigs:
                self.callConstructor(access, sig, throws, 0)
        else:
            for access, sig, throws in self.jconstructors:
                self.callConstructor(access, sig, throws, 1)


    def cleanMethods(self):
        for name, value in self.jmethods.items():
            if len(value) == 0:
                del self.jmethods[name]

    def addMethods(self, c):
        #print 'adding', c.__name__, c.getDeclaredMethods()
        for method in c.getDeclaredMethods():
            #Check that it's not already included
            name = method.name
            sig = tuple(method.parameterTypes)
            ret = method.returnType

            mdict = aget(self.jmethods, name, {})

            if mdict.has_key(sig):
                continue

            access = method.modifiers

            if isPrivate(access) or isStatic(access):
                continue

            if isNative(access):
                access = access & ~NATIVE

            if isProtected(access):
                #access = access & ~PROTECTED | PUBLIC
                if isFinal(access):
                    pass
                    #addSuperMethod(method, access)     
            #elif isFinal(access):
            #    continue

            throws = method.exceptionTypes
            mdict[sig] = access, ret, throws

        sc = c.getSuperclass()
        if sc is not None:
            self.addMethods(sc)

        for interface in c.getInterfaces():
            self.addMethods(interface)


    def addConstructors(self, c):
        for constructor in c.getDeclaredConstructors():
            access = constructor.modifiers
            if isPrivate(access):
                continue
            if isNative(access):
                access = access & ~NATIVE
            if isProtected(access):
                access = access & ~PROTECTED | PUBLIC
            parameters = tuple(constructor.parameterTypes)
            throws = constructor.exceptionTypes
            self.jconstructors.append( (access, parameters, throws) )


    def callSuperMethod(self, name, supername, access, ret, sig, throws=[]):
        if self.issuperproxy:
            return
        self.supermethods[supername] = supername
        args = [typeName(ret)]
        argids = []
        throws = filterThrows(throws)
        for c in sig:
            if isinstance(c, TupleType):
                argname = c[1]
                c = c[0]
            else:
                argname = "arg"+str(len(argids))
            args.append( (typeName(c), argname) )
            argid = jast.Identifier(argname)
            argids.append(argid)

        supercall = jast.Invoke(jast.Identifier("super"), name, argids)
        if ret != Void.TYPE:
            supercall = jast.Return(supercall)

        supermethod = jast.Method(supername,
                                  jast.Modifier.ModifierString(access), 
                                  args, jast.Block([supercall]), throws)
        self.statements.append(supermethod)
        return


    def callMethod(self, name, access, ret, sig, throws=[], dosuper=1):
        args = [typeName(ret)]
        argids = []
        objects = []
        throws = filterThrows(throws)
        for c in sig:
            if isinstance(c, TupleType):
                argname = c[1]
                c = c[0]
            else:
                argname = "arg"+str(len(argids))
            args.append( (typeName(c), argname) )
            argid = jast.Identifier(argname)
            argids.append(argid)
            objects.append(makeObject(argid, c))

        objects = jast.FilledArray("Object", objects)

        stmts = []
        this = jast.Identifier("this")

        if isinstance(access, IntType) and isAbstract(access):
            dosuper = 0
            access = access & ~ABSTRACT

        if not dosuper and not self.isAdapter:
            getattr = jast.InvokeStatic("Py", "jgetattr",
                                        [this, jast.StringConstant(name)])
        else:
            getattr = jast.InvokeStatic("Py", "jfindattr",
                                        [this, jast.StringConstant(name)])

        inst = jast.Identifier("inst")
        if len(throws) == 0:
            jcall = "_jcall"
        else:
            jcall = "_jcallexc"

        jcall = makeReturn(jast.Invoke(inst, jcall, [objects]), ret)
        jcall = wrapThrows(jcall, throws, ret)

        if dosuper:
            supercall = jast.Invoke(jast.Identifier("super"), name, argids)
            if ret != Void.TYPE:
                supercall = jast.Return(supercall)

            supermethod = None
            if not self.issuperproxy:
                supermethod = jast.Method("super__"+name,
                                      jast.Modifier.ModifierString(access), 
                                      args, jast.Block([supercall]), throws)
                self.supermethods["super__"+name] = "super__"+name
        else:           
            if self.isAdapter:
                supercall = nullReturn(ret)
            else:
                supercall = None
            supermethod = None

        if not dosuper and not self.isAdapter:
            test = jcall
        else:
            test = jast.If(jast.Operation("!=", inst, jast.Null),
                           jcall, supercall)
        code = jast.Block([jast.Declare("PyObject", inst, getattr), test])

        meth = jast.Method(name, jast.Modifier.ModifierString(access),
                           args, code, throws)

        if supermethod is not None:
            self.statements.append(supermethod)

        self.statements.append(meth)

    def callConstructor(self, access, sig, throws=[], dosuper=1):
        args = []
        argids = []
        objects = []
        throws = filterThrows(throws)
        for c in sig:
            if isinstance(c, TupleType):
                argname = c[1]
                c = c[0]
            else:
                argname = "arg"+str(len(argids))
            args.append( (typeName(c), argname) )
            argid = jast.Identifier(argname)
            argids.append(argid)
            objects.append(makeObject(argid, c))

        objects = jast.FilledArray("Object", objects)

        stmts = []
        this = jast.Identifier("this")

        if dosuper:
            supercall = jast.InvokeLocal("super", argids)
        else:
            supercall = jast.InvokeLocal("super", [])
##          for saccess, ssig in self.jconstructors:
##              if len(ssig) == len(sig):
##                  supercall = jast.InvokeLocal("super", argids)
##                  break
##          else:
##              supercall = jast.InvokeLocal("super", [])

        frozen = self.module.getFrozen()

        initargs = [objects]
        initproxy = jast.InvokeLocal("__initProxy__", initargs)

        code = jast.Block([supercall, initproxy])
        self.statements.append(jast.Constructor(
            self.name,
            jast.Modifier.ModifierString(access), args, code, throws))

    def addPyProxyInterface(self):
        self.statements.append(jast.Declare('private PyInstance',
                                            jast.Identifier('__proxy')))
        code = jast.Set(jast.Identifier("__proxy"), jast.Identifier("inst")) 
        code = jast.Block( [code] )
        self.statements.append(jast.Method("_setPyInstance", "public",
                                           ["void", ("PyInstance", "inst")],
                                           code))
        code = jast.Block([jast.Return(jast.Identifier("__proxy"))])
        self.statements.append(jast.Method("_getPyInstance", "public",
                                           ["PyInstance"], code))

        self.statements.append(jast.Declare('private PySystemState',
                                            jast.Identifier('__sysstate')))
        code = jast.Set(jast.Identifier("__sysstate"),
                        jast.Identifier("inst")) 
        code = jast.Block( [code] )
        self.statements.append(jast.Method("_setPySystemState", "public",
                                           ["void", ("PySystemState", "inst")],
                                           code))
        code = jast.Block([jast.Return(jast.Identifier("__sysstate"))])
        self.statements.append(jast.Method("_getPySystemState", "public",
                                           ["PySystemState"], code))


        frozen = self.module.getFrozen()
        this = jast.Identifier("this")
        initargs = [this, jast.StringConstant(self.modname),
                    jast.StringConstant(self.name),
                    jast.Identifier("args"), self.packages, self.properties,
                    frozen, jast.StringArray(self.modules)]

        initproxy = jast.InvokeStatic("Py", "initProxy", initargs)


        code = jast.Block([initproxy])
        self.statements.append(jast.Method("__initProxy__", "public",
                                           ["void", ("Object[]", "args")], code))

        self.interfaces.append(org.python.core.PyProxy)

    def addClassDictInit(self):
        self.interfaces.append(org.python.core.ClassDictInit)

        namelist = jast.InvokeStatic("Py", "java2py", [
             jast.StringArray(self.supermethods.keys()) ]);

        code = jast.Invoke(jast.Identifier("dict"), "__setitem__", [
                         jast.StringConstant("__supernames__"), namelist]);

        code = jast.Block([code])
        self.statements.append(jast.Method("classDictInit", "static public",
                                           ["void", ("PyObject", "dict")], code))

    def makeClass(self):
        mycode = self.dumpAll()                 
        body = jast.Block(mycode)
        return jast.Class(self.name, self.modifier, self.supername,
                          map(lambda i: i.__name__, self.interfaces), body)

    def getDescription(self):
        COMMASPACE = ', '
        ret = self.name+' extends '+self.supername
        if len(self.interfaces) > 0:
            ret = ret + ' implements ' + \
                  COMMASPACE.join(map(lambda i: i.__name__, self.interfaces))
        return ret



if __name__ == '__main__':
    import java
    methods = [("init", None, None),
               ("enable", None, ("public", Void.TYPE,
                                 [(java.awt.Event, 'event')]
                                 ))
               ]
    jp = JavaProxy("Foo", [java.util.Random], methods) #applet.Applet], methods)

    print jast.Block(jp.dumpAll())
