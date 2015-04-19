package org.python.modules._threading;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyObject;


public class _threading implements ClassDictInit {

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", Py.newString("_threading"));
        dict.__setitem__("Lock", Lock.TYPE);
        dict.__setitem__("RLock", RLock.TYPE);
        dict.__setitem__("_Lock", Lock.TYPE);
        dict.__setitem__("_RLock", RLock.TYPE);
        dict.__setitem__("Condition", Condition.TYPE);

        // Hide from Python
        dict.__setitem__("classDictInit", null);
    }

}
