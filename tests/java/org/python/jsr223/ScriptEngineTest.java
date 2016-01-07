package org.python.jsr223;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.Arrays;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import junit.framework.TestCase;

import org.junit.Assert;
import org.python.core.Options;
import org.python.core.PyString;

public class ScriptEngineTest extends TestCase {

    public void testEvalString() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        ScriptContext context = pythonEngine.getContext();
        context.setAttribute(ScriptEngine.FILENAME, "sample.py", ScriptContext.ENGINE_SCOPE);
        assertNull(pythonEngine.eval("x = 5"));
        assertEquals(5, pythonEngine.eval("x"));
        assertEquals("sample.py", pythonEngine.eval("__file__"));
        pythonEngine.eval("import sys");
        assertEquals(Arrays.asList("sample.py"), pythonEngine.eval("sys.argv"));
    }

    public void testEvalStringArgv() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        ScriptContext context = pythonEngine.getContext();
        context.setAttribute(ScriptEngine.FILENAME, "sample.py", ScriptContext.ENGINE_SCOPE);
        context.setAttribute(ScriptEngine.ARGV, new String[] {"foo", "bar"}, ScriptContext.ENGINE_SCOPE);
        assertNull(pythonEngine.eval("x = 5"));
        assertEquals(5, pythonEngine.eval("x"));
        assertEquals("sample.py", pythonEngine.eval("__file__"));
        pythonEngine.eval("import sys");
        assertEquals(Arrays.asList("sample.py", "foo", "bar"), pythonEngine.eval("sys.argv"));
    }

    public void testEvalStringNoFilenameWithArgv() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        ScriptContext context = pythonEngine.getContext();
        context.setAttribute(ScriptEngine.ARGV, new String[] {"foo", "bar"}, ScriptContext.ENGINE_SCOPE);
        assertNull(pythonEngine.eval("x = 5"));
        assertEquals(5, pythonEngine.eval("x"));
        boolean gotExpectedException = false;
        try {
            pythonEngine.eval("__file__");
        } catch (ScriptException e) {
            assertTrue(e.getMessage().startsWith("NameError: "));
            gotExpectedException = true;
        }
        if (!gotExpectedException) {
            fail("Excepted __file__ to be undefined");
        }
        pythonEngine.eval("import sys");
        assertEquals(Arrays.asList("foo", "bar"), pythonEngine.eval("sys.argv"));
    }

    public void testSyntaxError() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");

        try {
            pythonEngine.eval("5q");
        } catch (ScriptException e) {
            assertEquals(e.getColumnNumber(), 1);
            assertEquals(e.getLineNumber(), 1);
            assertTrue(e.getMessage().startsWith("SyntaxError: "));
            return;
        }
        assertTrue("Expected a ScriptException", false);
    }

    public void testPythonException() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");

        try {
            pythonEngine.eval("pass\ndel undefined");
        } catch (ScriptException e) {
            assertEquals(e.getLineNumber(), 2);
            assertTrue(e.getMessage().startsWith("NameError: "));
            return;
        }
        assertTrue("Expected a ScriptException", false);
    }

    public void testScriptFilename() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setAttribute(ScriptEngine.FILENAME, "sample.py", ScriptContext.ENGINE_SCOPE);
        try {
            pythonEngine.eval("foo", scriptContext);
        } catch (ScriptException e) {
            assertEquals("sample.py", e.getFileName());
            return;
        }
        assertTrue("Expected a ScriptException", false);
    }

    public void testCompileEvalString() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        ScriptContext context = pythonEngine.getContext();
        context.setAttribute(ScriptEngine.FILENAME, "sample.py", ScriptContext.ENGINE_SCOPE);
        CompiledScript five = ((Compilable)pythonEngine).compile("5");
        assertEquals(5, five.eval());
        assertEquals("sample.py", pythonEngine.eval("__file__"));
        pythonEngine.eval("import sys");
        assertEquals(Arrays.asList("sample.py"), pythonEngine.eval("sys.argv"));
    }

    public void testEvalReader() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        ScriptContext context = pythonEngine.getContext();
        context.setAttribute(ScriptEngine.FILENAME, "sample.py", ScriptContext.ENGINE_SCOPE);
        assertNull(pythonEngine.eval(new StringReader("x = 5")));
        assertEquals(5, pythonEngine.eval(new StringReader("x")));
        assertEquals("sample.py", pythonEngine.eval("__file__"));
        pythonEngine.eval("import sys");
        assertEquals(Arrays.asList("sample.py"), pythonEngine.eval("sys.argv"));
    }

    public void testCompileEvalReader() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        ScriptContext context = pythonEngine.getContext();
        context.setAttribute(ScriptEngine.FILENAME, "sample.py", ScriptContext.ENGINE_SCOPE);
        CompiledScript five = ((Compilable)pythonEngine).compile(new StringReader("5"));
        assertEquals(5, five.eval());
        assertEquals("sample.py", pythonEngine.eval("__file__"));
        pythonEngine.eval("import sys");
        assertEquals(Arrays.asList("sample.py"), pythonEngine.eval("sys.argv"));
    }

    public void testBindings() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");

        pythonEngine.put("a", 42);
        assertEquals(42, pythonEngine.eval("a"));
        assertNull(pythonEngine.eval("x = 5"));
        assertEquals(5, pythonEngine.get("x"));
        assertNull(pythonEngine.eval("del x"));
        assertNull(pythonEngine.get("x"));
    }

    class ThreadLocalBindingsTest implements Runnable {

        ScriptEngine engine;
        Object x;
        Throwable exception;

        public ThreadLocalBindingsTest(ScriptEngine engine) {
            this.engine = engine;
        }

        @Override
        public void run() {
            try {
                Bindings bindings = engine.createBindings();
                assertNull(engine.eval(
                        "try: a\nexcept NameError: pass\nelse: raise Exception('a is defined', a)",
                        bindings));
                bindings.put("x", -7);
                x = engine.eval("x", bindings);
            } catch (Throwable e) {
                e.printStackTrace();
                exception = e;
            }
        }
    }

    public void testThreadLocalBindings() throws ScriptException, InterruptedException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");

        pythonEngine.put("a", 42);
        pythonEngine.put("x", 15);

        ThreadLocalBindingsTest test = new ThreadLocalBindingsTest(pythonEngine);
        Thread thread = new Thread(test);
        thread.run();
        thread.join();
        assertNull(test.exception);
        assertEquals(-7, test.x);
        assertEquals(15, pythonEngine.get("x"));
        assertNull(pythonEngine.eval("del x"));
        assertNull(pythonEngine.get("x"));
    }

    public void testInvoke() throws ScriptException, NoSuchMethodException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        Invocable invocableEngine = (Invocable)pythonEngine;

        assertNull(pythonEngine.eval("def f(x): return abs(x)"));
        assertEquals(5, invocableEngine.invokeFunction("f", -5));
        assertEquals("spam", invocableEngine.invokeMethod(new PyString("  spam  "), "strip"));
        assertEquals("spam", invocableEngine.invokeMethod("  spam  ", "strip"));
    }

    public void testInvokeFunctionNoSuchMethod() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        Invocable invocableEngine = (Invocable)manager.getEngineByName("python");

        try {
            invocableEngine.invokeFunction("undefined");
        } catch (NoSuchMethodException e) {
            return;
        }
        assertTrue("Expected a NoSuchMethodException", false);
    }

    public void testInvokeMethodNoSuchMethod() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        Invocable invocableEngine = (Invocable)manager.getEngineByName("python");

        try {
            invocableEngine.invokeMethod("eggs", "undefined");
            fail("Expected a NoSuchMethodException");
        } catch (NoSuchMethodException e) {
            assertEquals("undefined", e.getMessage());
        }
    }

    public void testGetInterface() throws ScriptException, IOException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        Invocable invocableEngine = (Invocable)pythonEngine;

        assertNull(pythonEngine.eval("def read(cb): return 1"));
        Readable readable = invocableEngine.getInterface(Readable.class);
        assertEquals(1, readable.read(null));

        assertNull(pythonEngine.eval("class C(object):\n" + "    def read(self, cb): return 2\n"
                + "c = C()"));
        readable = invocableEngine.getInterface(pythonEngine.get("c"), Readable.class);
        assertEquals(2, readable.read(null));
    }

    public void testInvokeMethodNoSuchArgs() throws ScriptException, NoSuchMethodException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        Invocable invocableEngine = (Invocable)pythonEngine;

        Object newStringCapitalize = invocableEngine.invokeMethod("test", "capitalize");
        assertEquals(newStringCapitalize, "Test");
    }

    public void testPdb() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        // String from issue 1674
        String pdbString = "from pdb import set_trace; set_trace()";
        try {
            pythonEngine.eval(pdbString);
            fail("bdb.BdbQuit expected");
        } catch (ScriptException e) {
            assertTrue(e.getMessage().startsWith("bdb.BdbQuit"));
        }
    }

    // FIXME PyScriptEngineScope lacks items(), iteritems(), and other dict methods
    // This should be added in a future release

    public void testScope_repr() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        pythonEngine.eval("a = 4");
        pythonEngine.eval("b = 'hi'");
        String repr = (String)pythonEngine.eval("repr(locals())");
        // locals() contains builtins as of 2.7.0, so we need to selectively test
        Assert.assertTrue(repr.contains("'a': 4"));
        Assert.assertTrue(repr.contains("'b': u'hi'"));
    }

    public void testScope_iter() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        pythonEngine.eval("a = 4");
        pythonEngine.eval("b = 'hi'");
        assertEquals(
                "['__builtins__', 'a', 'b']",
                pythonEngine.eval("repr(sorted((item for item in locals())))"));
    }

    public void testScope_lookup() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        pythonEngine.eval("a = 4");
        pythonEngine.eval("b = 'hi'");
        pythonEngine.eval("var_a = locals()['a']");
        pythonEngine.eval("arepr = repr(var_a)");
        assertEquals("4", pythonEngine.get("arepr"));
    }

    public void testIssue1681() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        pythonEngine.eval("from org.python.jsr223 import PythonCallable\n"
                + "class MyPythonCallable(PythonCallable):\n"
                + "    def getAString(self): return 'a string'\n\n"
                + "result = MyPythonCallable().getAString()\n" //
                + "test = MyPythonCallable()\n" //
                + "result2 = test.getAString()");
        assertEquals("a string", pythonEngine.get("result"));
        assertEquals("a string", pythonEngine.get("result2"));
    }

    public void testIssue1698() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        pythonEngine.eval("import warnings");
        // Would previously fail
        pythonEngine.eval("warnings.warn('test')");
    }

    public void testSiteImportedByDefault() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        pythonEngine.eval("import sys");
        pythonEngine.eval("'site' in sys.modules");
    }

    public void testSiteCanBeNotImported() throws ScriptException {
        try {
            Options.importSite = false;
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine pythonEngine = manager.getEngineByName("python");

            pythonEngine.eval("import sys");
            pythonEngine.eval("'site' not in sys.modules");
        } finally {
            Options.importSite = true;
        }
    }

    public void testIssue2090() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        pythonEngine.eval("a = 10L\n" + "b = a-1");
        Object r = pythonEngine.get("b");
        assertEquals(new BigInteger("9"), r);
    }
}
