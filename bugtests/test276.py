"""

"""

import support

support.compileJava("test276j.java")

from java.util import *
import test276j

h = Hashtable()

h.put("uno", "one")
h.put("zwei", "two")

tmm = test276j()

tmm.TellMeMoreO(h)
support.compare(tmm.getClassName(), "java.util.Hashtable")

tmm.TellMeMoreS(h)
support.compare(tmm.getClassName(), "java.util.Hashtable")

tmm.TellMeMoreS("abc")
support.compare(tmm.getClassName(), "java.lang.String")

tmm.TellMeMoreS(1)
support.compare(tmm.getClassName(), "java.lang.Integer")

tmm.TellMeMoreS(1.2)
support.compare(tmm.getClassName(), "java.lang.Double")

tmm.TellMeMoreS(Hashtable)
support.compare(tmm.getClassName(), "java.lang.Class")
