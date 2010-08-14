# test for ScriptEngine, otherwise we are not in Java 6/JSR223 support added

# test each feature

import sys
import unittest
from javax.script import ScriptEngine, ScriptEngineManager
from test import test_support


class JSR223TestCase(unittest.TestCase):
    def setUp(self):
        self.engine = ScriptEngineManager().getEngineByName("python")

    def test_eval(self):
        engine = self.engine
        engine.put("a", 42)
        engine.eval("b = a/6")
        self.assertEqual(engine.get("b"), 7)

    def test_factory(self):
        f = self.engine.factory
        language_version = ".".join(str(comp) for comp in sys.version_info[0:2]) # such as "2.5"
        impl_version =  ".".join(str(comp) for comp in sys.version_info[0:3]) # such as "2.5.2"

        self.assertNotEqual(f.scriptEngine, self.engine) # we don't pool engines
        
        self.assertEqual(f.engineName, "jython")
        self.assertEqual(f.engineVersion, impl_version)
        self.assertEqual(set(f.extensions), set(['py']))
        self.assertEqual(f.languageName, "python")
        self.assertEqual(f.languageVersion, language_version)
        self.assertEqual(set(f.names), set(["python", "jython"]))
        self.assertEqual(set(f.mimeTypes), set(["text/python", "application/python", "text/x-python", "application/x-python"]))
        
        # variants
        self.assertEqual(f.getParameter(ScriptEngine.ENGINE), "jython")
        self.assertEqual(f.getParameter(ScriptEngine.ENGINE_VERSION), impl_version)
        self.assertEqual(f.getParameter(ScriptEngine.NAME), "jython")
        self.assertEqual(f.getParameter(ScriptEngine.LANGUAGE), "python")
        self.assertEqual(f.getParameter(ScriptEngine.LANGUAGE_VERSION), language_version)

        self.assertEqual(f.getOutputStatement("abc"), "print u'abc'")
        self.assertEqual(f.getProgram("x = 42", "y = 'abc'"), "x = 42\ny = 'abc'\n")
        
        


def test_main():
    test_support.run_unittest(
        JSR223TestCase)


if __name__ == "__main__":
    test_main()
