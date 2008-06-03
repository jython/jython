/* Copyright (c) Jython Developers */
package org.python.modules._weakref;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyType;

/**
 * Base class for weakref types.
 */
public abstract class AbstractReference extends PyObject {

    PyObject callback;

    protected GlobalRef gref;

    public AbstractReference(PyType subType, GlobalRef gref, PyObject callback) {
        super(subType);
        this.gref = gref;
        this.callback = callback;
        gref.add(this);
    }

    void call() {
        if (callback == null)
            return;
        try {
            callback.__call__(this);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    protected PyObject py() {
        PyObject o = (PyObject)gref.get();
        if (o == null) {
            throw Py.ReferenceError("weakly-referenced object no longer exists");
        }
        return o;
    }

    public int hashCode() {
        if (gref.realHash) {
            return gref.hash;
        }
        throw Py.TypeError("unhashable instance");
    }

    public PyObject __eq__(PyObject other) {
        if (other.getClass() != getClass()) {
            return null;
        }
        PyObject pythis = (PyObject)gref.get();
        PyObject pyother = (PyObject)((AbstractReference)other).gref.get();
        if (pythis == null || pyother == null) {
            return this == other ? Py.True : Py.False;
        }
        return pythis._eq(pyother);
    }
}
