"""
Test the standalone starting (java -jar jython.jar some.py)
"""

import support
import sys
import os

import support_config as cfg

from java.io import File

TESTDIR = "test386jar"
JYTHON_JAR = "jython.jar"
TEST_PY_NAME = TESTDIR +"/test386called.py"

def checkTestDir():
  if not os.path.exists(TESTDIR):
    raise AssertionError, TESTDIR + " does not exist"
  if not os.path.exists(TEST_PY_NAME):
    raise AssertionError, TEST_PY_NAME + " does not exist"
    
# create a jython standalone jar file:
# add the contents of jython.jar and /Lib files to an new jython.jar
def mkjar():
  jarFile = File(TESTDIR, JYTHON_JAR)
  jarPacker = support.JarPacker(jarFile)
  jarPacker.addJarFile(File(cfg.jython_home + "/%s" % JYTHON_JAR))
  jarPacker.addDirectory(File(cfg.jython_home + "/Lib"))
  jarPacker.close()
  return jarFile


checkTestDir()
mkjar()
jarFileName = "%s/%s" % (TESTDIR, JYTHON_JAR)
support.runJavaJar(jarFileName, TEST_PY_NAME)