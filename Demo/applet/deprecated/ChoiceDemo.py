"""A rough translation of an example from the Java Tutorial
http://java.sun.com/docs/books/tutorial/

This example shows how to use Choice
"""

from java import awt, applet

class ChoiceDemo(applet.Applet):		
    def init(self):
	self.choices = awt.Choice(itemStateChanged = self.change)
	for item in ['ichi', 'ni', 'san', 'yon']:
	    self.choices.addItem(item)

	self.label = awt.Label()
	self.change()

	self.add(self.choices)
	self.add(self.label)

    def change(self, event=None):
	selection = self.choices.selectedIndex, self.choices.selectedItem
	self.label.text = 'Item #%d selected. Text = "%s".' % selection


if __name__ == '__main__':
    import pawt
    pawt.test(ChoiceDemo())
