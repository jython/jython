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
import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.python.core.PyArray.ItemType;
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
 *
 * <table>
 * <caption>Supported item types</caption>
 * <tr>
 * <th>typecode</th>
 * <th>Python type</th>
 * <th>Java type</th>
 * <th>serialised size</th>
 * <th>signed</th>
 * </tr>
 * <tr>
 * <td>{@code b}</td>
 * <td>{@code int}</td>
 * <td>{@code byte}</td>
 * <td>1</td>
 * </tr>
 * <tr>
 * <td>{@code B}</td>
 * <td>{@code int}</td>
 * <td>{@code byte}</td>
 * <td>1</td>
 * <td>unsigned</td>
 * </tr>
 * <tr>
 * <td>{@code h}</td>
 * <td>{@code int}</td>
 * <td>{@code short}</td>
 * <td>2</td>
 * </tr>
 * <tr>
 * <td>{@code H}</td>
 * <td>{@code int}</td>
 * <td>{@code short}</td>
 * <td>2</td>
 * <td>unsigned</td>
 * </tr>
 * <tr>
 * <td>{@code i}</td>
 * <td>{@code int}</td>
 * <td>{@code int}</td>
 * <td>4</td>
 * </tr>
 * <tr>
 * <td>{@code I}</td>
 * <td>{@code long}</td>
 * <td>{@code int}</td>
 * <td>4</td>
 * <td>unsigned</td>
 * </tr>
 * <tr>
 * <td>{@code l}</td>
 * <td>{@code long}</td>
 * <td>{@code long}</td>
 * <td>8</td>
 * </tr>
 * <tr>
 * <td>{@code L}</td>
 * <td>{@code long}</td>
 * <td>{@code long}</td>
 * <td>8</td>
 * <td>unsigned</td>
 * </tr>
 * <tr>
 * <td>{@code f}</td>
 * <td>{@code float}</td>
 * <td>{@code float}</td>
 * <td>4</td>
 * </tr>
 * <tr>
 * <td>{@code d}</td>
 * <td>{@code float}</td>
 * <td>{@code double}</td>
 * <td>8</td>
 * </tr>
 * <tr>
 * <td>{@code c}</td>
 * <td>{@code str}</td>
 * <td>{@code byte}</td>
 * <td>1</td>
 * <td>unsigned</td>
 * </tr>
 * <tr>
 * <td>{@code u}</td>
 * <td>{@code unicode}</td>
 * <td>{@code int}</td>
 * <td>1</td>
 * <td>unsigned</td>
 * </tr>
 * <tr>
 * <td>{@code z}</td>
 * <td>{@code bool}</td>
 * <td>{@code boolean}</td>
 * <td>1</td>
 * </tr>
 * </table>
 * Types shown as "unsigned" represent positive values encoded in the same number of bits as the
 * equivalent signed type. The Java value serialised makes no distinction. The consumer of the
 * stream has to know whether to make a signed or unsigned interpretation of the bits. When reading
 * a stream, the type code declared for the destination array decides the interpretation of the
 * bytes.
 */
@ExposedType(name = "array.array", base = PyObject.class)
public class PyArray extends PySequence implements Cloneable, BufferProtocol, Traverseproc {

    public static final PyType TYPE = PyType.fromClass(PyArray.class);

    /** The underlying Java array, a Java Array in practice. */
    private Object data;

    /** The Java class of elements in the {@code data} array. */
    private Class<?> itemClass;

    /** Everything else we need to know about the type of elements in the {@code data} array. */
    private ItemType itemType;

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
     * Create a {@code PyArray} with the given array item type, specific item class and content.
     *
     * The primary specification is the {@link ItemType} parameter and if that is
     * {@link ItemType#OBJECT} a specific Java class for the items.
     *
     * In a subtle twist, if {@code itemType =} {@link ItemType#OBJECT} but {@code itemClass} is one
     * of the types used to implement the "single letter" type codes, the item type of the array
     * will be the first signed type represented by that class, not {@code OBJECT}. This is to
     * preserve a legacy behaviour of the {@code PyArray} constructors.
     *
     * @param subtype actual Python type
     * @param itemType of the elements
     * @param itemClass when {@code itemType =} {@link ItemType#OBJECT}
     * @param data content array
     */
    PyArray(PyType subtype, ItemType itemType, Class<?> itemClass, Object data) {
        this(subtype);
        setElementType(itemType, itemClass);
        setData(data);
    }

    /**
     * Create a {@code PyArray} with the given array item type, specific item class and length, but
     * zero content.
     *
     * Roughly equivalent to<pre>
     * PyArray(itemType, itemClass, Array.newInstance(itemClass, n))
     * </pre> But with {@code itemClass} for the new array deduced from {@code itemType} as
     * explained in {@link PyArray#PyArray(ItemType, Class, Object)}.
     *
     * @param subtype actual Python type
     * @param itemType of the elements
     * @param itemClass when {@code itemType =} {@link ItemType#OBJECT}
     * @param n length of content array to create
     */
    PyArray(PyType subtype, ItemType itemType, Class<?> itemClass, int n) {
        this(subtype);
        setElementType(itemType, itemClass);
        setData(Array.newInstance(this.itemClass, n));
    }

    /**
     * Create a {@code PyArray} with the given array item class and content initialised from a
     * Python object (like an iterable).
     *
     * @param subtype actual Python type
     * @param itemType of the elements
     * @param itemClass when {@code itemType =} {@link ItemType#OBJECT}
     * @param initial provider of initial contents
     */
    public PyArray(PyType subtype, ItemType itemType, Class<?> itemClass, PyObject initial) {
        this(subtype, itemType, itemClass, 0);
        useInitial(initial);
    }

    /**
     * Create a {@code PyArray} with the given array item class and content. If {@code itemClass} is
     * one of the primitive types used to implement the "single letter" type codes, the type code of
     * the array will be a signed zero of that item class.
     *
     * @param itemClass of elements in the array
     * @param data content array
     */
    public PyArray(Class<?> itemClass, Object data) {
        this(TYPE, ItemType.OBJECT, itemClass, data);
    }

    /**
     * Create a {@code PyArray} with the given array item class and content initialised from a
     * Python object (iterable).
     *
     * @param itemClass of elements in the array
     * @param initial provider of initial contents
     */
    public PyArray(Class<?> itemClass, PyObject initial) {
        this(TYPE, ItemType.OBJECT, itemClass, initial);
    }

    /**
     * Create a {@code PyArray} with the given array item class and number of zero or {@code null}
     * elements. If {@code itemClass} is one of the primitive types used to implement the "single
     * letter" type codes, the type code of the array will be a signed zero of that item class.
     *
     * @param itemClass of elements in the array
     * @param n number of (zero or {@code null}) elements
     */
    public PyArray(Class<?> itemClass, int n) {
        this(TYPE, ItemType.OBJECT, itemClass, Array.newInstance(itemClass, n));
    }

    /**
     * Create a {@code PyArray} as a copy of another.
     *
     * @param toCopy the other array
     */
    public PyArray(PyArray toCopy) {
        this(TYPE, toCopy.itemType, toCopy.itemClass, toCopy.delegate.copyArray());
    }

    /**
     * Initialise this array from an {@link ItemType} (from a Python {@code array.array} type code
     * character) and if that is {@link ItemType#OBJECT} a specific Java class for the items.
     * <p>
     * If {@code itemType =} {@link ItemType#OBJECT} but {@code itemClass} is one of the types used
     * to implement the "single letter" type codes, the item type of the array will be the first
     * signed type represented by that class, not {@code OBJECT}. This is to preserve a legacy
     * behaviour of the {@code PyArray} constructors.
     * <p>
     * The way {@link #array_new(PyNewWrapper, boolean, PyType, PyObject[], String[]) array_new}
     * works, and the constructors, is to create an instance with the almost parameterless
     * {@link #PyArray(PyType)} with sub-type argument.
     * <p>
     * This blank canvas needs to be inscribed with a consistent state by a call to this method and
     * either {@link #setData(Object) setData} or {@link #useInitial(PyObject) useInitial}.
     *
     * @param itemType of the elements
     * @param itemClass when {@code itemType =} {@link ItemType#OBJECT}
     */
    private void setElementType(ItemType itemType, Class<?> itemClass) {
        if (itemType == ItemType.OBJECT) {
            /*
             * If itemClass is one of the types used to implement the "single letter" type codes,
             * the item type of the array will be the first signed type represented by that class,
             * not OBJECT. This is to preserve a legacy behaviour of the PyArray constructors.
             */
            this.itemClass = itemClass;
            this.itemType = ItemType.fromJavaClass(itemClass);
        } else {
            // itemType tells the whole story
            this.itemType = itemType;
            this.itemClass = itemType.itemClass;
        }
    }

    /**
     * Make a given object the storage for the array. Normally this is a Java array of type
     * consistent with the element type. It will be manipulated by {@link #delegate}.
     *
     * @param data the storage (or {@code null} to to create at zero length consistent with type).
     */
    private void setData(Object data) {
        this.data = data != null ? data : Array.newInstance(itemClass, 0);
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

        } else if (itemType == ItemType.UNICHAR) {
            if (initial instanceof PyUnicode) {
                extendArray((PyUnicode) initial);
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
            self.setElementType(ItemType.fromTypecode(typecode), null);
        } else if (obj instanceof PyJavaType) {
            Class<?> itemClass = ((PyJavaType) obj).getProxyType();
            self.setElementType(ItemType.OBJECT, itemClass);
        } else {
            throw Py.TypeError(
                    "array() argument 1 must be char, not " + obj.getType().fastGetName());
        }

        // Fill the array from the second argument (if there is one)
        self.useInitial(ap.getPyObject(1, null));
        return self;
    }

    /**
     * Create a {@code PyArray} with the given array type code and number of zero elements.
     *
     * @param typecode of elements in the array
     * @param n number of (zero or {@code null}) elements
     * @return created array
     */
    public static PyArray zeros(int n, char typecode) {
        return new PyArray(TYPE, ItemType.fromTypecode(typecode), null, n);
    }

    /**
     * Create a {@code PyArray} with the given array item class and number of zero or {@code null}
     * elements. If {@code itemClass} is one of the primitive types used to implement the "single
     * letter" type codes, the type code of the array will be a signed zero of that item class.
     *
     * @param itemClass
     * @param n number of (zero or {@code null}) elements
     * @return created array
     */
    public static PyArray zeros(int n, Class<?> itemClass) {
        return new PyArray(TYPE, ItemType.OBJECT, itemClass, n);
    }

    /**
     * Create a {@code PyArray} with the given array item type and content initialised from a Python
     * object (iterable).
     *
     * @param seq to suply content
     * @param typecode
     * @return created array
     */
    public static PyArray array(PyObject seq, char typecode) {
        return new PyArray(TYPE, ItemType.fromTypecode(typecode), null, seq);
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
        return new PyArray(TYPE, ItemType.OBJECT, itemClass, init);
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
     * @param otherObject supposed {@code PyArray}
     * @return {@code other}
     * @throws ClassCastException if {@code other} not {@code PyArray}
     */
    private PyArray arrayChecked(PyObject otherObject) throws ClassCastException {
        PyArray other = (PyArray) otherObject;
        if (itemType == other.itemType) {
            if (itemType != ItemType.OBJECT) {
                return other;
            } else if (itemClass.isAssignableFrom(other.itemClass)) {
                return other;
            }
        }
        throw Py.TypeError(String.format("bad argument types for built-in operation: (%s, %s)",
                reprTypecode(), other.reprTypecode()));
    }

    /**
     * Length of the array (as the number of elements, not a storage size).
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
        PyString typecode = Py.newString(getTypecode());
        if (__len__() > 0) {
            return new PyTuple(getType(), new PyTuple(typecode, Py.newString(tostring())), dict);
        } else {
            return new PyTuple(getType(), new PyTuple(typecode), dict);
        }
    }

    @Override
    public String toString() {
        if (__len__() == 0) {
            return String.format("array(%s)", reprTypecode());
        }
        String value;
        switch (itemType) {
            case CHAR:
                value = PyString.encode_UnicodeEscape(tostring(), true);
                break;
            case UNICHAR:
                value = (new PyUnicode(tounicode())).__repr__().toString();
                break;
            default:
                value = tolist().toString();
        }
        return String.format("array(%s, %s)", reprTypecode(), value);
    }

    private String reprTypecode() {
        if (itemType == ItemType.OBJECT) {
            return getTypecode();
        } else {
            return "'" + getTypecode() + "'";
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
                // when unboxing, shrink the array first, otherwise incorrect results to Java
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
        int afterLast = delegate.getSize();
        delegate.makeInsertSpace(afterLast);
        try {
            pyset(afterLast, value);
        } catch (PyException e) {
            delegate.setSize(afterLast);
            throw new PyException(e.type, e.value);
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
        if (itemType == ItemType.OBJECT) {
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
     *
     * @param typecode character code for the array type
     * @return {@code Class} of the native itemClass
     */
    public static Class<?> char2class(char typecode) throws PyIgnoreMethodTag {
        return ItemType.fromTypecode(typecode).itemClass;
    }

    @ExposedMethod
    public final int array_count(PyObject value) {
        // note: cpython does not raise type errors based on item type;
        int iCount = 0;
        int len = delegate.getSize();
        for (int i = 0; i < len; i++) {
            if (value.equals(itemType.get(this, i))) {
                iCount++;
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
            if (itemType == ItemType.UNICHAR) {
                extendUnicodeIter(iterable);
            } else if (itemType == ItemType.CHAR) {
                throw Py.TypeError("array item must be char");
            } else {
                throw Py.TypeError("an integer is required");
            }

        } else if (iterable instanceof PyArray) {
            PyArray source = (PyArray) iterable;
            if (source.itemType == itemType) {
                resizeCheck();  // Prohibited if exporting a buffer
                delegate.appendArray(source.delegate.copyArray());
            } else {
                throw Py.TypeError("can only extend with array of same kind");
            }

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
                pyset(last++, item);
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

    private void extendArray(PyUnicode codepoints) {

        // Prohibited operation if exporting a buffer
        resizeCheck();

        int last = delegate.getSize();
        int[] items = codepoints.toCodePoints();
        delegate.ensureCapacity(last + items.length);
        for (int item : items) {
            Array.setInt(data, last++, item);
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
                int readbytes = count * itemType.itemsize;
                String buffer = file.read(readbytes).toString();
                fromstring(buffer);
                // check for underflow
                if (buffer.length() < readbytes) {
                    int readcount = buffer.length() / itemType.itemsize;
                    throw Py.EOFError(String.format(NOT_ENOUGH_IN_FILE, count, readcount));
                }
            }
            return;
        }
        throw Py.TypeError("arg1 must be open file");
    }

    private static final String NOT_ENOUGH_IN_FILE =
            "not enough items in file. %d requested, %d actually read";

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
     * Fill the current array with primitive values (of the type the array holds) from a stream,
     * starting at array index zero, up to the capacity of the array, without resizing. Data are
     * read until the array is filled or the stream runs out. If the stream does not contain a whole
     * number of items (possible if the item size is not one byte), the behaviour in respect of the
     * final partial item and stream position is not defined.
     *
     * @param is InputStream to source the data from
     * @return number of primitives successfully read
     * @throws IOException reflecting I/O errors during reading
     */
    public int fillFromStream(InputStream is) throws IOException {
        return fromStream(is, 0, delegate.getSize());
    }

    /**
     * Read primitive values from a stream into a slice of the array, defined by a start and a
     * count. Data are read until the array slice is filled or the stream runs out. Data in the
     * array beyond the slice.
     * <p>
     * This method is behind the manipulation of bytes into the several primitive element types on
     * behalf of {@link #fillFromStream(InputStream)} etc.. The storage is resized if the slice
     * being written ends beyond the current end of the array, i.e. it is increased to the value of
     * {@code limit}. The return value should be checked against the count of items requested. If
     * the stream runs out before the request is satisfied, the return will be less than the count,
     * and items beyond the last whole item read are not altered.
     *
     * @param dis data stream source for the values
     * @param start first element index to read
     * @param count number of primitive elements to read
     * @return number of primitives successfully read ({@code =count}, if not ended by EOF)
     * @throws IOException reflecting I/O errors during reading
     */
    private int fromStream(InputStream is, int start, int count) throws IOException {
        // Ensure the array is dimensioned to fit the data expected
        int size = delegate.getSize(), limit = start + count;
        if (limit > size) {
            resizeCheck();
            delegate.setSize(limit);
        }
        // We need a wrapper capable of decoding the data from the representation defined by Java.
        DataInputStream dis = new DataInputStream(is);
        // itemType.fromStream returns *index* of first element *not* written
        return itemType.fromStream(dis, data, start, limit) - start;
    }

    /**
     * Append items from the object, which is a byte string of some kind ({@code PyString} or object
     * with the buffer interface providing bytes). The string of bytes is interpreted as an array of
     * machine values (as if it had been read from a file using the {@link #fromfile(PyObject, int)
     * fromfile()} method).
     *
     * @param input string of bytes containing array data
     */
    public void fromstring(PyObject input) {
        array_fromstring(input);
    }

    /**
     * Append items from the string, interpreting the string as an array of bytes (as if it had been
     * read from a file using the {@link #fromfile(PyObject, int) fromfile()} method). The bytes
     * encode primitive values of the type appropriate to the array,
     *
     * @param input string of bytes containing array data
     */
    public void fromstring(String input) {
        fromBytes(delegate.getSize(), StringUtil.toBytes(input));
    }

    /**
     * Read primitive values from a stream into a slice of the array, defined by a start and the
     * number of items encoded in the bytes. Data are read until the array slice is filled or the
     * stream runs out. Data in the array beyond the slice are not altered. Write a slice of the
     * array with primitive values
     *
     * items from the string, interpreting the string as an array of bytes (as if it had been read
     * from a file using the {@link #fromfile(PyObject, int) fromfile()} method). The bytes encode
     * primitive values of the type appropriate to the array,
     *
     * @param start first element index to read into
     * @param input string of bytes containing array data
     * @return number of primitives successfully read ({@code =count}, if not ended by EOF)
     */
    public void fromstring(int start, String input) {
        fromBytes(start, StringUtil.toBytes(input));
    }

    /**
     * Append items from a bytes-like object. The bytes encode primitive values of the type
     * appropriate to the array,
     *
     * @param input string of bytes containing array data
     */
    @ExposedMethod
    final void array_fromstring(PyObject input) {

        // This is an append, so start at the current end.
        int start = delegate.getSize();

        if (input instanceof BufferProtocol) {

            if (input instanceof PyUnicode) {
                // Unicode is treated as specifying a byte string via the default encoding.
                String s = ((PyUnicode) input).encode();
                fromBytes(start, StringUtil.toBytes(s));

            } else {
                // Access the bytes through the abstract API of the BufferProtocol
                try (PyBuffer pybuf = ((BufferProtocol) input).getBuffer(PyBUF.STRIDED_RO)) {
                    if (pybuf.getNdim() == 1) {
                        if (pybuf.getStrides()[0] == 1) {
                            // Data are contiguous in the buffer
                            fromBytes(start, pybuf.getNIOByteBuffer());
                        } else {
                            // As frombytesInternal only knows contiguous bytes, make a copy.
                            byte[] copy = new byte[pybuf.getLen()];
                            pybuf.copyTo(copy, 0);
                            fromBytes(start, ByteBuffer.wrap(copy));
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
     * Copy into this array, starting at the given item index and expanding if necessary, a sequence
     * of primitive values decoded from the contents of a byte array. The number of items copied is
     * determined by the size of the byte array.
     *
     *
     * Common code supporting Java and Python versions of <code>.fromstring()</code> or
     * <code>.frombytes()</code> (Python 3.2+ name).
     *
     * @param start item-index of first item to read from byte buffer
     * @param bytes array encoding the primitive values
     * @return number of primitives successfully read
     */
    private final int fromBytes(int start, byte[] bytes) {
        return fromBytes(start, ByteBuffer.wrap(bytes));
    }

    /**
     * Copy into this array, starting at the given item index and expanding if necessary, a sequence
     * of primitive values decoded from the remaining bytes of a {@code ByteBuffer} (from the
     * current position to the limit of the source buffer). The number of items copied is determined
     * by the size of the data.
     * <p>
     * This is common code supporting Java and Python versions of <code>.fromstring()</code> or
     * <code>.frombytes()</code> (Python 3.2+ name).
     *
     * @param start item-index of first item to read from byte buffer
     * @param bytes buffer encoding the primitive values
     * @return number of primitives successfully read
     */
    private final int fromBytes(int start, ByteBuffer bytes) {
        // Check validity wrt array itemsize
        int byteCount = bytes.remaining();
        int count = byteCount / itemType.itemsize;
        if (byteCount > count * itemType.itemsize) {
            throw Py.ValueError("data length not a multiple of item size");
        }

        try {
            // Provide argument as stream of bytes for fromstream method
            InputStream is = new ByteBufferBackedInputStream(bytes);
            return fromStream(is, start, count);
        } catch (IOException ioe) {
            // Not really possible since we just wrapped a byte buffer
            return 0;
        }
    }

    public void fromunicode(PyUnicode input) {
        array_fromunicode(input);
    }

    @ExposedMethod
    final void array_fromunicode(PyObject input) {
        if (!(input instanceof PyUnicode)) {
            throw Py.ValueError("fromunicode argument must be an unicode object");
        } else if (itemType != ItemType.UNICHAR) {
            throw Py.ValueError("fromunicode() may only be called on type 'u' arrays");
        } else {
            extend(input);
        }
    }

    /**
     * Get the element at position {@code i} from the array
     *
     * @param i index of the item to be retrieved from the array
     */
    @Override
    protected PyObject pyget(int i) {
        return itemType.get(this, i);
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
     * the case of streams of primitive values, this is the number of bytes written to, or read from
     * a stream. The amount of memory occupied by each item is an internal matter for Java.
     * <p>
     * This method is used by other methods to define read/write quanta from strings and streams.
     *
     * @return number of bytes used to store array type, relevant when serialising to an array of
     *         bytes, or the reverse.
     */
    @ExposedGet(name = "itemsize")
    public int getItemsize() {
        return itemType.itemsize;
    }

    /**
     * Getter for the storage size of the array's type, relevant when serialising to an array of
     * bytes, or the reverse.
     *
     * @return actual storage size
     * @deprecated Use {@link #getItemsize()} instead which (since 2.7.3) gives the same result.
     */
    @Deprecated
    public int getStorageSize() {
        return itemType.itemsize;
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
        // We have to specify both the type and the class (for use when OBJECT)
        PyArray ret = new PyArray(TYPE, itemType, itemClass, n);
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
     * Type as it would appear in an error message. Simple class name for {@code OBJECT}.
     *
     * @return single character type code or simple class name
     */
    private CharSequence description() {
        if (itemType == ItemType.OBJECT) {
            return itemClass.getName();
        } else {
            return itemType.description();
        }
    }

    /**
     * Return either a Python-style {@code array.array} type code for the element (item) type or the
     * Java class name.
     *
     * @return single character type code or simple class name
     */
    @ExposedGet(name = "typecode")
    public String getTypecode() {
        if (itemType == ItemType.OBJECT) {
            return itemClass.getName();
        } else {
            return itemType.typecode;
        }
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
        for (int i = 0; i < len; i++) {
            if (value.equals(pyget(i))) {
                return i;
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
        delegate.makeInsertSpace(index);
        pyset(index, value);
    }

    /**
     * Removes the item at the index {@code i} from the array and returns it. The optional argument
     * defaults to -1, so that by default the last item is removed and returned.
     */
    @ExposedMethod(defaults = "-1")
    public final PyObject array_pop(int i) {
        return pop(i);
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

        resizeCheck();  // Prohibit when exporting a buffer

        PyObject ret = itemType.get(this, index);
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
            resizeCheck();  // Prohibit when exporting a buffer
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
        ret.setElementType(itemType, itemClass);
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
        try {
            itemType.set(this, i, value);
        } catch (ClassCastException cce) {
            throw Py.TypeError(String.format("array item must be %s", description()));
        }
    }

    /**
     * Set element to integer value, tolerating primitive integer values in arrays of Unicode
     * character, {@code int} or {@code long}. Negative values assigned to unsigned elements adopt
     * their wrapped unsigned values.
     *
     * @param i index to set
     * @param value to set
     */
    public void set(int i, int value) {
        if (itemType == ItemType.UNICHAR || itemClass == Integer.TYPE || itemClass == Long.TYPE) {
            Array.setInt(data, i, value);
        } else {
            throw notCompatibleTypeError();
        }
    }

    /**
     * Set element to integer value given as a Java {@code char}, tolerating primitive integer
     * values in arrays of Unicode character, {@code int} or {@code long}.
     *
     * @param i index to set
     * @param value to set
     */
    public void set(int i, char value) {
        if (itemType == ItemType.CHAR || itemClass == Integer.TYPE || itemClass == Long.TYPE) {
            Array.setChar(data, i, value);
        } else {
            throw notCompatibleTypeError();
        }
    }

    /**
     * Set element in an array of element type 'b','B', or 'c' to a Java {@code byte}.
     *
     * @param i index to set
     * @param value to set
     */
    public void set(int i, byte value) {
        if (itemClass == Byte.TYPE) {
            Array.setByte(data, i, value);
        } else {
            throw notCompatibleTypeError();
        }
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
                PyArray array = arrayChecked(value);

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
                        throw Py.TypeError("Slice typecode " + array.reprTypecode()
                                + " is not compatible with this array (typecode " + reprTypecode()
                                + ")");
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
        for (int i = 0; i < len; i++) {
            list.append(itemType.get(this, i));
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
        itemType.toStream(dos, data, delegate.getSize());
        return dos.size(); // bytes written
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

    @ExposedMethod
    public final PyUnicode array_tounicode() {
        if (itemType != ItemType.UNICHAR) {
            throw Py.ValueError("tounicode() may only be called on type 'u' arrays");
        }
        int len = delegate.getSize();
        int[] codepoints = new int[len];
        for (int i = 0; i < len; i++) {
            codepoints[i] = Array.getInt(data, i);
        }
        return new PyUnicode(codepoints);
    }

    public String tounicode() {
        return array_tounicode().getString();
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

    /**
     * An enumeration of the supported array element (item) types and their properties (type code,
     * representation, range, etc.). One member {@code OBJECT} covers all other types, to support
     * Jython's ability to represent a (homogeneous) array of an arbitrary class.
     */
    static enum ItemType {

        // Put the signed case before an unsigned case of same Java class (fromJavaClass depends)

        BYTE("b", "byte", Byte.TYPE, 1, Byte.MIN_VALUE, Byte.MAX_VALUE) {

            @Override
            void set(PyArray a, int i, PyObject value) {
                Array.setByte(a.data, i, (byte) checkedInteger(value));
            }

            @Override
            PyObject get(PyArray a, int i) {
                return Py.newInteger(Array.getByte(a.data, i));
            }

            @Override
            void toStream(DataOutputStream dos, Object data, int n) throws IOException {
                for (int i = 0; i < n; i++) {
                    dos.writeByte(Array.getByte(data, i));
                }
            }

            @Override
            int fromStream(DataInputStream dis, Object data, int index, int limit)
                    throws IOException {
                try {
                    while (index < limit) {
                        byte val = dis.readByte();
                        Array.setByte(data, index++, val);
                    }
                } catch (EOFException eof) {}
                return index;
            }
        },

        UBYTE("B", "byte", Byte.TYPE, 1, Byte.MAX_VALUE * 2L + 1) {

            @Override
            void set(PyArray a, int i, PyObject value) {
                Array.setByte(a.data, i, (byte) checkedInteger(value));
            }

            @Override
            PyObject get(PyArray a, int i) {
                // Negative values must be masked to positive int
                return Py.newInteger(0xff & Array.getByte(a.data, i));
            }

            @Override
            void toStream(DataOutputStream dos, Object data, int n) throws IOException {
                for (int i = 0; i < n; i++) {
                    dos.writeByte(Array.getByte(data, i));
                }
            }

            @Override
            int fromStream(DataInputStream dis, Object data, int index, int limit)
                    throws IOException {
                try {
                    while (index < limit) {
                        byte val = dis.readByte();
                        Array.setByte(data, index++, val);
                    }
                } catch (EOFException eof) {}
                return index;
            }
        },

        SHORT("h", "short integer", Short.TYPE, 2, Short.MIN_VALUE, Short.MAX_VALUE) {

            @Override
            void set(PyArray a, int i, PyObject value) {
                Array.setShort(a.data, i, (short) checkedInteger(value));
            }

            @Override
            PyObject get(PyArray a, int i) {
                return Py.newInteger(Array.getShort(a.data, i));
            }

            @Override
            void toStream(DataOutputStream dos, Object data, int n) throws IOException {
                for (int i = 0; i < n; i++) {
                    dos.writeShort(Array.getShort(data, i));
                }
            }

            @Override
            int fromStream(DataInputStream dis, Object data, int index, int limit)
                    throws IOException {
                try {
                    while (index < limit) {
                        short val = dis.readShort();
                        Array.setShort(data, index++, val);
                    }
                } catch (EOFException eof) {}
                return index;
            }
        },

        USHORT("H", "short integer", Short.TYPE, 2, Short.MAX_VALUE * 2L + 1) {

            @Override
            void set(PyArray a, int i, PyObject value) {
                Array.setShort(a.data, i, (short) checkedInteger(value));
            }

            @Override
            PyObject get(PyArray a, int i) {
                // Negative values must be masked to positive int
                return Py.newInteger(0xffff & Array.getShort(a.data, i));
            }

            @Override
            void toStream(DataOutputStream dos, Object data, int n) throws IOException {
                for (int i = 0; i < n; i++) {
                    dos.writeShort(Array.getShort(data, i));
                }
            }

            @Override
            int fromStream(DataInputStream dis, Object data, int index, int limit)
                    throws IOException {
                try {
                    while (index < limit) {
                        short val = dis.readShort();
                        Array.setShort(data, index++, val);
                    }
                } catch (EOFException eof) {}
                return index;
            }
        },

        INT("i", "int", Integer.TYPE, 4, Integer.MIN_VALUE, Integer.MAX_VALUE) {

            @Override
            void set(PyArray a, int i, PyObject value) {
                Array.setInt(a.data, i, (int) checkedInteger(value));
            }

            @Override
            PyObject get(PyArray a, int i) {
                return Py.newInteger(Array.getInt(a.data, i));
            }

            @Override
            void toStream(DataOutputStream dos, Object data, int n) throws IOException {
                for (int i = 0; i < n; i++) {
                    dos.writeInt(Array.getInt(data, i));
                }
            }

            @Override
            int fromStream(DataInputStream dis, Object data, int index, int limit)
                    throws IOException {
                try {
                    while (index < limit) {
                        int val = dis.readInt();
                        Array.setInt(data, index++, val);
                    }
                } catch (EOFException eof) {}
                return index;
            }
        },

        UINT("I", "int", Integer.TYPE, 4, Integer.MAX_VALUE * 2L + 1) {

            @Override
            void set(PyArray a, int i, PyObject value) {
                Array.setInt(a.data, i, (int) checkedInteger(value));
            }

            @Override
            PyObject get(PyArray a, int i) {
                int val = Array.getInt(a.data, i);
                if (val >= 0) {
                    return Py.newInteger(val);
                } else {
                    // Negative values must be interpreted as positive
                    return new PyLong(0xffffffffL & val);
                }
            }

            @Override
            void toStream(DataOutputStream dos, Object data, int n) throws IOException {
                for (int i = 0; i < n; i++) {
                    dos.writeInt(Array.getInt(data, i));
                }
            }

            @Override
            int fromStream(DataInputStream dis, Object data, int index, int limit)
                    throws IOException {
                try {
                    while (index < limit) {
                        int val = dis.readInt();
                        Array.setInt(data, index++, val);
                    }
                } catch (EOFException eof) {}
                return index;
            }
        },

        LONG("l", "long", Long.TYPE, 8, Long.MIN_VALUE, Long.MAX_VALUE) {

            @Override
            void set(PyArray a, int i, PyObject value) {
                Array.setLong(a.data, i, checkedSignedLong(value));
            }

            @Override
            PyObject get(PyArray a, int i) {
                return new PyLong(Array.getLong(a.data, i));
            }

            @Override
            void toStream(DataOutputStream dos, Object data, int n) throws IOException {
                for (int i = 0; i < n; i++) {
                    dos.writeLong(Array.getLong(data, i));
                }
            }

            @Override
            int fromStream(DataInputStream dis, Object data, int index, int limit)
                    throws IOException {
                try {
                    while (index < limit) {
                        long val = dis.readLong();
                        Array.setLong(data, index++, val);
                    }
                } catch (EOFException eof) {}
                return index;
            }
        },

        ULONG("L", "long", Long.TYPE, 8, Long.MAX_VALUE * 2L + 1) {

            @Override
            void set(PyArray a, int i, PyObject value) {
                Array.setLong(a.data, i, checkedUnsignedLong(value));
            }

            @Override
            PyObject get(PyArray a, int i) {
                long val = Array.getLong(a.data, i);
                if (val >= 0) {
                    return new PyLong(val);
                } else {
                    // Negative values must be interpreted as positive
                    return new PyLong(BigInteger.valueOf(val).add(TWO_TO_64));
                }
            }

            @Override
            void toStream(DataOutputStream dos, Object data, int n) throws IOException {
                for (int i = 0; i < n; i++) {
                    dos.writeLong(Array.getLong(data, i));
                }
            }

            @Override
            int fromStream(DataInputStream dis, Object data, int index, int limit)
                    throws IOException {
                try {
                    while (index < limit) {
                        long val = dis.readLong();
                        Array.setLong(data, index++, val);
                    }
                } catch (EOFException eof) {}
                return index;
            }
        },

        FLOAT("f", "float", Float.TYPE, 4) {

            @Override
            void set(PyArray a, int i, PyObject value) {
                Array.setFloat(a.data, i, (float) value.asDouble());
            }

            @Override
            PyObject get(PyArray a, int i) {
                return new PyFloat(Array.getFloat(a.data, i));
            }

            @Override
            void toStream(DataOutputStream dos, Object data, int n) throws IOException {
                for (int i = 0; i < n; i++) {
                    dos.writeFloat(Array.getFloat(data, i));
                }
            }

            @Override
            int fromStream(DataInputStream dis, Object data, int index, int limit)
                    throws IOException {
                try {
                    while (index < limit) {
                        float val = dis.readFloat();
                        Array.setFloat(data, index++, val);
                    }
                } catch (EOFException eof) {}
                return index;
            }
        },

        DOUBLE("d", "double", Double.TYPE, 8) {

            @Override
            void set(PyArray a, int i, PyObject value) {
                Array.setDouble(a.data, i, value.asDouble());
            }

            @Override
            PyObject get(PyArray a, int i) {
                return new PyFloat(Array.getDouble(a.data, i));
            }

            @Override
            void toStream(DataOutputStream dos, Object data, int n) throws IOException {
                for (int i = 0; i < n; i++) {
                    dos.writeDouble(Array.getDouble(data, i));
                }
            }

            @Override
            int fromStream(DataInputStream dis, Object data, int index, int limit)
                    throws IOException {
                try {
                    while (index < limit) {
                        double val = dis.readDouble();
                        Array.setDouble(data, index++, val);
                    }
                } catch (EOFException eof) {}
                return index;
            }
        },

        CHAR("c", "char", Byte.TYPE, 1, Byte.MAX_VALUE * 2L + 1) {
            // Has to be after BYTE so fromJavaClass returns that for Byte.TYPE

            @Override
            void set(PyArray a, int i, PyObject value) {
                String s = ((PyString) value).getString();
                if (s.length() != 1) {
                    throw new ClassCastException();
                }
                Array.setByte(a.data, i, (byte) s.charAt(0));
            }

            @Override
            PyObject get(PyArray a, int i) {
                return Py.makeCharacter((char) (0xff & Array.getByte(a.data, i)));
            }

            @Override
            void toStream(DataOutputStream dos, Object data, int n) throws IOException {
                for (int i = 0; i < n; i++) {
                    dos.writeByte(Array.getByte(data, i));
                }
            }

            @Override
            int fromStream(DataInputStream dis, Object data, int index, int limit)
                    throws IOException {
                try {
                    while (index < limit) {
                        byte val = dis.readByte();
                        Array.setByte(data, index++, val);
                    }
                } catch (EOFException eof) {}
                return index;
            }
        },

        UNICHAR("u", "unicode character", Integer.TYPE, 4, Character.MAX_CODE_POINT) {
            // Has to be after INT so fromJavaClass returns that for Integer.TYPE

            @Override
            void set(PyArray a, int i, PyObject value) {
                String s = ((PyUnicode) value).getString();
                if (s.codePointCount(0, Math.min(s.length(), 4)) != 1) {
                    throw new ClassCastException();
                }
                Array.setInt(a.data, i, s.codePointAt(0));
            }

            @Override
            PyObject get(PyArray a, int i) {
                return new PyUnicode(Array.getInt(a.data, i));
            }

            @Override
            void toStream(DataOutputStream dos, Object data, int n) throws IOException {
                for (int i = 0; i < n; i++) {
                    dos.writeInt(Array.getInt(data, i));
                }
            }

            @Override
            int fromStream(DataInputStream dis, Object data, int index, int limit)
                    throws IOException {
                try {
                    while (index < limit) {
                        int val = dis.readInt();
                        Array.setInt(data, index++, val);
                    }
                } catch (EOFException eof) {}
                return index;
            }
        },

        BOOLEAN("z", "boolean", Boolean.TYPE, 1, 0) {

            @Override
            void set(PyArray a, int i, PyObject value) {
                Array.setBoolean(a.data, i, value.__nonzero__());
            }

            @Override
            PyObject get(PyArray a, int i) {
                return Py.newBoolean(Array.getBoolean(a.data, i));
            }

            @Override
            void toStream(DataOutputStream dos, Object data, int n) throws IOException {
                for (int i = 0; i < n; i++) {
                    dos.writeBoolean(Array.getBoolean(data, i));
                }
            }

            @Override
            int fromStream(DataInputStream dis, Object data, int index, int limit)
                    throws IOException {
                try {
                    while (index < limit) {
                        boolean val = dis.readBoolean();
                        Array.setBoolean(data, index++, val);
                    }
                } catch (EOFException eof) {}
                return index;
            }
        },

        OBJECT() {

            @Override
            void set(PyArray a, int i, PyObject value) {
                Object val = value.__tojava__(a.itemClass);
                if (val == Py.NoConversion) {
                    throw a.notCompatibleTypeError();
                }
                Array.set(a.data, i, val);
            }

            @Override
            PyObject get(PyArray a, int i) {
                return Py.java2py(Array.get(a.data, i));
            }

            @Override
            void toStream(DataOutputStream dos, Object data, int n) throws IOException {
                // Not supported: silently ignored (in versions so far)
            }

            @Override
            int fromStream(DataInputStream dis, Object data, int index, int limit)
                    throws IOException {
                // Not supported: silently ignored (in versions so far)
                return index;
            }
        };

        /**
         * Implementation of setting an element in the data array, specific to this
         * {@code ItemType}.
         *
         * @param a the PyArray on which to operate
         * @param i index of element
         * @param value to set
         */
        abstract void set(PyArray a, int i, PyObject value);

        /**
         * Implementation of getting an element from the data array, specific to this
         * {@code ItemType}.
         *
         * @param a the PyArray on which to operate
         * @param i index of element
         * @return value got
         */
        abstract PyObject get(PyArray a, int i);

        /**
         * Generic stream writer to write the entire contents of the array to the stream as
         * primitive types. After the call returns, {@code dos.size()} indicates the number of bytes
         * successfully written.
         *
         * @param dos to sink the array data to
         * @param data array to write from
         * @param n number of items to write
         * @throws IOException reflecting I/O errors during writing
         */
        abstract void toStream(DataOutputStream dos, Object data, int n) throws IOException;

        /**
         * Implementation of reading primitive values from a stream into a slice of the array. Data
         * are read until the array slice is filled or the stream runs out. Return the index of the
         * first item not written: if this is less than the limit, it is because the read ended
         * early on an end-of-file.
         *
         * Each item type provides its own manipulation of bytes into the several primitive element
         * types on behalf of {@link #fillFromStream(InputStream)} etc..
         *
         * If the stream does not contain a whole number of items (possible if the item size is not
         * one byte), the behaviour in respect of the final partial item and stream position is not
         * defined.
         *
         * @param dis data stream source for the values
         * @param data destination array
         * @param index of first array element to read
         * @param limit first element <b>not</b> to read
         * @return index of first element not read ({@code =limit}, if not ended by EOF)
         * @throws IOException reflecting I/O errors during reading
         */
        abstract int fromStream(DataInputStream dis, Object data, int index, int limit)
                throws IOException;

        /** Type name as it would appear in an {@code array.array} constructor. */
        String typecode;
        /** Type as it would appear in an error message. "" for OBJECT. */
        final String description;
        /** Class representing elements of this type (for primitive cases). "" for OBJECT. */
        Class<?> itemClass;
        /** Number of bytes in serialised form (not necessarily in memory). */
        int itemsize;
        /** Minimum value (only valid in integral and character types). */
        long min;
        /** Maximum value (only valid in integral and character types). */
        long max;

        /**
         * Do-it-all constructor.
         *
         * @param typecode type name (as seen in {@code array.array} constructor)
         * @param itemClass class of the elements of the array (when primitive)
         * @param itemsize size of items in the array for serialisation
         * @param min minimum value (for integer item types)
         * @param max maximum value (for integer item types)
         */
        ItemType(String typecode, String description, Class<?> itemClass, int itemsize, long min,
                long max) {
            this.typecode = typecode;
            this.description = description;
            this.itemClass = itemClass;
            this.itemsize = itemsize;
            this.min = min;
            this.max = max;
        }

        /** Signed types with values representable by a Java {@code double}. */
        ItemType(String typecode, String description, Class<?> itemClass, int itemsize) {
            this(typecode, description, itemClass, itemsize, 0, 0);
        }

        /** Unsigned types with values representable by a Java {@code long}. */
        ItemType(String typecode, String description, Class<?> itemClass, int itemsize, long max) {
            this(typecode, description, itemClass, itemsize, 0L, max);
        }

        /** Object element types (represented by the singular {@code ItemType OBJECT}. */
        ItemType() {
            this("", "Java Object", Object.class, 0, 0, 0);
        }

        /**
         * Converts a character code for the array type to the corresponding {@code ItemType} for
         * the elements of the implementation array. If the code is not one of the valid options,
         * {@code ValueError} is raised.
         *
         * @param typecode character code for the array item type
         * @return {@code ItemType} of the array elements
         */
        static ItemType fromTypecode(char typecode) {
            switch (typecode) {
                case 'z':
                    return BOOLEAN;
                case 'b':
                    return BYTE;
                case 'B':
                    return UBYTE;
                case 'u':
                    return UNICHAR;
                case 'c':
                    return CHAR;
                case 'h':
                    return SHORT;
                case 'H':
                    return USHORT;
                case 'i':
                    return INT;
                case 'I':
                    return UINT;
                case 'l':
                    return LONG;
                case 'L':
                    return ULONG;
                case 'f':
                    return FLOAT;
                case 'd':
                    return DOUBLE;
                default:
                    throw Py.ValueError("typecode must be " + reminder());
            }
        }

        /**
         * Map a Java class to the {@code ItemType} type code that represents it. Where that may be
         * ambiguous, the method assumes signed representation (so for example {@code Integer.TYPE}
         * maps to {@code 'i'} not {@code 'I'}). Classes other than those map to their Java class
         * name. This supports the extended repertoire {@code array.array} has in Jython.
         *
         * @param cls element class
         * @return the {@code array.array} type code that representing {@code cls}
         */
        static ItemType fromJavaClass(Class<?> cls) {
            for (ItemType i : ItemType.values()) {
                if (cls.equals(i.itemClass)) {
                    return i;
                }
            }
            return OBJECT;
        }

        protected static final BigInteger TWO_TO_64 = BigInteger.ONE.shiftLeft(64);

        /**
         * Convert an integer value to a Java long and test it against the bounds {@link #min} and
         * {@link #max}.
         * <p>
         * An {@code OverflowError} is raised if these bounds are exceeded and a
         * {@code ClassCastException} if the type is not integral (cannot be converted to a
         * {@code long}).
         *
         * @param value to convert
         * @return {@code value} as a {@code long}
         * @throws PyException {@code OverflowError} if out of range for item type
         * @throws ClassCastException if not integral (cannot be converted to a {@code long}).
         */
        protected long checkedInteger(PyObject value) {
            long val;
            if (min == 0) {
                val = checkedUnsignedLong(value);
            } else {
                val = checkedSignedLong(value);
                if (val < min) {
                    throw lessThanMinimum();
                }
            }
            if (val > max) {
                throw moreThanMaximum();
            } else {
                return val;
            }
        }

        /**
         * Check the range of an integer value is correct for a Java {@code long} and convert it to
         * that type. An {@code OverflowError} is raised if these bounds are exceeded.
         *
         * @param value to convert
         * @return {@code value} as a {@code long}
         * @throws PyException {@code OverflowError} if out of range for signed long
         * @throws ClassCastException if not integral (cannot be converted to a {@code long}).
         */
        protected long checkedSignedLong(PyObject value) throws PyException, ClassCastException {
            if (value instanceof PyInteger) {
                return ((PyInteger) value).getValue();
            } else if (value instanceof PyLong) {
                BigInteger val = ((PyLong) value).getValue();
                if (PyLong.MAX_LONG.compareTo(val) < 0) {
                    throw moreThanMaximum();
                } else if (PyLong.MIN_LONG.compareTo(val) > 0) {
                    throw lessThanMinimum();
                } else {
                    return val.longValue();
                }
            } else {
                Long val = (Long) value.__tojava__(Long.TYPE);
                if (val == null) {
                    throw new ClassCastException();
                }
                return val.longValue();
            }
        }

        /**
         * Check the range of an integer value is correct for an unsigned 64-bit integer, and
         * convert it to the Java {@code long} with the same bit pattern. (This is what represents
         * an {@code unsigned long} in the array.) An {@code OverflowError} is raised if the
         * allowable range is exceeded.
         *
         * @param value to convert
         * @return {@code value} as a {@code long}
         * @throws PyException {@code OverflowError} if out of range for unsigned long
         * @throws ClassCastException if not integral (cannot be converted to a {@code long}).
         */
        protected long checkedUnsignedLong(PyObject value) throws ClassCastException {
            if (value instanceof PyInteger) {
                int val = ((PyInteger) value).getValue();
                if (val < 0) {
                    throw lessThanMinimum();
                } else {
                    return val;
                }
            } else {
                BigInteger val;
                if (value instanceof PyLong) {
                    val = ((PyLong) value).getValue();
                } else {
                    val = (BigInteger) value.__tojava__(BigInteger.class);
                    if (val == null) {
                        throw new ClassCastException();
                    }
                }
                if (BigInteger.ZERO.compareTo(val) > 0) {
                    throw lessThanMinimum();
                } else if (PyLong.MAX_ULONG.compareTo(val) < 0) {
                    throw moreThanMaximum();
                } else if (PyLong.MAX_LONG.compareTo(val) < 0) {
                    // In range 2^63 <= value < 2^64: represent as negative long.
                    return val.subtract(TWO_TO_64).longValue();
                } else {
                    return val.longValue();
                }
            }
        }

        /**
         * Provide a description of the element type, typically for use in an error message.
         *
         * @return a description of the element type
         */
        CharSequence description() {
            StringBuilder buf = new StringBuilder(description.length() + 10);

            switch (this) {
                case BOOLEAN:
                case CHAR:
                case DOUBLE:
                case FLOAT:
                case OBJECT:
                case UNICHAR:
                    break;
                default:
                    buf.append(min < 0 ? "signed " : "unsigned ");
            }
            buf.append(description);
            return buf;
        }

        /** @return a reminder of the valid item types. */
        private static CharSequence reminder() {
            StringBuilder buf = new StringBuilder(100);
            for (ItemType i : ItemType.values()) {
                if (i != OBJECT) {
                    if (buf.length() != 0) {
                        buf.append(", ");
                    }
                    buf.append(i.typecode);
                }
            }
            buf.append(" or a Java class");
            return buf;
        }

        /**
         * Create throwable {@code OverflowError} along the lines "TYPE-array value is less than
         * minimum", where TYPE is the element type of the array.
         *
         * @return the {@code OverflowError}
         */
        protected PyException lessThanMinimum() {
            return Py.OverflowError(
                    String.format("%s array value is less than minimum", description()));
        }

        /**
         * Create throwable {@code OverflowError} along the lines "TYPE-array value is more than
         * maximum", where TYPE is the element type of the array.
         *
         * @return the {@code OverflowError}
         */
        protected PyException moreThanMaximum() {
            return Py.OverflowError(
                    String.format("%s array value is more than maximum", description()));
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
                // No existing export we can re-use: create a new one (acts as unsigned)
                if (itemClass == Byte.TYPE) {
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

    /**
     * Create throwable {@code TypeError} along the lines "Type not compatible with array of
     * TYPECODE", where TYPECODE is the element type of the array.
     *
     * @return the {@code TypeError}
     */
    PyException notCompatibleTypeError() {
        return Py.TypeError(String.format("Type not compatible with array of '%s'", description()));
    }
}
