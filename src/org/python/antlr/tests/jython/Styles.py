from pawt.swing.text import StyleContext, StyleConstants, TabSet, TabStop
import string

class Styles:
	def __init__(self, context=None):
		if context is None:
			context = StyleContext()
		self.context = context
		self.default = self.context.getStyle(StyleContext.DEFAULT_STYLE)

	def add(self, name, parent=None, tabsize=None, **keywords):
		if parent is None:
			parent = self.default
		style = self.context.addStyle(name, parent)

		for key, value in keywords.items():
			key = string.upper(key[0])+key[1:]
			meth = getattr(StyleConstants, "set"+key)
			meth(style, value)

		if tabsize is not None:
			charWidth=StyleConstants.getFontSize(style)
			tabs = []
			for i in range(20):
				tabs.append(TabStop(i*tabsize*charWidth))
			StyleConstants.setTabSet(style, TabSet(tabs))
		return style

	def get(self, stylename):
		return self.context.getStyle(stylename)

	def __tojava__(self, c):
		if isinstance(self.context, c):
			return self.context
