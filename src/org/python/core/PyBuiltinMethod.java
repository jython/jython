package org.python.core;

import org.python.expose.ExposeAsSuperclass;


public abstract class PyBuiltinMethod extends PyBuiltinFunction implements ExposeAsSuperclass  {

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

    protected PyObject self;
}
