"""
[ #485558 ] Synchronization bug in sys.initialize
"""

import support

support.compileJava("test343j.java")
support.runJava("test343j", classpath=".")

