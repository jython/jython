"""

"""

import support

support.compileJava("classes/test204j2.java")
support.compileJava("classes/test204j3.java")


import test204j1
support.compare(test204j1.classID, "2041")

import test204j2
support.compare(test204j2.classID, "2042")

import test204j3
support.compare(test204j3.classID, "2043")

support.compare(test204j1.classID, "2041")

