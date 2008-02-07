// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;

// Implementation of the MD5 object as returned from md5.new()

public class MD5Object extends PyObject
{
    private String data;

    public int digest_size = 16;

    public MD5Object(String s) {
        data = s;
    }

    public MD5Object(PyObject arg) {
        this("");
        update(arg);
    }

    public PyObject update(PyObject arg) {
        if (!(arg instanceof PyString))
            throw Py.TypeError("update() argument 1 must be string or read-only buffer, not "
                               + arg.getType().fastGetName());
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
