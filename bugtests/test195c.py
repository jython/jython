
try: 
   from java.swing import *
except ImportError:
   pass
else:
   print "Excepted an AttributeError"
