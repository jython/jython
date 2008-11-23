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
@ExposedType(name = "array.array", base = PyObject.class)
public class PyArray extends PySequence implements Cloneable {

    public static final PyType TYPE = PyType.fromClass(PyArray.class);

    /** The underlying Java array. */
    private Object data;

    /** The Java array class. */
    private Class type;

    /** The Python style typecode of the array. */
    private String typecode;

    private ArrayDelegate delegate;

    public PyArray(PyType type) {
        super(type);
    }

    public PyArray(Class type, Object data) {
        this(TYPE);
        setup(type, data);
    }

    public PyArray(Class type, int n) {
        this(type, Array.newInstance(type, n));
    }

    public PyArray(PyArray toCopy) {
        this(toCopy.type, toCopy.delegate.copyArray());
        typecode = toCopy.typecode;
    }

    private void setup(Class type, Object data) {
        this.type = type;
        typecode = class2char(type);
        if (data == null) {
            this.data = Array.newInstance(type, 0);
        } else {
            this.data = data;
        }
        delegate = new ArrayDelegate();
    }

    @ExposedNew
    static final PyObject array_new(PyNewWrapper new_, boolean init, PyType subtype,
                                   PyObject[] args, String[] keywords) {
        if (new_.for_type != subtype && keywords.length > 0) {
            int argc = args.length - keywords.length;
            PyObject[] justArgs = new PyObject[argc];
            System.arraycopy(args, 0, justArgs, 0, argc);
            args = justArgs;
        }
        ArgParser ap = new ArgParser("array", args, Py.NoKeywords, new String[] {"typecode", "initializer"},
                                     1);
        ap.noKeywords();
        PyObject obj = ap.getPyObject(0);
        PyObject initial = ap.getPyObject(1, null);

        Class type;
        String typecode;
        if (obj instanceof PyString && !(obj instanceof PyUnicode)) {
            if (obj.__len__() != 1) {
                throw Py.TypeError("array() argument 1 must be char, not str");
            }
            typecode = obj.toString();
            type = char2class(typecode.charAt(0));
        } else if (obj instanceof PyJavaType) {
            type = ((PyJavaType)obj).underlying_class;
            typecode = type.getName();
        } else {
            throw Py.TypeError("array() argument 1 must be char, not " + obj.getType().fastGetName());
        }

        PyArray self;
        if (new_.for_type == subtype) {
            self = new PyArray(subtype);
        } else {
            self = new PyArrayDerived(subtype);
        }
        // Initialize the typecode (and validate type) before creating the backing Array
        class2char(type);
        self.setup(type, Array.newInstance(type, 0));
        self.typecode = typecode;
        if (initial == null) {
            return self;
        }
        if (initial instanceof PyList) {
            self.fromlist(initial);
        } else if (initial instanceof PyString && !(initial instanceof PyUnicode)) {
            self.fromstring(initial.toString());
        } else if ("u".equals(typecode)) {
            if (initial instanceof PyUnicode) {
                self.extendArray(((PyUnicode) initial).toCodePoints());
            }
            else {
                self.extendUnicodeIter(initial);
            }
        } else {
            self.extendInternal(initial);
        }
        return self;
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

    public PyObject __imul__(PyObject o) {
        return array___imul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject array___imul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        if (delegate.getSize() > 0) {
            int count = o.asIndex(Py.OverflowError);
            if (count <= 0) {
                delegate.clear();
                return this;
            }
            Object copy = delegate.copyArray();
            delegate.ensureCapacity(delegate.getSize() * count);
            for (int i = 1; i < count; i++) {
                delegate.appendArray(copy);
            }
        }
        return this;
    }

    public PyObject __mul__(PyObject o) {
        return array___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject array___mul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    public PyObject __rmul__(PyObject o) {
        return array___rmul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject array___rmul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    public PyObject __iadd__(PyObject other) {
        return array___iadd__(other);
    }

    @ExposedMethod
    final PyObject array___iadd__(PyObject other) {
        PyArray otherArr = null;
        if (!(other instanceof PyArray)) {
            throw Py.TypeError(String.format("can only append array (not \"%.200s\") to array",
                                             other.getType().fastGetName()));
        }
        otherArr = (PyArray)other;
        if (!otherArr.typecode.equals(this.typecode)) {
            throw Py.TypeError("can only append arrays of the same type, "
                    + "expected '" + this.type + ", found " + otherArr.type);
        }
        delegate.appendArray(otherArr.delegate.copyArray());
        return this;
    }

    public PyObject __add__(PyObject other) {
        return array___add__(other);
    }

    /**
     * Adds (appends) two PyArrays together
     *
     * @param other
     *            a PyArray to be added to the instance
     * @return the result of the addition as a new PyArray instance
     */
    @ExposedMethod
    final PyObject array___add__(PyObject other) {
        PyArray otherArr = null;
        if(!(other instanceof PyArray)) {
            throw Py.TypeError("can only append another array to an array");
        }
        otherArr = (PyArray)other;
        if(!otherArr.typecode.equals(this.typecode)) {
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

    public PyObject __reduce__() {
        return array___reduce__();
    }

    @ExposedMethod
    final PyObject array___reduce__() {
        PyObject dict = __findattr__("__dict__");
        if (dict == null) {
            dict = Py.None;
        }
        if (__len__() > 0) {
            return new PyTuple(getType(), new PyTuple(Py.newString(typecode),
                                                      Py.newString(tostring())), dict);
        } else {
            return new PyTuple(getType(), new PyTuple(Py.newString(typecode)), dict);
        }
    }

    @Override
    public String toString() {
        if (__len__() == 0) {
            return String.format("array(%s)", encodeTypecode(typecode));
        }
        String value;
        if ("c".equals(typecode)) {
            value = PyString.encode_UnicodeEscape(tostring(), true);
        } else if ("u".equals(typecode)) {
            value = (new PyUnicode(tounicode())).__repr__().toString();
        } else {
            value = tolist().toString();
        }
        return String.format("array(%s, %s)", encodeTypecode(typecode), value);
    }

    private String encodeTypecode(String typecode) {
        if (typecode.length() > 1) return typecode;
        else return "'" + typecode + "'";
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

    private static int getCodePoint(PyObject obj) {
        if (obj instanceof PyUnicode) {
            PyUnicode u = (PyUnicode) obj;
            int[] codepoints = u.toCodePoints();
            if (codepoints.length == 1) {
                return codepoints[0];
            }
        }
        throw Py.TypeError("array item must be unicode character");
    }


    // relax to allow mixing with PyString, integers
    private static int getCodePointOrInt(PyObject obj) {
        if (obj instanceof PyUnicode) {
            PyUnicode u = (PyUnicode) obj;
            return u.toCodePoints()[0];
        }
        else if (obj instanceof PyString) {
            PyString s = (PyString) obj;
            return s.toString().charAt(0);
        }
        else if (obj.__nonzero__()) {
            return ((PyInteger)obj.__int__()).getValue();
        }
        else {
            return -1;
        }
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
        if ("u".equals(typecode)) {
            int codepoint = getCodePoint(value);
            delegate.makeInsertSpace(afterLast);
            Array.setInt(data, afterLast, codepoint);
        } else {

            delegate.makeInsertSpace(afterLast);
            try {
                set(afterLast, value);
            } catch (PyException e) {
                delegate.setSize(afterLast);
                throw new PyException(e.type, e.value);
            }
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
        if (getItemsize() == 0 || "u".equals(typecode)) {
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

    // promote B, H, I (unsigned int) to next larger size
    public static Class char2class(char type) throws PyIgnoreMethodTag {
        switch(type){
            case 'z':
                return Boolean.TYPE;
            case 'b':
                return Byte.TYPE;
            case 'B':
                return Short.TYPE;
            case 'u':
                return Integer.TYPE;
            case 'c':
                return Character.TYPE;
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
            case 'L':
                return Long.TYPE;
            case 'f':
                return Float.TYPE;
            case 'd':
                return Double.TYPE;
            default:
                throw Py.ValueError("bad typecode (must be c, b, B, u, h, H, i, I, l, L, f or d)");
        }
    }

    private static String class2char(Class cls) {
        if(cls.equals(Boolean.TYPE))
            return "z";
        else if(cls.equals(Character.TYPE))
            return "c";
        else if(cls.equals(Byte.TYPE))
            return "b";
        else if(cls.equals(Short.TYPE))
            return "h";
        else if(cls.equals(Integer.TYPE))
            return "i";
        else if(cls.equals(Long.TYPE))
            return "l";
        else if(cls.equals(Float.TYPE))
            return "f";
        else if(cls.equals(Double.TYPE))
            return "d";
        else
            return cls.getName();
    }

    @ExposedMethod
    public final int array_count(PyObject value) {
        // note: cpython does not raise type errors based on item type;
        int iCount = 0;
        int len = delegate.getSize();
        if ("u".equals(typecode)) {
            int codepoint = getCodePointOrInt(value);
            for (int i = 0; i < len; i++) {
                if (codepoint == Array.getInt(data, i)) {
                    iCount++;
                }
            }
        } else {

            for (int i = 0; i < len; i++) {
                if (value.equals(Py.java2py(Array.get(data, i)))) {
                    iCount++;
                }
            }
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
        if (step == 1) {
            delegate.remove(start, stop);
        } else if (step > 1) {
            for (int i = start; i < stop; i += step) {
                delegate.remove(i);
                i--;
                stop--;
            }
        } else if (step < 0) {
            for (int i = start; i >= 0 && i >= stop; i += step) {
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
        if (iterable instanceof PyUnicode) {
            if ("u".equals(typecode)) {
                extendUnicodeIter(iterable);
            } else if ("c".equals(typecode)){
                throw Py.TypeError("array item must be char");
            } else {
                throw Py.TypeError("an integer is required");
            }
        } else if (iterable instanceof PyString) {
            fromstring(((PyString) iterable).toString());
        } else if (iterable instanceof PyArray) {
            PyArray source = (PyArray) iterable;
            if (!source.typecode.equals(typecode)) {
                throw Py.TypeError("can only extend with array of same kind");
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

    private void extendUnicodeIter(PyObject iterable) {
        for (PyObject item : iterable.asIterable()) {
            PyUnicode uitem;
            try {
                uitem = (PyUnicode) item;
            } catch (ClassCastException e) {
                throw Py.TypeError("Type not compatible with array type");
            }
            for (int codepoint : uitem.toCodePoints()) {
                int afterLast = delegate.getSize();
                delegate.makeInsertSpace(afterLast);
                Array.setInt(data, afterLast, codepoint);
            }
        }
    }

    private void extendArray(int[] items) {
        int last = delegate.getSize();
        delegate.ensureCapacity(last + items.length);
        for (int item : items) {
            Array.set(data, last++, item);
            delegate.size++;
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
        if(!(obj instanceof PyList)) {
            throw Py.TypeError("arg must be list");
        }
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
                    Array.setChar(data, index, (char)(dis.readByte() & 0xff));
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

    public void fromstring(String input) {
        array_fromstring(input);
    }

    /**
     * Appends items from the string, interpreting the string as an array of
     * machine values (as if it had been read from a file using the
     * {@link #fromfile(PyObject, int) fromfile()} method).
     *
     * @param input
     *            string of bytes containing array data
     */
    @ExposedMethod
    final void array_fromstring(String input) {
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

    public void fromunicode(PyUnicode input) {
        array_fromunicode(input);
    }

    @ExposedMethod
    final void array_fromunicode(PyObject input) {
        if (!(input instanceof PyUnicode)) {
            throw Py.ValueError("fromunicode argument must be an unicode object");
        }
        if (!"u".equals(typecode)) {
            throw Py.ValueError("fromunicode() may only be called on type 'u' arrays");
        }
        extend(input);
    }

    /**
     * Get the element at position <em>i</em> from the array
     *
     * @param i
     *            index of the item to be retrieved from the array
     */
    protected PyObject pyget(int i) {
        if ("u".equals(typecode)) {
            return new PyUnicode(Array.getInt(data, i));
        }
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
        if (step > 0 && stop < start) {
            stop = start;
        }
        int n = sliceLength(start, stop, step);
        PyArray ret = new PyArray(type, n);
        // XXX:
        ret.typecode = typecode;
        if (step == 1) {
            System.arraycopy(data, start, ret.data, 0, n);
            return ret;
        }
        for (int i = start, j = 0; j < n; i += step, j++) {
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

        int len = delegate.getSize();
        if ("u".equals(typecode)) {
            int codepoint = getCodePointOrInt(value);
            for (int i = 0; i < len; i++) {
                if (codepoint == Array.getInt(data, i)) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (value.equals(Py.java2py(Array.get(data, i)))) {
                    return i;
                }
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
        index = calculateIndex(index);
        if ("u".equals(typecode)) {
            int codepoint = getCodePoint(value);
            delegate.makeInsertSpace(index);
            Array.setInt(data, index, codepoint);

        } else {
            delegate.makeInsertSpace(index);
            Array.set(data, index, Py.tojava(value, type));
        }
    }

    @ExposedMethod(defaults="-1")
    public final PyObject array_pop(int i){
        PyObject val = pop(i);
        if ("u".equals(typecode)) {
            return new PyUnicode(val.asInt());
        }
        return val;
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
        if (delegate.getSize() == 0) {
            throw Py.IndexError("pop from empty array");
        }
        index = fixindex(index);
        if (index == -1) {
            throw Py.IndexError("pop index out of range");
        }
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
        // XXX:
        ret.typecode = typecode;
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
        if ("u".equals(typecode)) {
            Array.setInt(data, i, getCodePoint(value));
            return;
        }

        if(type == Byte.TYPE) {
            long val;
            try {
                val = ((Long)value.__tojava__(Long.TYPE)).longValue();
            } catch(ClassCastException e) {
                throw Py.TypeError("Type not compatible with array type");
            }
            if(val < (isSigned() ? 0 : Byte.MIN_VALUE)) {
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
            if(val < (isSigned() ? 0 : Short.MIN_VALUE)) {
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
            if(val < (isSigned() ? 0 : Integer.MIN_VALUE)) {
                throw Py.OverflowError("value too small for " + type.getName());
            } else if(val > Integer.MAX_VALUE) {
                throw Py.OverflowError("value too large for " + type.getName());
            }
        } else if(type == Long.TYPE) {
            if (isSigned() && value instanceof PyInteger) {
                if (((PyInteger)value).getValue() < 0) {
                    throw Py.OverflowError("value too small for " + type.getName());
                }
            } else if (value instanceof PyLong) {
                ((PyLong)value).getLong(isSigned() ? 0 : Long.MIN_VALUE,
                                        Long.MAX_VALUE);
            } else {
                Object o;
                try {
                    o = value.__tojava__(Long.TYPE);
                } catch(ClassCastException e) {
                    throw Py.TypeError("Type not compatible with array type");
                }
                if(o == Py.NoConversion) {
                    throw Py.TypeError("Type not compatible with array type");
                }
            }
        }
        Object o = Py.tojava(value, type);
        if(o == Py.NoConversion) {
            throw Py.TypeError("Type not compatible with array type");
        }
        Array.set(data, i, o);
    }

    // xxx - add more efficient comparable typecode lookup via an enumset, and expand
    public void set(int i, int value) {
        if ("u".equals(typecode) || type == Integer.TYPE || type == Long.TYPE) {
            Array.setInt(data, i, value);
        } else {
            throw Py.TypeError("Type not compatible with array type");
        }
    }

    public void set(int i, char value) {
        if ("c".equals(typecode) || type == Integer.TYPE || type == Long.TYPE) {
            Array.setChar(data, i, value);
        } else {
            throw Py.TypeError("Type not compatible with array type");
        }
    }

    private boolean isSigned() {
        return typecode.length() == 1 && typecode.equals(typecode.toUpperCase());
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
        if (stop < start) {
            stop = start;
        }
        if(type == Character.TYPE && value instanceof PyString) {
            char[] chars = null;
            // if (value instanceof PyString) {
            if(step != 1) {
                throw Py.ValueError("invalid bounds for setting from string");
            }
            chars = value.toString().toCharArray();
            delegate.replaceSubArray(start, stop, chars, 0, chars.length);
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
                if (!array.typecode.equals(typecode)) {
                    throw Py.TypeError("bad argument type for built-in operation|" + array.typecode + "|" + typecode);
                }
                if (step == 1) {
                    Object arrayDelegate;
                    if (array == this) {
                        arrayDelegate = array.delegate.copyArray();
                    } else {
                        arrayDelegate = array.delegate.getArray();
                    }
                    try {
                        delegate.replaceSubArray(start, stop, arrayDelegate, 0, array.delegate.getSize());
                    } catch(IllegalArgumentException e) {
                        throw Py.TypeError("Slice typecode '" + array.typecode
                                           + "' is not compatible with this array (typecode '"
                                           + this.typecode + "')");
                    }
                } else if (step > 1) {
                    int len = array.__len__();
                    for (int i = 0, j = 0; i < len; i++, j += step) {
                        Array.set(data, j + start, Array.get(array.data, i));
                    }
                } else if (step < 0) {
                    if (array == this) {
                        array = (PyArray)array.clone();
                    }
                    int len = array.__len__();
                    for (int i = 0, j = delegate.getSize() - 1; i < len; i++, j += step) {
                        Array.set(data, j, Array.get(array.data, i));
                    }
                }
            } else {
                throw Py.TypeError(String.format("can only assign array (not \"%.200s\") to array "
                                                 + "slice", value.getType().fastGetName()));
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
        int len = delegate.getSize();
        if ("u".equals(typecode)) {
            for (int i = 0; i < len; i++) {
                list.append(new PyUnicode(Array.getInt(data, i)));
            }
        } else {
            for (int i = 0; i < len; i++) {
                list.append(Py.java2py(Array.get(data, i)));
            }
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

    public String tounicode() {
        if (!"u".equals(typecode)) {
            throw Py.ValueError("tounicode() may only be called on type 'u' arrays");
        }
        int len = delegate.getSize();
        int[] codepoints = new int[len];
        for(int i = 0; i < len; i++)
            codepoints[i] = Array.getInt(data, i);
        return new String(codepoints, 0, codepoints.length);
    }


    @ExposedMethod
    public final PyObject array_tounicode() {
        return new PyUnicode(tounicode());
    }

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
}
