"""
Check recursive assignment to list slices.
"""

import support
x = [1,2,3,4,5]
x[1:] = x

if x != [1, 1, 2, 3, 4, 5]:
    raise support.TestError("Recursive assignmemt to list slices failed: " + `x`)
