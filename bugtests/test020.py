"""
Check strange indexes/slices.
"""

import support

bits = ['1','1','0','0']
bits[2:1] = ['2']

if bits != ['1', '1', '2', '0', '0']:
   raise support.TestError("Assignment to slice gave wrong result: " + `bits`)


