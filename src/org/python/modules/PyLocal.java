package org.python.modules;

import org.python.core.Py;
import org.python.core.PyBuiltinMethod;
import org.python.core.PyDictionary;
import org.python.core.PyGetSetDescr;
import org.python.core.PyMethodDescr;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;

public class PyLocal extends PyObject {
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="local";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        dict.__setitem__("__dict__",new PyGetSetDescr("__dict__",PyLocal.class,"getDict","setDict",null));
        class exposed___init__ extends PyBuiltinMethod {

            exposed___init__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___init__(self,info);
            }

            public PyObject __call__(PyObject[]args) {
                return __call__(args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args,String[]keywords) {
                ((PyLocal)self).local_init(args,keywords);
                return Py.None;
            }

        }
        dict.__setitem__("__init__",new PyMethodDescr("__init__",PyLocal.class,-1,-1,new exposed___init__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyLocal.class,"__new__",-1,-1) {

                                                                                       public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                           PyLocal newobj;
                                                                                           if (for_type==subtype) {
                                                                                               newobj=new PyLocal();
                                                                                           } else {
                                                                                               newobj=new PyLocalDerived(subtype);
                                                                                           }
                                                                                           if (init)
                                                                                               newobj.local_init(args,keywords);
                                                                                           return newobj;
                                                                                       }

                                                                                   });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    private static final PyType LOCAL_TYPE = PyType.fromClass(PyLocal.class);
    private ThreadLocal<PyDictionary> tdict = new ThreadLocal<PyDictionary>();
    private PyObject args[];
    private String keywords[];

    public PyLocal() {
        this(LOCAL_TYPE);
    }

    public PyLocal(PyType subType) {
        super(subType);
        // Because the instantiation of a type instance in PyType.invoke_new_
        // calls dispatch__init__, we call tdict.set here so dispatch__init__
        // doesn't get called a second time for a thread in fastGetDict
        tdict.set(new PyDictionary());
    }

    final void local_init(PyObject[] args, String[] keywords) {
        PyObject[] where = new PyObject[1];
        getType().lookup_where("__init__", where);
        if(where[0] == LOCAL_TYPE && args.length > 0) {
            throw Py.TypeError("Initialization arguments are not supported");
        }
        this.args = args;
        this.keywords = keywords;
    }

    @Override
    public PyObject getDict() {
        return fastGetDict();
    }

    @Override
    public synchronized PyObject fastGetDict() {
        PyDictionary ldict = tdict.get();    
        if (ldict == null) {
            ldict = new PyDictionary();
            tdict.set(ldict);
            dispatch__init__(getType(), args, keywords);
        }
        return ldict;
    }
}
