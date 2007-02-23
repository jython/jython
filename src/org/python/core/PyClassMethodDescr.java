package org.python.core;

public class PyClassMethodDescr extends PyMethodDescr {

    public PyClassMethodDescr(String name,
                              Class c,
                              int minargs,
                              int maxargs,
                              PyBuiltinFunction func) {
        super(name, c, minargs, maxargs, func);
    }

    protected void checkCallerType(PyObject obj) {
        if((PyType)obj != dtype && !((PyType)obj).isSubType(dtype))
            throw get_wrongtype((PyType)obj);
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        if (obj != null) {
            checkCallerType(obj.getType());
            return func.makeBound(obj.getType()); 
        }else if(type != null){
            checkCallerType(type);
            return func.makeBound(type);
        }
        return this;
    }
}
