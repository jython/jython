"""\
This is a very simple example of using Java's AWT from JPython.
I expect that there will be a much higher level of accessing the
AWT in future releases of the system.

This example 
"""

import java
from java import awt

class action(awt.event.ActionListener):
	def __init__(self, frame):
		self.frame = frame
		
	def actionPerformed(self,event):
		if event.getActionCommand() == "Close Me!": 
			java.lang.System.exit(0)

frame = awt.Frame("A test")
button = awt.Button("Close Me!")
button.addActionListener(action(frame))
frame.add(button, "Center")
frame.pack()
frame.setVisible(1)
