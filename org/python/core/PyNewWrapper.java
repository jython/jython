package org.python.core;

public abstract class PyNewWrapper extends PyBuiltinFunctionWide {

    private PyType for_type;

    public PyNewWrapper(Class c, String name, int minargs, int maxargs) {
        super(new DefaultInfo(name, minargs, maxargs));
        for_type = PyType.fromClass(c);
    }

    public PyObject getSelf() {
        return for_type;
    }

    protected PyBuiltinFunction makeBound(PyObject self) {
        throw Py.SystemError("__new__ wrappers are already bound");
    }

    public PyObject inst_call(PyObject self, PyObject[] args) {
        throw Py.SystemError("__new__ wrappers are already bound");
    }

    public PyObject inst_call(
        PyObject self,
        PyObject[] args,
        String[] keywords) {
        throw Py.SystemError("__new__ wrappers are already bound");
    }

    public PyObject __call__(PyObject[] args) {
        return __call__(args, Py.NoKeywords);
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        int nargs = args.length;
        if (nargs < 1 || nargs == keywords.length) {
            throw Py.TypeError(
                for_type.fastGetName() + ".__new__(): not enough arguments");
        }
        PyObject arg0 = args[0];
        if (!(arg0 instanceof PyType)) {
            throw Py.TypeError(
                for_type.fastGetName()
                    + ".__new__(X): X is not a type object ("
                    + arg0.getType().fastGetName()
                    + ")");
        }
        PyType subtype = (PyType) arg0;
        if (!subtype.isSubType(for_type)) {
            throw Py.TypeError(
                for_type.fastGetName()
                    + ".__new__("
                    + subtype.fastGetName()
                    + "): "
                    + subtype.fastGetName()
                    + " is not a subtype of "
                    + for_type.fastGetName());
        }
        // xxx with subclassing this should become more subtle
        if (subtype != for_type) {
            throw Py.TypeError(
                for_type.fastGetName()
                    + ".__new__("
                    + subtype.fastGetName()
                    + ") is not safe, use "
                    + subtype.fastGetName()
                    + ".__new__()");
        }
        PyObject[] rest = new PyObject[nargs-1];
        System.arraycopy(args,1,rest,0,nargs-1);
        return new_impl(false,subtype,rest,keywords);
    }

    // init true => invoke subtype.__init__(...) unless it is known to be unnecessary
    public  abstract PyObject new_impl(boolean init,PyType subtype, PyObject[] args, String[] keywords);

}


