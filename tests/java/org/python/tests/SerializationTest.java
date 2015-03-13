package org.python.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.AssertionError;
import java.lang.Integer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import junit.framework.TestCase;
import static org.junit.Assert.*;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyFrozenSet;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PySet;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.core.PyTuple;
import org.python.modules._csv.PyDialect;
import org.python.util.PythonInterpreter;
import org.python.util.PythonObjectInputStream;

public class SerializationTest extends TestCase {

    private PythonInterpreter interp;

    @Override
    protected void setUp() throws Exception {
        interp = new PythonInterpreter(new PyStringMap(), new PySystemState());
        interp.exec("from java.io import Serializable");
        interp.exec("class Test(Serializable): pass");
        interp.exec("x = Test()");
    }

    public void testDirect() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ObjectOutputStream(os).writeObject(interp.get("x"));
        new PythonObjectInputStream(new ByteArrayInputStream(os.toByteArray())).readObject();
    }

    public void testJython() {
        interp.set("t", this);
        interp.exec("t.testDirect()");
    }

    public void testBasicTypes() {
        assertRoundtrip(Py.None);
        assertRoundtrip(Py.True);
        assertRoundtrip(Py.False);
        assertRoundtrip(Py.newInteger(42));
        assertRoundtrip(Py.newLong(47));
        assertRoundtrip(Py.newString("Jython: Python for the Java Platform"));
        assertRoundtrip(Py.newUnicode("Drink options include \uD83C\uDF7A, \uD83C\uDF75, \uD83C\uDF77, and \u2615"));
        Map<PyObject, PyObject> map = new HashMap<>();
        map.put(Py.newString("OEIS interesting number"), Py.newInteger(14228));
        map.put(Py.newString("Hardy-Ramanujan number"), Py.newInteger(1729));
        assertRoundtrip(new PyDictionary(map));
        assertRoundtrip(new PyList(new PyObject[]{Py.newInteger(1), Py.newInteger(28), Py.newInteger(546), Py.newInteger(9450), Py.newInteger(157773)})); // A001234
        assertRoundtrip(new PySet(new PyObject[]{Py.Zero, Py.One}));
        assertRoundtrip(new PyFrozenSet(new PyTuple(Py.newInteger(1), Py.newInteger(2), Py.newInteger(3))));
        assertRoundtrip(new PyTuple(Py.newInteger(2), Py.newInteger(8), Py.newInteger(248), Py.newInteger(113281))); // A012345
    }

    private static class CloneOutput extends ObjectOutputStream {
        Queue<Class<?>> classQueue = new LinkedList<Class<?>>();

        CloneOutput(OutputStream out) throws IOException {
            super(out);
        }

        @Override
        protected void annotateClass(Class<?> c) {
            classQueue.add(c);
        }

        @Override
        protected void annotateProxyClass(Class<?> c) {
            classQueue.add(c);
        }
    }

    private static class CloneInput extends ObjectInputStream {
        private final CloneOutput output;

        CloneInput(InputStream in, CloneOutput output) throws IOException {
            super(in);
            this.output = output;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass osc)
                throws IOException, ClassNotFoundException {
            Class<?> c = output.classQueue.poll();
            String expected = osc.getName();
            String found = (c == null) ? null : c.getName();
            if (!expected.equals(found)) {
                throw new InvalidClassException("Classes desynchronized: " +
                        "found " + found + " when expecting " + expected);
            }
            return c;
        }

        @Override
        protected Class<?> resolveProxyClass(String[] interfaceNames)
                throws IOException, ClassNotFoundException {
            return output.classQueue.poll();
        }
    }

    public void assertRoundtrip(Object obj) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            CloneOutput serializer = new CloneOutput(output);
            serializer.writeObject(obj);
            serializer.close();
            ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
            CloneInput unserializer = new CloneInput(input, serializer);
            assertEquals(obj, unserializer.readObject());
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        } catch (ClassNotFoundException ex) {
            throw new AssertionError(ex);
        }
    }
}

