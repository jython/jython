"""
[ #480017 ] Proxy supers are loaded from syspath
Running test338j will throw a ClassCastException if a proxy's superclass is loaded
by the syspath classloader.
"""

import support

support.compileJava("test338cl.java", classpath=".")
support.compileJava("test338j1.java", classpath=".")
support.compileJava("test338j.java", classpath=".")

support.runJava("test338j", classpath=".")

