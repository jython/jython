package org.python.jsr223;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.ScriptEngineFactory;
import javax.script.SimpleBindings;

import junit.framework.TestCase;

import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

public class ScriptEngineIOTest extends TestCase
{
    ScriptEngineFactory pythonEngineFactory;
    ScriptEngine pythonEngine;

    public void setUp() throws ScriptException
    {
        pythonEngineFactory = new PyScriptEngineFactory();
        pythonEngine = new PyScriptEngine(pythonEngineFactory);
    }

    public void testEvalString() throws ScriptException
    {
        assertNull(pythonEngine.eval("x = 5"));
        assertEquals(Integer.valueOf(5), pythonEngine.eval("x"));
    }

    public void testReadline() throws ScriptException
    {
        final String testString = "Shazaam Batman!\n";

        pythonEngine.getContext().setReader(new StringReader(testString));

        assertNull(pythonEngine.eval("import sys"));
        assertEquals(testString, pythonEngine.eval("sys.stdin.readline()"));
    }

    public void testReadlines() throws ScriptException
    {
        final String testString = "Holy Smokes Batman!\nBIF!\r\n\nKAPOW!!!\rTHE END.";

        pythonEngine.getContext().setReader(new StringReader(testString));

        assertNull(pythonEngine.eval("import sys"));
        final Object o = pythonEngine.eval("''.join(sys.stdin.readlines())");
        
        assertEquals("Holy Smokes Batman!\nBIF!\n\nKAPOW!!!\nTHE END.\n", o);
    }

    public void testWriter() throws ScriptException
    {
        final StringWriter sw = new StringWriter();

        pythonEngine.getContext().setWriter(sw);

        final String testString = "It is a wonderful world.";

        assertNull(pythonEngine.eval("print '" + testString + "',"));
        assertEquals(testString, sw.toString());
    }

    public void testErrorWriter() throws ScriptException
    {
        final StringWriter stdout = new StringWriter();
        final StringWriter stderr = new StringWriter();

        pythonEngine.getContext().setWriter(stdout);
        pythonEngine.getContext().setErrorWriter(stderr);

        final String testString1 = "It is a wonderful world.";
        final String testString2 = "Stuff happens!";

        assertNull(pythonEngine.eval("import sys"));
        assertNull(pythonEngine.eval("sys.stdout.write('" + testString1 + "')"));
        assertNull(pythonEngine.eval("sys.stderr.write('" + testString2 + "')"));

        assertEquals(testString1, stdout.toString());
        assertEquals(testString2, stderr.toString());
    }

    public void testEvalWithReader() throws ScriptException, FileNotFoundException
    {
        //Check that multiple evals don't cause an NPE.
        //See issue http://bugs.jython.org/issue1536
        final ScriptEngineManager manager = new ScriptEngineManager();

        final String engineType = "jython";
        final ScriptEngine engine = manager.getEngineByName(engineType);

        final StringWriter stdout = new StringWriter();
        final StringWriter stderr = new StringWriter();

        engine.getContext().setWriter(stdout);
        engine.getContext().setErrorWriter(stderr);


        final Bindings bindings = new SimpleBindings();
        bindings.put("firstLevelNodes", 10);
        bindings.put("secondLevelNodes", 5);

        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

        final Reader dfsScript = new FileReader("tests/python/dfs.py");

        for (int i = 1; i <= 10; i++) {
            engine.eval(dfsScript);
            assertEquals(61, engine.get("result"));
        }
    }

   public void testGetInterfaceCharSequence1() throws ScriptException, IOException {
           ScriptEngineManager manager = new ScriptEngineManager();
           ScriptEngine engine = manager.getEngineByName("python");
           Invocable invocableEngine = (Invocable) engine;

           assertNull(engine.eval(
                   "from java.lang import CharSequence\n" +
                   "class MyString(CharSequence):\n" +
                   "   def length(self): return 3\n" +
                   "   def charAt(self, index): return 'a'\n" +
                   "   def subSequence(self, start, end): return \"\"\n" +
                   "   def toString(self): return \"aaa\"\n" +
                   "c = MyString()"));
           CharSequence seq = invocableEngine.getInterface(engine.get("c"), CharSequence.class);
           assertEquals("aaa", seq.toString());
   }

   public void testGetInterfaceCharSequence2() throws ScriptException, IOException {
           ScriptEngineManager manager = new ScriptEngineManager();
           ScriptEngine pythonEngine = manager.getEngineByName("python");
           Invocable invocableEngine = (Invocable) pythonEngine;

           assertNull(pythonEngine.eval(
                   "from java.lang import StringBuilder\r\n" +
                   "c = StringBuilder(\"abc\")\r\n"));
           CharSequence seq = invocableEngine.getInterface(pythonEngine.get("c"), CharSequence.class);
           assertEquals("abc", seq.toString());
   }
}
