
#import exceptions

class MyException(ValueError):
   pass

try:
   raise MyException, "exception value"
except Exception, e:
   pass

