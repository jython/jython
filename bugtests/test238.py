"""
Try reloading a javaclass located on sys.path.
"""

import support
import java
import sys

def mkspam(a):
    f = open("test238p/test238j.java", "w")
    f.write("""
package test238p;
public class test238j {
    public static String spam() { return "%s"; }
    public static void %s() {}
}
""" % (a, a))
    f.close()
    support.compileJava("test238p/test238j.java")


mkspam("foo")

import test238p

spam1 = test238p.test238j.spam()
support.compare(spam1, "foo")

mkspam("bar")

#
# Test removed. Reloading a java package is not supposed to work.
#
#reload(test238p)
#spam1 = test238p.test238j.spam()
#support.compare(spam1, "bar")
#
#mkspam("baz")
#
#reload(test238p)
#spam1 = test238p.test238j.spam()
#support.compare(spam1, "baz")

