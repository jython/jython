
package org.python.modules.sets;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyObject;

public class Sets implements ClassDictInit {
    private Sets() {}

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("Set", PySet.TYPE);
        dict.__setitem__("ImmutableSet", PyImmutableSet.TYPE);
    }
}
