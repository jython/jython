/* Copyright (c) Jython Developers */
package org.python.modules._functools;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;

/**
 * The Python _functools module.
 */
public class _functools implements ClassDictInit {

    public static final PyString __doc__ = new PyString("Tools that operate on functions.");

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyString("_functools"));
        dict.__setitem__("__doc__", __doc__);
        dict.__setitem__("partial", PyPartial.TYPE);

        // Hide from Python
        dict.__setitem__("classDictInit", null);
    }

    public static PyString __doc__reduce = new PyString(
    "reduce(function, sequence[, initial]) -> value\n\n" +
    "Apply a function of two arguments cumulatively to the items of a sequence,\n" +
    "from left to right, so as to reduce the sequence to a single value.\n" +
    "For example, reduce(lambda x, y: x+y, [1, 2, 3, 4, 5]) calculates\n" +
    "((((1+2)+3)+4)+5).  If initial is present, it is placed before the items\n" +
    "of the sequence in the calculation, and serves as a default when the\n" +
    "sequence is empty.");

    public static PyObject reduce(PyObject f, PyObject l, PyObject z) {
        PyObject result = z;
        PyObject iter = Py.iter(l, "reduce() arg 2 must support iteration");

        for (PyObject item; (item = iter.__iternext__()) != null;) {
            if (result == null) {
                result = item;
            } else {
                result = f.__call__(result, item);
            }
        }
        if (result == null) {
            throw Py.TypeError("reduce of empty sequence with no initial value");
        }
        return result;
    }

    public static PyObject reduce(PyObject f, PyObject l) {
        return reduce(f, l, null);
    }

}
