
import support

s1 = r""" 
    \""" 1.triple-quote
    \""" 2.triple-quote
"""

s2 = r''' 
    \""" 1.triple-quote
    \""" 2.triple-quote
'''


for i in range(len(s2)):
    if s1[i] != s2[i]: print "diff at", i


if s1 != s2:
    raise support.TestError, "TQS different"