"""Extra unittests for ast in Jython.  Look to integrate into CPython in the
future"""

import unittest
import ast
from test import test_support

def srcExprToTree(source, kind='exec'):
    return compile(source, '<module>', kind, ast.PyCF_ONLY_AST)

class TestCompile(unittest.TestCase):

    def test_compile_ast(self):
        node = srcExprToTree("1/2")
        compile(node, "<string>", 'exec')

    def test_alias_trim(self):
        node = srcExprToTree("import os. path")
        self.assertEquals(node.body[0].names[0].name, "os.path")

        node = srcExprToTree("import os .path")
        self.assertEquals(node.body[0].names[0].name, "os.path")

        node = srcExprToTree("import os . path")
        self.assertEquals(node.body[0].names[0].name, "os.path")

    def test_cmpop(self):
        expr = srcExprToTree('a < b < c', 'eval')
        compare = expr.body
        self.assert_(isinstance(compare.ops[0], ast.Lt))
        self.assert_(isinstance(compare.comparators[0], ast.Name))
        self.assert_(isinstance(compare.ops[1], ast.Lt))
        self.assert_(isinstance(compare.comparators[1], ast.Name))
        self.assert_(isinstance(compare.ops[1:][0], ast.Lt))
        self.assert_(isinstance(compare.comparators[1:][0], ast.Name))
        z = zip( compare.ops[1:], compare.comparators[1:])
        self.assert_(isinstance(z[0][0], ast.Lt))
        self.assert_(isinstance(z[0][1], ast.Name))

#==============================================================================

def test_main(verbose=None):
    test_classes = [TestCompile]
    test_support.run_unittest(*test_classes)

if __name__ == "__main__":
    test_main(verbose=True)
