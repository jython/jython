
from java.util import Date

class SubDate(Date):
   def __init__(self, time, flag=0):
      "@sig public SubDate(java.lang.String time, boolean flag)"
      Date.__init__(self, 0)

   def after(self, when, flag=0):
      "@sig public boolean before(java.util.Date when2, boolean flag)"
      return Date.after(self, when)


sd = SubDate("", 1)
print sd
print sd.after(SubDate("", 0))
