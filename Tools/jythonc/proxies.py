import jast
from java.lang.reflect.Modifier import *
from java.lang.reflect import Modifier
from java.lang import *
import org, java
from types import *
import string


def aget(dict, key, default):
	if dict.has_key(key):
		return dict[key]
	else:
		ret = {}
		dict[key] = ret
		return ret

#Utility functions for converting between java and python types
def nullReturn(ret):
	if ret == Void.TYPE:
		return jast.SimpleComment("void")
		
	if ret.isPrimitive():
		value = jast.IntegerConstant(0)
	else:
		value = jast.Null
	return jast.Return(value)

def makeReturn(code, ret):
	if ret == Void.TYPE:
		return code
	
	if ret.isPrimitive():
		r = jast.InvokeStatic("Py", "py2"+ret.__name__, [code])
	else:
		r = jast.InvokeStatic("Py", "tojava", [code, jast.StringConstant(ret.__name__)])
		r = jast.Cast(ret.__name__, r)
		
	return jast.Return(r)

def makeObject(code, c):
	if c in [Integer.TYPE, Byte.TYPE, Short.TYPE, Long.TYPE]:
		mname = "newInteger"
	elif c in [Character.TYPE]:
		mname = "newString"
	elif c in [Float.TYPE, Double.TYPE]:
		mname = "newFloat"
	elif c in [Boolean.TYPE]:
		mname = "newBoolean"
	else:
		return code
	return jast.InvokeStatic("Py", mname, [code])

class JavaProxy:
	def __init__(self, name, bases, methods, module=None):
		self.bases = bases
		self.name = name
		self.methods = methods
		
		self.packages = self.properties = self.specialClasses = jast.Null
		self.modname = "foo"

		self.modifier = "public"

		self.package = None
		self.module = module
		if module is not None:
			self.packages = module.getPackages()
			self.properties = module.getProperties()
			self.specialClasses = module.getSpecialClasses()
			self.modname = module.name

		self.isAdapter = 0
		self.frozen = 1

		self.superclass = Object
		self.interfaces = []

		self.jmethods = {}
		for base in bases:
			self.addMethods(base)
			if base.isInterface():
				self.interfaces.append(base)
			else:
				self.superclass = base
		self.cleanMethods()
		
		self.jconstructors = []
		self.addConstructors(self.superclass)

		self.innerClasses = []

	def dumpAll(self):
		self.statements = []
		self.dumpInnerClasses()
		self.dumpMethods()
		self.dumpConstructors()
		self.addPyProxyInterface()
		return self.statements
		
	def dumpInnerClasses(self):
		for ic in self.innerClasses:
			self.statements.append(ic)
			
	def dumpMethods(self):
		names = self.jmethods.keys()
		names.sort()
		#print 'adding methods', self.name, names
		for name, args, sig in self.methods:
			#print name, args, sig
			if sig is not None:
				self.callMethod(name, sig[0], sig[1], sig[2], 0)
				continue
				
			if not self.jmethods.has_key(name): continue
			
			for sig, (access, ret) in self.jmethods[name].items():
				#print sig, access, ret
				self.callMethod(name, access, ret, sig, 1)
				
			sigs = self.jmethods[name]

	def dumpConstructors(self):
		for access, sig in self.jconstructors:
			self.callConstructor(access, sig)


	def cleanMethods(self):
		for name, value in self.jmethods.items():
			if len(value) == 0:
				del self.jmethods[name]
	
	def addMethods(self, c):
		#print 'adding', c.__name__, c.getDeclaredMethods()
		for method in c.getDeclaredMethods():
			#Check that it's not already included
			name = method.name
			sig = tuple(method.parameterTypes)
			ret = method.returnType
			
			mdict = aget(self.jmethods, name, {})
	
			if mdict.has_key(sig): continue
			
			access = method.modifiers
				
			if isPrivate(access) or isStatic(access):
				continue
				
			if isNative(access):
				access = access & ~NATIVE
			
			if isProtected(access):
				access = access & ~PROTECTED | PUBLIC
				if isFinal(access):
					pass
					#addSuperMethod(method, access)	
			elif isFinal(access):
				continue
			
			mdict[sig] = access, ret
			
		sc = c.getSuperclass()
		if sc is not None:
			self.addMethods(sc)
			
		for interface in c.getInterfaces():
			self.addMethods(interface)


	def addConstructors(self, c):
		for constructor in c.getDeclaredConstructors():
			access = constructor.modifiers
			if isPrivate(access): continue
			if isNative(access): access = access & ~NATIVE
			if isProtected(access): access = access & ~PROTECTED | PUBLIC
			parameters = tuple(constructor.parameterTypes)
			self.jconstructors.append( (access, parameters) )

	def callMethod(self, name, access, ret, sig, dosuper=1):
		args = [ret.__name__]
		argids = []
		objects = []
		for c in sig:
			if isinstance(c, TupleType):
				argname = c[1]
				c = c[0]
			else:
				argname = "arg"+str(len(argids))
			args.append( (c.__name__, argname) )
			argid = jast.Identifier(argname)
			argids.append(argid)
			objects.append(makeObject(argid, c))
	
		objects = jast.FilledArray("Object", objects)
	
		stmts = []
		this = jast.Identifier("this")
	
		if isinstance(access, IntType) and isAbstract(access):
			dosuper = 0
			access = access & ~ABSTRACT
			
		if not dosuper and not self.isAdapter:
			getattr = jast.InvokeStatic("Py", "jgetattr", [this, jast.StringConstant(name)])
		else:
			getattr = jast.InvokeStatic("Py", "jfindattr", [this, jast.StringConstant(name)])
	
		inst = jast.Identifier("inst")
		jcall = makeReturn(jast.Invoke(inst, "_jcall", [objects]), ret)
		
		if dosuper:
			supercall = jast.Invoke(jast.Identifier("super"), name, argids)
			if ret != Void.TYPE:
				supercall = jast.Return(supercall)
				
			supermethod = jast.Method("super__"+name, jast.Modifier.ModifierString(access), 
								args, jast.Block([supercall]))
		else:		
			if self.isAdapter:
				supercall = nullReturn(ret)
			else:
				supercall = None
			supermethod = None

				
		if not dosuper and not self.isAdapter:
			test = jcall
		else:
			test = jast.If(jast.Operation("!=", inst, jast.Null), jcall, supercall)
		code = jast.Block([jast.Declare("PyObject", inst, getattr), test])
		meth = jast.Method(name, jast.Modifier.ModifierString(access), args, code)
		
		if supermethod is not None:
			self.statements.append(supermethod)
			
		self.statements.append(meth)
		
	def callConstructor(self, access, sig):
		args = []
		argids = []
		objects = []
		for c in sig:
			argname = "arg"+str(len(argids))
			args.append( (c.__name__, argname) )
			argid = jast.Identifier(argname)
			argids.append(argid)
			objects.append(makeObject(argid, c))
	
		objects = jast.FilledArray("Object", objects)
	
		stmts = []
		this = jast.Identifier("this")
	
		supercall = jast.InvokeLocal("super", argids)

		#specialClasses = jast.StringArray(self.specialClasses)
		frozen = self.module.getFrozen()
		
		initargs = [this, jast.StringConstant(self.modname), jast.StringConstant(self.name),
				objects, self.packages, self.properties, self.specialClasses, frozen]
		
	
		initproxy = jast.InvokeStatic("Py", "initProxy", initargs)
		
		code = jast.Block([supercall, initproxy])
		self.statements.append(jast.Constructor(self.name, jast.Modifier.ModifierString(access), args, code))

	def addPyProxyInterface(self):
		self.statements.append(jast.Declare('private PyInstance', jast.Identifier('__proxy')))
		code = jast.Set(jast.Identifier("__proxy"), jast.Identifier("inst")) 
		code = jast.Block( [code] )
		self.statements.append(jast.Method("_setPyInstance", "public", ["void", ("PyInstance", "inst")], code))
		code = jast.Block([jast.Return(jast.Identifier("__proxy"))])
		self.statements.append(jast.Method("_getPyInstance", "public", ["PyInstance"], code))
		
		self.statements.append(jast.Declare('private PySystemState', jast.Identifier('__sysstate')))
		code = jast.Set(jast.Identifier("__sysstate"), jast.Identifier("inst")) 
		code = jast.Block( [code] )
		self.statements.append(jast.Method("_setPySystemState", "public", ["void", ("PySystemState", "inst")], code))
		code = jast.Block([jast.Return(jast.Identifier("__sysstate"))])
		self.statements.append(jast.Method("_getPySystemState", "public", ["PySystemState"], code))

		self.interfaces.append(org.python.core.PyProxy)


	def makeClass(self):
		mycode = self.dumpAll()			
		body = jast.Block(mycode)
		return jast.Class(self.name, self.modifier, self.superclass.__name__,
		    map(lambda i: i.__name__, self.interfaces), body)

	def getDescription(self):
		ret = self.name+' extends '+self.superclass.__name__
		if len(self.interfaces) > 0:
			ret = ret+' implements '+string.join(map(lambda i: i.__name__, self.interfaces), ', ')
		return ret


if __name__ == '__main__':
	import java
	methods = [ ("init", None, None), ("enable", None, ("public", Void.TYPE, [(java.awt.Event, 'event')])) ]
	
	jp = JavaProxy("Foo", [java.util.Random], methods) #applet.Applet], methods)
	
	print jast.Block(jp.dumpAll())