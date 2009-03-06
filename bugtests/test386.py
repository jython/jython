"""
Test the standalone starting (java -jar jython.jar some.py)
"""

import support
import sys
import os

import support_config as cfg

from java.io import File

TESTDIR = "test386jar"
JYTHON_DEV_JAR = "jython-dev.jar"
TEST_PY_NAME = TESTDIR +"/test386called.py"

def checkTestDir():
  if not os.path.exists(TESTDIR):
    raise AssertionError, TESTDIR + " does not exist"
  if not os.path.exists(TEST_PY_NAME):
    raise AssertionError, TEST_PY_NAME + " does not exist"
    
# create a jython standalone jar file:
# add the contents of jython-dev.jar and /Lib files to a new jython-dev.jar
def mkjar():
  jarFile = File(TESTDIR, JYTHON_DEV_JAR)
  jarPacker = support.JarPacker(jarFile)
  jarPacker.addJarFile(File(cfg.jython_home + "/%s" % JYTHON_DEV_JAR))
  jarPacker.addDirectory(File(cfg.jython_home + "/Lib"))
  jarPacker.close()
  return jarFile


checkTestDir()
mkjar()
jarFileName = "%s/%s" % (TESTDIR, JYTHON_DEV_JAR)
support.runJavaJar(jarFileName, TEST_PY_NAME)