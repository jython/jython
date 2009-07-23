package org.python.jsr223;

import java.io.IOException;
import java.io.StringReader;
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
        
        CompiledScript five = ((Compilable)pythonEngine).compile("5");
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

        CompiledScript five = ((Compilable)pythonEngine).compile(new StringReader("5"));
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

    public void testInvoke() throws ScriptException, NoSuchMethodException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        Invocable invocableEngine = (Invocable)pythonEngine;

        assertNull(pythonEngine.eval("def f(x): return abs(x)"));
        assertEquals(Integer.valueOf(5), invocableEngine.invokeFunction("f", Integer.valueOf(-5)));
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
        } catch (NoSuchMethodException e) {
            return;
        }
        assertTrue("Expected a NoSuchMethodException", false);
    }

    public void testGetInterface() throws ScriptException, IOException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine pythonEngine = manager.getEngineByName("python");
        Invocable invocableEngine = (Invocable)pythonEngine;

        assertNull(pythonEngine.eval("def read(cb): return 1"));
        Readable readable = invocableEngine.getInterface(Readable.class);
        assertEquals(1, readable.read(null));

        assertNull(pythonEngine.eval(
                "class C(object):\n" + 
                "    def read(self, cb): return 2\n" +
                "c = C()"));
        readable = invocableEngine.getInterface(pythonEngine.get("c"), Readable.class);
        assertEquals(2, readable.read(null));
    }
}
