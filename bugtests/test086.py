"""
Check autocreate of java superclass.
"""

import support

import java

class mythread(java.lang.Thread):
    def __init__( self, name="bla" ):
        self.name = name
    def run( self ):
        print self.name
        pass

mythread().start()
mythread("pipo 1").start()
mythread("pipo 2").start()

