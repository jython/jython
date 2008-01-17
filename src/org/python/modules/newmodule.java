/* Copyright (c) 2001, 2003 Finn Bock, Samuele Pedroni */
package org.python.modules;

import org.python.core.Py;
import org.python.core.PyClass;
import org.python.core.PyCode;
import org.python.core.PyFunction;
import org.python.core.PyInstance;
import org.python.core.PyMethod;
import org.python.core.PyModule;
import org.python.core.PyObject;
import org.python.core.PyTuple;

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
        return function(code, globals, null, Py.EmptyObjects, null);
    }

    public static PyFunction function(PyCode code, PyObject globals, String name) {
        return function(code, globals, name, Py.EmptyObjects, null);
    }

    public static PyFunction function(PyCode code, PyObject globals, String name,
                                      PyObject[] argdefs) {
        PyFunction f = new PyFunction(globals, argdefs, code, null, null);
        if (name != null) {
            f.__name__ = name;
        }
        return f;
    }

    public static PyFunction function(PyCode code, PyObject globals, String name,
                                      PyObject[] argdefs, PyObject[] closure) {
        PyFunction f = new PyFunction(globals, argdefs, code, null, closure);
        if (name != null) {
            f.__name__ = name;
        }
        return f;
    }

    public static PyModule module(String name) {
        return new PyModule(name, null);
    }

    public static PyClass classobj(String name, PyTuple bases, PyObject dict) {
        return new PyClass(name, bases, dict);
    }
}
