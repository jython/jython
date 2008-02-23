package org.python.core;

public class PyClassMethodDescr extends PyMethodDescr {

    PyClassMethodDescr(PyType t, PyBuiltinFunction meth) {
        super(t, meth);
    }

    protected void checkCallerType(PyObject obj) {
        if((PyType)obj != dtype && !((PyType)obj).isSubType(dtype))
            throw get_wrongtype((PyType)obj);
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        if (obj != null) {
            checkCallerType(obj.getType());
            return meth.bind(obj.getType()); 
        }else if(type != null){
            checkCallerType(type);
            return meth.bind(type);
        }
        return this;
    }
}
