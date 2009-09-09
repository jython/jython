package org.python.core;

import junit.framework.TestCase;

/**
 * Tests for PyList as Java List.
 */
public class PyListTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

    // Test for http://bugs.jython.org/issue1419
    // "Bug in PyTuple.indexOf and PyList.indexOf"
    public void testFoo() {
        PyList p = new PyList();
        p.add("foo");
        p.add("bar");
        assertEquals(0, p.indexOf("foo"));
        assertEquals(1, p.indexOf("bar"));
    }
}
