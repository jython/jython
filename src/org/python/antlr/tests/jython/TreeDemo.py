"""\
A simple demonstration of creating a swing tree widget from a
Python dictionary.
"""

data = {
	'PyObject': {
        'PyInteger':None,
        'PyFloat':None,
        'PyComplex':None,
        'PySequence': {
        	'PyArray':None,
            'PyList':None,
            'PyTuple':None,
            'PyString':None,
        },
        'PyClass': {
            'PyJavaClass':None,
        },
	},
    'sys':None,
    'Py':None,
    'PyException':None,
    '__builtin__':None,
    'ThreadState':None,
}


from pawt import swing
Node = swing.tree.DefaultMutableTreeNode

def addNode(tree, key, value):
	node = Node(key)
	tree.add(node)
	if value is not None:
		addLeaves(node, value.items())

def addLeaves(node, items):
	items.sort()
	for key, value in items:
		addNode(node, key, value)

def makeTree(name, data):
	tree = Node('A Few JPython Classes')
	addLeaves(tree, data.items())
	return tree

if __name__ == '__main__':
	tree = makeTree('Some JPython Classes', data)
	swing.test(swing.JScrollPane(swing.JTree(tree)))
