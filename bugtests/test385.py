"""
Try importing from a jar after sys.path.append(jar)

This nails down a bug reported here:
    http://sourceforge.net/mailarchive/message.php?msg_id=14088259
which only occurred on systems where java.io.File.separatorChar is not a forward slash ('/')
   
since - at the moment - jython modules hide java packages with the same name from import,
use a unique java package name for the sake of this test 
"""

import support
import java
import sys
import jarray

from java.io import File
from java.io import FileInputStream
from java.io import FileOutputStream
from java.util.jar import JarEntry
from java.util.jar import JarOutputStream

PACKAGE = "test385javapackage"
CLAZZ = "test385j"
JARDIR = "test385jar"
JARFILE = "test385.jar"
CLAZZ_FILE = File(PACKAGE, "%s.class" % CLAZZ) # java.io.File
        
def mkdir(dir):
    file = File(dir)
    if not file.exists():
        file.mkdir()
    else:
        if not file.isDirectory():
            file.mkdir()

def mkjavaclass():
    mkdir(PACKAGE)
    f = open("%s/%s.java" % (PACKAGE, CLAZZ), "w")
    f.write("""
package %s;
public class %s {
}
""" % (PACKAGE, CLAZZ))
    f.close()
    support.compileJava("%s/%s.java" % (PACKAGE, CLAZZ))
    
def mkjar():
    mkdir(JARDIR)
    jarFile = File(JARDIR, JARFILE)
    jarOutputStream = JarOutputStream(FileOutputStream(jarFile))
    buffer = jarray.zeros(128, 'b')
    inputStream = FileInputStream(CLAZZ_FILE)
    jarEntryName = PACKAGE + "/" + CLAZZ_FILE.getName()
    jarOutputStream.putNextEntry(JarEntry(jarEntryName));
    read = inputStream.read(buffer)
    while read <> -1:
        jarOutputStream.write(buffer, 0, read);
        read = inputStream.read(buffer)
    jarOutputStream.closeEntry();
    inputStream.close()
    jarOutputStream.close()
    return jarFile

    
# create a .jar file containing a .class file
mkjavaclass()
jarFile = mkjar()

# important: delete the class file from the file system (otherwise it can be imported)
CLAZZ_FILE.delete()
if CLAZZ_FILE.exists():
    raise AssertionError, "%s is still on the file system" % CLAZZ_FILE

# append this jar file to sys.path
sys.path.append(jarFile.getAbsolutePath())

# try to import the class
importStmt = "from %s import %s" % (PACKAGE, CLAZZ)
exec(importStmt)