// Copyright © Corporation for National Research Initiatives
package org.python.core;

public class PyFunction extends PyObject
{
    public String __name__;
    public PyObject __doc__;
    public PyObject func_globals;
    public PyObject[] func_defaults;
    public PyCode func_code;

    public PyFunction(PyObject globals, PyObject[] defaults, PyCode code,
                      PyObject doc)
    {
        func_globals = globals;
        __name__ = code.co_name;
        if (doc == null)
            __doc__ = Py.None;
        else
            __doc__ = doc;
        func_defaults = defaults;
        func_code = code;
    }
        
    public PyFunction(PyObject globals, PyObject[] defaults, PyCode code) {
        this(globals, defaults, code, null);
    }
        
    private static final String[] __members__ = {
        "__doc__", "func_doc",
        "__name__", "func_name",
        "func_globals", "func_defaults", "func_code"
    };

    public PyObject __dir__() {
        PyString members[] = new PyString[__members__.length];
        for (int i = 0; i < __members__.length; i++)
            members[i] = new PyString(__members__[i]);
        return new PyList(members);
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
        // not yet implemented:
        // func_code
        // func_defaults
        else throwReadonly(name);
    }

    public void __delattr__(String name) {
        // TBD: should __doc__ be del'able?
        throwReadonly(name);
    }

    public PyObject __findattr__(String name) {
        // these are special, everything else is findable by reflection
        if (name == "func_doc")
            return __doc__;
        if (name == "func_name")
            return new PyString(__name__);
        if (name == "func_defaults") {
            if (func_defaults.length == 0)
                return Py.None;
            return new PyTuple(func_defaults);
        }
        return super.__findattr__(name);
    }
    
    public PyObject _doget(PyObject container) {
        return _doget(container, null);
    }

    public PyObject _doget(PyObject container, PyObject wherefound) {
        return new PyMethod(container, this, wherefound);
    }

    public PyObject __call__() {
        return func_code.call(func_globals, func_defaults);
    }   
    public PyObject __call__(PyObject arg) {
        return func_code.call(arg, func_globals, func_defaults);
    }
    public PyObject __call__(PyObject arg1, PyObject arg2) {
        return func_code.call(arg1, arg2, func_globals, func_defaults);
    }
    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        return func_code.call(arg1, arg2, arg3, func_globals, func_defaults);
    }
        
    public PyObject __call__(PyObject[] args, String[] keywords) {
        return func_code.call(args, keywords, func_globals, func_defaults);
    }
    public PyObject __call__(PyObject arg1, PyObject[] args, String[] keywords)
    {
        return func_code.call(arg1, args, keywords, func_globals,
                              func_defaults);
    }
    public String toString() {
        return "<function "+__name__+" at "+hashCode()+">";
    }
}
