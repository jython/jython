import java
from java.io import File
import javapath
path = javapath

error = OSError

name = 'java' # descriminate based on JDK version?
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
	raise OSError(0, 'No such directory', path)
    return list(l)

def mkdir(path):
    if not File(path).mkdir():
	raise OSError(0, "couldn't make directory", path)

def makedirs(path):
    if not File(path).mkdirs():
	raise OSError(0, "couldn't make directories", path)

def remove(path):
    if not File(path).delete():
	raise OSError(0, "couldn't delete file", path)

def rename(path, newpath):
    if not File(path).renameTo(File(newpath)):
	raise OSError(0, "couldn't rename file", path)

def rmdir(path):
    if not File(path).delete():
	raise OSError(0, "couldn't delete directory", path)

unlink = remove

def stat(path):
    """The Java stat implementation only returns a small subset of
    the standard fields"""
    f = File(path)
    size = f.length()
    # Sadly, if the returned length is zero, we don't really know if the file
    # is zero sized or does not exist.
    if size == 0 and not f.exists():
        raise OSError(0, 'No such file or directory', path)
    mtime = f.lastModified()
    return (0, 0, 0, 0, 0, 0, size, mtime, mtime, 0)
