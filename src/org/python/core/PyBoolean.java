package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * A builtin python bool. 
 */
@ExposedType(name = "bool", isBaseType = false)
public class PyBoolean extends PyInteger {
    
    public static final PyType TYPE = PyType.fromClass(PyBoolean.class);
    
    @ExposedNew
    public static PyObject bool_new(PyNewWrapper new_,
                                    boolean init,
                                    PyType subtype,
                                    PyObject[] args,
                                    String[] keywords) {
        ArgParser ap = new ArgParser("bool", args, keywords, new String[] {"x"}, 0);
        PyObject x = ap.getPyObject(0, null);
        if (x == null) {
            return Py.False;
        }
        if (new_.for_type == subtype) {
            return x.__nonzero__() ? Py.True : Py.False;
        } else {
            return new PyBooleanDerived(subtype, x.__nonzero__());
        }
    }
    
    private boolean value;

    public PyBoolean(PyType subType, boolean v) {
        super(subType, v ? 1 : 0);
        value = v;
    }

    public PyBoolean(boolean v) {
        this(TYPE, v);
    }

    public int getValue() {
        return value ? 1 : 0;
    }

    public String toString() {
        return bool_toString();
    }

    @ExposedMethod(names = {"__str__", "__repr__"})
    final String bool_toString() {
        return value ? "True" : "False";
    }

    public int hashCode() {
        return bool___hash__();
    }

    @ExposedMethod
    final int bool___hash__() {
        return value ? 1 : 0;
    }

    public boolean __nonzero__() {
        return bool___nonzero__();
    }

    @ExposedMethod
    final boolean bool___nonzero__() {
        return value;
    }

    public Object __tojava__(Class c) {
        if (c == Boolean.TYPE || c == Boolean.class ||
            c == Object.class ) {
            return Boolean.valueOf(value);
        }
        if (c == Integer.TYPE || c == Number.class ||
            c == Integer.class) {
            return Integer.valueOf(value ? 1 : 0);
        }
        if (c == Byte.TYPE || c == Byte.class)
            return Byte.valueOf((byte)(value ? 1 : 0));
        if (c == Short.TYPE || c == Short.class)
            return Short.valueOf((short)(value ? 1 : 0));
        if (c == Long.TYPE || c == Long.class)
            return Long.valueOf(value ? 1 : 0);
        if (c == Float.TYPE || c == Float.class)
            return Float.valueOf(value ? 1 : 0);
        if (c == Double.TYPE || c == Double.class)
            return Double.valueOf(value ? 1 : 0);
        return super.__tojava__(c);
    }

    public PyObject __and__(PyObject right) {
        return bool___and__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
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

    @ExposedMethod(type = MethodType.BINARY)
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
    
    @ExposedMethod(type = MethodType.BINARY)
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

    @ExposedMethod
    final PyObject bool___neg__() {
        return Py.newInteger(value ? -1 : 0);
    }

    public PyObject __pos__() {
        return bool___pos__();
    }

    @ExposedMethod
    final PyObject bool___pos__() {
        return Py.newInteger(value ? 1 : 0);
    }

    public PyObject __abs__() {
        return bool___abs__();
    }

    @ExposedMethod
    final PyObject bool___abs__() {
        return Py.newInteger(value ? 1 : 0);
    }

    public long asLong(int index) {
        return value ? 1 : 0;
    }

    public int asInt(int index) {
        return value ? 1 : 0;
    }

    @Override
    public int asInt() {
        return value ? 1 : 0;
    }

}
