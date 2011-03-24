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
@ExposedType(name = "exceptions.BaseException", doc = BuiltinDocs.BaseException_doc)
public class PyBaseException extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyBaseException.class);

    /** Exception message. */
    private PyObject message = Py.EmptyString;

    /** Exception's arguments. */
    @ExposedGet(doc = BuiltinDocs.BaseException_args_doc)
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
    @ExposedMethod(doc = BuiltinDocs.BaseException___init___doc)
    final void BaseException___init__(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser(getType().getName(), args, keywords, "args");
        ap.noKeywords();
        this.args = ap.getList(0);
        if (args.length == 1) {
            message = args[0];
        }
    }

    @Override
    public PyObject __getitem__(PyObject index) {
        return BaseException___getitem__(index);
    }

    @ExposedMethod(doc = BuiltinDocs.BaseException___getitem___doc)
    final PyObject BaseException___getitem__(PyObject index) {
        Py.warnPy3k("__getitem__ not supported for exception classes in 3.x; use args "
                    + "attribute");
        return args.__getitem__(index);
    }

    @Override
    public PyObject __getslice__(PyObject start, PyObject stop) {
        return BaseException___getslice__(start, stop);
    }

    @ExposedMethod(doc = BuiltinDocs.BaseException___getslice___doc)
    final PyObject BaseException___getslice__(PyObject start, PyObject stop) {
        Py.warnPy3k("__getslice__ not supported for exception classes in 3.x; use args "
                    + "attribute");
        return args.__getslice__(start, stop);
    }


    @Override
    public PyObject __reduce__() {
        return BaseException___reduce__();
    }

    @ExposedMethod(doc = BuiltinDocs.BaseException___reduce___doc)
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

    @ExposedMethod(doc = BuiltinDocs.BaseException___setstate___doc)
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

    @Override
    public PyObject __findattr_ex__(String name) {
        return BaseException___findattr__(name);
    }

    final PyObject BaseException___findattr__(String name) {
        if (__dict__ != null) {
            PyObject attr = __dict__.__finditem__(name);
            if (attr != null) {
                return attr;
            }
        }

        return super.__findattr_ex__(name);
    }

    @Override
    public void __setattr__(String name, PyObject value) {
        BaseException___setattr__(name, value);
    }

    @ExposedMethod(doc = BuiltinDocs.BaseException___setattr___doc)
    final void BaseException___setattr__(String name, PyObject value) {
        ensureDict();
        super.__setattr__(name, value);
    }

    @Override
    public PyObject fastGetDict() {
        return __dict__;
    }
    
    @Override
    @ExposedGet(name = "__dict__", doc = BuiltinDocs.BaseException___dict___doc)
    public PyObject getDict() {
        ensureDict();
        return __dict__;
    }

    @Override
    @ExposedSet(name = "__dict__")
    public void setDict(PyObject val) {
        if (!(val instanceof PyStringMap) && !(val instanceof PyDictionary)) {
            throw Py.TypeError("__dict__ must be a dictionary");
        }
        __dict__ = val;
    }

    private void ensureDict() {
        // XXX: __dict__ should really be volatile
        if (__dict__ == null) {
            __dict__ = new PyStringMap();
        }
    }

    @Override
   public PyString __str__() {
        return BaseException___str__();
    }

    @ExposedMethod(doc = BuiltinDocs.BaseException___str___doc)
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

    @Override
    public PyUnicode __unicode__() {
        return BaseException___unicode__();
    }

    @ExposedMethod(doc = BuiltinDocs.BaseException___unicode___doc)
    final PyUnicode BaseException___unicode__() {
        // CPython issue6108: if __str__ has been overridden in the subclass, unicode()
        // should return the message returned by __str__ as used to happen before this
        // method was implemented
        PyType type = getType();
        PyObject[] where = new PyObject[1];
        PyObject str = type.lookup_where("__str__", where);
        if (str != null && where[0] != TYPE) {
            // Unlike str(), __str__ can return unicode (i.e. return the equivalent
            // of unicode(e.__str__()) instead of unicode(str(e)))
            return str.__get__(this, type).__call__().__unicode__();
        }
        
        switch (args.__len__()) {
        case 0:
            return new PyUnicode("");
        case 1:
            return args.__getitem__(0).__unicode__();
        default:
            return args.__unicode__();
        }
    }

    @Override
    public String toString() {
        return BaseException_toString();
    }

    @ExposedMethod(names = "__repr__", doc = BuiltinDocs.BaseException___repr___doc)
    final String BaseException_toString() {
        PyObject reprSuffix = args.__repr__();
        String name = getType().fastGetName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot != -1) {
            name = name.substring(lastDot + 1);
        }
        return name + reprSuffix.toString();
    }

    @ExposedSet(name = "args")
    public void setArgs(PyObject val) {
        args = PyTuple.fromIterable(val);
    }

    @ExposedGet(name = "message", doc = BuiltinDocs.BaseException_message_doc)
    public PyObject getMessage() {
        PyObject message;

        // if "message" is in self->dict, accessing a user-set message attribute
        if (__dict__ != null && (message = __dict__.__finditem__("message")) != null) {
            return message;
        }

        if (this.message == null) {
            throw Py.AttributeError("message attribute was deleted");
        }

        Py.DeprecationWarning("BaseException.message has been deprecated as of Python 2.6");
        return this.message;
    }

    @ExposedSet(name = "message")
    public void setMessage(PyObject value) {
        getDict().__setitem__("message", value);
    }

    @ExposedDelete(name = "message")
    public void delMessage() {
        if (__dict__ != null && (message = __dict__.__finditem__("message")) != null) {
            __dict__.__delitem__("message");
        }
        message = null;
    }
}
