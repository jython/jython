from java.lang import Object, String
from java.io import Serializable
from test217p import test217i
import java

class test217c(Object, Serializable, test217i):
	def add(self, x, y):
		"@sig public java.lang.String add(int x, int y)"
		return "The sum of %d and %d is %d" % (x, y, x+y)

