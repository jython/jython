"""
[ #480017 ] Proxy supers are loaded from syspath
"""

import support

support.compileJava("test338cl.java", classpath=".")
support.compileJava("test338j1.java", classpath=".")
support.compileJava("test338j.java", classpath=".")

#support.runJava("test338j", classpath=".")
raise support.TestWarning("Should (maybe) not fail.")

