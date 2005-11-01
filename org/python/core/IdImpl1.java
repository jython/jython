package org.python.core;

/**
 * 
 * @deprecated Java1 no longer supported.
 * 
 */
public class IdImpl1 extends IdImpl {

    public long id(PyObject o) {
        if (o instanceof PyJavaInstance) {
            return System.identityHashCode(((PyJavaInstance) o).javaProxy);
        } else {
            return System.identityHashCode(o);
        }
    }

    public String idstr(PyObject o) {
        return Long.toString(id(o));
    }

    public long java_obj_id(Object o) {
        return System.identityHashCode(o);
    }

}
