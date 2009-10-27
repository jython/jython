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
__all__.extend(['fsync', 'getenv', 'getpid', 'isatty', 'popen', 'putenv',
                'remove', 'rename', 'rmdir', 'system', 'umask', 'unlink',
                'unsetenv', 'urandom', 'utime'])

_name = _posix.__name__[1:]

# Java class representing the size of a time_t. internal use, lazily set
_time_t = None

# For urandom
urandom_source = None

def remove(path):
    """remove(path)

    Remove a file (same as unlink(path)).
    """
    from java.io import File
    if not File(sys.getPath(path)).delete():
        raise OSError(0, "couldn't delete file", path)

unlink = remove

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
        import jarray
        status = jarray.zeros(1, 'i')
        res_pid = _posix_impl.wait(status)
        if res_pid == -1:
            raise OSError(status[0], strerror(status[0]))
        return res_pid, status[0]

    def waitpid(pid, options):
        """waitpid(pid, options) -> (pid, status)

        Wait for completion of a given child process."""
        import jarray
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
    from org.python.core.io import FileDescriptors
    rawio = FileDescriptors.get(fd)
    rawio.checkClosed()

    from java.io import IOException
    from java.nio.channels import FileChannel
    channel = rawio.getChannel()
    if not isinstance(channel, FileChannel):
        raise OSError(errno.EINVAL, strerror(errno.EINVAL))

    try:
        channel.force(metadata)
    except IOException, ioe:
        raise OSError(ioe)

def getpid():
    """getpid() -> pid

    Return the current process id."""
    # XXX: getpid and umask should really be hidden from __all__ when
    # not _native_posix
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

    from org.python.core.io import IOBase
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
    import jarray
    buffer = jarray.zeros(n, 'b')
    urandom_source.nextBytes(buffer)
    return buffer.tostring()
