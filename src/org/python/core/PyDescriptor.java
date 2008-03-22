package org.python.core;

public abstract class PyDescriptor extends PyObject {

    protected PyType dtype;
    protected String name;

    protected PyException call_wrongtype(PyType objtype) {
        return Py.TypeError(
            "descriptor '"
                + name
                + "' requires '"
                + dtype.fastGetName()
                + "' object but received a '"
                + objtype.fastGetName()
                + "'");
    }
    
    protected PyException get_wrongtype(PyType objtype) {
         return Py.TypeError(
             "descriptor '"
                 + name
                 + "' for '"
                 + dtype.fastGetName()
                 + "' objects doesn't apply to '"
                 + objtype.fastGetName()
                 + "' object");
     }    
    
    protected String blurb() {
        return "descriptor '"+name+"' of '"+dtype.fastGetName()+"' object";
    }
}
