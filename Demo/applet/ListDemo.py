"""A rough translation of an example from the Java Tutorial
http://java.sun.com/docs/books/tutorial/

This example shows how to use List
"""

from java import applet, awt
from java.awt.event import ItemEvent
from pawt import GridBag

class ListDemo(applet.Applet):
    def fillList(self, list, names):
	list.actionPerformed=self.action
	list.itemStateChanged=self.change

	for name in names:
	    list.add(name)

    def init(self):
	self.spanish = awt.List(4, 1)
	self.fillList(self.spanish, ['uno', 'dos', 'tres', 'cuatro', 
				     'cinco', 'seis', 'siete'])
	self.italian = awt.List()
	self.fillList(self.italian, ['uno', 'due', 'tre', 'quattro',
				     'cinque', 'sei', 'sette'])

	self.output = awt.TextArea(10, 40, editable=0)

	bag = GridBag(self)
	bag.add(self.output,
		fill='BOTH', weightx=1.0, weighty=1.0,
		gridheight=2)

	bag.addRow(self.spanish, fill='VERTICAL')
	bag.addRow(self.italian, fill='VERTICAL')

	self.language = {self.spanish:'Spanish', self.italian:'Italian'}

    def action(self, e):
	list = e.source
	text = 'Action event occurred on "%s" in %s.\n'
	self.output.append(text % (list.selectedItem, self.language[list]))

    def change(self, e):
	list = e.source
	if e.stateChange == ItemEvent.SELECTED:
	    select = 'Select'
	else: 
	    select = 'Deselect'

	text = '%s event occurred on item #%d (%s) in %s.\n'
	params = (select, e.item, list.getItem(e.item), self.language[list])
	self.output.append(text % params)


if __name__ == '__main__':
    import pawt
    pawt.test(ListDemo())
