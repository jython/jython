"""
Check comparing a PyJavaClass with a Object.
"""

import support

from java import util, lang

class X(lang.Runnable):
    pass

v = util.Vector()
v.addElement(1)

v.indexOf(X())
   
