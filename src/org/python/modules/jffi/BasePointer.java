package org.python.modules.jffi;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedGet;

public abstract class BasePointer extends PyObject implements Pointer {

    BasePointer(PyType subtype) {
        super(subtype);
    }

    @ExposedGet(name = "address")
    public PyObject address() {
        return Py.newInteger(getMemory().getAddress());
    }

    @Override
    public boolean __nonzero__() {
        return !getMemory().isNull();
    }

    @Override
    public PyObject __int__() {
        return address();
    }

    @Override
    public PyObject __long__() {
        return address();
    }
}
