from org.python.parser import Visitor
from org.python.parser.PythonGrammarTreeConstants import *
import string, org

class FreezeVisitor(Visitor):
	def __init__(self, events, props, packages=None, classes=None, proxies=None,
				classname=None):
		if packages is None:
			self.packages = {}
		else:
			self.packages = packages
			
		if classes is None:
			self.classes = {}
		else:
			self.classes = classes
			
		if proxies is None:
			self.proxies = {}
		else:
			self.proxies = proxies
			
		self.events = events
		self.props = props
		
		self.classname = classname
		self.realclass = None
		self.methods = []
		self.addMethods = 0
			
	def suite(self, node):
		for i in range(node.numChildren):
			node.getChild(i).visit(self)
			
	def pass_stmt(self, node):
		pass

	print_stmt = del_stmt = break_stmt = continue_stmt = pass_stmt
	return_stmt = raise_stmt = global_stmt = exec_stmt = pass_stmt
	assert_stmt = lambdef = pass_stmt
	
	Int = Float = Complex = String = pass_stmt
	Slice = Ellipsis = list = dictionary = tuple = pass_stmt
	
	str_1op = __add__2op = pass_stmt

	add_2op = sub_2op = mul_2op = div_2op = mod_2op = pow_2op = pass_stmt
	and_2op = or_2op = xor_2op = lshift_2op = rshift_2op = pass_stmt
	
	pos_1op = neg_1op = invert_1op = pass_stmt

	comparision = pass_stmt
	
	Index_Op = pass_stmt

	def funcdef(self, node):
		if self.addMethods:
			self.methods.append(node.getChild(0).visit(self))
		self.suite(node.getChild(node.numChildren-1))
		
	def lhs_dot(self, node):
		#print 'lhs', node, node.id
		if node.id == JJTNAME:
			name = node.getInfo()
			#print 'name', name
			self.props[name] = 1			
		if node.id != JJTDOT_OP: return
		self.lhs_dot(node.getChild(1))
		

	def expr_stmt(self, node):
		rhs = node.getChild(node.numChildren-1)
		rhs.visit(self)
		
		for i in range(node.numChildren-1):
			lhs = node.getChild(i)
			self.lhs_dot(lhs)

	def classdef(self, node):
		name = node.getChild(0).visit(self)
		n = node.numChildren
		suite = node.getChild(n-1)
		bases = []
		
		for i in range(1,n-1):
			child = node.getChild(i)
			base = child.visit(self)
			#print name, base
			#if self.classname == name:
			#	print self.classes
			if self.classes.has_key(base):
				self.classes[name] = self.classes[base]
				#print self.classname, name, self.realclass, base
				if self.classname == name and self.realclass is None:
					self.realclass = self.classes[base]
					self.addMethods = 1
				else:
					self.proxies[base] = self.classes[base]
								
		#print name, bases
		suite.visit(self)
		self.addMethods = 0

	def addEvent(self, c):
		self.proxies[c.__name__] = c

	def Call_Op(self, node):
		name = node.getChild(0).visit(self)
		if name is None or node.numChildren == 1: return
		if self.classes.has_key(name):
			c = self.classes[name]
		else:
			return		


		args = node.getChild(1).visit(self)
		for arg in args:
			if isinstance(arg, type( () )) and len(arg) == 2:
				kw, v = arg
				if hasattr(c, kw):
					a = getattr(c, kw)
					if isinstance(a, org.python.core.PyBeanEventProperty):
						self.addEvent(a.myClass)
		#print name, args
		
	def arglist(self, node):
		args = []
		for i in range(node.numChildren):
			args.append(node.getChild(i).visit(self))
		return args
			
	def Keyword(self, node):
		return node.getChild(0).visit(self), node.getChild(1).visit(self)

	def Dot_Op(self, node):
		n1, n2 = node.getChild(0).visit(self), node.getChild(1).visit(self)
					
		if n1 is None: return
		if self.packages.has_key(n1):
			ret = getattr(self.packages[n1], n2)
			name = ret.__name__
			self.addClass(name, ret)
			return name
	
	def if_stmt(self, node):
		for i in range(node.numChildren):
			if i%2 == 1 or i == node.numChildren-1:
				node.getChild(i).visit(self)
				
	def while_stmt(self, node):
		node.getChild(1).visit(self)
		if node.numChildren == 3:
			node.getChild(2).visit(self)
			
	def for_stmt(self, node):
		self.while_stmt(node)
		
	def try_stmt(self, node):
		for i in range(node.numChildren):
			if i%2 == 1 and i != node.numChildren-1:
				continue
			node.getChild(i).visit(self)

	def addEvents(self, c):
		for base in c.__bases__:
			self.addClass(None, base)
		#print 'events', c, dir(c)
		for key, value in c.__dict__.items():
			#print key, value
			if isinstance(value, org.python.core.PyBeanEventProperty):
				#print 'event', key, value
				self.events[key] = value.myClass
				#self.addEvent(value.myClass)

	def addClass(self, name, c):
		#print 'add class', name, c, c.__class__
		if name is None: name = c.__name__
		if (isinstance(c, org.python.core.PyJavaPackage) or 
			isinstance(c, org.python.core.PyModule)):
			self.packages[name] = c
		elif isinstance(c, org.python.core.PyClass):
			#print 'instance', name, c, self.classes.has_key(name)
			doEvents = not self.classes.has_key(name)
			self.classes[name] = c
			name = c.__name__
			if doEvents:
				self.classes[name] = c
				self.addEvents(c)

	def Import(self, node):
		for i in range(node.numChildren):
			name = node.getChild(i).visit(self)
			self.addClass(string.split(name, '.')[0], __import__(name))


	def ImportFrom(self, node):
		root = node.getChild(0).visit(self)
		#print root
		rootclass = __import__(root)
		attrs = string.split(root, '.')[1:]
		#print root, rootclass
		for attr in attrs:
			rootclass = getattr(rootclass, attr)
		#print root, rootclass
		self.addClass(root, rootclass)
			
		#print rootclass
		for i in range(1, node.numChildren):
			name = node.getChild(i).visit(self)
			#print rootclass, name
			c = getattr(rootclass, name)
			self.addClass(name, c)
						
	def dotted_name(self, node):
		l = []
		for i in range(node.numChildren):
			l.append(node.getChild(i).visit(self))
		return string.join(l, '.')

	def Name(self, node):
		return node.getInfo()
