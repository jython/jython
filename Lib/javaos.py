r"""OS routines for Mac, DOS, NT, or Posix depending on what system we're on.

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
           "defpath", "name"]

import java
from java.io import File, BufferedReader, InputStreamReader
import javapath as path
from UserDict import UserDict
import string
import exceptions
import re
import sys
import thread


error = OSError

name = 'java' # descriminate based on JDK version?
curdir = '.'
pardir = '..' #This might not be right...
#curdir, pardir??
sep = java.io.File.separator
altsep = None
pathsep = java.io.File.pathSeparator
defpath = '.'
linesep = java.lang.System.getProperty('line.separator')

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
        
    def __init__( self, dict=None, populate=lambda: {}, keyTransform=None ):
        """dict is the starting dictionary of values
        populate is a function that returns the populated dictionary
        keyTransform is a function to transform the environment keys
        (e.g., upper or identity)
        """
        UserDict.__init__( self, dict )
        self._populated = 0
        self.__populateFunc = populate
        self._keyTransform = keyTransform or (lambda key: key)

    def __populate( self ):
        if not self._populated:
            # store as self.data so any 'set' sets in original as well
            self.data = self.__populateFunc()
            self._populated = 1 # not thread-safe! 

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


class _NullEnv:
    """Placeholder Environment for platforms w/o shell or environment
    functionality, like Mac"""
    def __init__( self ):
        self.environment = LazyDict()
        
    # note that this won't pass __test()
    def execute( self, cmd ):
        return None

    def system( self, cmd ):
        raise OSError( 0, "os.system not implemented. "
                          "Try setting python.environment=shell.", cmd ) 
        return -1

    def readLines( self, cmd ):
        return []
    
class _ShellEnv:
    """Environment derived by spawning a subshell and parsing its environment.
    Also supports current directory and system functions.
    """
    def __init__( self, cmd, getEnv, keyTransform=None ):
        """cmd is a list of arguments to come before the command in the
        Runtime.exec() call.
        getEnv is the string system command to list the environment variables
        envKeyTransform is a function to transform the environment keys
        (usually toupper or identity)
        """
        self.cmd = cmd
        self.getEnv = getEnv
        self.environment = LazyDict(populate=self._getEnvironment,
                                    keyTransform=keyTransform)
        self._keyTransform = self.environment._keyTransform

    ########## system
    def system( self, cmd ):
        """Act like the standard library 'system' call.
        Execute a command in a shell, and send output to stdout.
        """
        p = self.execute( cmd )

        # we want intermediate output while process runs, so call
        # _readLines( ... println )
        def println( arg, write=sys.stdout.write ):
            write( arg + "\n" )
        # read stderr in secondary thread
        thread.start_new_thread( self._readLines,
                                ( p.getErrorStream(), println ))
        # read stdin in main thread
        self._readLines( p.getInputStream(), println )
        return p.waitFor()


    def execute( self, cmd ):
        """Execute cmd in a shell, and then return the process instance"""
        shellCmd = self._formatCmd( cmd )
        if self.environment._populated:
            env = self._formatEnvironment( self.environment )
        else:
            env = None
        p = java.lang.Runtime.getRuntime().exec( shellCmd, env )
        return p

    ########## utility methods
    def _readLines( self, stream, func=None ):
        """Read lines of stream, and either append them to return
        array of lines, or call func on each line.
        """
        lines = []
        func = func or lines.append
        # should read both stderror and stdout in separate threads...
        bufStream = BufferedReader( InputStreamReader( stream ))
        while 1:
            line = bufStream.readLine()
            if line is None: break
            func( line )
        return lines or None

    def _formatCmd( self, cmd ):
        """Format a command for execution in a shell.
        """
        return self.cmd + [cmd]

    def _formatEnvironment( self, env ):
        """Format enviroment in lines suitable for Runtime.exec"""
        lines = []
        for keyValue in env.items():
            lines.append( "%s=%s" % keyValue )
        return lines

    def _getEnvironment( self ):
        """Get the environment variables by spawning a subshell.
        This allows multi-line variables as long as subsequent lines do
        not have '=' signs.
        """
        p = self.execute( self.getEnv )
        env = {}
        key = 'firstLine' # in case first line had no '='
        for line in self._readLines( p.getInputStream() ):
            try:
                i = line.index( '=' )
                key = self._keyTransform(line[:i])
                value = line[i+1:]
            except ValueError:
                # found no '=', so this line is part of previous value
                value = '%s\n%s' % ( value, line )
            env[ key ] = value
        return env

def _getOsType( os=None ):
    os = os or sys.registry.getProperty( "python.os" ) or \
               java.lang.System.getProperty( "os.name" )
        
    _osTypeMap = (
        ( "nt", r"(nt|Windows NT)|(Windows NT 4.0)|(WindowsNT)|"
                r"(Windows 2000)|(Windows XP)|(Windows CE)" ),
        ( "dos", r"(dos|Windows 95)|(Windows 98)|(Windows ME)" ),
        ( "mac", r"(mac|MacOS.*)|(Darwin)" ),
        ( "None", r"None" ),
        ( "unix", r".*" ), # last is default, even if doesn't match
        )
    for osType, pattern in _osTypeMap:
        if re.match( pattern, os ):
            break
    
    return osType

def _getShellEnv( os = None ):
    """Select the type of environment handling we want to provide.
    os is None to auto-detect, or something recognized by _getOsType()
    could add 'java'
    """
    if os == "shell":
        os = None
    osType = _getOsType( os )
    if osType == "nt":
        return _ShellEnv( ["cmd", "/c"], "set", string.upper )
    elif osType == "dos":
        return _ShellEnv( ["command", "/c"], "set", string.upper )
    elif osType == "mac":
        return _NullEnv()
    else: # osType == "unix":
        return _ShellEnv( ["sh", "-c"], "env" )

_envType = sys.registry.getProperty("python.environment", "shell")
if _envType == "shell":
    _shellEnv = _getShellEnv()
else:
    _shellEnv = _NullEnv()

# provide environ, putenv, getenv
environ = _shellEnv.environment
putenv = environ.__setitem__
getenv = environ.__getitem__
# provide system
system = _shellEnv.system

########## test code
def __test( shellEnv=_shellEnv ):
    # tests system and environment functionality
    key, value = "testKey", "testValue"
    org = environ
    testCmds = [
        # test commands and regexes to match first line of expected
        # output on first and second runs
        # Note that the validation is incomplete for several of these
        # - they should validate depending on platform and pre-post, but
        # they don't.

        # no quotes, should output both words
        ("echo hello there", "hello there"),
        # should print PATH (on NT)
        ("echo PATH=%PATH%", "(PATH=.*;.*)|(PATH=%PATH%)"),
        # should print 'testKey=%testKey%' on NT before initialization,
        # and 'testKey=testValue' after
        ("echo %s=%%%s%%" % (key,key),
                "(%s=%%%s%%)|(%s=%s)" % (key, key, key, value)),     
        # should print PATH (on Unix)
        ( "echo PATH=$PATH", "PATH=.*" ),
        # should print 'testKey=testValue' on Unix after initialization
        ( "echo %s=$%s" % (key,key),
                "(%s=$%s)|(%s=)|(%s=%s)" % (key, key, key, key, value ) ), 
        # should output quotes on NT but not on Unix
        ( 'echo "hello there"', '"?hello there"?' ),
        # should print 'why' to stdout. 
        ( r'''python -c "import sys;sys.stdout.write( 'why\n' )"''', "why" ),
        # should print 'why' to stderr, but it won't right now.  Have
        # to add the print to give some output...empty string matches every
        # thing...
        ( r'''python -c "import sys;sys.stderr.write('why\n');print " ''', "" )
        ]
    
    assert not environ._populated, \
            "before population, environ._populated should be false"

    # test system - we should really grab the output of the system
    # command, but we aren't
    for cmd, pattern in testCmds:
        print "\nExecuting %s with default environment" % cmd
        assert not _shellEnv.system( cmd ), \
                "%s failed with default environment" % cmd
        line = _shellEnv._readLines(_shellEnv.execute(cmd).getInputStream())[0]
        assert re.match( pattern, line ), \
                "expected match for %s, got %s" % ( pattern, line )
    
    # trigger initialization of environment
    environ[ key ] = value
    
    assert environ._populated, \
            "after population, environ._populated should be true"
    assert org.get( key, None ) == value, \
            "expected stub to have %s set" % key
    assert environ.get( key, None ) == value, \
            "expected real environment to have %s set" % key

    # test system using the non-default environment - should really grab
    # output, but oh, well.
    for cmd, pattern in testCmds:
        print "\nExecuting %s with initialized environment" % cmd
        assert not _shellEnv.system( cmd ), \
                "%s failed with default environment" % cmd
        line = _shellEnv._readLines(_shellEnv.execute(cmd).getInputStream())[0]
        assert re.match( pattern, line ), \
                "expected match for %s, got %s" % ( pattern, line )
    
    assert environ.has_key( "PATH" ), \
            "expected environment to have PATH attribute " \
            "(this may not apply to all platforms!)"

