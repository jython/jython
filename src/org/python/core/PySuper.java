package org.python.core;

public class PySuper extends PyObject implements PyType.Newstyle {
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="super";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        dict.__setitem__("__thisclass__",new PyGetSetDescr("__thisclass__",PySuper.class,"getThisClass",null));
        dict.__setitem__("__self__",new PyGetSetDescr("__self__",PySuper.class,"getSelf",null));
        dict.__setitem__("__self_class__",new PyGetSetDescr("__self_class__",PySuper.class,"getSelfClass",null));
        class exposed___getattribute__ extends PyBuiltinFunctionNarrow {

            private PySuper self;

            public PyObject getSelf() {
                return self;
            }

            exposed___getattribute__(PySuper self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___getattribute__((PySuper)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    String name=(arg0.asName(0));
                    PyObject ret=self.super___findattr__(name);
                    if (ret==null)
                        self.noAttributeError(name);
                    return ret;
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

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySuper self=(PySuper)gself;
                try {
                    String name=(arg0.asName(0));
                    PyObject ret=self.super___findattr__(name);
                    if (ret==null)
                        self.noAttributeError(name);
                    return ret;
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
        dict.__setitem__("__getattribute__",new PyMethodDescr("__getattribute__",PySuper.class,1,1,new exposed___getattribute__(null,null)));
        class exposed___get__ extends PyBuiltinFunctionNarrow {

            private PySuper self;

            public PyObject getSelf() {
                return self;
            }

            exposed___get__(PySuper self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___get__((PySuper)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                PyObject obj=(arg0==Py.None)?null:arg1;
                PyObject type=(arg1==Py.None)?null:arg0;
                return self.super___get__(obj,type);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PySuper self=(PySuper)gself;
                PyObject obj=(arg0==Py.None)?null:arg1;
                PyObject type=(arg1==Py.None)?null:arg0;
                return self.super___get__(obj,type);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject obj=(arg0==Py.None)?null:(null);
                PyObject type=((null)==Py.None)?null:arg0;
                return self.super___get__(obj,type);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySuper self=(PySuper)gself;
                PyObject obj=(arg0==Py.None)?null:(null);
                PyObject type=((null)==Py.None)?null:arg0;
                return self.super___get__(obj,type);
            }

        }
        dict.__setitem__("__get__",new PyMethodDescr("__get__",PySuper.class,1,2,new exposed___get__(null,null)));
        class exposed___init__ extends PyBuiltinFunctionWide {

            private PySuper self;

            public PyObject getSelf() {
                return self;
            }

            exposed___init__(PySuper self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___init__((PySuper)self,info);
            }

            public PyObject inst_call(PyObject self,PyObject[]args) {
                return inst_call(self,args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args) {
                return __call__(args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args,String[]keywords) {
                self.super_init(args,keywords);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject[]args,String[]keywords) {
                PySuper self=(PySuper)gself;
                self.super_init(args,keywords);
                return Py.None;
            }

        }
        dict.__setitem__("__init__",new PyMethodDescr("__init__",PySuper.class,-1,-1,new exposed___init__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PySuper.class,"__new__",-1,-1) {

                                                                                       public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                           PySuper newobj;
                                                                                           if (for_type==subtype) {
                                                                                               newobj=new PySuper();
                                                                                               if (init)
                                                                                                   newobj.super_init(args,keywords);
                                                                                           } else {
                                                                                               newobj=new PySuperDerived(subtype);
                                                                                           }
                                                                                           return newobj;
                                                                                       }

                                                                                   });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    private static final PyType SUPERTYPE = PyType.fromClass(PySuper.class);
   
    protected PyType thisClass;
    protected PyObject self;
    protected PyType selfClass;

    private PyType supercheck(PyType type,PyObject obj) {
        if (obj instanceof PyType && ((PyType)obj).isSubType(type)) {
            return (PyType)obj;
        }
        PyType obj_type = obj.getType();
        if (obj_type.isSubType(type))
            return obj_type;
        throw Py.TypeError("super(type, obj): "+
                "obj must be an instance or subtype of type");
    }
    
    public void super_init(PyObject[] args, String[] keywords) {
        if (keywords.length != 0
                || !PyBuiltinFunction.DefaultInfo.check(args.length, 1, 2)) {
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(args.length,
                    keywords.length != 0, "super", 1, 2);
        }
        if (!(args[0] instanceof PyType)) {
            throw Py.TypeError("super: argument 1 must be type");
        }
        PyType type = (PyType)args[0];
        PyObject obj = null;
        PyType obj_type = null;
        if (args.length == 2 && args[1] != Py.None)
            obj = args[1];
        if (obj != null) {
            obj_type = supercheck(type, obj);
        }
        this.thisClass = type;
        this.self = obj;
        this.selfClass = obj_type;
    }
    
    public PySuper() {
        this(SUPERTYPE);
    }

    public PySuper(PyType subType) {
        super(subType);
    }
    
    public PyObject getSelf() {
        return self;
    }
    public PyType getSelfClass() {
        return selfClass;
    }
    public PyType getThisClass() {
        return thisClass;
    }
    
    public PyObject __findattr__(String name) {
        return super___findattr__(name);
    }

    final PyObject super___findattr__(String name) {
        if (selfClass != null && name != "__class__") {
            PyObject descr = selfClass.super_lookup(thisClass, name);
            return descr.__get__(selfClass == self ? null : self, selfClass);
        }
        return super.__findattr__(name);
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        return super___get__(obj,type);
    }

    final PyObject super___get__(PyObject obj, PyObject type) { //xxx subtype case!
        if (obj == null || obj == Py.None || self != null)
            return this;
        PyType obj_type = supercheck(this.thisClass, obj);
        PySuper newsuper = new PySuper();
        newsuper.thisClass = this.thisClass;
        newsuper.self = obj;
        newsuper.selfClass = obj_type;
        return newsuper;
    }

}
