# Copyright © Corporation for National Research Initiatives

from BaseEvaluator import BaseEvaluator
from PythonVisitor import Arguments
import string
import jast


class Reference:
    def __init__(self, frame, name):
        self.iframe = frame.frame
        self.frame = frame
        self.locals = frame.locals
        self.name = name
        self.value = None
        self.init()

    def init(self): pass

    def noValue(self):
        raise NameError, 'try to get %s before set' % repr(self.name)

    def getValue(self):
        if self.value is None:
            return self.noValue()       
        return self.value

    def setValue(self, value):
        if self.value is None:
            self.value = value.makeReference(self.getCode())
        else:
            # Might want to try and merge types here...
            self.value = self.value.mergeWith(value)
            self.value.code = self.getCode()
            #PyObject(self.getCode(), None)
        return self.setCode(value)

    def delValue(self):
        #self.value = None
        return self.delCode()



class DynamicIntReference(Reference):
    def init(self):
        self.ivalue = jast.IntegerConstant(len(self.locals))

    def getCode(self):
        return jast.Invoke(self.iframe, "getlocal", (self.ivalue,))

    def setCode(self, value):
        return jast.Invoke(self.iframe, "setlocal",
                           (self.ivalue, value.asAny()))

    def delCode(self):
        return jast.Invoke(self.iframe, "dellocal", (self.ivalue,))



class DynamicStringReference(Reference):
    def init(self):
        self.ivalue = jast.StringConstant(self.name)

    def getCode(self):
        return jast.Invoke(self.iframe, "getname", (self.ivalue,))

    def delCode(self):
        return jast.Invoke(self.iframe, "delname", (self.ivalue,))

    def setCode(self, value):
        return jast.Invoke(self.iframe, "setlocal",
                           (self.ivalue, value.asAny()))



class DynamicGlobalStringReference(Reference):
    def init(self):
        self.ivalue = jast.StringConstant(self.name)

    def getCode(self):
        return jast.Invoke(self.iframe, "getglobal", (self.ivalue,))

    def delCode(self):
        return jast.Invoke(self.iframe, "delglobal", (self.ivalue,))

    def setCode(self, value):
        return jast.Invoke(self.iframe, "setglobal",
                           (self.ivalue, value.asAny()))

    def noValue(self):
        # Reference to builtin
        return self.frame.parent.factory.makePyObject(self.getCode())



class LocalFrame:
    def __init__(self, parent, newReference=DynamicIntReference):
        self.frame = jast.Identifier("frame")
        self.globalNamespace = parent.globalNamespace
        self.parent = parent
        self.newReference = newReference

        self.names = {}
        self.globals = {}
        self.locals = []

        self.temporaries = {}

    def gettemps(self, type):
        try:
            temps = self.temporaries[type]
        except KeyError:
            temps = []
            self.temporaries[type] = temps
        return temps            

    def gettemp(self, type):
        temps = self.gettemps(type)

        index = 0
        while index < len(temps):
            if temps[index] is None:
                break
            index = index + 1
        if index == len(temps):
            temps.append(None)

        tname = "t$%d$%s" % (index, type)
        temp = jast.Identifier(tname)
        temps[index] = temp
        #print 'get temp', index, type, temps
        return temp

    def freetemp(self, temp):
        index = int(string.split(temp.name, '$')[1])
        type = string.split(temp.name, '$')[2]
        temps = self.gettemps(type)

        #print 'free temp', index, type, temps

        if temps[index] is None:
            raise ValueError, 'temp already freed'
        temps[index] = None

    def getname(self, name):
        if not self.names.has_key(name):
            return self.globalNamespace.getname(self, name)
        ref = self.getReference(name)
        return ref.getValue()

    def delname(self, name):
        if self.globals.has_key(name):
            return self.globalNamespace.delname(self, name)
        ref = self.getReference(name)
        return ref.delValue()

    def setname(self, name, value):
        if self.globals.has_key(name):
            return self.globalNamespace.setname(self, name, value)
        ref = self.getReference(name)
        return ref.setValue(value)

    def addglobal(self, name):
        self.globals[name] = 1

    def addlocal(self, name):
        self.getReference(name)

    def getlocals(self):
        return self.locals

    def getReference(self, name):
        if self.names.has_key(name):
            return self.names[name]
        ret = self.newReference(self, name)
        self.names[name] = ret
        self.locals.append(name)
        return ret

    def getDeclarations(self):
        if len(self.temporaries) == 0:
            return []

        decs = [jast.SimpleComment("Temporary Variables")]
        for type, temps in self.temporaries.items():
            names = []
            for index in range(len(temps)):
                names.append("t$%d$%s" % (index, type))
            ident = "%s %s" % (type, string.join(names, ', '))
            decs.append(jast.Identifier(ident))
        decs.append(jast.Blank)
        return decs 



class GlobalFrame(LocalFrame):
    def __init__(self, parent):
        LocalFrame.__init__(self, parent)

    def getReference(self, name):
        return self.globalNamespace.getReference(self, name)



class BasicGlobals:
    def __init__(self, parent, newReference=DynamicGlobalStringReference):
        self.names = {}
        self.newReference = newReference
        self.parent = parent

    def delname(self, frame, name):
        ref = self.getReference(frame, name)
        return ref.delValue()           

    def getname(self, frame, name):
        ref = self.getReference(frame, name)
        return ref.getValue()

    def setname(self, frame, name, value):
        ref = self.getReference(frame, name)
        return ref.setValue(value)

    def getReference(self, frame, name):
        if self.names.has_key(name):
            return self.names[name]
        ret = self.newReference(frame, name)
        self.names[name] = ret
        return ret



class SimpleCompiler(BaseEvaluator):
    def __init__(self, module, factory, frame=None, options=None):
        BaseEvaluator.__init__(self)
        self.globalNamespace = BasicGlobals(self)
        if frame is None:
            frame = GlobalFrame(self)
        self.frame = frame
        self.module = module
        self.nthrowables = 0
        self.factory = factory
        self.options = options

    def isAlwaysFalse(self, name):
        if self.options is None:
            return 0
        return name in self.options.falsenames

    def parse(self, node):
        ret = BaseEvaluator.parse(self, node)
        #print 'parse', ret
        decs = self.frame.getDeclarations()
        if len(decs) != 0:
            return [decs, jast.SimpleComment('Code'), ret]
        else:
            return ret

    def makeTemp(self, value):
        tmp = self.frame.gettemp('PyObject')
        setit = jast.Set(tmp, value.asAny())
        return self.factory.makePyObject(tmp), setit

    def freeTemp(self, tmp):
        self.frame.freetemp(tmp.asAny())

    #primitive values
    def int_const(self, value):
        return self.factory.makeInteger(value)

    def long_const(self, value):
        return self.factory.makeLong(value)

    def complex_const(self, value):
        return self.factory.makeImaginary(value)

    def float_const(self, value):
        return self.factory.makeFloat(value)

    def string_const(self, value):
        return self.factory.makeString(value)

    # builtin types
    def visitall(self, values):
        ret = []
        for value in values:
            ret.append(self.visit(value))       
        return ret

    def list_op(self, values):
        return self.factory.makeList(self.visitall(values))

    def tuple_op(self, values):
        return self.factory.makeTuple(self.visitall(values))

    def dictionary_op(self, items):
        lst = []
        for key, value in items:
            lst.append( (self.visit(key), self.visit(value)) )
        return self.factory.makeDictionary(lst)

    #namespaces
    def set_name(self, name, value):
        return self.frame.setname(name, value)

    def del_name(self, name):
        return self.frame.delname(name)

    def name_const(self, name):
        return self.frame.getname(name)

    def global_stmt(self, names):
        for name in names:
            self.frame.addglobal(name)
        return jast.SimpleComment('global '+string.join(names, ','))

    def get_module(self, names, top=0):
        ret = self.factory.importName(names[0])
        top = ret

        for part in names[1:]:
            top = top.getattr(part)
        if top:
            return top
        else:
            return ret

    def importall_stmt(self, module):
        modname = jast.StringConstant(module.value.name)
        self.addModule(module.value.name, '*')
        #print 'import *', module.value.name
        return jast.InvokeStatic("imp", "importAll",
                                 [modname, self.frame.frame])

    def getSlice(self, index):
        indices = self.visitor.getSlice(index)
        ret = []
        for index in indices:
            if index is None:
                ret.append(self.factory.makeNull())
            else:
                ret.append(self.visit(index))
        return ret


    def bool_op(self, x, y, swap=0):
        tmp = self.frame.gettemp("PyObject")
        test = jast.Invoke(jast.Set(tmp, self.visit(x).asAny()),
                           "__nonzero__", [])
        yes, no = tmp, self.visit(y).asAny()
        if swap:
            yes, no = no, yes

        op = self.factory.makePyObject(jast.TriTest(test, yes, no))
        self.frame.freetemp(tmp)
        return op

    def and_op(self, x, y):
        return self.bool_op(x, y, 1)

    def or_op(self, x, y):
        return self.bool_op(x, y, 0)

    #flow control
    def do_comp(self, x, compares, tmps):
        False = jast.GetStaticAttribute("Py", "Zero")

        op, other = compares[0]
        y = self.visit(other)
        if len(compares) > 1:
            ytmp = self.frame.gettemp("PyObject")
            tmps.append(ytmp)
            gety = self.factory.makePyObject(jast.Set(ytmp, y.asAny()))
        else:
            gety = y

        test = x.compop(op, gety)

        if len(compares) == 1:
            return test.asAny()

        rest = self.do_comp(self.factory.makePyObject(ytmp),
                            compares[1:],
                            tmps)
        return jast.TriTest(test.nonzero(), rest, False)

    def compare_op(self, start, compares):
        x = self.visit(start)

        tmps = []
        ret = self.do_comp(x, compares, tmps)
        for tmp in tmps:
            self.frame.freetemp(tmp)
        return self.factory.makePyObject(ret)

    def print_line(self, value=None):
        if value is None:
            return jast.InvokeStatic("Py", "println", [])
        else: 
            v = self.visit(value)
            #print v, v.print_line
            return v.print_line()

    def pass_stmt(self):
        return jast.SimpleComment("pass")

    def continue_stmt(self):
        return jast.Continue()

    def break_stmt(self):
        return jast.Break()

    def exec_stmt(self, code, globs=None, locs=None):
        if globs is None:
            globCode = jast.Null
        else:
            globCode = globs.asAny()

        if locs is None:
            locCode = jast.Null
        else:
            locCode = locs.asAny()

        return jast.InvokeStatic("Py", "exec",
                                 [code.asAny(), globCode, locCode])

    def assert_stmt(self, test, message=None):
        if self.isAlwaysFalse("__debug__"):
            return jast.SimpleComment("assert")

        args = [test.asAny()]
        if message is not None:
            args.append(message.asAny())

        return jast.If(self.name_const("__debug__").nonzero(),
                       jast.InvokeStatic("Py", "assert", args))

    def return_stmt(self, value=None):
        if value is None:
            return jast.Return(jast.GetStaticAttribute("Py", "None"))
        else:
            return jast.Return(self.visit(value).asAny())

    def raise_stmt(self, exc_type=None, exc_value=None, exc_traceback=None):
        if exc_type is None:
            return jast.Throw(jast.InvokeStatic("Py", "makeException", []))
        return exc_type.doraise(exc_value, exc_traceback)

    def while_stmt(self, test, body, else_body=None):
        stest = self.visit(test).nonzero()
        sbody = jast.Block(self.visit(body))
        if else_body is not None:
            else_body = jast.Block(self.visit(else_body))
            wtmp = self.frame.gettemp('boolean')
            ret = jast.WhileElse(stest, sbody, else_body, wtmp)
            self.frame.freetemp(wtmp)
            return ret
        else:
            return jast.While(stest, sbody)

    def if_stmt(self, tests, else_body=None):
        jtests = []
        for test, body in tests:
            tname = self.getName(test)
            if tname is not None and self.isAlwaysFalse(tname):
                continue
            test = self.visit(test).nonzero()
            body = jast.Block(self.visit(body))
            jtests.append( (test, body) )

        if else_body is not None:
            else_body = jast.Block(self.visit(else_body))

        if len(jtests) == 0:
            if else_body is None:
                return jast.SimpleComment("if "+tname)
            else:
                return else_body

        if len(jtests) == 1:
            return jast.If(jtests[0][0], jtests[0][1], else_body)
        else:
            return jast.MultiIf(jtests, else_body)

    def tryfinally(self, body, finalbody):
        return jast.TryFinally(jast.Block(self.visit(body)),
                               jast.Block(self.visit(finalbody)))

    def tryexcept(self, body, exceptions, elseClause=None):
        if elseClause is not None:
            elseBool = self.frame.gettemp("boolean")

        jbody = jast.Block(self.visit(body))
        tests = []
        ifelse = None

        tname = jast.Identifier("x$%d" % self.nthrowables)
        self.nthrowables = self.nthrowables + 1

        exctmp = self.frame.gettemp("PyException")
        setexc = jast.Set(exctmp, jast.InvokeStatic("Py", "setException",
                                                    [tname, self.frame.frame]))

        for exc, ebody in exceptions:
            if exc is None:
                ifelse = jast.Block(self.visit(ebody))
                continue

            t = jast.InvokeStatic("Py", "matchException",
                                  [exctmp, exc[0].asAny()])
            newbody = []
            if elseClause is not None:
                newbody.append(jast.Set(elseBool, jast.False))

            if len(exc) == 2:
                exceptionValue = self.factory.makePyObject(
                    jast.GetInstanceAttribute(exctmp, "value"))
                #print exc[1], exceptionValue
                newbody.append(self.set(exc[1], exceptionValue))
                #print newbody

            #print self.visit(ebody)
            newbody.append(self.visit(ebody))
            #print newbody
            #print jast.Block(newbody)
            tests.append( (t, jast.Block(newbody)) )

        if ifelse is None:
            ifelse = jast.Throw(exctmp)

        if len(tests) == 0:
            catchBody = ifelse
        else:
            catchBody = jast.MultiIf(tests, ifelse)

        catchBody = jast.Block([setexc, catchBody])

        self.frame.freetemp(exctmp)

        ret = jast.TryCatch(jbody, "Throwable", tname, catchBody)

        if elseClause is not None:
            ret = jast.Block([jast.Set(elseBool, jast.True), ret, 
                              jast.If(elseBool,
                                      jast.Block(self.visit(elseClause)))])
            self.frame.freetemp(elseBool)

        return ret

    def for_stmt(self, index, sequence, body, else_body=None):
        counter = self.frame.gettemp('int')
        item = self.factory.makePyObject(self.frame.gettemp("PyObject"))
        seq = self.frame.gettemp("PyObject")

        init = []
        init.append( jast.Set(counter, jast.IntegerConstant(0)) )
        init.append( jast.Set(seq, self.visit(sequence).asAny()) )

        counter_inc = jast.PostOperation(counter, '++')

        test = jast.Set(item.asAny(), jast.Invoke(seq, "__finditem__",
                                                  [counter_inc]))
        test = jast.Operation('!=', test, jast.Identifier('null'))

        suite = []
        suite.append(self.set(index, item))
        suite.append(self.visit(body))
        suite = jast.Block(suite)

        if else_body is not None:
            else_body = jast.Block(self.visit(else_body))
            wtmp = self.frame.gettemp('boolean')
            ret = [init, jast.WhileElse(test, suite, else_body, wtmp)]
            self.frame.freetemp(wtmp)
            return ret
        else:
            return [init, jast.While(test, suite)]

    def funcdef(self, name, args, body, doc=None):
        func = self.factory.makeFunction(name, args, body, doc)
        return self.set_name(name, func)

    def lambdef(self, args, body):
        func = self.factory.makeFunction("<lambda>", args, body)
        return func

    def classdef(self, name, bases, body, doc=None):
        c = self.factory.makeClass(name, bases, body, doc)
        self.module.classes[name] = c
        return self.set_name(name, c)

    def addModule(self, mod, value=1):
        #print 'add module', mod.name, mod
        self.module.imports[mod] = value

    def addSetAttribute(self, obj, name, value):
        #print ' add set attribute', name, value
        self.module.addAttribute(name, value)
