"""This test validates the fix for 511493 (jreload truncates large class files).

We do this by loading a large class file from tools.jar.  If the load failes with ClassFormatError,
the bug is present, if the load succeeds, the bug is fixed, if something else occurs, we don't know.
"""

import unittest
import test_support

from jreload import makeLoadSet
from java.lang import System, ClassFormatError

import os

class JreloadTestCase(unittest.TestCase):
    # attempt to find tools.jar
    toolsjar = System.getProperty( "toolsjar" )
    if not toolsjar:
        extdirs = System.getProperty( "java.ext.dirs" ) #JDK_DIR/jre/lib/ext
        jdkdir = os.path.dirname( os.path.dirname( os.path.dirname( extdirs )))
        toolsjar = os.path.join( os.path.join( jdkdir, "lib" ), "tools.jar" )

    if not os.path.exists( toolsjar ):
        raise test_support.TestSkipped( "Unable to find tools.jar" )

    def test( self ):
        myls = makeLoadSet('myls', [self.toolsjar])

        try:
            from myls.com.sun.tools.corba.se.idl import Parser
        except ClassFormatError:
            print "Reload Error is present"
            raise
        
def test_main():
    test_support.run_unittest(JreloadTestCase)

if __name__ == "__main__":
    test_main()

