package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedType;

@ExposedType(name = "descriptor")
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
