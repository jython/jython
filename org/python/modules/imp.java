
package org.python.modules;

import java.io.*;
import org.python.core.*;

/*
 * A bogus implementation of the CPython builtin module "imp".
 * Onle the functions required by IDLE and PMW are implemented.
 * Luckily these function are also the only function that IMO can
 * be implemented under JPython.
 */


public class imp {
    public static PyString __doc__ = new PyString(
        "This module provides the components needed to build your own\n"+
        "__import__ function.  Undocumented functions are obsolete.\n"
    );

    public static final int PY_SOURCE = 1;
    public static final int PKG_DIRECTORY = 5;


    public static PyObject find_module(String name) {
        return find_module(name, null);
    }


    public static PyObject find_module(String name, PyObject path) {
        if (path == null || path == Py.None)
            path = Py.getSystemState().__getattr__("path");
        PyObject p = null;
        for (int i = 0; (p = path.__finditem__(i)) != null; i++) {
            File fullpath = new File(p.__str__().toString(), name + ".py");
            if (fullpath.isFile()) {
                String filename = fullpath.getPath();
                return new PyTuple(new PyObject[] {
                    new PyFile(filename, "r", -1),
                    new PyString(filename),
                    new PyTuple(new PyObject[] {
                       new PyString(".py"),
                       new PyString("r"),
                       Py.newInteger(PY_SOURCE),
                    }),
                });
             }
         }
         throw Py.ImportError("No module named " + name);
    }


    public static PyObject get_suffixes() {
        return new PyList(new PyObject[] {
            new PyTuple(new PyObject[] {
                new PyString(".py"),
                new PyString("r"),
                Py.newInteger(PY_SOURCE),
            }),
        });
    }



    public static PyModule new_module(String name) {
        return new PyModule(name, null);
    }

}
