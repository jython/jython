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
    def get_class_name(t):
        result = t.__class__.__name__
        if result == "AugLoad":
            result = "Load"
        elif result == "AugStore":
            result = "Store"
        return result

else:
    get_class_name = lambda node: node.__class__.__name__

get_lines_and_cols = True

def lispify_ast(node):
    return tuple(lispify_ast2(node))

def lispify_ast2(node):
    result = get_class_name(node)
    if get_lines_and_cols and hasattr(node, 'lineno') and hasattr(node, 'col_offset'):
        result = "%s (%s,%s)" % (result, node.lineno, node.col_offset)
    yield result
    try:
        for field in node._fields:
            yield tuple(lispify_field(field, getattr(node, field)))
    except:
        pass

def lispify_field(field, child):
    yield field
    if isinstance(child, list):
        children = child
    else:
        children = [child]

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
        node = compile(open(pyfile).read(), pyfile, "exec", _ast.PyCF_ONLY_AST)
    except SyntaxError:
        return "SyntaxError",
    return lispify_ast(node)

if __name__ == '__main__':
    import pprint
    pprint.pprint(tree(sys.argv[1]))
