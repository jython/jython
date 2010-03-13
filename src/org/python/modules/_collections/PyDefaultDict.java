/* Copyright (c) Jython Developers */
package org.python.modules._collections;

import java.util.Map;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

/**
 * PyDefaultDict - This is a subclass of the builtin dict(PyDictionary) class. It supports
 * one additional method __missing__ and adds one writable instance variable
 * defaultFactory. The remaining functionality is the same as for the dict class.
 *
 * collections.defaultdict([defaultFactory[, ...]]) - returns a new dictionary-like
 * object. The first argument provides the initial value for the defaultFactory attribute;
 * it defaults to None.  All remaining arguments are treated the same as if they were
 * passed to the dict constructor, including keyword arguments.
 */
@ExposedType(name = "collections.defaultdict")
public class PyDefaultDict extends PyDictionary {

    public static final PyType TYPE = PyType.fromClass(PyDefaultDict.class);

    /**
     * This attribute is used by the __missing__ method; it is initialized from the first
     * argument to the constructor, if present, or to None, if absent.
     */
    private PyObject defaultFactory = Py.None;

    public PyDefaultDict() {
        this(TYPE);
    }

    public PyDefaultDict(PyType subtype) {
        super(subtype);
    }

    public PyDefaultDict(PyType subtype, Map<PyObject, PyObject> map) {
        super(subtype, map);
    }

    @ExposedMethod
    @ExposedNew
    final void defaultdict___init__(PyObject[] args, String[] kwds) {
        int nargs = args.length - kwds.length;
        if (nargs != 0) {
            defaultFactory = args[0];
            if (!defaultFactory.isCallable()) {
                throw Py.TypeError("first argument must be callable");
            }
            PyObject newargs[] = new PyObject[args.length - 1];
            System.arraycopy(args, 1, newargs, 0, newargs.length);
            dict___init__(newargs , kwds);
        }
    }

    @Override
    public PyObject __finditem__(PyObject key) {
        return dict___getitem__(key);
    }

    /**
     * This method is called by the __getitem__ method of the dict class when the
     * requested key is not found; whatever it returns or raises is then returned or
     * raised by __getitem__.
     */
    @ExposedMethod
    final PyObject defaultdict___missing__(PyObject key) {
        if (defaultFactory == Py.None) {
            throw Py.KeyError(key);
        }
        PyObject value = defaultFactory.__call__();
        if (value == null) {
            return value;
        }
        __setitem__(key, value);
        return value;
    }

    @Override
    public PyObject __reduce__() {
        return defaultdict___reduce__();
    }

    @ExposedMethod
    final PyObject defaultdict___reduce__() {
        PyTuple args = null;
        if (defaultFactory == Py.None) {
            args = new PyTuple();
        } else {
            PyObject[] ob = {defaultFactory};
            args = new PyTuple(ob);
        }
        return new PyTuple(getType(), args, Py.None, Py.None, iteritems());
    }

    @Override
    public PyDictionary copy() {
        return defaultdict_copy();
    }

    @ExposedMethod(names = {"copy", "__copy__"})
    final PyDefaultDict defaultdict_copy() {
        PyDefaultDict ob = new PyDefaultDict(TYPE, table);
        ob.defaultFactory = defaultFactory;
        return ob;
    }

    @Override
    public String toString() {
        return defaultdict_toString();
    }

    @ExposedMethod(names = "__repr__")
    final String defaultdict_toString() {
        return String.format("defaultdict(%s, %s)", defaultFactory, super.toString());
    }

    @ExposedGet(name = "default_factory")
    public PyObject getDefaultFactory() {
        return defaultFactory;
    }

    @ExposedSet(name = "default_factory")
    public void setDefaultFactory(PyObject value) {
        defaultFactory = value;
    }

    @ExposedDelete(name = "default_factory")
    public void delDefaultFactory() {
        defaultFactory = Py.None;
    }
}
