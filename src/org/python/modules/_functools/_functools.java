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
}
