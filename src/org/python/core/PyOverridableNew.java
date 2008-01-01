package org.python.core;


/**
 * A __new__ function that tells its subclasses to just init if __new__ is being
 * called on the type the function was defined on. Otherwise, it just leaves
 * initting up to the subtype otherwise.
 */
public abstract class PyOverridableNew extends PyNewWrapper {

    @Override
    public PyObject new_impl(boolean init, PyType subtype, PyObject[] args, String[] keywords) {
        if (for_type==subtype) {
            return createOfType(init, args, keywords);
        } else {
            return createOfSubtype(subtype);
        }
    }
    
    /**
     * Called when new is invoked on the type the new was defined on.
     * 
     * @param init - if the new object should be initted.
     * @param args - args passed to call
     * @param keywords - keywords passed to call
     * @return - the new object.
     */
    public abstract PyObject createOfType(boolean init, PyObject[] args, String[] keywords);
    
    /** Called when new is invoked on a subtype of for_type. */
    public abstract PyObject createOfSubtype(PyType subtype);
}
