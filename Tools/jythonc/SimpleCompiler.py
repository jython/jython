from BaseEvaluator import BaseEvaluator
import string
import jast

def clean(node):
	if node.exits(): return node
	
	return jast.Block([node, jast.Return(jast.GetStaticAttribute('Py', 'None'))])


class Arguments:
	def __init__(self, argslist):
		self.arglist = 0
		self.keyworddict = 0
		self.names = []
		self.defaults = []
		
		for label, default in argslist:
			self.names.append(label)
			if default is not None:
				self.defaults.append(default)

def mkStrings(values):
	lst = []
	for value in values:
		if value is None:
			jv = jast.Null
		else:
			jv = jast.StringConstant(value)
			
		lst.append(jv)
	return jast.FilledArray("String", lst)

class BasicModule:
	def __init__(self, name, filename="<unknown>"):
		self.name = name
		self.filename = filename
		self.superclass = "Object"
		self.interfaces = []
		self.strings = {}
		self.integers = {}
		self.temps = []
		self.codes = []
		
		self.constants = []
		
		self.funccodes = []
		self.uniquenames = []
		
		self.prefix = "private static final PyObject"
		self.package = None
		
		self.javamain = 1

	def getIntegerConstant(self, value):
		if self.integers.has_key(value):
			return self.integers[value]
		ret = jast.Identifier("i$"+str(value))
		self.integers[value] = ret
		return ret
		
	def getStringConstant(self, value):
		if self.strings.has_key(value):
			return self.strings[value]
		ret = jast.Identifier("s$%d" % len(self.strings))
		self.strings[value] = ret
		return ret

	def getCodeConstant(self, name, args, locals, code):
		label = "c$%d_%s" % (len(self.codes), name)
		ret = jast.Identifier(label)
		self.codes.append( (label, name, args, locals, code) )
		return ret
		
	def dumpIntegers(self):
		items = self.integers.items()
		items.sort()
		for value, label in items:
			self.addConstant("PyObject", label, jast.InvokeStatic("Py", "newInteger", [jast.IntegerConstant(value)]))
					
	def dumpStrings(self):
		items = self.strings.items()
		items.sort()
		for value, label in items:
			self.addConstant("PyObject", label, jast.InvokeStatic("Py", "newString", [jast.StringConstant(value)]))
		
	def dumpCodes(self):
		self.constants.append(["PyFunctionTable", self.getFunctionTable(), jast.New(self.name, [])])

		for label, name, args, locals, code in self.codes:
			funcid = self.addFunctionCode(name, code)
			
			arglist = keyworddict = jast.False
			if args.arglist: arglist = jast.True
			if args.keyworddict: keyworddict = jast.True
			
			names = mkStrings(locals)

			cargs = [jast.IntegerConstant(len(args.names)),
				names,
				jast.StringConstant(self.filename),
				jast.StringConstant(name),
				arglist,
				keyworddict,
				self.getFunctionTable(),
				jast.IntegerConstant(funcid)]
			newcode = jast.InvokeStatic("Py", "newCode", cargs)
			self.addConstant("PyCode", jast.Identifier(label), newcode)

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
			
			body = jast.Return(jast.InvokeStatic(self.name, funcname, callargs))
			cases.append( [jast.IntegerConstant(funcid), jast.FreeBlock([body])] )

		defaultCase = jast.FreeBlock([jast.Return(jast.Null)])
		switch = jast.Block([jast.Switch(jast.Identifier('index'), cases, defaultCase)])

		meths.insert(0, jast.Method("call_function", "public", ["PyObject", ("int", "index"), ("PyFrame", "frame")], switch))
		self.superclass = "PyFunctionTable"

		return meths
		
	def addConstant(self, type, label, value):
		self.constants.append( (type, label, value) )
		
	def dumpConstants(self):
		self.dumpIntegers()
		self.dumpStrings()
		self.dumpCodes()
		
		stmts = []
		decls = []

		for type, label, value in self.constants:
			decls.append(jast.Declare("private static "+type, label))
			stmts.append(jast.Set(label, value))
		
		setconstants = jast.Method("initConstants", "private static", ["void"], jast.Block(stmts))
		decls.append(setconstants)
		
		return decls


	def getFunctionTable(self):
		return jast.Identifier("funcTable")
		
	def addFunctionCode(self, name, code):
		self.funccodes.append( (name, len(self.funccodes), code) )
		return len(self.funccodes)-1

	def addMain(self, code, cc):
		self.mainCode = self.getCodeConstant("main", Arguments([]), cc.frame.getlocals(), code)


	def dumpMain(self):
		if not hasattr(self, 'mainCode'): return []
		meths = []
		
		self.interfaces.append("PyRunnable")
		getmain = jast.Block(
				[jast.If(
					jast.Operation("==", self.mainCode, jast.Null),
					jast.InvokeStatic(self.name, "initConstants", [])),
				 jast.Return(self.mainCode)])
		meths.append(jast.Method("getMain", "public", ["PyCode"], getmain))

		if self.javamain:
			args = [jast.StringConstant(self.name), jast.Identifier('args'), 
					self.getPackages(), self.getProperties(), jast.False]
			maincode = jast.Block([jast.InvokeStatic("Py", "runMain", args)])
			meths.append(jast.Method("main", "public static", 
							["void", ("String[]", "args")], maincode))
			
		return meths

	def getProperties(self):
		props = [
			"python.packages.paths", "",
			"python.packages.directories", "",
			#"python.options.classExceptions", "false",
			"python.path", "c:\\jpython\\lib",
			#"python.home", "c:\\jpython"
		]
		
		return mkStrings(props)
		
	def getPackages(self):
		packs = ["java.awt", None, "java.applet", None]
		return mkStrings(packs)

	def dumpAll(self):
		return [self.dumpConstants(),
				jast.Blank, self.dumpMain(), self.dumpFuncs()]

	def makeClass(self):
		body = jast.Block(self.dumpAll())
		return jast.Class(self.name, "public", self.superclass, self.interfaces, body)

	def makeClassFile(self):
		header = []
		if self.package is not None:
			header.append(jast.Identifier("package %s" % self.package))
			header.append(jast.Blank)
		header.append(jast.Identifier("import org.python.core.*"))
		header.append(jast.Blank)
		
		return jast.FreeBlock([header, self.makeClass()])

	def dump(self, directory):
		cf = self.makeClassFile()
		sf = jast.Output.SourceFile(self.name)
		cf.writeSource(sf)
		sf.dump(directory) 
		return cf
		
class DynamicIntReference:
	def __init__(self, frame, name):
		self.ivalue = jast.IntegerConstant(len(frame.locals))
		self.name = name
		self.frame = frame.frame
				
	def getValue(self):
		return PyObject(jast.Invoke(self.frame, "getlocal", (self.ivalue,)), None)
		
	def setValue(self, value):
		return jast.Invoke(self.frame, "setlocal", (self.ivalue, value.asAny()))

class DynamicStringReference:
	def __init__(self, frame, name):
		self.ivalue = jast.StringConstant(name)
		self.name = name
		self.frame = frame.frame
				
	def getValue(self):
		return PyObject(jast.Invoke(self.frame, "getname", (self.ivalue,)), None)
		
	def setValue(self, value):
		return jast.Invoke(self.frame, "setlocal", (self.ivalue, value.asAny()))

class DynamicGlobalStringReference:
	def __init__(self, frame, name):
		self.ivalue = jast.StringConstant(name)
		self.name = name
		self.frame = frame.frame
		
	def getValue(self):
		return PyObject(jast.Invoke(self.frame, "getglobal", (self.ivalue,)), None)
		
	def setValue(self, value):
		return jast.Invoke(self.frame, "setglobal", (self.ivalue, value.asAny()))


class LocalFrame:
	def __init__(self, globalNamespace, newReference=DynamicIntReference):
		self.frame = jast.Identifier("frame")
		self.globalNamespace = globalNamespace
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
			if temps[index] is None: break
			index = index + 1
		if index == len(temps):
			temps.append(None)
			
		tname = "t$%d$%s" % (index, type)
		temp = jast.Identifier(tname)
		temps[index] = temp
		print 'get temp', index, type, temps
		return temp
		
	def freetemp(self, temp):
		index = int(string.split(temp.name, '$')[1])
		type = string.split(temp.name, '$')[2]
		temps = self.gettemps(type)
		
		print 'free temp', index, type, temps

		if temps[index] is None:
			raise ValueError, 'temp already freed'
		temps[index] = None
		
		
	def getname(self, name):
		if not self.names.has_key(name):
			return self.globalNamespace.getname(self, name)
		ref = self.getReference(name)
		return ref.getValue()
		
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
		if len(self.temporaries) == 0: return []
		
		decs = [jast.SimpleComment("Temporary Variables")]
		for type, temps in self.temporaries.items():
			names = []
			for index in range(len(temps)):
				names.append("t$%d$%s" % (index, type))
			decs.append(jast.Identifier("%s %s" % (type, string.join(names, ', '))))
		decs.append(jast.Blank)
		return decs 



class BasicGlobals:
	def __init__(self, newReference=DynamicGlobalStringReference):
		self.names = {}
		self.newReference = newReference
		
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

GenericGlobalFrame = LocalFrame(BasicGlobals(), DynamicStringReference)


class SimpleCompiler(BaseEvaluator):
	def __init__(self, module, frame=None):
		BaseEvaluator.__init__(self)
		
		if frame is None:
			frame = GenericGlobalFrame
		self.frame = frame
		self.module = module

	def parse(self, node):
		ret = BaseEvaluator.parse(self, node)
		decs = self.frame.getDeclarations()
		if len(decs) != 0:
			return [decs, jast.SimpleComment('Code'), ret]
		else:
			return ret

	def makeTemp(self, value):
		tmp = self.frame.gettemp('PyObject')
		setit = jast.Set(tmp, value.asAny())
		return PyObject(tmp, self.module), setit
		
	def freeTemp(self, tmp):
		self.frame.freetemp(tmp.asAny())

	#primitive values
	def int_const(self, value):
		return PyInteger(value, self.module)
		
	def float_const(self, value):
		return PyFloat(value, self.module)
		
	def string_const(self, value):
		return PyString(value, self.module)
	
	# builtin types
	def make_seq(self, name, values):
		ret = []
		for value in values:
			ret.append(self.visit(value))
		lst = jast.New(name, [mkArray(ret)])
		return PyObject(lst, self.module)

	def list_op(self, values):
		return self.make_seq('PyList', value)
		
	def tuple_op(self, values):
		return self.make_seq('PyTuple', value)
		
	def dictionary_op(self, items):
		lst = []
		for key, value in items:
			lst.append(key)
			lst.append(value)
		return self.make_seq('PyDictionary', lst)

	#namespaces
	def set_name(self, name, value):
		return self.frame.setname(name, value)
		
	def name_const(self, name):
		return self.frame.getname(name)

	def global_stmt(self, names):
		for name in names:
			self.frame.addglobal(name)


	def get_module(self, names):
		top = PyObject(jast.InvokeStatic("imp", "load", [jast.StringConstant(names[0])]), self.module)
		
		for part in names[1:]:
			top = top.getattr(part)
		return top


	#flow control
	def compare_op(self, start, compares):
		x = self.visit(start)
		firsttime = 1
		tmp = len(compares) > 1
		test = None
		for op, other in compares:
			y = self.visit(other)
			
			if tmp:
				tmp = self.frame.gettemp(PyObject.type)
				gety = PyObject(jast.Set(tmp, y.asAny()), self.module)
			else:
				gety = y
			
			thistest = x.compop(op, gety)
			if test is None:
				test = thistest
			else:
				test = PyObject(jast.TriTest(test.nonzero(), thistest.asAny(), x.asAny()), self.module)
			if tmp:
				if not firsttime:
					self.frame.freetemp(x.asAny())
				x = PyObject(tmp, self.module)
			firsttime = 0
		if tmp:
			self.frame.freetemp(tmp)
		return test

	def pass_stmt(self): pass
	def continue_stmt(self):
		return jast.Continue()
		
	def break_stmt(self):
		return jast.Break()
		
	def return_stmt(self, value):
		return jast.Return(self.visit(value).asAny())

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
			test = self.visit(test).nonzero()
			body = jast.Block(self.visit(body))
			jtests.append( (test, body) )
			
		if else_body is not None:
			else_body = jast.Block(self.visit(else_body))
			
		if len(jtests) == 1:
			return jast.If(jtests[0][0], jtests[0][1], else_body)
		else:
			return jast.MultiIf(jtests, else_body)
		
	def for_stmt(self, index, sequence, body, else_body=None):			
		counter = self.frame.gettemp('int')
		item = PyObject(self.frame.gettemp(PyObject.type), self.module)
		seq = self.frame.gettemp(PyObject.type)
		
		init = []
		init.append( jast.Set(counter, jast.IntegerConstant(0)) )
		init.append( jast.Set(seq, self.visit(sequence).asAny()) )
		
		counter_inc = jast.PostOperation(counter, '++')
		
		test = jast.Set(item.asAny(), jast.Invoke(seq, "__getitem__", [counter_inc]))
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

	def funcdef(self, name, args, body):
		func = PyFunction(name, args, body, self.module, self.frame)
		return self.set_name(name, func)

	def classdef(self, name, bases, body):
		c = PyClass(name, bases, body, self)
		return self.set_name(name, c)


def mkAnys(args):
	ret = []
	for arg in args:
		ret.append(arg.asAny())
	return ret

def mkArray(args):
	aargs = mkAnys(args)
	return jast.FilledArray("PyObject", aargs)

class PyObject:
	type = "PyObject"
	def asAny(self):
		return self.value
				
	def makeTemp(self, frame):
		return PyObject(jast.Set(frame.gettemp(self.type), self.asAny()), self.module)
		
	def freeTemp(self, frame):
		frame.freetemp(self.value)

	def print_line(self):
		#print self.value
		return jast.InvokeStatic("Py", "println", [self.asAny()])
		
	def print_continued(self):
		#print self.value,	
		return jast.InvokeStatic("Py", "printComma", [self.asAny()])

	def __init__(self, value, module):
		self.module = module
		self.makeValue(value)
		
	def makeValue(self, value):
		self.value = value
		
	def nonzero(self):
		return self.domethod("__nonzero__").value
		
	def unop(self, op):
		test = getattr(org.python.core.PyObject, '__'+op+'__')(self.value)
		return PyObject(test)
		
	def domethod(self, name, *args):
		return PyObject(jast.Invoke(self.asAny(), name, args), self.module)

	def compop(self, op, y):
		#print 'comp', self.value, op, y.value
		return self.domethod("_"+op, y.asAny())
		
	binop = compop
	
	def getitem(self, index):
		return self.domethod("__getitem__", index.asAny())
		
	def setitem(self, index, value):
		return self.domethod("__getitem__", index.asAny(), value.asAny()).value
				
	def getattr(self, name):
		return self.domethod("__getattr__", jast.StringConstant(name))
		
	def setattr(self, name, value):
		return self.domethod("__setattr__", jast.StringConstant(name), value.asAny()).value

	def call(self, args, keyargs=None):
		nargs = len(args)
		if keyargs is None or len(keyargs) == 0:
			if nargs == 0:
				return self.domethod("__call__")
			elif nargs == 1:
				return self.domethod("__call__", args[0].asAny())
			elif nargs == 2:
				return self.domethod("__call__", args[0].asAny(), args[1].asAny())
			else:
				return self.domethod("__call__", mkArray(args))
		else:
			keynames = []
			for name, value in keyargs:
				keynames.append(name)
				args.append(value)
				
			return self.domethod("__call__", mkArray(args), mkStrings(keynames))
		
	def invoke(self, name, args, keyargs):
		if keyargs:
			return self.getattr(name).call(args, keyargs)
		
		name = jast.StringConstant(name)
		nargs = len(args)
		if nargs == 0:
			return self.domethod("invoke", name)
		elif nargs == 1:
			return self.domethod("invoke", name, args[0].asAny())
		elif nargs == 2:
			return self.domethod("invoke", name, args[0].asAny(), args[1].asAny())
		else:
			return self.domethod("invoke", name, mkArray(args))
			
	def dir(self): return dir(self.value)
	
	def makeStatement(self):
		return self.value

class PyInteger(PyObject):
	def makeValue(self, value):
		self.value = self.module.getIntegerConstant(value)
		
class PyFloat(PyObject): pass

class PyString(PyObject):
	def makeValue(self, value):
		self.value = self.module.getStringConstant(value)
		self.string = value
		
	def makeStatement(self):
		return jast.Comment(self.string)




class PyFunction(PyObject):
	def __init__(self, name, args, body, module, frame):
		# Figure out arglist
		arguments = Arguments(args)
		
		# Don't handle a,b style args yet
		init_code = []

		# Add args to funcframe
		funcframe = LocalFrame(frame.globalNamespace)
		for argname in arguments.names:
			funcframe.addlocal(argname)

		# Parse the body
		comp = SimpleCompiler(module, funcframe)
		code = jast.Block([comp.parse(body)])
		
		#globals = frame.frame
		# Set up a code object
		pycode = module.getCodeConstant(name, arguments, funcframe.getlocals(), code)
		
		globals = jast.GetInstanceAttribute(frame.frame, "f_globals")

		# make a new function object (instantiating defaults now)
		self.value = jast.New("PyFunction", [globals, mkArray(arguments.defaults), pycode])


class PyClass(PyObject):
	def __init__(self, name, bases, body, parent):
		classframe = LocalFrame(parent.frame.globalNamespace, DynamicStringReference)
		comp = SimpleCompiler(parent.module, classframe)
		code = jast.Block([comp.parse(body), jast.Return(jast.Invoke(classframe.frame, "getf_locals", []))])
		
		pycode = parent.module.getCodeConstant(name, Arguments([]), classframe.getlocals(), code)
		
		self.value = jast.InvokeStatic("Py", "makeClass", 
		    [jast.StringConstant(name), mkArray(bases), pycode, jast.Null])

data = """
print 2, 2+2

print 1<2
print 1<2<3
print 3<2
print 1<3<2

print 2*8-9, 6/3

print 'testing 1, 2, 3'

while 1:
	print 2+2
	print 'done'
else:
	print 'finished loop'
	
if 1:
	print 'true'
else:
	print 'false'

x = 99
print x
y = x+9
print x, y

i = 0
while i < 5:
	print i
	i = i+1
	
while i < 10:
	print i
	i = i+1
	if i == 8: break
	
for i in range(9):
	print i
else:
	print 'bye'
"""
"""
print len
print [1,2,3]
print len([1,2,3])
print len(1,2,3,4)

import string
print string.join(["a", "b"])

#from string import split
print string.split, string.split("a b c d e")
"""
data1 = """
def foo(x=99):
	return x*2

#print foo(5)
"""
data1 = """
class Bar:
	print 2+2
	def foo(self): return 99
"""
data = """
d = {'a':1, 'b':2}
print d
"""

def getdata(filename):
	fp = open(filename, "r")
	data = fp.read()
	fp.close()
	return data
	
data = getdata("c:\\jpython\\tools\\jpythonc2\\test\\ButtonDemo.py")
#data = getdata("c:\\jpython\\demo\\applet\\ButtonDemo.py")

mod = BasicModule("foo")
pi = SimpleCompiler(mod)
code = jast.Block(pi.execstring(data))
mod.addMain(code, pi)

mod.dump("c:\\jpython\\Tools\\jpythonc2\\test")