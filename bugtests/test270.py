
import support

support.compileJava("test270p/test270j2.java", classpath=".")

from test270p import *
test270j2.printX(test270j1())


from java import net, lang
clu=net.URL(r'file:%s/' % lang.System.getProperty("user.dir"))
ld1=net.URLClassLoader([clu])
X=ld1.loadClass("test270p.test270j1")
Y=ld1.loadClass("test270p.test270j2")
Y.printX(X())

#import org
#org.python.core.PyJavaClass.dumpDebug()
