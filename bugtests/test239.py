import support

support.compileJava("test239j2.java", classpath=".")

import test239j1
import test239j2
config = test239j1()
test = test239j2(config)

if test239j1.getClassLoader() != test239j2.getClassLoader():
    raise support.TestError, "Loaded classes are not inter-operable"
