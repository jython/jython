"""
Check auto creation of super java class.
"""

import support

from java import awt

class R(awt.Rectangle):
    def __init__(self):
	#awt.Rectangle.__init__(self)
	self.size = awt.Dimension(6,7)
	
r = R()
support.compare(r.toString(), "width=6,height=7")
