

"""

"""

import support

support.compileJava("test278p/bug.java", classpath=".")

from test278p import bug 
b=bug() 
assert b.getName() == "name"
assert b.name== "name"


#raise support.TestError("" + `x`)
