package org.python.core;

public class PyClassMethod extends PyObject implements PyType.Newstyle {

    /* type info */
    public final static String exposed_name = "classmethod";
    
    public static void typeSetup(PyObject dict, PyType.Newstyle marker) {
        // xxx __get__
        // xxx __init__
        
        dict
            .__setitem__(
                "__new__",
                new PyNewWrapper(PyClassMethod.class, "__new__", 1, 1) {
            public PyObject new_impl(
                boolean init,
                PyType subtype,
                PyObject[] args,
                String[] keywords) {
                    if (keywords.length != 0 || args.length!=1) {
                        throw info.unexpectedCall(args.length,keywords.length!=0);
                    }
                    return new PyClassMethod(args[0]);
            } // xxx subclassing
        });   
    }
    
    protected PyObject callable;
    
    public PyClassMethod(PyObject callable) {
        this.callable = callable;
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        if (type == null)
            type = obj.getType();
        return new PyMethod(type, callable, type.getType());
    }

}
