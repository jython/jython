"""
Test TQS
"""

import support

s1 = r""" 
    \""" 1.triple-quote
    \""" 2.triple-quote
"""

import string

if '\r' in s1:
    raise support.TestError, "TQS should not contain CR"