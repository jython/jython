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

class JavaArchive:
	def __init__(self):
		self.files = []
		self.classes = []
		
	def addFile(self, rootdir, filename):
		self.files.append( (rootdir, filename) )
		
	def addClass(self, rootdir, classname):
		filename = apply(os.path.join, classname.split('.'))
		self.addFile(rootdir, filename+'.class')
		

	def addPackage(self, package, skipclasses): pass
	def addManifest(self, lines): pass

	def dumpFiles(self):
		for rootdir, filename in self.files:
			infile = os.path.join(rootdir, filename)
			outfile = string.join(filename.split(os.sep), '/')
			instream = FileInputStream(infile)
			self.zipfile.putNextEntry(ZipEntry(outfile))
			copy(instream, self.zipfile)
			instream.close()
			
	def dump(self, filename):
		self.zipfile = ZipOutputStream(FileOutputStream(filename))
		self.dumpFiles()
		self.zipfile.close()
		

if __name__ == '__main__':
	pass