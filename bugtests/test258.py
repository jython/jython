
import support 

try:
   import test258m1 
except:
   pass

import sys

if sys.modules.has_key('test258m1'):
   raise support.TestError, "Module should not exist in sys.modules"


