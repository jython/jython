/* Copyright (c) 2008 Jython Developers */
package org.python.core;

import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

/**
 * The base class for all standard Python exceptions.
 *
 */
@ExposedType(name = "exceptions.BaseException")
public class PyBaseException extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyBaseException.class);

    /** Exception message. */
    @ExposedGet
    @ExposedSet
    public PyObject message = Py.EmptyString;

    /** Exception's arguments. */
    @ExposedGet
    public PyObject args = Py.EmptyTuple;

    /** Exception's underlying dictionary, lazily created. */
    public PyObject __dict__;

    public PyBaseException() {
        super();
    }

    public PyBaseException(PyType subType) {
        super(subType);
    }

    public void __init__(PyObject[] args, String[] keywords) {
        BaseException___init__(args, keywords);
    }

    @ExposedNew
    @ExposedMethod
    final void BaseException___init__(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser(getType().getName(), args, keywords, "args");
        ap.noKeywords();
        this.args = ap.getList(0);
        if (args.length == 1) {
            message = args[0];
        }
    }

    public PyObject __getitem__(PyObject index) {
        return BaseException___getitem__(index);
    }

    @ExposedMethod
    final PyObject BaseException___getitem__(PyObject index) {
        return args.__getitem__(index);
    }

    public PyObject __getslice__(PyObject start, PyObject stop) {
        return BaseException___getslice__(start, stop);
    }

    @ExposedMethod
    final PyObject BaseException___getslice__(PyObject start, PyObject stop) {
        return args.__getslice__(start, stop);
    }


    public PyObject __reduce__() {
        return BaseException___reduce__();
    }

    @ExposedMethod
    final PyObject BaseException___reduce__() {
        if (__dict__ != null) {
            return new PyTuple(getType(), args, __dict__);
        } else {
            return new PyTuple(getType(), args);
        }
    }

    public PyObject __setstate__(PyObject state) {
        return BaseException___setstate__(state);
    }

    @ExposedMethod
    final PyObject BaseException___setstate__(PyObject state) {
        if (state != Py.None) {
            if (!(state instanceof PyStringMap) && !(state instanceof PyDictionary)) {
                throw Py.TypeError("state is not a dictionary");
            }
            for (PyObject key : state.asIterable()) {
                __setattr__((PyString)key, state.__finditem__(key));
            }
        }
        return Py.None;
    }

    public PyObject __findattr__(String name) {
        return BaseException___findattr__(name);
    }

    final PyObject BaseException___findattr__(String name) {
        if (__dict__ != null) {
            PyObject attr = __dict__.__finditem__(name);
            if (attr != null) {
                return attr;
            }
        }

        return super.__findattr__(name);
    }

    public void __setattr__(String name, PyObject value) {
        BaseException___setattr__(name, value);
    }

    @ExposedMethod
    final void BaseException___setattr__(String name, PyObject value) {
        ensureDict();
        super.__setattr__(name, value);
    }

    public PyObject fastGetDict() {
        return __dict__;
    }
    
    @ExposedGet(name = "__dict__")
    public PyObject getDict() {
        ensureDict();
        return __dict__;
    }

    @ExposedSet(name = "__dict__")
    public void setDict(PyObject val) {
        if (!(val instanceof PyStringMap) && !(val instanceof PyDictionary)) {
            throw Py.TypeError("__dict__ must be a dictionary");
        }
        __dict__ = val;
    }

    private void ensureDict() {
        if (__dict__ == null) {
            __dict__ = new PyStringMap();
        }
    }

    public PyString __str__() {
        return BaseException___str__();
    }

    @ExposedMethod
    final PyString BaseException___str__() {
        switch (args.__len__()) {
        case 0:
            return Py.EmptyString;
        case 1:
            return args.__getitem__(0).__str__();
        default:
            return args.__str__();
        }
    }

    public String toString() {
        return __repr__().toString();
    }
        
    public PyString __repr__() {
        return BaseException___repr__();
    }

    @ExposedMethod
    final PyString BaseException___repr__() {
        PyObject reprSuffix = args.__repr__();
        String name = getType().fastGetName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot != -1) {
            name = name.substring(lastDot + 1);
        }
        return Py.newString(name + reprSuffix.toString());
    }

    @ExposedSet(name = "args")
    public void setArgs(PyObject val) {
        args = PyTuple.fromIterable(val);
    }

    @ExposedDelete(name = "message")
    public void delMessage() {
        message = Py.None;
    }
}
