# Copyright © Corporation for National Research Initiatives

from BaseEvaluator import BaseEvaluator
import jast
import ImportName

COMMASPACE = ', '

from org.python.compiler import ScopesCompiler, Future, CompilationContext
from org.python.compiler.ScopeConstants import *

import warnings
from org.python.parser import ParseException


class LocalFrame:
    def __init__(self, compiler, scope=None):
        
        self.frame = jast.Identifier("frame")

        self.compiler = compiler

	self.names = {}

        self.temporaries = {}

        self.scope = scope

        self.fast_locals = 0
        self.opt_globals = 0

    def setupClosure(self,nested_scope):
        nested_scope.setup_closure(self.scope)

    def makeClosure(self,nested_scope):
        freenames = nested_scope.freevars
        if len(freenames) == 0: return None
        clos = []
        factory = self.compiler.factory
        for free in freenames:
            i = self.scope.tbl.get(free).env_index
            code = jast.Invoke(self.frame, "getclosure", [jast.IntegerConstant(i)])
            clos.append(factory.makePyObject(code))
        return clos

    def getnames(self):
        return self.scope.names

    def args_count(self):
        if self.scope.ac:
            return len(self.scope.ac.names)
        return 0
    
    def args_arglist(self):
        return self.scope.ac and self.scope.ac.arglist

    def args_keyworddict(self):
        return self.scope.ac and self.scope.ac.keywordlist

    def getfreenames(self):
        return self.scope.freevars

    def getcellnames(self):
        return self.scope.cellvars

    def get_npurecell(self):
        return self.scope.jy_npurecell

    def toCellPrepend(self,code):
        scope = self.scope
        pre = []
        for parmcell in scope.jy_paramcells:
            syminf = scope.tbl.get(parmcell)
            args = [jast.IntegerConstant(syminf.locals_index),
                    jast.IntegerConstant(syminf.env_index)]
            pre.append(jast.Invoke(self.frame, "to_cell", args))
        if not pre: return code
        pre.append(jast.BlankLine())
        return jast.Block(jast.flatten([pre,code]))

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
        parts = temp.name.split('$')
        index = int(parts[1])
        type = parts[2]
        temps = self.gettemps(type)

        #print 'free temp', index, type, temps

        if temps[index] is None:
            raise ValueError, 'temp already freed'
        temps[index] = None

    def get_local_value(self,name):
        if self.names.has_key(name): return self.names[name]
        return None # ?? better to fail? when?
        
    def get_closure_value(self,name,up=0):
        if not up:
            syminf = self.scope.tbl.get(name)
            if syminf and syminf.flags&CELL: return self.get_local_value(name)
        return self.compiler.parent_compiler.frame.get_closure_value(name)

    def get_global_value(self,name):
        return self.compiler.top_compiler.frame.get_local_value(name)

    def get_name_value(self,name):
        if self.names.has_key(name): return self.names[name]
        return self.get_global_value(name)

    def set_global_value(self,name,value):
        self.compiler.top_compiler.frame.set_value(name,value)

    def set_value(self,name,value):
        if self.names.has_key(name):
            self.names[name] = self.names[name].mergeWith(value)
            return
        self.names[name] = value

    def delCode(self,method,ref):
        if type(ref) is type(""):
            ref = (jast.StringConstant(ref),)
        else:
            ref = (jast.IntegerConstant(ref),)
        return jast.Invoke(self.frame,method,ref)
        
    def getReference(self,value,method,ref):
        code = self.delCode(method,ref)
        if value: return value.makeReference(code)
        return self.compiler.factory.makePyObject(code)
 
    def setCode(self,method,ref,value):
        if type(ref) is type(""):
            args = (jast.StringConstant(ref),value.asAny())
        else:
            args = (jast.IntegerConstant(ref),value.asAny())
        return jast.Invoke(self.frame,method,args)

    def getglobal(self,name):
        return self.getReference(self.get_global_value(name),'getglobal',name)

    def getname(self, name):
        syminf = self.scope.tbl.get(name)
        if syminf: 
            flags = syminf.flags
            if not self.scope.nested_scopes: flags &= ~FREE
            if flags&GLOBAL or self.opt_globals and not (flags&(BOUND|CELL|FREE)):
                return self.getglobal(name)
            if self.fast_locals:
                if flags&CELL: return self.getReference(
                    self.get_closure_value(name),'getderef',syminf.env_index)
                if flags&BOUND: return self.getReference(
                    self.get_local_value(name),'getlocal',syminf.locals_index)
            if flags&FREE and not flags&BOUND: return self.getReference(
                self.get_closure_value(name,up=1),'getderef',syminf.env_index)
        return self.getReference(self.get_name_value(name),'getname',name)
            

    def delname(self, name):
        syminf = self.scope.tbl.get(name)
        if syminf and syminf.flags&GLOBAL: return self.delCode('delglobal',name)
        if not self.fast_locals: return self.delCode('dellocal',name)
        if syminf.flags&CELL: raise NameError,"can not delete variable '%s' referenced in nested scope" % name
        return self.delCode('dellocal',syminf.locals_index)

    def setname(self, name, value):
        syminf = self.scope.tbl.get(name)
        if syminf and syminf.flags&GLOBAL:
            self.set_global_value(name,value)
            return self.setCode('setglobal',name,value) 

        self.set_value(name,value)

        if not self.fast_locals: return self.setCode('setlocal',name,value)
        if syminf and syminf.flags&CELL: return self.setCode('setderef',syminf.env_index,value)
        return self.setCode('setlocal',syminf.locals_index,value)
        
    def getDeclarations(self):
        if len(self.temporaries) == 0:
            return []

        decs = [jast.SimpleComment("Temporary Variables")]
        for type, temps in self.temporaries.items():
            names = []
            for index in range(len(temps)):
                names.append("t$%d$%s" % (index, type))
            ident = "%s %s" % (type, COMMASPACE.join(names))
            decs.append(jast.Identifier(ident))
        decs.append(jast.Blank)
        return decs 



class GlobalFrame(LocalFrame):
    def __init__(self, compiler):
        LocalFrame.__init__(self, compiler)

    def setScope(self,scope): self.scope = scope

class ClassFrame(LocalFrame):
    def getnames(self):
        return []

class FunctionFrame(LocalFrame):
    def __init__(self,compiler,scope):
        LocalFrame.__init__(self,compiler,scope=scope)
        self.fast_locals = 1
        self.opt_globals = not scope.exec and not scope.from_import_star


class SimpleCompiler(BaseEvaluator, CompilationContext):
    def __init__(self, module, factory, parent=None, frameCtr=None, scope=None,
                 options=None):
        BaseEvaluator.__init__(self)

        if parent is None:
            frame = GlobalFrame(self)
            self.parent_compiler = None
            self.top_compiler = self
        else:
            frame = frameCtr(self, scope=scope)
            self.parent_compiler = parent
            self.top_compiler = parent.top_compiler

        self.frame = frame
        self.module = module
        self.nthrowables = 0
        self.factory = factory
        self.options = options
        self.listComprehensionStack = []

        self.free_decls = []

    def isAlwaysFalse(self, name):
        if self.options is None:
            return 0
        return name in self.options.falsenames

    def getFutures(self):
        return self._futures

    def getFilename(self):
        return self.module.filename

    def error(self,msg,err,node):
        if not err:
            try:
                warnings.warn_explicit(msg,SyntaxWarning,self.getFilename(),node.beginLine)
                return
            except Exception,e:
                if not isinstance(e,SyntaxWarning): raise e
        raise ParseException(msg,node)

    def parse(self, node):
        if isinstance(self.frame,GlobalFrame):
            futures = self._futures = Future()
            futures.preprocessFutures(node,None)
            ScopesCompiler(self).parse(node)
            self.frame.setScope(node.scope)
        ret = BaseEvaluator.parse(self, node)
        #print 'parse', ret
        decs = self.free_decls + self.frame.getDeclarations()
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

    def makeFreeDecl(self,type,value):
        self.free_decls.append(jast.Declare(type,value))

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

    def ellipsis_const(self):
        return self.factory.makeEllipsis()

    # builtin types
    def visitall(self, values):
        ret = []
        for value in values:
            ret.append(self.visit(value))       
        return ret

    def list_op(self, values):
        return self.factory.makeList(self.visitall(values))

    def list_comprehension(self, node):
        # Since this code generated here is placed in its own 
        # java method, we need a new set of temp vrbls.
        oldtmps = self.frame.temporaries
        self.frame.temporaries = {}

        expr = node.getChild(0)
        suite = node.getChild(1)
        lst = self.factory.makeList([])

        lsttmp, lstcode = self.makeTemp(lst)

        append = self.factory.makePyObject(jast.Invoke(lsttmp.asAny(),
                     "__getattr__", [jast.StringConstant("append")]))

        appendtmp, appendcode = self.makeTemp(append)

        self.listComprehensionStack.append((appendtmp, expr))

        stmts = [lstcode, appendcode]
        stmts.append(self.visit(suite))
        stmts.append(jast.Return(lsttmp.asAny()))

        decs = self.frame.getDeclarations()
        if len(decs) != 0:
            stmts.insert(0, decs)

        self.listComprehensionStack.pop(-1)

        idx = self.module.addFunctionCode("__listcomprehension", 
              jast.Block(stmts))

        self.freeTemp(lsttmp)
        self.freeTemp(appendtmp)

        self.frame.temporaries = oldtmps

        return self.factory.makePyObject(
                   jast.InvokeLocal("__listcomprehension$%d" % (idx+1), 
                           [jast.Identifier("frame")]))

    def list_iter(self, node):
        if node.getNumChildren() == 0:
            append, expr = self.listComprehensionStack[-1]
            return [jast.Invoke(append.asAny(), "__call__",
                                         [self.visit(expr).asAny()])]

        return self.visit(node.getChild(0))

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
        return jast.SimpleComment('global ' + COMMASPACE.join(names))

    def get_module(self, names, topmost=0):
        ret = self.factory.importName(names[0])
        top = ret

        for part in names[1:]:
            ret = ret.getattr(part)
        if topmost:
            return top
        else:
            return ret

    def importall_stmt(self, module):
        modname = jast.StringConstant(module.value.name)
        self.addModule(module.value.name, '*')
        #print 'import *', module.value.name
        self._loadNames(self._getnames(module), module)
        return jast.InvokeStatic("org.python.core.imp", "importAll",
                                 [modname, self.frame.frame])


    def _getnames(self, module):
        #print module.value, module.value.__class__
        mod = ImportName.lookupName(module.value.name)
        if mod:
            return dir(mod.mod)
        return []

    # Stolen from imp.loadNames.
    def _loadNames(self, names, module):
        for name in names:
            if name == "__all__":
                loadNames(module.getattr(name).value, module)
            elif name[:2] == "__":
                continue;
            else:
                self.set_name(name, module.getattr(name))

    def import_stmt(self, names):
        ret = []
        for dotted, asname in names:
            modnameConst = jast.StringConstant(".".join(dotted))
            if asname:
                code = jast.InvokeStatic("org.python.core.imp","importOneAs",
                                         [modnameConst, self.frame.frame])
                code = self.get_module(dotted,0).makeReference(code)
                ret.append(self.set_name(asname,code))
            else:
                code = jast.InvokeStatic("org.python.core.imp","importOne",
                                         [modnameConst, self.frame.frame])
                code = self.get_module(dotted,1).makeReference(code)
            	ret.append(self.set_name(dotted[0],code))
        return ret


    def getSlice(self, index):
        indices = self.visitor.getSlice(index)
        ret = []
        for index in indices:
            if index is None:
                ret.append(self.factory.makeNull())
            else:
                ret.append(self.visit(index))
        return ret

    def slice_op(self, s0, s1, s2):
        ret = []
        for index in (s0, s1, s2):
            if index is None:
                ret.append(self.factory.makeNull())
            else:
                ret.append(self.visit(index))
        return  self.factory.makeSlice(ret)

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
            return self.visit(value).print_line()

    def print_line_to(self, file, value=None):
        f = self.visit(file)
        if value is None:
            return f.print_line_to()
        else:
            return f.print_line_to(self.visit(value).asAny())

    def print_continued_to(self, file, value):
        f = self.visit(file)
        if value is None:
            return f.print_continued_to()
        else:
            return f.print_continued_to(self.visit(value).asAny())

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

        return jast.If(self.frame.getglobal("__debug__").nonzero(),
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

        catchBody = [setexc, catchBody]
   
        if elseClause is not None:
            catchBody = [jast.Set(elseBool, jast.False), catchBody]

        catchBody = jast.Block([catchBody])

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

    def funcdef(self, name, scope, body, doc=None):
        self.frame.setupClosure(scope)
        func = self.factory.makeFunction(name, self, scope, body, doc)
        return self.set_name(name, func)

    def lambdef(self, scope, body):
        self.frame.setupClosure(scope)
        func = self.factory.makeFunction("<lambda>", self, scope, body)
        return func

    def classdef(self, name, bases, scope, body, doc=None):
        self.frame.setupClosure(scope)
        c = self.factory.makeClass(name, bases, self, scope, body, doc)
        self.module.classes[name] = c
        return self.set_name(name, c)

    def addModule(self, mod, value=1):
        #print 'add module', mod
        if self.module.imports.has_key(mod) and value == 1:
            return
        self.module.imports[mod] = (value, self.module)

    def addSetAttribute(self, obj, name, value):
        #print ' add set attribute', name, value
        self.module.addAttribute(name, value)
