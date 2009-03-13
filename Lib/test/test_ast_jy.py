"""Extra unittests for ast in Jython.  Look to integrate into CPython in the
future"""

import unittest
from test import test_support
from ast import PyCF_ONLY_AST

class TestCompile(unittest.TestCase):

    def test_compile_ast(self):
        node = compile("1/2", '<unknown>', 'exec', PyCF_ONLY_AST)
        compile(node, "<string>", 'exec')

    def test_alias_trim(self):
        node = compile("import os. path", '<unknown>', 'exec', PyCF_ONLY_AST)
        self.assertEquals(node.body[0].names[0].name, "os.path")

        node = compile("import os .path", '<unknown>', 'exec', PyCF_ONLY_AST)
        self.assertEquals(node.body[0].names[0].name, "os.path")

        node = compile("import os . path", '<unknown>', 'exec', PyCF_ONLY_AST)
        self.assertEquals(node.body[0].names[0].name, "os.path")


#==============================================================================

def test_main(verbose=None):
    test_classes = [TestCompile]
    test_support.run_unittest(*test_classes)

if __name__ == "__main__":
    test_main(verbose=True)
