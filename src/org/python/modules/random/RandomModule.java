package org.python.modules.random;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyObject;

public class RandomModule implements ClassDictInit {

    private RandomModule() {}

    public static void classDictInit(PyObject dict) {
        dict.invoke("clear");
        dict.__setitem__("Random", PyRandom.TYPE);
        dict.__setitem__("__name__", Py.newString("_random"));
    }
}
