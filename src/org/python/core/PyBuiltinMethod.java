package org.python.core;

import org.python.expose.ExposeAsSuperclass;

public abstract class PyBuiltinMethod extends PyBuiltinCallable implements ExposeAsSuperclass,
        Cloneable {

    protected PyObject self;

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

    @Override
    public PyBuiltinCallable bind(PyObject bindTo) {
        if(self == null) {
            PyBuiltinMethod bindable;
            try {
                bindable = (PyBuiltinMethod)clone();
            } catch(CloneNotSupportedException e) {
                throw new RuntimeException("Didn't expect PyBuiltinMethodto throw " +
                                           "CloneNotSupported since it implements Cloneable", e);
            }
            bindable.self = bindTo;
            return bindable;
        }
        return this;
    }

    public PyObject getSelf(){
        return self;
    }

    public PyMethodDescr makeDescriptor(PyType t) {
        return new PyMethodDescr(t, this);
    }

    @Override
    public int hashCode() {
        int hashCode = self == null ? 0 : self.hashCode();
        return hashCode ^ getClass().hashCode();
    }

    @Override
    public int __cmp__(PyObject other) {
        if (!(other instanceof PyBuiltinMethod)) {
            return -2;
        }
        PyBuiltinMethod otherMethod = (PyBuiltinMethod)other;
        if (self != otherMethod.self) {
            if (self == null) {
                return -1;
            } else if (otherMethod.self == null) {
                return 1;
            }
            return self._cmp(otherMethod.self);
        }
        if (getClass() == otherMethod.getClass()) {
            return 0;
        }
        int compareTo = info.getName().compareTo(otherMethod.info.getName());
        return compareTo < 0 ? -1 : compareTo > 0 ? 1 : 0;
    }
}
