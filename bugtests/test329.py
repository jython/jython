"""
[ #475445 ] incompatibility with python

Check comment handling when reading source from stdin.
"""

import support

support.runJava("org.python.util.jython -S < test329s1.py > test329.out")
support.runJava("org.python.util.jython -S < test329s2.py > test329.out")
support.runJava("org.python.util.jython -S < test329s3.py > test329.out")

