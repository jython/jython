// Copyright © Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;



class MD5Functions extends PyBuiltinFunctionSet 
{
    public MD5Functions(String name, int index, int minargs, int maxargs) {
        super(name, index, minargs, maxargs);
    }

    public PyObject __call__() {
        switch (index) {
        case 0:
            return new MD5Object("");
        default:
            throw argCountError(0);
        }
    }

    public PyObject __call__(PyObject arg1) {
        switch (index) {
        case 0:
            return new MD5Object(arg1);
        default:
            throw argCountError(1);
        }
    }
}



public class MD5Module implements InitModule 
{
    public void initModule(PyObject dict) {
        boolean gotmodule = false;
        try {
            Class.forName("org.python.modules.md5");
            gotmodule = true;
        }
	catch (ClassNotFoundException e) {}
        if (!gotmodule) {
            throw Py.ImportError("could not import org.python.modules.md5");
        }
        dict.__setitem__("new", new MD5Functions("new", 0, 0, 1));
        dict.__setitem__("md5", new MD5Functions("md5", 0, 0, 1));
    }
}
