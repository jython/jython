"""
[ #451746 ] jythonc --deep jpy$packages problem

"""

import support

support.compileJPythonc("test341c1.py", deep=1 , output="test341.err")

#raise support.TestError("" + `x`)
