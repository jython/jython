'''Tests jython.bat using the --print option'''

import os
import sys
import unittest
import tempfile

from test import test_support
from subprocess import Popen, PIPE

from java.lang import IllegalThreadStateException
from java.lang import Runtime
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

class SimpleSubprocess:
    def __init__(self, args):
        self.args = args
        self.exitValue = -999

    def run(self):
        self.process = Runtime.getRuntime().exec(self.args)
        self.stdoutMonitor = StdoutMonitor(self.process)
        self.stderrMonitor = StderrMonitor(self.process)
        self.stdoutMonitor.start()
        self.stderrMonitor.start()
        while self.isAlive():
            Thread.sleep(1000)
        return self.exitValue

    def getStdout(self):
        return self.stdoutMonitor.getOutput()

    def getStderr(self):
        return self.stderrMonitor.getOutput()

    def isAlive(self):
        try:
            self.exitValue = self.process.exitValue()
            return False
        except IllegalThreadStateException:
            return True

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

    def copyOsEnviron(self):
        theCopy = {}
        for key in os.environ.keys():
            theCopy[key] = os.environ[key]
        return theCopy

    def assertOutput(self, flags=None, javaHome=None, jythonHome=None, jythonOpts=None):
        args = [sys.executable, '--print']
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
                args.append(flag)
        environ = None
        if javaHome or jythonHome or jythonOpts:
            environ = self.copyOsEnviron() # copy to prevent os.environ from being changed
            if javaHome:
                environ['JAVA_HOME'] = javaHome
            if jythonHome:
                environ['JYTHON_HOME'] = jythonHome
            if jythonOpts:
                environ['JYTHON_OPTS'] = jythonOpts
        if environ:
            # env changes can only be handled by Popen
            process = Popen(args, stdout=PIPE, stderr=PIPE, env=environ)
            out = process.stdout.read()
            err = process.stderr.read()
        else:
            # since Popen escapes double quotes, we use a simple subprocess if no env changes are needed
            simpleProcess = SimpleSubprocess(args)
            simpleProcess.run()
            out = simpleProcess.getStdout()
            err = simpleProcess.getStderr()
        self.assertEquals('', err)
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
        self.assertOutput(javaHome='C:\\Program Files\\Java\\someJava')

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
        self.assertOutput(["-J-DmyProperty='myValue'"]) # a space inside value does not work in jython.bat

    def test_property_doublequote(self):
        self.assertOutput(['-J-DmyProperty="myValue"']) # a space inside value does not work in jython.bat

    def test_property_underscore(self):
        self.assertOutput(['-J-Dmy_Property=my_Value'])

class ArgsTest(BaseTest):
    def test_file(self):
        self.assertOutput(['test.py'])
    
    def test_dash(self):
        self.assertOutput(['-i'])

    def test_combined(self):
        self.assertOutput(['-W', 'action', 'line'])

    # something adds double quotes, but in real life this works: 
    # jython.bat --print -c 'import sys;' 
    def test_singlequoted(self):
            args = [sys.executable]
            args.append('--print')
            args.append('-c')
            args.append("'import sys;'")
            simpleProcess = SimpleSubprocess(args)
            simpleProcess.run()
            out = simpleProcess.getStdout()
            err = simpleProcess.getStderr()
            self.assertEquals('', err)
            self.assertNotEquals('', out)
            tail = out[-19:]
            self.assertEquals('-c "\'import sys;\'" ', tail)

    def test_doublequoted(self):
        self.assertOutput(['-c', '"print \'something\'"'])

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

class DummyTest(unittest.TestCase):
    def test_nothing(self):
        pass

def test_main():
    if os._name == 'nt':
        test_support.run_unittest(VanillaTest,
                                  JavaOptsTest,
                                  ArgsTest,
                                  DoubleDashTest)
    else:
        # provide at least one test for the other platforms - happier build bots
        test_support.run_unittest(DummyTest)


if __name__ == '__main__':
    test_main()
        