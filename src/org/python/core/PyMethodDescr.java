package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "method_descriptor", base = PyObject.class, isBaseType = false)
public class PyMethodDescr extends PyDescriptor implements PyBuiltinCallable.Info {

    protected int minargs, maxargs;

    protected PyBuiltinCallable meth;

    public PyMethodDescr(PyType t, PyBuiltinCallable func) {
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
        checkCallerType(args[0].getType());
        PyObject[] actualArgs = new PyObject[args.length - 1];
        System.arraycopy(args, 1, actualArgs, 0, actualArgs.length);
        return meth.bind(args[0]).__call__(actualArgs, kwargs);
    }

    public PyException unexpectedCall(int nargs, boolean keywords) {
        return PyBuiltinCallable.DefaultInfo.unexpectedCall(nargs, keywords, name, minargs,
                                                            maxargs);
    }

    @Override
    public PyObject __get__(PyObject obj, PyObject type) {
        return method_descriptor___get__(obj, type);
    }

    @ExposedMethod(defaults = "null")
    final PyObject method_descriptor___get__(PyObject obj, PyObject type) {
        if(obj != null) {
            checkGetterType(obj.getType());
            return meth.bind(obj);
        }
        return this;
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
