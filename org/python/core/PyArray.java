// Copyright (c) Corporation for National Research Initiatives
package org.python.core;
import java.lang.reflect.Array;

/**
 * A wrapper class around native java arrays.
 *
 * Instances of PyArray are created either by java functions or
 * directly by the jarray module.
 * <p>
 * See also the jarray module.
 */

public class PyArray extends PySequence {
    Object data;
    Class type;

    public static PyClass __class__;
    public PyArray(Class type, Object data) {
        super(__class__);
        this.type = type;
        this.data = data;
    }

    public PyArray(Class type, int n) {
        this(type, Array.newInstance(type, n));
    }

    public static PyArray zeros(int n, Class ctype) {
        return new PyArray(ctype, n);
    }

    public static PyArray array(PyObject seq, Class ctype) {
        PyArray array = new PyArray(ctype, seq.__len__());
        PyObject o;
        for(int i=0; (o=seq.__finditem__(i)) != null; i++) {
            array.set(i, o);
        }
        return array;
    }

    public static Class char2class(char type) {
        switch (type) {
        case 'z':
            return Boolean.TYPE;
        case 'c':
            return Character.TYPE;
        case 'b':
            return Byte.TYPE;
        case 'h':
            return Short.TYPE;
        case 'i':
            return Integer.TYPE;
        case 'l':
            return Long.TYPE;
        case 'f':
            return Float.TYPE;
        case 'd':
            return Double.TYPE;
        default:
            throw Py.ValueError("typecode must be in [zcbhilfd]");
        }
    }

    public Object __tojava__(Class c) {
        if (c == Object.class ||
            (c.isArray() && c.getComponentType().isAssignableFrom(type)))
        {
            return data;
        }
        if (c.isInstance(this)) return this;
        return Py.NoConversion;
    }

    public int __len__() {
        return Array.getLength(data);
    }

    protected PyObject get(int i) {
        return Py.java2py(Array.get(data, i));
    }

    protected PyObject getslice(int start, int stop, int step) {
        if (step != 1)
            throw Py.TypeError("step != 1 not implemented yet");
        int n = stop-start;
        PyArray ret = new PyArray(type, n);
        System.arraycopy(data, start, ret.data, 0, n);
        return ret;
    }

    protected PyObject repeat(int count) {
        throw Py.TypeError("can't apply '*' to arrays");
    }

    protected void del(int i) {
        throw Py.TypeError("can't remove from array");
    }
    protected void delRange(int start, int stop, int step) {
        throw Py.TypeError("can't remove from array");
    }

    protected void set(int i, PyObject value) {
        Object o = Py.tojava(value, type);
        Array.set(data, i, o);
    }

    protected void setslice(int start, int stop, int step, PyObject value) {
        if (value instanceof PyString && type == Character.TYPE) {
            char[] chars = value.toString().toCharArray();
            if (chars.length == stop-start && step == 1) {
                System.arraycopy(chars, 0, data, start, chars.length);
            } else {
                throw Py.ValueError("invalid bounds for setting from string");
            }

        } else {
            if (value instanceof PyString && type == Byte.TYPE) {
                byte[] chars = value.toString().getBytes();
                if (chars.length == stop-start && step == 1) {
                    System.arraycopy(chars, 0, data, start, chars.length);
                } else {
                    throw Py.ValueError(
                        "invalid bounds for setting from string");
                }
            } else {
                throw Py.TypeError(
                    "can't set slice in array (except from string)");
            }
        }

    }

    public String tostring() {
        if (type == Character.TYPE) {
            return new String((char[])data);
        }
        if (type == Byte.TYPE) {
            return new String((byte[])data, 0);
        }
        throw Py.TypeError(
            "can only convert arrays of byte and char to string");
    }

    public PyString __repr__() {
        StringBuffer buf = new StringBuffer("array([");
        for (int i=0; i<__len__()-1; i++) {
            buf.append(get(i).__repr__().toString());
            buf.append(", ");
        }
        if (__len__() > 0)
            buf.append(get(__len__()-1).__repr__().toString());
        buf.append("], ");
        buf.append(type.getName());
        buf.append(")");

        return new PyString(buf.toString());
    }
}
