# Copyright © Corporation for National Research Initiatives

from PythonVisitor import PythonVisitor, nodeToList
from org.python.parser.PythonGrammarTreeConstants import *
import jast



class BaseEvaluator:
    def __init__(self):
        self.globalnames = {}
        self.augtemps = {}
        self.lineno = -1
        self.visitor = PythonVisitor(self)

    def parse(self, node):
        try:
            return self.visit(node)
        except:
            print 'Parsing line: %d' % self.lineno
            if hasattr(self, 'data') and self.lineno > 0:
                print self.data.split('\n')[self.lineno-1]
            raise

    def setline(self, lineno):
        self.lineno = lineno

    def getName(self, name):
        return self.visitor.getName(name)

    def visit(self, node):
        return node.visit(self.visitor)

    def getAugTmps(self, node):
        tmps = self.augtemps[node]
        for var in tmps:
            self.freeTemp(var)
        del self.augtemps[node]
        return tmps
            
    def setAugTmps(self, node, stmts, *vars):
        ret = []
        for var in vars:
            tmp, code = self.makeTemp(var)
            stmts.append(code)
            ret.append(tmp)
        self.augtemps[node] = ret
        return ret

    def suite(self, nodes):
        ret = []
        for node in nodes:
            ret.append(self.visit(node))
        return ret

    def del_stmt(self, nodes):
        stmts = []
        for node in nodes:
            stmts.append(self.delete(node))
        return stmts

    def delete(self, node):
        if node.id == JJTNAME:
            return self.del_name(node.getInfo())
        elif node.id == JJTLIST or node.id == JJTTUPLE:
            return self.del_list(nodeToList(node))
        elif node.id == JJTINDEX_OP:
            return self.del_item(node.getChild(0), 
                            node.getChild(1))
        elif node.id == JJTDOT_OP:
            return self.del_attribute(node.getChild(0),
                            node.getChild(1).getInfo()) 
        else:
            raise TypeError, 'help, fancy lhs: %s' % node

    def del_list(self, seq):
        return self.del_stmt(seq)

    def del_item(self, obj, index):
        if index.id == JJTSLICE:
            start, stop, step = self.getSlice(index)
            return self.visit(obj).delslice(start, stop, step)
        return self.visit(obj).delitem(self.visit(index))

    def del_attribute(self, obj, name):
        return self.visit(obj).delattr(name)

    def set(self, node, value):
        if node.id == JJTNAME:
            return self.set_name(node.getInfo(), value)
        elif node.id == JJTLIST or node.id == JJTFPLIST or node.id == JJTTUPLE:
            return self.set_list(nodeToList(node), value)
        elif node.id == JJTINDEX_OP:
            return self.set_item(node.getChild(0), 
                            node.getChild(1), value)
        elif node.id == JJTDOT_OP:
            return self.set_attribute(node.getChild(0),
                            node.getChild(1).getInfo(), value)  
        else:
            raise TypeError, 'help, fancy lhs: %s' % node

    def set_list(self, seq, value):
        if hasattr(self, 'AUG'):
            raise SyntaxError, "augmented assign to tuple not possible"
        if len(seq) > 0 and seq[-1].id == JJTCOMMA:
            del seq[-1]
        n = len(seq)
        tmp, code = self.makeTemp(value)
        stmts = [code]

        for i in range(n):
            stmts.append(self.set(seq[i], tmp.igetitem(i)))
        self.freeTemp(tmp)
        return stmts

    def set_item(self, obj, index, value):
        if index.id == JJTSLICE:
            if hasattr(self, 'AUG'):
                o, start, stop, step = self.getAugTmps(obj)
            else:
                o = self.visit(obj)
                start, stop, step = self.getSlice(index)
            return o.setslice(start, stop, step, value)

        if hasattr(self, 'AUG'):
            o, idx = self.getAugTmps(obj)
        else:
            o = self.visit(obj)
            idx = self.visit(index)
        return o.setitem(idx, value)

    def set_attribute(self, obj, name, value):
        if hasattr(self, 'AUG'):
            o, = self.getAugTmps(obj)
        else:
            o = self.visit(obj)
        return o.setattr(name, value)

    def get_item(self, obj, index):
        if index.id == JJTSLICE:
            start, stop, step = self.getSlice(index)
            o = self.visit(obj)

            if hasattr(self, 'AUG'):
                o, start, stop, step = self.setAugTmps(obj, self.AUG, 
                             o, start, stop, step)
            return o.getslice(start, stop, step)

        idx = self.visit(index)
        o = self.visit(obj)

        if hasattr(self, 'AUG'):
            o, idx = self.setAugTmps(obj, self.AUG, o, idx)
        return o.getitem(idx)

    def get_attribute(self, obj, name):
        o = self.visit(obj)
        if hasattr(self, 'AUG'):
            o, = self.setAugTmps(obj, self.AUG, o)
        return o.getattr(name)

    def makeTemp(self, value):
        return value

    def freeTemp(self, tmp): pass

    def expr_stmt(self, lhss, rhs):
        if len(lhss) == 0:
            return self.visit(rhs).makeStatement()
        if len(lhss) == 1:
            return self.set(lhss[0], self.visit(rhs))

        tmp, code = self.makeTemp(self.visit(rhs))
        stmts = [code]
        for lhs in lhss:
            stmts.append(self.set(lhs, tmp))
        self.freeTemp(tmp)
        return stmts

    def binary_op(self, name, x, y):
        return self.visit(x).binop(name, self.visit(y))

    def unary_op(self, name, x):
        return self.visit(x).unop(name)

    def aug_binary_op(self, name, lhs, rhs):
        tmp, code = self.makeTemp(self.visit(rhs))
        stmts = [code]

        self.AUG = stmts
        l = self.visit(lhs)
        result = l.aug_binop(name, tmp)
        stmts.append(self.set(lhs, result))
        del self.AUG
        self.freeTemp(tmp)
        
        return stmts

    def compare_op(self, start, compares):
        x = self.visit(start)
        for op, other in compares:
            y = self.visit(other)
            test = x.compop(op, y)
            if not test.nonzero():
                return test
            x = y
        return test

    def print_line(self, value=None):
        if value is None:
            print
        else:
            return self.visit(value).print_line()

    def print_continued(self, value):
        return self.visit(value).print_continued()

    def visitall(self, args):
        ret = []
        for arg in args:
            ret.append(self.visit(arg))
        return ret

    def visitnames(self, kws):
        ret = []
        for name, value in kws:
            ret.append( (name, self.visit(value)) )
        return ret

    def invoke(self, obj, name, args, kws):
        return self.visit(obj).invoke(name, self.visitall(args),
                                      self.visitnames(kws))

    def call(self, callee, args, kws):
        return self.visit(callee).call(self.visitall(args),
                                       self.visitnames(kws))

    def call_extra(self, callee, args, kws, starargs, kwargs):
        #print "call_extra", self.visit(callee)
        if starargs:
            starargs = self.visit(starargs.getChild(0))
        if kwargs:
            kwargs = self.visit(kwargs.getChild(0))
        return self.visit(callee).call_extra(self.visitall(args),
                                            self.visitnames(kws),
                                            starargs,
                                            kwargs)

    def global_stmt(self, names):
        for name in names:
            self.globalnames[name] = 1

    def import_stmt(self, names):
        ret = []
        for name in names:
            ret.append(self.set_name(name[0], self.get_module(name)))
        return ret

    def importall_stmt(self, top):
        pass

    def importfrom_stmt(self, top, names):                      
        module = self.get_module(top, 0)
        if names == '*':
            return self.importall_stmt(module)
            #print 'import * from', module
            #names = module.dir()

        modnames = []
        asnames = []
        for modname, asname in names:
            if asname is None:
                asname = modname
            self.set_name(asname, module.getattr(modname))
            asnames.append(asname)
            modnames.append(modname)

        topmodname = jast.StringConstant(".".join(top))
        modnames = jast.FilledArray(
            "String",
            map(lambda x: jast.StringConstant(x), modnames))
        asnames = jast.FilledArray(
            "String",
            map(lambda x: jast.StringConstant(x), asnames))
        return jast.InvokeStatic(
            "org.python.core.imp", "importFromAs",
            [topmodname, modnames, asnames, self.frame.frame])

    #external interfaces
    def execstring(self, data):
        self.data = data
        from org.python.core import parser
        node = parser.parse(data, 'exec')
        return self.parse(node)

    def execfile(self, filename):
        fp = open(filename, 'r')
        data = fp.read()
        fp.close()
        return execstring(data)
