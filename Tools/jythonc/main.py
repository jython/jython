from compile import Compiler
import sys, string, os
from Options import *
		
def addCore(extraPackages):
	skiplist = ['org.python.core.parser', 'org.python.core.BytecodeLoader', 'org.python.core.jpython']
	extraPackages.append( ('org.python.core', skiplist) )

def addAll(extraPackages):
	for name in ['core', 'compiler', 'parser']:
		extraPackages.append( ('org.python.'+name, []) )
		
usage = """\
Usage: jpythonc [-options] module+

where options include:
  -package package    put all compiled code into a java package
  -jar jarfile        compile into jarfile (implies -deep)
  -deep               compile all python dependencies of the module 
  -core               include the core JPython libraries
  -all                include all of the JPython libraries

  -bean jarfile       compile into jarfile, include manifest for bean  
  -addpackages packs  include java dependencies from this list of packages
                      default is org.python.modules and com.oroinc.text.regex
  -workdir directory  working directory for compiler (default is ./jpywork)
  -skip modules       don't include any of these modules in compilation
  -compiler fullpath  use a different compiler than "standard" javac
                      if this is set to "NONE" then compile ends with .java
  -falsenames names   a comma-separated list of names that are always false
                      can be used to short-circuit if clauses"""
                      
options_to_add = """
  -addfiles files     add the comma-separated list of files to the archive
"""


def failed():
	print usage
	sys.exit(-1)	


def getOptions():
	try:
		opts = Options({'jar':None, 'workdir':"jpywork", 'core':YesNo, 'all':YesNo,
				'deep':YesNo, 'bean':None, 'skip':"", 'package':None, 'addfiles':"",
				'falsenames':"", 'compiler':None, 'bean':None, 'addpackages':""})
	except KeyError, value:
		print 'illegal argument: -'+value
		print
		failed()

	if len(opts.args) == 0:
		print 'nothing to compile'
		print
		failed()
		
	if opts.jar is not None:
		opts.deep = 1
	elif opts.core or opts.all:
		opts.deep = 1

	opts.falsenames = opts.falsenames.split(",")
	
	if not os.path.isabs(opts.workdir):
		opts.workdir = os.path.join(".", opts.workdir)	

	return opts

mainclass = basepath = None
def doCompile(opts):
	skiplist = string.split(opts.skip, ",")
	addpackages = ['org.python.modules', 'com.oroinc.text.regex']+string.split(opts.addpackages, ',')
	
	
	comp = Compiler(javapackage=opts.package, deep=opts.deep, include=addpackages, skip=skiplist, options=opts)
	global mainclass
	global basepath
	
	for target in opts.args:
		if target.endswith('.py'):
			classname = os.path.splitext(os.path.basename(target))[0]
			filename = target
			if basepath is None:
				basepath = os.path.split(target)[0]
				sys.path.insert(0, basepath)
		else:
			classname = target
			import ImportName
			m = ImportName.lookupName(classname)
			filename = m.file
		if mainclass is None:
			mainclass = classname
			if opts.package is not None:
				mainclass = opts.package+'.'+mainclass
		comp.compilefile(filename, classname)
		
	comp.dump(opts.workdir)
	return comp

def copyclass(jc, fromdir, todir):
	import jar
	from java.io import FileInputStream, FileOutputStream
	
	name = apply(os.path.join, jc.split('.'))+'.class'
	fromname = os.path.join(fromdir, name)
	toname = os.path.join(todir, name)
	tohead = os.path.split(toname)[0]
	if not os.path.exists(tohead):
		os.makedirs(tohead)
	istream = FileInputStream(fromname)
	ostream = FileOutputStream(toname)
	jar.copy(istream, ostream)
	istream.close()
	ostream.close()


def writeResults(comp, opts):
	global mainclass
	global basepath
	
	javaclasses = comp.javaclasses
	
	if opts.bean is not None:
		jarfile = opts.bean
	else:
		jarfile = opts.jar		
	
	if jarfile is None:
		if not opts.deep and opts.package is not None:
			for jc in javaclasses:
				if isinstance(jc, type( () )):
					jc = jc[0]
				if basepath is None: basepath = '.'
				copyclass(jc, opts.workdir, basepath)
		sys.exit(0)
	
	print 'Building archive:', jarfile
	from jar import JavaArchive
	
	extraPackages = []
	
	if opts.core: addCore(extraPackages)
	if opts.all: addAll(extraPackages)
	
	ja = JavaArchive(extraPackages)
	ja.addToManifest({'Main-Class':mainclass})
	for jc in javaclasses:
		if isinstance(jc, type( () )):
			ja.addClass(opts.workdir, jc[0], jc[1])
		else:
			ja.addClass(opts.workdir, jc)
		
	for dep in comp.trackJavaDependencies():
		ja.addEntry(dep)
		
	ja.dump(jarfile)
	
def main():
	opts = getOptions()
	comp = doCompile(opts)
	writeResults(comp, opts)