from java.util.zip import *
from java.io import *
import string, os, jarray

def copy(instream, outstream):
	data = jarray.zeros(1024*4, 'b')
	while 1:
		if instream.available() == 0: break
		n = instream.read(data)
		if n == -1: break
		outstream.write(data, 0, n)	

def lookup(name):
	names = name.split('.')
	top = __import__(names[0])
	for name in names[1:]:
		top = getattr(top, name)
	return top


class JavaArchive:
	def __init__(self, packages=[]):
		self.files = []
		self.classes = []
		self.packages = packages
		
	def addFile(self, rootdir, filename):
		self.files.append( (rootdir, filename) )
		
	def addClass(self, rootdir, classname):
		filename = apply(os.path.join, classname.split('.'))
		self.addFile(rootdir, filename+'.class')
		
	def addManifest(self, lines): pass

	def dumpFiles(self):
		for rootdir, filename in self.files:
			infile = os.path.join(rootdir, filename)
			outfile = string.join(filename.split(os.sep), '/')
			instream = FileInputStream(infile)
			self.zipfile.putNextEntry(ZipEntry(outfile))
			copy(instream, self.zipfile)
			instream.close()
			
		for package, skiplist in self.packages:
			self.addPackage(package, skiplist)
			
	def dump(self, filename):
		self.zipfile = ZipOutputStream(FileOutputStream(filename))
		self.dumpFiles()
		self.zipfile.close()
		

	# The next three methods handle packages (typically org.python.core, ...)
	def addPackage(self, package, skiplist=[]):
		pkg = lookup(package)
		if hasattr(pkg, '__file__'):
			return self.addZipPackage(package+'.', pkg.__file__, skiplist)
		elif hasattr(pkg, '__path__') and len(pkg.__path__) == 1:
			return self.addDirectoryPackage(package+'.', pkg.__path__[0], skiplist)
		raise ValueError, "can't find package: "+repr(package)

	def addZipPackage(self, package, zipfile, skiplist):
		zf = ZipFile(FileInputStream(zipfile))
		for entry in zf.entries():
			filename = entry.name
			if filename[-6:] != '.class': continue
			name = string.replace(filename[:-6], '/', '.')

			if name[:len(package)] != package: continue
			self.zipfile.putNextEntry(ZipEntry(filename))
			copy(zf.getInputStream(entry), self.zipfile)
		zf.close()

	def addDirectoryPackage(self, package, directory, skiplist):
		for file in os.listdir(directory):
			if file[-6:] != '.class': continue
			name = package+file[:-6]
			if name in skiplist: continue
			entryname = string.join(name.split('.'), '/')+'.class'
			self.zipfile.putNextEntry(ZipEntry(entryname))
			instream = FileInputStream(os.path.join(directory, file))
			copy(instream, self.zipfile)
			instream.close()


if __name__ == '__main__':
	root = "c:\\jpython\\tools\\jpythonc2"
	ja = JavaArchive()
	ja.addFile(root, "jar.py")
	ja.addFile(root, "proxies.py")
	ja.addClass(root, "jast.Statement$py")
	print ja.files
	ja.dump(os.path.join(root, "test\\t.jar"))