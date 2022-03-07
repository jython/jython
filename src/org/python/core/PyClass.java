// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.core.finalization.FinalizeTrigger;

/**
 * The classic Python class.
 */
@ExposedType(name = "classobj", isBaseType = false)
public class PyClass extends PyObject implements Traverseproc {

    public static final PyType TYPE = PyType.fromClass(PyClass.class);

    /** Holds the namespace for this class */
    public PyObject __dict__;

    /** The base classes of this class */
    public PyTuple __bases__;

    /** The name of this class */
    public String __name__;

    // Store these methods for performance optimization. These are only used by PyInstance
    PyObject __getattr__, __setattr__, __delattr__, __tojava__, __del__, __contains__;

    /**
     * Create a new instance of a Python classic class.
     */
    private PyClass() {
        super(TYPE);
    }

    @ExposedNew
    public static PyObject classobj___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("function", args, keywords, "name", "bases", "dict");
        PyObject name = ap.getPyObject(0);
        PyObject bases = ap.getPyObject(1);
        PyObject dict = ap.getPyObject(2);
        return classobj___new__(name, bases, dict);
    }

    public static PyObject classobj___new__(PyObject name, PyObject bases, PyObject dict) {
        if (!name.getType().isSubType(PyString.TYPE)) {
            throw Py.TypeError("PyClass_New: name must be a string");
        }
        if (!(dict instanceof AbstractDict)) {
            throw Py.TypeError("PyClass_New: dict must be a dictionary");
        }
        PyType.ensureDoc(dict);
        PyType.ensureModule(dict);

        if (!(bases instanceof PyTuple)) {
            throw Py.TypeError("PyClass_New: bases must be a tuple");
        }
        PyTuple basesTuple = (PyTuple)bases;
        for (PyObject base : basesTuple.getArray()) {
            if (!(base instanceof PyClass)) {
                if (base.getType().isCallable()) {
                    return base.getType().__call__(name, bases, dict);
                } else {
                    throw Py.TypeError("PyClass_New: base must be a class");
                }
            }
        }

        PyClass klass = new PyClass();
        klass.__name__ = name.toString();
        klass.__bases__ = basesTuple;
        klass.__dict__ = dict;
        klass.cacheDescriptors();
        return klass;
    }

    /**
     * Setup cached references to methods where performance really counts
     */
    private void cacheDescriptors() {
        __getattr__ = lookup("__getattr__");
        __setattr__ = lookup("__setattr__");
        __delattr__ = lookup("__delattr__");
        __tojava__ = lookup("__tojava__");
        __del__ = lookup("__del__");
        __contains__ = lookup("__contains__");
    }

    PyObject lookup(String name) {
        PyObject result = __dict__.__finditem__(name);
        if (result == null && __bases__ != null) {
            for (PyObject base : __bases__.getArray()) {
                result = ((PyClass)base).lookup(name);
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public PyObject fastGetDict() {
        return __dict__;
    }

    @Override
    public PyObject __findattr_ex__(String name) {
        if (name == null) {
            return null;
        }
        switch (name) {
            case "__dict__":
                return __dict__;
            case "__bases__":
                return __bases__;
            case "__name__":
                return Py.newString(__name__);
            default:
                PyObject result = lookup(name);
                if (result == null) {
                    return result;
                }
                return result.__get__(null, this);
        }
    }

    @Override
    public void __setattr__(String name, PyObject value) {
        if (name == null) {
            return;
        }
        switch (name) {
            case "__dict__":
                setDict(value);
                return;
            case "__bases__":
                setBases(value);
                return;
            case "__name__":
                setName(value);
                return;
            case "__getattr__":
                __getattr__ = value;
                return;
            case "__setattr__":
                __setattr__ = value;
                return;
            case "__delattr__":
                __delattr__ = value;
                return;
            case "__tojava__":
                __tojava__ = value;
                return;
            case "__del__":
                __del__ = value;
                return;
            case "__contains__":
                __contains__ = value;
                return;
            default:
                if (value == null) {
                    try {
                        __dict__.__delitem__(name);
                    } catch (PyException pye) {
                        noAttributeError(name);
                    }
                }
                __dict__.__setitem__(name, value);
        }
    }

    @Override
    public void __delattr__(String name) {
        __setattr__(name, null);
    }

    @Override
    public void __rawdir__(PyDictionary accum) {
        mergeClassDict(accum, this);
    }

    /**
     * Customized AttributeError for class objects.
     */
    @Override
    public void noAttributeError(String name) {
        throw Py.AttributeError(String.format("class %.50s has no attribute '%.400s'", __name__,
                                              name));
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        PyInstance inst;
        inst = new PyInstance(this);
        if (__del__ != null) {
            FinalizeTrigger.ensureFinalizer(inst);
        }
        inst.__init__(args, keywords);
        return inst;
    }

    @Override
    public boolean isCallable() {
        return true;
    }

    /* PyClass's are compared based on __name__ */
    @Override
    public int __cmp__(PyObject other) {
        if (!(other instanceof PyClass)) {
            return -2;
        }
        int c = __name__.compareTo(((PyClass) other).__name__);
        return Integer.compare(c, 0);
    }

    @Override
    public PyString __str__() {
        // Current CPython standard is that str(class) prints as
        // module.class. If the class has no module, then just the class
        // name is printed.
        if (__dict__ == null) {
            return new PyString(__name__);
        }
        PyObject mod = __dict__.__finditem__("__module__");
        if (!(mod instanceof PyString)) {
            return new PyString(__name__);
        }
        String smod = ((PyString) mod).toString();
        return new PyString(smod + "." + __name__);
    }

    @Override
    public String toString() {
        PyObject mod = __dict__.__finditem__("__module__");
        String modStr = (mod == null || !Py.isInstance(mod, PyString.TYPE)) ? "?" : mod.toString();
        return String.format("<class %s.%s at %s>", modStr, __name__, Py.idstr(this));
    }

    public boolean isSubClass(PyClass superclass) {
        if (this == superclass) {
            return true;
        }
        if (__bases__ == null || superclass.__bases__ == null) {
            return false;
        }
        for (PyObject base: __bases__.getArray()) {
            if (((PyClass)base).isSubClass(superclass)) {
                return true;
            }
        }
        return false;
    }

    public void setDict(PyObject value) {
        if (!(value instanceof AbstractDict)) {
            throw Py.TypeError("__dict__ must be a dictionary object");
        }
        __dict__ = value;
    }

    public void setBases(PyObject value) {
        if (!(value instanceof PyTuple)) {
            throw Py.TypeError("__bases__ must be a tuple object");
        }

        PyTuple bases = (PyTuple)value;
        for (PyObject base : bases.getArray()) {
            if (!(base instanceof PyClass)) {
                throw Py.TypeError("__bases__ items must be classes");
            }
            if (((PyClass)base).isSubClass(this)) {
                throw Py.TypeError("a __bases__ item causes an inheritance cycle");
            }
        }
        __bases__ = bases;
    }

    public void setName(PyObject value) {
        if (value == null || !Py.isInstance(value, PyString.TYPE)) {
            throw Py.TypeError("__name__ must be a string object");
        }
        String name = value.toString();
        if (name.contains("\u0000")) {
            throw Py.TypeError("__name__ must not contain null bytes");
        }
        __name__ = name;
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal;
        if (__bases__ != null) {
            retVal = visit.visit(__bases__, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (__dict__ != null) {
            retVal = visit.visit(__dict__, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        //CPython also traverses the name, which is not stored
        //as a PyObject in Jython.
        //Py_VISIT(o->cl_name);
        if (__getattr__ != null) {
            retVal = visit.visit(__getattr__, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (__setattr__ != null) {
            retVal = visit.visit(__setattr__, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (__delattr__ != null) {
            retVal = visit.visit(__delattr__, arg);
            if (retVal != 0) {
                return retVal;
            }
        }

        /* Jython-only */
        if (__tojava__ != null) {
            retVal = visit.visit(__tojava__, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (__del__ != null) {
            retVal = visit.visit(__del__, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        return __contains__ != null ? visit.visit(__contains__, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (__dict__ == ob || __bases__ == ob
            || __getattr__ == ob || __setattr__ == ob || __delattr__ == ob
            || __tojava__ == ob || __del__ == ob || __contains__ == ob);
    }
}
