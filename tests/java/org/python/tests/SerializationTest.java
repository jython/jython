package org.python.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

import org.python.core.PyStringMap;
import org.python.core.PySystemState;
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
}
