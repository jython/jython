// Copyright (c) Corporation for National Research Initiatives

// This is a JPython module wrapper around Harry Mantakos' md.java class,
// which provides the basic MD5 algorithm.  See also MD5Object.java which
// is the implementation of the md5 object returned by new() and md.java
// which provides the md5 implementation.

package org.python.modules;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyBuiltinFunctionSet;
import org.python.core.PyObject;
import org.python.core.PyString;

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

public class MD5Module implements ClassDictInit
{
    public static PyString __doc__ = new PyString(
        "This module implements the interface to RSA's MD5 message digest\n"+
        "algorithm (see also Internet RFC 1321). Its use is quite\n"+
        "straightforward: use the new() to create an md5 object. "+
            "You can now\n"+
        "feed this object with arbitrary strings using the update() method, "+
            "and\n"+
        "at any point you can ask it for the digest (a strong kind of "+
            "128-bit\n"+
        "checksum, a.k.a. ``fingerprint'') of the concatenation of the "+
            "strings\n"+
        "fed to it so far using the digest() method.\n"+
        "\n"+
        "Functions:\n"+
        "\n"+
        "new([arg]) -- return a new md5 object, initialized with arg if "+
            "provided\n"+
        "md5([arg]) -- DEPRECATED, same as new, but for compatibility\n"+
        "\n"+
        "Special Objects:\n"+
        "\n"+
        "MD5Type -- type object for md5 objects\n"
    );

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("new", new MD5Functions("new", 0, 0, 1));
        dict.__setitem__("md5", new MD5Functions("md5", 0, 0, 1));
        dict.__setitem__("digest_size", Py.newInteger(16));
        dict.__setitem__("classDictInit", null);
    }
}
