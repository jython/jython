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
    if dir.lower() == "jre":
        jhome = root
    javac = os.path.join(os.path.join(jhome, "bin"), "javac")
    return javac

def getClasspath():
    cpath = System.getProperty("java.class.path")
    return cpath

def compile(files, javac=None, cpathopt="-classpath",
            cpath=None, options=None):
    cmd = []
    # Search order for a Java compiler:
    #   1. -C/--compiler command line option
    #   2. python.jpythonc.compiler property (see registry)
    #   3. guess a path to javac
    if javac is None:
        javac = sys.registry.getProperty("python.jpythonc.compiler")
    if javac is None:
        javac = findDefaultJavac()
    cmd.append(javac)
    # Extra options
    #   1. -J/--compileropts command line option (passed in options)
    #   2. python.jpythonc.compileropts property
    if options is None:
        options = sys.registry.getProperty("python.jpythonc.compileropts")
        if options:
            options = options.split()
    if options is None:
        options = []
    cmd.extend(options)
    # Classpath:
    #   1. python.jpythonc.classpath property
    #   2. java.class.path property
    if cpath is None:
        cpath = sys.registry.getProperty("python.jpythonc.classpath")
    if cpath is None:
        cpath = getClasspath()
    cmd.extend([cpathopt, cpath])
    cmd.extend(files)
    print 'Compiling with args:', cmd

    try:
        proc = runtime.exec(cmd)
    except IOError, e:
        msg = '''%s

Consider using the -C/--compiler command line switch, or setting
the property python.jpythonc.compiler in the registry.''' % e
        return 1, '', msg
    done = None
    while not done:
        proc.waitFor()
	try:
	    proc.exitValue()
	    done = 1
	except java.lang.IllegalThreadStateException:
	    pass
    return (proc.exitValue(), dumpStream(proc.inputStream),
	    dumpStream(proc.errorStream))


if __name__ == '__main__':
    files = ["c:\\jpython\\tools\\jpythonc2\\test\\ButtonDemo.java",
            "c:\\jpython\\tools\\jpythonc2\\test\\pawt.java",]

    print compile(files)
    print compile(files, ["-foo", "bar"])
    print 'done'
