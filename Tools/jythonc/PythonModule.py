# Copyright © Corporation for National Research Initiatives

import os

import jast
import java, org
from PythonVisitor import Arguments

EMPTYSTRING = ''



"""
class foo
    class py -- what gets imported by jpython
        holds all py constants
        One of these no matter how many classes
        maybe have static inner classes as well
"""

def clean(node):
    if not hasattr(node, 'exits'):
        print node
    if node.exits():
        return node
    return jast.Block([node,
                       jast.Return(jast.GetStaticAttribute('Py', 'None'))])



from java.lang.Character import isJavaIdentifierPart
def legalJavaName(name):
    letters = []
    for c in name:
        if isJavaIdentifierPart(c):
            letters.append(c)
    if len(letters) == 0:
        return "x"
    elif len(letters) == len(name):
        return name
    else:
        return EMPTYSTRING.join(letters)



class PythonInner:
    def __init__(self, parent):
        self.constantValues = {}
        self.constants = []             
        self.codes = []
        self.interfaces = []

        self.funccodes = []
        self.uniquenames = []

        self.modifier = "public static"
        self.parent = parent
        self.filename = parent.filename
        self.name = "_PyInner"

        self.superclass = "Object"

    def getConstant(self, value, code, prefix):
        if self.constantValues.has_key( (value,prefix) ):
            return self.constantValues[(value,prefix) ]

        name = prefix+"$"+str(len(self.constants))
        ret = jast.Identifier(name)
        self.constantValues[(value,prefix)] = ret
        self.constants.append( ("PyObject", ret, code) )
        return ret

    def getIntegerConstant(self, value):
        code = jast.InvokeStatic("Py", "newInteger",
                                 [jast.IntegerConstant(value)])
        return self.getConstant(value, code, "i")

    def getLongConstant(self, value):
        code = jast.InvokeStatic("Py", "newLong",
                                 [jast.StringConstant(str(value))])
        return self.getConstant(value, code, "l")

    def getImaginaryConstant(self, value):
        code = jast.InvokeStatic("Py", "newImaginary",
                                 [jast.FloatConstant(value)])
        return self.getConstant(value, code, "j")

    def getFloatConstant(self, value):
        code = jast.InvokeStatic("Py", "newFloat", [jast.FloatConstant(value)])
        return self.getConstant(value, code, "f")

    def getStringConstant(self, value):
        code = jast.InvokeStatic("Py", "newString",
                                 [jast.StringConstant(value)])
        return self.getConstant(value, code, "s")

    def getCodeConstant(self, name, args, locals, code):
        if args is None:
            args = Arguments([])
        label = "c$%d_%s" % (len(self.codes), legalJavaName(name))
        ret = jast.Identifier(label)
        self.codes.append( (label, name, args, locals, code) )
        return ret

    def dumpConstants(self):
        self.dumpCodes()
        stmts = []
        decls = []
        for type, label, value in self.constants:
            decls.append(jast.Declare("private static "+type, label))
            stmts.append(jast.Set(label, value))

        setconstants = jast.Method("initConstants", "private static",
                                   ["void"], jast.Block(stmts))
        decls.append(setconstants)
        return decls

    def dumpCodes(self):
        self.constants.append(["PyFunctionTable", self.getFunctionTable(),
                               jast.New(self.name, [])])

        for label, name, args, locals, code in self.codes:
            funcid = self.addFunctionCode(name, code)

            arglist = keyworddict = jast.False
            if args.arglist:
                arglist = jast.True
            if args.keyworddict:
                keyworddict = jast.True

            names = jast.StringArray(locals)

            cargs = [jast.IntegerConstant(len(args.names)),
                     names,
                     jast.StringConstant(self.filename),
                     jast.StringConstant(name),
                     arglist,
                     keyworddict,
                     self.getFunctionTable(),
                     jast.IntegerConstant(funcid)]
            newcode = jast.InvokeStatic("Py", "newCode", cargs)
            self.constants.append(("PyCode", jast.Identifier(label), newcode))

    def uniquename(self, name):
        self.uniquenames.append(name)
        return name+"$"+str(len(self.uniquenames))

    def dumpFuncs(self):
        meths = []
        cases = []

        args = ["PyObject", ("PyFrame", "frame")]
        access = "private static"

        callargs = [jast.Identifier("frame")]

        for name, funcid, code in self.funccodes:
            funcname = self.uniquename(name)
            meths.append(jast.Method(funcname, access, args, clean(code)))

            body = jast.Return(jast.InvokeStatic(self.name, funcname,
                                                 callargs))
            cases.append([jast.IntegerConstant(funcid),
                          jast.FreeBlock([body])])

        defaultCase = jast.FreeBlock([jast.Return(jast.Null)])
        switch = jast.Block([jast.Switch(jast.Identifier('index'), cases,
                                         defaultCase)])

        meths.insert(0, jast.Method("call_function", "public",
                                    ["PyObject",
                                     ("int", "index"),
                                     ("PyFrame", "frame")],
                                    switch))
        self.superclass = "PyFunctionTable"
        return meths

    def getFunctionTable(self):
        return jast.Identifier("funcTable")

    def addFunctionCode(self, name, code):
        self.funccodes.append((legalJavaName(name), len(self.funccodes), code))
        return len(self.funccodes)-1

    def addMain(self, code, cc):
        self.mainCode = self.getCodeConstant("main", Arguments([]),
                                             cc.frame.getlocals(), code)

    def dumpMain(self):
        if not hasattr(self, 'mainCode'):
            return []
        meths = []

        self.interfaces.append("PyRunnable")
        getmain = jast.Block(
            [jast.If(
                jast.Operation("==", self.mainCode, jast.Null),
                jast.InvokeStatic(self.name, "initConstants", [])),
             jast.Return(self.mainCode)])
        meths.append(jast.Method("getMain", "public", ["PyCode"], getmain))
        return meths

    def dumpAll(self):
        return [self.dumpConstants(),
                jast.Blank, self.dumpMain(), self.dumpFuncs()]

    def makeClass(self):
        body = jast.Block(self.dumpAll())
        return jast.Class(self.name, self.modifier,
                          self.superclass, self.interfaces, body)


defaultProps = {
## now redundant    
##    "python.packages.paths": "",
##    "python.packages.directories": "",
    "python.options.showJavaExceptions": "true",
    "python.modules.builtin": "exceptions:org.python.core.exceptions",
    }



class PythonModule:
    def getclassname(self, name):
        if self.package is not None:
            return self.package + '.' + name
        return name

    def addjavaclass(self, name):
        self.javaclasses.append(self.getclassname(name))

    def addinnerclass(self, name):
        self.addjavaclass(self.name+'$'+name)

    def __init__(self, name, filename="<unknown>", packages = [],
                 properties=defaultProps, frozen=1):
        package = None
        dot = name.rfind('.')
        if dot != -1:
            package = name[:dot]
            name = name[dot+1:]

        self.name = name
        self.filename = filename
        self.superclass = java.lang.Object
        self.interfaces = []
        self.temps = []

        self.package = package

        self.javamain = 1

        self.attributes = {}
        self.classes = {}
        self.imports = {}

        self.packages = packages                
        self.properties = properties
        self.innerClasses = []

        self.modifier = "public"

        self.pyinner = PythonInner(self)

        #if frozen:
        self.innerClasses.append(self.pyinner)

        self.frozen = frozen    
        self.javaproxy = None

        self.javaclasses = []
        self.addjavaclass(self.name)

    def getFrozen(self):
        if self.frozen:
            if self.package is None:
                return jast.StringConstant("")
            else:
                return jast.StringConstant(self.package)
        else:
            return jast.Null


    def addAttribute(self, name, value):
        self.attributes[name] = value
        #print ' mod add attr', self.name, name, value, self.attributes.keys()

    #Delegate constants
    def getIntegerConstant(self, value):
        return self.pyinner.getIntegerConstant(value)

    def getLongConstant(self, value):
        return self.pyinner.getLongConstant(value)

    def getImaginaryConstant(self, value):
        return self.pyinner.getImaginaryConstant(value)

    def getFloatConstant(self, value):
        return self.pyinner.getFloatConstant(value)

    def getStringConstant(self, value):
        return self.pyinner.getStringConstant(value)

    def getCodeConstant(self, name, args, locals, code):        
        return self.pyinner.getCodeConstant(name, args, locals, code)

    def addFunctionCode(self, name, code):
        return self.pyinner.addFunctionCode(name, code)

    def addMain(self, code, cc):
        return self.pyinner.addMain(code, cc)

    #Properties and packages for registry
    def getProperties(self):
        return jast.Identifier("jpy$properties")

    def getPackages(self):
        return jast.Identifier("jpy$packages")

    def dumpDictionary(self, dict, field):
        props = []
        for name, value in dict.items():
            props.append(name)
            props.append(value)
        return jast.Declare("static String[]", field, jast.StringArray(props))

    def dumpProperties(self):
        return self.dumpDictionary(self.properties, self.getProperties())

    def dumpPackages(self):
        return self.dumpDictionary(self.packages, self.getPackages())

    def dumpFields(self):
        return [self.dumpProperties(), self.dumpPackages()]

    def dumpInitModule(self):
        meths = []

        dict = jast.Identifier("dict")
        sargs = [jast.StringConstant("__name__"),
                 jast.New("PyString", [jast.StringConstant(self.name)])]
        rargs = [jast.Invoke(jast.New("_PyInner", []), "getMain", []),
                 dict, dict]
        code = jast.Block(
            [jast.Invoke(dict, "__setitem__", sargs),
             jast.InvokeStatic("Py", "runCode", rargs)])
        meths.append(jast.Method("moduleDictInit", "public static", 
                                 ["void", ("PyObject", "dict")], code))
        return meths

    #Define a Java main to run directly
    def dumpMain(self):
        meths = []
        if self.javamain:
            code = []
            newargs = jast.Identifier("newargs")
            code.append(jast.Declare("String[]", newargs, 
                   jast.NewArray("String", ["args.length+1"])))
            code.append(jast.Set(jast.Identifier("newargs[0]"),  
                   jast.StringConstant(self.name)))

            args = [jast.Identifier('args'), 
                    jast.IntegerConstant(0),
                    jast.Identifier('newargs'), 
                    jast.IntegerConstant(1),
                    jast.Identifier('args.length')]
            code.append(jast.InvokeStatic("System", "arraycopy", args))

            args = [jast.StringConstant(self.getclassname(
                           self.name+'$'+self.pyinner.name)),
                    jast.Identifier('newargs'), 
                    self.getPackages(), self.getProperties(), 
                    self.getFrozen(), jast.StringArray(self.modules.keys())]

            code.append([jast.InvokeStatic("Py", "runMain", args)])
            maincode = jast.Block(code)
            meths.append(jast.Method("main", "public static", 
                                     ["void", ("String[]", "args")], maincode))

        return meths
        #args = [jast.StringConstant(self.name), jast.Identifier('dict')]
        #initcode = jast.Block([jast.InvokeStatic("Py", "initRunnable", args)])
        #meths.append(jast.Method("initModule", "public", 
        #       ["void", ("PyObject", "dict")], initcode))

    def dumpInnerClasses(self):
        ret = []                
        for inner in self.innerClasses:
            self.addinnerclass(inner.name)
            ret.append(inner.makeClass())
        return ret

    def dumpAll(self):
        return [self.dumpFields(),
                jast.Blank, self.dumpInnerClasses(),
                self.dumpInitModule(), self.dumpMain()]

    def makeClass(self):
        mycode = self.dumpAll()
        supername = self.superclass.__name__
        if self.javaproxy is not None:
            mycode = [mycode, self.javaproxy.dumpAll()]
            self.superclass = self.javaproxy.superclass
            self.interfaces = self.interfaces+self.javaproxy.interfaces
            supername = self.javaproxy.supername

        body = jast.Block(mycode)
        return jast.Class(self.name, self.modifier, supername,
                          map(lambda i: i.__name__, self.interfaces), body)


    def makeClassFile(self):
        header = []
        if self.package is not None:
            header.append(jast.Identifier("package %s" % self.package))
            header.append(jast.Blank)
        header.append(jast.Import("org.python.core.*"))
        header.append(jast.Blank)
        return jast.FreeBlock([header, self.makeClass()])

    def dump(self, directory):
        cf = self.makeClassFile()
        sf = jast.Output.SourceFile(self.name)
        if self.package is not None:
            pack = apply(os.path.join, self.package.split('.'))
            directory = os.path.join(directory, pack)
        if not os.path.exists(directory):
            os.makedirs(directory)
        try:
            cf.writeSource(sf)
        except:
            print EMPTYSTRING.join(sf.text)
            raise
        sf.dump(directory)
        return os.path.join(directory, self.name+'.java')


if __name__ == '__main__':
    pm = PythonModule("baz")
    pm.packages = ['java.lang', 'java.awt']
    pm.getIntegerConstant(22)
    pm.getStringConstant("hello world")
    pm.dump("c:\\jpython\\tools\\jpythonc2")
