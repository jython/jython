// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;

import org.python.core.util.ByteSwapper;
import org.python.core.util.StringUtil;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * A wrapper class around native java arrays.
 * 
 * Instances of PyArray are created either by java functions or directly by the
 * jarray module.
 * <p>
 * See also the jarray module.
 */
@ExposedType(name = "array", base = PyObject.class)
public class PyArray extends PySequence implements Cloneable {

    public static final PyType TYPE = PyType.fromClass(PyArray.class);
    
    private Object data;

    private Class type;

    private String typecode;

    private ArrayDelegate delegate;

    // PyArray can't extend anymore, so delegate
    private class ArrayDelegate extends AbstractArray {


        private ArrayDelegate() {
            super(data == null ? 0 : Array.getLength(data));
        }

        protected Object getArray() {
            return data;
        }

        protected void setArray(Object array) {
            data = array;
        }

        @Override
        protected Object createArray(int size) {
            Class baseType = data.getClass().getComponentType();
            return Array.newInstance(baseType, size);
        }
    }
    
    public PyArray(PyType type){
        super(type);
    }

    public PyArray(PyArray toCopy) {
        data = toCopy.delegate.copyArray();
        delegate = new ArrayDelegate();
        type = toCopy.type;
    }

    public PyArray(Class type, Object data) {
        this.type = type;
        this.data = data;
        delegate = new ArrayDelegate();
    }

    public PyArray(Class type, int n) {
        this(type, Array.newInstance(type, n));
    }

    @ExposedMethod
    @ExposedNew
    final void array_init(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("array", args, kwds, new String[] {"typecode", "seq"}, 1);
        PyObject obj = ap.getPyObject(0);
        if (obj instanceof PyString) {
            String code = obj.toString();
            if (code.length() != 1) {
                throw Py.ValueError("typecode must be in [zcbhilfd]");
            }
            type = char2class(code.charAt(0));
            typecode = code;
        } else if (obj instanceof PyJavaClass) {
            type = ((PyJavaClass)obj).proxyClass;
            typecode = type.getName();
        }
        data = Array.newInstance(type, 0);
        delegate = new ArrayDelegate();
        PyObject seq = ap.getPyObject(1, null);
        if (seq == null) {
            return;
        }
        extendInternal(seq);
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

    /**
     * Create a PyArray storing <em>ctype</em> types and being initialised
     * with <em>initialiser</em>.
     * 
     * @param init
     *            an initialiser for the array - can be PyString or PySequence
     *            (including PyArray) or iterable type.
     * @param ctype
     *            <code>Class</code> type of the elements stored in the array.
     * @return a new PyArray
     */
    public static PyArray array(PyObject init, Class ctype) {
        PyArray array = new PyArray(ctype, 0);
        array.typecode = ctype.getName();
        array.extendInternal(init);
        return array;
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject array___ne__(PyObject o) {
        return seq___ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject array___eq__(PyObject o) {
        return seq___eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject array___lt__(PyObject o) {
        return seq___lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject array___le__(PyObject o) {
        return seq___le__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject array___gt__(PyObject o) {
        return seq___gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject array___ge__(PyObject o) {
        return seq___ge__(o);
    }

    @ExposedMethod
    final boolean array___contains__(PyObject o) {
        return object___contains__(o);
    }

    @ExposedMethod
    final void array___delitem__(PyObject index) {
        seq___delitem__(index);
    }

    @ExposedMethod
    final void array___setitem__(PyObject o, PyObject def) {
        seq___setitem__(o, def);
    }

    @ExposedMethod
    final PyObject array___getitem__(PyObject o) {
        PyObject ret = seq___finditem__(o);
        if(ret == null) {
            throw Py.IndexError("index out of range: " + o);
        }
        return ret;
    }

    @ExposedMethod
    final boolean array___nonzero__() {
        return seq___nonzero__();
    }

    @ExposedMethod
    public PyObject array___iter__() {
        return seq___iter__();
    }

    @ExposedMethod(defaults = "null")
    final PyObject array___getslice__(PyObject start, PyObject stop, PyObject step) {
        return seq___getslice__(start, stop, step);
    }

    @ExposedMethod(defaults = "null")
    final void array___setslice__(PyObject start, PyObject stop, PyObject step, PyObject value) {
        if(value == null) {
            value = step;
            step = null;
        }
        seq___setslice__(start, stop, step, value);
    }

    @ExposedMethod(defaults = "null")
    final void array___delslice__(PyObject start, PyObject stop, PyObject step) {
        seq___delslice__(start, stop, step);
    }

    /**
     * Adds (appends) two PyArrays together
     * 
     * @param other
     *            a PyArray to be added to the instance
     * @return the result of the addition as a new PyArray instance
     */
    public PyObject __add__(PyObject other) {
        PyArray otherArr = null;
        if(!(other instanceof PyArray)) {
            throw Py.TypeError("can only append another array to an array");
        }
        otherArr = (PyArray)other;
        if(!otherArr.type.equals(this.type)) {
            throw Py.TypeError("can only append arrays of the same type, "
                    + "expected '" + this.type + ", found " + otherArr.type);
        }
        PyArray ret = new PyArray(this);
        ret.delegate.appendArray(otherArr.delegate.copyArray());
        return ret;
    }

    /**
     * Length of the array
     * 
     * @return number of elements in the array
     */
    public int __len__() {
        return delegate.getSize();
    }

    /**
     * String representation of PyArray
     * 
     * @return string representation of PyArray
     */
    public PyString __repr__() {
        StringBuffer buf = new StringBuffer(128);
        buf.append("array(").append(class2char(type)).append(",[");
        for(int i = 0; i < __len__() - 1; i++) {
            buf.append(pyget(i).__repr__().toString());
            buf.append(", ");
        }
        if(__len__() > 0) {
            buf.append(pyget(__len__() - 1).__repr__().toString());
        }
        buf.append("]) ");
        return new PyString(buf.toString());
    }

    /**
     * 
     * @param c
     *            target <em>Class</em> for the conversion
     * @return Java object converted to required class type if possible.
     */
    public Object __tojava__(Class c) {
        if(c == Object.class
                || (c.isArray() && c.getComponentType().isAssignableFrom(type))) {
            return data;
        }
        if(c.isInstance(this))
            return this;
        return Py.NoConversion;
    }

    @ExposedMethod
    public final void array_append(PyObject value) {
        append(value);
    }

    /**
     * Append new value x to the end of the array.
     * 
     * @param value
     *            item to be appended to the array
     */
    public void append(PyObject value) {
        // Currently, this is asymmetric with extend, which
        // *will* do conversions like append(5.0) to an int array.
        // Also, cpython 2.2 will do the append coersion. However,
        // it is deprecated in cpython 2.3, so maybe we are just
        // ahead of our time ;-)
        int afterLast = delegate.getSize();
        delegate.makeInsertSpace(afterLast);
        try {
            set(afterLast, value);
        } catch(PyException e) {
            delegate.setSize(afterLast);
            throw new PyException(e.type, e.value);
        }
    }

    @ExposedMethod
    public void array_byteswap() {
        byteswap();
    }

    /**
     * "Byteswap" all items of the array. This is only supported for values
     * which are 1, 2, 4, or 8 bytes in size; for other types of values,
     * RuntimeError is raised. It is useful when reading data from a file
     * written on a machine with a different byte order.
     */
    public void byteswap() {
        // unknown type - throw RuntimeError
        if(getItemsize() == 0) {
            throw Py.RuntimeError("don't know how to byteswap this array type");
        }
        ByteSwapper.swap(data);
    }

    /**
     * Implementation of <em>Cloneable</em> interface.
     * 
     * @return copy of current PyArray
     */
    public Object clone() {
        return new PyArray(this);
    }

    /**
     * Converts a character code for the array type to a Java <code>Class</code>.
     * <p />
     * 
     * The following character codes and their native types are supported:<br />
     * <table>
     * <tr>
     * <td><strong>Type code</strong></td>
     * <td><strong>native type</strong></td>
     * </tr>
     * <tr>
     * <td>z</td>
     * <td><code>boolean</code></td>
     * </tr>
     * <tr>
     * <td>c</td>
     * <td><code>char</code></td>
     * </tr>
     * <tr>
     * <td>b</td>
     * <td><code>byte</code></td>
     * </tr>
     * <tr>
     * <td>h</td>
     * <td><code>short</code></td>
     * </tr>
     * <tr>
     * <td>i</td>
     * <td><code>int</code></td>
     * </tr>
     * <tr>
     * <td>l</td>
     * <td><code>long</code></td>
     * </tr>
     * <tr>
     * <td>f</td>
     * <td><code>float</code></td>
     * </tr>
     * <tr>
     * <td>d</td>
     * <td><code>double</code></td>
     * </tr>
     * </table>
     * <p />
     * 
     * @param type
     *            character code for the array type
     * 
     * @return <code>Class</code> of the native type
     */

    // promote H, I (unsigned int) to next larger size
    public static Class char2class(char type) throws PyIgnoreMethodTag {
        switch(type){
            case 'z':
                return Boolean.TYPE;
            case 'c':
                return Character.TYPE;
            case 'b':
                return Byte.TYPE;
            case 'h':
                return Short.TYPE;
            case 'H':
                return Integer.TYPE;
            case 'i':
                return Integer.TYPE;
            case 'I':
                return Long.TYPE;
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

    private static String class2char(Class cls) {
        if(cls.equals(Boolean.TYPE))
            return "'z'";
        else if(cls.equals(Character.TYPE))
            return "'c'";
        else if(cls.equals(Byte.TYPE))
            return "'b'";
        else if(cls.equals(Short.TYPE))
            return "'h'";
        else if(cls.equals(Integer.TYPE))
            return "'i'";
        else if(cls.equals(Long.TYPE))
            return "'l'";
        else if(cls.equals(Float.TYPE))
            return "'f'";
        else if(cls.equals(Double.TYPE))
            return "'d'";
        else
            return cls.getName();
    }
    
    @ExposedMethod
    public final int array_count(PyObject value) {
        // note: cpython does not raise type errors based on item type;
        int iCount = 0;
        for(int i = 0; i < delegate.getSize(); i++) {
            if(value.equals(Py.java2py(Array.get(data, i))))
                iCount++;
        }
        return iCount;
    }

    /**
     * Return the number of occurrences of x in the array.
     * 
     * @param value
     *            instances of the value to be counted
     * @return number of time value was found in the array.
     */
    public PyInteger count(PyObject value) {
        return Py.newInteger(array_count(value));
    }

    /**
     * Delete the element at position <em>i</em> from the array
     * 
     * @param i
     *            index of the item to be deleted from the array
     */
    protected void del(int i) {
        // Now the AbstractArray can support this:
        // throw Py.TypeError("can't remove from array");
        delegate.remove(i);
    }

    /**
     * Delete the slice defined by <em>start</em>, <em>stop</em> and
     * <em>step</em> from the array.
     * 
     * @param start
     *            starting index of slice
     * @param stop
     *            finishing index of slice
     * @param step
     *            stepping increment between start and stop
     */
    protected void delRange(int start, int stop, int step) {
        // Now the AbstractArray can support this:
        // throw Py.TypeError("can't remove from array");
        if(step > 0 && stop < start)
            stop = start;
        if(step == 1) {
            delegate.remove(start, stop);
        } else {
            int n = sliceLength(start, stop, step);
            for(int i = start, j = 0; j < n; i += step, j++) {
                delegate.remove(i);
            }
        }
    }
    
    @ExposedMethod
    public final void array_extend(PyObject iterable){
        extendInternal(iterable);
    }

    /**
     * Append items from <em>iterable</em> to the end of the array. If
     * iterable is another array, it must have exactly the same type code; if
     * not, TypeError will be raised. If iterable is not an array, it must be
     * iterable and its elements must be the right type to be appended to the
     * array. Changed in version 2.4: Formerly, the argument could only be
     * another array.
     * 
     * @param iterable
     *            iterable object used to extend the array
     */
    public void extend(PyObject iterable) {
        extendInternal(iterable);
    }

    /**
     * Internal extend function, provides basic interface for extending arrays.
     * Handles specific cases of <em>iterable</em> being PyStrings or
     * PyArrays. Default behaviour is to defer to
     * {@link #extendInternalIter(PyObject) extendInternalIter }
     * 
     * @param iterable
     *            object of type PyString, PyArray or any object that can be
     *            iterated over.
     */
    private void extendInternal(PyObject iterable) {
        // string input
        if(iterable instanceof PyString) {
            fromstring(((PyString)iterable).toString());
            // PyArray input
        } else if(iterable instanceof PyArray) {
            PyArray source = (PyArray)iterable;
            if(!source.type.equals(this.type)) {
                throw Py.TypeError("can only extend with an array of the same kind");
            }
            delegate.appendArray(source.delegate.copyArray());
        } else {
            extendInternalIter(iterable);
        }
    }

    /**
     * Internal extend function to process iterable objects.
     * 
     * @param iterable
     *            any object that can be iterated over.
     */
    private void extendInternalIter(PyObject iterable) {
        // iterable object without a length property - cannot presize the
        // array, so append each item
        if(iterable.__findattr__("__len__") == null) {
            for (PyObject item : iterable.asIterable()) {
                append(item);
            }
        } else {
            // create room
            int last = delegate.getSize();
            delegate.ensureCapacity(last + iterable.__len__());
            for (PyObject item : iterable.asIterable()) {
                set(last++, item);
                delegate.size++;
            }
        }
    }
    
    @ExposedMethod
    public final void array_fromfile(PyObject f, int count){
        fromfile(f, count);
    }
    
    /**
     * Read <em>count</em> items (as machine values) from the file object
     * <em>f</em> and append them to the end of the array. If less than
     * <em>count</em> items are available, EOFError is raised, but the items
     * that were available are still inserted into the array. <em>f</em> must
     * be a real built-in file object; something else with a read() method won't
     * do.
     * 
     * @param f
     *            Python builtin file object to retrieve data
     * @param count
     *            number of array elements to read
     */
    public void fromfile(PyObject f, int count) {
        // check for arg1 as file object
        if(!(f instanceof PyFile)) {
            throw Py.TypeError("arg1 must be open file");
        }
        PyFile file = (PyFile)f;
        // check for read only
        if(file.mode.indexOf("r") == -1) {
            throw Py.TypeError("file needs to be in read mode");
        }
        // read the data via the PyFile
        int readbytes = count * getItemsize();
        String buffer = file.read(readbytes).toString();
        // load whatever was collected into the array
        fromstring(buffer);
        // check for underflow
        if(buffer.length() < readbytes) {
            int readcount = buffer.length() / getItemsize();
            throw Py.EOFError("not enough items in file. "
                    + Integer.toString(count) + " requested, "
                    + Integer.toString(readcount) + " actually read");
        }
    }
    
    @ExposedMethod
    public final void array_fromlist(PyObject obj){
        fromlist(obj);
    }

    /**
     * Append items from the list. This is equivalent to "for x in list:
     * a.append(x)"except that if there is a type error, the array is unchanged.
     * 
     * @param obj
     *            input list object that will be appended to the array
     */
    public void fromlist(PyObject obj) {
        // check for list
        if(!(obj instanceof PyList))
            throw Py.TypeError("expected list argument");
        // store the current size of the internal array
        int size = delegate.getSize();
        try {
            extendInternalIter(obj);
        } catch(PyException e) {
            // trap any exception - any error invalidates the whole list
            delegate.setSize(size);
            // re-throw
            throw new PyException(e.type, e.value);
        }
    }

    /**
     * Generic stream reader to read the entire contents of a stream into the
     * array.
     * 
     * @param is
     *            InputStream to source the data from
     * 
     * @return number of primitives successfully read
     * 
     * @throws IOException
     * @throws EOFException
     */
    private int fromStream(InputStream is) throws IOException, EOFException {
        return fromStream(is, is.available() / getItemsize());
    }

    /**
     * Generic stream reader to read <em>count</em> primitive types from a
     * stream into the array.
     * 
     * @param is
     *            InputStream to source the data from
     * @param count
     *            number of primitive types to read from the stream
     * 
     * @return number of primitives successfully read
     * 
     * @throws IOException
     * @throws EOFException
     */
    private int fromStream(InputStream is, int count) throws IOException,
            EOFException {
        DataInputStream dis = new DataInputStream(is);
        // current number of items present
        int origsize = delegate.getSize();
        // position to start inserting into
        int index = origsize;
        // create capacity for 'count' items
        delegate.ensureCapacity(index + count);
        if(type.isPrimitive()) {
            if(type == Boolean.TYPE) {
                for(int i = 0; i < count; i++, index++) {
                    Array.setBoolean(data, index, dis.readBoolean());
                    delegate.size++;
                }
            } else if(type == Byte.TYPE) {
                for(int i = 0; i < count; i++, index++) {
                    Array.setByte(data, index, dis.readByte());
                    delegate.size++;
                }
            } else if(type == Character.TYPE) {
                for(int i = 0; i < count; i++, index++) {
                    Array.setChar(data, index, (char)dis.readByte());
                    delegate.size++;
                }
            } else if(type == Integer.TYPE) {
                for(int i = 0; i < count; i++, index++) {
                    Array.setInt(data, index, dis.readInt());
                    delegate.size++;
                }
            } else if(type == Short.TYPE) {
                for(int i = 0; i < count; i++, index++) {
                    Array.setShort(data, index, dis.readShort());
                    delegate.size++;
                }
            } else if(type == Long.TYPE) {
                for(int i = 0; i < count; i++, index++) {
                    Array.setLong(data, index, dis.readLong());
                    delegate.size++;
                }
            } else if(type == Float.TYPE) {
                for(int i = 0; i < count; i++, index++) {
                    Array.setFloat(data, index, dis.readFloat());
                    delegate.size++;
                }
            } else if(type == Double.TYPE) {
                for(int i = 0; i < count; i++, index++) {
                    Array.setDouble(data, index, dis.readDouble());
                    delegate.size++;
                }
            }
        }
        dis.close();
        return (index - origsize);
    }

    /**
     * Appends items from the string, interpreting the string as an array of
     * machine values (as if it had been read from a file using the
     * {@link #fromfile(PyObject, int) fromfile()} method).
     * 
     * @param input
     *            string of bytes containing array data
     */
    public void fromstring(String input) {
        int itemsize = getItemsize();
        int strlen = input.length();
        if((strlen % itemsize) != 0) {
            throw Py.ValueError("string length not a multiple of item size");
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(StringUtil.toBytes(input));
        int origsize = delegate.getSize();
        try {
            fromStream(bis);
        } catch(EOFException e) {
            // stubbed catch for fromStream throws
            throw Py.EOFError("not enough items in string");
        } catch(IOException e) {
            // discard anything successfully loaded
            delegate.setSize(origsize);
            throw Py.IOError(e);
        }
    }

    /**
     * Get the element at position <em>i</em> from the array
     * 
     * @param i
     *            index of the item to be retrieved from the array
     */
    protected PyObject pyget(int i) {
        return Py.java2py(Array.get(data, i));
    }

    /**
     * Return the internal Java array storage of the PyArray instance
     * 
     * @return the <code>Array</code> store.
     */
    public Object getArray() throws PyIgnoreMethodTag {
        return delegate.copyArray();
    }

    /**
     * Getter for the storage size of the array's type.
     * <p />
     * 
     * The sizes returned by this method represent the number of bytes used to
     * store the type. In the case of streams, this is the number of bytes
     * written to, or read from a stream. For memory this value is the
     * <em>minimum</em> number of bytes required to store the type.
     * <p />
     * 
     * This method is used by other methods to define read/write quanta from
     * strings and streams.
     * <p />
     * 
     * Values returned are:<br />
     * <table>
     * <tr>
     * <td><strong>Type</strong></td>
     * <td><strong>Size</strong></td>
     * </tr>
     * <tr>
     * <td><code>boolean</code></td>
     * <td>1</td>
     * </tr>
     * <tr>
     * <td><code>byte</code></td>
     * <td>1</td>
     * </tr>
     * <tr>
     * <td><code>char</code></td>
     * <td>1</td>
     * </tr>
     * <tr>
     * <td><code>short</code></td>
     * <td>2</td>
     * </tr>
     * <tr>
     * <td><code>int</code></td>
     * <td>4</td>
     * </tr>
     * <tr>
     * <td><code>long</code></td>
     * <td>8</td>
     * </tr>
     * <tr>
     * <td><code>float</code></td>
     * <td>4</td>
     * </tr>
     * <tr>
     * <td><code>double</code></td>
     * <td>8</td>
     * </tr>
     * </table>
     * 
     * @return number of bytes used to store array type.
     */
    @ExposedGet(name = "itemsize")
    public int getItemsize() {
        if(type.isPrimitive()) {
            if(type == Boolean.TYPE)
                return 1;
            else if(type == Byte.TYPE)
                return 1;
            else if(type == Character.TYPE)
                return 1;
            else if(type == Short.TYPE)
                return 2;
            else if(type == Integer.TYPE)
                return 4;
            else if(type == Long.TYPE)
                return 8;
            else if(type == Float.TYPE)
                return 4;
            else if(type == Double.TYPE)
                return 8;
        }
        // return something here... could be a calculated size?
        return 0;
    }

    /**
     * Retrieve a slice from the array specified by the <em>start</em>,
     * <em>stop</em> and <em>step</em>.
     * 
     * @param start
     *            start index of the slice
     * @param stop
     *            stop index of the slice
     * @param step
     *            stepping increment of the slice
     * @return A new PyArray object containing the described slice
     */
    protected PyObject getslice(int start, int stop, int step) {
        if(step > 0 && stop < start)
            stop = start;
        int n = sliceLength(start, stop, step);
        PyArray ret = new PyArray(type, n);
        if(step == 1) {
            System.arraycopy(data, start, ret.data, 0, n);
            return ret;
        }
        for(int i = start, j = 0; j < n; i += step, j++) {
            Array.set(ret.data, j, Array.get(data, i));
        }
        return ret;
    }

    /**
     * Getter for the type code of the array.
     * {@link #char2class(char) char2class} describes the possible type codes
     * and their meaning.
     * 
     * @return single character type code for the array
     */
    @ExposedGet(name = "typecode")
    public String getTypecode() {
        return typecode;
    }
    
    @ExposedMethod
    public final int array_index(PyObject value){
        int index = indexInternal(value);
        if(index != -1)
            return index;
        throw Py.ValueError("array.index(" + value + "): " + value
                            + " not found in array");
    }

    /**
     * Return the smallest <em>i</em> such that <em>i</em> is the index of
     * the first occurrence of <em>value</em> in the array.
     * 
     * @param value
     *            value to find the index of
     * @return index of the first occurance of <em>value</em>
     */
    public PyObject index(PyObject value) {
        return Py.newInteger(array_index(value));
    }

    /**
     * Return the smallest <em>i</em> such that <em>i</em> is the index of
     * the first occurrence of <em>value</em> in the array.
     * 
     * @param value
     *            value to find the index of
     * @return index of the first occurance of <em>value</em>
     */
    private int indexInternal(PyObject value) {
        // note: cpython does not raise type errors based on item type
        for(int i = 0; i < delegate.getSize(); i++) {
            if(value.equals(Py.java2py(Array.get(data, i)))) {
                return i;
            }
        }
        return -1;
    }
    
    @ExposedMethod
    public final void array_insert(int index, PyObject value){
        insert(index, value);
    }

    /**
     * Insert a new item with value <em>value</em> in the array before
     * position <em>index</em>. Negative values are treated as being relative
     * to the end of the array.
     * 
     * @param index
     *            insert position
     * @param value
     *            value to be inserted into array
     */
    public void insert(int index, PyObject value) {
        delegate.makeInsertSpace(index);
        Array.set(data, index, Py.tojava(value, type));
    }
    
    @ExposedMethod(defaults="-1")
    public final PyObject array_pop(int i){
        return pop(i);
    }

    /**
     * Removes the item with the index <em>index</em> from the array and
     * returns it. The optional argument defaults to -1, so that by default the
     * last item is removed and returned.
     */
    public PyObject pop() {
        return pop(-1);
    }

    /**
     * Removes the item with the index <em>index</em> from the array and
     * returns it. The optional argument defaults to -1, so that by default the
     * last item is removed and returned.
     * 
     * @param index
     *            array location to be popped from the array
     * @return array element popped from index
     */
    public PyObject pop(int index) {
        // todo: python-style error handling
        index = (index < 0) ? delegate.getSize() + index : index;
        PyObject ret = Py.java2py(Array.get(data, index));
        delegate.remove(index);
        return ret;
    }
    
    @ExposedMethod
    public final void array_remove(PyObject value){
        remove(value);
    }

    /**
     * Remove the first occurrence of <em>value</em> from the array.
     * 
     * @param value
     *            array value to be removed
     */
    public void remove(PyObject value) {
        int index = indexInternal(value);
        if(index != -1) {
            delegate.remove(index);
            return;
        }
        throw Py.ValueError("array.remove(" + value + "): " + value
                + " not found in array");
    }

    /**
     * Repeat the array <em>count</em> times.
     * 
     * @param count
     *            number of times to repeat the array
     * @return A new PyArray object containing the source object repeated
     *         <em>count</em> times.
     */
    protected PyObject repeat(int count) {
        Object arraycopy = delegate.copyArray();
        PyArray ret = new PyArray(type, 0);
        for(int i = 0; i < count; i++) {
            ret.delegate.appendArray(arraycopy);
        }
        return ret;
    }
    
    @ExposedMethod
    public final void array_reverse(){
        reverse();
    }

    /**
     * Reverse the elements in the array
     * 
     */
    public void reverse() {
        // build a new reversed array and set this.data to it when done
        Object array = Array.newInstance(type, Array.getLength(data));
        for(int i = 0, lastIndex = delegate.getSize() - 1; i <= lastIndex; i++) {
            Array.set(array, lastIndex - i, Array.get(data, i));
        }
        data = array;
    }

    /**
     * Set an element in the array - the index needs to exist, this method does
     * not automatically extend the array. See
     * {@link AbstractArray#setSize(int) AbstractArray.setSize()} or
     * {@link AbstractArray#ensureCapacity(int) AbstractArray.ensureCapacity()}
     * for ways to extend capacity.
     * <p />
     * 
     * This code specifically checks for overflows of the integral types: byte,
     * short, int and long.
     * 
     * @param i
     *            index of the element to be set
     * @param value
     *            value to set the element to
     */
    public void set(int i, PyObject value) {
        // check for overflow of the integral types
        if(type == Byte.TYPE) {
            long val;
            try {
                val = ((Long)value.__tojava__(Long.TYPE)).longValue();
            } catch(ClassCastException e) {
                throw Py.TypeError("Type not compatible with array type");
            }
            if(val < Byte.MIN_VALUE) {
                throw Py.OverflowError("value too small for " + type.getName());
            } else if(val > Byte.MAX_VALUE) {
                throw Py.OverflowError("value too large for " + type.getName());
            }
        } else if(type == Short.TYPE) {
            long val;
            try {
                val = ((Long)value.__tojava__(Long.TYPE)).longValue();
            } catch(ClassCastException e) {
                throw Py.TypeError("Type not compatible with array type");
            }
            if(val < Short.MIN_VALUE) {
                throw Py.OverflowError("value too small for " + type.getName());
            } else if(val > Short.MAX_VALUE) {
                throw Py.OverflowError("value too large for " + type.getName());
            }
        } else if(type == Integer.TYPE) {
            long val;
            try {
                val = ((Long)value.__tojava__(Long.TYPE)).longValue();
            } catch(ClassCastException e) {
                throw Py.TypeError("Type not compatible with array type");
            }
            if(val < Integer.MIN_VALUE) {
                throw Py.OverflowError("value too small for " + type.getName());
            } else if(val > Integer.MAX_VALUE) {
                throw Py.OverflowError("value too large for " + type.getName());
            }
        } else if(type == Long.TYPE) {
            Object o;
            try {
                o = value.__tojava__(Long.TYPE);
            } catch(ClassCastException e) {
                throw Py.TypeError("Type not compatible with array type");
            }
            if(o == Py.NoConversion) {
                throw Py.OverflowError("value out of range for long");
            }
        }
        Object o = Py.tojava(value, type);
        if(o == Py.NoConversion) {
            throw Py.TypeError("Type not compatible with array type");
        }
        Array.set(data, i, o);
    }

    /**
     * Sets a slice of the array. <em>value</em> can be a string (for
     * <code>byte</code> and <code>char</code> types) or PyArray. If a
     * PyArray, its type must be convertible into the type of the target
     * PyArray.
     * 
     * @param start
     *            start index of the delete slice
     * @param stop
     *            end index of the delete slice
     * @param step
     *            stepping increment of the slice
     */
    protected void setslice(int start, int stop, int step, PyObject value) {
        if(type == Character.TYPE && value instanceof PyString) {
            char[] chars = null;
            // if (value instanceof PyString) {
            if(step != 1) {
                throw Py.ValueError("invalid bounds for setting from string");
            }
            chars = value.toString().toCharArray();
            // }
            // else if (value instanceof PyArray &&
            // ((PyArray)value).type == Character.TYPE) {
            // PyArray other = (PyArray)value;
            // chars = (char[])other.delegate.copyArray();
            // }
            int insertSpace = chars.length - (stop - start);
            // adjust the array, either adding space or removing space
            if(insertSpace > 0) {
                delegate.makeInsertSpace(start, insertSpace);
            } else if(insertSpace < 0) {
                delegate.remove(start, -insertSpace + start - 1);
            }
            delegate.replaceSubArray(chars, start);
        } else {
            if(value instanceof PyString && type == Byte.TYPE) {
                byte[] chars = ((PyString)value).toBytes();
                if(chars.length == stop - start && step == 1) {
                    System.arraycopy(chars, 0, data, start, chars.length);
                } else {
                    throw Py.ValueError("invalid bounds for setting from string");
                }
            } else if(value instanceof PyArray) {
                PyArray array = (PyArray)value;
                int insertSpace = array.delegate.getSize() - (stop - start);
                // adjust the array, either adding space or removing space
                // ...snapshot in case "value" is "this"
                Object arrayCopy = array.delegate.copyArray();
                if(insertSpace > 0) {
                    delegate.makeInsertSpace(start, insertSpace);
                } else if(insertSpace < 0) {
                    delegate.remove(start, -insertSpace + start - 1);
                }
                try {
                    delegate.replaceSubArray(arrayCopy, start);
                } catch(IllegalArgumentException e) {
                    throw Py.TypeError("Slice typecode '" + array.typecode
                            + "' is not compatible with this array (typecode '"
                            + this.typecode + "')");
                }
            }
        }
    }
    
    @ExposedMethod
    public final void array_tofile(PyObject f){
        tofile(f);
    }
    
    @ExposedMethod
    public void array_write(PyObject f){
        tofile(f);
    }

    /**
     * Write all items (as machine values) to the file object <em>f</em>.
     * 
     * @param f
     *            Python builtin file object to write data
     */
    public void tofile(PyObject f) {
        if(!(f instanceof PyFile))
            throw Py.TypeError("arg must be open file");
        PyFile file = (PyFile)f;
        if(file.mode.indexOf("w") == -1 && file.mode.indexOf("a") == -1) {
            throw Py.TypeError("file needs to be in write or append mode");
        }
        // write via the PyFile
        file.write(tostring());
    }

    @ExposedMethod
    public final PyObject array_tolist() {
        return tolist();
    }

    /**
     * Convert the array to an ordinary list with the same items.
     * 
     * @return array contents as a list
     */
    public PyObject tolist() {
        PyList list = new PyList();
        for(int i = 0; i < delegate.getSize(); i++) {
            list.append(Py.java2py(Array.get(data, i)));
        }
        return list;
    }

    /**
     * Generic stream writer to write the entire contents of the array to the
     * stream as primitive types.
     * 
     * @param os
     *            OutputStream to sink the array data to
     * 
     * @return number of primitives successfully written
     * 
     * @throws IOException
     */
    private int toStream(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        if(type.isPrimitive()) {
            if(type == Boolean.TYPE) {
                for(int i = 0; i < delegate.getSize(); i++)
                    dos.writeBoolean(Array.getBoolean(data, i));
            } else if(type == Byte.TYPE) {
                for(int i = 0; i < delegate.getSize(); i++)
                    dos.writeByte(Array.getByte(data, i));
            } else if(type == Character.TYPE) {
                for(int i = 0; i < delegate.getSize(); i++)
                    dos.writeByte((byte)Array.getChar(data, i));
            } else if(type == Integer.TYPE) {
                for(int i = 0; i < delegate.getSize(); i++)
                    dos.writeInt(Array.getInt(data, i));
            } else if(type == Short.TYPE) {
                for(int i = 0; i < delegate.getSize(); i++)
                    dos.writeShort(Array.getShort(data, i));
            } else if(type == Long.TYPE) {
                for(int i = 0; i < delegate.getSize(); i++)
                    dos.writeLong(Array.getLong(data, i));
            } else if(type == Float.TYPE) {
                for(int i = 0; i < delegate.getSize(); i++)
                    dos.writeFloat(Array.getFloat(data, i));
            } else if(type == Double.TYPE) {
                for(int i = 0; i < delegate.getSize(); i++)
                    dos.writeDouble(Array.getDouble(data, i));
            }
        }
        return dos.size();
    }
    
    @ExposedMethod
    public final PyObject array_tostring(){
        return new PyString(tostring());
    }

    /**
     * Convert the array to an array of machine values and return the string
     * representation (the same sequence of bytes that would be written to a
     * file by the {@link #tofile(PyObject) tofile()} method.)
     */
    public String tostring() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            toStream(bos);
        } catch(IOException e) {
            throw Py.IOError(e);
        }
        return StringUtil.fromBytes(bos.toByteArray());
    }
}
