/* Copyright (c) 2001, 2003 Finn Bock, Samuele Pedroni */
package org.python.modules;

import org.python.core.Py;
import org.python.core.PyCell;
import org.python.core.PyClass;
import org.python.core.PyCode;
import org.python.core.PyFunction;
import org.python.core.PyInstance;
import org.python.core.PyMethod;
import org.python.core.PyModule;
import org.python.core.PyObject;
import org.python.core.PyTableCode;
import org.python.core.PyTuple;
import org.python.core.PyType;

/**
 * The new module.
 *
 * An interface to the interpreter object creation functions.
 *
 */
public class newmodule {

    public static PyInstance instance(PyClass cls) {
        return new PyInstance(cls);
    }

    public static PyInstance instance(PyClass cls, PyObject dict) {
        if (dict == Py.None) {
            return new PyInstance(cls);
        } else {
            return new PyInstance(cls, dict);
        }
    }

    public static PyMethod instancemethod(PyObject func, PyObject instance, PyObject cls) {
        return new PyMethod(instance, func, cls);
    }

    public static PyFunction function(PyCode code, PyObject globals) {
        return function(code, globals, null, Py.EmptyObjects, Py.None);
    }

    public static PyFunction function(PyCode code, PyObject globals, String name) {
        return function(code, globals, name, Py.EmptyObjects, Py.None);
    }

    public static PyFunction function(PyCode code, PyObject globals, String name,
                                      PyObject[] defaults) {
        return function(code, globals, name, defaults, Py.None);
    }

    public static PyFunction function(PyCode code, PyObject globals, String name,
                                      PyObject[] defaults, PyObject closure) {
        PyTableCode tcode = (PyTableCode)code;
        int nfree = tcode.co_freevars == null ? 0 : tcode.co_freevars.length;

        if (!(closure instanceof PyTuple)) {
            if (nfree > 0 && closure == Py.None) {
                throw Py.TypeError("arg 5 (closure) must be tuple");
            } else if (closure != Py.None) {
                throw Py.TypeError("arg 5 (closure) must be None or tuple");
            }
        }

        int nclosure = closure == Py.None ? 0 : closure.__len__();
        if (nfree != nclosure) {
            throw Py.ValueError(String.format("%s requires closure of length %d, not %d",
                                              tcode.co_name, nfree, nclosure));
        }
        if (nclosure > 0) {
            for (PyObject o : ((PyTuple)closure).asIterable()) {
                if (!(o instanceof PyCell)) {
                    throw Py.TypeError(String.format("arg 5 (closure) expected cell, found %s",
                                                     o.getType().fastGetName()));
                }
            }
        }
        
        PyFunction f = new PyFunction(globals, defaults, code, null,
                                      closure == Py.None ? null : ((PyTuple)closure).getArray());
        if (name != null) {
            f.__name__ = name;
        }
        return f;
    }

    public static PyModule module(String name) {
        return new PyModule(name, null);
    }

    public static PyObject classobj(String name, PyTuple bases, PyObject dict) {
        for (int i = 0; i < bases.size(); i++) {
            if (bases.pyget(i) instanceof PyType) {
                return bases.pyget(i).__call__(Py.java2py(name), bases, dict);
            }
        }
        return new PyClass(name, bases, dict);
    }
}
