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
            Py.writeUnraisable(exc, callback);
        }
    }

    protected PyObject py() {
        PyObject o = (PyObject)gref.get();
        if (o == null) {
            throw Py.ReferenceError("weakly-referenced object no longer exists");
        }
        return o;
    }

    // Differentiate reference equality (equals) with what is being referred to (__eq__)
    @Override
    public boolean equals(Object ob_other) {
        return ob_other == this;
    }

    public int hashCode() {
        return gref.pythonHashCode();
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

    public PyObject __ne__(PyObject other) {
        if (other.getClass() != getClass()) {
            return Py.True;
        }
        PyObject pythis = (PyObject)gref.get();
        PyObject pyother = (PyObject)((AbstractReference)other).gref.get();
        if (pythis == null || pyother == null) {
            return this == other ? Py.False : Py.True;
        }
        return pythis._eq(pyother).__not__();
    }
}
