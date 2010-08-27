"""A rough translation of an example from the Java Tutorial
http://java.sun.com/docs/books/tutorial/

This example shows how to do very simple Canvas Drawing
"""

from java import applet, awt
from pawt import GridBag


class CoordinatesDemo(applet.Applet):
    def init(self):
	bag = GridBag(self)

	self.framedArea = FramedArea(self)
	bag.addRow(self.framedArea, weighty=1.0, fill='BOTH')

	self.label = awt.Label('Click within the framed area')
	bag.addRow(self.label, weightx=1.0, weighty=0.0, fill='HORIZONTAL')

    def updateLabel(self, point):
	text = 'Click occurred at coordinate (%d, %d).'
	self.label.text = text % (point.x, point.y)



class FramedArea(awt.Panel):
    def __init__(self, controller):
	self.background = awt.Color.lightGray
	self.setLayout(awt.GridLayout(1,0))

	self.add(CoordinateArea(controller))

    def getInsets(self):
	return awt.Insets(4,4,5,5)

    def paint(self, g):
	d = self.size

	g.color = self.background
	g.draw3DRect(0, 0, d.width-1, d.height-1, 1)
	g.draw3DRect(3, 3, d.width-7, d.height-7, 1)



class CoordinateArea(awt.Canvas):
    def __init__(self, controller):
	self.mousePressed = self.push
	self.controller = controller

    def push(self, e):
	try:
	    self.point.x = e.x
	    self.point.y = e.y
	except AttributeError:
	    self.point = awt.Point(e.x, e.y)

	self.repaint()

    def paint(self, g):
	if hasattr(self, 'point'):
	    self.controller.updateLabel(self.point)
	    g.fillRect(self.point.x-1, self.point.y-1, 2, 2)



if __name__ == '__main__':
    import pawt
    pawt.test(CoordinatesDemo(), size=(300, 200))
