"""This test validates the fix for 511493 (jreload truncates large class files).

We do this by loading a large class file from tools.jar.  If the load failes with ClassFormatError,
the bug is present, if the load succeeds, the bug is fixed, if something else occurs, we don't know.
"""

from test_support import *

print_test( 'jreload module (test_jreload.py)', 1)

from jreload import makeLoadSet
from java.lang import System, ClassFormatError

import os

# attempt to find tools.jar
toolsjar = System.getProperty( "toolsjar" )
if not toolsjar:
    extdirs = System.getProperty( "java.ext.dirs" ) #JDK_DIR/jre/lib/ext
    jdkdir = os.path.dirname( os.path.dirname( os.path.dirname( extdirs )))
    toolsjar = os.path.join( os.path.join( jdkdir, "lib" ), "tools.jar" )

if not os.path.exists( toolsjar ):
    print "Unable to find tools.jar, unable to perform test"
else:
    myls = makeLoadSet('myls', [toolsjar])

    try:
        from myls.com.sun.tools.corba.se.idl import Parser
    except ClassFormatError:
        print "Reload Error is present"
        raise
    print "Success"
