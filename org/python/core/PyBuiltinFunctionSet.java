// Copyright © Corporation for National Research Initiatives
package org.python.core;

public class PyBuiltinFunctionSet extends PyObject {
    public String name;
    public int index;
    public int minargs, maxargs;
    public boolean isMethod;

    public PyBuiltinFunctionSet() {; }

    public PyBuiltinFunctionSet(String name, int index,
                                int minargs, int maxargs)
    {
        this.name = name;
        this.index = index;
        this.minargs = minargs;
        this.maxargs = maxargs;
        this.isMethod = false;
    }

    public PyBuiltinFunctionSet init(String name, int index, int nargs) {
        return init(name, index, nargs, nargs, false);
    }
    public PyBuiltinFunctionSet init(String name, int index, int nargs,
                                     boolean isMethod)
    {
        return init(name, index, nargs, nargs, isMethod);
    }
    public PyBuiltinFunctionSet init(String name, int index, int minargs,
                                     int maxargs)
    {
        return init(name, index, minargs, maxargs, false);
    }
    public PyBuiltinFunctionSet init(String name, int index, int minargs,
                                     int maxargs, boolean isMethod)
    {
        this.name = name;
        this.index = index;
        this.minargs = minargs;
        this.maxargs = maxargs;
        this.isMethod = isMethod;
        return this;
    }
    
    public PyObject _doget(PyObject container) {
        return _doget(container, null);
    }

    public PyObject _doget(PyObject container, PyObject wherefound) {
        if (isMethod)
            return new PyMethod(container, this, wherefound);
        else
            return this;
    }
        
    public String toString() {
        if (isMethod) {
            return "<builtin method '"+name+"'>";            
        } else {
            return "<builtin function '"+name+"'>";
        }
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
        if (minargs != -1 && nargs > minargs || nargs < minargs) {
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
