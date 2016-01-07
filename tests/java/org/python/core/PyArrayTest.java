package org.python.core;

import junit.framework.TestCase;
import org.junit.Assert;
import org.python.util.PythonInterpreter;

/**
 * Tests for PyArray.
 */
public class PyArrayTest extends TestCase {

    public void testSetSliceNegativeStep() {
        PyArray arrayToModify = new PyArray(PyString.class, new String[] {"a", "b", "c", "d"});

        // Replacing single element
        PyArray arrayOneElement = new PyArray(PyString.class, new String[] {"z"});
        arrayToModify.setslice(0, 0, -1, arrayOneElement);
        assertEquals(new PyArray(PyString.class, new String[] {"z", "b", "c", "d"}), arrayToModify);

        // Replacing multiple elements
        arrayToModify = new PyArray(PyString.class, new String[] {"a", "b", "c", "d"});
        PyArray arrayThreeElements = new PyArray(PyString.class, new String[] {"x", "y", "z"});
        arrayToModify.setslice(2, 0, -1, arrayThreeElements);
        assertEquals(new PyArray(PyString.class, new String[] {"z", "y", "x", "d"}), arrayToModify);

        // Replacing multiple elements - step size = (-2)
        arrayToModify = new PyArray(PyString.class, new String[] {"a", "b", "c", "d"});
        PyArray arrayTwoElements = new PyArray(PyString.class, new String[] {"x", "y"});
        arrayToModify.setslice(3, 0, -2, arrayTwoElements);
        assertEquals(new PyArray(PyString.class, new String[] {"a", "y", "c", "x"}), arrayToModify);

    }

    public void testSetSlicePositiveStep() {
        PyArray arrayToModify = new PyArray(PyString.class, new String[] {"a", "b", "c", "d"});

        // Replacing single element
        PyArray arrayOneElement = new PyArray(PyString.class, new String[] {"z"});
        arrayToModify.setslice(0, 1, 1, arrayOneElement);
        assertEquals(new PyArray(PyString.class, new String[] {"z", "b", "c", "d"}), arrayToModify);

        // Replacing multiple elements
        arrayToModify = new PyArray(PyString.class, new String[] {"a", "b", "c", "d"});
        PyArray arrayMultipleElements = new PyArray(PyString.class, new String[] {"x", "y"});
        arrayToModify.setslice(1, 3, 1, arrayMultipleElements);
        assertEquals(new PyArray(PyString.class, new String[] {"a", "x", "y", "d"}), arrayToModify);

        // Replace multiple elements - step = 2
        arrayToModify = new PyArray(PyString.class, new String[] {"a", "b", "c", "d"});
        arrayMultipleElements = new PyArray(PyString.class, new String[] {"x", "y"});
        arrayToModify.setslice(0, 3, 2, arrayMultipleElements);
        assertEquals(new PyArray(PyString.class, new String[] {"x", "b", "y", "d"}), arrayToModify);

    }

    public void testCompactArray() {
        // tests http://bugs.jython.org/issue1745
        PythonInterpreter interp = new PythonInterpreter();
        interp.set("arr", new double[3]);
        interp.exec("arr.append(3.0)\n\n");
        Object o = interp.get("arr", Object.class);
        double[] a = (double[])o;
        assertEquals(4, a.length);
        assertEquals(0.0, a[0]);
        assertEquals(3.0, a[3]);
    }

    public void testToJava() {
        PythonInterpreter interp = new PythonInterpreter();
        PyObject pyobj = interp.eval("[i * 2 for i in xrange(5)]");
        Assert.assertArrayEquals
                (new int[] {0, 2, 4, 6, 8},
                (int[])pyobj.__tojava__(int[].class));
        Assert.assertArrayEquals(
                new Integer[] { 0, 2, 4, 6, 8},
                (Object[])pyobj.__tojava__(Object[].class));
    }
}
