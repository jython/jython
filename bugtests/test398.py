"""
test fix for bug #1642285

Try importing from a jar which contains a .class file which is completely empty (0bytes).
Make sure that the bad class file is skipped while good class file is processed.

Although this is an aberrant .class file, it has been seen in the wild (see bug report, found in a weblogic
jar).

"""

import support
import sys
import os

from java.io import File
from java.lang import String
from java.util import Properties
from org.python.core.packagecache import SysPackageManager

PACKAGE = "test398javapackage"
CACHEDIR = "test398cache"
BAD_CLAZZ = "test398j1"
GOOD_CLAZZ = "test398j2"
JARDIR = "test398jar"
JARFILE = "test398.jar"
GOOD_CLAZZ_FILE = File(PACKAGE, "%s.class" % GOOD_CLAZZ) # java.io.File
BAD_CLAZZ_FILE = File(PACKAGE, "%s.class" % BAD_CLAZZ) # java.io.File

def mkdir(dir):
  if not os.path.exists(dir):
    os.mkdir(dir)
  
def mkjavaclass():
  mkdir(PACKAGE)
  f = open("%s/%s.java" % (PACKAGE, GOOD_CLAZZ), "w")
  f.write("""
package %s;
public class %s {
}
""" % (PACKAGE, GOOD_CLAZZ))
  f.close()
  support.compileJava("%s/%s.java" % (PACKAGE, GOOD_CLAZZ))

def mkbadclass():
  mkdir(PACKAGE)
  f = open("%s/%s.class" % (PACKAGE, BAD_CLAZZ), "w")
  f.close()

def mkjar():
  mkdir(JARDIR)
  jarFile = File(JARDIR, JARFILE)
  jarPacker = support.JarPacker(jarFile, bufsize=128)
  jarPacker.addFile(GOOD_CLAZZ_FILE, parentDirName=PACKAGE)
  jarPacker.addFile(BAD_CLAZZ_FILE, parentDirName=PACKAGE)
  jarPacker.close()
  return jarFile

def mkprops():
    props = Properties()
    props.setProperty("java.ext.dirs", String(JARDIR));
    props.setProperty("python.security.respectJavaAccessibility", String("true"));
    return props
    
# create a .jar file containing a .class file
mkjavaclass()
mkbadclass()
jarFile = mkjar()
props = mkprops()
man = SysPackageManager(File(CACHEDIR, "packages"), props)
assert os.path.exists(os.path.join(CACHEDIR, "packages", "test398.pkc"))
