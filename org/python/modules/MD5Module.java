// Copyright © Corporation for National Research Initiatives

// This is a JPython module wrapper around Harry Mantakos' md.java class,
// which provides the basic MD5 algorithm.  See also MD5Object.java which
// is the implementation of the md5 object returned by new().

// In order to use this, do the following:
//
// Download Harry's source code from
// <http://www.cs.umd.edu/~harry/jotp/src.html>.  Hopefully, I'll
// eventually be able to redistribute this by default with JPython.
//
// Drop md.java into org/python/modules (or otherwise put it on your
// CLASSPATH) and edit the top of the file to include
// "package org.python.modules;"
//
// Compile md.java
//
// That's it.  As long as JPython can find the class org.python.modules.md5 
// you can, from JPython do something like the following:
//
// >>> import md5
// >>> n = md5.new("hello")
// >>> n.digest()
// ']A@*\274K*v\271q\235\221\020\027\305\222'

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
