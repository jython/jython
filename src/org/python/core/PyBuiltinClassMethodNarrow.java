package org.python.core;


/**
 * A builtin classmethod with a restricted number of arguments.
 */
public abstract class PyBuiltinClassMethodNarrow extends PyBuiltinMethodNarrow {

    protected PyBuiltinClassMethodNarrow(String name, int minArgs, int maxArgs) {
        super(name, minArgs, maxArgs);
    }

    protected PyBuiltinClassMethodNarrow(PyObject self, Info info) {
        super(self, info);
    }

    protected PyBuiltinClassMethodNarrow(PyType type, PyObject self, Info info) {
        super(type, self, info);
    }
    
    public PyMethodDescr makeDescriptor(PyType t) {
        return new PyClassMethodDescr(t, this);
    }
    
    
}
