// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;

// Implementation of the MD5 object as returned from md5.new()

public class MD5Object extends PyObject
{
    private String data;

    public MD5Object(String s) {
        data = s;
    }

    public MD5Object(PyObject arg) {
        this("");
        update(arg);
    }

    public PyObject update(PyObject arg) {
        if (!(arg instanceof PyString))
            // TBD: this should be able to call safeRepr() on the arg, but
            // I can't currently do this because safeRepr is protected so
            // that it's not accessible from Python.  This is bogus;
            // arbitrary Java code should be able to get safeRepr but we
            // still want to hide it from Python.  There should be another
            // way to hide Java methods from Python.
            throw Py.TypeError("argument 1 expected string");
        data += arg.toString();
        return Py.None;
    }

    public PyObject digest() {
        md md5obj = md.new_md5(data);
        md5obj.calc();
        // this is for compatibility with CPython's output
        String s = md5obj.toString();
        char[] x = new char[s.length() / 2];

        for (int i=0, j=0; i < s.length(); i+=2, j++) {
            String chr = s.substring(i, i+2);
            x[j] = (char)java.lang.Integer.parseInt(chr, 16);
        }
        return new PyString(new String(x));
    }

    public PyObject hexdigest() {
        md md5obj = md.new_md5(data);
        md5obj.calc();
        // this is for compatibility with CPython's output
        return new PyString(md5obj.toString());
    }

    public PyObject copy() {
        return new MD5Object(data);
    }
}
