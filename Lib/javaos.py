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
           "defpath", "name"]

import java
from java.io import File, BufferedReader, InputStreamReader, IOException
import javapath as path
from UserDict import UserDict
import string
import exceptions
import re
import sys
import thread


error = OSError

name = 'java' # discriminate based on JDK version?
curdir = '.'  # default to Posix for directory behavior, override below
pardir = '..' 
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
            self.data = self.__populateFunc()
            self._populated = 1 # race condition

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


class _ShellEnv:
    """Provide environment derived by spawning a subshell and parsing its
    environment.  Also supports system functions and provides empty
    environment support for platforms with unknown shell
    functionality.
    """
    def __init__( self, cmd=None, getEnv=None, keyTransform=None ):
        """cmd: list of exec() arguments to run command in subshell, or None
        getEnv: shell command to list environment variables, or None
        keyTransform: normalization function for environment keys, or None
        """
        self.cmd = cmd
        self.getEnv = getEnv
        self.environment = LazyDict(populate=self._getEnvironment,
                                    keyTransform=keyTransform)
        self._keyTransform = self.environment._keyTransform

    ########## system
    def system( self, cmd ):
        """Imitate the standard library 'system' call.
        Execute 'cmd' in a shell, and send output to stdout & stderr.
        """
        p = self.execute( cmd )

        def println( arg, write=sys.stdout.write ):
            write( arg + "\n" )
        def printlnStdErr( arg, write=sys.stderr.write ):
            write( arg + "\n" )
            
        # read stderr in new thread
        thread.start_new_thread( self._readLines,
                                ( p.getErrorStream(), printlnStdErr ))
        # read stdin in main thread
        self._readLines( p.getInputStream(), println )
        
        return p.waitFor()

    def execute( self, cmd ):
        """Execute cmd in a shell, and return the process instance"""
        shellCmd = self._formatCmd( cmd )
        if self.environment._populated:
            env = self._formatEnvironment( self.environment )
        else:
            env = None
        try:
            p = java.lang.Runtime.getRuntime().exec( shellCmd, env )
            return p
        except IOException, ex:
            raise OSError(
                0,
                "Failed to execute command (%s): %s" % ( shellCmd, ex )
                )

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
        """Format a command for execution in a shell."""
        if self.cmd is None:
            msgFmt = "Unable to execute commands in subshell because shell" \
                     " functionality not implemented for OS %s with shell"  \
                     " setting %s. Failed command=%s""" 
            raise OSError( 0, msgFmt % ( _osType, _envType, cmd ))
            
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
        env = {}
        if self.getEnv:
            try:
                p = self.execute( self.getEnv )
                lines = self._readLines( p.getInputStream() )
                if '=' not in lines[0]:
                    print "getEnv command (%s) did not print environment.\n" \
                        "Output=%s" % (
                        self.getEnv, '\n'.join( lines )
                        )
                    return env

                for line in lines:
                    try:
                        i = line.index( '=' )
                        key = self._keyTransform(line[:i])
                        value = line[i+1:]
                    except ValueError:
                        # found no '=', so line is part of previous value
                        value = '%s\n%s' % ( value, line )
                    env[ key ] = value
            except OSError, ex:
                print "Failed to get environment, environ will be empty:", ex
        return env

def _getOsType( os=None ):
    """Select the OS behavior based on os argument, 'python.os' registry
    setting and 'os.name' Java property.
    os: explicitly select desired OS. os=None to autodetect, os='None' to
    disable 
    """
    os = os or sys.registry.getProperty( "python.os" ) or \
               java.lang.System.getProperty( "os.name" )
        
    _osTypeMap = (
        ( "nt", r"(nt)|(Windows NT)|(Windows NT 4.0)|(WindowsNT)|"
                r"(Windows 2000)|(Windows XP)|(Windows CE)" ),
        ( "dos", r"(dos)|(Windows 95)|(Windows 98)|(Windows ME)" ),
        ( "mac", r"(mac)|(MacOS.*)|(Darwin)" ),
        ( "None", r"(None)" ),
        ( "posix", r"(.*)" ), # default - posix seems to vary mast widely
        )
    for osType, pattern in _osTypeMap:
        if re.match( pattern, os ):
            break
    return osType

def _getShellEnv( envType, shellCmd, envCmd, envTransform ):
    """Create the desired environment type.
    envType: 'shell' or None
    """
    if envType == "shell":
        return _ShellEnv( shellCmd, envCmd, envTransform )
    else:
        return _ShellEnv()
    
_osType = _getOsType()
_envType = sys.registry.getProperty("python.environment", "shell")

# default to None/empty for shell and environment behavior
_shellCmd = None
_envCmd = None
_envTransform = None

# override defaults based on _osType
if _osType == "nt":
    _shellCmd = ["cmd", "/c"]
    _envCmd = "set"
    _envTransform = string.upper
elif _osType == "dos":
    _shellCmd = ["command.com", "/c"]
    _envCmd = "set"
    _envTransform = string.upper
elif _osType == "posix":
    _shellCmd = ["sh", "-c"]
    _envCmd = "env"
elif _osType == "mac":
    curdir = ':'  # override Posix directories
    pardir = '::' 
elif _osType == "None":
    pass
# else:
#    # may want a warning, but only at high verbosity:
#    warn( "Unknown os type '%s', using default behavior." % _osType )

_shellEnv = _getShellEnv( _envType, _shellCmd, _envCmd, _envTransform )

# provide environ, putenv, getenv
environ = _shellEnv.environment
putenv = environ.__setitem__
getenv = environ.__getitem__
# provide system
system = _shellEnv.system

########## test code
def _testGetOsType():
    testVals = {
        "Windows NT": "nt",
        "Windows 95": "dos",
        "MacOS": "mac",
        "Solaris": "posix",
        "Linux": "posix",
        "None": "None"
        }

    msgFmt = "_getOsType( '%s' ) should return '%s', not '%s'"
    # test basic mappings
    for key, val in testVals.items():
        got = _getOsType( key )
        assert got == val, msgFmt % ( key, val, got )

def _testCmds( _shellEnv, testCmds, whichEnv ):
    # test commands (key) and compare output to expected output (value).
    # this actually executes all the commands twice, testing the return
    # code by calling system(), and testing some of the output by calling
    # execute()
    for cmd, pattern in testCmds:
        print "\nExecuting '%s' with %s environment" % (cmd, whichEnv)
        assert not _shellEnv.system( cmd ), \
                "%s failed with %s environment" % (cmd, whichEnv)
        line = _shellEnv._readLines(
            _shellEnv.execute(cmd).getInputStream())[0]
        assert re.match( pattern, line ), \
                "expected match for %s, got %s" % ( pattern, line )
    
def _testSystem( shellEnv=_shellEnv ):
    # test system and environment functionality
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
        # should print 'testKey=' on 95 before initialization,
        # and 'testKey=testValue' after
        ("echo %s=%%%s%%" % (key,key),
                "(%s=)" % (key,)),     
        # should print PATH (on Unix)
        ( "echo PATH=$PATH", "PATH=.*" ),
        # should print 'testKey=testValue' on Unix after initialization
        ( "echo %s=$%s" % (key,key),
                "(%s=$%s)|(%s=)|(%s=%s)" % (key, key, key, key, value ) ), 
        # should output quotes on NT but not on Unix
        ( 'echo "hello there"', '"?hello there"?' ),
        # should print 'why' to stdout. 
        ( r'''jython -c "import sys;sys.stdout.write( 'why\n' )"''', "why" ),
        # should print 'why' to stderr, but it won't right now.  Have
        # to add the print to give some output...empty string matches every
        # thing...
        ( r'''jython -c "import sys;sys.stderr.write('why\n');print " ''',
          "" )
        ]
    
    assert not environ._populated, \
            "before population, environ._populated should be false"

    _testCmds( _shellEnv, testCmds, "default" )
    
    # trigger initialization of environment
    environ[ key ] = value
    
    assert environ._populated, \
            "after population, environ._populated should be true"
    assert org.get( key, None ) == value, \
            "expected stub to have %s set" % key
    assert environ.get( key, None ) == value, \
            "expected real environment to have %s set" % key

    # test system using the non-default environment
    _testCmds( _shellEnv, testCmds, "initialized" )
    
    assert environ.has_key( "PATH" ), \
            "expected environment to have PATH attribute " \
            "(this may not apply to all platforms!)"

def _testBadShell():
    # attempt to get an environment with a shell that is not startable
    se2 = _ShellEnv( ["badshell", "-c"], "set" )
    str(se2.environment) # trigger initialization
    assert not se2.environment.items(), "environment should be empty"

def _testBadGetEnv():
    # attempt to get an environment with a command that does not print an environment
    se2 = _getShellEnv( "shell", _shellCmd, _envCmd, _envTransform )
    se2.getEnv="echo This command does not print environment"
    str(se2.environment) # trigger initialization
    assert not se2.environment.items(), "environment should be empty"
    
def _test():
    _testGetOsType()
    _testBadShell()
    _testBadGetEnv()
    _testSystem()
        
