class DFS:

  def __init__(self):
    self.visited_node_counter = 0

  def visitor(self):
    self.visited_node_counter += 1

  def visit(self, node):
    node.accept_visitor(self.visitor)
    for child in node.children: self.visit(child)

class Node:

  def __init__(self):
    self.children = []

  def add_child(self, node):
    self.children.append(node)

  def accept_visitor(self, visitor):
    visitor()

root = Node()

for i in xrange(0, firstLevelNodes):
  root.add_child(Node())

for child in root.children:
  for i in xrange(0, secondLevelNodes): child.add_child(Node())

dfs = DFS()
dfs.visit(root)

result = dfs.visited_node_counter
