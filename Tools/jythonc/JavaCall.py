import java, org.python.core
import jast
import string

funcs = {}
def call(func, this, args=None):
	if not funcs.has_key(func):
		jf = JavaFunction(func)
		funcs[func] = jf
	else:
		jf = funcs[func]
		
	if args is None:
		return jf.call(this)
	else:
		return jf.invoke(this, args)
	

class Signature:
	def __init__(self, name, inTypes, retType, isStatic=0, isPyArgs=0, isConstructor=0):
		self.isPyArgs = isPyArgs
		self.isStatic = isStatic
		self.retType = retType
		self.inTypes = inTypes
		self.name = name
		self.isConstructor = isConstructor
	
	def handle(self, callee, args, isa=1):
		#if self.isPyArgs:
		#	pyargs = []
		#	for arg in args:
		#		pyargs.append(arg.asa(AnyType).getCode())
		#	return self.mkcall(callee, [Statement.PyObjectArray(pyargs)] )
		
		if len(args) != len(self.inTypes): return None

		callargs = []
		for i in range(len(args)):
			#print i, args[i]
			if isa:
				callarg = args[i].isa(self.inTypes[i])
			else:
				callarg = args[i].asa(self.inTypes[i])
			if callarg == None: return None

			callargs.append(callarg)
			
		return self.mkcall(callee, callargs)
		
	def mkcall(self, callee, args):
		if self.isConstructor:
			op = jast.New(self.name, args)
		elif self.isStatic:
			op = jast.InvokeStatic(callee, self.name, args)
		else:
			#print callee, self.name, args
			op = jast.Invoke(callee, self.name, args)

		return op, self.retType

	def __repr__(self):
		r = []
		for arg in self.inTypes:
			r.append(arg.name)
		return '('+string.join(r, ', ')+')->'+self.retType.name

class JavaFunction:
	def __init__(self, reflectedFunction):
		self.name = reflectedFunction.__name__
		self.isConstructor = isinstance(reflectedFunction, org.python.core.PyReflectedConstructor)
		sigs = []
		self.callee = None
		for i in range(reflectedFunction.nargs):
			sigs.append(self.makeSignature(reflectedFunction.argslist[i]))
		self.sigs = sigs
		self.function = reflectedFunction
		self.declaringClass = self.function.argslist[0].declaringClass
		
	def makeSignature(self, rargs):
		inTypes = list(rargs.args)
		isPyArgs = rargs.flags == rargs.PyArgsCall
		isStatic = rargs.isStatic
		name = rargs.data.name
			
		if self.isConstructor:
			outType = rargs.data.declaringClass			
		else:
			outType = rargs.data.returnType
		
		if isStatic:
			self.callee = rargs.data.declaringClass.__name__
		
		return Signature(name, inTypes, outType, isStatic, isPyArgs, self.isConstructor)
		
	def __repr__(self):
		if len(self.sigs) == 1:
			return self.name+repr(self.sigs[0])
		else:
			return self.name+repr(self.sigs)

	def call(self, args):
		for isa in [1, 0]:
			for sig in self.sigs:
				ret = sig.handle(self.callee, args, isa=isa)
				if ret is None: continue
				return ret
			
		raise TypeError, repr(args)+' args do not match: '+repr(self)

	def invoke(self, this, args):
		this = this.asa(self.declaringClass)
		for isa in [1, 0]:
			for sig in self.sigs:
				ret = sig.handle(this, args, isa=isa)
				if ret is None: continue
				return ret
			
		raise TypeError, repr(args)+' args do not match: '+repr(self)


if __name__ == '__main__':
	f = JavaFunction(org.python.core.PyObject._add)
	print f
	
	class Object:
		def __init__(self, code, istypes, astypes):
			self.code = code
			self.istypes = istypes
			self.astypes = astypes
		
		def isa(self, type):
			if type in self.istypes:
				return self.code
				
		def asa(self, type):
			if type in self.istypes:
				return self.code
				
			for astype, ascode in self.astypes:
				if astype == type: return ascode
				
	one = Object(jast.IntegerConstant(1), [java.lang.Integer.TYPE], 
				[(org.python.core.PyObject, jast.Identifier("one"))])
	foo = Object(jast.Identifier("foo"), [org.python.core.PyObject],
				[(java.lang.Integer.TYPE, jast.InvokeStatic("Py", "toint", [jast.Identifier("foo")]))])
	
	print f.invoke(foo, [one])
	print f.invoke(foo, [foo])
	
	print call(org.python.core.Py.py2int, [one])
	
