"""
This module provides access to operating system functionality that is
standardized by the C Standard and the POSIX standard (a thinly
disguised Unix interface).  Refer to the library manual and
corresponding Unix manual entries for more information on calls.
"""
try:
    import _posix
    from _posix import *
except:
    import _nt as _posix
    from _nt import *
import errno
import jarray
import stat as _stat
import sys
from java.io import File
from org.python.core.io import FileDescriptors, FileIO, IOBase
from org.python.core.Py import newString as asPyString

__all__ = [name for name in _posix.__all__ if not name.startswith('__doc__')]
__all__.extend(['access', 'chdir', 'chmod', 'close', 'fdopen', 'fsync',
                'ftruncate', 'getcwd', 'getcwdu', 'getenv', 'getpid', 'isatty',
                'lseek', 'mkdir', 'open', 'popen', 'putenv', 'read', 'remove',
                'rename', 'rmdir', 'system', 'umask', 'unlink', 'unsetenv',
                'urandom', 'utime', 'write'])

_name = _posix.__name__[1:]

# Java class representing the size of a time_t. internal use, lazily set
_time_t = None

# For urandom
urandom_source = None

# Lazily loaded path module
_path = None

def getcwd():
    """getcwd() -> path

    Return a string representing the current working directory.
    """
    return asPyString(sys.getCurrentWorkingDir())

def getcwdu():
    """getcwd() -> path

    Return a unicode string representing the current working directory.
    """
    return sys.getCurrentWorkingDir()

def chdir(path):
    """chdir(path)

    Change the current working directory to the specified path.
    """
    global _path
    if not _stat.S_ISDIR(stat(path).st_mode):
        raise OSError(errno.ENOTDIR, strerror(errno.ENOTDIR), path)
    if _path is None:
        import os
        _path = os.path
    sys.setCurrentWorkingDir(_path.realpath(path))

def chmod(path, mode):
    """chmod(path, mode)

    Change the access permissions of a file.
    """
    # XXX no error handling for chmod in jna-posix
    # catch not found errors explicitly here, for now
    abs_path = sys.getPath(path)
    if not File(abs_path).exists():
        raise OSError(errno.ENOENT, strerror(errno.ENOENT), path)
    _posix_impl.chmod(abs_path, mode)

def mkdir(path, mode='ignored'):
    """mkdir(path [, mode=0777])

    Create a directory.

    The optional parameter is currently ignored.
    """
    # XXX: use _posix_impl.mkdir when we can get the real errno upon failure
    fp = File(sys.getPath(path))
    if not fp.mkdir():
        if fp.isDirectory() or fp.isFile():
            err = errno.EEXIST
        else:
            err = 0
        msg = strerror(err) if err else "couldn't make directory"
        raise OSError(err, msg, path)

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

def rmdir(path):
    """rmdir(path)

    Remove a directory."""
    f = File(sys.getPath(path))
    if not f.exists():
        raise OSError(errno.ENOENT, strerror(errno.ENOENT), path)
    elif not f.isDirectory():
        raise OSError(errno.ENOTDIR, strerror(errno.ENOTDIR), path)
    elif not f.delete():
        raise OSError(0, "couldn't delete directory", path)

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
    result = True
    if not f.exists():
        result = False
    if mode & R_OK and not f.canRead():
        result = False
    if mode & W_OK and not f.canWrite():
        result = False
    if mode & X_OK:
        # NOTE: always False without jna-posix stat
        try:
            result = (stat(path).st_mode & _stat.S_IEXEC) != 0
        except OSError:
            result = False
    return result

def utime(path, times):
    """utime(path, (atime, mtime))
    utime(path, None)

    Set the access and modification time of the file to the given values.
    If the second form is used, set the access and modification times to the
    current time.

    Due to Java limitations, on some platforms only the modification time
    may be changed.
    """
    if path is None:
        raise TypeError('path must be specified, not None')

    if times is None:
        atimeval = mtimeval = None
    elif isinstance(times, tuple) and len(times) == 2:
        atimeval = _to_timeval(times[0])
        mtimeval = _to_timeval(times[1])
    else:
        raise TypeError('utime() arg 2 must be a tuple (atime, mtime)')

    _posix_impl.utimes(path, atimeval, mtimeval)

def _to_timeval(seconds):
    """Convert seconds (with a fraction) from epoch to a 2 item tuple of
    seconds, microseconds from epoch as longs
    """
    global _time_t
    if _time_t is None:
        from java.lang import Integer, Long
        try:
            from org.python.posix.util import Platform
        except ImportError:
            from org.jruby.ext.posix.util import Platform
        _time_t = Integer if Platform.IS_32_BIT else Long

    try:
        floor = long(seconds)
    except TypeError:
        raise TypeError('an integer is required')
    if not _time_t.MIN_VALUE <= floor <= _time_t.MAX_VALUE:
        raise OverflowError('long int too large to convert to int')

    # usec can't exceed 1000000
    usec = long((seconds - floor) * 1e6)
    if usec < 0:
        # If rounding gave us a negative number, truncate
        usec = 0
    return floor, usec

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
        raise OSError(errno.EBADF, strerror(errno.EBADF))

    try:
        fp = FileDescriptors.wrap(rawio, mode, bufsize)
    except IOError:
        raise OSError(errno.EINVAL, strerror(errno.EINVAL))
    return fp

def ftruncate(fd, length):
    """ftruncate(fd, length)

    Truncate a file to a specified length.
    """
    rawio = FileDescriptors.get(fd)
    try:
        rawio.truncate(length)
    except Exception, e:
        raise IOError(errno.EBADF, strerror(errno.EBADF))

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
        raise OSError(errno.EINVAL, strerror(errno.EINVAL), filename)

    if not creating:
        # raises ENOENT if it doesn't exist
        stat(filename)

    if not writing:
        if updating:
            writing = True
        else:
            reading = True

    if truncating and not writing:
        # Explicitly truncate, writing will truncate anyway
        FileIO(filename, 'w').close()

    if exclusive and creating:
        try:
            if not File(sys.getPath(filename)).createNewFile():
                raise OSError(errno.EEXIST, strerror(errno.EEXIST),
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
            if _stat.S_ISDIR(stat(filename).st_mode):
                raise OSError(errno.EISDIR, strerror(errno.EISDIR))
            raise OSError(errno.ENOENT, strerror(errno.ENOENT), filename)
        return FileIO(fchannel, mode)

    return FileIO(filename, mode)

def read(fd, buffersize):
    """read(fd, buffersize) -> string

    Read a file descriptor.
    """
    from org.python.core.util import StringUtil
    rawio = FileDescriptors.get(fd)
    buf = _handle_oserror(rawio.read, buffersize)
    return asPyString(StringUtil.fromBytes(buf))

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
        raise OSError(errno.EBADF, strerror(errno.EBADF))

def system(command):
    """system(command) -> exit_status

    Execute the command (a string) in a subshell.
    """
    import subprocess
    return subprocess.call(command, shell=True)

def popen(command, mode='r', bufsize=-1):
    """popen(command [, mode='r' [, bufsize]]) -> pipe

    Open a pipe to/from a command returning a file object.
    """
    import subprocess
    if mode == 'r':
        proc = subprocess.Popen(command, bufsize=bufsize, shell=True,
                                stdout=subprocess.PIPE)
        return _wrap_close(proc.stdout, proc)
    elif mode == 'w':
        proc = subprocess.Popen(command, bufsize=bufsize, shell=True,
                                stdin=subprocess.PIPE)
        return _wrap_close(proc.stdin, proc)
    else:
        raise OSError(errno.EINVAL, strerror(errno.EINVAL))

# Helper for popen() -- a proxy for a file whose close waits for the process
class _wrap_close(object):
    def __init__(self, stream, proc):
        self._stream = stream
        self._proc = proc
    def close(self):
        self._stream.close()
        returncode = self._proc.wait()
        if returncode == 0:
            return None
        if _name == 'nt':
            return returncode
        else:
            return returncode
    def __getattr__(self, name):
        return getattr(self._stream, name)
    def __iter__(self):
        return iter(self._stream)

def putenv(key, value):
    """putenv(key, value)

    Change or add an environment variable.
    """
    # XXX: put/unset/getenv should probably be deprecated
    import os
    os.environ[key] = value

def unsetenv(key):
    """unsetenv(key)

    Delete an environment variable.
    """
    import os
    try:
        del os.environ[key]
    except KeyError:
        pass

def getenv(key, default=None):
    """Get an environment variable, return None if it doesn't exist.
    The optional second argument can specify an alternate default."""
    import os
    return os.environ.get(key, default)

if _name == 'posix':
    def link(src, dst):
        """link(src, dst)

        Create a hard link to a file.
        """
        _posix_impl.link(sys.getPath(src), sys.getPath(dst))

    def symlink(src, dst):
        """symlink(src, dst)

        Create a symbolic link pointing to src named dst.
        """
        _posix_impl.symlink(src, sys.getPath(dst))

    def readlink(path):
        """readlink(path) -> path

        Return a string representing the path to which the symbolic link
        points.
        """
        return _posix_impl.readlink(sys.getPath(path))

    def getegid():
        """getegid() -> egid

        Return the current process's effective group id."""
        return _posix_impl.getegid()

    def geteuid():
        """geteuid() -> euid

        Return the current process's effective user id."""
        return _posix_impl.geteuid()

    def getgid():
        """getgid() -> gid

        Return the current process's group id."""
        return _posix_impl.getgid()

    def getlogin():
        """getlogin() -> string

        Return the actual login name."""
        return _posix_impl.getlogin()

    def getpgrp():
        """getpgrp() -> pgrp

        Return the current process group id."""
        return _posix_impl.getpgrp()

    def getppid():
        """getppid() -> ppid

        Return the parent's process id."""
        return _posix_impl.getppid()

    def getuid():
        """getuid() -> uid

        Return the current process's user id."""
        return _posix_impl.getuid()

    def setpgrp():
        """setpgrp()

        Make this process a session leader."""
        return _posix_impl.setpgrp()

    def setsid():
        """setsid()

        Call the system call setsid()."""
        return _posix_impl.setsid()

    # This implementation of fork partially works on
    # Jython. Diagnosing what works, what doesn't, and fixing it is
    # left for another day. In any event, this would only be
    # marginally useful.

    # def fork():
    #     """fork() -> pid
    #
    #     Fork a child process.
    #     Return 0 to child process and PID of child to parent process."""
    #     return _posix_impl.fork()

    def kill(pid, sig):
        """kill(pid, sig)

        Kill a process with a signal."""
        return _posix_impl.kill(pid, sig)

    def wait():
        """wait() -> (pid, status)

        Wait for completion of a child process."""

        status = jarray.zeros(1, 'i')
        res_pid = _posix_impl.wait(status)
        if res_pid == -1:
            raise OSError(status[0], strerror(status[0]))
        return res_pid, status[0]

    def waitpid(pid, options):
        """waitpid(pid, options) -> (pid, status)

        Wait for completion of a given child process."""
        status = jarray.zeros(1, 'i')
        res_pid = _posix_impl.waitpid(pid, status, options)
        if res_pid == -1:
            raise OSError(status[0], strerror(status[0]))
        return res_pid, status[0]

    def fdatasync(fd):
        """fdatasync(fildes)

        force write of file with filedescriptor to disk.
        does not force update of metadata.
        """
        _fsync(fd, False)

    __all__.extend(['link', 'symlink', 'readlink', 'getegid', 'geteuid',
                    'getgid', 'getlogin', 'getpgrp', 'getppid', 'getuid',
                    'setpgrp', 'setsid', 'kill', 'wait', 'waitpid',
                    'fdatasync'])

def fsync(fd):
    """fsync(fildes)

    force write of file with filedescriptor to disk.
    """
    _fsync(fd, True)

def _fsync(fd, metadata):
    """Internal fsync impl"""
    rawio = FileDescriptors.get(fd)
    rawio.checkClosed()

    from java.nio.channels import FileChannel
    channel = rawio.getChannel()
    if not isinstance(channel, FileChannel):
        raise OSError(errno.EINVAL, strerror(errno.EINVAL))

    try:
        channel.force(metadata)
    except java.io.IOException, ioe:
        raise OSError(ioe)

def getpid():
    """getpid() -> pid

    Return the current process id."""
    return _posix_impl.getpid()

def isatty(fileno):
    """isatty(fd) -> bool

    Return True if the file descriptor 'fd' is an open file descriptor
    connected to the slave end of a terminal."""
    from java.io import FileDescriptor

    if isinstance(fileno, int):
        if fileno == 0:
            fd = getattr(FileDescriptor, 'in')
        elif fileno == 1:
            fd = FileDescriptor.out
        elif fileno == 2:
            fd = FileDescriptor.err
        else:
            raise NotImplemented('Integer file descriptor compatibility only '
                                 'available for stdin, stdout and stderr (0-2)')

        return _posix_impl.isatty(fd)

    if isinstance(fileno, FileDescriptor):
        return _posix_impl.isatty(fileno)

    if not isinstance(fileno, IOBase):
        raise TypeError('a file descriptor is required')

    return fileno.isatty()

def umask(new_mask):
    """umask(new_mask) -> old_mask

    Set the current numeric umask and return the previous umask."""
    return _posix_impl.umask(int(new_mask))

def urandom(n):
    global urandom_source
    if urandom_source is None:
        from java.security import SecureRandom
        urandom_source = SecureRandom()
    buffer = jarray.zeros(n, 'b')
    urandom_source.nextBytes(buffer)
    return buffer.tostring()
