"""

"""

import support

raise support.TestWarning("mixing base classes between Jython and Java is not supported")

import java.util 
import org.python.core 
class LX(org.python.core.PyList,java.util.List): 
   pass 

l = LX() 

try:
   l.add('x') 
except AttributeError:
   pass
else:
   raise support.TestError("expected an AttributeError")
 
