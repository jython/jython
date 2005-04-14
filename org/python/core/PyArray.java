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
public class PyArray extends PySequence implements Cloneable, ClassDictInit {

    protected Object data;
    protected Class type;
    protected String typecode;
    protected ArrayDelegate delegate;

    // PyArray can't extend anymore, so delegate
    private class ArrayDelegate extends AbstractArray {
        
        final PyArray pyArray;
        
        private ArrayDelegate(PyArray pyArray) {
            super((pyArray.data == null) ? 0 : Array.getLength(pyArray.data));
            this.pyArray = pyArray;
        }
        
        protected Object getArray() {
            return pyArray.data;
        }
        protected void setArray(Object array) {
            pyArray.data = array;
        }
        
        protected void makeInsertSpace(int index) {
            super.makeInsertSpace(index, 1);
        }
        
        protected void makeInsertSpace(int index, int length) {
            super.makeInsertSpace(index, length);
        }
        
        public void remove(int index) {
            super.remove(index);
        }
    }

    private PyArray() {
        // do nothing, shell instance
    }
    
    public PyArray(PyArray toCopy) {
        
        data = toCopy.delegate.copyArray();
        delegate = new ArrayDelegate(this);
        type = toCopy.type;
    }
    
    public PyArray(Class type, Object data) {
        this.type = type;
        this.data = data;
        delegate = new ArrayDelegate(this);
    }

    public PyArray(Class type, int n) {
        this(type, Array.newInstance(type, n));
    }

    public static PyArray zeros(int n, char typecode) {
        PyArray array = zeros(n, char2class(typecode));
        array.typecode = Character.toString(typecode);
        return array;
    }
    
    public static PyArray zeros(int n, Class ctype) {
        PyArray array = new PyArray(ctype, n);
        array.typecode = ctype.getName();
        return array;
    }

    public static PyArray array(PyObject seq, char typecode) {
        PyArray array = PyArray.array(seq, char2class(typecode));
        array.typecode = Character.toString(typecode);
        return array;
    }
    
    public static PyArray array(PyObject seq, Class ctype) {
        PyArray array = new PyArray(ctype, seq.__len__());
        array.typecode = ctype.getName();
        PyObject iter = seq.__iter__();
        PyObject item = null;
        for (int i = 0; (item = iter.__iternext__()) != null; i++) {
            array.set(i, item);
        }
        return array;
    }

    public Object getArray() throws PyIgnoreMethodTag {
        return delegate.copyArray();
    }
    
    /**
     * Finds the attribute.
     *
     * @param name the name of the attribute of interest
     * @return the value for the attribute of the specified name
     */
    public PyObject __findattr__(String name) {
        if("typecode".equals(name)) {
            return new PyString(getTypecode());
        }
        return super.__findattr__(name);
    }

    public static void classDictInit(PyObject dict) throws PyIgnoreMethodTag {
        PySequence.classDictInit(dict);
        dict.__setitem__("clone", null);
        dict.__setitem__("classDictInit", null);
    }

    public static Class char2class(char type) throws PyIgnoreMethodTag {
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
        return delegate.getSize();
    }

    protected PyObject get(int i) {
        return Py.java2py(Array.get(data, i));
    }

    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start) stop = start;
        int n = sliceLength(start, stop, step);

        PyArray ret = new PyArray(type, n);
        if (step == 1) {
            System.arraycopy(data, start, ret.data, 0, n);
            return ret;
        }
        for (int i = start, j = 0; j < n; i += step, j++) {
            Array.set(ret.data, j, Array.get(data, i));
        }
        return ret;
    }

    protected PyObject repeat(int count) {
        throw Py.TypeError("can't apply '*' to arrays");
    }

    protected void del(int i) {
        //Now the AbstractArray can support this:
        //throw Py.TypeError("can't remove from array");
        delegate.remove(i);
    }
    
    public PyObject count(PyObject value) {
        // note: cpython does not raise type errors based on item type
        int iCount = 0;
        for (int i = 0; i < delegate.getSize(); i++) {
            if (value.equals(Py.java2py(Array.get(data, i)))) iCount++;
        }
        return new PyInteger(iCount);
    }
    
    private int indexInternal(PyObject value) {
        // note: cpython does not raise type errors based on item type
        for (int i = 0; i < delegate.getSize(); i++) {
            if (value.equals(Py.java2py(Array.get(data, i)))) {
                return i;
            }
        }
     return -1;
    }

    public PyObject index(PyObject value) {
        
        int index = indexInternal(value);  
        if(index != -1) return new PyInteger(index);
        
        throw Py.ValueError("array.index(" + value + "): " + value + 
        " not found in array");
    }

    
    public void remove(PyObject value) {
        int index = indexInternal(value) ;
        if(index != -1) {
            delegate.remove(index);
            return;
        }

        throw Py.ValueError("array.remove(" + value + "): " 
                + value + " not found in array");
    }
    
    public PyObject __add__(PyObject other) {
        PyArray otherArr = null;
        if (!(other instanceof PyArray)) {            
            throw Py.TypeError("can only append another array to an array");
        }
        otherArr = (PyArray)other;
        if (!otherArr.type.equals(this.type)) {
            throw Py.TypeError(
                    "can only append arrays of the same type, " + 
                    "expected '" + this.type + ", found " + otherArr.type);
        }            
        PyArray ret = new PyArray(this);
        ret.delegate.appendArray(otherArr.delegate.copyArray());
        return ret;
    }
    
    public void append(PyObject value) {
        // Currently, this is asymmetric with extend, which
        // *will* do conversions like append(5.0) to an int array.
        // Also, cpython 2.2 will do the append coersion.  However, 
        // it is deprecated in cpython 2.3, so maybe we are just 
        // ahead of our time ;-)
        Object o = Py.tojava(value, type);
        int afterLast = delegate.getSize();
        delegate.makeInsertSpace(afterLast);
        Array.set(data, afterLast, o);
    }
    
    public void extend(PyObject array) {
        PyArray otherArr = null;
        if (!(array instanceof PyArray)) { 
            throw Py.TypeError("can only extend an array witn another array");
        }
        otherArr = (PyArray)array;
        if (!otherArr.type.equals(this.type)) {
            throw Py.TypeError(
                    "can only extend with an array of the same type, " + 
                    "expected '" + this.type + ", found " + otherArr.type);
        }            
        delegate.appendArray(otherArr.delegate.copyArray());
    }
    
    public void insert(int index, PyObject value) {
        delegate.makeInsertSpace(index);
        Array.set(data, index, Py.tojava(value, type));
    }
    
    public PyObject pop() {
        return pop(-1);
    }
    
    public PyObject pop(int index) {
        // todo: python-style error handling
        index = (index < 0) 
              ? delegate.getSize() + index
              : index;
        PyObject ret = Py.java2py(Array.get(data, index));
        delegate.remove(index);
        return ret;
    }
    
    protected void delRange(int start, int stop, int step) {
        // Now the AbstractArray can support this:
        //throw Py.TypeError("can't remove from array");
        if (step > 0 && stop < start) stop = start;
        
        if (step == 1) {
            delegate.remove(start, stop);
        } else {
            int n = sliceLength(start, stop, step);

            for (int i = start, j = 0; j < n; i += step, j++) {
                delegate.remove(i);
            }
        }
    }

    protected void set(int i, PyObject value) {
        Object o = Py.tojava(value, type);
        Array.set(data, i, o);
    }

    protected void setslice(int start, int stop, int step, PyObject value) {
        if(type == Character.TYPE && value instanceof PyString) {
            char[] chars = null;
          //if (value instanceof PyString) {
              if (step != 1) {
                  throw Py.ValueError(
                          "invalid bounds for setting from string");
              }
              chars = value.toString().toCharArray();
  
          //} 
//           else if (value instanceof PyArray && 
//                      ((PyArray)value).type == Character.TYPE) {
//               PyArray other = (PyArray)value;
//               chars = (char[])other.delegate.copyArray();
//           }
          int insertSpace = chars.length - (stop - start);
          // adjust the array, either adding space or removing space
          if(insertSpace > 0) {
              delegate.makeInsertSpace(start, insertSpace);
          } else if (insertSpace < 0) {
              delegate.remove(start, -insertSpace + start - 1);
          }
          delegate.replaceSubArray(chars, start);        
             
        } else {
            if (value instanceof PyString && type == Byte.TYPE) {
                byte[] chars = value.toString().getBytes();
                if (chars.length == stop-start && step == 1) {
                    System.arraycopy(chars, 0, data, start, chars.length);
                } else {
                    throw Py.ValueError(
                        "invalid bounds for setting from string");
                }
            } else if(value instanceof PyArray){
                PyArray array = (PyArray)value;
                int insertSpace = array.delegate.getSize() - (stop - start);
             // adjust the array, either adding space or removing space
                // ...snapshot in case "value" is "this"
                Object arrayCopy = array.delegate.copyArray(); 
             if(insertSpace > 0) {
                 delegate.makeInsertSpace(start, insertSpace);
             } else if (insertSpace < 0) {
                 delegate.remove(start, -insertSpace + start - 1);
             }
             try {
                 delegate.replaceSubArray(arrayCopy, start);
             } catch (IllegalArgumentException e) {
                 throw Py.TypeError("Slice typecode '" + array.typecode + 
                         "' is not compatible with this array (typecode '" 
                         + this.typecode + "')");
             }
            }
        }

    }

    public void reverse() {
        // build a new reversed array and set this.data to it when done
        Object array = Array.newInstance(type, Array.getLength(data));
        for(int i = 0, lastIndex = delegate.getSize() - 1; i <= lastIndex; i++) {
            Array.set(array, lastIndex - i, Array.get(data, i));
        }
        data = array;
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
    
    public Object clone() { 
        return new PyArray(this);
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
    
    public String getTypecode() throws PyIgnoreMethodTag {
        return typecode;
    }
}
