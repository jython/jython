package org.python.core;

import junit.framework.TestCase;

import org.python.util.PythonInterpreter;

public class WrappedIntegerTest extends TestCase {

    // Simulate the use case where you want to expose some (possibly mutable)
    // java int field to an interpreter without having to set the value to a new
    // PyInteger each time it changes.
    @SuppressWarnings("serial")
    static class WrappedInteger extends PyInteger {
        public WrappedInteger() {
            super(0);
        }

        private int mutableValue;

        @Override
        public int getValue() {
            return mutableValue;
        }

        public void setMutableValue(final int newValue) {
            mutableValue = newValue;
        }
    }

    private PythonInterpreter interp;
    private WrappedInteger a, b;

    @Override
    protected void setUp() throws Exception {
        interp = new PythonInterpreter(new PyStringMap(), new PySystemState());
        a = new WrappedInteger();
        b = new WrappedInteger();
        a.setMutableValue(13);
        b.setMutableValue(17);
        interp.set("a", a);
        interp.set("b", b);
    }

    public void testAdd() {
        interp.exec("c = a + b");
        assertEquals(new PyInteger(30), interp.get("c"));
        b.setMutableValue(18);
        interp.exec("c = a + b");
        assertEquals(new PyInteger(31), interp.get("c"));
    }

    public void testDiv() {
        interp.exec("c = a / float(b)");
        assertEquals(new PyFloat(13 / 17.), interp.get("c"));
    }

    public void testMod() {
        interp.exec("c = b % a");
        assertEquals(new PyInteger(4), interp.get("c"));
    }
}
