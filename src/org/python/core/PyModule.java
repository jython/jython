// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

public class PyModule extends PyObject
{
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="module";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        dict.__setitem__("__dict__",new PyGetSetDescr("__dict__",PyModule.class,"getDict","setDict","delDict"));
        dict.__setitem__("__doc__",new PyGetSetDescr("__doc__",PyModule.class,"getDoc",null,null));
        class exposed___repr__ extends PyBuiltinMethodNarrow {

            exposed___repr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___repr__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyModule)self).module_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyModule.class,0,0,new exposed___repr__(null,null)));
        class exposed___setattr__ extends PyBuiltinMethodNarrow {

            exposed___setattr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___setattr__(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    ((PyModule)self).module___setattr__(arg0.asName(0),arg1);
                    return Py.None;
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="attribute name must be a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("__setattr__",new PyMethodDescr("__setattr__",PyModule.class,2,2,new exposed___setattr__(null,null)));
        class exposed___delattr__ extends PyBuiltinMethodNarrow {

            exposed___delattr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___delattr__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    ((PyModule)self).module___delattr__(arg0.asName(0));
                    return Py.None;
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="attribute name must be a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("__delattr__",new PyMethodDescr("__delattr__",PyModule.class,1,1,new exposed___delattr__(null,null)));
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
                ((PyModule)self).module_init(args,keywords);
                return Py.None;
            }

        }
        dict.__setitem__("__init__",new PyMethodDescr("__init__",PyModule.class,-1,-1,new exposed___init__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyModule.class,"__new__",-1,-1) {

                                                                                        public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                            PyModule newobj;
                                                                                            if (for_type==subtype) {
                                                                                                newobj=new PyModule();
                                                                                                if (init)
                                                                                                    newobj.module_init(args,keywords);
                                                                                            } else {
                                                                                                newobj=new PyModuleDerived(subtype);
                                                                                            }
                                                                                            return newobj;
                                                                                        }

                                                                                    });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    private final PyObject module_doc = new PyString(
      "module(name[, doc])\n" +
      "\n" +
      "Create a module object.\n" + 
      "The name must be a string; the optional doc argument can have any type.");

    public PyObject __dict__;

    public PyModule() {
        super();
    }

    public PyModule(PyType subType) {
        super(subType);
    }

    public PyModule(PyType subType, String name) {
        super(subType);
        module_init(new PyString(name), Py.None);
    }

    public PyModule(String name) {
        this(name, null);
    }

    public PyModule(String name, PyObject dict) {
        super();
        __dict__ = dict;
        module_init(new PyString(name), Py.None);
    }

    final void module_init(PyObject name, PyObject doc) {
        ensureDict();
        __dict__.__setitem__("__name__", name);
        __dict__.__setitem__("__doc__", doc);
    }

    final void module_init(PyObject[] args, String[] keywords) {
      ArgParser ap = new ArgParser("__init__", args, keywords, new String[] {"name",
                                                                             "doc"});
      PyObject name = ap.getPyObject(0);
      PyObject docs = ap.getPyObject(1, Py.None);
      module_init(name, docs);
    }

    public PyObject fastGetDict() {
        return __dict__;
    }

    public PyObject getDict() {
        if (__dict__ == null)
            return Py.None;
        return __dict__;
    }
  
    public void setDict(PyObject newDict) {
        throw Py.TypeError("readonly attribute");
    }

    public void delDict() {
        throw Py.TypeError("readonly attribute");
    }

    public PyObject getDoc() {
        PyObject d = fastGetDict();
        if (d != null) {
            PyObject doc = d.__finditem__("__doc__");
            if (doc != null) {
                return doc;
            }
        }
        return module_doc;
    }

    protected PyObject impAttr(String attr) {
        PyObject path = __dict__.__finditem__("__path__");
        PyObject pyname = __dict__.__finditem__("__name__");

        if (path == null || pyname == null) return null;

        String name = pyname.__str__().toString();
        String fullName = (name+'.'+attr).intern();

        PyObject ret = null;

        //System.err.println("PyModule.impAttr " + attr + " " + name + " " + fullName);
        if (path == Py.None) {
            /* disabled:
            ret = imp.loadFromClassLoader(
            (name+'.'+attr).intern(),
            Py.getSystemState().getClassLoader());
             */
        }
        else if (path instanceof PyList) {
            ret = imp.find_module(attr, fullName, (PyList)path);
        }
        else {
            throw Py.TypeError("__path__ must be list or None");
        }

        if (ret == null) {
            ret = PySystemState.packageManager.lookupName(fullName);
        }

        if (ret != null) {
            // Allow a package component to change its own meaning
            PyObject tmp = Py.getSystemState().modules.__finditem__(fullName);
            if (tmp != null) ret = tmp;
            __dict__.__setitem__(attr, ret);
            return ret;
        }

        return null;
    }

    public PyObject __findattr__(String attr) {
        return module___findattr__(attr);
    }

    final PyObject module___findattr__(String attr) {
        PyObject ret;

        if (__dict__ != null) {
          ret = __dict__.__finditem__(attr);
          if (ret != null) return ret;
        }

        ret = super.__findattr__(attr);
        if (ret != null) return ret;

        if (__dict__ == null) {
            return null;
        }

        PyObject pyname = __dict__.__finditem__("__name__");
        if (pyname == null) return null;

        return impHook(pyname.__str__().toString()+'.'+attr);
    }

    public void __setattr__(String attr, PyObject value) {
        module___setattr__(attr, value);
    }

    final void module___setattr__(String attr, PyObject value) {
        if (attr != "__dict__")
            ensureDict();
        super.__setattr__(attr, value);
    }

    public void __delattr__(String attr) {
        module___delattr__(attr);
    }

    final void module___delattr__(String attr) {
        super.__delattr__(attr);
    }

    public String toString()  {
        return module_toString();
    }

    final String module_toString()  {
        PyObject name = null;
        PyObject filename = null;
        if (__dict__ != null) {
          name = __dict__.__finditem__("__name__");
          filename = __dict__.__finditem__("__file__");
        }
        if (name == null)
            name = new PyString("?");
        if (filename == null)
            filename = new PyString("(built-in)");
        else
            filename = new PyString("from '" + filename + "'");
        return "<module '" + name + "' " + filename + ">";
    }

    public PyObject __dir__() {
        if (__dict__ == null)
            throw Py.TypeError("module.__dict__ is not a dictionary");
        return __dict__.invoke("keys");
    }

    private void ensureDict() {
        if (__dict__ == null)
            __dict__ = new PyStringMap();
    }

    static private PyObject silly_list = null;

    private static PyObject impHook(String name) {
        if (silly_list == null) {
            silly_list = new PyTuple(new PyString[] {
                                     Py.newString("__doc__"),});
        }
        try {
            return __builtin__.__import__(name, null, null, silly_list);
        } catch(PyException e) {
            if (Py.matchException(e, Py.ImportError)) {
                return null;
            }
            throw e;
        }
    }

}
