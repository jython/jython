
import exceptions

class MyException(exceptions.ValueError):
   pass

try:
   raise MyException, "exception value"
except Exception, e:
   pass

