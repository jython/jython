#!/usr/bin/env python2.7
# -*- coding: utf-8 -*-

# Launch script for Jython. It may be wrapped as an executable with
# tools like PyInstaller, creating jython.exe, or run directly. The
# installer will make this the default launcher under the name
# bin/jython if CPython 2.7 is available with the above shebang
# invocation.

import argparse
import glob
import inspect
import os
import os.path
import pipes
import subprocess
import sys
from collections import OrderedDict


is_windows = os.name == "nt" or (os.name == "java" and os._name == "nt")


def make_parser(provided_args):
    parser = argparse.ArgumentParser(description="Jython", add_help=False)
    parser.add_argument("-D", dest="properties", action="append")
    parser.add_argument("-J", dest="java", action="append")
    parser.add_argument("--boot", action="store_true")
    parser.add_argument("--jdb", action="store_true")
    parser.add_argument("--help", "-h", action="store_true")
    parser.add_argument("--print", dest="print_requested", action="store_true")
    parser.add_argument("--profile", action="store_true")
    args, remainder = parser.parse_known_args(provided_args)

    items = args.java or []
    args.java = []
    for item in items:
        if item.startswith("-Xmx"):
            args.mem = item
        elif item.startswith("-Xss"):
            args.stack = item
        else:
            args.java.append(item)
    
    # need to account for the fact that -c and -cp/-classpath are ambiguous options as far
    # as argparse is concerned, so parse separately
    args.classpath = []
    r = iter(remainder)
    r2 = []
    while True:
        try:
            arg = next(r)
        except StopIteration:
            break
        if arg == "-cp" or arg == "-classpath":
            try:
                args.classpath = next(r)
                if args.classpath.startswith("-"):
                    parser.error("Invalid classpath for -classpath: %s" % repr(args.classpath)[1:])
            except StopIteration:
                parser.error("-classpath requires an argument")
        else:
            r2.append(arg)
    remainder = r2

    if args.properties is None:
        args.properties = []
    props = OrderedDict()
    for kv in args.properties:
        k, v = kv.split("=")
        props[k] = v
    args.properties = props
    args.encoding = args.properties.get("file.encoding", None)

    return parser, args


class JythonCommand(object):

    def __init__(self, parser, args, jython_args):
        self.parser = parser
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
        if "JAVA_HOME" not in os.environ:
            self._java_home = None
            self._java_command = "jdb" if self.args.jdb else "java"
        else:
            self._java_home = os.environ["JAVA_HOME"]
            #if self.uname == "cygwin":
            #    self._java_home = subprocess.check_output(["cygpath", "--windows", self._java_home]).strip()
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
    def classpath(self):
        if hasattr(self, "_classpath"):
            return self._classpath
        if os.path.exists(os.path.join(self.jython_home, "jython-dev.jar")):
            jars = [os.path.join(self.jython_home, "jython-dev.jar")]
            jars.append(os.path.join(self.jython_home, "javalib", "*"))
        elif not os.path.exists(os.path.join(self.jython_home, "jython.jar")): 
            self.parser.error(
"""{executable}:
{jython_home} contains neither jython-dev.jar nor jython.jar.
Try running this script from the 'bin' directory of an installed Jython or 
setting {envvar_specifier}JYTHON_HOME.""".\
                format(
                    executable=self.executable,
                    jython_home=self.jython_home,
                    envvar_specifier="%" if self.uname == "windows" else "$"))
        else:
            jars = [os.path.join(self.jython_home, "jython.jar")]
        self._classpath = self.classpath_delimiter.join(jars)
        if self.args.classpath and not self.args.boot:
            self._classpath += self.classpath_delimiter + self.args.classpath
        return self._classpath

    @property
    def java_mem(self):
        if hasattr(self.args.java, "mem"):
            return self.args.java.mem
        else:
            return os.environ.get("JAVA_MEM", "-Xmx512m")

    @property
    def java_stack(self):
        if hasattr(self.args.java, "stack"):
            return self.args.java.mem
        else:
            return os.environ.get("JAVA_STACK", "-Xss1024k")

    @property
    def java_opts(self):
        if "JAVA_OPTS" in os.environ:
            options = os.environ["JAVA_OPTS"].split()
        else:
            options = []
        options.extend([self.java_mem, self.java_stack])
        return options
        
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
        if self.args.boot:
            args.append("-Xbootclasspath/a:%s" % self.convert_path(self.classpath))
            if self.args.classpath:
                args.extend(["-classpath", self.convert_path(self.args.classpath)])
        else:
            args.extend(["-classpath", self.convert_path(self.classpath)])
        if "python.home" not in self.args.properties:
            args.append("-Dpython.home=%s" % self.convert_path(self.jython_home))
        if "python.executable" not in self.args.properties:
            args.append("-Dpython.executable=%s" % self.convert_path(self.executable))
        if "python.launcher.uname" not in self.args.properties:
            args.append("-Dpython.launcher.uname=%s" % self.uname)
        # determine if is-a-tty for the benefit of running on cygwin - mintty doesn't behave like
        # a standard windows tty and so JNR posix doesn't detect it properly
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


def print_help():
    print >> sys.stderr, """
Jython launcher options:
-Jarg    : pass argument through to Java VM (e.g. -J-Xmx512m)
--jdb    : run under JDB
--print  : print the Java command instead of executing it
--profile: run with the Java Interactive Profiler (http://jiprof.sf.net)
--boot   : put jython on the boot classpath (disables the bytecode verifier)
--       : pass remaining arguments through to Jython
Jython launcher environment variables:
JAVA_HOME  : Java installation directory
JYTHON_HOME: Jython installation directory
JYTHON_OPTS: default command line arguments
"""


def split_launcher_args(args):
    it = iter(args)
    i = 1
    next(it)
    while True:
        try:
            arg = next(it)
        except StopIteration:
            break
        if arg.startswith("-D") or arg.startswith("-J") or \
           arg in ("--boot", "--jdb", "--help", "--print", "--profile"):
            i += 1
        elif arg in ("-cp", "-classpath"):
            i += 1
            try:
                next(it)
                i += 1
            except StopIteration:
                break  # will be picked up in argparse, where an error will be raised
        elif arg == "--":
            i += 1
            break
        else:
            break
    return args[:i], args[i:]


def main():
    if sys.stdout.encoding:
        if sys.stdout.encoding.lower() == "cp65001":
            sys.exit("""Jython does not support code page 65001 (CP_UTF8).
Please try another code page by setting it with the chcp command.""")
        sys.argv = [arg.decode(sys.stdout.encoding) for arg in sys.argv]
    launcher_args, jython_args = split_launcher_args(sys.argv)
    parser, args = make_parser(launcher_args)
    jython_command = JythonCommand(parser, args, jython_args)
    command = jython_command.command

    if args.profile:
        try:
            os.unlink("profile.txt")
        except OSError:
            pass
    if args.print_requested:
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
    main()
