
import support 

support.compileJava("classes/test231j2.java")

import test231j, test231j2

class MyHandler(test231j):
	def handleRemove(self, start):
		if start  != 4294967295L:
			raise support.TestError("long not passed correcttly")
    
m = MyHandler()

test231j2.callback(m);
