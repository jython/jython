from java.util.zip import ZipEntry, ZipOutputStream
from java.io import File, FileInputStream, ByteArrayOutputStream, FileOutputStream, ByteArrayInputStream
import string, os, jarray, sys

class DirectoryOutput:
	def __init__(self, dirname):
		self.outdir = dirname
		
	def getFile(self, name):
		fname = apply(os.path.join, tuple([self.outdir]+string.split(name, '.')))+'.class'
		file = File(fname)
		File(file.getParent()).mkdirs()
		return FileOutputStream(file)	
	
	def write(self, name, file):
		fp = self.getFile(name)
		if isinstance(file, ByteArrayOutputStream):
			file.writeTo(fp)
		else:
			if isinstance(file, type('')):
				file = FileInputStream(file)
			data = jarray.zeros(1024*4, 'b')
			while 1:
				n = file.read(data)
				if n == -1: break
				fp.write(data, 0, n)

	def close(self):
		pass			

class ZipOutput(DirectoryOutput):
	def __init__(self, filename):
		self.zipfile = ZipOutputStream(FileOutputStream(filename))
		
	def getName(self, name):
		return string.join(string.split(name, '.'), '/')+'.class'

	def getFile(self, name):
		fname = self.getName(name)
		self.zipfile.putNextEntry(ZipEntry(fname))
		return self.zipfile
		
	def close(self):
		self.zipfile.close()

try:		
	sys.add_package('com.ms.util.cab')
	from com.ms.util import cab
	import com.ms.util.cab.CabCreator
	from java.util import Date
	
	class CabOutput(cab.CabProgressInterface):
		def progress(self, ptype, val1, val2, data):
			pass
			#print 'cab progress made'
	
		def __init__(self, filename):
			self.cabfile = cab.CabCreator(self)
			self.cabfile.create(FileOutputStream(filename))
			folder = cab.CabFolderEntry()
			folder.setCompression(cab.CabConstants.COMPRESSION_LZX, 20)
			#print folder.compressionToString()
			self.cabfile.newFolder(folder)
			
		def getName(self, name):
			return string.join(string.split(name, '.'), '\\')+'.class'
			
		def write(self, name, file):
			fname = self.getName(name)
			entry = cab.CabFileEntry(name=fname, date=Date())
			if isinstance(file, ByteArrayOutputStream):
				file = ByteArrayInputStream(file.toByteArray())
			elif isinstance(file, type('')):
				file = FileInputStream(file)
			
			self.cabfile.addStream(file, entry)	
	
		def close(self):
			self.cabfile.complete()
except AttributeError:
	pass
		
if __name__ == '__main__':
	for of in [CabOutput('c:\\jpython\\test.cab')]: #DirectoryOutput('c:\\jpython\\dtest'), ZipOutput('c:\\jpython\\test.jar')]:
		of.write('org.python.core.PyInteger', 'c:\\jpython\\JavaCode\\org\\python\\core\\PyInteger.class')
		of.write('org.python.core.PyFloat', 'c:\\jpython\\JavaCode\\org\\python\\core\\PyFloat.class')
		bytes = ByteArrayOutputStream()
		bytes.write(jarray.array([10]*500, 'b'))
		of.write('hi.there', bytes)
		of.close()

