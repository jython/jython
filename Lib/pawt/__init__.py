import java
from java import awt

def test(panel, size=None):
	f = awt.Frame('AWT Tester', windowClosing=lambda event: java.lang.System.exit(0))
	if hasattr(panel, 'init'):
		panel.init()

	f.add('Center', panel)
	f.pack()
	if size is not None:
		f.size = size
	f.visible = 1

class GridBag:
	def __init__(self, frame, **defaults):
		self.frame = frame
		self.gridbag = awt.GridBagLayout()
		self.defaults = defaults
		frame.setLayout(self.gridbag)
		
	def addRow(self, widget, **kw):
		kw['gridwidth'] = 'REMAINDER'
		apply(self.add, (widget, ), kw)

	def add(self, widget, **kw):
		constraints = awt.GridBagConstraints()

		for key, value in self.defaults.items()+kw.items():
			if isinstance(value, type('')):
				value = getattr(awt.GridBagConstraints, value)
			setattr(constraints, key, value)
		self.gridbag.setConstraints(widget, constraints)
		self.frame.add(widget)
		
try:
	import java.awt.swing.Icon
	swing = java.awt.swing
except ImportError:
	try:
		import com.sun.java.awt.swing.Icon
		swing = com.sun.java.awt.swing
	except ImportError:
		pass
		