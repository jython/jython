"""
Test sys.path import when a security profile prevents
creating classloaders.
"""

import support, java

support.compileJava("classes/test292j.java")

home = java.lang.System.getProperty("python.home")
cmd = """\
-Djava.security.manager -Djava.security.policy=test292.policy \
-Dpython.home=%s test292j""" % home

support.runJava(cmd)

