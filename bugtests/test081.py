"""
Try reloading a javaclass located on sys.path.
"""

import support
import java
import sys

def mkspam(a):
    f = open("test081j.java", "w")
    f.write("""
public class test081j {
    public static String spam() { return "%s"; }
    public static void %s() {}
}
""" % (a, a))
    f.close()
    support.compileJava("test081j.java")


mkspam("foo")

import test081j
spam1 = test081j.spam()
support.compare(spam1, "foo")

#
# Test removed. Reloading java classes are not supposed to work.
#
#mkspam("bar")
#
#test081j = reload(test081j)
#spam2 = test081j.spam()
#support.compare(spam2, "bar")
#
#mkspam("baz")
#
#test081j = reload(test081j)
#spam2 = test081j.spam()
#support.compare(spam2, "baz")

