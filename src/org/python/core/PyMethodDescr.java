package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "method_descriptor", base = PyObject.class)
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

    public int getMaxargs() {
        return maxargs;
    }

    public int getMinargs() {
        return minargs;
    }

    @Override
    public String toString() {
        return String.format("<method '%s' of '%s' objects>", name, dtype.fastGetName());
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] kwargs) {
        return method_descriptor___call__(args, kwargs);
    }

    @ExposedMethod
    final PyObject method_descriptor___call__(PyObject[] args, String[] kwargs) {
        if(args.length == kwargs.length) {
            throw Py.TypeError(name + " requires at least one argument");
        }
        checkCallerType(args[0]);
        PyObject[] actualArgs = new PyObject[args.length - 1];
        System.arraycopy(args, 1, actualArgs, 0, actualArgs.length);
        return meth.bind(args[0]).__call__(actualArgs, kwargs);
    }

    public PyException unexpectedCall(int nargs, boolean keywords) {
        return PyBuiltinFunction.DefaultInfo.unexpectedCall(nargs, keywords, name, minargs,
                                                            maxargs);
    }

    @Override
    public PyObject __get__(PyObject obj, PyObject type) {
        return method_descriptor___get__(obj, type);
    }

    @ExposedMethod
    final PyObject method_descriptor___get__(PyObject obj, PyObject type) {
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

    /**
     * Return the name this descriptor is exposed as.
     *
     * @return a name String
     */
    @ExposedGet(name = "__name__")
    public String getName() {
        return name;
    }

    /**
     * Return the owner class of this descriptor.
     *
     * @return this descriptor's owner
     */
    @ExposedGet(name = "__objclass__")
    public PyObject getObjClass() {
        return dtype;
    }
}
