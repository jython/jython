from org.antlr.runtime import * 
from org.antlr.runtime.tree import *
from org.python.antlr import *

def build_ast(path):
    input = ANTLRFileStream(path)
    lexer = Main.MyLexer(input)
    tokens = CommonTokenStream(lexer) 
    tokens.discardOffChannelTokens(1)
    indentedSource = PythonTokenSource(tokens)
    tokens2 = CommonTokenStream(indentedSource)
    parser = PythonParser(tokens2)
    return parser.module().getTree()

def lispify_ast2(node):
    stuff = [node]
    for i in range(node.getChildCount()):
        child = node.getChild(i)
        if isinstance(child, CommonTree):
            stuff.append(lispify_ast(child))
        else:
            stuff.append(child.toString())
    return tuple(stuff)

def lispify_ast(node):
    if not isinstance(node, CommonTree):
        return node
    stuff = [node]
    for i in range(node.getChildCount()):
        child = node.getChild(i)
        stuff.append(lispify_ast(child))
    return tuple(stuff)


def lispify_ast(node):
    if not isinstance(node, CommonTree):
        return node
    stuff = [node]
    for i in range(node.getChildCount()):
        child = node.getChild(i)
        if child.getChildCount():
            stuff.append(lispify_ast(child))
        else:
            stuff.append(child)
    return tuple(stuff)

# Display ASTs graphically
# thanks to the code from Jorg Werner
# src: ANTLR wiki
def graph_ast(node):
	af = ASTFrame("Jython tree for "+sys.argv[1],node)
	af.setVisible(True)


if __name__ == '__main__':
    import sys
    #from pprint import pprint
    
    ast = build_ast(sys.argv[1])
    # print ast.toStringTree()
    print (lispify_ast(ast))
    #graph_ast(ast)
