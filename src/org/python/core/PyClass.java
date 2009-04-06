// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

/**
 * The classic Python class.
 */
@ExposedType(name = "classobj", isBaseType = false)
public class PyClass extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyClass.class);

    /** Holds the namespace for this class */
    @ExposedGet
    public PyObject __dict__;

    /** The base classes of this class */
    @ExposedGet
    public PyTuple __bases__;

    /** The name of this class */
    @ExposedGet
    @ExposedSet
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
        if (!(dict instanceof PyStringMap || dict instanceof PyDictionary)) {
            throw Py.TypeError("PyClass_New: dict must be a dictionary");
        }
        if (dict.__finditem__("__doc__") == null) {
            dict.__setitem__("__doc__", Py.None);
        }
        findModule(dict);

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

    private static void findModule(PyObject dict) {
        PyObject module = dict.__finditem__("__module__");
        if (module == null || module == Py.None) {
            PyFrame f = Py.getFrame();
            if (f != null) {
                PyObject nm = f.f_globals.__finditem__("__name__");
                if (nm != null) {
                    dict.__setitem__("__module__", nm);
                }
            }
        }
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
        PyObject result = lookup(name);
        if (result == null) {
            return super.__findattr_ex__(name);
        }
        return result.__get__(null, this);
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
        return classobj___call__(args, keywords);
    }

    @ExposedMethod
    final PyObject classobj___call__(PyObject[] args, String[] keywords) {
        PyInstance inst;
        if (__del__ == null) {
            inst = new PyInstance(this);
        } else {
            // the class defined a __del__ method
            inst = new PyFinalizableInstance(this);
        }
        inst.__init__(args, keywords);
        return inst;
    }

    /* PyClass's are compared based on __name__ */
    @Override
    public int __cmp__(PyObject other) {
        if (!(other instanceof PyClass)) {
            return -2;
        }
        int c = __name__.compareTo(((PyClass) other).__name__);
        return c < 0 ? -1 : c > 0 ? 1 : 0;
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
        if (mod == null || !(mod instanceof PyString)) {
            return new PyString(__name__);
        }
        String smod = ((PyString) mod).toString();
        return new PyString(smod + "." + __name__);
    }

    @Override
    public String toString() {
        PyObject mod = __dict__.__finditem__("__module__");
        String smod;
        if (mod == null || !(mod instanceof PyString)) {
            smod = "<unknown>";
        } else {
            smod = ((PyString) mod).toString();
        }
        return "<class " + smod + "." + __name__ + " at " + Py.idstr(this) + ">";
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

    @ExposedSet(name = "__dict__")
    public void setDict(PyObject value) {
        if (!(value instanceof PyStringMap || value instanceof PyDictionary)) {
            throw Py.TypeError("__dict__ must be a dictionary object");
        }
        __dict__ = value;
    }

    @ExposedSet(name = "__bases__")
    public void setBases(PyTuple value) {
        for (PyObject base : value.getArray()) {
            if (!(base instanceof PyClass)) {
                throw Py.TypeError("__bases__ items must be classes");
            }
            if (((PyClass)base).isSubClass(this)) {
                throw Py.TypeError("a __bases__ item causes an inheritance cycle");
            }
        }
        __bases__ = value;
    }
}
