"""
[ #449956 ] jythonc 2.1a3 --package problem

"""

import support

support.compileJPythonc("test339c.py", package="test339p", output="test339.err")

#raise support.TestError("" + `x`)
