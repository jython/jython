// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * A builtin python dictionary.
 */

public class PyDictionary extends PyObject {

    /* type info */

    public static final String exposed_name="dict";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___ne__ extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed___ne__(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___ne__((PyDictionary)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.dict___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyDictionary self=(PyDictionary)gself;
                PyObject ret=self.dict___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ne__",new PyMethodDescr("__ne__",PyDictionary.class,1,1,new exposed___ne__(null,null)));
        class exposed___eq__ extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed___eq__(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___eq__((PyDictionary)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.dict___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyDictionary self=(PyDictionary)gself;
                PyObject ret=self.dict___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__eq__",new PyMethodDescr("__eq__",PyDictionary.class,1,1,new exposed___eq__(null,null)));
        class exposed___cmp__ extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed___cmp__(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___cmp__((PyDictionary)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                int ret=self.dict___cmp__(arg0);
                if (ret==-2) {
                    throw Py.TypeError("dict"+".__cmp__(x,y) requires y to be '"+"dict"+"', not a '"+(arg0).getType().fastGetName()+"'");
                }
                return Py.newInteger(ret);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyDictionary self=(PyDictionary)gself;
                int ret=self.dict___cmp__(arg0);
                if (ret==-2) {
                    throw Py.TypeError("dict"+".__cmp__(x,y) requires y to be '"+"dict"+"', not a '"+(arg0).getType().fastGetName()+"'");
                }
                return Py.newInteger(ret);
            }

        }
        dict.__setitem__("__cmp__",new PyMethodDescr("__cmp__",PyDictionary.class,1,1,new exposed___cmp__(null,null)));
        class exposed___getitem__ extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed___getitem__(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___getitem__((PyDictionary)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.dict___finditem__(arg0);
                if (ret==null)
                    throw Py.KeyError(arg0.toString());
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyDictionary self=(PyDictionary)gself;
                PyObject ret=self.dict___finditem__(arg0);
                if (ret==null)
                    throw Py.KeyError(arg0.toString());
                return ret;
            }

        }
        dict.__setitem__("__getitem__",new PyMethodDescr("__getitem__",PyDictionary.class,1,1,new exposed___getitem__(null,null)));
        class exposed_get extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed_get(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_get((PyDictionary)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                return self.dict_get(arg0,arg1);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyDictionary self=(PyDictionary)gself;
                return self.dict_get(arg0,arg1);
            }

            public PyObject __call__(PyObject arg0) {
                return self.dict_get(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyDictionary self=(PyDictionary)gself;
                return self.dict_get(arg0);
            }

        }
        dict.__setitem__("get",new PyMethodDescr("get",PyDictionary.class,1,2,new exposed_get(null,null)));
        class exposed_setdefault extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed_setdefault(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_setdefault((PyDictionary)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                return self.dict_setdefault(arg0,arg1);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyDictionary self=(PyDictionary)gself;
                return self.dict_setdefault(arg0,arg1);
            }

            public PyObject __call__(PyObject arg0) {
                return self.dict_setdefault(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyDictionary self=(PyDictionary)gself;
                return self.dict_setdefault(arg0);
            }

        }
        dict.__setitem__("setdefault",new PyMethodDescr("setdefault",PyDictionary.class,1,2,new exposed_setdefault(null,null)));
        class exposed_popitem extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed_popitem(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_popitem((PyDictionary)self,info);
            }

            public PyObject __call__() {
                return self.dict_popitem();
            }

            public PyObject inst_call(PyObject gself) {
                PyDictionary self=(PyDictionary)gself;
                return self.dict_popitem();
            }

        }
        dict.__setitem__("popitem",new PyMethodDescr("popitem",PyDictionary.class,0,0,new exposed_popitem(null,null)));
        class exposed_has_key extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed_has_key(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_has_key((PyDictionary)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newBoolean(self.dict_has_key(arg0));
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyDictionary self=(PyDictionary)gself;
                return Py.newBoolean(self.dict_has_key(arg0));
            }

        }
        dict.__setitem__("has_key",new PyMethodDescr("has_key",PyDictionary.class,1,1,new exposed_has_key(null,null)));
        class exposed___contains__ extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed___contains__(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___contains__((PyDictionary)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newBoolean(self.dict___contains__(arg0));
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyDictionary self=(PyDictionary)gself;
                return Py.newBoolean(self.dict___contains__(arg0));
            }

        }
        dict.__setitem__("__contains__",new PyMethodDescr("__contains__",PyDictionary.class,1,1,new exposed___contains__(null,null)));
        class exposed___len__ extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed___len__(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___len__((PyDictionary)self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(self.dict___len__());
            }

            public PyObject inst_call(PyObject gself) {
                PyDictionary self=(PyDictionary)gself;
                return Py.newInteger(self.dict___len__());
            }

        }
        dict.__setitem__("__len__",new PyMethodDescr("__len__",PyDictionary.class,0,0,new exposed___len__(null,null)));
        class exposed___setitem__ extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed___setitem__(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___setitem__((PyDictionary)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                self.dict___setitem__(arg0,arg1);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyDictionary self=(PyDictionary)gself;
                self.dict___setitem__(arg0,arg1);
                return Py.None;
            }

        }
        dict.__setitem__("__setitem__",new PyMethodDescr("__setitem__",PyDictionary.class,2,2,new exposed___setitem__(null,null)));
        class exposed___delitem__ extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed___delitem__(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___delitem__((PyDictionary)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                self.dict___delitem__(arg0);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyDictionary self=(PyDictionary)gself;
                self.dict___delitem__(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("__delitem__",new PyMethodDescr("__delitem__",PyDictionary.class,1,1,new exposed___delitem__(null,null)));
        class exposed_keys extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed_keys(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_keys((PyDictionary)self,info);
            }

            public PyObject __call__() {
                return self.dict_keys();
            }

            public PyObject inst_call(PyObject gself) {
                PyDictionary self=(PyDictionary)gself;
                return self.dict_keys();
            }

        }
        dict.__setitem__("keys",new PyMethodDescr("keys",PyDictionary.class,0,0,new exposed_keys(null,null)));
        class exposed_update extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed_update(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_update((PyDictionary)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                self.dict_update(arg0);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyDictionary self=(PyDictionary)gself;
                self.dict_update(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("update",new PyMethodDescr("update",PyDictionary.class,1,1,new exposed_update(null,null)));
        class exposed_itervalues extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed_itervalues(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_itervalues((PyDictionary)self,info);
            }

            public PyObject __call__() {
                return self.dict_itervalues();
            }

            public PyObject inst_call(PyObject gself) {
                PyDictionary self=(PyDictionary)gself;
                return self.dict_itervalues();
            }

        }
        dict.__setitem__("itervalues",new PyMethodDescr("itervalues",PyDictionary.class,0,0,new exposed_itervalues(null,null)));
        class exposed_iteritems extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed_iteritems(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_iteritems((PyDictionary)self,info);
            }

            public PyObject __call__() {
                return self.dict_iteritems();
            }

            public PyObject inst_call(PyObject gself) {
                PyDictionary self=(PyDictionary)gself;
                return self.dict_iteritems();
            }

        }
        dict.__setitem__("iteritems",new PyMethodDescr("iteritems",PyDictionary.class,0,0,new exposed_iteritems(null,null)));
        class exposed_iterkeys extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed_iterkeys(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_iterkeys((PyDictionary)self,info);
            }

            public PyObject __call__() {
                return self.dict_iterkeys();
            }

            public PyObject inst_call(PyObject gself) {
                PyDictionary self=(PyDictionary)gself;
                return self.dict_iterkeys();
            }

        }
        dict.__setitem__("iterkeys",new PyMethodDescr("iterkeys",PyDictionary.class,0,0,new exposed_iterkeys(null,null)));
        class exposed_items extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed_items(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_items((PyDictionary)self,info);
            }

            public PyObject __call__() {
                return self.dict_items();
            }

            public PyObject inst_call(PyObject gself) {
                PyDictionary self=(PyDictionary)gself;
                return self.dict_items();
            }

        }
        dict.__setitem__("items",new PyMethodDescr("items",PyDictionary.class,0,0,new exposed_items(null,null)));
        class exposed_values extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed_values(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_values((PyDictionary)self,info);
            }

            public PyObject __call__() {
                return self.dict_values();
            }

            public PyObject inst_call(PyObject gself) {
                PyDictionary self=(PyDictionary)gself;
                return self.dict_values();
            }

        }
        dict.__setitem__("values",new PyMethodDescr("values",PyDictionary.class,0,0,new exposed_values(null,null)));
        class exposed_clear extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed_clear(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_clear((PyDictionary)self,info);
            }

            public PyObject __call__() {
                self.dict_clear();
                return Py.None;
            }

            public PyObject inst_call(PyObject gself) {
                PyDictionary self=(PyDictionary)gself;
                self.dict_clear();
                return Py.None;
            }

        }
        dict.__setitem__("clear",new PyMethodDescr("clear",PyDictionary.class,0,0,new exposed_clear(null,null)));
        class exposed_copy extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed_copy(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_copy((PyDictionary)self,info);
            }

            public PyObject __call__() {
                return self.dict_copy();
            }

            public PyObject inst_call(PyObject gself) {
                PyDictionary self=(PyDictionary)gself;
                return self.dict_copy();
            }

        }
        dict.__setitem__("copy",new PyMethodDescr("copy",PyDictionary.class,0,0,new exposed_copy(null,null)));
        class exposed___hash__ extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed___hash__(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___hash__((PyDictionary)self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(self.dict_hashCode());
            }

            public PyObject inst_call(PyObject gself) {
                PyDictionary self=(PyDictionary)gself;
                return Py.newInteger(self.dict_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyDictionary.class,0,0,new exposed___hash__(null,null)));
        class exposed___repr__ extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed___repr__(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___repr__((PyDictionary)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.dict_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PyDictionary self=(PyDictionary)gself;
                return new PyString(self.dict_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyDictionary.class,0,0,new exposed___repr__(null,null)));
        class exposed___iter__ extends PyBuiltinFunctionNarrow {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed___iter__(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___iter__((PyDictionary)self,info);
            }

            public PyObject __call__() {
                return self.dict___iter__();
            }

            public PyObject inst_call(PyObject gself) {
                PyDictionary self=(PyDictionary)gself;
                return self.dict___iter__();
            }

        }
        dict.__setitem__("__iter__",new PyMethodDescr("__iter__",PyDictionary.class,0,0,new exposed___iter__(null,null)));
        class exposed___init__ extends PyBuiltinFunctionWide {

            private PyDictionary self;

            public PyObject getSelf() {
                return self;
            }

            exposed___init__(PyDictionary self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___init__((PyDictionary)self,info);
            }

            public PyObject inst_call(PyObject self,PyObject[]args) {
                return inst_call(self,args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args) {
                return __call__(args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args,String[]keywords) {
                self.dict_init(args,keywords);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject[]args,String[]keywords) {
                PyDictionary self=(PyDictionary)gself;
                self.dict_init(args,keywords);
                return Py.None;
            }

        }
        dict.__setitem__("__init__",new PyMethodDescr("__init__",PyDictionary.class,-1,-1,new exposed___init__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyDictionary.class,"__new__",-1,-1) {

        public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
            PyDictionary newobj;
            if (for_type==subtype) {
                newobj=new PyDictionary();
                if (init)
                    newobj.dict_init(args,keywords);
            } else {
                newobj=new PyDictionaryDerived(subtype);
            }
            return newobj;
        }
        });
    }

    protected Hashtable table;

    /**
     * Create an empty dictionary.
     */
    public PyDictionary() {
        this(new Hashtable());
    }

    /**
     * For derived types
     * @param subtype
     */
    public PyDictionary(PyType subtype) {
        super(subtype);
        table = new Hashtable();
    }

    /**
     * Create an new dictionary which is based on the hashtable.
     * @param t  the hashtable used. The supplied hashtable is used as
     *           is and must only contain PyObject key:value pairs.
     */
    public PyDictionary(Hashtable t) {
        table = t;
    }

    /**
     * Create a new dictionary with the element as content.
     * @param elements The initial elements that is inserted in the
     *                 dictionary. Even numbered elements are keys,
     *                 odd numbered elements are values.
     */
    public PyDictionary(PyObject elements[]) {
        this();
        for (int i = 0; i < elements.length; i+=2) {
            table.put(elements[i], elements[i+1]);
        }
    }

    final void dict_init(PyObject[] args,String[] kwds) {
        int nargs = args.length - kwds.length;
        if (nargs > 1)
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(
                    nargs,
                    false,
                    exposed_name,
                    0,
                    1);
        if (nargs == 1) {
            PyObject src = args[0];
            if (src.__findattr__("keys") != null)
                this.update(src);
            else {
                PyObject pairs = Py.iter(src, "iteration over non-sequence");
                PyObject pair;
                int cnt = 0;
                for (; (pair = pairs.__iternext__()) != null; cnt++) {
                    try {
                        pair = PySequence.fastSequence(pair, "");
                    } catch(PyException e) {
                        if (Py.matchException(e, Py.TypeError))
                            throw Py.TypeError("cannot convert dictionary update "+
                                    "sequence element #"+cnt+" to a sequence");
                        throw e;
                    }
                    int n;
                    if ((n = pair.__len__()) != 2) {
                        throw Py.TypeError("dictionary update sequence element #"+cnt+
                             " has length "+n+"; 2 is required");
                    }
                    this.__setitem__(pair.__getitem__(0),pair.__getitem__(1));
                }
            }
            for (int i=0; i < kwds.length; i++) {
                this.__setitem__(kwds[i],args[nargs+i]);
            }
        }
    }


    public String safeRepr() throws PyIgnoreMethodTag {
        return "'dict' object";
    }

    public int __len__() {
        return dict___len__();
    }

    final int dict___len__() {
        return table.size();
    }

    public boolean __nonzero__() {
        return dict___nonzero__();
    }

    final boolean dict___nonzero__() {
        return table.size() != 0;
    }

    public PyObject __finditem__(int index) {
        throw Py.TypeError("loop over non-sequence");
    }

    public PyObject __finditem__(PyObject key) {
        return dict___finditem__(key);
    }

    final PyObject dict___finditem__(PyObject key) {
        return (PyObject)table.get(key);
    }

    public void __setitem__(PyObject key, PyObject value) {
        dict___setitem__(key,value);
    }

    final void dict___setitem__(PyObject key, PyObject value)  {
        table.put(key, value);
    }

    public void __delitem__(PyObject key) {
        dict___delitem__(key);
    }

    final void dict___delitem__(PyObject key) {
        Object ret = table.remove(key);
        if (ret == null)
            throw Py.KeyError(key.toString());
    }

    public PyObject __iter__() {
        return dict___iter__();
    }

    final PyObject dict___iter__() {
        return new PyDictionaryIter(this, table.keys(), PyDictionaryIter.KEYS);
    }

    public String toString() {
        return dict_toString();
    }

    final String dict_toString() {
        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) {
            return "{...}";
        }

        java.util.Enumeration ek = table.keys();
        java.util.Enumeration ev = table.elements();
        int n = table.size();
        StringBuffer buf = new StringBuffer("{");

        for(int i=0; i<n; i++) {
            buf.append(((PyObject)ek.nextElement()).__repr__().toString());
            buf.append(": ");
            buf.append(((PyObject)ev.nextElement()).__repr__().toString());
            if (i < n-1)
                buf.append(", ");
        }
        buf.append("}");

        ts.exitRepr(this);
        return buf.toString();
    }

    public PyObject __eq__(PyObject ob_other) {
        return dict___eq__(ob_other);
    }

    final PyObject dict___eq__(PyObject ob_other) {
        if (ob_other.getType() != getType())
            return null;

        PyDictionary other = (PyDictionary)ob_other;
        int an = table.size();
        int bn = other.table.size();
        if (an != bn)
            return Py.Zero;

        PyList akeys = keys();
        for (int i=0; i<an; i++) {
            PyObject akey = akeys.get(i);
            PyObject bvalue = other.__finditem__(akey);
            if (bvalue == null)
                return Py.Zero;
            PyObject avalue = __finditem__(akey);
            if (!avalue._eq(bvalue).__nonzero__())
                return Py.Zero;
        }
        return Py.One;
    }

    public PyObject __ne__(PyObject ob_other) {
        return dict___ne__(ob_other);
    }

    final PyObject dict___ne__(PyObject ob_other) {
        PyObject eq_result = __eq__(ob_other);
        if (eq_result == null) return null;
        return  eq_result == Py.One?Py.Zero:Py.One;
    }

    public int __cmp__(PyObject ob_other) {
        return dict___cmp__(ob_other);
    }

    final int dict___cmp__(PyObject ob_other) {
        if (ob_other.getType() != getType())
            return -2;

        PyDictionary other = (PyDictionary)ob_other;
        int an = table.size();
        int bn = other.table.size();
        if (an < bn) return -1;
        if (an > bn) return 1;

        PyList akeys = keys();
        PyList bkeys = other.keys();

        akeys.sort();
        bkeys.sort();

        for (int i=0; i<bn; i++) {
            PyObject akey = akeys.get(i);
            PyObject bkey = bkeys.get(i);
            int c = akey._cmp(bkey);
            if (c != 0)
                return c;

            PyObject avalue = __finditem__(akey);
            PyObject bvalue = other.__finditem__(bkey);
            c = avalue._cmp(bvalue);
            if (c != 0)
                return c;
        }
        return 0;
    }

    /**
     * Return true if the key exist in the dictionary.
     */
    public boolean has_key(PyObject key) {
        return dict_has_key(key);
    }

    final boolean dict_has_key(PyObject key) {
        return table.containsKey(key);
    }

    final boolean dict___contains__(PyObject o) {
        return dict_has_key(o);
    }

    /**
     * Return this[key] if the key exists in the mapping, default_object
     * is returned otherwise.
     *
     * @param key            the key to lookup in the dictionary.
     * @param default_object the value to return if the key does not
     *                       exists in the mapping.
     */
    public PyObject get(PyObject key, PyObject default_object) {
        return dict_get(key,default_object);
    }

    final PyObject dict_get(PyObject key, PyObject default_object) {
        PyObject o = __finditem__(key);
        if (o == null)
            return default_object;
        else
            return o;
    }

    /**
     * Return this[key] if the key exists in the mapping, None
     * is returned otherwise.
     *
     * @param key  the key to lookup in the dictionary.
     */
    public PyObject get(PyObject key) {
        return dict_get(key);
    }

    final PyObject dict_get(PyObject key) {
        return get(key, Py.None);
    }

    /**
     * Return a shallow copy of the dictionary.
     */
    public PyDictionary copy() {
        return dict_copy();
    }

    final PyDictionary dict_copy() {
        return new PyDictionary((Hashtable)table.clone());
    }

    /**
     * Remove all items from the dictionary.
     */
    public void clear() {
        dict_clear();
    }

    final void dict_clear() {
        table.clear();
    }

    /**
     * Insert all the key:value pairs from <code>d</code> into
     * this dictionary.
     */
    public void update(PyObject d) {
        dict_update(d);
    }

    final void dict_update(PyObject d) {
        if (d instanceof PyDictionary) {
            do_update((PyDictionary)d);
        } else if (d instanceof PyStringMap) {
            do_update(d,((PyStringMap)d).keys());
        } else {
            do_update(d,d.invoke("keys"));
        }

    }

    private void do_update(PyDictionary d) {
        Hashtable otable = d.table;

        java.util.Enumeration ek = otable.keys();
        java.util.Enumeration ev = otable.elements();
        int n = otable.size();

        for (int i=0; i<n; i++)
            table.put(ek.nextElement(), ev.nextElement());
    }

    private void do_update(PyObject d,PyObject keys) {
        PyObject iter = keys.__iter__();
        for (PyObject key; (key = iter.__iternext__()) != null; )
            __setitem__(key, d.__getitem__(key));
    }

    /**
     * Return this[key] if the key exist, otherwise insert key with
     * a None value and return None.
     *
     * @param key   the key to lookup in the dictionary.
     */
    public PyObject setdefault(PyObject key) {
        return dict_setdefault(key);
    }

    final PyObject dict_setdefault(PyObject key) {
        return setdefault(key, Py.None);
    }

    /**
     * Return this[key] if the key exist, otherwise insert key with
     * the value of failobj and return failobj
     *
     * @param key     the key to lookup in the dictionary.
     * @param failobj the default value to insert in the dictionary
     *                if key does not already exist.
     */
    public PyObject setdefault(PyObject key, PyObject failobj) {
        return dict_setdefault(key,failobj);
    }

    final PyObject dict_setdefault(PyObject key, PyObject failobj) {
        PyObject o = __finditem__(key);
        if (o == null)
            __setitem__(key, o = failobj);
        return o;
    }

    /**
     * Return a random (key, value) tuple pair and remove the pair
     * from the dictionary.
     */
    public PyObject popitem() {
        return dict_popitem();
    }

    final PyObject dict_popitem() {
        java.util.Enumeration keys = table.keys();
        if (!keys.hasMoreElements())
            throw Py.KeyError("popitem(): dictionary is empty");
        PyObject key = (PyObject) keys.nextElement();
        PyObject val = (PyObject) table.get(key);
        table.remove(key);
        return new PyTuple(new PyObject[] { key, val });
    }

    /**
     * Return a copy of the dictionarys list of (key, value) tuple
     * pairs.
     */
    public PyList items() {
        return dict_items();
    }

    final PyList dict_items() {
        java.util.Enumeration ek = table.keys();
        java.util.Enumeration ev = table.elements();
        int n = table.size();
        java.util.Vector l = new java.util.Vector(n);

        for (int i=0; i<n; i++)
            l.addElement(new PyTuple(new PyObject[] {
                (PyObject)ek.nextElement(), (PyObject)ev.nextElement()
            }));
        return new PyList(l);
    }

    /**
     * Return a copy of the dictionarys list of keys.
     */
    public PyList keys() {
        return dict_keys();
    }

    final PyList dict_keys() {
        java.util.Enumeration e = table.keys();
        int n = table.size();
        java.util.Vector l = new java.util.Vector(n);

        for (int i=0; i<n; i++)
            l.addElement(e.nextElement());
        return new PyList(l);
    }

    /**
     * Return a copy of the dictionarys list of values.
     */
    public PyList values() {
        return dict_values();
    }

    final PyList dict_values() {
        java.util.Enumeration e = table.elements();
        int n = table.size();
        java.util.Vector l = new java.util.Vector(n);

        for (int i=0; i<n; i++)
            l.addElement(e.nextElement());
        return new PyList(l);
    }

    /**
     * Return an interator over (key, value) pairs.
     */
    public PyObject iteritems() {
        return dict_iteritems();
    }

    final PyObject dict_iteritems() {
        return new PyDictionaryIter(this, table.keys(),
                    PyDictionaryIter.ITEMS);
    }

    /**
     * Return an interator over (key, value) pairs.
     */
    public PyObject iterkeys() {
        return dict_iterkeys();
    }

    final PyObject dict_iterkeys() {
        return new PyDictionaryIter(this, table.keys(),
                    PyDictionaryIter.KEYS);
    }

    /**
     * Return an interator over (key, value) pairs.
     */
    public PyObject itervalues() {
        return dict_itervalues();
    }

    final PyObject dict_itervalues() {
        return new PyDictionaryIter(this, table.keys(),
                    PyDictionaryIter.VALUES);
    }

    public int hashCode() {
        return dict_hashCode();
    }

    final int dict_hashCode() {
        throw Py.TypeError("unhashable type");
    }

}

class PyDictionaryIter extends PyIterator {
    public static final int KEYS = 0;
    public static final int VALUES = 1;
    public static final int ITEMS = 2;

    private PyObject dict;
    private Enumeration enumeration;
    private int type;

    public PyDictionaryIter(PyObject dict, Enumeration e, int type) {
        this.dict = dict;
        this.enumeration = e;
        this.type = type;
    }

    public PyObject __iternext__() {
        if (!enumeration.hasMoreElements())
            return null;
        PyObject key = (PyObject) enumeration.nextElement();
        switch (type) {
        case VALUES:
            return dict.__finditem__(key);
        case ITEMS:
            return new PyTuple(new PyObject[] { key, dict.__finditem__(key) });
        default: // KEYS
            return key;
        }
    }
}

