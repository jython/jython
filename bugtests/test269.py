
import support

support.compileJava("test269p/test269j2.java", classpath=".")

from java import net, lang
clu=net.URL(r'file:%s/' % lang.System.getProperty("user.dir"))
ld1=net.URLClassLoader([clu])
X=ld1.loadClass("test269p.test269j1")
Y=ld1.loadClass("test269p.test269j2")
Y.printX(X())

from test269p import *
test269j2.printX(test269j1())

#import org
#print "Second dump"
#org.python.core.PyJavaClass.dumpDebug()
