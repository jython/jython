"""
Access to java names which are al reserved words
"""

import support

import java

s = java.lang.System.in
support.compare(`s`, "java.io.BufferedInputStream")

e = java.lang.Runtime.getRuntime().exec
support.compare(`e`, "method .*exec")

