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

__all__ = ["altsep", "chdir", "curdir", "defpath", "environ", "getcwd", 
           "getenv", "getlogin", "linesep", "listdir", "mkdir", "name", 
           "pardir", "pathsep", "popen", "popen2", "popen3", "popen4", 
           "putenv","remove", "rename", "rmdir", "sep", "stat", "system",
           "unlink", "utime"]

from java.io import File
import java.lang.System
import javapath as path
from UserDict import UserDict
import time

class stat_result:
  import stat as _stat

  _stat_members = (
    ('st_mode', _stat.ST_MODE),
    ('st_ino', _stat.ST_INO),
    ('st_dev', _stat.ST_DEV),
    ('st_nlink', _stat.ST_NLINK),
    ('st_uid', _stat.ST_UID),
    ('st_gid', _stat.ST_GID),
    ('st_size', _stat.ST_SIZE),
    ('st_atime', _stat.ST_ATIME),
    ('st_mtime', _stat.ST_MTIME),
    ('st_ctime', _stat.ST_CTIME),
  )

  def __init__(self, results):
    if len(results) != 10:
      raise TypeError("stat_result() takes an a  10-sequence")
    for (name, index) in stat_result._stat_members:
      self.__dict__[name] = results[index]

  def __getitem__(self, i):
    if i < 0 or i > 9:
      raise IndexError(i)
    return getattr(self, stat_result._stat_members[i][0])
  
  def __setitem__(self, x, value):
    raise TypeError("object doesn't support item assignment")
  
  def __setattr__(self, name, value):
    if name in [x[0] for x in stat_result._stat_members]:
      raise TypeError(name)
    raise AttributeError("readonly attribute")
  
  def __len__(self):
    return 10

  def __cmp__(self, other):
    if not isinstance(other, stat_result):
      return 1
    return cmp(self.__dict__, other.__dict__)

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
path.curdir = curdir
path.pardir = pardir
path.sep = sep
path.altsep = altsep
path.pathsep = pathsep
path.defpath = defpath
path.extsep = extsep

def _exit(n=0):
    """_exit(status)

    Exit to the system with specified status, without normal exit
    processing.
    """
    java.lang.System.exit(n)

def getcwd():
    """getcwd() -> path

    Return a string representing the current working directory.
    """
    foo = File(File("foo").getAbsolutePath())
    return foo.getParent()

def chdir(path):
    """chdir(path)

    Change the current working directory to the specified path.
    """
    raise OSError(0, 'chdir not supported in Java', path)

def listdir(path):
    """listdir(path) -> list_of_strings

    Return a list containing the names of the entries in the directory.

        path: path of directory to list

    The list is in arbitrary order.  It does not include the special
    entries '.' and '..' even if they are present in the directory.
    """
    l = File(path).list()
    if l is None:
        raise OSError(0, 'No such directory', path)
    return list(l)

def mkdir(path, mode='ignored'):
    """mkdir(path [, mode=0777])

    Create a directory.

    The optional parameter is currently ignored.
    """
    if not File(path).mkdir():
        raise OSError(0, "couldn't make directory", path)

def makedirs(path, mode='ignored'):
    """makedirs(path [, mode=0777])

    Super-mkdir; create a leaf directory and all intermediate ones.

    Works like mkdir, except that any intermediate path segment (not
    just the rightmost) will be created if it does not exist.
    The optional parameter is currently ignored.
    """
    if not File(path).mkdirs():
        raise OSError(0, "couldn't make directories", path)

def remove(path):
    """remove(path)

    Remove a file (same as unlink(path)).
    """
    if not File(path).delete():
        raise OSError(0, "couldn't delete file", path)

unlink = remove

def rename(path, newpath):
    """rename(old, new)

    Rename a file or directory.
    """
    if not File(path).renameTo(File(newpath)):
        raise OSError(0, "couldn't rename file", path)

#XXX: copied from CPython 2.5.1
def renames(old, new):
    """renames(old, new)

    Super-rename; create directories as necessary and delete any left
    empty.  Works like rename, except creation of any intermediate
    directories needed to make the new pathname good is attempted
    first.  After the rename, directories corresponding to rightmost
    path segments of the old name will be pruned way until either the
    whole path is consumed or a nonempty directory is found.

    Note: this function can fail with the new directory structure made
    if you lack permissions needed to unlink the leaf directory or
    file.

    """
    head, tail = path.split(new)
    if head and tail and not path.exists(head):
        makedirs(head)
    rename(old, new)
    head, tail = path.split(old)
    if head and tail:
        try:
            removedirs(head)
        except error:
            pass

def rmdir(path):
    """rmdir(path)

    Remove a directory."""
    if not File(path).delete():
        raise OSError(0, "couldn't delete directory", path)

#XXX: copied from CPython 2.5.1
def removedirs(name):
    """removedirs(path)

    Super-rmdir; remove a leaf directory and empty all intermediate
    ones.  Works like rmdir except that, if the leaf directory is
    successfully removed, directories corresponding to rightmost path
    segments will be pruned away until either the whole path is
    consumed or an error occurs.  Errors during this latter phase are
    ignored -- they generally mean that a directory was not empty.

    """
    rmdir(name)
    head, tail = path.split(name)
    if not tail:
        head, tail = path.split(head)
    while head and tail:
        try:
            rmdir(head)
        except error:
            break
        head, tail = path.split(head)

__all__.extend(['makedirs', 'renames', 'removedirs'])

def stat(path):
    """stat(path) -> stat result

    Perform a stat system call on the given path.

    The Java stat implementation only returns a small subset of
    the standard fields: size, modification time and change time.
    """
    f = File(path)
    size = f.length()
    # Sadly, if the returned length is zero, we don't really know if the file
    # is zero sized or does not exist.
    if size == 0 and not f.exists():
        raise OSError(0, 'No such file or directory', path)
    mtime = f.lastModified() / 1000.0
    return stat_result((0, 0, 0, 0, 0, 0, size, mtime, mtime, 0))

def utime(path, times):
    """utime(path, (atime, mtime))
    utime(path, None)

    Set the access and modified time of the file to the given values.
    If the second form is used, set the access and modified times to the
    current time.

    Due to java limitations only the modification time is changed.
    """
    if times is not None:
        mtime = times[1]
    else:
        mtime = time.time()
    # Only the modification time is changed (and only on java2).
    if hasattr(File, "setLastModified"):
        File(path).setLastModified(long(mtime * 1000.0))

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
    def pop(self, *args):
      self.__populate()
      return UserDict.pop(self, *args)
    def iteritems(self):
      self.__populate()
      return UserDict.iteritems(self)
    def iterkeys(self):
      self.__populate()
      return UserDict.iterkeys(self)
    def itervalues(self):
      self.__populate()
      return UserDict.itervalues(self)
    def __contains__(self, key):
      self.__populate()
      return UserDict.__contains__(self, key)

# Provide lazy environ, popen*, and system objects
# Do these lazily, as most jython programs don't need them,
# and they are very expensive to initialize

def _getEnvironment():
    import javashell
    return javashell._shellEnv.environment

environ = LazyDict( populate=_getEnvironment )
putenv = environ.__setitem__

def getenv(key, default=None):
    """Get an environment variable, return None if it doesn't exist.

    The optional second argument can specify an alternate default.
    """
    return environ.get(key, default)

def system( *args, **kwargs ):
    """system(command) -> exit_status

    Execute the command (a string) in a subshell.
    """
    # allow lazy import of popen2 and javashell
    import popen2
    return popen2.system( *args, **kwargs )

def popen( *args, **kwargs ):
    """popen(command [, mode='r' [, bufsize]]) -> pipe

    Open a pipe to/from a command returning a file object.
    """
    # allow lazy import of popen2 and javashell
    import popen2
    return popen2.popen( *args, **kwargs )

# os module versions of the popen# methods have different return value
# order than popen2 functions

def popen2(cmd, mode="t", bufsize=-1):
    """Execute the shell command cmd in a sub-process.

    On UNIX, 'cmd' may be a sequence, in which case arguments will be
    passed directly to the program without shell intervention (as with
    os.spawnv()).  If 'cmd' is a string it will be passed to the shell
    (as with os.system()).  If 'bufsize' is specified, it sets the
    buffer size for the I/O pipes.  The file objects (child_stdin,
    child_stdout) are returned.
    """
    import popen2
    stdout, stdin = popen2.popen2(cmd, bufsize)
    return stdin, stdout

def popen3(cmd, mode="t", bufsize=-1):
    """Execute the shell command 'cmd' in a sub-process.

    On UNIX, 'cmd' may be a sequence, in which case arguments will be
    passed directly to the program without shell intervention
    (as with os.spawnv()).  If 'cmd' is a string it will be passed
    to the shell (as with os.system()).  If 'bufsize' is specified,
    it sets the buffer size for the I/O pipes.  The file objects
    (child_stdin, child_stdout, child_stderr) are returned.
    """
    import popen2
    stdout, stdin, stderr = popen2.popen3(cmd, bufsize)
    return stdin, stdout, stderr

def popen4(cmd, mode="t", bufsize=-1):
    """Execute the shell command 'cmd' in a sub-process.

    On UNIX, 'cmd' may be a sequence, in which case arguments will be
    passed directly to the program without shell intervention
    (as with os.spawnv()).  If 'cmd' is a string it will be passed
    to the shell (as with os.system()).  If 'bufsize' is specified,
    it sets the buffer size for the I/O pipes.  The file objects
    (child_stdin, child_stdout_stderr) are returned.
    """
    import popen2
    stdout, stdin = popen2.popen4(cmd, bufsize)
    return stdin, stdout

def getlogin():
    """getlogin() -> string

    Return the actual login name.
    """
    return java.lang.System.getProperty("user.name")
