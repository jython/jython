'''Tests jython.bat using the --print option'''

import os
import sys
import unittest
import tempfile

from test import test_support

from java.lang import IllegalThreadStateException
from java.lang import Runtime
from java.lang import System
from java.lang import Thread
from java.io import File
from java.io import BufferedReader;
from java.io import InputStreamReader;

class Monitor(Thread):
    def __init__(self, process):
        self.process = process
        self.output = ''

    def run(self):
        reader = BufferedReader(InputStreamReader(self.getStream()))
        try:
            line = reader.readLine()
            while line:
                self.output += line
                line = reader.readLine()
        finally:
            reader.close()
    
    def getOutput(self):
        return self.output

class StdoutMonitor(Monitor):
    def __init_(self, process):
        Monitor.__init__(self, process)

    def getStream(self):
        return self.process.getInputStream()

class StderrMonitor(Monitor):
    def __init_(self, process):
        Monitor.__init__(self, process)

    def getStream(self):
        return self.process.getErrorStream()

class StarterProcess:
    def writeStarter(self, args, javaHome, jythonHome, jythonOpts):
        (starter, starterPath) = tempfile.mkstemp(suffix='.bat', prefix='starter', text=True)
        starter.close()
        outfilePath = starterPath[:-4] + '.out'
        starter = open(starterPath, 'w') # open starter as simple file
        try:
            if javaHome:
                starter.write('set JAVA_HOME=%s\n' % javaHome)
            if jythonHome:
                starter.write('set JYTHON_HOME=%s\n' % jythonHome)
            if jythonOpts:
                starter.write('set JYTHON_OPTS=%s\n' % jythonOpts)
            starter.write(self.buildCommand(args, outfilePath))
            return (starterPath, outfilePath)
        finally:
            starter.close()

    def buildCommand(self, args, outfilePath):
        line = ''
        for arg in args:
            line += arg
            line += ' '
        line += '> '
        line += outfilePath
        line += ' 2>&1'
        return line

    def getOutput(self, outfilePath):
        lines = ''
        outfile = open(outfilePath, 'r')
        try:
            for line in outfile.readlines():
                lines += line
        finally:
            outfile.close()
        return lines

    def isAlive(self, process):
        try:
            process.exitValue()
            return False
        except IllegalThreadStateException:
            return True

    def run(self, args, javaHome, jythonHome, jythonOpts):
        ''' creates a start script, executes it and captures the output '''
        (starterPath, outfilePath) = self.writeStarter(args, javaHome, jythonHome, jythonOpts)
        try:
            process = Runtime.getRuntime().exec(starterPath)
            stdoutMonitor = StdoutMonitor(process)
            stderrMonitor = StderrMonitor(process)
            stdoutMonitor.start()
            stderrMonitor.start()
            while self.isAlive(process):
                Thread.sleep(300)
            return self.getOutput(outfilePath)
        finally:
            os.remove(starterPath)
            os.remove(outfilePath)

class BaseTest(unittest.TestCase):
    def quote(self, s):
        return '"' + s + '"'

    def unquote(self, s):
        if len(s) > 0:
            if s[:1] == '"':
                s = s[1:]
        if len(s) > 0:
            if s[-1:] == '"':
                s = s[:-1]
        return s

    def getHomeDir(self):
        ex = sys.executable
        tail = ex[-15:]
        if tail == '\\bin\\jython.bat':
            home = ex[:-15]
        else:
            home = ex[:-11] # \jython.bat
        return home

    def assertOutput(self, flags=None, javaHome=None, jythonHome=None, jythonOpts=None):
        args = [self.quote(sys.executable), '--print']
        memory = None
        stack = None
        prop = None
        jythonArgs = None
        boot = False
        jdb = False
        if flags:
            for flag in flags:
                if flag[:2] == '-J':
                    if flag[2:6] == '-Xmx':
                        memory = flag[6:]
                    elif flag[2:6] == '-Xss':
                        stack = flag[6:]
                    elif flag[2:4] == '-D':
                        prop = flag[2:]
                elif flag[:2] == '--':
                    if flag[2:6] == 'boot':
                        boot = True
                    elif flag[2:5] == 'jdb':
                        jdb = True
                else:
                    if jythonArgs:
                        jythonArgs += ' '
                        jythonArgs += flag
                    else:
                        jythonArgs = flag
                    jythonArgs = jythonArgs.replace('%%', '%') # workaround two .bat files
                args.append(flag)
        process = StarterProcess()
        out = process.run(args, javaHome, jythonHome, jythonOpts)
        self.assertNotEquals('', out)
        homeIdx = out.find('-Dpython.home=')
        java = 'java'
        if javaHome:
            java = self.quote(self.unquote(javaHome) + '\\bin\\java')
        elif jdb:
            java = 'jdb'
        if not memory:
            memory = '512m'
        if not stack:
            stack = '1152k'
        beginning = java + ' '
        if prop:
            beginning += ' ' + prop
        beginning += ' -Xmx' + memory + ' -Xss' + stack + ' '
        self.assertEquals(beginning, out[:homeIdx])
        executableIdx = out.find('-Dpython.executable=')
        homeDir = self.getHomeDir()
        if jythonHome:
            homeDir = self.unquote(jythonHome)
        home = '-Dpython.home=' + self.quote(homeDir) + ' '
        self.assertEquals(home, out[homeIdx:executableIdx])
        if boot:
            classpathFlag = '-Xbootclasspath/a:'
        else:
            classpathFlag = '-classpath'
        classpathIdx = out.find(classpathFlag)
        executable = '-Dpython.executable=' + self.quote(sys.executable) + ' '
        if not boot:
            executable += ' '
        self.assertEquals(executable, out[executableIdx:classpathIdx])
        # ignore full contents of classpath at the moment
        classIdx = out.find('org.python.util.jython')
        self.assertTrue(classIdx > classpathIdx)
        restIdx = classIdx + len('org.python.util.jython')
        rest = out[restIdx:].strip()
        if jythonOpts:
            self.assertEquals(self.quote(jythonOpts), rest)
        else:
            if jythonArgs:
                self.assertEquals(jythonArgs, rest)
            else:
                self.assertEquals('', rest)

class VanillaTest(BaseTest):
    def test_plain(self):
        self.assertOutput()

class JavaHomeTest(BaseTest):
    def test_unquoted(self):
        # for the build bot, try to specify a real java home
        javaHome = System.getProperty('java.home', 'C:\\Program Files\\Java\\someJava')
        self.assertOutput(javaHome=javaHome)

    def test_quoted(self):
        self.assertOutput(javaHome=self.quote('C:\\Program Files\\Java\\someJava'))

    # this currently fails, meaning we accept only quoted (x86) homes ...
    def __test_x86_unquoted(self):
        self.assertOutput(javaHome='C:\\Program Files (x86)\\Java\\someJava')

    def test_x86_quoted(self):
        self.assertOutput(javaHome=self.quote('C:\\Program Files (x86)\\Java\\someJava'))
        
class JythonHomeTest(BaseTest):
    def createJythonJar(self, parentDir):
        jar = File(parentDir, 'jython.jar')
        if not jar.exists():
            self.assertTrue(jar.createNewFile())
        return jar

    def cleanup(self, tmpdir, jar=None):
        if jar and jar.exists():
            self.assertTrue(jar.delete())
        os.rmdir(tmpdir)

    def test_unquoted(self):
        jythonHomeDir = tempfile.mkdtemp()
        jar = self.createJythonJar(jythonHomeDir)
        self.assertOutput(jythonHome=jythonHomeDir)
        self.cleanup(jythonHomeDir, jar)

    def test_quoted(self):
        jythonHomeDir = tempfile.mkdtemp()
        jar = self.createJythonJar(jythonHomeDir)
        self.assertOutput(jythonHome=self.quote(jythonHomeDir))
        self.cleanup(jythonHomeDir, jar)

class JythonOptsTest(BaseTest):
    def test_single(self):
        self.assertOutput(jythonOpts='myOpt')
        
    def test_multiple(self):
        self.assertOutput(jythonOpts='some arbitrary options')
     
class JavaOptsTest(BaseTest):
    def test_memory(self):
        self.assertOutput(['-J-Xmx321m'])

    def test_stack(self):
        self.assertOutput(['-J-Xss321k'])

    def test_property(self):
        self.assertOutput(['-J-DmyProperty=myValue'])

    def test_property_singlequote(self):
        self.assertOutput(["-J-DmyProperty='myValue'"]) 

    # a space inside value does not work in jython.bat
    def __test_property_singlequote_space(self):
        self.assertOutput(["-J-DmyProperty='my Value'"])

    def test_property_doublequote(self):
        self.assertOutput(['-J-DmyProperty="myValue"']) 

    # a space inside value does not work in jython.bat
    def __test_property_doublequote_space(self):
        self.assertOutput(['-J-DmyProperty="my Value"'])

    def test_property_underscore(self):
        self.assertOutput(['-J-Dmy_Property=my_Value'])

class ArgsTest(BaseTest):
    def test_file(self):
        self.assertOutput(['test.py'])
    
    def test_dash(self):
        self.assertOutput(['-i'])

    def test_combined(self):
        self.assertOutput(['-W', 'action', 'line'])

    def test_singlequoted(self):
        self.assertOutput(['-c', "'import sys;'"])

    def test_doublequoted(self):
        self.assertOutput(['-c', '"print \'something\'"'])

    def test_nestedquotes(self):
        self.assertOutput(['-c', '"print \'something \"really\" cool\'"'])

    def test_nestedquotes2(self):
        self.assertOutput(['-c', "'print \"something \'really\' cool\"'"])

    def test_underscored(self):
        self.assertOutput(['-jar', 'my_stuff.jar'])
    
    def test_property(self):
        self.assertOutput(['-DmyProperty=myValue'])

    def test_property_underscored(self):
        self.assertOutput(['-DmyProperty=my_Value'])

    def test_property_singlequoted(self):
        self.assertOutput(["-DmyProperty='my_Value'"])

    def test_property_doublequoted(self):
        self.assertOutput(['-DmyProperty="my_Value"'])

class DoubleDashTest(BaseTest):
    def test_boot(self):
        self.assertOutput(['--boot'])

    def test_jdb(self):
        self.assertOutput(['--jdb'])

class GlobPatternTest(BaseTest):
    def test_star_nonexisting(self):
        self.assertOutput(['-c', 'import sys; print sys.argv[1:]', '*.nonexisting', '*.nonexisting'])

    def test_star_nonexisting_doublequoted(self):
        self.assertOutput(['-c', 'import sys; print sys.argv[1:]', '"*.nonexisting"', '"*.nonexisting"'])

    def test_star_nonexistingfile_singlequoted(self):
        self.assertOutput(['-c', 'import sys; print sys.argv[1:]', "'*.nonexisting'", "'*.nonexisting'"])

    def test_star_existing(self):
        self.assertOutput(['-c', 'import sys; print sys.argv[1:]', '*.bat', '*.bat'])

    def test_star_existing_doublequoted(self):
        self.assertOutput(['-c', 'import sys; print sys.argv[1:]', '"*.bat"', '"*.bat"'])

    def test_star_existing_singlequoted(self):
        self.assertOutput(['-c', 'import sys; print sys.argv[1:]', "'*.bat'", "'*.bat'"])

class ArgsSpacesTest(BaseTest):
    def test_doublequoted(self):
        self.assertOutput(['-c', 'import sys; print sys.argv[1:]', '"part1 part2"', '2nd'])

    def test_singlequoted(self):
        self.assertOutput(['-c', 'import sys; print sys.argv[1:]', "'part1 part2'", '2nd'])

    # this test currently fails
    def __test_unbalanced_doublequote(self):
        self.assertOutput(['-c', 'import sys; print sys.argv[1:]', 'Scarlet O"Hara', '2nd'])

    def test_unbalanced_singlequote(self):
        self.assertOutput(['-c', 'import sys; print sys.argv[1:]', "Scarlet O'Hara", '2nd'])

class ArgsSpecialCharsTest(BaseTest):
    # exclamation marks are still very special ...
    def __test_exclamationmark(self):
        self.assertOutput(['-c', 'import sys; print sys.argv[1:]', 'foo!', 'ba!r', '!baz', '!'])

    # because we go through a starter.bat file, we have to simulate % with %%
    def test_percentsign(self):
        self.assertOutput(['-c', 'import sys; print sys.argv[1:]', 'foo%%1', '%%1bar', '%%1', '%%'])

    def test_colon(self):
        self.assertOutput(['-c', 'import sys; print sys.argv[1:]', 'foo:', ':bar'])

    # a semicolon at the beginning of an arg currently fails (e.g. ;bar)
    def test_semicolon(self):
        self.assertOutput(['-c', 'import sys; print sys.argv[1:]', 'foo;'])

class DummyTest(unittest.TestCase):
    def test_nothing(self):
        pass

def test_main():
    if os._name == 'nt':
        test_support.run_unittest(VanillaTest,
                                  JavaHomeTest,
                                  JythonHomeTest,
                                  JythonOptsTest,
                                  JavaOptsTest,
                                  ArgsTest,
                                  DoubleDashTest,
                                  GlobPatternTest,
                                  ArgsSpacesTest,
                                  ArgsSpecialCharsTest)
    else:
        # provide at least one test for the other platforms - happier build bots
        test_support.run_unittest(DummyTest)


if __name__ == '__main__':
    test_main()
        
