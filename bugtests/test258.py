
import support 

try:
   import test258m1 
except:
   pass

import sys

if not sys.modules.has_key('test258m1'):
   raise support.TestError, "Module should exists in sys.modules"


