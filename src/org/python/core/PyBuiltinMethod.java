package org.python.core;


public abstract class PyBuiltinMethod extends PyBuiltinFunction {

    public static final Class exposed_as = PyBuiltinFunction.class;

    protected PyBuiltinMethod(PyObject self, Info info) {
        super(info);
        this.self = self;
    }
    
    public PyObject getSelf(){
        return self;
    }

    protected PyObject self;
}
