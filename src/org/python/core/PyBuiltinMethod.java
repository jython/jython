package org.python.core;


public abstract class PyBuiltinMethod extends PyBuiltinFunction {

    public static final Class exposed_as = PyBuiltinFunction.class;

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
