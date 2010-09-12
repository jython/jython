package org.python.core;

import java.math.BigInteger;

import junit.framework.TestCase;

import org.python.util.PythonInterpreter;

public class WrappedLongTest extends TestCase {

    // Simulate the use case where you want to expose some (possibly mutable)
    // java long field to an interpreter without having to set the value to a
    // new PyLong each time it changes.
    @SuppressWarnings("serial")
    static class WrappedLong extends PyLong {
        public WrappedLong() {
            super(0);
        }

        private long mutableValue;

        @Override
        public BigInteger getValue() {
            return BigInteger.valueOf(mutableValue);
        }

        public void setMutableValue(final long newValue) {
            mutableValue = newValue;
        }
    }

    private PythonInterpreter interp;
    private WrappedLong a, b;

    @Override
    protected void setUp() throws Exception {
        interp = new PythonInterpreter(new PyStringMap(), new PySystemState());
        a = new WrappedLong();
        b = new WrappedLong();
        a.setMutableValue(13000000000L);
        b.setMutableValue(17000000000L);
        interp.set("a", a);
        interp.set("b", b);
    }

    public void testAdd() {
        interp.exec("c = a + b");
        assertEquals(new PyLong(30000000000L), interp.get("c"));
        b.setMutableValue(18000000000L);
        interp.exec("c = a + b");
        assertEquals(new PyLong(31000000000L), interp.get("c"));
    }

    public void testMod() {
        interp.exec("c = b % a");
        assertEquals(new PyLong(4000000000L), interp.get("c"));
    }
}
