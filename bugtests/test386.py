"""
Test the stanalone starting (java -jar jython.jar some.py)
"""

import support
import sys
import os

import support_config as cfg

from java.io import File

TESTDIR = "test386jar"
JYTHON_JAR = "jython.jar"
TEST_PY = "test386.py"

def mkdir(dir):
  if not os.path.exists(dir):
    os.mkdir(dir)

def mkPy():
  f = open("%s/%s" % (TESTDIR, TEST_PY), "w")
  f.write("""
import getopt # import a non-builtin module which is not imported by default on startup
""")
  f.close()
    
# create a jython standalone jar file:
# add the contents of jython.jar and /Lib files to an new jython.jar
def mkjar():
    jarFile = File(TESTDIR, JYTHON_JAR)
    jarPacker = support.JarPacker(jarFile)
    jarPacker.addJarFile(File(cfg.jython_home + "/%s" % JYTHON_JAR))
    jarPacker.addDirectory(File(cfg.jython_home + "/Lib"))
    jarPacker.close()
    return jarFile


mkdir(TESTDIR)
mkPy()
mkjar()
jarFileName = "%s/%s" % (TESTDIR, JYTHON_JAR)
testFileName = "%s/%s" % (TESTDIR, TEST_PY)
support.runJavaJar(jarFileName, testFileName)