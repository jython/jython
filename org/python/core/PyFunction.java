// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * A python function.
 */

public class PyFunction extends PyObject
{
    public String __name__;
    public PyObject __doc__;
    public PyObject func_globals;
    public PyObject[] func_defaults;
    public PyCode func_code;
    public PyObject __dict__;
    public PyObject func_closure; // nested scopes: closure

    public PyFunction(PyObject globals, PyObject[] defaults, PyCode code,
                      PyObject doc,PyObject[] closure_cells)
    {
        func_globals = globals;
        __name__ = code.co_name;
        if (doc == null)
            __doc__ = Py.None;
        else
            __doc__ = doc;
        func_defaults = defaults;
        func_code = code;
        if (closure_cells != null) {
            func_closure = new PyTuple(closure_cells);
        } else {
            func_closure = null;
        }
    }

    public PyFunction(PyObject globals, PyObject[] defaults, PyCode code,
                      PyObject doc) {
        this(globals,defaults,code,doc,null);
    }

    public PyFunction(PyObject globals, PyObject[] defaults, PyCode code) {
        this(globals, defaults, code, null,null);
    }

    public PyFunction(PyObject globals, PyObject[] defaults, PyCode code,
                      PyObject[] closure_cells)
    {
        this(globals, defaults, code, null,closure_cells);
    }


    private static final String[] __members__ = {
        "__doc__", "func_doc",
        "__name__", "func_name", "__dict__",
        "func_globals", "func_defaults", "func_code",
        "func_closure"
    };

    public PyObject __dir__() {
        PyString members[] = new PyString[__members__.length];
        for (int i = 0; i < __members__.length; i++)
            members[i] = new PyString(__members__[i]);
        PyList ret = new PyList(members);
        addKeys(ret, "__dict__");
        ret.sort();
        return ret;
    }

    private void throwReadonly(String name) {
        for (int i = 0; i < __members__.length; i++)
            if (__members__[i] == name)
                throw Py.TypeError("readonly attribute");
        throw Py.AttributeError(name);
    }

    public void __setattr__(String name, PyObject value) {
        // TBD: in CPython, func_code, func_defaults, func_doc, __doc__ are
        // writable.  For now, only func_doc, __doc__ are writable in
        // JPython.
        if (name == "func_doc" || name == "__doc__")
            __doc__ = value;
        else if (name == "func_closure") {
            if (!(value instanceof PyTuple)) {
                throw Py.TypeError("func_closure must be set to a tuple");
            }
            func_closure = value;
        }
        // not yet implemented:
        // func_code
        // func_defaults
        else if (name == "func_defaults")
            throwReadonly(name);
        else if (name == "func_code") {
            if (value instanceof PyCode)
                func_code = (PyCode) value;
            else
                throw Py.TypeError("func_code must be set to a code object");
        } else if (name == "__dict__" || name == "func_dict") {
            if (value instanceof PyDictionary || value instanceof PyStringMap)
                __dict__ = value;
            else
                throw Py.TypeError("setting function's dictionary " + 
                                   "to a non-dict");
        } else {
            if (__dict__ == null)
                __dict__ = new PyStringMap();
            __dict__.__setitem__(name, value);
        }
    }

    public void __delattr__(String name) {
        if (name == "__dict__" || name == "func_dict") {
            throw Py.TypeError("function's dictionary may not be deleted");
        } else if (name == "func_defaults") {
            func_defaults = Py.EmptyObjects;
            return;
        } else if (name == "func_doc" || name == "__doc__") {
            __doc__ = Py.None;
            return;
        }
        if (__dict__ == null)
            throw Py.AttributeError(name);
        __dict__.__delitem__(name);
    }

    public boolean isMappingType() { return false; }
    public boolean isNumberType() { return false; }
    public boolean isSequenceType() { return false; }

    public PyObject __findattr__(String name) {
        // these are special, everything else is findable by reflection
        if (name == "func_doc")
            return __doc__;
        if (name == "func_name")
            return new PyString(__name__);
        if (name == "func_closure") {
            if (func_closure != null) return func_closure;
            return Py.None;
        }
        if (name == "func_defaults") {
            if (func_defaults.length == 0)
                return Py.None;
            return new PyTuple(func_defaults);
        }
        if (name == "__dict__" || name == "func_dict") {
            if (__dict__ == null)
                __dict__ = new PyStringMap();
            return __dict__;
        }
        if (__dict__ != null) {
            PyObject ret = __dict__.__finditem__(name);
            if (ret != null)
                return ret;
        }
        return super.__findattr__(name);
    }

    public PyObject _doget(PyObject container) {
        //System.out.println("_doget(c):"+(container==null?null:container.safeRepr())); // debug
        return _doget(container, null);
    }

    public PyObject _doget(PyObject container, PyObject wherefound) {
        //System.out.println("_doget(c,w):"+(container==null?null:container.safeRepr())
        //+","+(wherefound==null?null:wherefound.safeRepr())); // debug
        return new PyMethod(container, this, wherefound);
    }

    public PyObject __call__() {
        return func_code.call(func_globals, func_defaults, func_closure);
    }
    public PyObject __call__(PyObject arg) {
        return func_code.call(arg, func_globals, func_defaults, func_closure);
    }
    public PyObject __call__(PyObject arg1, PyObject arg2) {
        return func_code.call(arg1, arg2, func_globals, func_defaults,
                              func_closure);
    }
    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        return func_code.call(arg1, arg2, arg3, func_globals, func_defaults,
                              func_closure);
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        return func_code.call(args, keywords, func_globals, func_defaults,
                              func_closure);
    }
    public PyObject __call__(PyObject arg1, PyObject[] args,
                             String[] keywords)
    {
        return func_code.call(arg1, args, keywords, func_globals,
                              func_defaults,  func_closure);
    }
    public String toString() {
        return "<function "+__name__+" "+Py.idstr(this)+">";
    }
}
