"""
Coercion of Integer and Number.
"""

import support

support.compileJava("test091j.java")

import test091j 

r = test091j.takeInt(12) 
support.compare(r, "takeInt")

r = test091j.takeInteger(12) 
support.compare(r, "takeInteger")

r = test091j.takeNumber(12)
support.compare(r, "takeNumber")
