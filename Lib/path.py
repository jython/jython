import java
from java.io import File

def exists(path):
	return File(path).exists()
	
def isabs(path):
	return File(path).isAbsolute()
	
def isfile(path):
	return File(path).isFile()
	
def isdir(path):
	return File(path).isDirectory()
	
def join(*args):
	path = File(args[0])
	for d in args[1:]:
		path = File(path, d)
	return path.getPath()
	

def normcase(path):
	#Java guarantees case-sensitive files?
	return path
	
del File
	