package org.python.core;

import junit.framework.TestCase;

import org.python.util.PythonInterpreter;

public class WrappedFloatTest extends TestCase {

    // Simulate the use case where you want to expose some (possibly mutable)
    // java float field to an interpreter without having to set the value to a new
    // PyFloat each time it changes.
    @SuppressWarnings("serial")
    static class WrappedFloat extends PyFloat {
        public WrappedFloat() {
            super(0);
        }

        private double mutableValue;

        @Override
        public double getValue() {
            return mutableValue;
        }

        public void setMutableValue(final double newValue) {
            mutableValue = newValue;
        }
    }

    private PythonInterpreter interp;
    private WrappedFloat a, b;

    @Override
    protected void setUp() throws Exception {
        interp = new PythonInterpreter(new PyStringMap(), new PySystemState());
        a = new WrappedFloat();
        b = new WrappedFloat();
        a.setMutableValue(13.0);
        b.setMutableValue(17.0);
        interp.set("a", a);
        interp.set("b", b);
    }

    public void testAdd() {
        interp.exec("c = a + b");
        assertEquals(new PyFloat(30), interp.get("c"));
        b.setMutableValue(18.0);
        interp.exec("c = a + b");
        assertEquals(new PyFloat(31), interp.get("c"));
    }

    public void testDiv() {
        interp.exec("c = a / b");
        assertEquals(new PyFloat(13 / 17.), interp.get("c"));
    }

    public void testMod() {
        interp.exec("c = b % a");
        assertEquals(new PyFloat(4), interp.get("c"));
    }
}
