"""
Test class identity for inner classes
[ #452947 ] Class of innerclass inst <> innerclas
"""

import support

support.compileJava('test319j.java')

import test319j

id1 = id(test319j.inner)
id2 = id(test319j.mkinner().__class__)

if id1 != id2:
    print "innerclass different", test319j.inner, test319j.mkinner().__class__
    raise support.TestWarning("innerclass different %s %s" % (
                test319j.inner, test319j.mkinner().__class__))
