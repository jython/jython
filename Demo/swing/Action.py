# I don't really like the design of this one...
from pawt import swing

class Action(swing.AbstractAction):
	def __init__(self, name, action=None, icon=None, description=None, needEvent=0):
		if action is None:
			action = name
			name = action.__name__

		#swing.AbstractAction.__init__(self, name)
		self.name = name
		self.icon = icon
		if icon:
			self.setIcon(swing.Action.SMALL_ICON, icon)
		if description:
			self.setText(swing.Action.SHORT_DESCRIPTION, description)
			self.description = description
		else:
			self.description = name
		self.action = action

		self.enabled = 1
		self.needEvent = needEvent

	def actionPerformed(self, event):
		if self.needEvent:
			self.action(event)
		else:
			self.action()

	def createMenuItem(self):
		mi = swing.JMenuItem(self.name, actionListener=self, enabled=self.enabled)
		return mi

class TargetAction(Action):
	def actionPerformed(self, event):
		if self.needEvent:
			self.action(self.getTarget(), event)
		else:
			self.action(self.getTarget())