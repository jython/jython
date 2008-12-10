// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * A python class.
 */
public class PyClass extends PyObject {
    /**
     * Holds the namespace for this class
     */
    public PyObject __dict__;

    /**
     * The base classes of this class
     */
    public PyTuple __bases__;

    /**
     * The name of this class
     */
    public String __name__;

    // Store these methods for performance optimization
    // These are only used by PyInstance
    PyObject __getattr__, __setattr__, __delattr__, __tojava__, __del__,
            __contains__;

    protected PyClass() {
        super();
    }

    /**
     * Create a python class.
     *
     * @param name name of the class.
     * @param bases A list of base classes.
     * @param dict The class dict. Normally this dict is returned by the class
     *            code object.
     *
     * @see org.python.core.Py#makeClass(String, PyObject[], PyCode, PyObject)
     */
    public PyClass(String name, PyTuple bases, PyObject dict) {
        __name__ = name;
        __bases__ = bases;
        __dict__ = dict;

        findModule(dict);

        if (dict.__finditem__("__doc__") == null) {
            dict.__setitem__("__doc__", Py.None);
        }

        // Setup cached references to methods where performance really counts
        __getattr__ = lookup("__getattr__", false);
        __setattr__ = lookup("__setattr__", false);
        __delattr__ = lookup("__delattr__", false);
        __tojava__ = lookup("__tojava__", false);
        __del__ = lookup("__del__", false);
        __contains__ = lookup("__contains__", false);
    }

    protected void findModule(PyObject dict) {
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

    // returns [PyObject, PyClass]
    PyObject[] lookupGivingClass(String name, boolean stop_at_java) {
        PyObject result = __dict__.__finditem__(name);
        PyClass resolvedClass = this;
        if (result == null && __bases__ != null) {
            int n = __bases__.__len__();
            for (int i = 0; i < n; i++) {
                resolvedClass = (PyClass)(__bases__.__getitem__(i));
                PyObject[] res = resolvedClass.lookupGivingClass(name, stop_at_java);
                if (res[0] != null) {
                    return res;
                }
            }
        }
        return new PyObject[] { result, resolvedClass };
    }

    public PyObject fastGetDict() {
        return __dict__;
    }

    PyObject lookup(String name, boolean stop_at_java) {
        PyObject[] result = lookupGivingClass(name, stop_at_java);
        return result[0];
    }

    public PyObject __findattr_ex__(String name) {
        if (name == "__dict__") {
            return __dict__;
        }
        if (name == "__name__") {
            return new PyString(__name__);
        }
        if (name == "__bases__") {
            return __bases__;
        }
        if (name == "__class__") {
            return null;
        }

        PyObject[] result = lookupGivingClass(name, false);

        if (result[0] == null) {
            return super.__findattr_ex__(name);
        }
        // xxx do we need to use result[1] (wherefound) for java cases for backw
        // comp?
        return result[0].__get__(null, this);
    }

    public void __setattr__(String name, PyObject value) {
        if (name == "__dict__") {
            if (!value.isMappingType())
                throw Py.TypeError("__dict__ must be a dictionary object");
            __dict__ = value;
            return;
        }
        if (name == "__name__") {
            if (!(value instanceof PyString)) {
                throw Py.TypeError("__name__ must be a string object");
            }
            __name__ = value.toString();
            return;
        }
        if (name == "__bases__") {
            if (!(value instanceof PyTuple)) {
                throw Py.TypeError("__bases__ must be a tuple object");
            }
            __bases__ = (PyTuple) value;
            return;
        }

        __dict__.__setitem__(name, value);
    }

    public void __delattr__(String name) {
        __dict__.__delitem__(name);
    }

    public void __rawdir__(PyDictionary accum) {
        mergeClassDict(accum, this);
    }

    /**
     * Customized AttributeError for class objects.
     */
    public void noAttributeError(String name) {
        throw Py.AttributeError(String.format("class %.50s has no attribute '%.400s'", __name__,
                                              name));
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        PyInstance inst;
        if (__del__ == null) {
            inst = new PyInstance(this);
        } else {
            // the class defined an __del__ method
            inst = new PyFinalizableInstance(this);
        }
        inst.__init__(args, keywords);

        return inst;
    }

    /* PyClass's are compared based on __name__ */
    public int __cmp__(PyObject other) {
        if (!(other instanceof PyClass)) {
            return -2;
        }
        int c = __name__.compareTo(((PyClass) other).__name__);
        return c < 0 ? -1 : c > 0 ? 1 : 0;
    }

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
        if (this.__bases__ == null || superclass.__bases__ == null) {
            return false;
        }
        PyObject[] bases = this.__bases__.getArray();
        int n = bases.length;
        for (int i = 0; i < n; i++) {
            PyClass c = (PyClass) bases[i];
            if (c.isSubClass(superclass)) {
                return true;
            }
        }
        return false;
    }
}
