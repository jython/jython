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

import errno
import java.lang.System
import javapath as path
import time
import stat as _stat

from java.io import File
from org.python.core.io import FileDescriptors
from UserDict import UserDict

import sys
sys.modules['os.path'] = _path = path

# open for reading only
O_RDONLY = 0x0
# open for writing only
O_WRONLY = 0x1
# open for reading and writing
O_RDWR = 0x2

# set append mode
O_APPEND = 0x8
# synchronous writes
O_SYNC = 0x80

# create if nonexistant
O_CREAT = 0x200
# truncate to zero length
O_TRUNC = 0x400
# error if already exists
O_EXCL = 0x800

# seek variables
SEEK_SET = 0
SEEK_CUR = 1
SEEK_END = 2

# test for existence of file
F_OK = 0
# test for execute or search permission
X_OK = 1<<0
# test for write permission
W_OK = 1<<1
# test for read permission
R_OK = 1<<2

# successful termination
EX_OK = 0

class stat_result:

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
    return sys.getCurrentWorkingDir()

def chdir(path):
    """chdir(path)

    Change the current working directory to the specified path.
    """
    if not _path.exists(path):
        raise OSError(errno.ENOENT, errno.strerror(errno.ENOENT), path)
    if not _path.isdir(path):
        raise OSError(errno.ENOTDIR, errno.strerror(errno.ENOTDIR), path)
    sys.setCurrentWorkingDir(path)

def listdir(path):
    """listdir(path) -> list_of_strings

    Return a list containing the names of the entries in the directory.

        path: path of directory to list

    The list is in arbitrary order.  It does not include the special
    entries '.' and '..' even if they are present in the directory.
    """
    l = File(sys.getPath(path)).list()
    if l is None:
        raise OSError(0, 'No such directory', path)
    return list(l)

def mkdir(path, mode='ignored'):
    """mkdir(path [, mode=0777])

    Create a directory.

    The optional parameter is currently ignored.
    """
    if not File(sys.getPath(path)).mkdir():
        raise OSError(0, "couldn't make directory", path)

def makedirs(path, mode='ignored'):
    """makedirs(path [, mode=0777])

    Super-mkdir; create a leaf directory and all intermediate ones.

    Works like mkdir, except that any intermediate path segment (not
    just the rightmost) will be created if it does not exist.
    The optional parameter is currently ignored.
    """
    if not File(sys.getPath(path)).mkdirs():
        raise OSError(0, "couldn't make directories", path)

def remove(path):
    """remove(path)

    Remove a file (same as unlink(path)).
    """
    if not File(sys.getPath(path)).delete():
        raise OSError(0, "couldn't delete file", path)

unlink = remove

def rename(path, newpath):
    """rename(old, new)

    Rename a file or directory.
    """
    if not File(sys.getPath(path)).renameTo(File(sys.getPath(newpath))):
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
    if not File(sys.getPath(path)).delete():
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

def strerror(code):
    """strerror(code) -> string
    
    Translate an error code to a message string.
    """
    if not isinstance(code, (int, long)):
        raise TypeError('an integer is required')
    try:
        return errno.strerror(code)
    except KeyError:
        return 'Unknown error: %d' % code

def access(path, mode):
    """access(path, mode) -> True if granted, False otherwise
        
    Use the real uid/gid to test for access to a path.  Note that most
    operations will use the effective uid/gid, therefore this routine can
    be used in a suid/sgid environment to test if the invoking user has the
    specified access to the path.  The mode argument can be F_OK to test
    existence, or the inclusive-OR of R_OK, W_OK, and X_OK.
    """
    if not isinstance(mode, (int, long)):
        raise TypeError('an integer is required')

    f = File(sys.getPath(path))
    if not f.exists():
      return False
    if mode & R_OK and not f.canRead():
        return False
    if mode & W_OK and not f.canWrite():
        return False
    if mode & X_OK:
        return False
    return True

def stat(path):
    """stat(path) -> stat result

    Perform a stat system call on the given path.

    The Java stat implementation only returns a small subset of
    the standard fields: size, modification time and change time.
    """
    f = File(sys.getPath(path))
    if not f.exists():
        raise OSError(errno.ENOENT, errno.strerror(errno.ENOENT), path)
    size = f.length()
    mtime = f.lastModified() / 1000.0
    mode = 0
    if f.isDirectory():
        mode = _stat.S_IFDIR
    elif f.isFile():
        mode = _stat.S_IFREG
    if f.canRead():
        mode = mode | _stat.S_IREAD
    if f.canWrite():
        mode = mode | _stat.S_IWRITE
    return stat_result((mode, 0, 0, 0, 0, 0, size, mtime, mtime, 0))

def lstat(path):
    """lstat(path) -> stat result
    
    Like stat(path), but do not follow symbolic links.
    """
    f = File(sys.getPath(path))
    abs_parent = f.getAbsoluteFile().getParentFile()
    can_parent = abs_parent.getCanonicalFile()

    if can_parent.getAbsolutePath() == abs_parent.getAbsolutePath():
        # The parent directory's absolute path is canonical..
        if f.getAbsolutePath() != f.getCanonicalPath():
            # but the file's absolute and paths differ (a link)
            return stat_result((_stat.S_IFLNK, 0, 0, 0, 0, 0, 0, 0, 0, 0))

    # The parent directory's path is not canonical (one of the parent
    # directories is a symlink). Build a new path with the parent's
    # canonical path and compare the files
    f = File(_path.join(can_parent.getAbsolutePath(), f.getName()))
    if f.getAbsolutePath() != f.getCanonicalPath():
        return stat_result((_stat.S_IFLNK, 0, 0, 0, 0, 0, 0, 0, 0, 0))

    # Not a link, only now can we determine if it exists (because
    # File.exists() returns False for dead links)
    if not f.exists():
        raise OSError(errno.ENOENT, errno.strerror(errno.ENOENT), path)
    return stat(path)

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
    # Only the modification time is changed
    File(sys.getPath(path)).setLastModified(long(mtime * 1000.0))

def close(fd):
    """close(fd)

    Close a file descriptor (for low level IO).
    """
    rawio = FileDescriptors.get(fd)
    _handle_oserror(rawio.close)

def fdopen(fd, mode='r', bufsize=-1):
    """fdopen(fd [, mode='r' [, bufsize]]) -> file_object

    Return an open file object connected to a file descriptor.
    """
    rawio = FileDescriptors.get(fd)
    if (len(mode) and mode[0] or '') not in 'rwa':
        raise ValueError("invalid file mode '%s'" % mode)
    if rawio.closed():
        raise OSError(errno.EBADF, errno.strerror(errno.EBADF))

    from org.python.core import PyFile
    try:
        fp = PyFile(rawio, '<fdopen>', mode, bufsize)
    except IOError:
        raise OSError(errno.EINVAL, errno.strerror(errno.EINVAL))
    return fp

def ftruncate(fd, length):
    """ftruncate(fd, length)

    Truncate a file to a specified length.
    """    
    rawio = FileDescriptors.get(fd)
    try:
        rawio.truncate(length)
    except Exception, e:
        raise IOError(errno.EBADF, errno.strerror(errno.EBADF))

def lseek(fd, pos, how):
    """lseek(fd, pos, how) -> newpos

    Set the current position of a file descriptor.
    """
    rawio = FileDescriptors.get(fd)
    return _handle_oserror(rawio.seek, pos, how)

def open(filename, flag, mode=0777):
    """open(filename, flag [, mode=0777]) -> fd

    Open a file (for low level IO).
    """
    reading = flag & O_RDONLY
    writing = flag & O_WRONLY
    updating = flag & O_RDWR
    creating = flag & O_CREAT

    truncating = flag & O_TRUNC
    exclusive = flag & O_EXCL
    sync = flag & O_SYNC
    appending = flag & O_APPEND

    if updating and writing:
        raise OSError(errno.EINVAL, errno.strerror(errno.EINVAL), filename)

    if not creating and not path.exists(filename):
        raise OSError(errno.ENOENT, errno.strerror(errno.ENOENT), filename)

    if not writing or updating:
        # Default to reading
        reading = True

    from org.python.core.io import FileIO
    if truncating and not writing:
        # Explicitly truncate, writing will truncate anyway
        FileIO(filename, 'w').close()

    if exclusive and creating:
        try:
            if not File(sys.getPath(filename)).createNewFile():
                raise OSError(errno.EEXIST, errno.strerror(errno.EEXIST),
                              filename)
        except java.io.IOException, ioe:
            raise OSError(ioe)

    mode = '%s%s%s%s' % (reading and 'r' or '',
                         (not appending and writing) and 'w' or '',
                         (appending and (writing or updating)) and 'a' or '',
                         updating and '+' or '')

    if sync and (writing or updating):
        from java.io import FileNotFoundException, RandomAccessFile
        try:
            fchannel = RandomAccessFile(sys.getPath(filename), 'rws').getChannel()
        except FileNotFoundException, fnfe:
            if path.isdir(filename):
                raise OSError(errno.EISDIR, errno.strerror(errno.EISDIR))
            raise OSError(errno.ENOENT, errno.strerror(errno.ENOENT), filename)
        return FileIO(fchannel, mode)

    return FileIO(filename, mode)

def read(fd, buffersize):
    """read(fd, buffersize) -> string

    Read a file descriptor.
    """
    from org.python.core.util import StringUtil
    rawio = FileDescriptors.get(fd)
    buf = _handle_oserror(rawio.read, buffersize)
    return str(StringUtil.fromBytes(buf))

def write(fd, string):
    """write(fd, string) -> byteswritten

    Write a string to a file descriptor.
    """
    from java.nio import ByteBuffer
    from org.python.core.util import StringUtil
    rawio = FileDescriptors.get(fd)
    return _handle_oserror(rawio.write,
                           ByteBuffer.wrap(StringUtil.toBytes(string)))

def _handle_oserror(func, *args, **kwargs):
    """Translate exceptions into OSErrors"""
    try:
        return func(*args, **kwargs)
    except:
        raise OSError(errno.EBADF, errno.strerror(errno.EBADF))

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
    def __iter__(self):
        self.__populate()
        return iter( self.data )
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

#XXX: copied from CPython's release23-maint branch revision 56502
def walk(top, topdown=True, onerror=None):
    """Directory tree generator.

    For each directory in the directory tree rooted at top (including top
    itself, but excluding '.' and '..'), yields a 3-tuple

        dirpath, dirnames, filenames

    dirpath is a string, the path to the directory.  dirnames is a list of
    the names of the subdirectories in dirpath (excluding '.' and '..').
    filenames is a list of the names of the non-directory files in dirpath.
    Note that the names in the lists are just names, with no path components.
    To get a full path (which begins with top) to a file or directory in
    dirpath, do os.path.join(dirpath, name).

    If optional arg 'topdown' is true or not specified, the triple for a
    directory is generated before the triples for any of its subdirectories
    (directories are generated top down).  If topdown is false, the triple
    for a directory is generated after the triples for all of its
    subdirectories (directories are generated bottom up).

    When topdown is true, the caller can modify the dirnames list in-place
    (e.g., via del or slice assignment), and walk will only recurse into the
    subdirectories whose names remain in dirnames; this can be used to prune
    the search, or to impose a specific order of visiting.  Modifying
    dirnames when topdown is false is ineffective, since the directories in
    dirnames have already been generated by the time dirnames itself is
    generated.

    By default errors from the os.listdir() call are ignored.  If
    optional arg 'onerror' is specified, it should be a function; it
    will be called with one argument, an os.error instance.  It can
    report the error to continue with the walk, or raise the exception
    to abort the walk.  Note that the filename is available as the
    filename attribute of the exception object.

    Caution:  if you pass a relative pathname for top, don't change the
    current working directory between resumptions of walk.  walk never
    changes the current directory, and assumes that the client doesn't
    either.

    Example:

    from os.path import join, getsize
    for root, dirs, files in walk('python/Lib/email'):
        print root, "consumes",
        print sum([getsize(join(root, name)) for name in files]),
        print "bytes in", len(files), "non-directory files"
        if 'CVS' in dirs:
            dirs.remove('CVS')  # don't visit CVS directories
    """

    from os.path import join, isdir, islink

    # We may not have read permission for top, in which case we can't
    # get a list of the files the directory contains.  os.path.walk
    # always suppressed the exception then, rather than blow up for a
    # minor reason when (say) a thousand readable directories are still
    # left to visit.  That logic is copied here.
    try:
        # Note that listdir and error are globals in this module due
        # to earlier import-*.
        names = listdir(top)
    except error, err:
        if onerror is not None:
            onerror(err)
        return

    dirs, nondirs = [], []
    for name in names:
        if isdir(join(top, name)):
            dirs.append(name)
        else:
            nondirs.append(name)

    if topdown:
        yield top, dirs, nondirs
    for name in dirs:
        path = join(top, name)
        if not islink(path):
            for x in walk(path, topdown, onerror):
                yield x
    if not topdown:
        yield top, dirs, nondirs

__all__.append("walk")


