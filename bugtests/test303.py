"""
Test deeply nested classes
[ #440660 ] using nested java cls @ level >2 fails
"""

import support
support.compileJava("test303j.java")

import test303j.A
import test303j.A.B
