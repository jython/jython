"""
Test deeply nested classes
[ #440660 ] using nested java cls @ level >2 fails
"""

import support
support.compileJava("test303j.java")

import test303j.A
try:
    import test303j.A.B
except ImportError:
    raise support.TestWarning('It should be possible to import test303j.A.B')
