"""
Check reload a java package (not a java class) (properly shouldn't work).
"""

import support

j1file = """
package test092m;
public class test092j1 {
    final static String j1_Version = "j1 Version %s";
    public String getJ1Version() {
	return j1_Version;
    }
}
"""

j2file = """
package test092m;
public class test092j2 extends test092j1 {
    final static String j2_Version = "j2 Version %s";
    public String getJ2Version() {
	return j2_Version;
    }
}
"""

def mkj1(v):
    f = open("classes/test092m/test092j1.java", "w")
    f.write(j1file % v)
    f.close()
    support.compileJava("classes/test092m/test092j1.java")

def mkj2(v):
    f = open("classes/test092m/test092j2.java", "w")
    f.write(j2file % v);
    f.close();
    support.compileJava("classes/test092m/test092j2.java")

import sys

mkj1("1")
mkj2("2")

import test092m

foo = test092m.test092j2()

support.compare(foo.j1Version, "j1 Version 1")
support.compare(foo.j2Version, "j2 Version 2")

mkj1("3")
mkj2("4")

#
# Removed. Reloading java packages is not supposed to work
#
#reload(test092m)
#
#foo = test092m.test092j2()
#support.compare(foo.j1Version, "j1 Version 3")
#support.compare(foo.j2Version, "j2 Version 4")



