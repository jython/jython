from PythonVisitor import PythonVisitor, nodeToList
from org.python.parser.PythonGrammarTreeConstants import *
import string

class BaseEvaluator:
	def __init__(self):
		self.globalnames = {}
		self.lineno = -1
		self.visitor = PythonVisitor(self)

	def parse(self, node):
		try:
			return self.visit(node)
		except:
			print 'Parsing line: %d' % self.lineno
			if hasattr(self, 'data') and self.lineno > 0:
				print string.split(self.data, '\n')[self.lineno-1]
			raise

	def setline(self, lineno):
		self.lineno = lineno
		
	def visit(self, node):
		return node.visit(self.visitor)

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
		elif node.id == JJTLIST or node.id == JJTTUPLE:
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
		n = len(seq)
		tmp, code = self.makeTemp(value)
		stmts = [code]
		
		for i in range(n):
			stmts.append(self.set(seq[i], tmp.igetitem(i)))
		self.freeTemp(tmp)
		return stmts

	def set_item(self, obj, index, value):
		if index.id == JJTSLICE:
			start, stop, step = self.getSlice(index)
			return self.visit(obj).setslice(start, stop, step, value)
		return self.visit(obj).setitem(self.visit(index), value)
		
	def set_attribute(self, obj, name, value):
		return self.visit(obj).setattr(name, value)

	def get_item(self, obj, index):
		if index.id == JJTSLICE:
			start, stop, step = self.getSlice(index)
			return self.visit(obj).getslice(start, stop, step)
		return self.visit(obj).getitem(self.visit(index))
		
	def get_attribute(self, obj, name):
		return self.visit(obj).getattr(name)

	def makeTemp(self, value):
		return value
		
	def freeTemp(self, tmp): pass

	def expr_stmt(self, lhss, rhs):
		if len(lhss) == 0: return self.visit(rhs).makeStatement()
		if len(lhss) == 1: return self.set(lhss[0], self.visit(rhs))
		
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

	def compare_op(self, start, compares):
		x = self.visit(start)
		for op, other in compares:
			y = self.visit(other)
			test = x.compop(op, y)
			if not test.nonzero(): return test
			x = y
		return test

	def print_line(self, value=None):
		if value is None: print
		else: return self.visit(value).print_line()
			
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
		return self.visit(obj).invoke(name, self.visitall(args), self.visitnames(kws))

	def call(self, callee, args, kws):
		return self.visit(callee).call(self.visitall(args), self.visitnames(kws))

	def global_stmt(self, names):
		for name in names:
			self.globalnames[name] = 1
			
	def import_stmt(self, names):
		ret = []
		for name in names:
			ret.append(self.set_name(name[0], self.get_module(name)))
		return ret
		
	def importfrom_stmt(self, top, names):
		module = self.get_module(top, 1)
		if names == '*':
			print 'import * from', module
			names = module.dir()
		ret = []
		for name in names:
			ret.append(self.set_name(name, module.getattr(name)))
		return ret
	
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