# Copyright (c) Corporation for National Research Initiatives

import jast
import ImportName

COMMASPACE = ', '

from org.python.compiler import ScopesCompiler, Future, CompilationContext
from org.python.compiler.ScopeConstants import *
from org.python.parser import ast

import warnings
from org.python.parser import ParseException, Visitor

def getDocString(suite):
    if len(suite) > 0:
        n = suite[0]
        if isinstance(n, ast.Expr) and isinstance(n.value, ast.Str):
            return n.value.s
    return None


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

        tname = "t$%d$%s" % (index, type.replace("[]", "__"))
        temp = jast.Identifier(tname)
        temp.type = type
        temps[index] = temp
        #print 'get temp', index, type, temps
        return temp

    def freetemp(self, temp):
        parts = temp.name.split('$')
        index = int(parts[1])
        type = temp.type
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
                names.append("t$%d$%s" % (index, type.replace("[]", "__")))
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


class SrcGenCompiler(Visitor, CompilationContext):
    def __init__(self, module, factory, parent=None, frameCtr=None, scope=None,
                 options=None, className=None):
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
        self.className = className
        self.listComprehensionStack = []
        self.list_comprehension_count = 0

        self.free_decls = []
        import java
        self.scopes = java.util.Hashtable()
        self.lineno = 0

    def isAlwaysFalse(self, name):
        if self.options is None:
            return 0
        return name in self.options.falsenames

    def getFutures(self):
        return self.module.futures

    def getFilename(self):
        return self.module.filename

    def getScopeInfo(self, node):
        return self.top_compiler.scopes.get(node)

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
            self.getFutures().preprocessFutures(node,None)
            ScopesCompiler(self, self.scopes).parse(node)
            self.frame.setScope(self.getScopeInfo(node))
        try:
            ret = self.visit(node)
        except:
            #print 'Parsing line: %d' % self.lineno
            if hasattr(self, 'data') and self.lineno > 0:
                print self.data.split('\n')[self.lineno-1]
            raise

        #print 'parse', ret
        decs = self.free_decls + self.frame.getDeclarations()
        if len(decs) != 0:
            return [decs, jast.SimpleComment('Code'), ret]
        else:
            return ret

    def suite(self, nodes):
        return [self.visit(node) for node in nodes]

    deflt = "$$$"
    def eval(self, node, default=deflt):
        if node == None:
            if default == self.deflt:
                return self.factory.makePyNone()
            return default
        return self.visit(node)

    def visitnames(self, kws):
        return [ (kw.arg, self.visit(kw.value)) for kw in kws]

    def visitall(self, args):
        return [ self.visit(arg) for arg in args ]

    def setAugTmps(self, *args):
        self.augtmps = []
        for var in args:
            tmp, code = self.makeTemp(var)
            self.augstmts.append(code)
            self.augtmps.append(tmp)
        return self.augtmps

    def getAugTmps(self):
        tmps = self.augtmps
        for var in tmps:
            self.freeTemp(var)
        return tmps

    def visitModule(self, node):
        return self.suite(node.body)

    def visitSuite(self, node):
        return self.suite(node.body)

    def makeTemp(self, value):
        tmp = self.frame.gettemp('PyObject')
        setit = jast.Set(tmp, value.asAny())
        return self.factory.makePyObject(tmp), setit

    def freeTemp(self, tmp):
        self.frame.freetemp(tmp.asAny())

    def makeFreeDecl(self,type,value):
        self.free_decls.append(jast.Declare(type,value))

    def getName(self, s, fast_locals=0):
        if fast_locals:
            return s
        if s[:2] == '__' and s[-2:] != '__' and self.className:
            s = "_%s%s" % (self.className, s)
        return s
        
    def set(self, node, value):
        self.temporary = value
        return self.visit(node)

    #primitive values
    def visitNum(self, node):
        n = node.n
        if isinstance(n, type(0)):
            return self.factory.makeInteger(n)
        elif isinstance(n, type(0.0)):
            return self.factory.makeFloat(n)
        elif isinstance(n, type(0L)):
            return self.factory.makeLong(n)
        elif isinstance(n, type(0j)):
            return self.factory.makeImaginary(str(n)[:-1])

    def visitStr(self, node):
        return self.factory.makeString(node.s)

    def visitEllipsis(self, node):
        return self.factory.makeEllipsis()

    def visitName(self, node):
        s = self.getName(node.id, self.frame.fast_locals)
        ctx = node.ctx
        if ctx == node.AugStore:
            ctx = self.augmode
        if ctx == node.Load:
            return self.frame.getname(s)
        elif ctx == node.Store:
            return self.frame.setname(s, self.temporary)
        elif ctx == node.Del:
            return self.frame.delname(s)

        
    # builtin types
    def visitall(self, values):
        ret = []
        for value in values:
            ret.append(self.visit(value))       
        return ret

    def visitList(self, node):
        if node.ctx == node.Store:
            return self.seqSet(node.elts)
        elif node.ctx == node.Del:
            return self.visitall(node.elts)
        return self.factory.makeList(self.visitall(node.elts))

    def visitListComp(self, node):
        # Since this code generated here is placed in its own 
        # java method, we need a new set of temp vrbls.
        oldtmps = self.frame.temporaries
        self.frame.temporaries = {}

        lst = self.factory.makeList([])

        lsttmp, lstcode = self.makeTemp(lst)

        append = self.factory.makePyObject(jast.Invoke(lsttmp.asAny(),
                     "__getattr__", [jast.StringConstant("append")]))

        appendtmp, appendcode = self.makeTemp(append)

        self.list_comprehension_count += 1
        tmp_append = "_[%d]" % self.list_comprehension_count
        #tmp_append = "_[1]"


        n = ast.Expr(ast.Call(ast.Name(tmp_append, ast.Name.Load, node), 
                                       [ node.target ], [], None, None, node),
                                            node);

        for lc in node.generators[::-1]:
            for ifs in lc.ifs[::-1]:
                n = ast.If(ifs, [ n ], None, ifs);
            n = ast.For(lc.target, lc.iter, [ n ], None, lc);
        #visit(new Delete(new exprType[] { new Name(tmp_append, Name.Del) }));

        stmts = [ lstcode ]
        stmts.append(self.set_name(tmp_append, append))
        #stmts.append(appendcode)
        stmts.append(self.visit(n))
        stmts.append(jast.Return(lsttmp.asAny()))

        decs = self.frame.getDeclarations()
        if len(decs) != 0:
            stmts.insert(0, decs)

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

    def seqSet(self, elts):
        n = len(elts)
        unpacked = jast.InvokeStatic("org.python.core.Py", "unpackSequence",
                           [self.temporary.asAny(), jast.IntegerConstant(n)])
        tmp = self.frame.gettemp('PyObject[]')
        stmts = [ jast.Set(tmp, unpacked) ]
        
        for i in range(n):
            code = jast.Subscript(tmp, i)
            stmts.append(self.set(elts[i], self.factory.makePyObject(code)))
        self.frame.freetemp(tmp)
        return stmts
        
    def visitTuple(self, node):
        if node.ctx == node.Store:
            return self.seqSet(node.elts)
        elif node.ctx == node.Del:
            return self.visitall(node.elts)
        return self.factory.makeTuple(self.visitall(node.elts))

    def visitDict(self, node):
        lst = []
        for key, value in zip(node.keys, node.values):
            lst.append( (self.visit(key), self.visit(value)) )
        return self.factory.makeDictionary(lst)

    def visitCall(self, node):
        if node.starargs is not None or node.kwargs is not None:
            starargs = self.eval(node.starargs)
            kwargs = self.eval(node.kwargs)
            return self.visit(node.func).call_extra(self.visitall(node.args),
                                            self.visitnames(node.keywords),
                                            starargs, kwargs)

        if isinstance(node.func, ast.Attribute):
            return self.visit(node.func.value).invoke(
                                      self.getName(node.func.attr),
                                      self.visitall(node.args),
                                      self.visitnames(node.keywords))
            
        return self.visit(node.func).call(self.visitall(node.args),
                                       self.visitnames(node.keywords))


    def visitExpr(self, node):
        val = self.visit(node.value);
        stmt = val.makeStatement()
        if isinstance(stmt, jast.TriTest):
            tmp, code = self.makeTemp(val)
            self.freeTemp(tmp)
            return code
        return stmt

    def visitAssign(self, node):
        if len(node.targets) == 1:
            self.temporary = self.visit(node.value)
            return self.visit(node.targets[0])

        self.temporary, code = self.makeTemp(self.visit(node.value))
        stmts = [code]
        for lhs in node.targets:
            stmts.append(self.visit(lhs))
        self.freeTemp(self.temporary)
        return stmts

    def visitAugAssign(self, node):
        value = self.visit(node.value)
        ops = {
            ast.AugAssign.Add      : "__iadd__",
            ast.AugAssign.Sub      : "__isub__",
            ast.AugAssign.Mult     : "__imul__",
            ast.AugAssign.Div      : "__idiv__",
            ast.AugAssign.Mod      : "__imod__",
            ast.AugAssign.Pow      : "__ipow__",
            ast.AugAssign.LShift   : "__ilshift__",
            ast.AugAssign.RShift   : "__irshift__",
            ast.AugAssign.BitOr    : "__ior__",
            ast.AugAssign.BitXor   : "__ixor__",
            ast.AugAssign.BitAnd   : "__iand__",
            ast.AugAssign.FloorDiv : "__ifloordiv__",
        }
        name = ops[node.op]
        if node.op == ast.AugAssign.Div and self.getFutures().areDivisionOn():
            name = "__itruediv__";
        self.augstmts = []
        self.augmode = ast.expr_contextType.Load
        self.temporary = self.visit(node.target).augbinop(name, value)
        self.augmode = ast.expr_contextType.Store
        self.augstmts.append(self.visit(node.target))
        return self.augstmts

    #namespaces
    def set_name(self, name, value):
        return self.frame.setname(name, value)

    def del_name(self, name):
        return self.frame.delname(name)

    def name_const(self, name):
        return self.frame.getname(name)

    def visitGlobal(self, node):
        return jast.SimpleComment('global ' + COMMASPACE.join(node.names))

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

    def visitImportFrom(self, node):
        modname = jast.StringConstant(node.module)
        module = self.get_module(node.module.split('.'), 0)
        if len(node.names) == 0:
            self.addModule(node.module, '*')
            self._loadNames(self._getnames(module), module)
            return jast.InvokeStatic("org.python.core.imp", "importAll",
                                    [modname, self.frame.frame])

        topmodname = jast.StringConstant(node.module)
        modnames = [alias.name for alias in node.names]
        modnamesArray = jast.FilledArray(
            "String",
            map(lambda x: jast.StringConstant(x), modnames))

        do_import = jast.InvokeStatic("org.python.core.imp", "importFrom",
                                      [topmodname, modnamesArray,
                                       self.frame.frame])

        imp_accu = self.frame.gettemp('PyObject[]')
        stmts = [ jast.Set(imp_accu, do_import) ]
        
        for i in range(len(node.names)):
            modname = node.names[i].name
            asname = node.names[i].asname
            if asname is None:
                asname = modname
            code = jast.Subscript(imp_accu, i)
            stmts.append(self.set_name(asname, 
         	module.getattr(modname).makeReference(code)))

        stmts.append(jast.Set(imp_accu,jast.Null))
        self.frame.freetemp(imp_accu)
        return stmts       

        
    def visitImport(self, node):
        ret = []
        for alias in node.names:
            modnameConst = jast.StringConstant(alias.name)
            dotted = alias.name.split('.')
            if alias.asname:
                code = jast.InvokeStatic("org.python.core.imp","importOneAs",
                                         [modnameConst, self.frame.frame])
                code = self.get_module(dotted, 0).makeReference(code)
                ret.append(self.set_name(alias.asname, code))
            else:
                code = jast.InvokeStatic("org.python.core.imp","importOne",
                                         [modnameConst, self.frame.frame])
                code = self.get_module(dotted, 1).makeReference(code)
            	ret.append(self.set_name(dotted[0], code))
        return ret

    def visitAttribute(self, node):
        ctx = node.ctx
        if ctx == node.AugStore and self.augmode == node.Store:
            o, = self.getAugTmps()
            attr = self.augtmps2
            ctx = node.Store
        else:
            o = self.visit(node.value)
            attr = self.getName(node.attr)
            if ctx == node.AugStore and self.augmode == node.Load:
                o, = self.setAugTmps(o)
                self.augtmps2 = attr
                ctx = node.Load

        if ctx == node.Load:
            return o.getattr(attr)
        elif ctx == node.Store:
            return o.setattr(attr, self.temporary)
        elif ctx == node.Del:
            return o.delattr(attr)
        
    def doSlice(self, node, slice):
        ctx = node.ctx
        if ctx == node.AugStore and self.augmode == node.Store:
            o, start, stop, step = self.getAugTmps()
            ctx = node.Store
        else:
            o = self.visit(node.value)
            start, stop, step = self.getSlice(slice)
            if ctx == node.AugStore and self.augmode == node.Load:
                o, start, stop, step = self.setAugTmps(o, start, stop, step)
                ctx = node.Load
        if ctx == node.Load:
            return o.getslice(start, stop, step)        
        elif ctx == node.Store:
            return o.setslice(start, stop, step, self.temporary)
        elif ctx == node.Del:
            return o.delslice(start, stop, step)        

    def visitSubscript(self, node):
        if isinstance(node.slice, ast.Slice):
            return self.doSlice(node, node.slice);
        ctx = node.ctx
        if ctx == node.AugStore and self.augmode == node.Store:
            o, idx = self.getAugTmps()
            ctx = node.Store
        else:
            o = self.visit(node.value)
            idx = self.visit(node.slice)
            if node.ctx == node.AugStore and self.augmode == node.Load:
                o, idx = self.setAugTmps(o, idx)
                ctx = node.Load
        if ctx == node.Load:
            return o.getitem(idx)
        elif ctx == node.Store:
            return o.setitem(idx, self.temporary)
        elif ctx == node.Del:
            return o.delitem(idx)        
                            
    def getSlice(self, node):
        ret = []
        for index in (node.lower, node.upper, node.step):
            if index is None:
                ret.append(self.factory.makeNull())
            else:
                ret.append(self.visit(index))
        return ret

    def visitSlice(self, node):
        return self.factory.makeSlice(self.getSlice(node))

    def visitIndex(self, node):
        return self.visit(node.value)

    def visitExtSlice(self, node):
        return self.factory.makeTuple(self.visitall(node.dims))

    def visitBinOp(self, node):
        ops = {
            ast.BinOp.Add      : "_add",
            ast.BinOp.Sub      : "_sub",
            ast.BinOp.Mult     : "_mul",
            ast.BinOp.Div      : "_div",
            ast.BinOp.Mod      : "_mod",
            ast.BinOp.Pow      : "_pow",
            ast.BinOp.LShift   : "_lshift",
            ast.BinOp.RShift   : "_rshift",
            ast.BinOp.BitOr    : "_or",
            ast.BinOp.BitXor   : "_xor",
            ast.BinOp.BitAnd   : "_and",
            ast.BinOp.FloorDiv : "_floordiv",
        }
        name = ops[node.op]
        if node.op == ast.BinOp.Div and self.getFutures().areDivisionOn():
            name = "_truediv";
        return self.visit(node.left).binop(name, self.visit(node.right))

    def visitUnaryOp(self, node):
        ops = {
            ast.UnaryOp.Invert : "__invert__",
            ast.UnaryOp.Not    : "__not__",
            ast.UnaryOp.UAdd   : "__pos__",
            ast.UnaryOp.USub   : "__neg__",
        }
        return self.visit(node.operand).unop(ops[node.op])

    def visitBoolOp(self, node):
        left = self.visit(node.values[0])
        for i in range(1, len(node.values)):
            left = self.bool_op(left, self.visit(node.values[i]), node.op)
        return left    

    def bool_op(self, x, y, op):
        tmp = self.frame.gettemp("PyObject")
        test = jast.Invoke(jast.Set(tmp, x.asAny()),
                           "__nonzero__", [])
        if op == ast.BoolOp.Or:
            yes, no = tmp, y.asAny()
        else:
            no, yes = tmp, y.asAny()

        op = self.factory.makePyObject(jast.TriTest(test, yes, no))
        self.frame.freetemp(tmp)
        return op

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

        ops = {
            ast.Compare.Eq:    "_eq",
            ast.Compare.NotEq: "_ne",
            ast.Compare.Lt:    "_lt",
            ast.Compare.LtE:   "_le",
            ast.Compare.Gt:    "_gt",
            ast.Compare.GtE:   "_ge",
            ast.Compare.Is:    "_is",
            ast.Compare.IsNot: "_isnot",
            ast.Compare.In:    "_in",
            ast.Compare.NotIn: "_notin",
        }

        test = x.compop(ops[op], gety)

        if len(compares) == 1:
            return test.asAny()

        rest = self.do_comp(self.factory.makePyObject(ytmp),
                            compares[1:],
                            tmps)
        return jast.TriTest(test.nonzero(), rest, False)

    def visitCompare(self, node):
        x = self.visit(node.left)

        tmps = []
        ret = self.do_comp(x, zip(node.ops, node.comparators), tmps)
        for tmp in tmps:
            self.frame.freetemp(tmp)
        return self.factory.makePyObject(ret)

    def visitRepr(self, node):
        return self.visit(node.value).unop('__repr__')

    def visitPrint(self, node):
        self.setline(node)
        dest = self.factory.makePyNone()

        if node.dest:
            dest = self.visit(node.dest)

        stmts = []
        if node.value:
            for i, v in zip(range(len(node.value)), node.value):
                v = self.visit(v)
                if node.nl and i == len(node.value) - 1:
                    stmts.append(jast.InvokeStatic("Py", "println", [
                                    dest.asAny(), v.asAny()]))
                else:
                    stmts.append(jast.InvokeStatic("Py", "printComma", [
                                    dest.asAny(), v.asAny()]))
        else:
            stmts.append(jast.InvokeStatic("Py", "printlnv", [dest.asAny()]))
        return stmts;
        
    def visitDelete(self, node):
        return [ self.visit(node) for node in node.targets]

    def visitPass(self, node):
        return jast.SimpleComment("pass")

    def visitContinue(self, node):
        return jast.Continue()

    def visitBreak(self, node):
        return jast.Break()

    def visitExec(self, node):
        code = self.visit(node.body).asAny()
        if node.globals is None:
            globCode = jast.Null
        else:
            globCode = self.visit(node.globals).asAny()

        if node.locals is None:
            locCode = jast.Null
        else:
            locCode = self.visit(node.locals).asAny()

        return jast.InvokeStatic("Py", "exec", [code, globCode, locCode])

    def visitAssert(self, node):
        if self.isAlwaysFalse("__debug__"):
            return jast.SimpleComment("assert")

        args = [self.visit(node.test).asAny()]
        if node.msg is not None:
            args.append(self.visit(node.msg).asAny())

        return jast.If(self.frame.getglobal("__debug__").nonzero(),
                       jast.InvokeStatic("Py", "assert", args))

    def visitReturn(self, node):
        if node.value is None:
            return jast.Return(jast.GetStaticAttribute("Py", "None"))
        else:
            return jast.Return(self.visit(node.value).asAny())

    def visitRaise(self, node):
        if node.type is None:
            return jast.Throw(jast.InvokeStatic("Py", "makeException", []))
        type = self.visit(node.type)
        inst = self.eval(node.inst, None)
        tback = self.eval(node.tback, None)
        return type.doraise(inst, tback)

    def visitWhile(self, node):
        stest = self.visit(node.test).nonzero()
        sbody = jast.Block(self.suite(node.body))
        if node.orelse is not None:
            orelse = jast.Block(self.suite(node.orelse))
            wtmp = self.frame.gettemp('boolean')
            ret = jast.WhileElse(stest, sbody, orelse, wtmp)
            self.frame.freetemp(wtmp)
            return ret
        else:
            return jast.While(stest, sbody)

    def visitIf(self, node):
        test = self.visit(node.test).nonzero()
        body = jast.Block(self.suite(node.body))

        orelse = None
        if node.orelse is not None:
            orelse = jast.Block(self.suite(node.orelse))

        if isinstance(node.test, ast.Name):
            tname = self.getName(node.test.id)
            if tname is not None and self.isAlwaysFalse(tname):
                if node.orelse is None:
                    return jast.SimpleComment("if "+tname)
                else:
                    return orelse

        return jast.If(test, body, orelse)

    def visitTryFinally(self, node):
        return jast.TryFinally(jast.Block(self.suite(node.body)),
                               jast.Block(self.suite(node.finalbody)))

    def visitTryExcept(self, node):
        if node.orelse is not None:
            elseBool = self.frame.gettemp("boolean")

        jbody = jast.Block(self.suite(node.body))
        tests = []
        ifelse = None

        tname = jast.Identifier("x$%d" % self.nthrowables)
        self.nthrowables = self.nthrowables + 1

        exctmp = self.frame.gettemp("PyException")
        setexc = jast.Set(exctmp, jast.InvokeStatic("Py", "setException",
                                                    [tname, self.frame.frame]))

        for exchandler in node.handlers:
            if exchandler.type is None:
                ifelse = jast.Block(self.suite(exchandler.body))
                continue

            type = self.visit(exchandler.type)
            t = jast.InvokeStatic("Py", "matchException",
                                  [exctmp, type.asAny()])
            newbody = []

            if exchandler.name is not None:
                exceptionValue = self.factory.makePyObject(
                    jast.GetInstanceAttribute(exctmp, "value"))
                newbody.append(self.set(exchandler.name, exceptionValue))

            #print self.visit(ebody)
            newbody.append(self.suite(exchandler.body))
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
   
        if node.orelse is not None:
            catchBody = [jast.Set(elseBool, jast.False), catchBody]

        catchBody = jast.Block([catchBody])

        self.frame.freetemp(exctmp)

        ret = jast.TryCatch(jbody, "Throwable", tname, catchBody)

        if node.orelse is not None:
            ret = jast.Block([jast.Set(elseBool, jast.True), ret, 
                              jast.If(elseBool,
                                      jast.Block(self.suite(node.orelse)))])
            self.frame.freetemp(elseBool)

        return ret

    def visitFor(self, node):
        iter = self.frame.gettemp('PyObject')
        item = self.factory.makePyObject(self.frame.gettemp("PyObject"))
        seq = self.visit(node.iter).asAny() 

        init = []
        init.append(jast.Set(iter, jast.Invoke(seq, "__iter__", [])))

        test = jast.Set(item.asAny(), jast.Invoke(iter, "__iternext__", []))
        test = jast.Operation('!=', test, jast.Identifier('null'))

        suite = []
        suite.append(self.set(node.target, item))
        suite.append(self.suite(node.body))
        suite = jast.Block(suite)

        if node.orelse is not None:
            orelse = jast.Block(self.suite(node.orelse))
            wtmp = self.frame.gettemp('boolean')
            ret = [init, jast.WhileElse(test, suite, orelse, wtmp)]
            self.frame.freetemp(wtmp)
            return ret
        else:
            return [init, jast.While(test, suite)]

    def visitFunctionDef(self, node):
        scope = self.getScopeInfo(node)
        self.frame.setupClosure(scope)
        doc = getDocString(node.body)
        func = self.factory.makeFunction(node.name, self, scope, node.body, doc)
        return self.set_name(self.getName(node.name), func)

    def visitLambda(self, node):
        scope = self.getScopeInfo(node)
        self.frame.setupClosure(scope)
        body = [ast.Return(node.body)]
        func = self.factory.makeFunction("<lambda>", self, scope, body)
        return func

    def visitClassDef(self, node):
        scope = self.getScopeInfo(node)
        self.frame.setupClosure(scope)
        bases = [self.visit(x) for x in node.bases]
        doc = getDocString(node.body)
        c = self.factory.makeClass(node.name, bases, self, scope,
                                   node.body, doc)
        self.module.classes[node.name] = c
        return self.set_name(node.name, c)

    def addModule(self, mod, value=1):
        #print 'add module', mod
        if self.module.imports.has_key(mod) and value == 1:
            return
        self.module.imports[mod] = (value, self.module)

    def addSetAttribute(self, obj, name, value):
        #print ' add set attribute', name, value
        self.module.addAttribute(name, value)

    def setline(self, node):
        self.lineno = node.beginLine

    def execstring(self, data):
        self.data = data
        from org.python.core import parser
        node = parser.parse(data, 'exec')
        return self.parse(node)

    def unhandled_node(self, node):
        raise Exception("Unhandled node " + str(node))

    #def open_level(self, node):
    #    print "open", node
    #def close_level(self, node):
    #    print "close", node
