/* Copyright (c) Jython Developers */
package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * The Python super type.
 */
@ExposedType(name = "super")
public class PySuper extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PySuper.class);

    @ExposedGet(name = "__thisclass__")
    protected PyType superType;

    @ExposedGet(name = "__self__")
    protected PyObject obj;

    @ExposedGet(name = "__self_class__")
    protected PyType objType;

    public PySuper() {
        this(TYPE);
    }

    public PySuper(PyType subType) {
        super(subType);
    }

    @ExposedMethod
    @ExposedNew
    public void super___init__(PyObject[] args, String[] keywords) {
        if (keywords.length != 0 || !PyBuiltinFunction.DefaultInfo.check(args.length, 1, 2)) {
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(args.length, keywords.length != 0,
                                                               "super", 1, 2);
        }
        if (!(args[0] instanceof PyType)) {
            throw Py.TypeError("super: argument 1 must be type");
        }
        PyType type = (PyType)args[0];
        PyObject obj = null;
        PyType objType = null;
        if (args.length == 2 && args[1] != Py.None) {
            obj = args[1];
        }
        if (obj != null) {
            objType = supercheck(type, obj);
        }
        this.superType = type;
        this.obj = obj;
        this.objType = objType;
    }

    private PyType supercheck(PyType type, PyObject obj) {
        if (obj instanceof PyType && ((PyType)obj).isSubType(type)) {
            return (PyType)obj;
        }
        PyType objType = obj.getType();
        if (objType.isSubType(type)) {
            return objType;
        }
        throw Py.TypeError("super(type, obj): obj must be an instance or subtype of type");
    }

    public PyObject __findattr__(String name) {
        return super___findattr__(name);
    }

    final PyObject super___findattr__(String name) {
        if (objType != null && name != "__class__") {
            PyObject descr = objType.super_lookup(superType, name);
            if (descr != null) {
                return descr.__get__(objType == obj ? null : obj, objType);
            }
        }
        return super.__findattr__(name);
    }

    @ExposedMethod
    final PyObject super___getattribute__(PyObject name) {
        PyObject ret = super___findattr__(asName(name));
        if (ret == null) {
            noAttributeError(asName(name));
        }
        return ret;
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        return super___get__(obj, type);
    }

    @ExposedMethod(defaults = "null")
    final PyObject super___get__(PyObject obj, PyObject type) {
        if (obj == null || obj == Py.None || this.obj != null) {
            return this;
        }
        if (getType() != TYPE) {
            // If an instance of a (strict) subclass of super, call its type
            return getType().__call__(type, obj);
        } else {
            // Inline the common case
            PyType objType = supercheck(this.superType, obj);
            PySuper newsuper = new PySuper();
            newsuper.superType = this.superType;
            newsuper.obj = obj;
            newsuper.objType = objType;
            return newsuper;
        }
    }

    public String toString() {
        String superTypeName = superType != null ? superType.fastGetName() : "NULL";
        if (objType != null) {
            return String.format("<super: <class '%s'>, <%s object>>", superTypeName,
                                 objType.fastGetName());
        } else {
            return String.format("<super: <class '%s'>, NULL>", superTypeName);
        }
    }

    public PyType getSuperType() {
        return superType;
    }

    public PyObject getObj() {
        return obj;
    }

    public PyType getObjType() {
        return objType;
    }
}
