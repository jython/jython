package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

@ExposedType(name = "property")
public class PyProperty extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyProperty.class);

    @ExposedGet
    protected PyObject fget;

    @ExposedGet
    protected PyObject fset;

    @ExposedGet
    protected PyObject fdel;

    @ExposedGet(name = "__doc__")
    protected PyObject doc;

    public PyProperty() {
        this(TYPE);
    }

    public PyProperty(PyType subType) {
        super(subType);
    }

    @ExposedNew
    @ExposedMethod
    public void property___init__(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("property", args, keywords,
                                     new String[] {"fget", "fset", "fdel", "doc"}, 0);
        fget = ap.getPyObject(0, null);
        fget = fget == Py.None ? null : fget;
        fset = ap.getPyObject(1, null);
        fset = fset == Py.None ? null : fset;
        fdel = ap.getPyObject(2, null);
        fdel = fdel == Py.None ? null : fdel;
        doc = ap.getPyObject(3, null);

        // if no docstring given and the getter has one, use fget's
        if ((doc == null || doc == Py.None) && fget != null) {
            doc = fget.__findattr__("__doc__");
        }
    }

    public PyObject __call__(PyObject arg1, PyObject args[], String keywords[]) {
        return fget.__call__(arg1);
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        return property___get__(obj,type);
    }

    @ExposedMethod(defaults = "null")
    final PyObject property___get__(PyObject obj, PyObject type) {
        if (obj == null || obj == Py.None) {
            return this;
        }
        if (fget == null) {
            throw Py.AttributeError("unreadable attribute");
        }
        return fget.__call__(obj);
    }

    public void __set__(PyObject obj, PyObject value) {
        property___set__(obj, value);
    }

    @ExposedMethod
    final void property___set__(PyObject obj, PyObject value) {
        if (fset == null) {
            throw Py.AttributeError("can't set attribute");
        }
        fset.__call__(obj, value);
    }

    public void __delete__(PyObject obj) {
        property___delete__(obj);
    }

    @ExposedMethod
    final void property___delete__(PyObject obj) {
        if (fdel == null) {
            throw Py.AttributeError("can't delete attribute");
        }
        fdel.__call__(obj);
    }
}
