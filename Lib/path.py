import java
from java.io import File

def dirname(path):
	return File(path).getParent()

def basename(path):
	return File(path).getName()

def split(path):
	f = File(path)
	return (f.getParent(), f.getName())

def splitext(path):
	i = 0
	n = -1
	for c in path:
		if c == '.': n = i
		i = i+1
	if n < 0:
		return (path, "")
	else:
		return (path[:n], path[n:])

def splitdrive(path):
	# What about DOS?
	return ("", path)

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
		if File(d).isAbsolute():
			path = d
		else:
			path = File(path, d)
	return path.getPath()
	

def normcase(path):
	# Java guarantees case-sensitive files?
	# (I don't think so --Guido)
	return path

# Missing:
# commonprefix
# islink
# samefile
# sameopenfile
# samestat
# ismount
# walk
# expanduser
# expandvars
# normpath
