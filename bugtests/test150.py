"""
setChanged was not found.
"""

import support

from java.util import Observable
class TestProperties( Observable ):
    def __init__( self ):
        self.props = {
            'logFileDir'        : "./",
            'logFileName'       : "testLog.txt",
            'maxFailures'       : 1,
            }

    def get( self, name ):
        return self.props[ name ]

    def set( self, name, value ):
        self.props[ name ] = value
        self.notifyObservers()
        self.setChanged()

t = TestProperties()
t.set("k", "v")

#raise support.TestError("" + `x`)
