from java import io
import string
import sys, os
from java.util.zip import ZipFile

def fix(n):
	return string.join(string.split(n, '/'), '.')

def getClass(t, names):
	#print t, names
	if t[0] == '(':
		index = 1
		while index < len(t) and t[index] != ')':
			off, value = getClass(t[index:], names)
			index = index+off
		getClass(t[index+1:], names)
	elif t[0] == 'L':
		end = string.index(t, ';')
		value = t[1:end]
		names[fix(value)] = 1
		return end, t[1:end]
	elif t[0] == '[':
		off, ret = getClass(t[1:], names)
		return off+1, ret
	else:
		return 1, None


def dependencies(file):
	data = io.DataInputStream(file)

	data.readInt()
	data.readShort()
	data.readShort()

	n = data.readShort()

	strings = [None]*n
	classes = []
	types = []

	i = 0
	while i < n:
		tag = data.readByte()
		if tag == 1:
			name = data.readUTF()
			strings[i] = name
		elif tag == 7:
			classes.append(data.readShort())
		elif tag == 9 or tag == 10 or tag == 11:
			data.readShort()
			data.readShort()
		elif tag == 8:
			data.readShort()
		elif tag == 3:
			data.readInt()
		elif tag == 4:
			data.readFloat()
		elif tag == 5:
			data.readLong()
			i = i+1
		elif tag == 6:
			data.readDouble()
			i = i+1
		elif tag == 12:
			data.readShort()
			types.append(data.readShort())
		i = i+1
	names = {}

	for c in classes:
		#print c, strings[c-1]
		name = fix(strings[c-1])
		if name[0] == '[': continue
		names[name]= 1

	for t in types:
		getClass(strings[t-1], names)


	return names.keys()

def defaultFilter(name):
	return not (name[:5] == 'java.' or name[:16] == 'org.python.core.')


from util import lookup, getzip
def getFile(name):
	dot = name.rfind('.')
	if dot == -1:
		return topFile(name)
		
	package = lookup(name[:dot])
	if hasattr(package, '__file__'):
		return ZipEntry(package.__file__, name)
	elif hasattr(package, '__path__') and len(package.__path__) == 1:
		return DirEntry(package.__path__[0], name)

class ZipEntry:
	def __init__(self, filename, classname):
		self.filename = filename
		self.classname = classname
		
	def __repr__(self):
		return "ZipEntry(%s, %s)" % (self.filename, self.classname)
		
	def getInputStream(self):
		zf = getzip(self.filename)
		zfilename = string.join(string.split(self.classname, '.'), '/')+'.class'
		entry = zf.getEntry(zfilename)
		return zf.getInputStream(entry)

class DirEntry:
	def __init__(self, dirname, classname):
		self.dirname = dirname
		self.classname = classname
		
	def __repr__(self):
		return "DirEntry(%s, %s)" % (self.dirname, self.classname)

	def getInputStream(self):
		lastname = self.classname.split('.')[-1]
		fullname = os.path.join(self.dirname, lastname+'.class')
		return io.FileInputStream(fullname)


def depends(name):
	ze = getFile(name)
	ip = ze.getInputStream()
	ret = dependencies(ip)
	ip.close()
	return ze, ret

if __name__ == '__main__':
	#print getFile('org.python.modules.strop')
	#print getFile('java.lang.String')

	#print depend('com.oroinc.text.regex.Perl5Matcher')
	for name in ['javax.swing.JButton', 'java.lang.String']:
		print depends(name)