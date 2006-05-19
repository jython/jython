package org.python.core;

public class PyStaticMethod extends PyObject implements PyType.Newstyle {

    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */
    public final static String exposed_name = "staticmethod";
    
    public static void typeSetup(PyObject dict, PyType.Newstyle marker) {
        // xxx __get__
        // xxx __init__
        
        dict
            .__setitem__(
                "__new__",
                new PyNewWrapper(PyStaticMethod.class, "__new__", 1, 1) {
            public PyObject new_impl(
                boolean init,
                PyType subtype,
                PyObject[] args,
                String[] keywords) {
                    if (keywords.length != 0 || args.length!=1) {
                        throw info.unexpectedCall(args.length,keywords.length!=0);
                    }
                    return new PyStaticMethod(args[0]);
            } // xxx subclassing
        });   
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    
    protected PyObject callable;
    
    public PyStaticMethod(PyObject callable) {
        this.callable = callable;
    }
    
    /*
     * @see org.python.core.PyObject#__get__(org.python.core.PyObject, org.python.core.PyObject)
     */
    public PyObject __get__(PyObject obj, PyObject type) {
        return callable;
    }

}
