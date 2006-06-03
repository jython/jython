package org.python.modules.random;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyObject;

public class RandomModule implements ClassDictInit {

    private RandomModule() {}

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("Random", Py.java2py(PyRandom.class));
    }
}
