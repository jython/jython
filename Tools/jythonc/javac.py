# Copyright © Corporation for National Research Initiatives
import java, jarray, os, time, sys
from java.lang import System

runtime = java.lang.Runtime.getRuntime()

def dumpStream(stream):
    count = stream.available()
    array = jarray.zeros( count, 'b' )
    count = stream.read( array )
    return array.tostring()

def findDefaultJavac():
    jhome = System.getProperty("java.home")
    if jhome is None:
        return None
    root, dir = os.path.split(jhome)
    if dir == "jre":
        jhome = root
    javac = os.path.join(os.path.join(jhome, "bin"), "javac")
    return javac

def getClasspath():
    cpath = System.getProperty("java.class.path")
    return cpath

def compile(files, javac=None, cpathopt="-classpath", cpath=None, options=[]):
    if javac is None:
        javac = findDefaultJavac()
    if cpath is None:
        cpath = getClasspath()
    args = [javac, cpathopt, cpath]+options+files
    print args

    proc = runtime.exec(args)
    ev = None
    errs = ""
    while ev is None:
        try:
            newErrs = dumpStream(proc.errorStream)
            sys.stderr.write(newErrs)
            errs = errs+newErrs
            time.sleep(0.1)
            ev = proc.exitValue()
        except java.lang.IllegalThreadStateException:
            pass

    return proc.exitValue(), dumpStream(proc.inputStream), errs


if __name__ == '__main__':
    files = ["c:\\jpython\\tools\\jpythonc2\\test\\ButtonDemo.java",
            "c:\\jpython\\tools\\jpythonc2\\test\\pawt.java",]

    print compile(files)
    print compile(files, ["-foo", "bar"])
    print 'done'
