package org.python.core;

public class PyClassMethodDescr extends PyMethodDescr {

    PyClassMethodDescr(PyType t, PyBuiltinFunction meth) {
        super(t, meth);
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        if (obj != null) {
            checkGetterType(obj.getType());
            return meth.bind(obj.getType()); 
        } else if(type != null) {
            checkGetterType((PyType)type);
            return meth.bind(type);
        }
        return this;
    }
}
