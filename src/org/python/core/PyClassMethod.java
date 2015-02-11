package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * The classmethod descriptor.
 */
@ExposedType(name = "classmethod", doc = BuiltinDocs.classmethod_doc)
public class PyClassMethod extends PyObject implements Traverseproc {

    public static final PyType TYPE = PyType.fromClass(PyClassMethod.class);

    @ExposedGet(name = "__func__")
    protected PyObject callable;

    public PyClassMethod(PyObject callable) {
        if (!callable.isCallable()) {
            throw Py.TypeError("'" + callable.getType().fastGetName() + "' object is not callable");
        }
        this.callable = callable;
    }

    @ExposedNew
    final static PyObject classmethod_new(PyNewWrapper new_, boolean init, PyType subtype,
                                          PyObject[] args, String[] keywords) {
        if (keywords.length != 0) {
            throw Py.TypeError("classmethod does not accept keyword arguments");
        }
        if (args.length != 1) {
            throw Py.TypeError("classmethod expected 1 argument, got " + args.length);
        }
        return new PyClassMethod(args[0]);
    }

    public PyObject __get__(PyObject obj) {
        return classmethod___get__(obj, null);
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        return classmethod___get__(obj, type);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.classmethod___get___doc)
    final PyObject classmethod___get__(PyObject obj, PyObject type) {
        if(type == null) {
            type = obj.getType();
        }
        return new PyMethod(callable, type, type.getType());
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return callable != null ? visit.visit(callable, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && ob == callable;
    }
}
