"""
Test the re.M flag. Fails when using re. Success with sre.
"""

import support

import re

var ="T\nO"

m = re.search("^O",var,re.M)
if m == None:
   raise support.TestError("Should match")


m = re.search("^O",var)
if m != None:
   raise support.TestError("Should not match")

