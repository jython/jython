/* Copyright (c) 2001, 2003 Finn Bock, Samuele Pedroni */
package org.python.modules;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyClass;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;

/**
 * The internal new module; just provides a hack for new.classobj.
 *
 */
public class _newmodule  implements ClassDictInit {

    public static void classDictInit(PyObject dict)
    {
        dict.__setitem__("__name__", Py.newString("_new"));
    }

    public static PyObject classobj(String name, PyTuple bases, PyObject dict) {
        // XXX: Hack to return new style classes (originally from
        // r4225). types.ClassType (PyClass) should be doing this
        // instead, but it needs to be new style so it can return new
        // style classes via __new__. When that happens we can use the
        // pure python new.py completely
        // XXX: This workaround can't be done in new.py (pure python)
        // because the caller's stack frame would be wrong (which is
        // used to determine the new class's __module__)
        for (PyObject base : bases.getArray()) {
            if (base instanceof PyType) {
                return base.getType().__call__(Py.newString(name), bases, dict);
            }
        }
        return new PyClass(name, bases, dict);
    }
}
