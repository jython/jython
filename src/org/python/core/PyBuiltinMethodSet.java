package org.python.core;

public class PyBuiltinMethodSet extends PyBuiltinFunctionSet implements
        Cloneable {

    public PyBuiltinMethodSet(String name,
                              int index,
                              int minargs,
                              int maxargs,
                              String doc,
                              Class type) {
        super(name, index, minargs, maxargs, doc);
        this.type = type;
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        if(obj != null) {
            if(this.type.isAssignableFrom(obj.getClass())) {
                return bind(obj);
            } else {
                throw Py.TypeError("descriptor '" + info.getName() + "' for '" + PyType.fromClass(this.type)
                        + "' objects doesn't apply to '" + obj.getType() + "' object");
            }
        }
        return this;
    }

    public PyBuiltinFunction bind(PyObject bindTo) {
        if(__self__ == Py.None) {
            PyBuiltinMethodSet bindable;
            try {
                bindable = (PyBuiltinMethodSet)clone();
            } catch(CloneNotSupportedException e) {
                throw new RuntimeException("Didn't expect PyBuiltinMethodSet to throw CloneNotSupported since it implements Cloneable",
                                           e);
            }
            bindable.__self__ = bindTo;
            return bindable;
        }
        return this;
    }

    public PyObject getSelf() {
        return __self__;
    }
    
    public String toString(){
        return "<built-in method "+info.getName()+">";
    }

    private Class type;
    
    protected PyObject __self__ = Py.None;
}
