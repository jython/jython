// Copyright © Corporation for National Research Initiatives
package org.python.core;

public class PyBuiltinFunctionSet extends PyObject implements Cloneable
{
    // part of the public interface for built-in functions
    public PyObject __name__;
    public PyObject __doc__;
    public PyObject __self__;
    public static PyObject __members__;
    
    // internal implementation
    protected String name;
    protected int minargs, maxargs;
    protected boolean isMethod;
    // used as an index into a big switch statement in the various derived
    // class's __call__() methods.
    protected int index;
    protected String doc;

    static {
        PyString[] members = new PyString[3];
        members[0] = new PyString("__doc__");
        members[1] = new PyString("__name__");
        members[2] = new PyString("__self__");
        __members__ = new PyList(members);
    }

    // full-blown constructor, specifying everything
    public PyBuiltinFunctionSet(String name, int index, int minargs,
                                int maxargs, boolean isMethod, String doc)
    {
        this.name = name;
        this.index = index;
        this.minargs = minargs;
        this.maxargs = maxargs;
        this.isMethod = isMethod;
        this.doc = doc;

        __name__ = new PyString(name);
        if (doc == null)
            __doc__ = Py.None;
        else
            __doc__ = new PyString(doc);
        __self__ = Py.None;
    }

    public PyObject _doget(PyObject container) {
        return _doget(container, null);
    }

    public PyObject _doget(PyObject container, PyObject wherefound) {
        if (isMethod) {
            // TBD: is there a better way?
            try {
                PyBuiltinFunctionSet unique = (PyBuiltinFunctionSet)clone();
                unique.__self__ = container;
                return unique;
            }
            catch (CloneNotSupportedException e) {}
        }
        return this;
    }
        
    public String toString() {
        if (isMethod)
            return "<builtin method '"+name+"'>";
        else
            return "<builtin function '"+name+"'>";
    }

    public PyException argCountError(int nargs) {
        if (minargs == maxargs) {
            return Py.TypeError(name+"(): expected "+minargs+" args; got "+
                                nargs);
        } else {
            return Py.TypeError(name+"(): expected "+minargs+"-"+maxargs+
                                " args; got "+nargs);
        }
    }

    public PyObject fancyCall(PyObject[] args) {
        throw Py.TypeError("surprising call");
    }

    public PyObject __call__(PyObject[] args) {
        int nargs = args.length;
        if (minargs != -1 && (nargs > maxargs || nargs < minargs)) {
            throw argCountError(nargs);
        }
        switch (nargs) {
        case 0:
            return __call__();
        case 1:
            return __call__(args[0]);
        case 2:
            return __call__(args[0], args[1]);
        case 3:
            return __call__(args[0], args[1], args[2]);
        case 4:
            return __call__(args[0], args[1], args[2], args[3]);
        default:
            return fancyCall(args);
        }
    }

    public PyObject __call__() {
        throw argCountError(0);
    }

    public PyObject __call__(PyObject arg1) {
        throw argCountError(1);
    }

    public PyObject __call__(PyObject arg1, PyObject arg2) {
        throw argCountError(2);
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        throw argCountError(3);
    }    
    
    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3,
                             PyObject arg4)
    {
        throw argCountError(4);
    }
}
