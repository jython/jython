package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "super")
public class PySuper extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PySuper.class);
   
    @ExposedGet
    protected PyType thisClass;
    @ExposedGet
    protected PyObject self;
    @ExposedGet
    protected PyType selfClass;

    private PyType supercheck(PyType type,PyObject obj) {
        if (obj instanceof PyType && ((PyType)obj).isSubType(type)) {
            return (PyType)obj;
        }
        PyType obj_type = obj.getType();
        if (obj_type.isSubType(type))
            return obj_type;
        throw Py.TypeError("super(type, obj): "+
                "obj must be an instance or subtype of type");
    }
    
    @ExposedMethod
    @ExposedNew
    public void super___init__(PyObject[] args, String[] keywords) {
        if (keywords.length != 0
                || !PyBuiltinFunction.DefaultInfo.check(args.length, 1, 2)) {
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(args.length,
                    keywords.length != 0, "super", 1, 2);
        }
        if (!(args[0] instanceof PyType)) {
            throw Py.TypeError("super: argument 1 must be type");
        }
        PyType type = (PyType)args[0];
        PyObject obj = null;
        PyType obj_type = null;
        if (args.length == 2 && args[1] != Py.None)
            obj = args[1];
        if (obj != null) {
            obj_type = supercheck(type, obj);
        }
        this.thisClass = type;
        this.self = obj;
        this.selfClass = obj_type;
    }
    
    public PySuper() {
        this(TYPE);
    }

    public PySuper(PyType subType) {
        super(subType);
    }
    
    public PyObject getSelf() {
        return self;
    }
    public PyType getSelfClass() {
        return selfClass;
    }
    public PyType getThisClass() {
        return thisClass;
    }
    
    public PyObject __findattr__(String name) {
        return super___findattr__(name);
    }

    final PyObject super___findattr__(String name) {
        if (selfClass != null && name != "__class__") {
            PyObject descr = selfClass.super_lookup(thisClass, name);
            return descr.__get__(selfClass == self ? null : self, selfClass);
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
        return super___get__(obj,type);
    }

    @ExposedMethod(defaults = "null")
    final PyObject super___get__(PyObject obj, PyObject type) { //xxx subtype case!
        if (obj == null || obj == Py.None || self != null)
            return this;
        PyType obj_type = supercheck(this.thisClass, obj);
        PySuper newsuper = new PySuper();
        newsuper.thisClass = this.thisClass;
        newsuper.self = obj;
        newsuper.selfClass = obj_type;
        return newsuper;
    }

}
