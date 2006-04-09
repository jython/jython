// Copyright 2002 Finn Bock

package org.python.core;


public class PyGenerator extends PyIterator {
    public PyFrame gi_frame;
    PyObject closure;
    public boolean gi_running;

    public PyGenerator(PyFrame frame, PyObject closure) {
        this.gi_frame = frame;
        this.closure = closure;
        this.gi_running = false;
    }

    private static final String[] __members__ = {
        "gi_frame", "gi_running", "next",
    };

    public PyObject __dir__() {
        PyString members[] = new PyString[__members__.length];
        for (int i = 0; i < __members__.length; i++)
            members[i] = new PyString(__members__[i]);
        PyList ret = new PyList(members);
        PyDictionary accum = new PyDictionary();
        addKeys(accum, "__dict__");
        ret.extend(accum.keys());
        ret.sort();
        return ret;
    }

    public PyObject __iternext__() {
        if (gi_running)
            throw Py.ValueError("generator already executing");
        if (gi_frame.f_lasti == -1)
            return null;
        gi_running = true;
        PyObject result = null;
        try {
            result = gi_frame.f_code.call(gi_frame, closure);
        } finally {
            gi_running = false;
        }
//        System.out.println("lasti:" + gi_frame.f_lasti);
//if (result == Py.None)
//    new Exception().printStackTrace();
        if (result == Py.None && gi_frame.f_lasti == -1)
            return null;
        return result;
    }
}
