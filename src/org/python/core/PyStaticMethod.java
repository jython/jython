package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * The staticmethod descriptor.
 */
@ExposedType(name = "staticmethod")
public class PyStaticMethod extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyStaticMethod.class);

    protected PyObject callable;

    public PyStaticMethod(PyObject callable) {
        this.callable = callable;
    }

    @ExposedNew
    final static PyObject staticmethod_new(PyNewWrapper new_, boolean init, PyType subtype,
                                           PyObject[] args, String[] keywords) {
        if (keywords.length != 0) {
            throw Py.TypeError("staticmethod does not accept keyword arguments");
        }
        if (args.length != 1) {
            throw Py.TypeError("staticmethod expected 1 argument, got " + args.length);
        }
        return new PyStaticMethod(args[0]);
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        return staticmethod___get__(obj, type);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.staticmethod___get___doc)
    final PyObject staticmethod___get__(PyObject obj, PyObject type) {
        return callable;
    }
}
