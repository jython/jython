"""
Passing an python implemented interface to a java method.
"""

import support

support.compileJava("classes/test082j1.java")
support.compileJava("classes/test082j2.java")

import test082j1, test082j2

class pydoit( test082j1 ):
        def doit( a, b): # too many arguments for interface
                pass

        def dosomethingelse( a ):
                pass


test082j2().doDoit( pydoit() )

