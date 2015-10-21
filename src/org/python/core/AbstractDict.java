package org.python.core;

import java.util.concurrent.ConcurrentMap;
import java.util.Collection;

public abstract class AbstractDict extends PyObject {

    public AbstractDict(PyType type) {
        super(type);
    }

    public abstract void clear();
    public abstract AbstractDict copy();
    public abstract PyObject get(PyObject key);
    public abstract PyObject get(PyObject key, PyObject defaultObj);
    public abstract ConcurrentMap<? extends Object, PyObject> getMap();
    public abstract boolean has_key(PyObject key);
    public abstract PyList items();
    public abstract PyObject iteritems();
    public abstract PyObject iterkeys();
    public abstract PyObject itervalues();
    public abstract PyList keys();
    public abstract void merge(PyObject other, boolean override);
    public abstract void mergeFromKeys(PyObject other, PyObject keys, boolean override);
    public abstract void mergeFromSeq(PyObject other, boolean override);
    public abstract PyObject pop(PyObject key);
    public abstract PyObject pop(PyObject key, PyObject defaultValue);
    public abstract PyObject popitem();
    public abstract PyObject setdefault(PyObject key);
    public abstract PyObject setdefault(PyObject key, PyObject failobj);
    public abstract void update(PyObject other);
    public abstract Collection<? extends Object> values();
}
