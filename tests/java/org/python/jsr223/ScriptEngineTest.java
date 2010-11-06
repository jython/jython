package org.python.jsr223;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.python.core.PyString;

public class ScriptEngineTest extends TestCase {

    public void testEvalString() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");

        assertNull(pythonEngine.eval("x = 5"));
        assertEquals(Integer.valueOf(5), pythonEngine.eval("x"));
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

        CompiledScript five = ((Compilable) pythonEngine).compile("5");
        assertEquals(Integer.valueOf(5), five.eval());
    }

    public void testEvalReader() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");

        assertNull(pythonEngine.eval(new StringReader("x = 5")));
        assertEquals(Integer.valueOf(5), pythonEngine.eval(new StringReader("x")));
    }

    public void testCompileEvalReader() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");

        CompiledScript five = ((Compilable) pythonEngine).compile(new StringReader("5"));
        assertEquals(Integer.valueOf(5), five.eval());
    }

    public void testBindings() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");

        pythonEngine.put("a", 42);
        assertEquals(Integer.valueOf(42), pythonEngine.eval("a"));
        assertNull(pythonEngine.eval("x = 5"));
        assertEquals(Integer.valueOf(5), pythonEngine.get("x"));
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

        public void run() {
            try {
                Bindings bindings = engine.createBindings();
                assertNull(engine.eval("try: a\nexcept NameError: pass\nelse: raise Exception('a is defined', a)", bindings));
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
        assertEquals(Integer.valueOf(-7), test.x);
        assertEquals(Integer.valueOf(15), pythonEngine.get("x"));
        assertNull(pythonEngine.eval("del x"));
        assertNull(pythonEngine.get("x"));
    }

    public void testInvoke() throws ScriptException, NoSuchMethodException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        Invocable invocableEngine = (Invocable) pythonEngine;

        assertNull(pythonEngine.eval("def f(x): return abs(x)"));
        assertEquals(Integer.valueOf(5), invocableEngine.invokeFunction("f", Integer.valueOf(-5)));
        assertEquals("spam", invocableEngine.invokeMethod(new PyString("  spam  "), "strip"));
        assertEquals("spam", invocableEngine.invokeMethod("  spam  ", "strip"));
    }

    public void testInvokeFunctionNoSuchMethod() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        Invocable invocableEngine = (Invocable) manager.getEngineByName("python");

        try {
            invocableEngine.invokeFunction("undefined");
        } catch (NoSuchMethodException e) {
            return;
        }
        assertTrue("Expected a NoSuchMethodException", false);
    }

    public void testInvokeMethodNoSuchMethod() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        Invocable invocableEngine = (Invocable) manager.getEngineByName("python");

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
        Invocable invocableEngine = (Invocable) pythonEngine;

        assertNull(pythonEngine.eval("def read(cb): return 1"));
        Readable readable = invocableEngine.getInterface(Readable.class);
        assertEquals(1, readable.read(null));

        assertNull(pythonEngine.eval(
                "class C(object):\n"
                + "    def read(self, cb): return 2\n"
                + "c = C()"));
        readable = invocableEngine.getInterface(pythonEngine.get("c"), Readable.class);
        assertEquals(2, readable.read(null));
    }

    public void testInvokeMethodNoSuchArgs() throws ScriptException, NoSuchMethodException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        Invocable invocableEngine = (Invocable) pythonEngine;

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
    
    public void testScope_repr() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        pythonEngine.eval("a = 4");
        pythonEngine.eval("b = 'hi'");
        pythonEngine.eval("localrepr = `locals()`");
        assertEquals("{'b': u'hi', 'a': 4}", pythonEngine.get("localrepr"));
    }
    
    public void testScope_iter() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        pythonEngine.eval("a = 4");
        pythonEngine.eval("b = 'hi'");
        pythonEngine.eval("list = []");
        pythonEngine.eval("for loc in locals(): list.append(loc)");
        pythonEngine.eval("listrepr = `list`");
        assertEquals("[u'a', u'b', u'list']", pythonEngine.get("listrepr"));
    }
    
    public void testScope_lookup() throws ScriptException{
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        pythonEngine.eval("a = 4");
        pythonEngine.eval("b = 'hi'");
        pythonEngine.eval("var_a = locals()['a']");
        pythonEngine.eval("arepr = `var_a`");
        assertEquals("4", pythonEngine.get("arepr"));
    }
    
}
