"""

"""

import support

from javax.swing import *
class TestAction(AbstractAction):
   def __init__(self, verb):
      AbstractAction.__init__(self, verb)
   def actionPerformed(self, evt):
      print str(self.getValue(Action.NAME)) + " performed by " + str(self)

m = TestAction("Crash")

#raise support.TestError("" + `x`)
