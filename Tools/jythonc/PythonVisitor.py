# Copyright © Corporation for National Research Initiatives
from org.python.parser import Visitor, SimpleNode
from org.python.parser.PythonGrammarTreeConstants import *
from org.python.parser import SimpleNode


comp_ops = {JJTLESS_CMP:'lt',
            JJTEQUAL_CMP:'eq',
            JJTGREATER_CMP:'gt', 
            JJTGREATER_EQUAL_CMP:'ge',
            JJTLESS_EQUAL_CMP:'le',
            JJTNOTEQUAL_CMP:'ne',
            JJTIS_NOT_CMP:'isnot',
            JJTIS_CMP:'is',
            JJTIN_CMP:'in',
            JJTNOT_IN_CMP:'notin'
            }



def nodeToList(node, start=0):
    nodes = []
    for i in range(start, node.numChildren):
        nodes.append(node.getChild(i))
    return nodes

def nodeToStrings(node, start=0):
    names = []
    for i in range(start, node.numChildren):
        names.append(node.getChild(i).getInfo())
    return names



class Arguments(Visitor):
    def __init__(self, parent, argslist=None):
        self.arglist = 0
        self.keyworddict = 0
        self.names = []
        self.defaults = []

        self.parent = parent

        if argslist is not None:
            argslist.visit(self)

    def varargslist(self, node):
        for i in range(node.numChildren):
            node.getChild(i).visit(self)

    def ExtraArgList(self, node):
        self.arglist = 1
        self.names.append(node.getChild(0).visit(self))

    def ExtraKeywordList(self, node):
        self.keyworddict = 1
        self.names.append(node.getChild(0).visit(self))

    def defaultarg(self, node):
        name = node.getChild(0).visit(self)
        self.names.append(name)
        if node.numChildren > 1:
            self.defaults.append(node.getChild(1).visit(self.parent))

    def fplist(self, node):
        return 'ugh'
        pass # ???

    def Name(self, node):
        return node.getInfo()



def getDocString(suite):
    if suite.numChildren > 0:
        n = suite.getChild(0)
        if n.id == JJTEXPR_STMT and n.getChild(0).id == JJTSTRING:
            return n.getChild(0).getInfo()
    return None



class PythonVisitor(Visitor):
    def __init__(self, walker):
        self.walker = walker

    def getName(self, node):
        if not node.id == JJTNAME:
            return None
        return node.getInfo()

    def walk(self, node):
        self.suite(node)

    def startnode(self, node):
        self.walker.setline(node.beginLine)

    def suite(self, node):
        return self.walker.suite(nodeToList(node))

    file_input = suite
    single_input = suite
    eval_input = suite

    def exec_stmt(self, node):
        self.startnode(node)
        code = node.getChild(0).visit(self)
        globs = locs = None
        if node.numChildren > 1:
            globs = node.getChild(1).visit(self)
        if node.numChildren > 2:
            locs = node.getChild(2).visit(self)
        return self.walker.exec_stmt(code, globs, locs)

    def assert_stmt(self, node):
        self.startnode(node)
        test = node.getChild(0).visit(self)
        if node.numChildren > 1:
            message = node.getChild(1).visit(self)
        else:
            message = None
        return self.walker.assert_stmt(test, message)

    def pass_stmt(self, node):
        self.startnode(node)
        return self.walker.pass_stmt()

    def break_stmt(self, node):
        self.startnode(node)    
        return self.walker.break_stmt()

    def continue_stmt(self, node):
        self.startnode(node)
        return self.walker.continue_stmt()

    def return_stmt(self, node):
        self.startnode(node)
        if node.numChildren == 0:
            return self.walker.return_stmt()
        else:
            return self.walker.return_stmt(node.getChild(0))

    def global_stmt(self, node):
        self.startnode(node)
        return self.walker.global_stmt(nodeToStrings(node))

    def raise_stmt(self, node):
        self.startnode(node)
        exc_type = exc_value = exc_traceback = None
        n = node.numChildren
        if n > 0:
            exc_type = node.getChild(0).visit(self)
        if n > 1:
            exc_value = node.getChild(1).visit(self)
        if n > 2:
            exc_traceback = node.getChild(2).visit(self)
        return self.walker.raise_stmt(exc_type, exc_value, exc_traceback)

    def Import(self, node):
        self.startnode(node)
        names = []
        for i in range(node.numChildren):
            names.append(node.getChild(i).visit(self))
        return self.walker.import_stmt(names)

    def ImportFrom(self, node):
        self.startnode(node)
        if node.numChildren > 1:
            return self.walker.importfrom_stmt(node.getChild(0).visit(self), 
                                                     nodeToStrings(node, 1))
        else:
            return self.walker.importfrom_stmt(
                node.getChild(0).visit(self), "*")

    def dotted_name(self, node):
        return nodeToStrings(node)

    def print_stmt(self, node):
        self.startnode(node)
        n = node.numChildren
        rets = []
        for i in range(n-1):
            rets.append(self.walker.print_continued(node.getChild(i)))

        if n == 0:
            rets.append(self.walker.print_line())
        elif node.getChild(n-1).id != JJTCOMMA:
            rets.append(self.walker.print_line(node.getChild(n-1)))

        return rets

    def if_stmt(self, node):
        self.startnode(node)
        tests = []
        else_body = None
        n = node.numChildren
        for i in range(0,n-1,2):
            tests.append( (node.getChild(i), node.getChild(i+1)) )

        if n % 2 == 1:
            else_body = node.getChild(n-1)

        return self.walker.if_stmt(tests, else_body)

    def while_stmt(self, node):
        self.startnode(node)
        test = node.getChild(0)
        suite = node.getChild(1)
        if node.numChildren == 3:
            else_body = node.getChild(2)
        else:
            else_body = None

        return self.walker.while_stmt(test, suite, else_body)

    def for_stmt(self, node):
        self.startnode(node)
        index = node.getChild(0)
        sequence = node.getChild(1)
        body = node.getChild(2)
        if node.numChildren == 4:
            else_body = node.getChild(3)
        else:
            else_body = None

        return self.walker.for_stmt(index, sequence, body, else_body)

    def expr_stmt(self, node):
        self.startnode(node)
        rhs = node.getChild(node.numChildren-1)
        return self.walker.expr_stmt(nodeToList(node)[:-1], rhs)

    def del_stmt(self, node):
        self.startnode(node)
        return self.walker.del_stmt(nodeToList(node))

    def Name(self, node):
        #self.startnode(node)
        return self.walker.name_const(node.getInfo())

    def Int(self, node):
        #self.startnode(node)
        i = node.getInfo()
        if isinstance(i, type(0)):
            return self.walker.int_const(int(i))
        else:
            return self.walker.long_const(long(i))

    def Float(self, node):
        #self.startnode(node)
        return self.walker.float_const(float(node.getInfo()))

    def Complex(self, node):
        #self.startnode(node)
        return self.walker.complex_const(float(node.getInfo())) 

    def String(self, node):
        #self.startnode(node)
        return self.walker.string_const(node.getInfo())

    def getSlice(self, node):
        s = [None, None, None]
        n = node.numChildren
        index = 0
        for i in range(n):
            child = node.getChild(i)
            if child.id == JJTCOLON:
                index = index+1
            else:
                s[index] = child
        return s

    def Slice(self, node):
        #self.startnode(node)
        s = self.getSlice(node)
        return self.walker.slice_op(s[0], s[1], s[2])

    def makeSeqArgs(self, node):
        values = nodeToList(node)
        ret = []
        for value in values:
            if value.id == JJTCOMMA:
                continue
            ret.append(value)
        return ret


    def list(self, node):
        #self.startnode(node)
        return self.walker.list_op(self.makeSeqArgs(node))

    def tuple(self, node):
        #self.startnode(node)
        return self.walker.tuple_op(self.makeSeqArgs(node))

    def dictionary(self, node):
        #self.startnode(node)
        items = []
        for i in range(0, node.numChildren, 2):
            items.append( (node.getChild(i), node.getChild(i+1)) )
        return self.walker.dictionary_op(items)

    def Dot_Op(self, node):
        #self.startnode(node)   
        obj = node.getChild(0)
        name = node.getChild(1).getInfo()
        return self.walker.get_attribute(obj, name)

    def Index_Op(self, node):
        obj = node.getChild(0)
        index = node.getChild(1)
        return self.walker.get_item(obj, index)

    def Call_Op(self, node):
        callee = node.getChild(0)

        args = []
        keyargs = []

        if node.numChildren != 1:
            argsNode = node.getChild(1)
            for i in range(argsNode.numChildren):
                argNode = argsNode.getChild(i)
                if argNode.id != JJTKEYWORD:
                    if len(keyargs) > 0:
                        raise ValueError, \
                              "non-keyword argument following keyword"
                    args.append(argNode)
                else:
                    keyargs.append((argNode.getChild(0).getInfo(),
                                    argNode.getChild(1)))

        # Check for method invocation
        if callee.id == JJTDOT_OP:
            object = callee.getChild(0)
            name = callee.getChild(1).getInfo()
            return self.walker.invoke(object, name, args, keyargs)
        return self.walker.call(callee, args, keyargs)

    def binop(self, node, name):
        #self.startnode(node)
        return self.walker.binary_op(name, node.getChild(0), node.getChild(1))

    def add_2op(self, node): return self.binop(node, 'add')     
    def sub_2op(self, node): return self.binop(node, 'sub')     
    def mul_2op(self, node): return self.binop(node, 'mul')     
    def div_2op(self, node): return self.binop(node, 'div')     
    def mod_2op(self, node): return self.binop(node, 'mod')
    def and_2op(self, node): return self.binop(node, 'and')
    def lshift_2op(self, node): return self.binop(node, 'lshift')
    def rshift_2op(self, node): return self.binop(node, 'rshift')
    def or_2op(self, node): return self.binop(node, 'or')
    def xor_2op(self, node): return self.binop(node, 'xor')
    def pow_2op(self, node): return self.binop(node, 'pow')

    def unop(self, node, name):
        #self.startnode(node)
        return self.walker.unary_op(name, node.getChild(0))

    def abs_1op(self, node): return self.unop(node, 'abs')
    def invert_1op(self, node): return self.unop(node, 'invert')
    def neg_1op(self, node): return self.unop(node, 'neg')
    def abs_1op(self, node): return self.unop(node, 'abs')
    def pos_1op(self, node): return self.unop(node, 'pos')
    def not_1op(self, node): return self.unop(node, 'not')
    def str_1op(self, node): return self.unop(node, 'str')

    def getString(self, node):
        if node.id == JJTSTRING:
            return node.getInfo()
        elif node.id == JJTSTRJOIN:
            return self.getString(node.getChild(0))+self.getString(node.getChild(1))
        else:
            raise ValueError, 'non string!'

    def strjoin(self, node):
        return self.walker.string_const(self.getString(node))

    def comparision(self, node):
        #self.startnode(node)
        start = node.getChild(0)
        tests = []
        for i in range(1, node.numChildren, 2):
            op = comp_ops[node.getChild(i).id]
            obj = node.getChild(i+1)
            tests.append( (op, obj) )
        return self.walker.compare_op(start, tests)

    def and_boolean(self, node):
        return self.walker.and_op(node.getChild(0), node.getChild(1))

    def or_boolean(self, node):
        return self.walker.or_op(node.getChild(0), node.getChild(1))

    def try_stmt(self, node):
        self.startnode(node)

        n = node.numChildren
        if n == 2:
            return self.walker.tryfinally(node.getChild(0), node.getChild(1))

        body = node.getChild(0)
        exceptions = []

        for i in range(1, n-1, 2):
            exc = node.getChild(i)
            if exc.numChildren == 0:
                exc = None
            elif exc.numChildren == 1:
                exc = [exc.getChild(0).visit(self)]
            else:
                exc = [exc.getChild(0).visit(self), exc.getChild(1)]
            exceptions.append( (exc, node.getChild(i+1)) )

        if n%2 == 0:
            elseClause = node.getChild(n-1)
        else:
            elseClause = None

        return self.walker.tryexcept(body, exceptions, elseClause)

    def funcdef(self, node):
        self.startnode(node)

        funcname = node.getChild(0).getInfo()

        Body = node.getChild(node.numChildren-1)

        doc = getDocString(Body)
        if node.numChildren > 2:
            args = Arguments(self, node.getChild(1))
        else:
            args = Arguments(self)

        return self.walker.funcdef(funcname, args, Body, doc)

    def lambdef(self, node):
        Body = node.getChild(node.numChildren-1)

        if node.numChildren > 1:
            args = Arguments(self, node.getChild(0))
        else:
            args = Arguments(self)

        retBody = SimpleNode(JJTRETURN_STMT)
        retBody.jjtAddChild(Body, 0)

        return self.walker.lambdef(args, retBody)

    def classdef(self, node):
        self.startnode(node)
        name = node.getChild(0).getInfo()

        n = node.numChildren
        suite = node.getChild(n-1)
        doc = getDocString(suite)
        bases = []
        for i in range(1, n-1):
            bases.append(node.getChild(i).visit(self))

        return self.walker.classdef(name, bases, suite, doc)
