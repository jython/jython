#!/usr/bin/env python2.7
# -*- coding: utf-8 -*-

# Launch script for Jython. It may be run directly (note the shebang line), but
# importantly it supplies python.exe, the launcher we use on Windows.
#
# Each time this file changes, we must regenerate an executable with
# PyInstaller, using the command:
#
#    pyinstaller --onefile jython.py
#
# This is best done in a virtual environment (more about this in the Jython
# Developers' Guide).

import glob
import os
import os.path
import shlex
import subprocess
import sys
from collections import OrderedDict


is_windows = os.name == "nt" or (os.name == "java" and os._name == "nt")

# A note about encoding:
#
# A major motivation for this program is to launch Jython on Windows, where
# console and file encoding may be different. Command-line arguments and
# environment variables are presented in Python 2.7 as byte-data, encoded
# "somehow". It becomes important to know which decoding to use as soon as
# paths may contain non-ascii characters. It is not the console encoding.
# Experiment shows that sys.getfilesystemencoding() is generally applicable
# to arguments, environment variables and spawning a subprocess.
#
# On a Windows 10 box, this comes up with pseudo-codec 'mbcs'. This supports
# European accented characters pretty well.
#
# When localised to Chinese(simplified) the FS encoding mbcs includes many
# more points than cp936 (the console encoding), although it still struggles
# with European accented characters.

ENCODING = sys.getfilesystemencoding() or "utf-8"


def get_env(envvar, default=None):
    """ Return the named environment variable, decoded to Unicode."""
    v = os.environ.get(envvar, default)
    # Result may be bytes but we want unicode for the command
    if isinstance(v, bytes):
        v = v.decode(ENCODING)
    # Remove quotes sometimes necessary around the value
    if v is not None and v.startswith('"') and v.endswith('"'):
        v = v[1:-1]
    return v

def get_env_mem(envvar, default):
    """ Return the named memory environment variable, decoded to Unicode.
        The default should begin with -Xmx or -Xss as in the java command,
        but this part will be added to the environmental value if missing.
    """
    # Tolerate default given as bytes, as we're bound to forget sometimes
    if isinstance(default, bytes):
        default = default.decode(ENCODING)
    v = os.environ.get(envvar, default)
    # Result may be bytes but we want unicode for the command
    if isinstance(v, bytes):
        v = v.decode(ENCODING)
    # Accept either a form like 16m or one like -Xmx16m
    if not v.startswith(u"-X"):
        v = default[:4] + v
    return v

def encode_list(args, encoding=ENCODING):
    """ Convert list of Unicode strings to list of encoded byte strings."""
    r = []
    for a in args:
        if not isinstance(a, bytes): a = a.encode(encoding)
        r.append(a)
    return r

def decode_list(args, encoding=ENCODING):
    """ Convert list of byte strings to list of Unicode strings."""
    r = []
    for a in args:
        if not isinstance(a, unicode): a = a.decode(encoding)
        r.append(a)
    return r

def parse_launcher_args(args):
    """ Process the given argument list into two objects, the first part being
        a namespace of checked arguments to the interpreter itself, and the rest
        being the Python program it will run and its arguments.
    """
    class Namespace(object):
        pass
    parsed = Namespace()
    parsed.boot = False # --boot flag given
    parsed.jdb = False # --jdb flag given
    parsed.help = False # --help or -h flag given
    parsed.print_requested = False # --print flag given
    parsed.profile = False # --profile flag given
    parsed.properties = OrderedDict() # properties to give the JVM
    parsed.java = [] # any other arguments to give the JVM
    unparsed = list()

    it = iter(args)
    next(it)  # ignore sys.argv[0]
    i = 1
    while True:
        try:
            arg = next(it)
        except StopIteration:
            break
        if arg.startswith(u"-D"):
            k, v = arg[2:].split(u"=")
            parsed.properties[k] = v
            i += 1
        elif arg in (u"-J-classpath", u"-J-cp"):
            try:
                next_arg = next(it)
            except StopIteration:
                bad_option("Argument expected for -J-classpath option")
            if next_arg.startswith("-"):
                bad_option("Bad option for -J-classpath")
            parsed.classpath = next_arg
            i += 2
        elif arg.startswith(u"-J-Xmx"):
            parsed.mem = arg[2:]
            i += 1
        elif arg.startswith(u"-J-Xss"):
            parsed.stack = arg[2:]
            i += 1
        elif arg.startswith(u"-J"):
            parsed.java.append(arg[2:])
            i += 1
        elif arg == u"--print":
            parsed.print_requested = True
            i += 1
        elif arg in (u"-h", u"--help"):
            parsed.help = True
        elif arg in (u"--boot", u"--jdb", u"--profile"):
            setattr(parsed, arg[2:], True)
            i += 1
        elif len(arg) >= 2 and arg[0] == u'-' and arg[1] in u"BEisSuvV3":
            unparsed.append(arg)
            i += 1
        elif arg == u"--":
            i += 1
            break
        else:
            break

    unparsed.extend(args[i:])
    return parsed, unparsed


class JythonCommand(object):

    def __init__(self, args, jython_args):
        self.args = args
        self.jython_args = jython_args

    @property
    def uname(self):
        if hasattr(self, "_uname"):
            return self._uname
        if is_windows:
            self._uname = u"windows"
        else:
            uname = subprocess.check_output(["uname"]).strip().lower()
            if uname.startswith("cygwin"):
                self._uname = u"cygwin"
            else:
                self._uname = uname.decode(ENCODING)
        return self._uname

    @property
    def java_home(self):
        if not hasattr(self, "_java_home"):
            self.setup_java_command()
        return self._java_home

    @property
    def java_command(self):
        if not hasattr(self, "_java_command"):
            self.setup_java_command()
        return self._java_command

    def setup_java_command(self):
        """ Sets java_home and java_command according to environment and parsed
            launcher arguments --jdb and --help.
        """
        if self.args.help:
            self._java_home = None
            self._java_command = u"java"
            return

        command = u"jdb" if self.args.jdb else u"java"

        self._java_home = get_env("JAVA_HOME")
        if self._java_home is None or self.uname == u"cygwin":
            # Assume java or jdb on the path
            self._java_command = command
        else:
            # Assume java or jdb in JAVA_HOME/bin
            self._java_command = os.path.join(self._java_home, u"bin", command)

    @property
    def executable(self):
        """Path to executable"""
        if hasattr(self, "_executable"):
            return self._executable
        # Modified from
        # http://stackoverflow.com/questions/3718657/how-to-properly-determine-current-script-directory-in-python/22881871#22881871
        if getattr(sys, "frozen", False): # py2exe, PyInstaller, cx_Freeze
            # Frozen. Let it go with the executable path.
            bytes_path = sys.executable
        else:
            # Not frozen. Use the __file__ of this module.
            bytes_path = __file__
        # Python 2 thinks in bytes. Carefully normalise in Unicode.
        path = os.path.realpath(bytes_path.decode(ENCODING))
        try:
            # If shorter, make this relative to the CWD.
            relpath = os.path.relpath(path, os.getcwdu())
            if len(relpath) < len(path): path = relpath
        except ValueError:
            # Many reasons why this might be impossible: use an absolute path.
            path = os.path.abspath(path)
        self._executable = path
        return self._executable

    @property
    def jython_home(self):
        if hasattr(self, "_jython_home"):
            return self._jython_home
        home = get_env("JYTHON_HOME")
        if home is None:
            # Not just dirname twice in case dirname(executable) == ''
            home = os.path.join(os.path.dirname(self.executable), u'..')
        # This could be a relative path like .\..
        home = os.path.normpath(home)
        if self.uname == u"cygwin":
            # Even on Cygwin, we need a Windows-style path for this
            home = unicode_subprocess(["cygpath", "--windows", home])
        self._jython_home = home
        return self._jython_home

    @property
    def jython_opts():
        return get_env("JYTHON_OPTS", "")

    @property
    def classpath_delimiter(self):
        return ";" if (is_windows or self.uname == "cygwin") else ":"

    @property
    def jython_jars(self):
        if hasattr(self, "_jython_jars"):
            return self._jython_jars
        if os.path.exists(os.path.join(self.jython_home, "jython-dev.jar")):
            jars = [os.path.join(self.jython_home, "jython-dev.jar")]
            if self.args.boot:
                # Wildcard expansion does not work for bootclasspath
                for jar in glob.glob(os.path.join(self.jython_home, "javalib", "*.jar")):
                    jars.append(jar)
            else:
                jars.append(os.path.join(self.jython_home, "javalib", "*"))
        elif not os.path.exists(os.path.join(self.jython_home, "jython.jar")): 
            bad_option(u"""{} contains neither jython-dev.jar nor jython.jar.
Try running this script from the 'bin' directory of an installed Jython or 
setting JYTHON_HOME.""".format(self.jython_home))
        else:
            jars = [os.path.join(self.jython_home, "jython.jar")]
        self._jython_jars = jars
        return self._jython_jars

    @property
    def java_classpath(self):
        if hasattr(self.args, "classpath"):
            return self.args.classpath
        else:
            return get_env("CLASSPATH", ".")

    @property
    def java_mem(self):
        if hasattr(self.args, "mem"):
            return self.args.mem
        else:
            return get_env_mem("JAVA_MEM", "-Xmx512m")

    @property
    def java_stack(self):
        if hasattr(self.args, "stack"):
            return self.args.stack
        else:
            return get_env_mem("JAVA_STACK", "-Xss2560k")

    @property
    def java_opts(self):
        return [self.java_mem, self.java_stack]

    @property
    def java_profile_agent(self):
        return os.path.join(self.jython_home, "javalib", "profile.jar")

    def set_encoding(self):
        if "JAVA_ENCODING" not in os.environ and self.uname == "darwin" and "file.encoding" not in self.args.properties:
            self.args.properties["file.encoding"] = "UTF-8"

    def make_classpath(self, jars):
        return self.classpath_delimiter.join(jars)

    def convert_path(self, arg):
        if self.uname == u"cygwin":
            if not arg.startswith(u"/cygdrive/"):
                return arg.replace(u"/", u"\\")
            else:
                arg = arg.replace('*', r'\*') # prevent globbing
                return unicode_subprocess(["cygpath", "-pw", arg])
        else:
            return arg

    def unicode_subprocess(self, unicode_command):
        """ Launch a command with subprocess.check_output() and read the
            output, except everything is expected to be in Unicode.
        """
        cmd = []
        for c in unicode_command:
            if isinstance(c, bytes):
                cmd.append(c)
            else:
                cmd.append(c.encode(ENCODING))
        return subprocess.check_output(cmd).strip().decode(ENCODING)

    @property
    def command(self):
        # Set default file encoding for just for Darwin (?)
        self.set_encoding()

        # Begin to build the Java part of the ultimate command
        args = [self.java_command]
        args.extend(self.java_opts)
        args.extend(self.args.java)

        # Get the class path right (depends on --boot)
        classpath = self.java_classpath
        jython_jars = self.jython_jars
        if self.args.boot:
            args.append(u"-Xbootclasspath/a:%s" % self.convert_path(self.make_classpath(jython_jars)))
        else:
            classpath = self.make_classpath(jython_jars) + self.classpath_delimiter + classpath
        args.extend([u"-classpath", self.convert_path(classpath)])

        if "python.home" not in self.args.properties:
            args.append(u"-Dpython.home=%s" % self.convert_path(self.jython_home))
        if "python.executable" not in self.args.properties:
            args.append(u"-Dpython.executable=%s" % self.convert_path(self.executable))
        if "python.launcher.uname" not in self.args.properties:
            args.append(u"-Dpython.launcher.uname=%s" % self.uname)

        # Determine whether running on a tty for the benefit of
        # running on Cygwin. This step is needed because the Mintty
        # terminal emulator doesn't behave like a standard Microsoft
        # Windows tty, and so JNR Posix doesn't detect it properly.
        if "python.launcher.tty" not in self.args.properties:
            args.append(u"-Dpython.launcher.tty=%s" % str(os.isatty(sys.stdin.fileno())).lower())
        if self.uname == u"cygwin" and "python.console" not in self.args.properties:
            args.append(u"-Dpython.console=org.python.core.PlainConsole")

        if self.args.profile:
            args.append(u"-XX:-UseSplitVerifier")
            args.append(u"-javaagent:%s" % self.convert_path(self.java_profile_agent))

        for k, v in self.args.properties.iteritems():
            args.append(u"-D%s=%s" % (k, v))

        args.append(u"org.python.util.jython")

        if self.args.help:
            args.append(u"--help")

        args.extend(self.jython_args)
        return args


def bad_option(msg):
    print >> sys.stderr, u"""
{msg}
usage: jython [option] ... [-c cmd | -m mod | file | -] [arg] ...
Try `jython -h' for more information.
""".format(msg=msg)
    sys.exit(2)


def print_help():
    print >> sys.stderr, """
Jython launcher-specific options:
-Dname=value : pass name=value property to Java VM (e.g. -Dpython.path=/a/b/c)
-Jarg    : pass argument through to Java VM (e.g. -J-Xmx512m)
--boot   : speeds up launch performance by putting Jython jars on the boot classpath
--help   : this help message
--jdb    : run under JDB java debugger
--print  : print the Java command with args for launching Jython instead of executing it
--profile: run with the Java Interactive Profiler (http://jiprof.sf.net)
--       : pass remaining arguments through to Jython
Jython launcher environment variables:
JAVA_MEM   : Java memory size as a java option e.g. -Xmx600m or just 600m
JAVA_STACK : Java stack size as a java option e.g. -Xss5120k or just 5120k
JAVA_OPTS  : options to pass directly to Java
JAVA_HOME  : Java installation directory
JYTHON_HOME: Jython installation directory
JYTHON_OPTS: default command line arguments
"""

def support_java_opts(args):
    """ Generator from options intended for the JVM. Options beginning -D go
        through unchanged, others are prefixed with -J.
    """
    # Input is expected to be Unicode, but just in case ...
    if isinstance(args, bytes): args = args.decode(ENCODING)
    it = iter(args)
    while it:
        arg = next(it)
        if arg.startswith(u"-D"):
            yield arg
        elif arg in (u"-classpath", u"-cp"):
            yield u"-J" + arg
            try:
                yield next(it)
            except StopIteration:
                bad_option("Argument expected for -classpath option in JAVA_OPTS")
        else:
            yield u"-J" + arg


# copied from subprocess module in Jython; see
# http://bugs.python.org/issue1724822 where it is discussed to include
# in Python 3.x for shlex:
def cmdline2list(cmdline):
    """Build an argv list from a Microsoft shell style cmdline str

    The reverse of list2cmdline that follows the same MS C runtime
    rules.
    """
    whitespace = ' \t'
    # count of preceding '\'
    bs_count = 0
    in_quotes = False
    arg = []
    argv = []

    for ch in cmdline:
        if ch in whitespace and not in_quotes:
            if arg:
                # finalize arg and reset
                argv.append(''.join(arg))
                arg = []
            bs_count = 0
        elif ch == '\\':
            arg.append(ch)
            bs_count += 1
        elif ch == '"':
            if not bs_count % 2:
                # Even number of '\' followed by a '"'. Place one
                # '\' for every pair and treat '"' as a delimiter
                if bs_count:
                    del arg[-(bs_count / 2):]
                in_quotes = not in_quotes
            else:
                # Odd number of '\' followed by a '"'. Place one '\'
                # for every pair and treat '"' as an escape sequence
                # by the remaining '\'
                del arg[-(bs_count / 2 + 1):]
                arg.append(ch)
            bs_count = 0
        else:
            # regular char
            arg.append(ch)
            bs_count = 0

    # A single trailing '"' delimiter yields an empty arg
    if arg or in_quotes:
        argv.append(''.join(arg))

    return argv

def get_env_opts(envvar):
    """ Return a list of the values in the named environment variable,
        split according to shell conventions, and decoded to Unicode.
    """
    opts = os.environ.get(envvar, "") # bytes at this point
    if is_windows:
        opts = cmdline2list(opts)
    else:
        opts = shlex.split(opts)
    return decode_list(opts)

def maybe_quote(s):
    """ Enclose the string argument in single quotes if it looks like it needs it.
        Spaces and quotes will trigger; single quotes in the argument are escaped.
        This is only used to compose the --print output so need only satisfy shlex.
    """
    NEED_QUOTE = u" \t\"\\'"
    clean = True
    for c in s:
        if c in NEED_QUOTE:
            clean = False
            break
    if clean: return s
    # Something needs quoting or escaping.
    QUOTE = u"'"
    ESC = u"\\"
    arg = [QUOTE]
    for c in s:
        if c == QUOTE:
            arg.append(QUOTE)
            arg.append(ESC)
            arg.append(QUOTE)
        elif c == ESC:
            arg.append(ESC)
        arg.append(c)
    arg.append(QUOTE)
    return ''.join(arg)

def main(sys_args):
    # The entire program must work in Unicode
    sys_args = decode_list(sys_args)

    # sys_args[0] is this script (which we'll replace with 'java' eventually).
    # Insert options for the java command from the environment.
    sys_args[1:1] = support_java_opts(get_env_opts("JAVA_OPTS"))

    # Parse the composite arguments (yes, even the ones from JAVA_OPTS),
    # and return the "unparsed" tail considered arguments for Jython itself.
    args, jython_args = parse_launcher_args(sys_args)

    # Build the data from which we can generate the command ultimately.
    # Jython options supplied from the environment stand in front of the
    # unparsed tail from the command line. 
    jython_opts = get_env_opts("JYTHON_OPTS")
    jython_command = JythonCommand(args, jython_opts + jython_args)

    # This is the "fully adjusted" command to launch, but still as Unicode.
    command = jython_command.command

    if args.profile and not args.help:
        try:
            os.unlink("profile.txt")
        except OSError:
            pass

    if args.print_requested and not args.help:
        if jython_command.uname == u"windows":
            # Add escapes and quotes necessary to Windows.
            # Normally used for a byte strings but Python is tolerant :)
            command_line = subprocess.list2cmdline(command)
        else:
            # Transform any element that seems to need quotes
            command = map(maybe_quote, command)
            # Now concatenate with spaces
            command_line = u" ".join(command)
        # It is possible the Unicode cannot be encoded for the console
        enc = sys.stdout.encoding or 'ascii'
        sys.stdout.write(command_line.encode(enc, 'replace') + "\n")
    else:
        try:
            if not (is_windows or not hasattr(os, "execvp") or args.help or 
                    jython_command.uname == u"cygwin"):
                # Replace this process with the java process.
                #
                # NB such replacements actually do not work under Windows,
                # but if tried, they also fail very badly by hanging.
                # So don't even try!
                command = encode_list(command)
                os.execvp(command[0], command[1:])
            else:
                result = 1
                try:
                    result = subprocess.call(encode_list(command))
                    if args.help:
                        print_help()
                except KeyboardInterrupt:
                    pass
                sys.exit(result)
        except OSError as e:
            print >> sys.stderr, "Failed to launch Jython using command:",\
                    command[0], "...\n", \
                    "    Use the --print option to see the command in full."
            if jython_command.java_home:
                print >> sys.stderr, "    Launcher used JAVA_HOME =",\
                    jython_command.java_home
            else:
                print >> sys.stderr, "    Check PATH for java/jdb command."
            print >> sys.stderr, e
            sys.exit(1)


if __name__ == "__main__":
    main(sys.argv)
