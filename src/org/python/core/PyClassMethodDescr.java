package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "classmethod_descriptor")
public class PyClassMethodDescr extends PyMethodDescr {

    public static final PyType TYPE = PyType.fromClass(PyClassMethodDescr.class);

    PyClassMethodDescr(PyType t, PyBuiltinFunction meth) {
        super(t, meth);
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        return classmethod_descriptor___get__(obj, type);
    }

    @ExposedMethod(defaults = "null")
    public PyObject classmethod_descriptor___get__(PyObject obj, PyObject type) {
        if (type == null || type == Py.None) {
            if (obj != null) {
                type = obj.getType();
            } else {
                throw Py.TypeError(String.format("descriptor '%s' for type '%s' needs either an "
                                                 + " object or a type", name, dtype.fastGetName()));
            }
        } else if (!(type instanceof PyType)) {
            throw Py.TypeError(String.format("descriptor '%s' for type '%s' needs a type, not a"
                                             + " '%s' as arg 2", name, dtype.fastGetName(),
                                             type.getType().fastGetName()));
        }
        checkGetterType((PyType)type);
        return meth.bind(type);
    }
}
