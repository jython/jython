"""A simple Python object browser.
This will create a tree that can be used to browse the objects in a given
Python namespace.  Like Console.py, this is a demo only, and needs a lot
of work before it would be a truly valuable tool.
"""

from pawt import swing
from types import *
import java

leaves = None, TypeType, IntType, StringType, FloatType, NoneType, BuiltinFunctionType, BuiltinMethodType


class PyEnumeration(java.util.Enumeration):
	def __init__(self, seq):
		self.seq = seq
		self.index = 0
		
	def hasMoreElements(self):
		return self.index < len(self.seq)
		
	def nextElement(self):
		self.index = self.index+1
		return self.seq[self.index-1]

def classattrs(c, attrs):
	for base in c.__bases__:
		classattrs(base, attrs)
	for name in c.__dict__.keys():
		attrs[name] = 1

def mydir(obj):
	attrs = {}
	if hasattr(obj, '__class__'):
		classattrs(obj.__class__, attrs)
	if hasattr(obj, '__dict__'):
		for name in obj.__dict__.keys():
			attrs[name] = 1
	ret = attrs.keys()
	ret.sort()
	return ret
	
def shortrepr(obj):
	r = repr(obj)
	if len(r) > 80:
		r = r[:77]+"..."
	return r

class ObjectNode(swing.tree.TreeNode):
	def __init__(self, parent, name, object):
		self.myparent = parent
		self.name = name
		self.object = object
		
	def getChildren(self):
		if hasattr(self, 'mychildren'): return self.mychildren
		
		if self.isLeaf():
			self.mychildren = None
			return None

		children = []
		for name in mydir(self.object):
			if name[:2] == '__': continue
			try:
				children.append(ObjectNode(self, name, getattr(self.object, name)))
			except TypeError:
				print 'type error on', name, self.object
		self.mychildren = children
		return children

	def children(self):
		return PyEnumeration(self.getChildren())
		
	def getAllowsChildren(self):
		return not self.isLeaf()
		
	def isLeaf(self):
		if hasattr(self.object, '__class__'):
			myclass = self.object.__class__
		else:
			myclass = None
			
		return myclass in leaves
		
	def getChildAt(self, i):
		return self.getChildren()[i]
		
	def getChildCount(self):
		return len(self.getChildren())
		
	def getIndex(self, node):
		index = 0
		for child in self.getChildren():
			if child == node: return index
			index = index+1
		return -1
		
	def getParent(self):
		return self.myparent

	def toString(self):
		return self.name+' = '+shortrepr(self.object)

if __name__ == '__main__':
	class foo:
		bar=99
		eggs='hello'
		class baz:
			x,y,z=1,2,3
		func = range
		
	import __main__

	f = foo()
	f.pyfunc = mydir

	root = ObjectNode(None, 'foo', __main__)
	tree = swing.JTree(root)
	swing.test(swing.JScrollPane(tree))
	
