"""
Test long indexing of sequences (1.6)
"""

import support


l = (0,1,2,3)

if l[2L:4L]  != (2, 3):
   raise support.TestError, "long slice error #1"

if "abcbdef"[2L:4L] != "cb":
   raise support.TestError, "long slice error #2"
