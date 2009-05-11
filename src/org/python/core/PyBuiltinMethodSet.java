/* Copyright (c) Jython Developers */
package org.python.core;

public class PyBuiltinMethodSet extends PyBuiltinFunctionSet implements Cloneable {

    private Class<?> type;

    protected PyObject __self__ = Py.None;

    public PyBuiltinMethodSet(String name, int index, int minargs, int maxargs, String doc,
                              Class<?> type) {
        super(name, index, minargs, maxargs, doc);
        this.type = type;
    }

    @Override
    public PyObject __get__(PyObject obj, PyObject type) {
        if (obj != null) {
            if (this.type.isAssignableFrom(obj.getClass())) {
                return bind(obj);
            } else {
                throw Py.TypeError(String.format("descriptor '%s' for '%s' objects doesn't apply "
                                                 + "to '%s' object", info.getName(),
                                                 PyType.fromClass(this.type), obj.getType()));
            }
        }
        return this;
    }

    @Override
    public PyBuiltinCallable bind(PyObject bindTo) {
        if (__self__ != Py.None) {
            return this;
        }
        PyBuiltinMethodSet bindable;
        try {
            bindable = (PyBuiltinMethodSet)clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Didn't expect PyBuiltinMethodSet to throw "
                                       + "CloneNotSupported since it implements Cloneable", e);
        }
        bindable.__self__ = bindTo;
        return bindable;
    }

    @Override
    public PyObject getSelf() {
        return __self__;
    }

    @Override
    public String toString() {
        return String.format("<built-in method %s>", info.getName());
    }
}
