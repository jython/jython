from org.python.parser import Visitor, SimpleNode
from org.python.parser.PythonGrammarTreeConstants import *

comp_ops = {JJTLESS_CMP:'lt', JJTEQUAL_CMP:'eq', JJTGREATER_CMP:'gt', 
	JJTGREATER_EQUAL_CMP:'ge', JJTLESS_EQUAL_CMP:'le', JJTNOTEQUAL_CMP:'ne',
	JJTIS_NOT_CMP:'isnot', JJTIS_CMP:'is', JJTIN_CMP:'in', JJTNOT_IN_CMP:'notin'}

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

"""
class Arguments(Visitor):
	def __init__(self, argslist):
		self.arglist = 0
		self.keyworddict = 0
		self.names = []
		self.defaults = []
		
		for label, default in args:
			self.names.append(label)
			if default is not None:
				self.defaults.append(default)
				

	def ExtraArgList(self, node):
		self.arglist = 1
		names.append(node.getChild(0).visit(self)
		
	def ExtraKeywordList(self, node):
		self.keyworddict = 1
		names.append(node.getChild(0).visit(self)
		


	def fplist(self, node):
		???

	def Name(self, node):
		return node.getInfo()
"""
	

class PythonVisitor(Visitor):
	def __init__(self, walker):
		self.walker = walker
		
	def walk(self, node):
		self.suite(node)

	def startnode(self, node):
		self.walker.setline(node.beginLine)

	def suite(self, node):
		return self.walker.suite(nodeToList(node))
		
	file_input = suite
	single_input = suite
	eval_input = suite
	
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
			return self.walker.return_stmt(NoneNode)
		else:
			return self.walker.return_stmt(node.getChild(0))
		
	def global_stmt(self, node):
		self.startnode(node)
		return self.walker.global_stmt(nodeToStrings(node))
		
	#def raise_stmt(self, node):
		
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
			return self.walker.importfrom_stmt(node.getChild(0).visit(self), "*")
		
		
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
		
	def Name(self, node):
		self.startnode(node)
		return self.walker.name_const(node.getInfo())

	def Int(self, node):
		self.startnode(node)
		return self.walker.int_const(int(node.getInfo()))
		
	def Float(self, node):
		self.startnode(node)
		return self.walker.float_const(float(node.getInfo()))
		
	def String(self, node):
		self.startnode(node)
		return self.walker.string_const(node.getInfo())
		
	def list(self, node):
		self.startnode(node)
		return self.walker.list_op(nodeToList(node))
		
	def tuple(self, node):
		self.startnode(node)
		return self.walker.tuple_op(nodeToList(node))

	def dictionary(self, node):
		self.startnode(node)
		items = []
		for i in range(0, node.numChildren, 2):
			items.append( (node.getChild(i), node.getChild(i+1)) )
		return self.walker.dictionary_op(items)

	def Dot_Op(self, node):
		self.startnode(node)	
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
						raise ValueError, "non-keyword argument following keyword"
					args.append(argNode)
				else:
					keyargs.append( (argNode.getChild(0).getInfo(), argNode.getChild(1)) )

		#Check for method invocation
		if callee.id == JJTDOT_OP:
			object = callee.getChild(0)
			name = callee.getChild(1).getInfo()
			return self.walker.invoke(object, name, args, keyargs)

		return self.walker.call(callee, args, keyargs)

	def binop(self, node, name):
		self.startnode(node)
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
		self.startnode(node)
		return self.walker.unary_op(name, node.getChild(0))

	def abs_1op(self, node): return self.unop(node, 'abs')
	def invert_1op(self, node): return self.unop(node, 'invert')
	def neg_1op(self, node): return self.unop(node, 'neg')
	def abs_1op(self, node): return self.unop(node, 'abs')
	def pos_1op(self, node): return self.unop(node, 'pos')
	
	def comparision(self, node):
		self.startnode(node)
		start = node.getChild(0)
		tests = []
		for i in range(1, node.numChildren, 2):
			op = comp_ops[node.getChild(i).id]
			obj = node.getChild(i+1)
			tests.append( (op, obj) )
		return self.walker.compare_op(start, tests)
		
	def parseArgs(self, Args):
		args = []

		for i in range(Args.numChildren):
			node = Args.getChild(i)
			name = node.getChild(0).getInfo()
			
			if node.numChildren > 1:
				default = node.getChild(1).visit(self)
			else:
				default = None
			args.append( (name, default) )
		return args

	def funcdef(self, node):
		funcname = node.getChild(0).getInfo()
		
		Body = node.getChild(node.numChildren-1)
		
		if node.numChildren > 2:
			args = self.parseArgs(node.getChild(1))
		else:
			args = []
			
		return self.walker.funcdef(funcname, args, Body)
		
	def classdef(self, node):
		name = node.getChild(0).getInfo()

		n = node.numChildren
		suite = node.getChild(n-1)
		bases = []
		for i in range(1, n-1):
			bases.append(node.getChild(i).visit(self))
		
		return self.walker.classdef(name, bases, suite)
