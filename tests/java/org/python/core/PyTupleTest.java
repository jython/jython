package org.python.core;

import junit.framework.TestCase;

/**
 * Tests for PyTuple as Java Tuple.
 */
public class PyTupleTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

    // Test for http://bugs.jython.org/issue1419
    // "Bug in PyTuple.indexOf and PyTuple.indexOf"
    public void testFoo() {
        PyTuple p = new PyTuple(new PyString("foo"), new PyString("bar"));
        assertEquals(0, p.indexOf("foo"));
        assertEquals(1, p.indexOf("bar"));
    }
}
