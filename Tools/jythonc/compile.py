from SimpleCompiler import BasicModule, SimpleCompiler
import jast
import ImportName
import os, string
from types import *
import java, org

import javac
import proxies

from java.lang import *

def getdata(filename):
	fp = open(filename, "r")
	data = fp.read()
	fp.close()
	return data

def getsig(doc):
	if doc is None: return None
	
	lines = doc.split("\n")
	txt = None
	for line in lines:
		line = line.strip()
		if line.startswith("@sig "):
			txt = line[5:]
			break
	if txt is None: return None
	front, back = txt.split("(")
	back = back[:-1]
	front = front.split()
	ret = front[-2]
	mods = string.join(front[:-2], ' ')
	
	arglist = []
	args = back.split(',')
	for arg in args:
		c, name = arg.split()
		jc = getJavaClass(c)
		arglist.append( (jc, name) )
	
	return [mods, getJavaClass(ret), arglist]

primitives = {'void':Void.TYPE, 'int':Integer.TYPE, 'byte':Byte.TYPE, 
	'short':Short.TYPE, 'long':Long.TYPE, 'float':Float.TYPE, 'double':Double.TYPE,
	'boolean':Boolean.TYPE, 'char':Character.TYPE}

def getJavaClass(c):
	if isinstance(c, StringType):
		if primitives.has_key(c): return primitives[c]
		return Class.forName(c)
	elif isinstance(c, ImportName.JavaClass):
		return Class.forName(c.name)
	elif isinstance(c, Class):
		return c
	else:
		return None


def makeJavaProxy(pyc):
	#print pyc
	frame = pyc.classframe
	#print frame.names
	methods = []
	for name, func in frame.names.items():
		v = func.value
		if hasattr(v, 'reference'): v = v.reference
		#print name, v
		args = None
		if hasattr(v, 'args'):
			args = v.args
		sig = None
		if hasattr(v, 'doc'):
			sig = getsig(v.doc)
			
		methods.append( (name, args, sig) )

	bases = []
	for base in pyc.bases:
		#print 'base', base
		if hasattr(base, 'reference'):
			base = base.reference
		if hasattr(base, 'mod'):
			mod = base.mod
			jc = getJavaClass(mod)
			if jc is not None:
				bases.append(jc)

	if len(bases) == 0:
		return None

	module = pyc.parent.module
	print methods
	print bases
	print module

	jp = proxies.JavaProxy(pyc.name, bases, methods, module)
	return jp # jp.makeClass()



class Compiler:
	def __init__(self):
		self.packages = {}
		self.events = {}
		self.depends = {}
		self.modules = {}
		self.javasources = []
		self.files = []
		
		self.deep = 1
		
	def compilefile(self, filename, name):
		filename = java.io.File(filename).getCanonicalPath()
		
		if self.modules.has_key(filename): return
		print 'compilefile', filename, name
		
		self.modules[filename] = 1
		mod = self.compile(getdata(filename), filename, name)
		self.modules[filename] = mod

	def compile(self, data, filename, name):
		mod = BasicModule(name)
		pi = SimpleCompiler(mod)
		code = jast.Block(pi.execstring(data))
		mod.addMain(code, pi)
		
		self.addDependencies(mod)

		return mod
		
	def addDependencies(self, mod):
		print 'attrs', mod.attributes
		for name, value in mod.imports.items():
			m = value.mod
			print 'depend', name, m
			#if hasattr(m, 'reference'):
			#	m = m.reference
			if isinstance(m, ImportName.Package):
				self.packages[name] = 1
			elif isinstance(m, ImportName.Module):
				self.depends[m.file] = name
			elif isinstance(m, ImportName.JavaClass):
				m.addEvents(mod.attributes, self.events)
				
		if self.deep:
			for filename, name in self.depends.items():
				self.compilefile(filename, name)

	def filterpackages(self):
		prefixes = {}
		for name in self.packages.keys():
			parts = name.split('.')
			for i in range(1, len(parts)):
				prefixes[string.join(parts[:i], '.')] = 1
		#print prefixes
		for name in self.packages.keys():
			if prefixes.has_key(name):
				del self.packages[name]
	

	def dump(self, outdir):
		self.filterpackages()
		print self.packages.keys()
		adapters = {}
		for meth, jc in self.events.items():
			adapters[jc.__name__] = 1
		print adapters.keys()
		for adapter in adapters.keys():
			self.makeAdapter(outdir, adapter)
			
		for filename, mod in self.modules.items():
			print filename, mod, mod.name
			proxyClasses = []
			mainProxy = None
			for name, pyc in mod.classes.items():
				
				print name, pyc
				proxy = makeJavaProxy(pyc)
				if name == mod.name:
					mainProxy = proxy
				elif proxy is not None:
					proxyClasses.append(proxy)
		
			mod.packages = self.packages.keys()			
			top = mod
			specialClasses = []
			if mainProxy is not None:
				mod.name = "py"
				mod.modifiers = "public static"				
				top = mainProxy
				top.innerClasses.append(mod.makeClass())
				print 'top', mod.packages
				top.packages = mod.getPackages()
				specialClasses.append(top.name+'.'+top.name)
				specialClasses.append(top.name)


			for proxy in proxyClasses:
				proxy.modifier = "public static"
				top.innerClasses.append(proxy.makeClass())
				specialClasses.append(top.name+'.'+proxy.name)
				specialClasses.append(top.name+'$'+proxy.name)

			top.specialClasses = specialClasses
			top.dump(outdir)
			self.javasources.append(os.path.join(outdir, top.name+".java"))
		self.java2class()
	
	def java2class(self):
		print 'compiling java files', self.javasources
		code, outtext, errtext = javac.compile(self.javasources)
		for jfile in self.javasources:
			self.files.append(os.path.splitext(jfile)[0]+'.class')


	def makeAdapter(self, outdir, proxy):
		os = java.io.ByteArrayOutputStream()
		org.python.compiler.AdapterMaker.makeAdapter(proxy, os)
		filename = writeclass(outdir, 'org.python.proxies.'+proxy+'$Adapter', os)
		self.files.append(filename)


from java.io import *
def writefile(filename, instream):
	file = File(filename)
	File(file.getParent()).mkdirs()
	outstream = FileOutputStream(file)	
	instream.writeTo(outstream)
	
def writeclass(outdir, classname, stream):
	filename = apply(os.path.join, tuple(classname.split('.')))+'.class'
	filename = os.path.join(outdir, filename)
	writefile(filename, stream)
	return filename

def compile(files, outdir):
	c = Compiler()
	for filename, classname in files:
		c.compilefile(filename, classname)
	c.dump(outdir)
	return c.files
	
if __name__ == '__main__':
	import sys
	print sys.argv
	filenames = sys.argv[1:]
	print filenames
	outdir = "."
	files = []
	for filename in filenames:
		outdir = os.path.dirname(filename)
		classname = os.path.splitext(os.path.basename(filename))[0]
		files.append( (filename, classname) )
	print compile(files, outdir)