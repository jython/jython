package org.python.core;

public abstract class PyDescriptor extends PyObject implements Traverseproc {

    protected PyType dtype;

    protected String name;

    protected void checkCallerType(PyType type) {
        if (type == dtype || type.isSubType(dtype)) {
            return;
        }
        String msg = String.format("descriptor '%s' requires a '%s' object but received a '%s'",
                                   name, dtype.fastGetName(), type.fastGetName());
        throw Py.TypeError(msg);
    }
    
    protected void checkGetterType(PyType type) {
        if (type == dtype || type.isSubType(dtype)) {
            return;
        }
        String msg = String.format("descriptor '%s' for '%s' objects doesn't apply to '%s' object",
                                   name, dtype.fastGetName(), type.fastGetName());
        throw Py.TypeError(msg);
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return dtype != null ? visit.visit(dtype,  arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob == dtype;
    }
}
