// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;

import org.python.core.buffer.BaseBuffer;
import org.python.core.buffer.SimpleBuffer;
import org.python.core.buffer.SimpleStringBuffer;
import org.python.core.util.ByteSwapper;
import org.python.core.util.StringUtil;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;
import org.python.modules.gc;

/**
 * The type {@code array.array}. This is a wrapper around native Java arrays. Instances of
 * {@code PyArray} are created either by Java functions or directly by the {@code jarray} module
 * (q.v.).
 * <p>
 * The range of possible element (item) types exceeds that in Python, since it allows for arbitrary
 * Java classes. This extended behaviour is accessible from Python by supplying a Java type (class)
 * to the constructor, where one might have used a single character type code. For example:<pre>
 * >>> ax = array.array(BigDecimal, (BigDecimal(str(n)) for n in range(5)))
 * >>> ax
 * array(java.math.BigDecimal, [0, 1, 2, 3, 4])
 * >>> type(ax[2])
 * &lt;type 'java.math.BigDecimal'>
 * </pre>
 */
@ExposedType(name = "array.array", base = PyObject.class)
public class PyArray extends PySequence implements Cloneable, BufferProtocol, Traverseproc {

    public static final PyType TYPE = PyType.fromClass(PyArray.class);

    /** The underlying Java array, a Java Array in practice. */
    private Object data;

    /** The Java class of elements in the {@code data} array. */
    private Class<?> itemClass;

    /**
     * Either a Python-style {@code array.array} type code for the element (item) type or the Java
     * class name.
     */
    private String typecode;

    /**
     * Mix in the mechanisms for manipulating the underlying array as this "delegate" object. Many
     * operations on this {@code array.array} are actually operations on this delegate (an
     * {@code AbstractArray} in practice), which in turn manipulates {@link #data}.
     */
    private ArrayDelegate delegate;

    /**
     * Create a default {@code PyArray} of specific Python type (for sub-class use).
     *
     * @param subtype actual Python type
     */
    public PyArray(PyType subtype) {
        super(subtype);
    }

    /**
     * Create a {@code PyArray} with the given array item class and content.
     *
     * @param itemClass of elements in the array
     * @param data
     */
    public PyArray(Class<?> itemClass, Object data) {
        this(TYPE);
        setElementType(itemClass);
        setData(data);
    }

    public PyArray(Class<?> itemClass, PyObject initial) {
        this(TYPE);
        setElementType(itemClass);
        useInitial(initial);
    }

    public PyArray(Class<?> itemClass, int n) {
        this(itemClass, Array.newInstance(itemClass, n));
    }

    public PyArray(PyArray toCopy) {
        this(toCopy.itemClass, toCopy.delegate.copyArray());
        typecode = toCopy.typecode;
    }

    /**
     * Initialise this array from a Python {@code array.array} type code character. The way
     * {@link #array_new(PyNewWrapper, boolean, PyType, PyObject[], String[]) array_new} works, and
     * the constructors, is to create an instance with the almost parameterless
     * {@link #PyArray(PyType)} with sub-type argument. This blank canvas needs to be inscribed with
     * a consistent state by a call to this method and either {@link #setData(Object) setData} or
     * {@link #useInitial(PyObject) useInitial}.
     *
     * @param typecode of the elements
     */
    private void setElementType(char typecode) {
        this.itemClass = char2class(typecode);
        this.typecode = Character.toString(typecode);
    }

    /**
     * Initialise this array from the Java element class. The way
     * {@link #array_new(PyNewWrapper, boolean, PyType, PyObject[], String[]) array_new} works and
     * the constructors, is to create an instance with the almost parameterless
     * {@link #PyArray(PyType)} with sub-type argument. This blank canvas needs to be inscribed with
     * a consistent state by a call to this method and either {@link #setData(Object) setData} or
     * {@link #useInitial(PyObject) useInitial}.
     *
     * @param itemClass of the elements
     */
    private void setElementType(Class<?> itemClass) {
        this.itemClass = itemClass;
        this.typecode = classToTypecode(itemClass);
    }

    /**
     * Make a given object the storage for the array. Normally this is a Java array of type
     * consistent with the element type. It will be manipulated by {@link #delegate}.
     *
     * @param data the storage.
     */
    private void setData(Object data) {
        this.data = data;
        this.delegate = new ArrayDelegate();
    }

    /**
     * Provide initial values to the internal storage array from one of several types in the broad
     * categories of a byte string (which is treated as a machine representation of the data) or an
     * iterable yielding values assignable to the elements. There is special treatment for typecode
     * 'u', itemClass Unicode.
     *
     * @param initial source of values or {@code null}
     */
    private void useInitial(PyObject initial) {

        // If we do not yet have a representation array, provide one
        if (this.data == null || this.delegate == null) {
            setData(Array.newInstance(this.itemClass, 0));
        }

        // The initialiser may be omitted, or may validly be one of several types.
        if (initial == null) {
            // Fall through

        } else if (initial instanceof PyList) {
            fromlist(initial);

        } else if (initial instanceof PyString && !(initial instanceof PyUnicode)) {
            fromstring(initial.toString());

        } else if ("u".equals(typecode)) {
            if (initial instanceof PyUnicode) {
                extendArray(((PyUnicode) initial).toCodePoints());
            } else {
                extendUnicodeIter(initial);
            }

        } else {
            extendInternal(initial);
        }
    }

    @ExposedNew
    static final PyObject array_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {

        if (new_.for_type != subtype && keywords.length > 0) {
            /*
             * We're constructing as a base for a derived type (via PyDerived) and there are
             * keywords. The effective args locally should not include the keywords.
             */
            int argc = args.length - keywords.length;
            PyObject[] justArgs = new PyObject[argc];
            System.arraycopy(args, 0, justArgs, 0, argc);
            args = justArgs;
        }

        // Create a 'blank canvas' of the appropriate concrete class.
        PyArray self =
                new_.for_type == subtype ? new PyArray(subtype) : new PyArrayDerived(subtype);

        // Build the argument parser for this call
        ArgParser ap = new ArgParser("array", args, Py.NoKeywords,
                new String[] {"typecode", "initializer"}, 1);
        ap.noKeywords();

        // Retrieve the mandatory type code that determines the element itemClass
        PyObject obj = ap.getPyObject(0);
        if (obj instanceof PyString && !(obj instanceof PyUnicode)) {
            if (obj.__len__() != 1) {
                throw Py.TypeError("array() argument 1 must be char, not str");
            }
            char typecode = obj.toString().charAt(0);
            self.setElementType(typecode);
        } else if (obj instanceof PyJavaType) {
            Class<?> itemClass = ((PyJavaType) obj).getProxyType();
            self.setElementType(itemClass);
        } else {
            throw Py.TypeError(
                    "array() argument 1 must be char, not " + obj.getType().fastGetName());
        }

        // Fill the array from the second argument (if there is one)
        self.useInitial(ap.getPyObject(1, null));
        return self;
    }

    public static PyArray zeros(int n, char typecode) {
        PyArray array = zeros(n, char2class(typecode));
        array.typecode = Character.toString(typecode);
        return array;
    }

    public static PyArray zeros(int n, Class<?> ctype) {
        PyArray array = new PyArray(ctype, n);
        array.typecode = ctype.getName();
        return array;
    }

    public static PyArray array(PyObject seq, char typecode) {
        PyArray array = PyArray.array(seq, char2class(typecode));
        array.typecode = Character.toString(typecode);
        return array;
    }

    public static Class<?> array_class(Class<?> type) {
        return Array.newInstance(type, 0).getClass();
    }

    /**
     * Create a {@code PyArray} storing {@code ctype} types and being initialised with {@code init}.
     *
     * @param init an initialiser for the array - can be {@code PyString} or {@code PySequence}
     *            (including {@code PyArray}) or iterable type.
     * @param itemClass {@code Class} of the elements stored in the array.
     * @return a new PyArray
     */
    public static PyArray array(PyObject init, Class<?> itemClass) {
        PyArray array = new PyArray(itemClass, 0);
        array.typecode = itemClass.getName();
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

    @Override
    public int hashCode() {
        return array___hash__();
    }

    @ExposedMethod
    final int array___hash__() {
        throw Py.TypeError(String.format("unhashable type: '%.200s'", getType().fastGetName()));
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
        if (ret == null) {
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

        seq___setslice__(start, stop, step, value);
    }

    @ExposedMethod(defaults = "null")
    final void array___delslice__(PyObject start, PyObject stop, PyObject step) {
        seq___delslice__(start, stop, step);
    }

    @Override
    public PyObject __imul__(PyObject o) {
        return array___imul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject array___imul__(PyObject o) {

        if (!o.isIndex()) {
            return null;
        }

        resizeCheck();  // Prohibited if exporting a buffer

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

    @Override
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

    @Override
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

    @Override
    public PyObject __iadd__(PyObject other) {
        return array___iadd__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject array___iadd__(PyObject other) {
        try {
            PyArray otherArr = arrayChecked(other);
            resizeCheck();  // Prohibited if exporting a buffer
            delegate.appendArray(otherArr.delegate.copyArray());
            return this;
        } catch (ClassCastException e) {
            // other wasn't a PyArray
            return null;
        }
    }

    @Override
    public PyObject __add__(PyObject other) {
        return array___add__(other);
    }

    /**
     * Adds (appends) two PyArrays together
     *
     * @param other a {@code PyArray} to be added to the instance
     * @return the result of the addition as a new {@code PyArray} instance
     */
    @ExposedMethod(type = MethodType.BINARY)
    final PyObject array___add__(PyObject other) {
        try {
            PyArray otherArr = arrayChecked(other);
            PyArray ret = new PyArray(this);
            ret.delegate.appendArray(otherArr.delegate.copyArray());
            return ret;
        } catch (ClassCastException e) {
            // other wasn't a PyArray
            return null;
        }

    }

    /**
     * Check the other array is an array and is compatible for element type. Raise {@code TypeError}
     * if not.
     *
     * @param other supposed {@code PyArray}
     * @return {@code other}
     * @throws ClassCastException if {@code other} not {@code PyArray}
     */
    private PyArray arrayChecked(PyObject other) throws ClassCastException {
        PyArray otherArr = (PyArray) other;
        if (!otherArr.typecode.equals(this.typecode)) {
            throw Py.TypeError("can only append arrays of the same type, expected '"
                    + this.itemClass + ", found " + otherArr.itemClass);
        }
        return otherArr;
    }

    /**
     * Length of the array
     *
     * @return number of elements in the array
     */
    @Override
    public int __len__() {
        return array___len__();
    }

    @ExposedMethod
    final int array___len__() {
        return delegate.getSize();
    }

    @Override
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
            return new PyTuple(getType(),
                    new PyTuple(Py.newString(typecode), Py.newString(tostring())), dict);
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
        if (typecode.length() > 1) {
            return typecode;
        } else {
            return "'" + typecode + "'";
        }
    }

    /**
     *
     * @param c target {@code Class} for the conversion
     * @return Java object converted to required class type if possible.
     */
    @Override
    public Object __tojava__(Class<?> c) {
        boolean isArray = c.isArray();
        Class<?> componentType = c.getComponentType();

        if (c == Object.class || (isArray && componentType.isAssignableFrom(itemClass))) {
            if (delegate.capacity != delegate.size) {
                // when unboxing, need to shrink the array first, otherwise incorrect
                // results to Java
                return delegate.copyArray();
            } else {
                return data;
            }
        }

        // rebox: this array is made of primitives but converting to Object[]
        if (isArray && componentType == Object.class) {
            Object[] boxed = new Object[delegate.size];
            for (int i = 0; i < delegate.size; i++) {
                boxed[i] = Array.get(data, i);
            }
            return boxed;
        }

        if (c.isInstance(this)) {
            return this;
        }

        return Py.NoConversion;
    }

    @ExposedMethod
    public final void array_append(PyObject value) {
        resizeCheck();  // Prohibited if exporting a buffer
        appendUnchecked(value);
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
        } else if (obj instanceof PyString) {
            PyString s = (PyString) obj;
            return s.toString().charAt(0);
        } else if (obj.__nonzero__()) {
            return obj.asInt();
        } else {
            return -1;
        }
    }

    /**
     * Append new value x to the end of the array.
     *
     * @param value item to be appended to the array
     */
    public void append(PyObject value) {
        resizeCheck();  // Prohibited if exporting a buffer
        appendUnchecked(value);
    }

    /**
     * Common helper method used internally to append a new value x to the end of the array:
     * {@link #resizeCheck()} is not called, so the client must do so in advance.
     *
     * @param value item to be appended to the array
     */
    private final void appendUnchecked(PyObject value) {
        // Currently, append is asymmetric with extend, which
        // *will* do conversions like append(5.0) to an int array.
        // Also, CPython 2.2 will do the append coercion. However,
        // it is deprecated in CPython 2.3, so maybe we are just
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
     * "Byteswap" all items of the array. This is only supported for values which are 1, 2, 4, or 8
     * bytes in size; for other types of values, {@code RuntimeError} is raised. It is useful when
     * reading data from a file written on a machine with a different byte order.
     */
    public void byteswap() {
        if (getStorageSize() == 0 || "u".equals(typecode)) {
            throw Py.RuntimeError("don't know how to byteswap this array type");
        }
        ByteSwapper.swap(data);
    }

    /**
     * Implementation of {@code Cloneable} interface.
     *
     * @return copy of current PyArray
     */
    @Override
    public Object clone() {
        return new PyArray(this);
    }

    /**
     * Converts a character code for the array type to the Java {@code Class} of the elements of the
     * implementation array.
     * <table>
     * <caption>Supported character codes and their native types</caption>
     * <tr>
     * <td><strong>Type code</strong></td>
     * <td><strong>native type</strong></td>
     * </tr>
     * <tr>
     * <td>z</td>
     * <td>{@code boolean}</td>
     * </tr>
     * <tr>
     * <td>c</td>
     * <td>{@code char}</td>
     * </tr>
     * <tr>
     * <td>b</td>
     * <td>{@code byte}</td>
     * </tr>
     * <tr>
     * <td>h</td>
     * <td>{@code short}</td>
     * </tr>
     * <tr>
     * <td>i</td>
     * <td>{@code int}</td>
     * </tr>
     * <tr>
     * <td>l</td>
     * <td>{@code long}</td>
     * </tr>
     * <tr>
     * <td>f</td>
     * <td>{@code float}</td>
     * </tr>
     * <tr>
     * <td>d</td>
     * <td>{@code double}</td>
     * </tr>
     * </table>
     * <p>
     *
     * @param typecode character code for the array type
     * @return {@code Class} of the native itemClass
     */
    // promote B, H, I (unsigned int) to next larger size
    public static Class<?> char2class(char typecode) throws PyIgnoreMethodTag {
        switch (typecode) {
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
                throw Py.ValueError(
                        "bad typecode (must be c, b, B, u, h, H, i, I, l, L, f, d or z)");
        }
    }

    /**
     * Map a Java class to the {@code array.array} type code that represents it. Where that may be
     * ambiguous, the method assumes signed representation (so for example {@code Integer} maps to
     * {@code 'i'} not {@code 'I'}). Classes other than those map to their Java class name. THis
     * supports the extended repertoire {@code array.array} has in Jython.
     *
     * @param cls element class
     * @return the {@code array.array} type code that representing {@code cls}
     */
    private static String classToTypecode(Class<?> cls) {
        if (cls.equals(Boolean.TYPE)) {
            return "z";
        } else if (cls.equals(Character.TYPE)) {
            return "c";
        } else if (cls.equals(Byte.TYPE)) {
            return "b";
        } else if (cls.equals(Short.TYPE)) {
            return "h";
        } else if (cls.equals(Integer.TYPE)) {
            return "i";
        } else if (cls.equals(Long.TYPE)) {
            return "l";
        } else if (cls.equals(Float.TYPE)) {
            return "f";
        } else if (cls.equals(Double.TYPE)) {
            return "d";
        } else {
            return cls.getName();
        }
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
     * @param value instances of the value to be counted
     * @return number of time value was found in the array.
     */
    public PyInteger count(PyObject value) {
        return Py.newInteger(array_count(value));
    }

    /**
     * Delete the element at position {@code i} from the array
     *
     * @param i index of the item to be deleted from the array
     */
    @Override
    protected void del(int i) {
        resizeCheck();  // Prohibited if exporting a buffer
        delegate.remove(i);
    }

    /**
     * Delete the slice defined by {@code start} to {@code stop} from the array.
     *
     * @param start starting index of slice
     * @param stop finishing index of slice
     */
    @Override
    protected void delRange(int start, int stop) {
        resizeCheck();  // Prohibited if exporting a buffer
        delegate.remove(start, stop);
    }

    @ExposedMethod
    public final void array_extend(PyObject iterable) {
        extendInternal(iterable);
    }

    /**
     * Append items from {@code iterable} to the end of the array. If iterable is another array, it
     * must have exactly the same type code; if not, TypeError will be raised. If iterable is not an
     * array, it must be iterable and its elements must be the right type to be appended to the
     * array.
     *
     * @param iterable iterable object used to extend the array
     */
    public void extend(PyObject iterable) {
        extendInternal(iterable);
    }

    /**
     * Internal extend function, provides basic interface for extending arrays. Handles specific
     * cases of {@code iterable} being PyStrings or PyArrays. Default behaviour is to defer to
     * {@link #extendInternalIter(PyObject) extendInternalIter }
     *
     * @param iterable object of type PyString, PyArray or any object that can be iterated over.
     */
    private void extendInternal(PyObject iterable) {

        if (iterable instanceof PyUnicode) {
            if ("u".equals(typecode)) {
                extendUnicodeIter(iterable);
            } else if ("c".equals(typecode)) {
                throw Py.TypeError("array item must be char");
            } else {
                throw Py.TypeError("an integer is required");
            }

            // } else if (iterable instanceof PyString) {
            // // XXX CPython treats a str/bytes as an iterable, not as previously here:
            // fromstring(((PyString)iterable).toString());

        } else if (iterable instanceof PyArray) {
            PyArray source = (PyArray) iterable;
            if (!source.typecode.equals(typecode)) {
                throw Py.TypeError("can only extend with array of same kind");
            }
            resizeCheck();  // Prohibited if exporting a buffer
            delegate.appendArray(source.delegate.copyArray());

        } else {
            extendInternalIter(iterable);
        }
    }

    /**
     * Internal extend function to process iterable objects.
     *
     * @param iterable any object that can be iterated over.
     */
    private void extendInternalIter(PyObject iterable) {

        // Prohibited operation if exporting a buffer
        resizeCheck();

        if (iterable.__findattr__("__len__") != null) {
            // Make room according to source length
            int last = delegate.getSize();
            delegate.ensureCapacity(last + iterable.__len__());
            for (PyObject item : iterable.asIterable()) {
                set(last++, item);
                delegate.size++;
            }

        } else {
            // iterable has no length property: cannot size the array so append each item.
            for (PyObject item : iterable.asIterable()) {
                appendUnchecked(item); // we already did a resizeCheck
            }
        }
    }

    /**
     * Helper used only when the array elements are Unicode characters (<code>typecode=='u'</code>).
     * (Characters are stored as integer point codes.) The parameter must be an iterable yielding
     * {@link PyUnicode}s. Often this will be an instance of {@link PyUnicode}, which is an iterable
     * yielding single-character {@code PyUnicode}s. But it is also acceptable to this method for
     * the argument to yield arbitrary {@code PyUnicode}s, which will be concatenated in the array.
     *
     * @param iterable of {@link PyUnicode}s
     */
    private void extendUnicodeIter(PyObject iterable) {

        // Prohibited operation if exporting a buffer
        resizeCheck();

        try {
            // Append all the code points of all the strings in the iterable
            for (PyObject item : iterable.asIterable()) {
                PyUnicode uitem = (PyUnicode) item;
                // Append all the code points of this item
                for (int codepoint : uitem.toCodePoints()) {
                    int afterLast = delegate.getSize();
                    delegate.makeInsertSpace(afterLast);
                    Array.setInt(data, afterLast, codepoint);
                }
            }
        } catch (ClassCastException e) {
            // One of the PyUnicodes wasn't
            throw notCompatibleTypeError();
        }
    }

    private void extendArray(int[] items) {

        // Prohibited operation if exporting a buffer
        resizeCheck();

        int last = delegate.getSize();
        delegate.ensureCapacity(last + items.length);
        for (int item : items) {
            Array.set(data, last++, item);
            delegate.size++;
        }
    }

    @ExposedMethod
    public final void array_fromfile(PyObject f, int count) {
        fromfile(f, count);
    }

    /**
     * Read {@code count} items (as machine values) from the file object {@code f} and append them
     * to the end of the array. If less than {@code count} items are available, EOFError is raised,
     * but the items that were available are still inserted into the array. {@code f} must be a real
     * built-in file object; something else with a read() method won't do.
     *
     * @param f Python builtin file object to retrieve data
     * @param count number of array elements to read
     */
    public void fromfile(PyObject f, int count) {
        /*
         * Prohibit when exporting a buffer. Different from CPython, BufferError takes precedence in
         * Jython over EOFError: if there's nowhere to write the data, we don't read it.
         */
        resizeCheck();

        /*
         * Now get the required number of bytes from the file. Guard against non-file or closed.
         */
        if (f instanceof PyFile) {
            PyFile file = (PyFile) f;
            if (!file.getClosed()) {
                // Load required amount or whatever is available into a bytes object
                int readbytes = count * getStorageSize();
                String buffer = file.read(readbytes).toString();
                fromstring(buffer);
                // check for underflow
                if (buffer.length() < readbytes) {
                    int readcount = buffer.length() / getStorageSize();
                    throw Py.EOFError("not enough items in file. " + Integer.toString(count)
                            + " requested, " + Integer.toString(readcount) + " actually read");
                }
            }
            return;
        }
        throw Py.TypeError("arg1 must be open file");
    }

    @ExposedMethod
    public final void array_fromlist(PyObject obj) {
        fromlist(obj);
    }

    /**
     * Append items from the list. This is equivalent to {@code for x in list: a.append(x)} except
     * that if there is a type error, the array is unchanged.
     *
     * @param obj input list object that will be appended to the array
     */
    public void fromlist(PyObject obj) {
        if (!(obj instanceof PyList)) {
            throw Py.TypeError("arg must be list");
        }

        // Prohibited operation if exporting a buffer
        resizeCheck();

        // store the current size of the internal array
        int size = delegate.getSize();
        try {
            extendInternalIter(obj);
        } catch (PyException e) {
            // trap any exception - any error invalidates the whole list
            delegate.setSize(size);
            // re-throw
            throw new PyException(e.type, e.value);
        }
    }

    /**
     * Generic stream reader to read the entire contents of a stream into the array.
     *
     * @param is InputStream to source the data from
     *
     * @return number of primitives successfully read
     *
     * @throws IOException
     * @throws EOFException
     */
    private int fromStream(InputStream is) throws IOException, EOFException {
        return fromStream(is, is.available() / getStorageSize());
    }

    /**
     * Generic stream reader to read {@code count} primitive types from a stream into the array.
     *
     * @param is InputStream to source the data from
     * @param count number of primitive types to read from the stream
     *
     * @return number of primitives successfully read
     *
     * @throws IOException
     * @throws EOFException
     */
    private int fromStream(InputStream is, int count) throws IOException, EOFException {

        // Current number of items present
        int origsize = delegate.getSize();

        // Read into the array, after the current contents, up to new size (or EOF thrown)
        int n = fromStream(is, origsize, origsize + count, true);
        return n - origsize;
    }

    /**
     * Read primitive values from a stream into the array without resizing. Data is read until the
     * array is filled or the stream runs out. If the stream does not contain a whole number of
     * items (possible if the item size is not one byte), the behaviour in respect of the final
     * partial item and straem position is not defined.
     *
     * @param is InputStream to source the data from
     * @return number of primitives successfully read
     * @throws IOException reflecting I/O errors during reading
     */
    public int fillFromStream(InputStream is) throws IOException {
        return fromStream(is, 0, delegate.size, false);
    }

    /**
     * Helper for reading primitive values from a stream into a slice of the array. Data is read
     * until the array slice is filled or the stream runs out. The purpose of the method is to
     * concentrate in one place the manipulation of bytes into the several primitive element types
     * on behalf of {@link #fillFromStream(InputStream)} etc.. The storage is resized if the slice
     * being written ends beyond the current end of the array, i.e. it is increased to the value of
     * {@code limit}.
     * <p>
     * Since different read methods respond differently to it, the caller must specify whether the
     * exhaustion of the stream (EOF) should be treated as an error or not. If the stream does not
     * contain a whole number of items (possible if the item size is not one byte), the behaviour in
     * respect of the final partial item and stream position is not defined.
     *
     * @param dis data stream source for the values
     * @param index first element index to read
     * @param limit first element index <b>not</b> to read
     * @param eofIsError if true, treat EOF as expected way to end
     * @return index of first element not read (<code>=limit</code>, if not ended by EOF)
     * @throws IOException reflecting I/O errors during reading
     * @throws EOFException if stream ends before read is satisfied and eofIsError is true
     */
    private int fromStream(InputStream is, int index, int limit, boolean eofIsError)
            throws IOException, EOFException {

        // Ensure the array is dimensioned to fit the data expected
        if (limit > delegate.getSize()) {
            // Prohibited operation if exporting a buffer
            resizeCheck();
            delegate.setSize(limit);
        }

        // We need a wrapper capable of decoding the data from the representation defined by Java.
        DataInputStream dis = new DataInputStream(is);

        try {
            // We have to deal with each primitive itemClass as a distinct case
            if (itemClass.isPrimitive()) {
                switch (typecode.charAt(0)) {
                    case 'z':
                        for (; index < limit; index++) {
                            Array.setBoolean(data, index, dis.readBoolean());
                        }
                        break;
                    case 'b':
                        for (; index < limit; index++) {
                            Array.setByte(data, index, dis.readByte());
                        }
                        break;
                    case 'B':
                        for (; index < limit; index++) {
                            Array.setShort(data, index, unsignedByte(dis.readByte()));
                        }
                        break;
                    case 'u':
                        // use 32-bit integers since we want UCS-4 storage
                        for (; index < limit; index++) {
                            Array.setInt(data, index, dis.readInt());
                        }
                        break;
                    case 'c':
                        for (; index < limit; index++) {
                            Array.setChar(data, index, (char) (dis.readByte() & 0xff));
                        }
                        break;
                    case 'h':
                        for (; index < limit; index++) {
                            Array.setShort(data, index, dis.readShort());
                        }
                        break;
                    case 'H':
                        for (; index < limit; index++) {
                            Array.setInt(data, index, unsignedShort(dis.readShort()));
                        }
                        break;
                    case 'i':
                        for (; index < limit; index++) {
                            Array.setInt(data, index, dis.readInt());
                        }
                        break;
                    case 'I':
                        for (; index < limit; index++) {
                            Array.setLong(data, index, unsignedInt(dis.readInt()));
                        }
                        break;
                    case 'l':
                        for (; index < limit; index++) {
                            Array.setLong(data, index, dis.readLong());
                        }
                        break;
                    case 'L': // faking it
                        for (; index < limit; index++) {
                            Array.setLong(data, index, dis.readLong());
                        }
                        break;
                    case 'f':
                        for (; index < limit; index++) {
                            Array.setFloat(data, index, dis.readFloat());
                        }
                        break;
                    case 'd':
                        for (; index < limit; index++) {
                            Array.setDouble(data, index, dis.readDouble());
                        }
                        break;
                }
            }

        } catch (EOFException eof) {
            if (eofIsError) {
                throw eof;
            }
            // EOF = end of reading: excess odd bytes read inside dis.readXXX() discarded
        }

        // index points to the first element *not* written
        return index;
    }

    /**
     * Appends items from the object, which is a byte string of some kind (PyString or object with
     * the buffer interface providing bytes) The string of bytes is interpreted as an array of
     * machine values (as if it had been read from a file using the {@link #fromfile(PyObject, int)
     * fromfile()} method).
     *
     * @param input string of bytes containing array data
     */
    public void fromstring(PyObject input) {
        array_fromstring(input);
    }

    /**
     * Appends items from the string, interpreting the string as an array of machine values (as if
     * it had been read from a file using the {@link #fromfile(PyObject, int) fromfile()} method).
     *
     * @param input string of bytes containing array data
     */
    public void fromstring(String input) {
        frombytesInternal(StringUtil.toBytes(input));
    }

    /**
     * Appends items from the string, interpreting the string as an array of machine values (as if
     * it had been read from a file using the {@link #fromfile(PyObject, int) fromfile()} method).
     *
     * @param input string of bytes containing array data
     */
    @ExposedMethod
    final void array_fromstring(PyObject input) {

        if (input instanceof BufferProtocol) {

            if (input instanceof PyUnicode) {
                // Unicode is treated as specifying a byte string via the default encoding.
                String s = ((PyUnicode) input).encode();
                frombytesInternal(StringUtil.toBytes(s));

            } else {
                // Access the bytes through the abstract API of the BufferProtocol
                try (PyBuffer pybuf = ((BufferProtocol) input).getBuffer(PyBUF.STRIDED_RO)) {
                    if (pybuf.getNdim() == 1) {
                        if (pybuf.getStrides()[0] == 1) {
                            // Data are contiguous in the buffer
                            frombytesInternal(pybuf.getNIOByteBuffer());
                        } else {
                            // As frombytesInternal only knows contiguous bytes, make a copy.
                            byte[] copy = new byte[pybuf.getLen()];
                            pybuf.copyTo(copy, 0);
                            frombytesInternal(ByteBuffer.wrap(copy));
                        }
                    } else {
                        // Currently don't support n-dimensional sources
                        throw Py.ValueError("multi-dimensional buffer not supported");
                    }
                }
            }

        } else {
            String fmt = "must be string or read-only buffer, not %s";
            throw Py.TypeError(String.format(fmt, input.getType().fastGetName()));
        }
    }

    /**
     * Common code supporting Java and Python versions of <code>.fromstring()</code> or
     * <code>.frombytes()</code> (Python 3.2+ name).
     *
     * @param bytes array containing the new array data in machine encoding
     */
    private final void frombytesInternal(byte[] bytes) {
        frombytesInternal(ByteBuffer.wrap(bytes));
    }

    /**
     * Copy into this array, the remaining bytes of a ByteBuffer (from the current position to the
     * limit). This is common code supporting Java and Python versions of <code>.fromstring()</code>
     * or <code>.frombytes()</code> (Python 3.2+ name).
     *
     * @param bytes buffer containing the new array data in machine encoding
     */
    private final void frombytesInternal(ByteBuffer bytes) {

        // Access the bytes
        int origsize = delegate.getSize();

        // Check validity wrt array itemsize
        int itemsize = getStorageSize();
        int count = bytes.remaining();
        if ((count % itemsize) != 0) {
            throw Py.ValueError("string length not a multiple of item size");
        }

        // Prohibited operation if we are exporting a buffer
        resizeCheck();

        try {

            // Provide argument as stream of bytes for fromstream method
            InputStream is = new ByteBufferBackedInputStream(bytes);
            fromStream(is);

        } catch (EOFException e) {
            // stubbed catch for fromStream throws
            throw Py.EOFError("not enough items in string");

        } catch (IOException e) {
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
     * Get the element at position {@code i} from the array
     *
     * @param i index of the item to be retrieved from the array
     */
    @Override
    protected PyObject pyget(int i) {
        if ("u".equals(typecode)) {
            return new PyUnicode(Array.getInt(data, i));
        }
        return Py.java2py(Array.get(data, i));
    }

    /**
     * Return the internal Java array storage of the {@code PyArray} instance
     *
     * @return the {@code Array} store.
     */
    public Object getArray() throws PyIgnoreMethodTag {
        return delegate.copyArray();
    }

    /**
     * Getter for the item size of the array element type.
     * <p>
     * The sizes returned by this method represent the number of bytes used to store the type. In
     * the case of streams, this is the number of bytes written to, or read from a stream. For
     * memory this value is the <em>minimum</em> number of bytes required to store the type.
     * <p>
     * This method is used by other methods to define read/write quanta from strings and streams.
     * <table>
     * <caption>Values returned</caption>
     * <tr>
     * <th>typecode</th>
     * <th>Java type</th>
     * <th>itemsize</th>
     * </tr>
     * <tr>
     * <td>{@code z}</td>
     * <td>{@code boolean}</td>
     * <td>1</td>
     * </tr>
     * <tr>
     * <td>{@code b}</td>
     * <td>{@code byte}</td>
     * <td>1</td>
     * </tr>
     * <tr>
     * <td>{@code c}</td>
     * <td>{@code char}</td>
     * <td>1</td>
     * </tr>
     * <tr>
     * <td>{@code h}</td>
     * <td>{@code short}</td>
     * <td>2</td>
     * </tr>
     * <tr>
     * <td>{@code i}</td>
     * <td>{@code int}</td>
     * <td>4</td>
     * </tr>
     * <tr>
     * <td>{@code l}</td>
     * <td>{@code long}</td>
     * <td>8</td>
     * </tr>
     * <tr>
     * <td>{@code f}</td>
     * <td>{@code float}</td>
     * <td>4</td>
     * </tr>
     * <tr>
     * <td>{@code d}</td>
     * <td>{@code double}</td>
     * <td>8</td>
     * </tr>
     * </table>
     *
     * @return number of bytes used to store array type.
     */
    @ExposedGet(name = "itemsize")
    public int getItemsize() {
        if (itemClass.isPrimitive()) {
            if (itemClass == Boolean.TYPE) {
                return 1;
            } else if (itemClass == Byte.TYPE) {
                return 1;
            } else if (itemClass == Character.TYPE) {
                return 1;
            } else if (itemClass == Short.TYPE) {
                return 2;
            } else if (itemClass == Integer.TYPE) {
                return 4;
            } else if (itemClass == Long.TYPE) {
                return 8;
            } else if (itemClass == Float.TYPE) {
                return 4;
            } else if (itemClass == Double.TYPE) {
                return 8;
            }
        }
        // return something here... could be a calculated size?
        return 0;
    }

    /**
     * Getter for the storage size of the array's type, relevant when serialising to an array of
     * bytes, or the reverse.
     *
     * @return actual storage size
     */
    public int getStorageSize() {
        if (itemClass.isPrimitive()) {
            switch (typecode.charAt(0)) {
                case 'z':
                    return 1;
                case 'b':
                    return 1;
                case 'B':
                    return 1;
                case 'u':
                    return 4;
                case 'c':
                    return 1;
                case 'h':
                    return 2;
                case 'H':
                    return 2;
                case 'i':
                    return 4;
                case 'I':
                    return 4;
                case 'l':
                    return 8;
                case 'L':
                    return 8;
                case 'f':
                    return 4;
                case 'd':
                    return 8;
                default:
                    throw Py.ValueError(
                            "bad typecode (must be c, b, B, u, h, H, i, I, l, L, f or d)");
            }
        }
        // return something here... could be a calculated size?
        return 0;
    }

    /**
     * Retrieve a slice from the array specified by the {@code start}, {@code stop} and
     * {@code step}.
     *
     * @param start start index of the slice
     * @param stop stop index of the slice
     * @param step stepping increment of the slice
     * @return A new PyArray object containing the described slice
     */
    @Override
    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start) {
            stop = start;
        }
        int n = sliceLength(start, stop, step);
        PyArray ret = new PyArray(itemClass, n);
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
     * Getter for the type code of the array. {@link #char2class(char) char2class} describes the
     * possible type codes and their meaning.
     *
     * @return single character type code for the array
     */
    @ExposedGet(name = "typecode")
    public String getTypecode() {
        return typecode;
    }

    @ExposedMethod
    public final int array_index(PyObject value) {
        int index = indexInternal(value);
        if (index != -1) {
            return index;
        }
        throw Py.ValueError("array.index(" + value + "): " + value + " not found in array");
    }

    /**
     * Return the smallest <em>i</em> such that <em>i</em> is the index of the first occurrence of
     * {@code value} in the array.
     *
     * @param value value to find the index of
     * @return index of the first occurrence of {@code value}
     */
    public PyObject index(PyObject value) {
        return Py.newInteger(array_index(value));
    }

    /**
     * Return the smallest <em>i</em> such that <em>i</em> is the index of the first occurrence of
     * {@code value} in the array.
     *
     * @param value value to find the index of
     * @return index of the first occurrence of {@code value}
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
    public final void array_insert(int index, PyObject value) {
        insert(index, value);
    }

    /**
     * Insert a new item with value {@code value} in the array before position {@code index}.
     * Negative values are treated as being relative to the end of the array.
     *
     * @param index insert position
     * @param value value to be inserted into array
     */
    public void insert(int index, PyObject value) {
        resizeCheck();  // Prohibited operation if exporting a buffer
        index = boundToSequence(index);
        if ("u".equals(typecode)) {
            int codepoint = getCodePoint(value);
            delegate.makeInsertSpace(index);
            Array.setInt(data, index, codepoint);

        } else {
            delegate.makeInsertSpace(index);
            Array.set(data, index, Py.tojava(value, itemClass));
        }
    }

    /**
     * Removes the item at the index {@code i} from the array and returns it. The optional argument
     * defaults to -1, so that by default the last item is removed and returned.
     */
    @ExposedMethod(defaults = "-1")
    public final PyObject array_pop(int i) {
        PyObject val = pop(i);
        if ("u".equals(typecode)) {
            return new PyUnicode(val.asInt());
        }
        return val;
    }

    /**
     * Removes the last item from the array and return it.
     */
    public PyObject pop() {
        return pop(-1);
    }

    /**
     * Removes the item with the index {@code index} from the array and returns it.
     *
     * @param index array location to be popped from the array
     * @return array element popped from index
     */
    public PyObject pop(int index) {
        if (delegate.getSize() == 0) {
            throw Py.IndexError("pop from empty array");
        }
        index = delegator.fixindex(index);
        if (index == -1) {
            throw Py.IndexError("pop index out of range");
        }

        // Prohibited operation if exporting a buffer
        resizeCheck();

        PyObject ret = Py.java2py(Array.get(data, index));
        delegate.remove(index);
        return ret;
    }

    @ExposedMethod
    public final void array_remove(PyObject value) {
        remove(value);
    }

    /**
     * Remove the first occurrence of {@code value} from the array.
     *
     * @param value array value to be removed
     */
    public void remove(PyObject value) {
        int index = indexInternal(value);
        if (index != -1) {
            // Prohibited operation if exporting a buffer
            resizeCheck();
            delegate.remove(index);
            return;
        }
        throw Py.ValueError("array.remove(" + value + "): " + value + " not found in array");
    }

    /**
     * Repeat the array {@code count} times.
     *
     * @param count number of times to repeat the array
     * @return A new PyArray object containing the source object repeated {@code count} times.
     */
    @Override
    protected PyObject repeat(int count) {
        Object arraycopy = delegate.copyArray();
        PyArray ret = new PyArray(itemClass, 0);
        ret.typecode = typecode;
        for (int i = 0; i < count; i++) {
            ret.delegate.appendArray(arraycopy);
        }
        return ret;
    }

    @ExposedMethod
    public final void array_reverse() {
        reverse();
    }

    /**
     * Reverse the elements in the array
     */
    public void reverse() {
        // build a new reversed array and set this.data to it when done
        Object array = Array.newInstance(itemClass, Array.getLength(data));
        for (int i = 0, lastIndex = delegate.getSize() - 1; i <= lastIndex; i++) {
            Array.set(array, lastIndex - i, Array.get(data, i));
        }
        data = array;
    }

    /**
     * Set an element in the array - the index needs to exist, this method does not automatically
     * extend the array. See {@link AbstractArray#setSize(int) AbstractArray.setSize()} or
     * {@link AbstractArray#ensureCapacity(int) AbstractArray.ensureCapacity()} for ways to extend
     * capacity.
     * <p>
     * This code specifically checks for overflows of the integral types: byte, short, int and long.
     *
     * @param i index of the element to be set
     * @param value value to set the element to
     */
    public void set(int i, PyObject value) {
        pyset(i, value);
    }

    @Override
    protected void pyset(int i, PyObject value) {

        if ("u".equals(typecode)) {
            Array.setInt(data, i, getCodePoint(value));
            return;
        }

        if (itemClass == Byte.TYPE) {
            long val;
            try {
                val = ((Long) value.__tojava__(Long.TYPE)).longValue();
            } catch (ClassCastException e) {
                throw notCompatibleTypeError();
            }
            if (val < (isSigned() ? 0 : Byte.MIN_VALUE)) {
                throw lessThanMinimum();
            } else if (val > Byte.MAX_VALUE) {
                throw moreThanMaximum();
            }

        } else if (itemClass == Short.TYPE) {
            long val;
            try {
                val = ((Long) value.__tojava__(Long.TYPE)).longValue();
            } catch (ClassCastException e) {
                throw notCompatibleTypeError();
            }
            if (val < (isSigned() ? 0 : Short.MIN_VALUE)) {
                throw lessThanMinimum();
            } else if (val > Short.MAX_VALUE) {
                throw moreThanMaximum();
            }

        } else if (itemClass == Integer.TYPE) {
            long val;
            try {
                val = ((Long) value.__tojava__(Long.TYPE)).longValue();
            } catch (ClassCastException e) {
                throw notCompatibleTypeError();
            }
            if (val < (isSigned() ? 0 : Integer.MIN_VALUE)) {
                throw lessThanMinimum();
            } else if (val > Integer.MAX_VALUE) {
                throw moreThanMaximum();
            }

        } else if (itemClass == Long.TYPE) {
            if (isSigned() && value instanceof PyInteger) {
                if (((PyInteger) value).getValue() < 0) {
                    throw lessThanMinimum();
                }
            } else if (value instanceof PyLong) {
                ((PyLong) value).getLong(isSigned() ? 0 : Long.MIN_VALUE, Long.MAX_VALUE);
            } else {
                Object o;
                try {
                    o = value.__tojava__(Long.TYPE);
                } catch (ClassCastException e) {
                    throw notCompatibleTypeError();
                }
                if (o == Py.NoConversion) {
                    throw notCompatibleTypeError();
                }
            }
        }

        Object o = Py.tojava(value, itemClass);
        if (o == Py.NoConversion) {
            throw notCompatibleTypeError();
        }
        Array.set(data, i, o);
    }

    // xxx - add more efficient comparable typecode lookup via an enumset, and expand
    public void set(int i, int value) {
        if ("u".equals(typecode) || itemClass == Integer.TYPE || itemClass == Long.TYPE) {
            Array.setInt(data, i, value);
        } else {
            throw notCompatibleTypeError();
        }
    }

    public void set(int i, char value) {
        if ("c".equals(typecode) || itemClass == Integer.TYPE || itemClass == Long.TYPE) {
            Array.setChar(data, i, value);
        } else {
            throw notCompatibleTypeError();
        }
    }

    private boolean isSigned() {
        return typecode.length() == 1 && typecode.equals(typecode.toUpperCase());
    }

    /**
     * Sets a slice of the array. {@code value} can be a string (for {@code byte} and {@code char}
     * types) or {@code PyArray}. If a {@code PyArray}, its type must be convertible into the type
     * of the target {@code PyArray}.
     *
     * @param start start index of the delete slice
     * @param stop end index of the delete slice
     * @param step stepping increment of the slice
     */
    @Override
    protected void setslice(int start, int stop, int step, PyObject value) {

        if (stop < start) {
            stop = start;
        }

        if (itemClass == Character.TYPE && value instanceof PyString) {
            char[] chars = null;
            // if (value instanceof PyString) {
            if (step != 1) {
                throw Py.ValueError("invalid bounds for setting from string");
            }
            chars = value.toString().toCharArray();
            if (start + chars.length != stop) {
                // This is a size-changing operation: check for buffer exports
                resizeCheck();
            }
            delegate.replaceSubArray(start, stop, chars, 0, chars.length);

        } else {

            if (value instanceof PyString && itemClass == Byte.TYPE) {
                byte[] chars = ((PyString) value).toBytes();
                if (chars.length == stop - start && step == 1) {
                    System.arraycopy(chars, 0, data, start, chars.length);
                } else {
                    throw Py.ValueError("invalid bounds for setting from string");
                }

            } else if (value instanceof PyArray) {
                PyArray array = (PyArray) value;
                if (!array.typecode.equals(typecode)) {
                    throw Py.TypeError("bad argument type for built-in operation|" + array.typecode
                            + "|" + typecode);
                }

                if (step == 1) {
                    Object arrayDelegate;
                    if (array == this) {
                        arrayDelegate = array.delegate.copyArray();
                    } else {
                        arrayDelegate = array.delegate.getArray();
                    }
                    int len = array.delegate.getSize();
                    if (start + len != stop) {
                        // This is a size-changing operation: check for buffer exports
                        resizeCheck();
                    }
                    try {
                        delegate.replaceSubArray(start, stop, arrayDelegate, 0, len);
                    } catch (IllegalArgumentException e) {
                        throw Py.TypeError("Slice typecode '" + array.typecode
                                + "' is not compatible with this array (typecode '" + this.typecode
                                + "')");
                    }

                } else if (step > 1) {
                    int len = array.__len__();
                    for (int i = 0, j = 0; i < len; i++, j += step) {
                        Array.set(data, j + start, Array.get(array.data, i));
                    }

                } else if (step < 0) {
                    if (array == this) {
                        array = (PyArray) array.clone();
                    }

                    int len = array.__len__();
                    for (int i = 0, j = start; i < len; i++, j += step) {
                        Array.set(data, j, Array.get(array.data, i));
                    }
                }

            } else {
                throw Py.TypeError(
                        String.format("can only assign array (not \"%.200s\") to array " + "slice",
                                value.getType().fastGetName()));
            }
        }
    }

    @ExposedMethod
    public final void array_tofile(PyObject f) {
        tofile(f);
    }

    @ExposedMethod
    public void array_write(PyObject f) {
        tofile(f);
    }

    /**
     * Write all items (as machine values) to the file object {@code f}.
     *
     * @param f Python builtin file object to write data
     */
    public void tofile(PyObject f) {
        if (!(f instanceof PyFile)) {
            throw Py.TypeError("arg must be open file");
        }
        PyFile file = (PyFile) f;
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
     * Generic stream writer to write the entire contents of the array to the stream as primitive
     * types.
     *
     * @param os OutputStream to sink the array data to
     * @return number of bytes successfully written
     * @throws IOException
     */
    public int toStream(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        switch (typecode.charAt(0)) {
            case 'z':
                for (int i = 0; i < delegate.getSize(); i++) {
                    dos.writeBoolean(Array.getBoolean(data, i));
                }
                break;
            case 'b':
                for (int i = 0; i < delegate.getSize(); i++) {
                    dos.writeByte(Array.getByte(data, i));
                }
                break;
            case 'B':
                for (int i = 0; i < delegate.getSize(); i++) {
                    dos.writeByte(signedByte(Array.getShort(data, i)));
                }
                break;
            case 'u':
                // use 32-bit integers since we want UCS-4 storage
                for (int i = 0; i < delegate.getSize(); i++) {
                    dos.writeInt(Array.getInt(data, i));
                }
                break;
            case 'c':
                for (int i = 0; i < delegate.getSize(); i++) {
                    dos.writeByte((byte) Array.getChar(data, i));
                }
                break;
            case 'h':
                for (int i = 0; i < delegate.getSize(); i++) {
                    dos.writeShort(Array.getShort(data, i));
                }
                break;
            case 'H':
                for (int i = 0; i < delegate.getSize(); i++) {
                    dos.writeShort(signedShort(Array.getInt(data, i)));
                }
                break;
            case 'i':
                for (int i = 0; i < delegate.getSize(); i++) {
                    dos.writeInt(Array.getInt(data, i));
                }
                break;
            case 'I':
                for (int i = 0; i < delegate.getSize(); i++) {
                    dos.writeInt(signedInt(Array.getLong(data, i)));
                }
                break;
            case 'l':
                for (int i = 0; i < delegate.getSize(); i++) {
                    dos.writeLong(Array.getLong(data, i));
                }
                break;
            case 'L': // faking it
                for (int i = 0; i < delegate.getSize(); i++) {
                    dos.writeLong(Array.getLong(data, i));
                }
                break;
            case 'f':
                for (int i = 0; i < delegate.getSize(); i++) {
                    dos.writeFloat(Array.getFloat(data, i));
                }
                break;
            case 'd':
                for (int i = 0; i < delegate.getSize(); i++) {
                    dos.writeDouble(Array.getDouble(data, i));
                }
                break;
        }
        return dos.size(); // bytes written
    }

    private static byte signedByte(short x) {
        if (x >= 128 && x < 256) {
            return (byte) (x - 256);
        } else if (x >= 0) {
            return (byte) x;
        } else {
            throw Py.ValueError("invalid storage");
        }
    }

    private static short signedShort(int x) {
        if (x >= 32768 && x < 65536) {
            return (short) (x - 65536);
        } else if (x >= 0) {
            return (short) x;
        } else {
            throw Py.ValueError("invalid storage");
        }
    }

    private static int signedInt(long x) {
        if (x >= 2147483648L && x < 4294967296L) {
            return (int) (x - 4294967296L);
        } else if (x >= 0) {
            return (int) x;
        } else {
            throw Py.ValueError("invalid storage");
        }
    }

    private static short unsignedByte(byte x) {
        if (x < 0) {
            return (short) (x + 256);
        } else {
            return x;
        }
    }

    private static int unsignedShort(short x) {
        if (x < 0) {
            return x + 65536;
        } else {
            return x;
        }
    }

    private static long unsignedInt(int x) {
        if (x < 0) {
            return x + 4294967296L;
        } else {
            return x;
        }

    }

    @ExposedMethod
    public final PyObject array_tostring() {
        return new PyString(tostring());
    }

    /**
     * Convert the array to an array of machine values and return the string representation (the
     * same sequence of bytes that would be written to a file by the {@link #tofile(PyObject)
     * tofile()} method.)
     */
    public String tostring() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            toStream(bos);
        } catch (IOException e) {
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
        for (int i = 0; i < len; i++) {
            codepoints[i] = Array.getInt(data, i);
        }
        return new String(codepoints, 0, codepoints.length);
    }

    @ExposedMethod
    public final PyObject array_tounicode() {
        return new PyUnicode(tounicode());
    }

    // PyArray can't extend anymore, so delegate
    private class ArrayDelegate extends AbstractArray {

        /**
         * Construct a delegate that manages the {@link PyArray#data} in the containing
         * {@code PyArray} or a private array of length zero it creates (if {@link PyArray#data} is
         * {@code null} at the time of the call).
         */
        private ArrayDelegate() {
            // When do we need the data==null path?
            super(data == null ? 0 : Array.getLength(data));
        }

        @Override
        protected Object getArray() {
            return data;
        }

        @Override
        protected void setArray(Object array) {
            data = array;
        }

        @Override
        protected Object createArray(int size) {
            Class<?> baseType = data.getClass().getComponentType();
            return Array.newInstance(baseType, size);
        }
    }

    /*
     * ============================================================================================
     * Support for the Buffer API
     * ============================================================================================
     *
     * The buffer API allows other classes to access the storage directly.
     *
     * This is a close duplicate of the same mechanism in PyByteArray. There is perhaps scope for a
     * shared helper class to implement this logic. For type code 'b', the workings are almost
     * identical. The fully-fledged buffer interface for PyArray is richer, more like the Python 3
     * memoryview, as it must cope with items of size other than one byte. This goes beyond the
     * capabilities of the Jython BufferProtocol at this stage of its development.
     */

    /**
     * Hold weakly a reference to a PyBuffer export not yet released, used to prevent untimely
     * resizing.
     */
    private WeakReference<BaseBuffer> export;

    /**
     * {@inheritDoc}
     * <p>
     * The {@link PyBuffer} returned from this method is a one-dimensional array of single byte
     * items that allows modification of the object state. The existence of this export <b>prohibits
     * resizing</b> the array. This prohibition is not only on the consumer of the view but extends
     * to operations on the underlying array, such as {@link #insert(int, PyObject)} or
     * {@link #pop()}.
     */
    @Override
    public synchronized PyBuffer getBuffer(int flags) {

        if ((flags & ~PyBUF.WRITABLE) == PyBUF.SIMPLE) {
            // Client requests a flat byte-oriented read-view, typically from buffer(a).

            // If we have already exported a buffer it may still be available for re-use
            BaseBuffer pybuf = getExistingBuffer(flags);

            if (pybuf == null) {
                // No existing export we can re-use: create a new one
                if ("b".equals(typecode)) {
                    // This is byte data, so we can export directly
                    byte[] storage = (byte[]) data;
                    int size = delegate.getSize();
                    pybuf = new SimpleBuffer(flags, this, storage, 0, size);
                } else {
                    // As the client only intends to read, fake the answer with a String
                    pybuf = new SimpleStringBuffer(flags, this, tostring());
                }
                // Hold a reference for possible re-use
                export = new WeakReference<BaseBuffer>(pybuf);
            }

            return pybuf;

        } else {
            // Client request goes beyond Python 2 capability, typically from memoryview(a).
            throw new ClassCastException("'array' supports only a byte-buffer view");
        }
    }

    /**
     * Try to re-use an existing exported buffer, or return {@code null} if we can't.
     *
     * @throws PyException {@code BufferError} if the the flags are incompatible with the buffer
     */
    private BaseBuffer getExistingBuffer(int flags) throws PyException {
        BaseBuffer pybuf = null;
        if (export != null) {
            // A buffer was exported at some time.
            pybuf = export.get();
            if (pybuf != null) {
                /*
                 * We do not test for pybuf.isReleased() as, if any operation had taken place that
                 * invalidated the buffer, resizeCheck() would have set export=null. The exported
                 * buffer (navigation, buf member, etc.) remains valid through any operation that
                 * does not need a resizeCheck.
                 */
                pybuf = pybuf.getBufferAgain(flags);
            }
        }
        return pybuf;
    }

    /**
     * Test to see if the array may be resized and raise a BufferError if not. This must be called
     * by the implementation of any operation that changes the number of elements in the array.
     *
     * @throws PyException {@code BufferError} if there are buffer exports preventing a resize
     */
    private void resizeCheck() throws PyException {
        if (export != null) {
            // A buffer was exported at some time and we have not explicitly discarded it.
            PyBuffer pybuf = export.get();
            if (pybuf != null && !pybuf.isReleased()) {
                // A consumer still has the exported buffer
                throw Py.BufferError("cannot resize an array that is exporting buffers");
            } else {
                /*
                 * Either the reference has expired or all consumers have released it. Either way,
                 * the weak reference is useless now.
                 */
                export = null;
            }
        }
    }

    /**
     * Wrap a {@link ByteBuffer} in an InputStream. Reference: <a
     * href=http://stackoverflow.com/questions/4332264/wrapping-a-bytebuffer-with-an-inputstream>
     * Stackoverflow question 4332264</a>.
     */
    private class ByteBufferBackedInputStream extends InputStream {

        ByteBuffer buf;

        public ByteBufferBackedInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        /**
         * Return the number of bytes remaining in the underlying buffer.
         */
        @Override
        public int available() throws IOException {
            return buf.remaining();
        }

        @Override
        public int read() {
            return buf.hasRemaining() ? buf.get() & 0xff : -1;
        }

        @Override
        public int read(byte[] bytes, int off, int len) {
            int n = buf.remaining();
            if (n >= len) {
                // There are enough bytes remaining to satisfy the request.
                buf.get(bytes, off, len);
                return len;
            } else if (n > 0) {
                // There are some bytes remaining: truncate request.
                buf.get(bytes, off, n);
                return n;
            } else {
                // Signal that there are no bytes left
                return -1;
            }
        }
    }

    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        if (data == null || !gc.canLinkToPyObject(data.getClass(), true)) {
            return 0;
        }
        return gc.traverseByReflection(data, visit, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) throws UnsupportedOperationException {
        if (data == null || !gc.canLinkToPyObject(data.getClass(), true)) {
            return false;
        }
        throw new UnsupportedOperationException();
    }

    private static final String TYPE_NOT_COMPATIBLE = "Type not compatible with array of '%s'";

    /**
     * Create throwable {@code TypeError} along the lines "Type not compatible with array of
     * TYPECODE", where TYPECODE is the element type of the array.
     *
     * @return the {@code TypeError}
     */
    private PyException notCompatibleTypeError() {
        return Py.TypeError(String.format(TYPE_NOT_COMPATIBLE, typecode));
    }

    /**
     * Create throwable {@code OverflowError} along the lines "TYPE-array value is less than
     * minimum", where TYPE is the element type of the array.
     *
     * @return the {@code OverflowError}
     */
    private PyException lessThanMinimum() {
        return Py.OverflowError(
                String.format("'%s'-array value is less than minimum", itemClass.getName()));
    }

    /**
     * Create throwable {@code OverflowError} along the lines "TYPE-array value is more than
     * maximum", where TYPE is the element type of the array.
     *
     * @return the {@code OverflowError}
     */
    private PyException moreThanMaximum() {
        return Py.OverflowError(
                String.format("'%s'-array value is more than maximum", itemClass.getName()));
    }

}
