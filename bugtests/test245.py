

import support

support.compileJava("test245j.java")

#import test245j
#test245j.main([])

support.runJava("test245j", classpath=".")

