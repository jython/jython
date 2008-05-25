#!/usr/bin/env python
"""lispify_ast - returns a tuple representation of the AST

Uses 2.5's _ast, not other AST implementations in CPython, since these
are not used by the compilation phase. And that's what we're
interested in.

Since this is a tuple, we can directly compare, and this is going to
be handy when comparing Jython's implementation vs CPython.

"""

import _ast
import sys

if sys.platform.startswith('java'):

    get_symbol_key = lambda op: op
        
    def get_class_name(t):
        result = t.__class__.__name__
        if result in ("org.python.antlr.ast.expr_contextType",
                "org.python.antlr.ast.boolopType",
                "org.python.antlr.ast.unaryopType",
                "org.python.antlr.ast.cmpopType",
                "org.python.antlr.ast.operatorType"):
            result = str(t)
        else:
            result = result.split(".")[-1]
            if result.endswith("Type"):
                result = result[:-4]
            if result == "Unicode":
                result = "Str"
        return result

else:
    get_symbol_key = type
    get_class_name = lambda node: node.__class__.__name__

def lispify_ast(node):
    return tuple(lispify_ast2(node))

def lispify_ast2(node):
    yield get_class_name(node)
    try:
        for field in node._fields:
            yield tuple(lispify_field(field, getattr(node, field)))
    except:
        pass

def lispify_field(field, child):
    yield field
    if not hasattr(child, '__iter__'):
        children = [child]
    else:
        children = child

    for node in children:
        if isinstance(node, _ast.AST):
            yield lispify_ast(node)
        else:
            if isinstance(node, float):
                #XXX: stringify floats so they match Java's float representation better
                #This may mask problems for very small numbers.
                if .0001 < node < 10000:
                    yield "%5.5f" % node
                else:
                    yield "%.5e" % node
            else:
                yield node

def tree(pyfile):
    try:
        ast = compile(open(pyfile).read(), pyfile, "exec", _ast.PyCF_ONLY_AST)
    except SyntaxError:
        return "SyntaxError",
    return lispify_ast(ast)

if __name__ == '__main__':
    import pprint
    pprint.pprint(tree(sys.argv[1]))
