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
import sys

__all__ = [name for name in _posix.__all__ if not name.startswith('__doc__')]
__all__.extend(['getenv', 'isatty', 'popen', 'putenv',
                'rename', 'rmdir', 'system',
                'unsetenv', 'urandom', 'utime'])

_name = _posix.__name__[1:]

# Java class representing the size of a time_t. internal use, lazily set
_time_t = None

# For urandom
urandom_source = None

def rename(path, newpath):
    """rename(old, new)

    Rename a file or directory.
    """
    from java.io import File
    if not File(sys.getPath(path)).renameTo(File(sys.getPath(newpath))):
        raise OSError(0, "couldn't rename file", path)

def rmdir(path):
    """rmdir(path)

    Remove a directory."""
    from java.io import File
    f = File(sys.getPath(path))
    if not f.exists():
        raise OSError(errno.ENOENT, strerror(errno.ENOENT), path)
    elif not f.isDirectory():
        raise OSError(errno.ENOTDIR, strerror(errno.ENOTDIR), path)
    elif not f.delete():
        raise OSError(0, "couldn't delete directory", path)

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

    from org.python.core.io import IOBase
    if not isinstance(fileno, IOBase):
        raise TypeError('a file descriptor is required')

    return fileno.isatty()

def urandom(n):
    global urandom_source
    if urandom_source is None:
        from java.security import SecureRandom
        urandom_source = SecureRandom()
    import jarray
    buffer = jarray.zeros(n, 'b')
    urandom_source.nextBytes(buffer)
    return buffer.tostring()
