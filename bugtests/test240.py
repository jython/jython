
import support

support.compileJava("test240p/test240j2.java", classpath=".")

from test240p import test240j1
from test240p import test240j2
config = test240j1()
test = test240j2(config)

if test240j1.getClassLoader() != test240j2.getClassLoader():
   raise support.TestError, "Loaded classes are not inter-operable"
