// Copyright (c) Corporation for National Research Initiatives
package org.python.core;


/**
 * A python slice object.
 */

public class PySlice extends PyObject {
    public PyObject start, stop, step;

    public PySlice(PyObject start, PyObject stop, PyObject step) {
        if (start == null) start = Py.None;
        if (stop == null) stop = Py.None;
        if (step == null) step = Py.One;

        this.start = start;
        this.stop = stop;
        this.step = step;
    }

    public PyString __str__() {
        return new PyString(start.__repr__()+":"+stop.__repr__()+":"+
                            step.__repr__());
    }

    public PyString __repr__() {
        return new PyString("slice("+start.__repr__()+", "+
                            stop.__repr__()+", "+
                            step.__repr__()+")");
    }
}
