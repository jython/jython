import java
from java.io import File
import javapath
path = javapath

error = 'os.error'

name = 'jdk1.1'
curdir = '.'
pardir = '..' #This might not be right...
#curdir, pardir??
sep = java.io.File.separator
pathsep = java.io.File.pathSeparator
#defpath?

#I can do better than this...
environ = {}


def _exit(n=0):
	java.lang.System.exit(n)

def getcwd():
	foo = File(File("foo").getAbsolutePath())
	return foo.getParent()

def listdir(path):
	l = File(path).list()
	if l is None:
		raise error, 'No such directory'
	return list(l)

def mkdir(path):
	if not File(path).mkdir():
		raise error, "couldn't make directory"
		
def makedirs(path):
	if not File(path).mkdirs():
		raise error, "couldn't make directories"

def remove(path):
	if not File(path).delete():
		raise error, "couldn't delete file"

def rename(path, newpath):
	if not File(path).renameTo(File(newpath)):
	    raise error, "couldn't rename file"

def rmdir(path):
	if not File(path).delete():
		raise error, "couldn't delete directory"

unlink = remove

def stat(path):
	"""The Java stat implementation only returns a small subset of
	the standard fields"""

	file = java.io.File(path)
	mtime = file.lastModified()
	size = file.length()

	return (0,0,0,0,0,0, size, mtime, mtime, 0)
