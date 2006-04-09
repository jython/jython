
package org.python.modules.sets;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyObject;

public class Sets implements ClassDictInit {
    private Sets() {}

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("Set", Py.java2py(PySet.class));
        dict.__setitem__("ImmutableSet", Py.java2py(PyImmutableSet.class));
    }
}
