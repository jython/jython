package org.python.core;

import org.python.expose.ExposeAsSuperclass;


public abstract class PyBuiltinMethod extends PyBuiltinFunction implements ExposeAsSuperclass  {

    protected PyBuiltinMethod(PyType type, PyObject self, Info info) {
        super(type, info);
        this.self = self;
    }
    protected PyBuiltinMethod(PyObject self, Info info) {
        super(info);
        this.self = self;
    }
    
    protected PyBuiltinMethod(String name) {
        this(null, new DefaultInfo(name));
    }
    
    public PyObject getSelf(){
        return self;
    }
    
    public PyMethodDescr makeDescriptor(PyType t) {
        return new PyMethodDescr(t, this);
    }

    protected PyObject self;
}
