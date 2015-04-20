#!/usr/bin/env python2.7 -E
# -*- coding: utf-8 -*-

# Launch script for Jython. It may be wrapped as an executable with
# tools like PyInstaller, creating jython.exe, or run directly. The
# installer will make this the default launcher under the name
# bin/jython if CPython 2.7 is available with the above shebang
# invocation.

import glob
import inspect
import os
import os.path
import pipes
import shlex
import subprocess
import sys
from collections import OrderedDict


is_windows = os.name == "nt" or (os.name == "java" and os._name == "nt")


def parse_launcher_args(args):
    class Namespace(object):
        pass
    parsed = Namespace()
    parsed.java = []
    parsed.properties = OrderedDict()
    parsed.boot = False
    parsed.jdb = False
    parsed.help = False
    parsed.print_requested = False
    parsed.profile = False
    parsed.jdb = None

    it = iter(args)
    next(it)  # ignore sys.argv[0]
    i = 1
    while True:
        try:
            arg = next(it)
        except StopIteration:
            break
        if arg.startswith("-D"):
            k, v = arg[2:].split("=")
            parsed.properties[k] = v
            i += 1
        elif arg in ("-J-classpath", "-J-cp"):
            try:
                next_arg = next(it)
            except StopIteration:
                bad_option("Argument expected for -J-classpath option")
            if next_arg.startswith("-"):
                bad_option("Bad option for -J-classpath")
            parsed.classpath = next_arg
            i += 2
        elif arg.startswith("-J-Xmx"):
            parsed.mem = arg[2:]
            i += 1
        elif arg.startswith("-J-Xss"):
            parsed.stack = arg[2:]
            i += 1
        elif arg.startswith("-J"):
            parsed.java.append(arg[2:])
            i += 1
        elif arg == "--print":
            parsed.print_requested = True
            i += 1
        elif arg in ("-h", "--help"):
            parsed.help = True
        elif arg in ("--boot", "--jdb", "--profile"):
            setattr(parsed, arg[2:], True)
            i += 1
        elif arg == "--":
            i += 1
            break
        else:
            break

    return parsed, args[i:]


class JythonCommand(object):

    def __init__(self, args, jython_args):
        self.args = args
        self.jython_args = jython_args

    @property
    def uname(self):
        if hasattr(self, "_uname"):
            return self._uname
        if is_windows:
            self._uname = "windows"
        else:
            uname = subprocess.check_output(["uname"]).strip().lower()
            if uname.startswith("cygwin"):
                self._uname = "cygwin"
            else:
                self._uname = uname
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
        if self.args.help:
            self._java_home = None
            self._java_command = "java"
            return
            
        if "JAVA_HOME" not in os.environ:
            self._java_home = None
            self._java_command = "jdb" if self.args.jdb else "java"
        else:
            self._java_home = os.environ["JAVA_HOME"]
            if self.uname == "cygwin":
                self._java_command = "jdb" if self.args.jdb else "java"
            else:
                self._java_command = os.path.join(
                    self.java_home, "bin",
                    "jdb" if self.args.jdb else "java")

    @property
    def executable(self):
        """Path to executable"""
        if hasattr(self, "_executable"):
            return self._executable
        # Modified from
        # http://stackoverflow.com/questions/3718657/how-to-properly-determine-current-script-directory-in-python/22881871#22881871
        if getattr(sys, "frozen", False): # py2exe, PyInstaller, cx_Freeze
            path = os.path.abspath(sys.executable)
        else:
            def inspect_this(): pass
            path = inspect.getabsfile(inspect_this)
        self._executable = os.path.realpath(path)
        return self._executable

    @property
    def jython_home(self):
        if hasattr(self, "_jython_home"):
            return self._jython_home
        if "JYTHON_HOME" in os.environ:
            self._jython_home = os.environ["JYTHON_HOME"]
        else:
            self._jython_home = os.path.dirname(os.path.dirname(self.executable))
        if self.uname == "cygwin":
            self._jython_home = subprocess.check_output(["cygpath", "--windows", self._jython_home]).strip()
        return self._jython_home

    @property
    def jython_opts():
        return os.environ.get("JYTHON_OPTS", "")

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
            bad_option("""{jython_home} contains neither jython-dev.jar nor jython.jar.
Try running this script from the 'bin' directory of an installed Jython or 
setting {envvar_specifier}JYTHON_HOME.""".format(
                    jython_home=self.jython_home,
                    envvar_specifier="%" if self.uname == "windows" else "$"))
        else:
            jars = [os.path.join(self.jython_home, "jython.jar")]
        self._jython_jars = jars
        return self._jython_jars

    @property
    def java_classpath(self):
        if hasattr(self.args, "classpath"):
            return self.args.classpath
        else:
            return os.environ.get("CLASSPATH", ".")

    @property
    def java_mem(self):
        if hasattr(self.args, "mem"):
            return self.args.mem
        else:
            return os.environ.get("JAVA_MEM", "-Xmx512m")

    @property
    def java_stack(self):
        if hasattr(self.args, "stack"):
            return self.args.stack
        else:
            return os.environ.get("JAVA_STACK", "-Xss1024k")

    @property
    def java_opts(self):
        return [self.java_mem, self.java_stack]
        
    @property
    def java_profile_agent(self):
        return os.path.join(self.jython_home, "javalib", "profile.jar")

    def set_encoding(self):
        if "JAVA_ENCODING" not in os.environ and self.uname == "darwin" and "file.encoding" not in self.args.properties:
            self.args.properties["file.encoding"] = "UTF-8"

    def convert(self, arg):
        if sys.stdout.encoding:
            return arg.encode(sys.stdout.encoding)
        else:
            return arg

    def make_classpath(self, jars):
        return self.classpath_delimiter.join(jars)

    def convert_path(self, arg):
        if self.uname == "cygwin":
            if not arg.startswith("/cygdrive/"):
                new_path = self.convert(arg).replace("/", "\\")
            else:
                new_path = subprocess.check_output(["cygpath", "-pw", self.convert(arg)]).strip()
            return new_path
        else:
            return self.convert(arg)

    @property
    def command(self):
        self.set_encoding()
        args = [self.java_command]
        args.extend(self.java_opts)
        args.extend(self.args.java)

        classpath = self.java_classpath
        jython_jars = self.jython_jars
        if self.args.boot:
            args.append("-Xbootclasspath/a:%s" % self.convert_path(self.make_classpath(jython_jars)))
        else:
            classpath = self.make_classpath(jython_jars) + self.classpath_delimiter + classpath
        args.extend(["-classpath", self.convert_path(classpath)])

        if "python.home" not in self.args.properties:
            args.append("-Dpython.home=%s" % self.convert_path(self.jython_home))
        if "python.executable" not in self.args.properties:
            args.append("-Dpython.executable=%s" % self.convert_path(self.executable))
        if "python.launcher.uname" not in self.args.properties:
            args.append("-Dpython.launcher.uname=%s" % self.uname)
        # Determines whether running on a tty for the benefit of
        # running on Cygwin. This step is needed because the Mintty
        # terminal emulator doesn't behave like a standard Microsoft
        # Windows tty, and so JNR Posix doesn't detect it properly.
        if "python.launcher.tty" not in self.args.properties:
            args.append("-Dpython.launcher.tty=%s" % str(os.isatty(sys.stdin.fileno())).lower())
        if self.uname == "cygwin" and "python.console" not in self.args.properties:
            args.append("-Dpython.console=org.python.core.PlainConsole")
        if self.args.profile:
            args.append("-XX:-UseSplitVerifier")
            args.append("-javaagent:%s" % self.convert_path(self.java_profile_agent))
        for k, v in self.args.properties.iteritems():
            args.append("-D%s=%s" % (self.convert(k), self.convert(v)))
        args.append("org.python.util.jython")
        if self.args.help:
            args.append("--help")
        args.extend(self.jython_args)
        return args


def bad_option(msg):
    print >> sys.stderr, """
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
JAVA_MEM   : Java memory (sets via -Xmx)
JAVA_OPTS  : options to pass directly to Java
JAVA_STACK : Java stack size (sets via -Xss)
JAVA_HOME  : Java installation directory
JYTHON_HOME: Jython installation directory
JYTHON_OPTS: default command line arguments
"""

def support_java_opts(args):
    it = iter(args)
    while it:
        arg = next(it)
        if arg.startswith("-D"):
            yield arg
        elif arg in ("-classpath", "-cp"):
            yield "-J" + arg
            try:
                yield next(it)
            except StopIteration:
                bad_option("Argument expected for -classpath option in JAVA_OPTS")
        else:
            yield "-J" + arg


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


def decode_args(sys_args):
    args = [sys_args[0]]

    def get_env_opts(envvar):
        opts = os.environ.get(envvar, "")
        if is_windows:
            return cmdline2list(opts)
        else:
            return shlex.split(opts)

    java_opts = get_env_opts("JAVA_OPTS")
    jython_opts = get_env_opts("JYTHON_OPTS")

    args.extend(support_java_opts(java_opts))
    args.extend(sys_args[1:])

    if sys.stdout.encoding:
        if sys.stdout.encoding.lower() == "cp65001":
            sys.exit("""Jython does not support code page 65001 (CP_UTF8).
Please try another code page by setting it with the chcp command.""")
        args = [arg.decode(sys.stdout.encoding) for arg in args]
        jython_opts = [arg.decode(sys.stdout.encoding) for arg in jython_opts]

    return args, jython_opts


def main(sys_args):
    sys_args, jython_opts = decode_args(sys_args)
    args, jython_args = parse_launcher_args(sys_args)
    jython_command = JythonCommand(args, jython_opts + jython_args)
    command = jython_command.command

    if args.profile and not args.help:
        try:
            os.unlink("profile.txt")
        except OSError:
            pass
    if args.print_requested and not args.help:
        if jython_command.uname == "windows":
            print subprocess.list2cmdline(jython_command.command)
        else:
            print " ".join(pipes.quote(arg) for arg in jython_command.command)
    else:
        if not (is_windows or not hasattr(os, "execvp") or args.help or jython_command.uname == "cygwin"):
            # Replace this process with the java process.
            #
            # NB such replacements actually do not work under Windows,
            # but if tried, they also fail very badly by hanging.
            # So don't even try!
            os.execvp(command[0], command[1:])
        else:
            result = 1
            try:
                result = subprocess.call(command)
                if args.help:
                    print_help()
            except KeyboardInterrupt:
                pass
            sys.exit(result)


if __name__ == "__main__":
    main(sys.argv)
