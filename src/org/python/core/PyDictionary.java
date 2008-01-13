// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * A builtin python dictionary.
 */

public class PyDictionary extends PyObject implements Map {

    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="dict";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___ne__ extends PyBuiltinMethodNarrow {

            exposed___ne__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___ne__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyDictionary)self).dict___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ne__",new PyMethodDescr("__ne__",PyDictionary.class,1,1,new exposed___ne__(null,null)));
        class exposed___eq__ extends PyBuiltinMethodNarrow {

            exposed___eq__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___eq__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyDictionary)self).dict___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__eq__",new PyMethodDescr("__eq__",PyDictionary.class,1,1,new exposed___eq__(null,null)));
        class exposed___lt__ extends PyBuiltinMethodNarrow {

            exposed___lt__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___lt__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyDictionary)self).dict___lt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__lt__",new PyMethodDescr("__lt__",PyDictionary.class,1,1,new exposed___lt__(null,null)));
        class exposed___gt__ extends PyBuiltinMethodNarrow {

            exposed___gt__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___gt__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyDictionary)self).dict___gt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__gt__",new PyMethodDescr("__gt__",PyDictionary.class,1,1,new exposed___gt__(null,null)));
        class exposed___ge__ extends PyBuiltinMethodNarrow {

            exposed___ge__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___ge__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyDictionary)self).dict___ge__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ge__",new PyMethodDescr("__ge__",PyDictionary.class,1,1,new exposed___ge__(null,null)));
        class exposed___le__ extends PyBuiltinMethodNarrow {

            exposed___le__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___le__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyDictionary)self).dict___le__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__le__",new PyMethodDescr("__le__",PyDictionary.class,1,1,new exposed___le__(null,null)));
        class exposed___cmp__ extends PyBuiltinMethodNarrow {

            exposed___cmp__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___cmp__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                int ret=((PyDictionary)self).dict___cmp__(arg0);
                if (ret==-2) {
                    throw Py.TypeError("dict"+".__cmp__(x,y) requires y to be '"+"dict"+"', not a '"+(arg0).getType().fastGetName()+"'");
                }
                return Py.newInteger(ret);
            }

        }
        dict.__setitem__("__cmp__",new PyMethodDescr("__cmp__",PyDictionary.class,1,1,new exposed___cmp__(null,null)));
        class exposed___getitem__ extends PyBuiltinMethodNarrow {

            exposed___getitem__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___getitem__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyDictionary)self).dict___finditem__(arg0);
                if (ret==null)
                    throw Py.KeyError(arg0.toString());
                return ret;
            }

        }
        dict.__setitem__("__getitem__",new PyMethodDescr("__getitem__",PyDictionary.class,1,1,new exposed___getitem__(null,null)));
        class exposed_fromkeys extends PyBuiltinMethodNarrow {

            exposed_fromkeys(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_fromkeys(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                return dict_fromkeys((PyType)getSelf(),arg0,arg1);
            }

            public PyObject __call__(PyObject arg0) {
                return dict_fromkeys((PyType)getSelf(),arg0);
            }

        }
        dict.__setitem__("fromkeys",new PyClassMethodDescr("fromkeys",PyDictionary.class,1,2,new exposed_fromkeys(null,null)));
        class exposed_get extends PyBuiltinMethodNarrow {

            exposed_get(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_get(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                return((PyDictionary)self).dict_get(arg0,arg1);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyDictionary)self).dict_get(arg0);
            }

        }
        dict.__setitem__("get",new PyMethodDescr("get",PyDictionary.class,1,2,new exposed_get(null,null)));
        class exposed_setdefault extends PyBuiltinMethodNarrow {

            exposed_setdefault(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_setdefault(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                return((PyDictionary)self).dict_setdefault(arg0,arg1);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyDictionary)self).dict_setdefault(arg0);
            }

        }
        dict.__setitem__("setdefault",new PyMethodDescr("setdefault",PyDictionary.class,1,2,new exposed_setdefault(null,null)));
        class exposed_pop extends PyBuiltinMethodNarrow {

            exposed_pop(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_pop(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                return((PyDictionary)self).dict_pop(arg0,arg1);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyDictionary)self).dict_pop(arg0);
            }

        }
        dict.__setitem__("pop",new PyMethodDescr("pop",PyDictionary.class,1,2,new exposed_pop(null,null)));
        class exposed_popitem extends PyBuiltinMethodNarrow {

            exposed_popitem(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_popitem(self,info);
            }

            public PyObject __call__() {
                return((PyDictionary)self).dict_popitem();
            }

        }
        dict.__setitem__("popitem",new PyMethodDescr("popitem",PyDictionary.class,0,0,new exposed_popitem(null,null)));
        class exposed_has_key extends PyBuiltinMethodNarrow {

            exposed_has_key(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_has_key(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newBoolean(((PyDictionary)self).dict_has_key(arg0));
            }

        }
        dict.__setitem__("has_key",new PyMethodDescr("has_key",PyDictionary.class,1,1,new exposed_has_key(null,null)));
        class exposed___contains__ extends PyBuiltinMethodNarrow {

            exposed___contains__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___contains__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newBoolean(((PyDictionary)self).dict___contains__(arg0));
            }

        }
        dict.__setitem__("__contains__",new PyMethodDescr("__contains__",PyDictionary.class,1,1,new exposed___contains__(null,null)));
        class exposed___len__ extends PyBuiltinMethodNarrow {

            exposed___len__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___len__(self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(((PyDictionary)self).dict___len__());
            }

        }
        dict.__setitem__("__len__",new PyMethodDescr("__len__",PyDictionary.class,0,0,new exposed___len__(null,null)));
        class exposed___setitem__ extends PyBuiltinMethodNarrow {

            exposed___setitem__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___setitem__(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                ((PyDictionary)self).dict___setitem__(arg0,arg1);
                return Py.None;
            }

        }
        dict.__setitem__("__setitem__",new PyMethodDescr("__setitem__",PyDictionary.class,2,2,new exposed___setitem__(null,null)));
        class exposed___delitem__ extends PyBuiltinMethodNarrow {

            exposed___delitem__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___delitem__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PyDictionary)self).dict___delitem__(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("__delitem__",new PyMethodDescr("__delitem__",PyDictionary.class,1,1,new exposed___delitem__(null,null)));
        class exposed_keys extends PyBuiltinMethodNarrow {

            exposed_keys(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_keys(self,info);
            }

            public PyObject __call__() {
                return((PyDictionary)self).dict_keys();
            }

        }
        dict.__setitem__("keys",new PyMethodDescr("keys",PyDictionary.class,0,0,new exposed_keys(null,null)));
        class exposed_update extends PyBuiltinMethodNarrow {

            exposed_update(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_update(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PyDictionary)self).dict_update(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("update",new PyMethodDescr("update",PyDictionary.class,1,1,new exposed_update(null,null)));
        class exposed_itervalues extends PyBuiltinMethodNarrow {

            exposed_itervalues(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_itervalues(self,info);
            }

            public PyObject __call__() {
                return((PyDictionary)self).dict_itervalues();
            }

        }
        dict.__setitem__("itervalues",new PyMethodDescr("itervalues",PyDictionary.class,0,0,new exposed_itervalues(null,null)));
        class exposed_iteritems extends PyBuiltinMethodNarrow {

            exposed_iteritems(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_iteritems(self,info);
            }

            public PyObject __call__() {
                return((PyDictionary)self).dict_iteritems();
            }

        }
        dict.__setitem__("iteritems",new PyMethodDescr("iteritems",PyDictionary.class,0,0,new exposed_iteritems(null,null)));
        class exposed_iterkeys extends PyBuiltinMethodNarrow {

            exposed_iterkeys(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_iterkeys(self,info);
            }

            public PyObject __call__() {
                return((PyDictionary)self).dict_iterkeys();
            }

        }
        dict.__setitem__("iterkeys",new PyMethodDescr("iterkeys",PyDictionary.class,0,0,new exposed_iterkeys(null,null)));
        class exposed_items extends PyBuiltinMethodNarrow {

            exposed_items(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_items(self,info);
            }

            public PyObject __call__() {
                return((PyDictionary)self).dict_items();
            }

        }
        dict.__setitem__("items",new PyMethodDescr("items",PyDictionary.class,0,0,new exposed_items(null,null)));
        class exposed_values extends PyBuiltinMethodNarrow {

            exposed_values(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_values(self,info);
            }

            public PyObject __call__() {
                return((PyDictionary)self).dict_values();
            }

        }
        dict.__setitem__("values",new PyMethodDescr("values",PyDictionary.class,0,0,new exposed_values(null,null)));
        class exposed_clear extends PyBuiltinMethodNarrow {

            exposed_clear(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_clear(self,info);
            }

            public PyObject __call__() {
                ((PyDictionary)self).dict_clear();
                return Py.None;
            }

        }
        dict.__setitem__("clear",new PyMethodDescr("clear",PyDictionary.class,0,0,new exposed_clear(null,null)));
        class exposed_copy extends PyBuiltinMethodNarrow {

            exposed_copy(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_copy(self,info);
            }

            public PyObject __call__() {
                return((PyDictionary)self).dict_copy();
            }

        }
        dict.__setitem__("copy",new PyMethodDescr("copy",PyDictionary.class,0,0,new exposed_copy(null,null)));
        class exposed___iter__ extends PyBuiltinMethodNarrow {

            exposed___iter__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___iter__(self,info);
            }

            public PyObject __call__() {
                return((PyDictionary)self).dict___iter__();
            }

        }
        dict.__setitem__("__iter__",new PyMethodDescr("__iter__",PyDictionary.class,0,0,new exposed___iter__(null,null)));
        class exposed___hash__ extends PyBuiltinMethodNarrow {

            exposed___hash__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___hash__(self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(((PyDictionary)self).dict_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyDictionary.class,0,0,new exposed___hash__(null,null)));
        class exposed___repr__ extends PyBuiltinMethodNarrow {

            exposed___repr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___repr__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyDictionary)self).dict_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyDictionary.class,0,0,new exposed___repr__(null,null)));
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
                ((PyDictionary)self).dict_init(args,keywords);
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
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    protected final Map<PyObject, PyObject> table;

    /**
     * Create an empty dictionary.
     */
    public PyDictionary() {
        this(new ConcurrentHashMap<PyObject, PyObject>());
    }

    /**
     * For derived types
     * @param subtype
     */
    public PyDictionary(PyType subtype) {
        super(subtype);
        table = new ConcurrentHashMap<PyObject, PyObject>();
    }

    /**
     * Create an new dictionary which is based on the hashtable.
     * @param t  the hashtable used. The supplied hashtable is used as
     *           is and must only contain PyObject key:value pairs.
     */
    public PyDictionary(Map<PyObject, PyObject> t) {
        table = new ConcurrentHashMap<PyObject, PyObject>(t);
    }

     /**
     * Create an new dictionary which is based on the map and for derived types.
     * @param subtype
     * @param t  the hashtable used. The supplied hashtable is used as
     *           is and must only contain PyObject key:value pairs.
     */
    public PyDictionary(PyType subtype, Map<PyObject, PyObject> t) {
        super(subtype);
        table = new ConcurrentHashMap<PyObject, PyObject>(t);
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

    final protected void dict_init(PyObject[] args,String[] kwds) {
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
                PyObject pairs = Py.iter(src, "iteration over non-sequence -not");
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
                        throw Py.ValueError("dictionary update sequence element #"+cnt+
                             " has length "+n+"; 2 is required");
                    }
                    dict___setitem__(pair.__getitem__(0), pair.__getitem__(1));
                }
            }
        }
        for(int i = 0; i < kwds.length; i++) {
            dict___setitem__(Py.newString(kwds[i]), args[nargs + i]);
        }        
    }
    public static PyObject fromkeys(PyObject keys) {
        return fromkeys(keys, null);
    }

    public static PyObject fromkeys(PyObject keys, PyObject value) {
        return dict_fromkeys(PyType.fromClass(PyDictionary.class), keys, value);
    }

    final static PyObject dict_fromkeys(PyType type, PyObject keys) {
        return dict_fromkeys(type, keys, null);
    }

    final static PyObject dict_fromkeys(PyType type, PyObject keys, PyObject value) {
        if (value == null) {
            value = Py.None;
        }
        PyObject d = type.__call__();
        for (PyObject o : keys.asIterable()) {
            d.__setitem__(o, value);
        }
        return d;
    }



    /* commenting this out -- PyObject.safeRepr() does the same thing, and this one
       messes up subclasses of dict.  XXX: delete all of this if this turns out okay.

    public String safeRepr() throws PyIgnoreMethodTag {
        return "'dict' object";
    }
    */

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
        return table.get(key);
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
        return iterkeys();
    }

    public String toString() {
        return dict_toString();
    }

    final String dict_toString() {
        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) {
            return "{...}";
        }

        StringBuffer buf = new StringBuffer("{");

        for (Entry<PyObject, PyObject> entry : table.entrySet()) {
            buf.append((entry.getKey()).__repr__().toString());
            buf.append(": ");
            buf.append((entry.getValue()).__repr__().toString());
            buf.append(", ");
        }
        if(buf.length() > 1){
            buf.delete(buf.length() - 2, buf.length());
        }
        buf.append("}");

        ts.exitRepr(this);
        return buf.toString();
    }

    public PyObject __eq__(PyObject ob_other) {
        return dict___eq__(ob_other);
    }

    final PyObject dict___eq__(PyObject ob_other) {               
        PyType thisType = getType();
        PyType otherType = ob_other.getType();
        if (otherType != thisType && !thisType.isSubType(otherType) 
                && !otherType.isSubType(thisType)) {
            return null;
        }
        PyDictionary other = (PyDictionary)ob_other;
        int an = table.size();
        int bn = other.table.size();
        if (an != bn)
            return Py.False;

        PyList akeys = keys();
        for (int i=0; i<an; i++) {
            PyObject akey = akeys.pyget(i);
            PyObject bvalue = other.__finditem__(akey);
            if (bvalue == null)
                return Py.False;
            PyObject avalue = __finditem__(akey);
            if (!avalue._eq(bvalue).__nonzero__())
                return Py.False;
        }
        return Py.True;
    }

    public PyObject __ne__(PyObject ob_other) {
        return dict___ne__(ob_other);
    }

    final PyObject dict___ne__(PyObject ob_other) {
        PyObject eq_result = __eq__(ob_other);
        if (eq_result == null) return null;
        return eq_result.__not__();
    }
    
    final PyObject dict___lt__(PyObject ob_other){
    	int result = __cmp__(ob_other);
    	if(result == -2){
    		return null;
    	}
    	return result < 0 ? Py.True : Py.False;
    }
    
    final PyObject dict___gt__(PyObject ob_other){
    	int result = __cmp__(ob_other);
    	if(result == -2){
    		return null;
    	}
    	return result > 0 ? Py.True : Py.False;
    }
    
    final PyObject dict___le__(PyObject ob_other){
    	int result = __cmp__(ob_other);
    	if(result == -2){
    		return null;
    	}
    	return result <= 0 ? Py.True : Py.False;
    }
    
    final PyObject dict___ge__(PyObject ob_other){
    	int result = __cmp__(ob_other);
    	if(result == -2){
    		return null;
    	}
    	return result >= 0 ? Py.True : Py.False;
    }

    public int __cmp__(PyObject ob_other) {
        return dict___cmp__(ob_other);
    }

    final int dict___cmp__(PyObject ob_other) {
        PyType thisType = getType();
        PyType otherType = ob_other.getType();
        if (otherType != thisType && !thisType.isSubType(otherType) 
                && !otherType.isSubType(thisType)) {
            return -2;
        }
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
            PyObject akey = akeys.pyget(i);
            PyObject bkey = bkeys.pyget(i);
            int c = akey._cmp(bkey);
            if (c != 0)
                return c;

            PyObject avalue = __finditem__(akey);
            PyObject bvalue = other.__finditem__(bkey);
            if(avalue == null){
                if(bvalue == null){
                    continue;
                }
                return -3;
            }else if(bvalue == null){
                return -3;
            }
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
        PyObject o = dict___finditem__(key);
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
        return new PyDictionary(table); // no need to clone()
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
        table.putAll(d.table);
    }

    private void do_update(PyObject d, PyObject keys) {
        for (PyObject key : keys.asIterable()) {
            __setitem__(key, d.__getitem__(key));
        }
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
        if (o == null) {
            __setitem__(key, o = failobj);
        }
        return o;
    }
    
    /**
     * Return a value based on key
     * from the dictionary.
     */
    public PyObject pop(PyObject key) {
        return dict_pop(key);
    }

    final PyObject dict_pop(PyObject key) {
        if (!table.containsKey(key)) {
            throw Py.KeyError("popitem(): dictionary is empty");
        }
        return table.remove(key);
    }

    /**
     * Return a value based on key
     * from the dictionary or default if that key is not found.
     */
    public PyObject pop(PyObject key, PyObject defaultValue) {
        return dict_pop(key, defaultValue);
    }

    final PyObject dict_pop(PyObject key, PyObject defaultValue) {
        if (!table.containsKey(key)) {
            return defaultValue;
        }
        return table.remove(key);
    }


    /**
     * Return a random (key, value) tuple pair and remove the pair
     * from the dictionary.
     */
    public PyObject popitem() {
        return dict_popitem();
    }

    final PyObject dict_popitem() {
        Iterator it = table.entrySet().iterator();
        if (!it.hasNext())
            throw Py.KeyError("popitem(): dictionary is empty");
        Entry entry = (Entry)it.next();
        PyTuple tuple = new PyTuple(
                (PyObject)entry.getKey(), (PyObject)entry.getValue());
        it.remove();
        return tuple;
    }

    /**
     * Return a copy of the dictionarys list of (key, value) tuple
     * pairs.
     */
    public PyList items() {
        return dict_items();
    }

    final PyList dict_items() {
        List<PyObject> list = new ArrayList<PyObject>(table.size());
        for (Entry<PyObject, PyObject> entry : table.entrySet()) {
            list.add(new PyTuple(entry.getKey(), entry.getValue()));
        }
        return new PyList(list);
    }

    /**
     * Return a copy of the dictionarys list of keys.
     */
    public PyList keys() {
        return dict_keys();
    }

    final PyList dict_keys() {
        return new PyList(new ArrayList(table.keySet()));
    }

    final PyList dict_values() {
        return new PyList(new ArrayList(table.values()));
    }

    /**
     * Return an interator over (key, value) pairs.
     */
    public PyObject iteritems() {
        return dict_iteritems();
    }

    final PyObject dict_iteritems() {
        return new ItemsIter(table.entrySet());
    }

    /**
     * Return an interator over (key, value) pairs.
     */
    public PyObject iterkeys() {
        return dict_iterkeys();
    }

    final PyObject dict_iterkeys() {
        return new ValuesIter(table.keySet());
    }

    /**
     * Return an interator over (key, value) pairs.
     */
    public PyObject itervalues() {
        return dict_itervalues();
    }

    final PyObject dict_itervalues() {
        return new ValuesIter(table.values());
    }

    public int hashCode() {
        return dict_hashCode();
    }

    final int dict_hashCode() {
        throw Py.TypeError("unhashable type");
    }

    public boolean isSequenceType() {
        return false;
    }

    class ValuesIter extends PyIterator {
        private final Iterator iterator;
        public ValuesIter(Collection c) {
            this.iterator = c.iterator();
        }

        public PyObject __iternext__() {
            if (!iterator.hasNext())
                return null;
            return (PyObject)iterator.next();
        }
    }

    class ItemsIter extends PyIterator {
        private Iterator iterator;
        public ItemsIter(Set s) {
            this.iterator = s.iterator();
        }
        
        public PyObject __iternext__() {
        if (!iterator.hasNext())
            return null;
        Entry entry =  (Entry)iterator.next();
        return new PyTuple(new PyObject[] 
        { (PyObject)entry.getKey(), (PyObject)entry.getValue() });
        }
    }

        /* The following methods implement the java.util.Map interface
    which allows PyDictionary to be passed to java methods that take
    java.util.Map as a parameter.  Basically, the Map methods are a
    wrapper around the PyDictionary's Map container stored in member
    variable 'table'. These methods simply convert java Object to
    PyObjects on insertion, and PyObject to Objects on retrieval. */

    /** @see java.util.Map#entrySet() */
    public Set entrySet() {
        return new PyMapEntrySet(table.entrySet());
    }

    /** @see java.util.Map#keySet() */
    public Set keySet() {
        return new PyMapKeyValSet(table.keySet());
    }

    /** Return a copy of the dictionarys list of values. */
    public Collection values() {
        return new PyMapKeyValSet(table.values());
    }

  
    /** @see java.util.Map#putAll(Map map) */
    public void putAll(Map map) {
        Iterator i = map.entrySet().iterator();
        while (i.hasNext()) {
            Entry entry = (Entry)i.next();
            table.put(Py.java2py(entry.getKey()), Py.java2py(entry.getValue()));
        }
    }

    /** @see java.util.Map#remove(Object key) */
    public Object remove(Object key) {
        return tojava(table.remove(Py.java2py(key)));
    }

    /** @see java.util.Map#put(Object key, Object value) */
    public Object put(Object key, Object value) {
        return tojava(table.put(Py.java2py(key), Py.java2py(value)));
    }

    /** @see java.util.Map#get(Object key) */
    public Object get(Object key) {
        return tojava(table.get(Py.java2py(key)));
    }

    /** @see java.util.Map#containsValue(Object key) */
    public boolean containsValue(Object value) {
        return table.containsValue(Py.java2py(value));
    }

    /** @see java.util.Map#containsValue(Object key) */
    public boolean containsKey(Object key) {
        return table.containsKey(Py.java2py(key));
    }
    
    /** @see java.util.Map#isEmpty(Object key) */
    public boolean isEmpty() {
        return table.isEmpty();
    }
    
    /** @see java.util.Map#size(Object key) */
    public int size() {
        return table.size();
    }

    /** Convert return values to java objects */
    static final Object tojava(Object val) {
        return val == null ? null : ((PyObject)val).__tojava__(Object.class);
    }
}

/** Basic implementation of Entry that just holds onto a key and value and returns them. */
class SimpleEntry<K, V> implements Entry<K, V> {
    
    public SimpleEntry(K key, V value){
        this.key = key;
        this.value = value;
    }
    
    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public boolean equals(Object o) {
        if(!(o instanceof Map.Entry)) {
            return false;
        }
        Map.Entry e = (Map.Entry)o;
        return eq(key, e.getKey()) && eq(value, e.getValue());
    }

    private static boolean eq(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public int hashCode() {
        return ((key == null) ? 0 : key.hashCode()) ^ ((value == null) ? 0 : value.hashCode());
    }

    public String toString() {
        return key + "=" + value;
    }

    public V setValue(V val) {
        throw new UnsupportedOperationException("Not supported by this view");
    }

    protected K key;

    protected V value;
}

/**
 * Wrapper for a Entry object returned from the java.util.Set
 * object which in turn is returned by the entrySet method of
 * java.util.Map.  This is needed to correctly convert from PyObjects
 * to java Objects.  Note that we take care in the equals and hashCode
 * methods to make sure these methods are consistent with Entry
 * objects that contain java Objects for a value so that on the java
 * side they can be reliable compared.
 */
class PyToJavaMapEntry extends SimpleEntry {

    /** Create a copy of the Entry with Py.None coverted to null */
    PyToJavaMapEntry(Entry entry) {
        super(entry.getKey(), entry.getValue());
    }
    
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Entry)) return false;
        Entry me = new JavaToPyMapEntry((Entry)o);
        return o.equals(me);
    }

    // tojava is called in getKey and getValue so the raw key and value can be
    // used to create a new SimpleEntry in getEntry.
    public Object getKey() {
        return PyDictionary.tojava(key);
    }
    
    public Object getValue() {
        return PyDictionary.tojava(value);
    }

    /**
     * @return an entry that returns the original values given to this entry.
     */
    public Entry getEntry() {
        return new SimpleEntry(key, value);
    }

}

/**
 * MapEntry Object for java MapEntry objects passed to the java.util.Set
 * interface which is returned by the entrySet method of PyDictionary.
 * Essentially like PyTojavaMapEntry, but going the other way converting java
 * Objects to PyObjects.
 */
class JavaToPyMapEntry extends SimpleEntry {
    
    public JavaToPyMapEntry(Entry entry) {
        super(Py.java2py(entry.getKey()), Py.java2py(entry.getValue()));
    }
}

/**
 *  Wrapper collection class for the keySet and values methods of
 *  java.util.Map
 */
class PyMapKeyValSet extends PyMapSet {
    
    PyMapKeyValSet(Collection coll) {
        super(coll);
    }

    Object toJava(Object o) {
        return PyDictionary.tojava(o);
    }
    
    Object toPython(Object o) {
        return Py.java2py(o);
    }
}

/**
 * Set wrapper for the java.util.EntrySet method. Entry
 * objects are wrapped further in JavaToPyMapEntry and
 * PyToJavaMapEntry.  Note - The set interface is reliable for
 * standard objects like strings and integers, but may be inconstant
 * for other types of objects since the equals method may return false
 * for Entry object that hold more elaborate PyObject types.
 * However, We insure that this iterface works when the Entry
 * object originates from a Set object retrieved from a PyDictionary.
 */
class PyMapEntrySet extends PyMapSet {

    PyMapEntrySet(Collection coll) {
        super(coll);
    }

    // We know that PyMapEntrySet will only contains Entrys, so
    // if the object being passed in is null or not a Entry, then
    // return null which will match nothing for remove and contains methods.
    Object toPython(Object o) {
        if(o == null || !(o instanceof Entry))
            return null;
        if(o instanceof PyToJavaMapEntry) {
            // Use the original entry from PyDictionary
            return ((PyToJavaMapEntry)o).getEntry();
        } else {
            return new JavaToPyMapEntry((Entry)o);
        }
    }

    Object toJava(Object o) {
        return new PyToJavaMapEntry((Entry)o);
    }
}

/**
 * PyMapSet serves as a wrapper around Set Objects returned by the
 * java.util.Map interface of PyDictionary. entrySet, values and
 * keySet methods return this type for the java.util.Map
 * implementation.  This class is necessary as a wrapper to convert
 * PyObjects to java Objects for methods that return values, and
 * convert Objects to PyObjects for methods that take values. The
 * translation is necessary to provide java access to jython
 * dictionary objects. This wrapper also provides the expected backing
 * functionality such that changes to the wrapper set or reflected in
 * PyDictionary.
 */
abstract class PyMapSet extends AbstractSet {

    PyMapSet(Collection coll) {
        this.coll = coll;
    }

    abstract Object toJava(Object obj);

    abstract Object toPython(Object obj);

    public int size() {
        return coll.size();
    }
    
    public boolean contains(Object o) {
        return coll.contains(toPython(o));
    }
    
     public boolean remove(Object o) {
         return coll.remove(toPython(o));
    }
    
    public void clear() {
        coll.clear();
    }

    // Iterator wrapper class returned by the PyMapSet iterator
    // method. We need this wrapper to return PyToJavaMapEntry objects
    // for the 'next()' method.
    class PySetIter implements Iterator {
        Iterator itr;

        PySetIter(Iterator itr) {
            this.itr = itr;
        }

        public boolean hasNext() {
            return itr.hasNext();
        }

        public Object next() {
            return toJava(itr.next());
        }
    
        public void remove() {
            itr.remove();
        }
    }
    
    public Iterator iterator() {
        return new PySetIter(coll.iterator());
    }
    
    private final Collection coll;
}




