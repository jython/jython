"""
Implement subshell functionality for Jython.

This is mostly to provide the environ object for the os module,
and subshell execution functionality for os.system and popen* functions.

javashell attempts to determine a suitable command shell for the host
operating system, and uses that shell to determine environment variables
and to provide subshell execution functionality.
"""
from java.lang import System, Runtime
from java.io import IOException
from org.python.core import PyFile
from UserDict import UserDict
import jarray
import re
import string
import sys
#import threading
import types

__all__ = [ "shellexecute", "environ", "putenv", "getenv" ]

def __warn( *args ):
    print " ".join( [str( arg ) for arg in args ])
    
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
    environment.  Also supports subshell execution functions and provides
    empty environment support for platforms with unknown shell functionality.
    """
    def __init__( self, cmd=None, getEnv=None, keyTransform=None ):
        """Construct _ShellEnv instance.
        cmd: list of exec() arguments required to run a command in
            subshell, or None
        getEnv: shell command to list environment variables, or None
        keyTransform: normalization function for environment keys,
          such as 'string.upper', or None
        """
        self.cmd = cmd
        self.getEnv = getEnv
        self.environment = LazyDict(populate=self._getEnvironment,
                                    keyTransform=keyTransform)
        self._keyTransform = self.environment._keyTransform

    def execute( self, cmd ):
        """Execute cmd in a shell, and return the java.lang.Process instance.
        Accepts either a string command to be executed in a shell,
        or a sequence of [executable, args...].
        """
        shellCmd = self._formatCmd( cmd )

        if self.environment._populated:
            env = self._formatEnvironment( self.environment )
        else:
            env = None
        try:
            p = Runtime.getRuntime().exec( shellCmd, env )
            return p
        except IOException, ex:
            raise OSError(
                0,
                "Failed to execute command (%s): %s" % ( shellCmd, ex )
                )

    ########## utility methods
    def _formatCmd( self, cmd ):
        """Format a command for execution in a shell."""
        if self.cmd is None:
            msgFmt = "Unable to execute commands in subshell because shell" \
                     " functionality not implemented for OS %s with shell"  \
                     " setting %s. Failed command=%s""" 
            raise OSError( 0, msgFmt % ( _osType, _envType, cmd ))
            
        if isinstance(cmd, types.StringType):
            shellCmd = self.cmd + [cmd]
        else:
            shellCmd = cmd
            
        return shellCmd

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
                lines = PyFile( p.getInputStream() ).readlines()
                if '=' not in lines[0]:
                    __warn(
                        "Failed to get environment, getEnv command (%s) " \
                        "did not print environment as key=value lines.\n" \
                        "Output=%s" % ( self.getEnv, '\n'.join( lines ) )
                        )
                    return env

                for line in lines:
                    try:
                        i = line.index( '=' )
                        key = self._keyTransform(line[:i])
                        value = line[i+1:-1] # remove = and end-of-line
                    except ValueError:
                        # found no '=', treat line as part of previous value
                        value = '%s\n%s' % ( value, line[:-1] )
                    env[ key ] = value
            except OSError, ex:
                __warn( "Failed to get environment, environ will be empty:",
                        ex )
        return env

def _getOsType( os=None ):
    """Select the OS behavior based on os argument, 'python.os' registry
    setting and 'os.name' Java property.
    os: explicitly select desired OS. os=None to autodetect, os='None' to
    disable 
    """
    os = os or sys.registry.getProperty( "python.os" ) or \
               System.getProperty( "os.name" )
        
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

def _getShellEnv():
    # default to None/empty for shell and environment behavior
    shellCmd = None
    envCmd = None
    envTransform = None

    envType = sys.registry.getProperty("python.environment", "shell")
    if envType == "shell":
        osType = _getOsType()
        
        # override defaults based on osType
        if osType == "nt":
            shellCmd = ["cmd", "/c"]
            envCmd = "set"
            envTransform = string.upper
        elif osType == "dos":
            shellCmd = ["command.com", "/c"]
            envCmd = "set"
            envTransform = string.upper
        elif osType == "posix":
            shellCmd = ["sh", "-c"]
            envCmd = "env"
        elif osType == "mac":
            curdir = ':'  # override Posix directories
            pardir = '::' 
        elif osType == "None":
            pass
        # else:
        #    # may want a warning, but only at high verbosity:
        #    __warn( "Unknown os type '%s', using default behavior." % osType )

    return _ShellEnv( shellCmd, envCmd, envTransform )

_shellEnv = _getShellEnv()

# provide environ, putenv, getenv
environ = _shellEnv.environment
putenv = environ.__setitem__
getenv = environ.__getitem__

shellexecute = _shellEnv.execute

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
        p = shellexecute(cmd)
        line = PyFile( p.getInputStream() ).readlines()[0]
        assert re.match( pattern, line ), \
                "expected match for %s, got %s" % ( pattern, line )
        print "waiting for", cmd, "to complete"
        assert not p.waitFor(), \
                "%s failed with %s environment" % (cmd, whichEnv)
    
def _testSystem():
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

    # if environment is initialized and jython gets ARGS=-i, it thinks
    # it is running in interactive mode, and fails to exit until
    # process.getOutputStream().close()
    del environ[ "ARGS" ] 


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
    envCmd="echo This command does not print environment"    
    se2 = _ShellEnv( _shellEnv.cmd, envCmd, None )
    str(se2.environment) # trigger initialization
    assert not se2.environment.items(), "environment should be empty"
    
def _test():
    _testGetOsType()
    _testBadShell()
    _testBadGetEnv()
    _testSystem()
