"""

"""

import support


support.compileJava("classes/test205j2.java")

import test205j1
support.compare(test205j1.classID, "2051")

import test205j2

support.compare(test205j2.classID, "2052")

support.compare(test205j1.classID, "2051")





support.compare(test205j1().classID, "20510")
support.compare(test205j2().classID, "20520")
support.compare(test205j1().classID, "20510")


