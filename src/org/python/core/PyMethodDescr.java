package org.python.core;

public class PyMethodDescr extends PyDescriptor implements PyBuiltinFunction.Info {

    protected int minargs, maxargs;

    protected PyBuiltinFunction meth;

    public PyMethodDescr(PyType t, PyBuiltinFunction func) {
        name = func.info.getName();
        dtype = t;
        minargs = func.info.getMinargs();
        maxargs = func.info.getMaxargs();
        meth = func;
        meth.setInfo(this);
    }

    public String getName() {
        return name;
    }

    public int getMaxargs() {
        return maxargs;
    }

    public int getMinargs() {
        return minargs;
    }

    public String toString() {
        return "<method '" + name + "' of '" + dtype.fastGetName() + "' objects>";
    }

    public PyObject __call__(PyObject[] args, String[] kwargs) {
        if(args.length == kwargs.length) {
            throw Py.TypeError(name + " requires at least one argument");
        }
        checkCallerType(args[0]);
        PyObject[] actualArgs = new PyObject[args.length - 1];
        System.arraycopy(args, 1, actualArgs, 0, actualArgs.length);
        return meth.bind(args[0]).__call__(actualArgs, kwargs);
    }

    public PyException unexpectedCall(int nargs, boolean keywords) {
        return PyBuiltinFunction.DefaultInfo.unexpectedCall(nargs, keywords, name, minargs, maxargs);
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        if(obj != null) {
            checkCallerType(obj);
            return meth.bind(obj);
        }
        return this;
    }

    protected void checkCallerType(PyObject obj) {
        PyType objtype = obj.getType();
        if(objtype != dtype && !objtype.isSubType(dtype)) {
            throw get_wrongtype(objtype);
        }
    }
}
