import support

support.compileJava("test268j2.java", classpath=".")


from java import net, lang
clu=net.URL(r'file:%s/' % lang.System.getProperty("user.dir"))
ld1=net.URLClassLoader([clu])
X=ld1.loadClass("test268j1")
Y=ld1.loadClass("test268j2")
Y.printX(X())

import test268j1
import test268j2

#import org
#print "First dump"
#org.python.core.PyJavaClass.dump()
