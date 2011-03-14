/* Copyright (c) Jython Developers */
package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "property", doc = BuiltinDocs.property_doc)
public class PyProperty extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyProperty.class);

    @ExposedGet(doc = BuiltinDocs.property_fget_doc)
    protected PyObject fget;

    @ExposedGet(doc = BuiltinDocs.property_fset_doc)
    protected PyObject fset;

    @ExposedGet(doc = BuiltinDocs.property_fdel_doc)
    protected PyObject fdel;

    /** Whether this property's __doc__ was copied from its getter. */
    protected boolean docFromGetter;

    @ExposedGet(name = "__doc__")
    protected PyObject doc;

    public PyProperty() {
        this(TYPE);
    }

    public PyProperty(PyType subType) {
        super(subType);
    }

    @ExposedNew
    @ExposedMethod(doc = BuiltinDocs.property___init___doc)
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
            PyObject getDoc = fget.__findattr__("__doc__");
            if (getType() == TYPE) {
                doc = getDoc;
            } else {
                // Put __doc__ in dict of the subclass instance instead, otherwise it gets
                // shadowed by class's __doc__
                __setattr__("__doc__", getDoc);
            }
            docFromGetter = true;
        }
    }

    @Override
    public PyObject __call__(PyObject arg1, PyObject args[], String keywords[]) {
        return fget.__call__(arg1);
    }

    @Override
    public PyObject __get__(PyObject obj, PyObject type) {
        return property___get__(obj,type);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.property___get___doc)
    final PyObject property___get__(PyObject obj, PyObject type) {
        if (obj == null || obj == Py.None) {
            return this;
        }
        if (fget == null) {
            throw Py.AttributeError("unreadable attribute");
        }
        return fget.__call__(obj);
    }

    @Override
    public void __set__(PyObject obj, PyObject value) {
        property___set__(obj, value);
    }

    @ExposedMethod(doc = BuiltinDocs.property___set___doc)
    final void property___set__(PyObject obj, PyObject value) {
        if (fset == null) {
            throw Py.AttributeError("can't set attribute");
        }
        fset.__call__(obj, value);
    }

    @Override
    public void __delete__(PyObject obj) {
        property___delete__(obj);
    }

    @ExposedMethod(doc = BuiltinDocs.property___delete___doc)
    final void property___delete__(PyObject obj) {
        if (fdel == null) {
            throw Py.AttributeError("can't delete attribute");
        }
        fdel.__call__(obj);
    }

    public PyObject getter(PyObject getter) {
        return property_getter(getter);
    }

    @ExposedMethod(doc = BuiltinDocs.property_getter_doc)
    final PyObject property_getter(PyObject getter) {
        return propertyCopy(getter, null, null);
    }

    public PyObject setter(PyObject setter) {
        return property_setter(setter);
    }

    @ExposedMethod(doc = BuiltinDocs.property_setter_doc)
    final PyObject property_setter(PyObject setter) {
        return propertyCopy(null, setter, null);
    }

    public PyObject deleter(PyObject deleter) {
        return property_deleter(deleter);
    }

    @ExposedMethod(doc = BuiltinDocs.property_deleter_doc)
    final PyObject property_deleter(PyObject deleter) {
        return propertyCopy(null, null, deleter);
    }

    /**
     * Return a copy of this property with the optional addition of a get/set/del. Helper
     * method for the getter/setter/deleter methods.
     */
    private PyObject propertyCopy(PyObject get, PyObject set, PyObject del) {
        if (get == null) {
            get = fget != null ? fget : Py.None;
        }
        if (set == null) {
            set = fset != null ? fset : Py.None;
        }
        if (del == null) {
            del = fdel != null ? fdel : Py.None;
        }

        PyObject doc;
        if (docFromGetter) {
            // make _init use __doc__ from getter
            doc = Py.None;
        } else {
            doc = this.doc != null ? this.doc : Py.None;
        }

        return getType().__call__(get, set, del, doc);
    }
}
