"""
Should reloading a python module cause java classes to be reloaded?
"""

import support
src= """
package test122p;
public class test122j {
    public String version = "%s";
}
"""

def mk(v):
   f = open("test122p/test122j.java", "w")
   f.write(src % v)
   f.close()
   support.compileJava("test122p/test122j.java")

mk(1)

import test122p
bar1 = test122p.test122j()
if bar1.version != "1":
    raise support.TestError("Wrong version#1 %s" % bar1.version)

#
# Test removed. Reloading java packages are not supposed to work.
#
#mk(2)
#reload(test122p)
#bar2 = test122p.test122j()
#if bar2.version != "2":
#    raise support.TestError("Wrong version#2 %s" % bar2.version)
