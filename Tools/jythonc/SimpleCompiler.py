from BaseEvaluator import BaseEvaluator
from PythonVisitor import Arguments
import string
import jast

def clean(node):
	if not hasattr(node, 'exits'): print node
	if node.exits(): return node
	
	return jast.Block([node, jast.Return(jast.GetStaticAttribute('Py', 'None'))])

def mkStrings(values):
	lst = []
	for value in values:
		if value is None:
			jv = jast.Null
		else:
			jv = jast.StringConstant(value)
			
		lst.append(jv)
	return jast.FilledArray("String", lst)


from java.lang.Character import isJavaIdentifierPart
def legalJavaName(name):
	letters = []
	for c in name:
		if isJavaIdentifierPart(c): letters.append(c)
	if len(letters) == 0: return "x"
	elif len(letters) == len(name): return name
	else: return string.join(letters, '')


class BasicModule:
	def __init__(self, name, filename="<unknown>", packages = []):
		self.name = name
		self.filename = filename
		self.superclass = "Object"
		self.interfaces = ["InitModule"]
		self.strings = {}
		self.integers = {}
		self.floats = {}
		self.temps = []
		self.codes = []
		
		self.constants = []
		
		self.funccodes = []
		self.uniquenames = []
		
		self.prefix = "private static final PyObject"
		self.package = None
		
		self.javamain = 1

		self.attributes = {}
		self.classes = {}
		self.imports = {}
		
		self.packages = packages
		self.modifiers = "public"

	def addAttribute(self, name, value):
		self.attributes[name] = value

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
			
	def getFloatConstant(self, value):
		if self.floats.has_key(value):
			return self.floats[value]
		ret = jast.Identifier("f$%d" % len(self.floats))
		self.floats[value] = ret
		return ret

	def getCodeConstant(self, name, args, locals, code):
		label = "c$%d_%s" % (len(self.codes), legalJavaName(name))
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
		
	def dumpFloats(self):
		items = self.floats.items()
		items.sort()
		for value, label in items:
			self.addConstant("PyObject", label, jast.InvokeStatic("Py", "newFloat", [jast.FloatConstant(value)]))
		
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
		self.dumpFloats()
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
		self.funccodes.append( (legalJavaName(name), len(self.funccodes), code) )
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
		
		args = [jast.StringConstant(self.name), jast.Identifier('dict')]
		initcode = jast.Block([jast.InvokeStatic("Py", "initRunnable", args)])
		meths.append(jast.Method("initModule", "public", 
			["void", ("PyObject", "dict")], initcode))		
	
		return meths

	def getProperties(self):
		props = [
			"python.packages.paths", "",
			"python.packages.directories", "",
			"python.options.classExceptions", "false",
			"python.options.showJavaExceptions", "true",
		]
		
		return mkStrings(props)
		
	def getPackages(self):
		packs = []
		for p in self.packages:
			packs.append(p)
			packs.append(None)
		return mkStrings(packs)

	def dumpAll(self):
		return [self.dumpConstants(),
				jast.Blank, self.dumpMain(), self.dumpFuncs()]

	def makeClass(self):
		body = jast.Block(self.dumpAll())
		return jast.Class(self.name, self.modifiers, self.superclass, self.interfaces, body)

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
		

class Reference:
	def __init__(self, frame, name):
		self.frame = frame.frame
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
			#PyObject(self.getCode(), None)
		return self.setCode(value)


class DynamicIntReference(Reference):
	def init(self):
		self.ivalue = jast.IntegerConstant(len(self.locals))
		
	def getCode(self):
		return jast.Invoke(self.frame, "getlocal", (self.ivalue,))
		
	def setCode(self, value):
		return jast.Invoke(self.frame, "setlocal", (self.ivalue, value.asAny()))

class DynamicStringReference(Reference):
	def init(self):
		self.ivalue = jast.StringConstant(self.name)
		
	def getCode(self):
		return jast.Invoke(self.frame, "getname", (self.ivalue,))
		
	def setCode(self, value):
		return jast.Invoke(self.frame, "setlocal", (self.ivalue, value.asAny()))


class DynamicGlobalStringReference(Reference):
	def init(self):
		self.ivalue = jast.StringConstant(self.name)
		
	def getCode(self):
		return jast.Invoke(self.frame, "getglobal", (self.ivalue,))
		
	def setCode(self, value):
		return jast.Invoke(self.frame, "setglobal", (self.ivalue, value.asAny()))

	def noValue(self):
		# Reference to builtin
		return PyObject(self.getCode())

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

class GlobalFrame(LocalFrame):
	def __init__(self, globals):
		LocalFrame.__init__(self, globals)

	def getReference(self, name):
		return self.globalNamespace.getReference(self, name)


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

GenericGlobalFrame = GlobalFrame(BasicGlobals())

class SimpleCompiler(BaseEvaluator):
	def __init__(self, module, frame=None):
		BaseEvaluator.__init__(self)
		
		if frame is None:
			frame = GenericGlobalFrame
		self.frame = frame
		self.module = module
		self.nthrowables = 0

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
		return PyObject(tmp, self), setit
		
	def freeTemp(self, tmp):
		self.frame.freetemp(tmp.asAny())

	#primitive values
	def int_const(self, value):
		return PyInteger(value, self)
		
	def float_const(self, value):
		return PyFloat(value, self)
		
	def string_const(self, value):
		return PyString(value, self)
	
	# builtin types
	def make_seq(self, name, values):
		ret = []
		for value in values:
			ret.append(self.visit(value))
		lst = jast.New(name, [mkArray(ret)])
		return PyObject(lst, self)

	def list_op(self, values):
		return self.make_seq('PyList', values)
		
	def tuple_op(self, values):
		return self.make_seq('PyTuple', values)
		
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

	def get_module(self, names, top=0):
		ret = PyModule(names[0], self)
		top = ret
		
		for part in names[1:]:
			top = top.getattr(part)
		if top: return top
		else: return ret

	def getSlice(self, index):
		indices = self.visitor.getSlice(index)
		ret = []
		for index in indices:
			if index is None:
				ret.append(PyObject(jast.Null, self))
			else:
				ret.append(self.visit(index))
		return ret


	def and_op(self, x, y):
		tmp = self.frame.gettemp("PyObject")
		test = jast.Invoke(jast.Set(tmp, self.visit(x).asAny()), "__nonzero__", [])
		op = PyObject(jast.TriTest(test, tmp, self.visit(y).asAny()), self)
		self.frame.freetemp(tmp)
		return op
		

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
				gety = PyObject(jast.Set(tmp, y.asAny()), self)
			else:
				gety = y
			
			thistest = x.compop(op, gety)
			if test is None:
				test = thistest
			else:
				test = PyObject(jast.TriTest(test.nonzero(), thistest.asAny(), x.asAny()), self)
			if tmp:
				if not firsttime:
					self.frame.freetemp(x.asAny())
				x = PyObject(tmp, self)
			firsttime = 0
		if tmp:
			self.frame.freetemp(tmp)
		return test

	def pass_stmt(self):
		return jast.SimpleComment("pass")

	def continue_stmt(self):
		return jast.Continue()
		
	def break_stmt(self):
		return jast.Break()
		
	def return_stmt(self, value):
		return jast.Return(self.visit(value).asAny())
		
	def raise_stmt(self, values):
		args = mkAnys(self.visitall(values))
		return jast.Throw(jast.InvokeStatic("Py", "makeException", args))

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
		
	def tryfinally(self, body, finalbody):
		return jast.TryFinally(jast.Block(self.visit(body)), jast.Block(self.visit(finalbody)))

	def tryexcept(self, body, exceptions, elseClause=None):
		if elseClause is not None:
			raise ValueError, "else not supported for try/except"
		
		jbody = jast.Block(self.visit(body))
		tests = []
		ifelse = None
		
		tname = jast.Identifier("x$%d" % self.nthrowables)
		self.nthrowables = self.nthrowables + 1
		
		exctmp = self.frame.gettemp("PyException")
		setexc = jast.Set(exctmp, jast.InvokeStatic("Py", "setException", [tname, self.frame.frame]))
		
		for exc, ebody in exceptions:
			if exc is None:
				ifelse = jast.Block(self.visit(ebody))
				continue

			t = jast.InvokeStatic("Py", "matchException", [exctmp, exc[0].asAny()])
			newbody = []
			if len(exc) == 2:
				newbody.append(self.set(exc[1], exceptionValue))
				
			newbody.append(self.visit(ebody))
			
			tests.append( (t, jast.Block(newbody)) )


		if ifelse is None:
			ifelse = jast.Throw(exctmp)
			
		if len(tests) == 0:
			catchBody = ifelse
		else:
			catchBody = jast.MultiIf(tests, ifelse)

		catchBody = jast.Block([setexc, catchBody])
		
		self.frame.freetemp(exctmp)

		return jast.TryCatch(jbody, "Throwable", tname, catchBody)


	def for_stmt(self, index, sequence, body, else_body=None):			
		counter = self.frame.gettemp('int')
		item = PyObject(self.frame.gettemp(PyObject.type), self)
		seq = self.frame.gettemp(PyObject.type)
		
		init = []
		init.append( jast.Set(counter, jast.IntegerConstant(0)) )
		init.append( jast.Set(seq, self.visit(sequence).asAny()) )
		
		counter_inc = jast.PostOperation(counter, '++')
		
		test = jast.Set(item.asAny(), jast.Invoke(seq, "__finditem__", [counter_inc]))
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
		func = PyFunction(name, args, body, self, doc)
		return self.set_name(name, func)
		
	def lambdef(self, args, body):
		func = PyFunction("<lambda>", args, body, self)
		return func

	def classdef(self, name, bases, body, doc=None):
		c = PyClass(name, bases, body, self)
		self.module.classes[c.name] = c
		return self.set_name(name, c)
		
	def addModule(self, mod):
		print 'add module', mod.name, mod
		self.module.imports[mod.name] = mod
	
	def addSetAttribute(self, obj, name, value):
		self.module.addAttribute(name, value)
		


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
		return PyObject(jast.Set(frame.gettemp(self.type), self.asAny()), self.parent)
	def freeTemp(self, frame):
		frame.freetemp(self.value)

	def print_line(self):
		#print self.value
		return jast.InvokeStatic("Py", "println", [self.asAny()])
		
	def print_continued(self):
		#print self.value,	
		return jast.InvokeStatic("Py", "printComma", [self.asAny()])

	def __init__(self, value, parent=None):
		self.parent = parent
		self.makeValue(value)
		
	def makeValue(self, value):
		self.value = value
		
	def nonzero(self):
		return self.domethod("__nonzero__").value
		
	def unop(self, op):
		return self.domethod('__'+op+'__')
		
	def domethod(self, name, *args):
		return PyObject(jast.Invoke(self.asAny(), name, args), self.parent)

	def compop(self, op, y):
		#print 'comp', self.value, op, y.value
		return self.domethod("_"+op, y.asAny())
		
	binop = compop
	
	def igetitem(self, index):
		return self.domethod("__getitem__", jast.IntegerConstant(index))

	def getslice(self, start, stop, step):
		print start, stop, step
		return self.domethod("__getslice__", start.asAny(), stop.asAny(), step.asAny())
		
	def setslice(self, start, stop, step, value):
		return self.domethod("__setslice__", start.asAny(), stop.asAny(), step.asAny(), value)

	def getitem(self, index):
		return self.domethod("__getitem__", index.asAny())
		
	def setitem(self, index, value):
		return self.domethod("__setitem__", index.asAny(), value.asAny()).value
				
	def getattr(self, name):
		return self.domethod("__getattr__", jast.StringConstant(name))
		
	def setattr(self, name, value):
		if self.parent is not None:
			self.parent.addSetAttribute(self, name, value)
		else:
			print 'missing parent for', self, name, value
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
				if self.parent is not None:
					self.parent.addSetAttribute(self, name, value)
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

	def makeReference(self, code):
		return PyObject(code, self.parent)
		
	def mergeWith(self, other):
		# In simplest world, all types can be merged with each other
		return self

class PyInteger(PyObject):
	def makeValue(self, value):
		self.value = self.parent.module.getIntegerConstant(value)
		
class PyFloat(PyObject):
	def makeValue(self, value):
		self.value = self.parent.module.getFloatConstant(value)

class PyString(PyObject):
	def makeValue(self, value):
		self.value = self.parent.module.getStringConstant(value)
		self.string = value
		
	def makeStatement(self):
		return jast.Comment(self.string)

class ShadowReference(PyObject):
	def __init__(self, code, parent, reference):
		self.value = code
		self.parent = parent
		self.reference = reference
		
	def getattr(self, name):
		return ShadowReference(PyObject.getattr(self, name).value, self.parent, 
				self.reference.getattr(name))

	def invoke(self, name, args, keyargs):
		return self.getattr(name).call(args, keyargs)

from ImportName import lookupName
class PyModule(PyObject):
	def __init__(self, name, parent, code=None, mod=None):
		self.parent = parent
		self.name = name
		
		if code is None: 
			code = jast.InvokeStatic("imp", "load", [jast.StringConstant(name)])
		self.value = code
		if mod is None:
			mod = lookupName(name)
		self.mod = mod
		
		parent.addModule(self)
		
	def makeReference(self, code):
		return ShadowReference(code, self.parent, self)

	def __repr__(self):
		return "<mod %s>" % self.name
		
	def getattr(self, name):
		ret = PyObject.getattr(self, name)
		newmod = PyModule(self.name+'.'+name, self.parent, ret.value)
		return newmod

class PyFunction(PyObject):
	def __init__(self, name, args, body, parent, doc=None):
		# Figure out arglist
		arguments = args
		#print 'func', name, args, doc
		
		# Don't handle a,b style args yet
		init_code = []

		self.parent = parent
		frame = parent.frame
		# Add args to funcframe
		funcframe = LocalFrame(frame.globalNamespace)
		for argname in arguments.names:
			funcframe.setname(argname, PyObject(None, parent))
			#funcframe.addlocal(argname)


		# Parse the body
		comp = SimpleCompiler(parent.module, funcframe)
		code = jast.Block([comp.parse(body)])
		
		#globals = frame.frame
		# Set up a code object
		pycode = parent.module.getCodeConstant(name, arguments, funcframe.getlocals(), code)
		
		globals = jast.GetInstanceAttribute(parent.frame.frame, "f_globals")

		self.frame = funcframe
		self.doc = doc
		self.args = arguments

		# make a new function object (instantiating defaults now)
		self.value = jast.New("PyFunction", [globals, mkArray(arguments.defaults), pycode])

	def makeReference(self, code):
		return ShadowReference(code, self.parent, self)

class PyClass(PyObject):
	def __init__(self, name, bases, body, parent):
		classframe = LocalFrame(parent.frame.globalNamespace, DynamicStringReference)
		comp = SimpleCompiler(parent.module, classframe)
		code = jast.Block([comp.parse(body), jast.Return(jast.Invoke(classframe.frame, "getf_locals", []))])
		
		pycode = parent.module.getCodeConstant(name, Arguments([]), classframe.getlocals(), code)
		self.parent = parent
		self.name = name
		self.bases = bases
		self.classframe = classframe
		self.value = jast.InvokeStatic("Py", "makeClass", 
		    [jast.StringConstant(name), mkArray(bases), pycode, jast.Null])

	def makeReference(self, code):
		return ShadowReference(code, self.parent, self)
