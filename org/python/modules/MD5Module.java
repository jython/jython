// Copyright © Corporation for National Research Initiatives

// This is a JPython module wrapper around Harry Mantakos' md.java class,
// which provides the basic MD5 algorithm.  See also MD5Object.java which
// is the implementation of the md5 object returned by new() and md.java
// which provides the md5 implementation.

package org.python.modules;

import org.python.core.*;



class MD5Functions extends PyBuiltinFunctionSet 
{
    public MD5Functions(String name, int index, int minargs, int maxargs) {
        super(name, index, minargs, maxargs, false, null);
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
        dict.__setitem__("new", new MD5Functions("new", 0, 0, 1));
        dict.__setitem__("md5", new MD5Functions("md5", 0, 0, 1));
        dict.__setitem__("initModule", null);
    }
}
