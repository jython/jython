package org.python.core;

public abstract class PyNewWrapper extends PyBuiltinMethod {

    public PyType for_type;

    /**
     * Creates a wrapper without binding it to a type. setWrappedType must be called
     * before this wrapper can be used.
     */
    public PyNewWrapper() {
        this((PyType)null, "__new__", -1, -1);
    }

    public PyNewWrapper(Class c, String name, int minargs, int maxargs) {
        this(PyType.fromClass(c), name, minargs, maxargs);
    }

    public PyNewWrapper(PyType type, String name, int minargs, int maxargs) {
        super(type, new DefaultInfo(name, minargs, maxargs));
        for_type = (PyType)getSelf();
    }


    // init true => invoke subtype.__init__(...) unless it is known to be
    // unnecessary
    public abstract PyObject new_impl(boolean init,
                                      PyType subtype,
                                      PyObject[] args,
                                      String[] keywords);
    
    public PyBuiltinCallable bind(PyObject self) {
        throw Py.SystemError("__new__ wrappers are already bound");
    }
    
    public PyType getWrappedType() {
        return for_type;
    }
    
    public void setWrappedType(PyType type) {
        self = type;
        for_type = type;
    }

    public PyObject __call__(PyObject[] args) {
        return __call__(args, Py.NoKeywords);
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        int nargs = args.length;
        if(nargs < 1 || nargs == keywords.length) {
            throw Py.TypeError(for_type.fastGetName()
                    + ".__new__(): not enough arguments");
        }
        PyObject arg0 = args[0];
        if(!(arg0 instanceof PyType)) {
            throw Py.TypeError(for_type.fastGetName()
                    + ".__new__(X): X is not a type object ("
                    + arg0.getType().fastGetName() + ")");
        }
        PyType subtype = (PyType)arg0;
        if(!subtype.isSubType(for_type)) {
            throw Py.TypeError(for_type.fastGetName() + ".__new__("
                    + subtype.fastGetName() + "): " + subtype.fastGetName()
                    + " is not a subtype of " + for_type.fastGetName());
        }
        if(subtype.getStatic() != for_type) {
            throw Py.TypeError(for_type.fastGetName() + ".__new__("
                    + subtype.fastGetName() + ") is not safe, use "
                    + subtype.fastGetName() + ".__new__()");
        }
        PyObject[] rest = new PyObject[nargs - 1];
        System.arraycopy(args, 1, rest, 0, nargs - 1);
        return new_impl(false, subtype, rest, keywords);
    }
}
