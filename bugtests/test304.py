"""
Test that non-public classes gets loaded with dir() when respectJavaAcc is
false.
Feature request [ #428582 ] Access to non-public classes
"""

import support

support.runJava("org.python.util.jython " +
                "-Dpython.security.respectJavaAccessibility=false test304m.py", pass_jython_home=1)

