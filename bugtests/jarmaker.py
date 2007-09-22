import support
import sys
import os

from java.io import File

package = "javapackage"
clazz = "JavaClass"
jardir = "simplejar"
jarfn = "simple.jar"
clazzfile = File(jardir + '/'+ package, "%s.class" % clazz) # java.io.File

def mkjar():
    jarfile = File(jardir, jarfn)    
    # create a .jar file containing a .class file
    if not jarfile.exists():
        support.compileJava("%s/%s/%s.java" % (jardir, package, clazz))
        jarPacker = support.JarPacker(jarfile, bufsize=128)
        jarPacker.addFile(clazzfile, parentDirName=package)
        jarPacker.close()
    return jardir + '/' + jarfn, package, clazz

    

