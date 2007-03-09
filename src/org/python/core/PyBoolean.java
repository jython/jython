package org.python.core;

import java.io.Serializable;

public class PyBoolean extends PyInteger {

/**
 * A builtin python bool. 
 */
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="bool";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___abs__ extends PyBuiltinMethodNarrow {

            exposed___abs__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___abs__(self,info);
            }

            public PyObject __call__() {
                return((PyBoolean)self).bool___abs__();
            }

        }
        dict.__setitem__("__abs__",new PyMethodDescr("__abs__",PyBoolean.class,0,0,new exposed___abs__(null,null)));
        class exposed___neg__ extends PyBuiltinMethodNarrow {

            exposed___neg__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___neg__(self,info);
            }

            public PyObject __call__() {
                return((PyBoolean)self).bool___neg__();
            }

        }
        dict.__setitem__("__neg__",new PyMethodDescr("__neg__",PyBoolean.class,0,0,new exposed___neg__(null,null)));
        class exposed___pos__ extends PyBuiltinMethodNarrow {

            exposed___pos__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___pos__(self,info);
            }

            public PyObject __call__() {
                return((PyBoolean)self).bool___pos__();
            }

        }
        dict.__setitem__("__pos__",new PyMethodDescr("__pos__",PyBoolean.class,0,0,new exposed___pos__(null,null)));
        class exposed___and__ extends PyBuiltinMethodNarrow {

            exposed___and__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___and__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyBoolean)self).bool___and__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__and__",new PyMethodDescr("__and__",PyBoolean.class,1,1,new exposed___and__(null,null)));
        class exposed___or__ extends PyBuiltinMethodNarrow {

            exposed___or__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___or__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyBoolean)self).bool___or__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__or__",new PyMethodDescr("__or__",PyBoolean.class,1,1,new exposed___or__(null,null)));
        class exposed___xor__ extends PyBuiltinMethodNarrow {

            exposed___xor__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___xor__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyBoolean)self).bool___xor__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__xor__",new PyMethodDescr("__xor__",PyBoolean.class,1,1,new exposed___xor__(null,null)));
        class exposed___nonzero__ extends PyBuiltinMethodNarrow {

            exposed___nonzero__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___nonzero__(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyBoolean)self).bool___nonzero__());
            }

        }
        dict.__setitem__("__nonzero__",new PyMethodDescr("__nonzero__",PyBoolean.class,0,0,new exposed___nonzero__(null,null)));
        class exposed___repr__ extends PyBuiltinMethodNarrow {

            exposed___repr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___repr__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyBoolean)self).bool_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyBoolean.class,0,0,new exposed___repr__(null,null)));
        class exposed___str__ extends PyBuiltinMethodNarrow {

            exposed___str__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___str__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyBoolean)self).bool_toString());
            }

        }
        dict.__setitem__("__str__",new PyMethodDescr("__str__",PyBoolean.class,0,0,new exposed___str__(null,null)));
        class exposed___hash__ extends PyBuiltinMethodNarrow {

            exposed___hash__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___hash__(self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(((PyBoolean)self).bool_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyBoolean.class,0,0,new exposed___hash__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyBoolean.class,"__new__",-1,-1) {

                                                                                         public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                             return bool_new(this,init,subtype,args,keywords);
                                                                                         }

                                                                                     });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    
    public static PyObject bool_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("bool", args, keywords, new String[] { "x" }, 0);
        PyObject x = ap.getPyObject(0, null);
        if (x == null) {
        	return Py.False;
        }
        if (new_.for_type == subtype) {
            return x.__nonzero__() ? Py.True : Py.False;
        } else {
            return new PyBooleanDerived(subtype, x.__nonzero__());
        }
    } // xxx
    
    private static final PyType BOOLTYPE = PyType.fromClass(PyBoolean.class);
    
    private boolean value;

    public PyBoolean(PyType subType, boolean v) {
        super(subType, v ? 1 : 0);
        value = v;
    }

    public PyBoolean(boolean v) {
        this(BOOLTYPE, v);
    }

    public int getValue() {
        return value ? 1 : 0;
    }

    public String safeRepr() throws PyIgnoreMethodTag {
        return "'bool' object";
    }

    public String toString() {
        return bool_toString();
    }

    final String bool_toString() {
        return value ? "True" : "False";
    }

    public int hashCode() {
        return bool_hashCode();
    }

    final int bool_hashCode() {
        return value ? 1 : 0;
    }

    private static void err_ovf(String msg) {
        try {
            Py.OverflowWarning(msg);
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.OverflowWarning))
                throw Py.OverflowError(msg);
        }
    }

    public boolean __nonzero__() {
        return bool___nonzero__();
    }

    final boolean bool___nonzero__() {
        return value;
    }

    public Object __tojava__(Class c) {
        if (c == Integer.TYPE || c == Number.class ||
            c == Object.class || c == Integer.class ||
            c == Serializable.class)
        {
            return new Integer(value ? 1 : 0);
        }

        if (c == Boolean.TYPE || c == Boolean.class)
            return new Boolean(value);
        if (c == Byte.TYPE || c == Byte.class)
            return new Byte((byte)(value ? 1 : 0));
        if (c == Short.TYPE || c == Short.class)
            return new Short((short)(value ? 1 : 0));
        if (c == Long.TYPE || c == Long.class)
            return new Long(value ? 1 : 0);
        if (c == Float.TYPE || c == Float.class)
            return new Float(value ? 1 : 0);
        if (c == Double.TYPE || c == Double.class)
            return new Double(value ? 1 : 0);
        return super.__tojava__(c);
    }

    public PyObject __and__(PyObject right) {
        return bool___and__(right);
    }

    final PyObject bool___and__(PyObject right) {
    	if (right instanceof PyBoolean) {
	        return Py.newBoolean(value & ((PyBoolean)right).value);
    	} else if (right instanceof PyInteger) {
            return Py.newInteger(getValue() & ((PyInteger)right).getValue());        	
        } else {
	    	return null;
	    }
    }

    public PyObject __xor__(PyObject right) {
        return bool___xor__(right);
    }

    final PyObject bool___xor__(PyObject right) {
    	if (right instanceof PyBoolean) {
	        return Py.newBoolean(value ^ ((PyBoolean)right).value);
    	} else if (right instanceof PyInteger) {
            return Py.newInteger(getValue() ^ ((PyInteger)right).getValue());        	
        } else {
	    	return null;
	    }
    }

    public PyObject __or__(PyObject right) {
        return bool___or__(right);
    }

    final PyObject bool___or__(PyObject right) {
    	if (right instanceof PyBoolean) {
	        return Py.newBoolean(value | ((PyBoolean)right).value);
    	} else if (right instanceof PyInteger) {
            return Py.newInteger(getValue() | ((PyInteger)right).getValue());        	
        } else {
	    	return null;
	    }
    }

    public PyObject __neg__() {
        return bool___neg__();
    }

    final PyObject bool___neg__() {
        return Py.newInteger(value ? -1 : 0);
    }

    public PyObject __pos__() {
        return bool___pos__();
    }

    final PyObject bool___pos__() {
        return Py.newInteger(value ? 1 : 0);
    }

    public PyObject __abs__() {
        return bool___abs__();
    }

    final PyObject bool___abs__() {
        return Py.newInteger(value ? 1 : 0);
    }

    public long asLong(int index) {
        return value ? 1 : 0;
    }

    public int asInt(int index) {
        return value ? 1 : 0;
    }

}
