"""
Test multilevel overriding of java methods.
"""

from java.util import Date 

class SubDate(Date): 
   def toString(self): 
       s = Date.toString(self)
       return 'SubDate -> Date'

class SubSubDate(SubDate): 
    def toString(self):
        return 'SubSubDate -> ' + SubDate.toString(self) 

assert SubDate().toString() == 'SubDate -> Date'
assert SubSubDate().toString() == 'SubSubDate -> SubDate -> Date'
