"""
Try importing from a jar after sys.path.append(jar)

This nails down a bug reported here:
    http://sourceforge.net/mailarchive/message.php?msg_id=14088259
which only occurred on systems where java.io.File.separatorChar is not a forward slash ('/')
   
since - at the moment - jython modules hide java packages with the same name from import,
use a unique java package name for the sake of this test 
"""

import jarmaker
import support
import sys

jarfn, package, clazz = jarmaker.mkjar()
# append this jar file to sys.path
sys.path.append(jarfn)

# try to import the class
importStmt = "from %s import %s" % (package, clazz)
try:
    exec(importStmt)
finally:
    sys.path.remove(jarfn)
