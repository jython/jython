// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * An implementation of PyCode where the actual executable content
 * is stored as a PyFunctionTable instance and an integer index.
 */

public class PyTableCode extends PyBaseCode
{

    PyFunctionTable funcs;
    int func_id;
    public String co_code = ""; // only used by inspect

    public PyTableCode(int argcount, String varnames[],
                       String filename, String name,
                       int firstlineno,
                       boolean varargs, boolean varkwargs,
                       PyFunctionTable funcs, int func_id)
    {
        this(argcount, varnames, filename, name, firstlineno, varargs,
             varkwargs, funcs, func_id, null, null, 0, 0);
    }

    public PyTableCode(int argcount, String varnames[],
                       String filename, String name,
                       int firstlineno,
                       boolean varargs, boolean varkwargs,
                       PyFunctionTable funcs, int func_id,
                       String[] cellvars, String[] freevars, int npurecell,
                       int moreflags) // may change
    {
        co_argcount = nargs = argcount;
        co_varnames = varnames;
        co_nlocals = varnames.length;
        co_filename = filename;
        co_firstlineno = firstlineno;
        co_cellvars = cellvars;
        co_freevars = freevars;
        this.jy_npurecell = npurecell;
        this.varargs = varargs;
        co_name = name;
        if (varargs) {
            co_argcount -= 1;
            co_flags |= CO_VARARGS;
        }
        this.varkwargs = varkwargs;
        if (varkwargs) {
            co_argcount -= 1;
            co_flags |= CO_VARKEYWORDS;
        }
        co_flags |= moreflags;
        this.funcs = funcs;
        this.func_id = func_id;
    }

    private static final String[] __members__ = {
        "co_name", "co_argcount",
        "co_varnames", "co_filename", "co_firstlineno",
        "co_flags","co_cellvars","co_freevars","co_nlocals"
        // not supported: co_code, co_consts, co_names,
        // co_lnotab, co_stacksize
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
        // no writable attributes
        throwReadonly(name);
    }

    public void __delattr__(String name) {
        throwReadonly(name);
    }

    private static PyTuple toPyStringTuple(String[] ar) {
        if (ar == null) return Py.EmptyTuple;
        int sz = ar.length;
        PyString[] pystr = new PyString[sz];
        for (int i = 0; i < sz; i++) {
            pystr[i] = new PyString(ar[i]);
        }
        return new PyTuple(pystr);
    }

    public PyObject __findattr_ex__(String name) {
        // have to craft co_varnames specially
        if (name == "co_varnames") {
            return toPyStringTuple(co_varnames);
        }
        if (name == "co_cellvars") {
            return toPyStringTuple(co_cellvars);
        }
        if (name == "co_freevars") {
            return toPyStringTuple(co_freevars);
        }
        if (name == "co_filename") {
            return new PyString(co_filename);
        }
        if (name == "co_name") {
            return new PyString(co_name);
        }
        return super.__findattr_ex__(name);
    }

    protected PyObject interpret(PyFrame frame) {
        return funcs.call_function(func_id, frame);
    }
}
