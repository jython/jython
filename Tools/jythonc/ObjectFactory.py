import jast
import org, java
import string
from Object import Object, PyObject, Generic

def makeAnys(args):
	ret = []
	for arg in args:
		ret.append(arg.asAny())
	return ret

def PyObjectArray(args):
	aargs = makeAnys(args)
	return jast.FilledArray("PyObject", aargs)

import SimpleCompiler

class ObjectFactory:
	def __init__(self, parent=None):
		self.parent = parent
	#makeModule????

	def importName(self, name):
		ns = PyNamespace(self.parent, name)
		return Object(ns.getNew(), ns)
		
	def makeFunction(self, name, args, body, doc=None):
		func = PyFunction(self.parent, self, name, args, body, doc)
		return Object(func.getNew(), func)
		
	def makeClass(self, name, bases, body, doc=None):
		cls =  PyClass(self.parent, self, name, bases, body, doc)
		return Object(cls.getNew(), cls)

	def makeList(self, items):
		code = jast.New("PyList", [PyObjectArray(items)])
		return Object(code, Generic)
		
	def makeTuple(self, items):
		code = jast.New("PyTuple", [PyObjectArray(items)])
		return Object(code, Generic)
	
	def makeDictionary(self, items):
		lst = []
		for key, value in items:
			lst.append(key); lst.append(value)
			
		code = jast.New("PyDictionary", [PyObjectArray(lst)])
		return Object(code, Generic)
		
	def makeInteger(self, value):
		return Object(self.parent.module.getIntegerConstant(value), PyConstant(value)) 
		
	def makeFloat(self, value):
		return Object(self.parent.module.getFloatConstant(value), PyConstant(value)) 

	def makeString(self, value):
		return Object(self.parent.module.getStringConstant(value), PyConstant(value)) 

	def makeJavaInteger(self, code):
		return Object(code, IntValue)
		
	def makeJavaString(self, code):
		return Object(code, StringValue)

	def makeObject(self, code, value):
		return Object(code, value)
		
	def makePyObject(self, code):
		return Object(code, Generic)

	def makeNull(self):
		return Object(jast.Null, Generic)

	def makeFunctionFrame(self):
		return SimpleCompiler.LocalFrame(self.parent)
		
	def makeClassFrame(self):
		return SimpleCompiler.LocalFrame(self.parent, 
								SimpleCompiler.DynamicStringReference)
	
	def getCompiler(self, frame):
		return SimpleCompiler.SimpleCompiler(self.parent.module, self, frame)

class FixedObject(PyObject): pass

class PyConstant(FixedObject):
	def __init__(self, value):
		self.value = value
		
	def getStatement(self, code=None):
		return jast.Comment(str(self.value))

class PyFunction(FixedObject):
	def __init__(self, parent, factory, name, args, body, doc=None):
		self.name = name
		self.factory = factory
		self.parent = parent
		self.args = args
		self.body = body
		self.doc = doc
		
	def getNew(self):
		globals = jast.GetInstanceAttribute(self.parent.frame.frame, "f_globals")
		pycode = self.makeCode()
		return jast.New("PyFunction", [globals, PyObjectArray(self.args.defaults), pycode])
	
	def makeCode(self):
		# Don't handle a,b style args yet	
		init_code = []
		frame = self.parent.frame
		
		# Add args to funcframe
		funcframe = self.factory.makeFunctionFrame()
		for argname in self.args.names:
			funcframe.setname(argname, self.factory.makePyObject(None))
			#funcframe.addlocal(argname)

		# Parse the body
		comp = self.factory.getCompiler(funcframe)
		code = jast.Block([comp.parse(self.body)])
		
		# Set up a code object
		self.pycode = self.parent.module.getCodeConstant(self.name, self.args, funcframe.getlocals(), code)
		self.frame = funcframe
		
		return self.pycode
		
class PyClass(FixedObject):
	def __init__(self, parent, factory, name, bases, body, doc=None):
		self.name = name
		self.parent = parent
		self.factory = factory
		self.bases = bases
		self.body = body
		self.doc = doc

	def getNew(self):
		pycode = self.makeCode()
		return jast.InvokeStatic("Py", "makeClass", 
		    [jast.StringConstant(self.name), PyObjectArray(self.bases), pycode, jast.Null])

	def makeCode(self):
		classframe = self.factory.makeClassFrame()
		comp = self.factory.getCompiler(classframe)
		code = jast.Block([comp.parse(self.body), jast.Return(jast.Invoke(classframe.frame, "getf_locals", []))])
		self.frame = classframe
		self.pycode = self.parent.module.getCodeConstant(self.name, None, classframe.getlocals(), code)
		return self.pycode
		
class PyNamespace(FixedObject):
	def __init__(self, parent, name):
		self.name = name
		self.parent = parent
		self.parent.addModule(name)

	def getNew(self):
		return jast.InvokeStatic("imp", "load", [jast.StringConstant(self.name)])

	def getattr(self, code, name):
		newobj = FixedObject.getattr(self, code, name)
		newobj.value = PyNamespace(self.parent, self.name+'.'+name)
		return newobj
		
	#Use the base definition of invoke so that getattr will be properly handled
	def invoke(self, code, name, args, keyargs):
		return self.getattr(code, name).call(args, keyargs)


data = """
x=1+1
#import pawt
#print pawt.test

for i in [1,2,3]:
	print i
	
def bar(x):
	print x*10
	y = x+2
	print y

bar(42)

class Baz:
	def eggs(self, x, y, z):
		return x, y, z
		
b = Baz()
print b.eggs(1,2,3)
"""

if __name__ == '__main__':
	mod = SimpleCompiler.BasicModule("foo")
	fact = ObjectFactory()
	pi = SimpleCompiler.SimpleCompiler(mod, fact)
	fact.parent = pi
	code = jast.Block(pi.execstring(data))
	mod.addMain(code, pi)
	
	print mod.attributes.keys()
	print mod.imports.keys()
	print mod
	mod.dump("c:\\jpython\\tools\\jpythonc2\\test")