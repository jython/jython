r"""OS routines for Java, with some attempts to support DOS, NT, and
Posix functionality.

This exports:
  - all functions from posix, nt, dos, os2, mac, or ce, e.g. unlink, stat, etc.
  - os.path is one of the modules posixpath, ntpath, macpath, or dospath
  - os.name is 'posix', 'nt', 'dos', 'os2', 'mac', 'ce' or 'riscos'
  - os.curdir is a string representing the current directory ('.' or ':')
  - os.pardir is a string representing the parent directory ('..' or '::')
  - os.sep is the (or a most common) pathname separator ('/' or ':' or '\\')
  - os.altsep is the alternate pathname separator (None or '/')
  - os.pathsep is the component separator used in $PATH etc
  - os.linesep is the line separator in text files ('\r' or '\n' or '\r\n')
  - os.defpath is the default search path for executables

Programs that import and use 'os' stand a better chance of being
portable between different platforms.  Of course, they must then
only use functions that are defined by all platforms (e.g., unlink
and opendir), and leave all pathname manipulation to os.path
(e.g., split and join).
"""

__all__ = ["altsep", "curdir", "pardir", "sep", "pathsep", "linesep",
           "defpath", "name",
           "system", "environ", "putenv", "getenv",
           "popen", "popen2", "popen3", "popen4", "getlogin"
           ]

import java
from java.io import File
import javapath as path
from UserDict import UserDict

error = OSError

name = 'java' # discriminate based on JDK version?
curdir = '.'  # default to Posix for directory behavior, override below
pardir = '..' 
sep = File.separator
altsep = None
pathsep = File.pathSeparator
defpath = '.'
linesep = java.lang.System.getProperty('line.separator')
if sep=='.':
    extsep = '/'
else:
    extsep = '.'

def _exit(n=0):
    java.lang.System.exit(n)

def getcwd():
    foo = File(File("foo").getAbsolutePath())
    return foo.getParent()

def chdir(path):
    raise OSError(0, 'chdir not supported in Java', path)

def listdir(path):
    l = File(path).list()
    if l is None:
        raise OSError(0, 'No such directory', path)
    return list(l)

def mkdir(path, mode='ignored'):
    if not File(path).mkdir():
        raise OSError(0, "couldn't make directory", path)

def makedirs(path, mode='ignored'):
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
    mtime = f.lastModified() / 1000.0
    return (0, 0, 0, 0, 0, 0, size, mtime, mtime, 0)

def utime(path, times):
    # Only the modification time is changed (and only on java2).
    if times and hasattr(File, "setLastModified"):
        File(path).setLastModified(long(times[1] * 1000.0))

class LazyDict( UserDict ):
    """A lazy-populating User Dictionary.
    Lazy initialization is not thread-safe.
    """
    def __init__( self,
                  dict=None,
                  populate=None,
                  keyTransform=None ):
        """dict: starting dictionary of values
        populate: function that returns the populated dictionary
        keyTransform: function to normalize the keys (e.g., toupper/None)
        """
        UserDict.__init__( self, dict )
        self._populated = 0
        self.__populateFunc = populate or (lambda: {})
        self._keyTransform = keyTransform or (lambda key: key)

    def __populate( self ):
        if not self._populated:
            # race condition - test, populate, set
            # make sure you don't set _populated until __populateFunc completes...
            self.data = self.__populateFunc()
            self._populated = 1 

    ########## extend methods from UserDict by pre-populating
    def __repr__(self):
        self.__populate()
        return UserDict.__repr__( self )
    def __cmp__(self, dict):
        self.__populate()
        return UserDict.__cmp__( self, dict )
    def __len__(self):
        self.__populate()
        return UserDict.__len__( self )
    def __getitem__(self, key):
        self.__populate()
        return UserDict.__getitem__( self, self._keyTransform(key) )
    def __setitem__(self, key, item):
        self.__populate()
        UserDict.__setitem__( self, self._keyTransform(key), item )
    def __delitem__(self, key):
        self.__populate()
        UserDict.__delitem__( self, self._keyTransform(key) )
    def clear(self):
        self.__populate()
        UserDict.clear( self )
    def copy(self):
        self.__populate()
        return UserDict.copy( self )
    def keys(self):
        self.__populate()
        return UserDict.keys( self )
    def items(self):
        self.__populate()
        return UserDict.items( self )
    def values(self):
        self.__populate()
        return UserDict.values( self )
    def has_key(self, key):
        self.__populate()
        return UserDict.has_key( self, self._keyTransform(key) )
    def update(self, dict):
        self.__populate()
        UserDict.update( self, dict )
    def get(self, key, failobj=None):
        self.__populate()
        return UserDict.get( self, self._keyTransform(key), failobj )
    def setdefault(self, key, failobj=None):
        self.__populate()
        return UserDict.setdefault( self, self._keyTransform(key), failobj )
    def popitem(self):
        self.__populate()
        return UserDict.popitem( self )


# Provide lazy environ, popen*, and system objects
# Do these lazily, as most jython programs don't need them,
# and they are very expensive to initialize

def _getEnvironment():
    import javashell
    return javashell._shellEnv.environment

environ = LazyDict( populate=_getEnvironment )
putenv = environ.__setitem__
getenv = environ.__getitem__

def system( *args, **kwargs ):
    # allow lazy import of popen2 and javashell
    import popen2
    return popen2.system( *args, **kwargs )

def popen( *args, **kwargs ):
    # allow lazy import of popen2 and javashell
    import popen2
    return popen2.popen( *args, **kwargs )

# os module versions of the popen# methods have different return value
# order than popen2 functions

def popen2(cmd, mode="t", bufsize=-1):
    import popen2
    stdout, stdin = popen2.popen2(cmd, bufsize)
    return stdin, stdout

def popen3(cmd, mode="t", bufsize=-1):
    import popen2
    stdout, stdin, stderr = popen2.popen3(cmd, bufsize)
    return stdin, stdout, stderr

def popen4(cmd, mode="t", bufsize=-1):
    import popen2
    stdout, stdin = popen2.popen4(cmd, bufsize)
    return stdin, stdout

def getlogin():
  return java.lang.System.getProperty("user.name")

