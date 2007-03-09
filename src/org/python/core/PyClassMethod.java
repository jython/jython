package org.python.core;

public class PyClassMethod extends PyObject implements PyType.Newstyle {
    // xxx __init__

    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="classmethod";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___get__ extends PyBuiltinMethodNarrow {

            exposed___get__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___get__(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                return((PyClassMethod)self).classmethod___get__(arg0,arg1);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyClassMethod)self).classmethod___get__(arg0);
            }

        }
        dict.__setitem__("__get__",new PyMethodDescr("__get__",PyClassMethod.class,1,2,new exposed___get__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyClassMethod.class,"__new__",1,1) {

                                                                                           public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                               return classmethod_new(this,init,subtype,args,keywords);
                                                                                           }

                                                                                       });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    
    protected PyObject callable;
    
    public PyClassMethod(PyObject callable) {
        if (!callable.isCallable()) {                   
            throw Py.TypeError("'" + callable.getType().fastGetName() + "' object is not callable");
        }
        this.callable = callable;
    }

    public PyObject __get__(PyObject obj) {
        return classmethod___get__(obj, null);
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        return classmethod___get__(obj, type);
    }

    final PyObject classmethod___get__(PyObject obj) {
        return classmethod___get__(obj, null);
    }

    final PyObject classmethod___get__(PyObject obj, PyObject type) {
        if(type == null) {
            type = obj.getType();
        }
        return new PyMethod(type, callable, type.getType());
    }

    final static PyObject classmethod_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        if (keywords.length != 0) {
            throw Py.TypeError("classmethod does not accept keyword arguments");
        }
        if (args.length != 1) {
            throw Py.TypeError("classmethod expected 1 argument, got " + args.length);
        }
        return new PyClassMethod(args[0]);
    }
}
