package org.python.core;

import junit.framework.TestCase;

/**
 * Tests for PyList as Java List.
 */
public class PyListTest extends TestCase {

    private PyList p = null;

    @Override
    protected void setUp() throws Exception {
        p = new PyList();
        p.add("foo");
        p.add("bar");
    }

    @Override
    protected void tearDown() throws Exception {
        p = null;
    }

    // Test for http://bugs.jython.org/issue1419
    // "Bug in PyTuple.indexOf and PyList.indexOf"
    public void testIndexOf() {
        assertEquals(0, p.indexOf("foo"));
        assertEquals(1, p.indexOf("bar"));
    }

    public void testToArray() {
        // In Jython 2.5.0 if an array was passed into toArray() that was
        // too short, an Object[] was always returned instead of an array
        // of the proper type.
        Object[] test = new String[1];
        String[] s = (String[])p.toArray(test);
        assertEquals(s[0], "foo");
        assertEquals(s[1], "bar");
    }
}
