"""A translation of an example from the Java Tutorial
http://java.sun.com/docs/books/tutorial/

This example shows how to use Checkboxes
"""

from java import awt, applet

class CheckboxDemo(applet.Applet):
	def init(self):
		cb1 = awt.Checkbox('Checkbox 1')
		cb2 = awt.Checkbox('Checkbox 2')
		cb3 = awt.Checkbox('Checkbox 3', state=1)
		
		p1 = awt.Panel(layout=awt.FlowLayout())

		p1.add(cb1)
		p1.add(cb2)
		p1.add(cb3)

		cbg = awt.CheckboxGroup()
		cb4 = awt.Checkbox('Checkbox 4', cbg, 0)
		cb5 = awt.Checkbox('Checkbox 5', cbg, 0)
		cb6 = awt.Checkbox('Checkbox 6', cbg, 0)
		
		p2 = awt.Panel(layout=awt.FlowLayout())
		p2.add(cb4)
		p2.add(cb5)
		p2.add(cb6)
				
		self.setLayout(awt.GridLayout(0, 2))
		self.add(p1)
		self.add(p2)
		
		self.validate()

if __name__ == '__main__':	
	import pawt
	pawt.test(CheckboxDemo())

