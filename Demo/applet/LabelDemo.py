"""A rough translation of an example from the Java Tutorial
http://java.sun.com/docs/books/tutorial/

This example shows how to use Label
"""

from java import applet
from java.awt import Label, GridLayout

class LabelDemo(applet.Applet):		
	def init(self):
		self.setLayout(GridLayout(0,1))
		self.add(Label('Left'))
		self.add(Label('Center', Label.CENTER))
		self.add(Label('Right', Label.RIGHT))
		
if __name__ == '__main__':
	import pawt
	pawt.test(LabelDemo())