package org.python.util;

import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;

import org.python.core.Console;
import org.python.core.PlainConsole;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyUnicode;

public class InterpreterTest extends TestCase {

    /**
     * Motivated by a NPE reported on http://bugs.jython.org/issue1174.
     */
    public void testBasicEval() throws Exception {
        PyDictionary test = new PyDictionary();
        test.__setitem__(new PyUnicode("one"), new PyUnicode("two"));
        PythonInterpreter.initialize(System.getProperties(), null, new String[] {});
        PythonInterpreter interp = new PythonInterpreter();
        PyObject pyo = interp.eval("{u'one': u'two'}");
        assertEquals(test, pyo);
    }

    public void testMultipleThreads() {
        final CountDownLatch doneSignal = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            new Thread() {
                @Override
                public void run() {
                    PythonInterpreter interp = new PythonInterpreter();
                    interp.exec("import sys");
                    interp.set("a", new PyInteger(41));
                    int set = Py.tojava(interp.get("a"), Integer.class);
                    assertEquals(41, set);
                    interp.exec("x = 'hello ' + 'goodbye'");
                    assertEquals("hello goodbye", Py.tojava(interp.get("x"), String.class));
                    doneSignal.countDown();
                }
            }.start();
        }
        try {
            doneSignal.await();
        } catch (InterruptedException e) {
            System.err.println("Interpreters in multiple threads test interrupted, bailing");
        }
    }

    public void testCallInstancesFromJava() {
        PythonInterpreter interp = new PythonInterpreter();
        interp.exec("class Blah(object):\n" +
                    "    def __init__(self, val):\n" +
                    "        self.val = val\n" +
                    "    def incval(self):\n" +
                    "        self.val += 1\n" +
                    "        return self.val");
        PyObject blahClass = interp.get("Blah");
        int base = 42;
        PyObject blahInstance = blahClass.__call__(new PyInteger(base));
        for (int i = 0; i < 4; i++) {
            assertEquals(++base, blahInstance.invoke("incval").__tojava__(Integer.class));
        }
    }

    /**
     * Show that a PythonInterpreter comes by default with a PlainConsole (not JLine say).
     */
    public void testConsoleIsPlain() throws Exception {
        PythonInterpreter interp = new PythonInterpreter();
        interp.exec("import sys");
        Console console = Py.tojava(interp.eval("sys._jy_console"), Console.class);
        assertEquals(PlainConsole.class, console.getClass());
        Console console2 = Py.getConsole();
        assertEquals(PlainConsole.class, console2.getClass());
    }

}
