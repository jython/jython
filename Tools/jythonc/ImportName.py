import string
from org.python.core import PyModule, PyJavaClass, PyClass, PyJavaPackage, PyBeanEventProperty

def importName(name):
	try:
		names = string.split(name, '.')
		top = __import__(names[0])
		for name in names[1:]:
			top = getattr(top, name)
		return top
	except "fake": #ImportError, AttributeError:
		return None
	
def lookupName(name):
	mod = importName(name)
	if mod is None: #Might want to do something nicer here
		return None
	
	if isinstance(mod, PyModule):
		return Module(mod)
	elif isinstance(mod, PyJavaClass):
		return JavaClass(mod)
	elif isinstance(mod, PyClass):
		return Class(mod)
	elif isinstance(mod, PyJavaPackage):
		return Package(mod)
	else:
		return Namespace(mod)
		

class Namespace:
	def __init__(self, mod):
		if hasattr(mod, '__name__'):
			self.name = mod.__name__
		else:
			self.name = "<unknown>"
		
	def addEvents(self, attrs, events): pass

class Package(Namespace): pass
class Class(Namespace): pass

class Module(Namespace):
	def __init__(self, mod):
		Namespace.__init__(self, mod)
		self.file = mod.__file__
		
class JavaClass(Namespace):
	def __init__(self, mod):
		Namespace.__init__(self, mod)	
		self.file = None
		self.bases = self.findBases(mod)
		self.eventProperties = self.findEventProperties(mod)
	
	def findBases(self, c):
		bases = []
		for base in c.__bases__:
			bases.append(JavaClass(base))
		return bases

	def findEventProperties(self, c):
		eps = {}
		for name, value in c.__dict__.items():
			if isinstance(value, PyBeanEventProperty):
				eps[name] = value.eventClass
		return eps

	def addEvents(self, attrs, events):		
		for name, value in self.eventProperties.items():
			if attrs.has_key(name):
				events[name] = value
		
		for base in self.bases:
			base.addEvents(attrs, events)
		

	