"""lispify_ast - returns a tuple representation of the AST

Uses 2.5's _ast, not other AST implementations in CPython, since these
are not used by the compilation phase. And that's what we're
interested in.

Since this is a tuple, we can directly compare, and this is going to
be handy when comparing Jython's implementation vs CPython.

"""
import _ast
import os
import globwalk

def lispify_ast(node):
    return tuple(lispify_ast2(node))

def lispify_ast2(node):
    yield node.__class__.__name__
    try:
        for field in node._fields:
            yield tuple(lispify_field(field, getattr(node, field)))
    except:
        pass

def lispify_field(field, child):
    yield field
    if not isinstance(child, list):
        children = [child]
    else:
        children = child

    for node in children:
        if isinstance(node, _ast.AST):
            yield lispify_ast(node)
        else:
            if isinstance(node, float):
                #stringify floats so they match Java's float representation better
                yield str(node)
            else:
                yield node

if __name__ == '__main__':
    import sys
    from pprint import pprint

    code_path = sys.argv[1]
    ast = compile(open(code_path).read(), code_path, "exec", _ast.PyCF_ONLY_AST)

    lispified = lispify_ast(ast)
    pprint(lispified)
