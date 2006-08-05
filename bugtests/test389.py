'''
Test for Bug #1533624.  A call that crosses threads causes a null pointer
exception when building a traceback.  If it reappears instead of the
TypeError showing up a null pointer exception will be thrown.
'''

from java.awt import EventQueue
from java.lang import Runnable

class PyRunnable(Runnable):
    def run(self):
	raise TypeError, 'this is only a test'

def g():
    EventQueue.invokeAndWait(PyRunnable())

try:
   g()
except TypeError:
   pass
