"""A translation of an example from the Java Tutorial
http://java.sun.com/docs/books/tutorial/

This example converts between metric and english units
"""

from java import awt
from java.applet import Applet
from java.awt.event import ActionListener, ItemListener, AdjustmentListener
from pawt import GridBag

basicUnits = [
	['Metric System', [('Centimeters', 0.01), ('Meters', 1.0),
		('Kilometers', 1000.0)]],
	['U.S. System', [('Inches', 0.0254), ('Feet', 0.305),
		('Yards', 0.914), ('Miles', 1613.0)]]]

class SimpleBorder:
	def paint(self, g):
		g.drawRect(0,0,self.size.width-1, self.size.height-1)

	def getInsets(self):
		return awt.Insets(5,5,5,5)

class Converter(Applet, SimpleBorder):
	def init(self, unitSets=basicUnits):
		self.setLayout(awt.GridLayout(2,0,5,5))
		self.panels = []
		for name, units in unitSets:
			panel = ConversionPanel(name, units, self)
			self.panels.append(panel)
			self.add(panel)

	def convert(self, master):
		value = master.getValue()
		multiplier = master.getMultiplier()

		for panel in self.panels:
			if panel is not master:
				panel.setValue(multiplier/panel.getMultiplier()*value)


class ConversionPanel(awt.Panel, SimpleBorder,
				ActionListener, AdjustmentListener, ItemListener):
	max, block = 10000, 100

	def __init__(self, title, units, controller):
		self.units = units
		self.controller = controller

		bag = GridBag(self, fill='HORIZONTAL')

		label = awt.Label(title, awt.Label.CENTER)
		bag.addRow(label)

		self.text = awt.TextField('0', 10, actionListener=self)
		bag.add(self.text, weightx=1.0)

		self.chooser = awt.Choice(itemListener=self)
		for name, multiplier in units:
			self.chooser.add(name)
		bag.addRow(self.chooser)

		self.slider = awt.Scrollbar(awt.Scrollbar.HORIZONTAL,
				maximum=self.max+10, blockIncrement=self.block,
				adjustmentListener=self)
		bag.add(self.slider)

	def getMultiplier(self):
		return self.units[self.chooser.selectedIndex][1]

	def getValue(self):
		try:
			return float(self.text.getText())
		except:
			return 0.0

	def actionPerformed(self, e):
		self.setSlider(self.getValue())
		self.controller.convert(self)

	def itemStateChanged(self, e):
		self.controller.convert(self)

	def adjustmentValueChanged(self, e):
		self.text.setText(str(e.getValue()))
		self.controller.convert(self)

	def setValue(self, v):
		self.text.setText(str(v))
		self.setSlider(v)

	def setSlider(self, f):
		if f > self.max: f = self.max
		if f < 0: f = 0
		self.slider.value = int(f)


if __name__ == '__main__':
	import pawt
	pawt.test(Converter())
