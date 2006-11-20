"""
Test standalone starting,
where the manifest of a .jar refers to jython.jar


This used to give an error importing site, as follows:

error importing site
Traceback (innermost last):
  File "C:\workspace\jython\bugtests\test394jar\jython.jar\Lib/site.py", line 210, in ?
TypeError: unsupported operand type(s) for +: 'NoneType' and 'str'
Traceback (innermost last):
  File "C:/workspace/jython/bugtests/test394.py", line 71, in ?
  File "C:\workspace\jython\bugtests\support.py", line 100, in runJavaJar
  File "C:\workspace\jython\bugtests\support.py", line 65, in execCmd
TestError: cmd /C "C:/Programme/Java/jdk1.5.0_09/bin/java.exe -jar test394jar/run.jar " failed with -1

"""

import support
import sys
import os

import support_config as cfg

from java.io import File

TESTDIR = "test394jar"
JYTHON_JAR = "jython.jar"
RUN_JAR = "run.jar"
TEST_PY_NAME = TESTDIR +"/test394called.py"
CLAZZ = "Runner"
MANIFEST = "MANIFEST.MF"

def checkTestDir():
  if not os.path.exists(TESTDIR):
    raise AssertionError, TESTDIR + " does not exist"
  if not os.path.exists(TEST_PY_NAME):
    raise AssertionError, TEST_PY_NAME + " does not exist"
  javaFileName = TESTDIR + "/" + CLAZZ + ".java"
  if not os.path.exists(javaFileName):
    raise AssertionError, javaFileName + " does not exist"
  manifestFileName = TESTDIR + "/" + MANIFEST
  if not os.path.exists(manifestFileName):
    raise AssertionError, manifestFileName + " does not exist"
                        
  
# create a jython standalone jar file:
# add the contents of jython.jar and /Lib files to an new jython.jar
def mkJythonJar():
  jarFile = File(TESTDIR, JYTHON_JAR)
  jarPacker = support.JarPacker(jarFile)
  jarPacker.addJarFile(File(cfg.jython_home + "/%s" % JYTHON_JAR))
  jarPacker.addDirectory(File(cfg.jython_home + "/Lib"))
  jarPacker.close()
  return jarFile
  
# make a java class calling jython main
def mkJavaClass():
  support.compileJava("%s/%s.java" % (TESTDIR, CLAZZ))

# create a runnable jar file with a manifest referring to jython.jar
def mkRunJar():
  jarFile = File(TESTDIR, RUN_JAR)
  manifestFile = File(TESTDIR, MANIFEST)
  jarPacker = support.JarPacker(jarFile)
  jarPacker.addManifestFile(manifestFile)
  jarPacker.addFile(File(TESTDIR, CLAZZ+".class"), TESTDIR)
  jarPacker.close()
                       
                    

checkTestDir()
mkJythonJar()
mkJavaClass()
mkRunJar()
jarFileName = "%s/%s" % (TESTDIR, RUN_JAR)
support.runJavaJar(jarFileName)