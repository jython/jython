package org.python.jsr223;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.ScriptEngineFactory;

import junit.framework.TestCase;

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
}
