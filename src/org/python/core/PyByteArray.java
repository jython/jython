package org.python.core;

import java.util.Arrays;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * Partial implementation of Python bytearray. At the present stage of development, the class
 * provides:
 * <ul>
 * <li>constructors (both __init__ and the Java API constructors),</li>
 * <li>the slice operations (get, set and delete)</li>
 * <li>a <code>List&lt;PyInteger&gt;</code> implementation for the Java API</li>
 * </ul>
 * and this is founded on a particular approach to storage management internally. However, the
 * implementation does not support the <code>memoryview</code> interface either for access or a a
 * source for its constructors although the signatures are present. The rich set of string-like
 * operations due a <code>bytearray</code> is not implemented.
 *
 */
@ExposedType(name = "bytearray", base = PyObject.class, doc = BuiltinDocs.bytearray_doc)
public class PyByteArray extends BaseBytes {

    public static final PyType TYPE = PyType.fromClass(PyByteArray.class);

    /**
     * Create a zero-length Python bytearray of explicitly-specified sub-type
     *
     * @param type explicit Jython type
     */
    public PyByteArray(PyType type) {
        super(type);
    }

    /**
     * Create a zero-length Python bytearray.
     */
    public PyByteArray() {
        super(TYPE);
    }

    /**
     * Create zero-filled Python bytearray of specified size.
     *
     * @param size of bytearray
     */
    public PyByteArray(int size) {
        super(TYPE);
        init(size);
    }

//    /**
//     * Create zero-filled Python bytearray of specified size and capacity for appending to. The
//     * capacity is the (minimum) storage allocated, while the size is the number of zero-filled
//     * bytes it currently contains. Simple append and extend operations on a bytearray will not
//     * shrink the allocated capacity, but insertions and deletions may cause it to be reallocated at
//     * the size then in use.
//     *
//     * @param size of bytearray
//     * @param capacity allocated
//     */
//    public PyByteArray(int size, int capacity) {
//        super(TYPE);
//        setStorage(new byte[capacity], size);
//    }
//
    /**
     * Construct bytearray by copying values from int[].
     *
     * @param value source of the bytes (and size)
     */
    public PyByteArray(int[] value) {
        super(TYPE, value);
    }

    /**
     * Create a new array filled exactly by a copy of the contents of the source, which is a
     * bytearray (or bytes).
     *
     * @param value source of the bytes (and size)
     */
    public PyByteArray(BaseBytes value) {
        super(TYPE);
        init(value);
    }

    /**
     * Create a new array filled exactly by a copy of the contents of the source, which is a
     * byte-oriented view.
     *
     * @param value source of the bytes (and size)
     */
    PyByteArray(View value) {
        super(TYPE);
        init(value);
    }

    /**
     * Create a new array filled exactly by a copy of the contents of the source, which is a
     * memoryview.
     *
     * @param value source of the bytes (and size)
     */
    public PyByteArray(MemoryViewProtocol value) {
        super(TYPE);
        init(value.getMemoryView());
    }

    /**
     * Create a new array filled from an iterable of PyObject. The iterable must yield objects
     * convertible to Python bytes (non-negative integers less than 256 or strings of length 1).
     *
     * @param value source of the bytes (and size)
     */
    public PyByteArray(Iterable<? extends PyObject> value) {
        super(TYPE);
        init(value);
    }

    /**
     * Create a new array by encoding a PyString argument to bytes. If the PyString is actually a
     * PyUnicode, the encoding must be explicitly specified.
     *
     * @param arg primary argument from which value is taken
     * @param encoding name of optional encoding (must be a string type)
     * @param errors name of optional errors policy (must be a string type)
     */
    public PyByteArray(PyString arg, PyObject encoding, PyObject errors) {
        super(TYPE);
        init(arg, encoding, errors);
    }

    /**
     * Create a new array by encoding a PyString argument to bytes. If the PyString is actually a
     * PyUnicode, the encoding must be explicitly specified.
     *
     * @param arg primary argument from which value is taken
     * @param encoding name of optional encoding (may be null to select the default for this
     *            installation)
     * @param errors name of optional errors policy
     */
    public PyByteArray(PyString arg, String encoding, String errors) {
        super(TYPE);
        init(arg, encoding, errors);
    }

    /**
     * Create a new array by encoding a PyString argument to bytes. If the PyString is actually a
     * PyUnicode, an exception is thrown saying that the encoding must be explicitly specified.
     *
     * @param arg primary argument from which value is taken
     */
    public PyByteArray(PyString arg) {
        super(TYPE);
        init(arg, (String)null, (String)null);
    }

    /**
     * Construct bytearray by re-using an array of byte as storage initialised by the client.
     *
     * @param newStorage pre-initialised storage: the caller should not keep a reference
     */
    PyByteArray(byte[] newStorage) {
        super(TYPE);
        setStorage(newStorage);
    }

    /**
     * Create a new bytearray object from an arbitrary Python object according to the same rules as
     * apply in Python to the bytearray() constructor:
     * <ul>
     * <li>bytearray() Construct a zero-length bytearray (arg is null).</li>
     * <li>bytearray(int) Construct a zero-initialized bytearray of the given length.</li>
     * <li>bytearray(iterable_of_ints) Construct from iterable yielding integers in [0..255]</li>
     * <li>bytearray(string [, encoding [, errors] ]) Construct from a text string, optionally using
     * the specified encoding.</li>
     * <li>bytearray(unicode, encoding [, errors]) Construct from a unicode string using the
     * specified encoding.</li>
     * <li>bytearray(bytes_or_bytearray) Construct as a mutable copy of bytes or existing bytearray
     * object.</li>
     * </ul>
     * When it is necessary to specify an encoding, as in the Python signature
     * <code>bytearray(string, encoding[, errors])</code>, use the constructor
     * {@link #PyByteArray(PyString, String, String)}. If the PyString is actually a PyUnicode, an
     * encoding must be specified, and using this constructor will throw an exception about that.
     *
     * @param arg primary argument from which value is taken (may be null)
     * @throws PyException in the same circumstances as bytearray(arg), TypeError for non-iterable,
     *             non-integer argument type, and ValueError if iterables do not yield byte [0..255]
     *             values.
     */
    public PyByteArray(PyObject arg) throws PyException {
        super(TYPE);
        init(arg);
    }

    /* ============================================================================================
     * API for org.python.core.PySequence
     * ============================================================================================
     */

    /**
     * Returns a slice of elements from this sequence as a PyByteArray.
     *
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @return a PyByteArray corresponding the the given range of elements.
     */
    @Override
    protected synchronized PyByteArray getslice(int start, int stop, int step) {
        if (step == 1) {
            // Efficiently copy contiguous slice
            return this.getslice(start, stop);
        } else {
            int n = sliceLength(start, stop, step);
            PyByteArray ret = new PyByteArray(n);
            n += ret.offset;
            byte[] dst = ret.storage;
            for (int io = start + offset, jo = ret.offset; jo < n; io += step, jo++) {
                dst[jo] = storage[io];
            }
            return ret;
        }
    }

    /**
     * Specialisation of {@link #getslice(int, int, int)} to contiguous slices (of step size 1) for
     * brevity and efficiency.
     */
    @Override
    protected synchronized PyByteArray getslice(int start, int stop) {
        // Efficiently copy contiguous slice
        int n = stop - start;
        if (n <= 0) {
            return new PyByteArray();
        } else {
            PyByteArray ret = new PyByteArray(n);
            System.arraycopy(storage, offset + start, ret.storage, ret.offset, n);
            return ret;
        }
    }

    /**
     * Returns a PyByteArray that repeats this sequence the given number of times, as in the
     * implementation of <tt>__mul__</tt> for strings.
     *
     * @param count the number of times to repeat this.
     * @return this byte array repeated count times.
     */
    @Override
    protected synchronized PyByteArray repeat(int count) {
        PyByteArray ret = new PyByteArray();
        ret.setStorage(repeatImpl(count));
        return ret;
    }

    /**
     * Sets the indexed element of the bytearray to the given value. This is an extension point
     * called by PySequence in its implementation of {@link #__setitem__} It is guaranteed by
     * PySequence that the index is within the bounds of the array. Any other clients calling
     * <tt>pyset(int)</tt> must make the same guarantee.
     *
     * @param index index of the element to set.
     * @param value the value to set this element to.
     * @throws PyException(AttributeError) if value cannot be converted to an integer
     * @throws PyException(ValueError) if value<0 or value>255
     */
    public synchronized void pyset(int index, PyObject value) throws PyException {
        storage[index + offset] = byteCheck(value);
    }

    /**
     * Insert the element (interpreted as a Python byte value) at the given index.
     * Python int, long and string types of length 1 are allowed.
     *
     * @param index to insert at
     * @param element to insert (by value)
     * @throws PyException(IndexError) if the index is outside the array bounds
     * @throws PyException(ValueError) if element<0 or element>255
     * @throws PyException(TypeError) if the subclass is immutable
     */
    @Override
    public synchronized void pyinsert(int index, PyObject element) {
        // Open a space at the right location.
        storageReplace(index, 0, 1);
        storage[offset+index] = byteCheck(element);
    }

    /**
     * Sets the given range of elements according to Python slice assignment semantics. If the step
     * size is one, it is a simple slice and the operation is equivalent to deleting that slice,
     * then inserting the value at that position, regarding the value as a sequence (if possible) or
     * as a single element if it is not a sequence. If the step size is not one, but start=stop, it
     * is equivalent to insertion at that point. If the step size is not one, and start!=stop, the
     * slice defines a certain number of elements to be replaced, and the value must be a sequence
     * of exactly that many elements (or convertible to such a sequence).
     * <p>
     * When assigning from a sequence type or iterator, the sequence may contain arbitrary
     * <code>PyObject</code>s, but acceptable ones are PyInteger, PyLong or PyString of length 1. If
     * any one of them proves unsuitable for assignment to a Python bytarray element, an exception
     * is thrown and this bytearray is unchanged.
     *
     * <pre>
     * a = bytearray(b'abcdefghijklmnopqrst')
     * a[2:12:2] = iter( [65, 66, 67, long(68), "E"] )
     * </pre>
     *
     * Results in <code>a=bytearray(b'abAdBfChDjElmnopqrst')</code>.
     * <p>
     *
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param value an object consistent with the slice assignment
     */
    @Override
    protected synchronized void setslice(int start, int stop, int step, PyObject value) {

        if (step == 1 && stop < start) {
            // Because "b[5:2] = v" means insert v just before 5 not 2.
            // ... although "b[5:2:-1] = v means b[5]=v[0], b[4]=v[1], b[3]=v[2]
            stop = start;
        }

        /*
         * The actual behaviour depends on the nature (type) of value. It may be any kind of
         * PyObject (but not other kinds of Java Object). The function is implementing assignment to
         * a slice. PEP 3137 declares that the value may be "any type that implements the PEP 3118
         * buffer interface, which isn't implemented yet in Jython.
         */
        // XXX correct this when the buffer interface is available in Jython
        /*
         * The following is essentially equivalent to b[start:stop[:step]]=bytearray(value) except
         * we avoid constructing a copy of value if we can easily do so. The logic is the same as
         * BaseBytes.init(PyObject), without support for value==null.
         */

        if (value instanceof PyString) {
            /*
             * Value is a string (but must be 8-bit).
             */
            setslice(start, stop, step, (PyString)value);

        } else if (value.isIndex()) {
            /*
             * Value behaves as a zero-initialised bytearray of the given length.
             */
            setslice(start, stop, step, value.asIndex(Py.OverflowError));

        } else if (value instanceof BaseBytes) {
            /*
             * Value behaves as a bytearray, and can be can be inserted without making a copy
             * (unless it is this object).
             */
            setslice(start, stop, step, (BaseBytes)value);

        } else if (value instanceof MemoryViewProtocol) {
            /*
             * Value supports Jython implementation of PEP 3118, and can be can be inserted without
             * making a copy.
             */
            setslice(start, stop, step, ((MemoryViewProtocol)value).getMemoryView());

        } else {
            /*
             * The remaining alternative is an iterable returning (hopefully) right-sized ints. If
             * it isn't one, we get an exception about not being iterable, or about the values.
             */
            setslice(start, stop, step, value.asIterable());
        }
    }

    /**
     * Sets the given range of elements according to Python slice assignment semantics from a
     * zero-filled bytearray of the given length.
     *
     * @see #setslice(int, int, int, PyObject)
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param len number of zeros to insert consistent with the slice assignment
     * @throws PyException(SliceSizeError) if the value size is inconsistent with an extended slice
     */
    private void setslice(int start, int stop, int step, int len) throws PyException {
        if (step == 1) {
            // Delete this[start:stop] and open a space of the right size = len
            storageReplace(start, stop - start, len);
            Arrays.fill(storage, start + offset, (start + offset) + len, (byte)0);

        } else {
            // This is an extended slice which means we are replacing elements
            int n = sliceLength(start, stop, step);
            if (n != len) {
                throw SliceSizeError("bytes", len, n);
            }
            for (int io = start + offset; n > 0; io += step, --n) {
                storage[io] = 0;
            }
        }
    }

    /**
     * Sets the given range of elements according to Python slice assignment semantics from a
     * PyString.
     *
     * @see #setslice(int, int, int, PyObject)
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param value a PyString object consistent with the slice assignment
     * @throws PyException(SliceSizeError) if the value size is inconsistent with an extended slice
     */
    private void setslice(int start, int stop, int step, PyString value) throws PyException {
        String v = value.asString();
        int len = v.length();
        if (step == 1) {
            // Delete this[start:stop] and open a space of the right size
            storageReplace(start, stop - start, len);
            setBytes(start, v);
        } else {
            // This is an extended slice which means we are replacing elements
            int n = sliceLength(start, stop, step);
            if (n != len) {
                throw SliceSizeError("bytes", len, n);
            }
            setBytes(start, step, v);
        }
    }

    /**
     * Sets the given range of elements according to Python slice assignment semantics from an
     * object supporting the Jython implementation of PEP 3118.
     *
     * @see #setslice(int, int, int, PyObject)
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param value a memoryview object consistent with the slice assignment
     * @throws PyException(SliceSizeError) if the value size is inconsistent with an extended slice
     */
    private void setslice(int start, int stop, int step, MemoryView value) throws PyException {
        // XXX Support memoryview once means of access to bytes is defined
        throw Py.NotImplementedError("memoryview not yet supported in bytearray");
    }

    /**
     * Sets the given range of elements according to Python slice assignment semantics from a
     * bytearray (or bytes).
     *
     * @see #setslice(int, int, int, PyObject)
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param value a bytearray (or bytes) object consistent with the slice assignment
     * @throws PyException(SliceSizeError) if the value size is inconsistent with an extended slice
     */
    private void setslice(int start, int stop, int step, BaseBytes value) throws PyException {

        if (value == this) {  // Must work with a copy
            value = new PyByteArray(value);
        }

        int len = value.size;

        if (step == 1) {
            // Delete this[start:stop] and open a space of the right size
            storageReplace(start, stop - start, len);
            System.arraycopy(value.storage, value.offset, storage, start + offset, len);

        } else {
            // This is an extended slice which means we are replacing elements
            int n = sliceLength(start, stop, step);
            if (n != len) {
                throw SliceSizeError("bytes", len, n);
            }

            int no = n + value.offset;
            for (int io = start + offset, jo = value.offset; jo < no; io += step, jo++) {
                storage[io] = value.storage[jo];    // Assign this[i] = value[j]
            }

        }
    }

    /**
     * Sets the given range of elements according to Python slice assignment semantics from a
     * bytearray (or bytes).
     *
     * @see #setslice(int, int, int, PyObject)
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param iter iterable source of values to enter in the array
     * @throws PyException(SliceSizeError) if the iterable size is inconsistent with an extended
     *             slice
     */
    private void setslice(int start, int stop, int step, Iterable<? extends PyObject> iter) {
        /*
         * As we don't know how many elements the iterable represents, we can't adjust the array
         * until after we run the iterator. We use this elastic byte structure to hold the bytes
         * until then.
         */
        FragmentList fragList = new BaseBytes.FragmentList();
        fragList.loadFrom(iter);

        if (step == 1) {
            // Delete this[start:stop] and open a space of the right size
            storageReplace(start, stop - start, fragList.totalCount);
            if (fragList.totalCount > 0) {
                // Stitch the fragments together in the space we made
                fragList.emptyInto(storage, start + offset);
            }

        } else {
            // This is an extended slice which means we are replacing elements
            int n = sliceLength(start, stop, step);
            if (n != fragList.totalCount) {
                throw SliceSizeError("bytes", fragList.totalCount, n);
            }
            fragList.emptyInto(storage, start + offset, step);
        }
    }

// Idiom:
// if (step == 1) {
// // Do something efficient with block start...stop-1
// } else {
// int n = sliceLength(start, stop, step);
// for (int i = start, j = 0; j < n; i += step, j++) {
// // Perform jth operation with element i
// }
// }

    /*
     * Deletes an element from the sequence (and closes up the gap).
     *
     * @param index index of the element to delete.
     */
    protected synchronized void del(int index) {
        // XXX Change SequenceIndexDelegate to avoid repeated calls to del(int) for extended slice
        storageDelete(index, 1);
    }

    /*
     * Deletes contiguous sub-sequence (and closes up the gap).
     *
     * @param start the position of the first element.
     *
     * @param stop one more than the position of the last element.
     */
    protected synchronized void delRange(int start, int stop) {
        storageDelete(start, stop - start);
    }

    /**
     * Deletes a simple or extended slice and closes up the gap(s).
     *
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step from one element to the next
     */
    protected synchronized void delslice(int start, int stop, int step) {
        if (step == 1) {
            // Delete this[start:stop] and closing up the space
            storageDelete(start, stop - start);
        } else {
            // This is an extended slice which means we are removing isolated elements
            int n = sliceLength(start, stop, step);

            if (n > 0) {
                if (step > 0) {
                    // The first element is x[start] and the last is x[start+(n-1)*step+1]
                    storageDeleteEx(start, step, n);
                } else {
                    // The first element is x[start+(n-1)*step+1] and the last is x[start]
                    storageDeleteEx(start + (n - 1) * step + 1, -step, n);
                }
            }
        }
    }

    /**
     * Convenience method to build (but not throw) a <code>ValueError</code> PyException with the
     * message "attempt to assign {type} of size {valueSize} to extended slice of size {sliceSize}"
     *
     * @param valueType
     * @param valueSize size of sequence being assigned to slice
     * @param sliceSize size of slice expected to receive
     * @return PyException (ValueError) as detailed
     */
    public static PyException SliceSizeError(String valueType, int valueSize, int sliceSize) {
        String fmt = "attempt to assign %s of size %d to extended slice of size %d";
        return Py.ValueError(String.format(fmt, valueType, valueSize, sliceSize));
        // XXX consider moving to SequenceIndexDelegate.java or somewhere else generic, even Py
    }

    /**
     * Initialise a mutable bytearray object from various arguments. This single initialisation must
     * support:
     * <ul>
     * <li>bytearray() Construct a zero-length bytearray.</li>
     * <li>bytearray(int) Construct a zero-initialized bytearray of the given length.</li>
     * <li>bytearray(iterable_of_ints) Construct from iterable yielding integers in [0..255]</li>
     * <li>bytearray(string [, encoding [, errors] ]) Construct from a text string, optionally using
     * the specified encoding.</li>
     * <li>bytearray(unicode, encoding [, errors]) Construct from a unicode string using the
     * specified encoding.</li>
     * <li>bytearray(bytes_or_bytearray) Construct as a mutable copy of bytes or existing bytearray
     * object.</li>
     * </ul>
     * Unlike CPython we are not able to support the initialisation: <li>bytearray(memory_view)
     * Construct as copy of any object implementing the buffer API.</li> </ul> Although effectively
     * a constructor, it is possible to call __init__ on a 'used' object so the method does not
     * assume any particular prior state.
     *
     * @param args argument array according to Jython conventions
     * @param kwds Keywords according to Jython conventions
     * @throws PyException in the same circumstances as bytearray(arg), TypeError for non-iterable,
     *             non-integer argument type, and ValueError if iterables do not yield byte [0..255]
     *             values.
     */
    @ExposedNew
    @ExposedMethod(doc = BuiltinDocs.bytearray___init___doc)
    final synchronized void bytearray___init__(PyObject[] args, String[] kwds) {

        ArgParser ap = new ArgParser("bytearray", args, kwds, "source", "encoding", "errors");
        PyObject arg = ap.getPyObject(0, null);
        // If not null, encoding and errors must be PyString (or PyUnicode)
        PyObject encoding = ap.getPyObjectByType(1, PyBaseString.TYPE, null);
        PyObject errors = ap.getPyObjectByType(2, PyBaseString.TYPE, null);

        /*
         * This method and the related init()s are modelled on CPython (see
         * Objects/bytearrayobject.c : bytes_init()) but reorganised somewhat to maximise re-use
         * with the implementation of assignment to a slice, which essentially has to construct a
         * bytearray from the right-hand side. Hopefully, it still tries the same things in the same
         * order and fails in the same way.
         */

        if (encoding != null || errors != null) {
            /*
             * bytearray(string [, encoding [, errors]]) Construct from a text string by encoding it
             * using the specified encoding.
             */
            if (arg == null || !(arg instanceof PyString)) {
                throw Py.TypeError("encoding or errors without sequence argument");
            }
            init((PyString)arg, encoding, errors);

        } else {
            // Now construct from arbitrary object (or null)
            init(arg);
        }

    }


    /* ============================================================================================
     * Python API rich comparison operations
     * ============================================================================================
     */

    @Override
    public PyObject __eq__(PyObject other) {
        return basebytes___eq__(other);
    }

    @Override
    public PyObject __ne__(PyObject other) {
        return basebytes___ne__(other);
    }

    @Override
    public PyObject __lt__(PyObject other) {
        return basebytes___lt__(other);
    }

    @Override
    public PyObject __le__(PyObject other) {
        return basebytes___le__(other);
    }

    @Override
    public PyObject __ge__(PyObject other) {
        return basebytes___ge__(other);
    }

    @Override
    public PyObject __gt__(PyObject other) {
        return basebytes___gt__(other);
    }



    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___eq___doc)
    final synchronized PyObject bytearray___eq__(PyObject other) {
        return basebytes___eq__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___ne___doc)
    final synchronized PyObject bytearray___ne__(PyObject other) {
        return basebytes___ne__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___lt___doc)
    final synchronized PyObject bytearray___lt__(PyObject other) {
        return basebytes___lt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___le___doc)
    final synchronized PyObject bytearray___le__(PyObject other) {
        return basebytes___le__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___ge___doc)
    final synchronized PyObject bytearray___ge__(PyObject other) {
        return basebytes___ge__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___gt___doc)
    final synchronized PyObject bytearray___gt__(PyObject other) {
        return basebytes___gt__(other);
    }

/* ============================================================================================
 * Python API for bytearray
 * ============================================================================================
 */

    @Override
    public PyObject __add__(PyObject o) {
        return bytearray___add__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___add___doc)
    final synchronized PyObject bytearray___add__(PyObject o) {
        PyByteArray sum = null;


        // XXX re-write using View


        if (o instanceof BaseBytes) {
            BaseBytes ob = (BaseBytes)o;
            // Quick route: allocate the right size bytearray and copy the two parts in.
            sum = new PyByteArray(size + ob.size);
            System.arraycopy(storage, offset, sum.storage, sum.offset, size);
            System.arraycopy(ob.storage, ob.offset, sum.storage, sum.offset + size, ob.size);

        } else if (o.getType() == PyString.TYPE) {
            // Support bytes type, which in in Python 2.7 is an alias of str. Remove in 3.0
            PyString os = (PyString)o;
            // Allocate the right size bytearray and copy the two parts in.
            sum = new PyByteArray(size + os.__len__());
            System.arraycopy(storage, offset, sum.storage, sum.offset, size);
            sum.setslice(size, sum.size, 1, os);

        } else {
            // Unsuitable type
            // XXX note reversed order relative to __iadd__ may be wrong, matches Python 2.7
            throw ConcatenationTypeError(TYPE, o.getType());
        }

        return sum;
    }

    /**
     * Returns the number of bytes actually allocated.
     */
    public int __alloc__() {
        return bytearray___alloc__();
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray___alloc___doc)
    final int bytearray___alloc__() {
        return storage.length;
    }

    /**
     * Append a single element to the end of the array, equivalent to:
     * <code>s[len(s):len(s)] = o</code>. The argument must be a PyInteger, PyLong or string of
     * length 1.
     *
     * @param o the item to append to the list.
     */
    public void append(PyObject o) {
        bytearray_append(o);
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_append_doc)
    final synchronized void bytearray_append(PyObject o) {
        // Insert at the end, checked for type and range
        pyinsert(size, o);
    }

    /**
     * Implement to the standard Python __contains__ method, which in turn implements the
     * <code>in</code> operator.
     *
     * @param o the element to search for in this bytearray.
     * @return the result of the search.
     **/
    public boolean __contains__(PyObject o) {
        return basebytes___contains__(o);
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray___contains___doc)
    final boolean bytearray___contains__(PyObject o) {
        return basebytes___contains__(o);
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_decode_doc)
    final PyObject bytearray_decode(PyObject[] args, String[] keywords) {
        return basebytes_decode(args, keywords);
    }

    /**
     * Implementation of Python <code>count(sub)</code>. Return the number of non-overlapping
     * occurrences of <code>sub</code> in this byte array.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @return count of occurrences of sub within this byte array
     */
    public int count(PyObject sub) {
        return basebytes_count(sub, null, null);
    }

    /**
     * Implementation of Python <code>count( sub [, start ] )</code>. Return the number of
     * non-overlapping occurrences of <code>sub</code> in the range [start:].
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @return count of occurrences of sub within this byte array
     */
    public int count(PyObject sub, PyObject start) {
        return basebytes_count(sub, start, null);
    }

    /**
     * Implementation of Python <code>count( sub [, start [, end ]] )</code>. Return the number of
     * non-overlapping occurrences of <code>sub</code> in the range [start, end]. Optional arguments
     * <code>start</code> and <code>end</code> (which may be <code>null</code> or
     * <code>Py.None</code> ) are interpreted as in slice notation.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @param end of slice to search
     * @return count of occurrences of sub within this byte array
     */
    public int count(PyObject sub, PyObject start, PyObject end) {
        return basebytes_count(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytearray_count_doc)
    final int bytearray_count(PyObject sub, PyObject start, PyObject end) {
        return basebytes_count(sub, start, end);
    }

    /**
     * Implementation of Python <code>endswith(suffix)</code>.
     *
     * When <code>suffix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this bytearray ends with the <code>suffix</code>.
     * <code>suffix</code> can also be a tuple of suffixes to look for.
     *
     * @param suffix byte array to match, or object viewable as such, or a tuple of them
     * @return true if and only if this bytearray ends with the suffix (or one of them)
     */
    public boolean endswith(PyObject suffix) {
        return basebytes_starts_or_endswith(suffix, null, null, true);
    }

    /**
     * Implementation of Python <code>endswith( suffix [, start ] )</code>.
     *
     * When <code>suffix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this bytearray ends with the <code>suffix</code>.
     * <code>suffix</code> can also be a tuple of suffixes to look for. With optional
     * <code>start</code> (which may be <code>null</code> or <code>Py.None</code>), define the
     * effective bytearray to be the slice <code>[start:]</code> of this bytearray.
     *
     * @param suffix byte array to match, or object viewable as such, or a tuple of them
     * @param start of slice in this bytearray to match
     * @return true if and only if this[start:] ends with the suffix (or one of them)
     */
    public boolean endswith(PyObject suffix, PyObject start) {
        return basebytes_starts_or_endswith(suffix, start, null, true);
    }

    /**
     * Implementation of Python <code>endswith( suffix [, start [, end ]] )</code>.
     *
     * When <code>suffix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this bytearray ends with the <code>suffix</code>.
     * <code>suffix</code> can also be a tuple of suffixes to look for. With optional
     * <code>start</code> and <code>end</code> (which may be <code>null</code> or
     * <code>Py.None</code>), define the effective bytearray to be the slice
     * <code>[start:end]</code> of this bytearray.
     *
     * @param suffix byte array to match, or object viewable as such, or a tuple of them
     * @param start of slice in this bytearray to match
     * @param end of slice in this bytearray to match
     * @return true if and only if this[start:end] ends with the suffix (or one of them)
     */
    public boolean endswith(PyObject suffix, PyObject start, PyObject end) {
        return basebytes_starts_or_endswith(suffix, start, end, true);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytearray_endswith_doc)
    final boolean bytearray_endswith(PyObject suffix, PyObject start, PyObject end) {
        return basebytes_starts_or_endswith(suffix, start, end, true);
    }

    /**
     * Append the elements in the argument sequence to the end of the array, equivalent to:
     * <code>s[len(s):len(s)] = o</code>. The argument must be a subclass of BaseBytes or an
     * iterable type returning elements compatible with byte assignment.
     *
     * @param o the sequence of items to append to the list.
     */
    public void extend(PyObject o) {
        bytearray_extend(o);
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_extend_doc)
    final synchronized void bytearray_extend(PyObject o) {
        // Use the general method, assigning to the crack at the end of the array.
        // Note this deals with all legitimate PyObject types and the case o==this.
        setslice(size, size, 1, o);
    }

    /**
     * Implementation of Python <code>find(sub)</code>. Return the lowest index in the byte array
     * where byte sequence <code>sub</code> is found. Return -1 if <code>sub</code> is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @return index of start of occurrence of sub within this byte array
     */
    public int find(PyObject sub) {
        return basebytes_find(sub, null, null);
    }

    /**
     * Implementation of Python <code>find( sub [, start ] )</code>. Return the lowest index in the
     * byte array where byte sequence <code>sub</code> is found, such that <code>sub</code> is
     * contained in the slice <code>[start:]</code>. Return -1 if <code>sub</code> is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @return index of start of occurrence of sub within this byte array
     */
    public int find(PyObject sub, PyObject start) {
        return basebytes_find(sub, start, null);
    }

    /**
     * Implementation of Python <code>find( sub [, start [, end ]] )</code>. Return the lowest index
     * in the byte array where byte sequence <code>sub</code> is found, such that <code>sub</code>
     * is contained in the slice <code>[start:end]</code>. Arguments <code>start</code> and
     * <code>end</code> (which may be <code>null</code> or <code>Py.None</code> ) are interpreted as
     * in slice notation. Return -1 if <code>sub</code> is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @param end of slice to search
     * @return index of start of occurrence of sub within this byte array
     */
    public int find(PyObject sub, PyObject start, PyObject end) {
        return basebytes_find(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytearray_find_doc)
    final int bytearray_find(PyObject sub, PyObject start, PyObject end) {
        return basebytes_find(sub, start, end);
    }

    @Override
    public PyObject __iadd__(PyObject o) {
        return bytearray___iadd__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___iadd___doc)
    final synchronized PyObject bytearray___iadd__(PyObject o) {
        PyType oType = o.getType();
        if (oType == TYPE) {
            // Use the general method, specifying the crack at the end of the array.
            // Note this deals with the case o==this.
            setslice(size, size, 1, (BaseBytes)o);
        } else if (oType == PyString.TYPE) {
            // Will fail if somehow not 8-bit clean
            setslice(size, size, 1, (PyString)o);
        } else {
            // Unsuitable type
            throw ConcatenationTypeError(oType, TYPE);
        }
        return this;
    }

    /**
     * Implementation of Python <code>index(sub)</code>. Like {@link #find(PyObject)} but raise
     * {@link Py#ValueError} if <code>sub</code> is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @return index of start of occurrence of sub within this byte array
     */
    public int index(PyObject sub) {
        return bytearray_index(sub, null, null);
    }

    /**
     * Implementation of Python <code>index( sub [, start ] )</code>. Like
     * {@link #find(PyObject,PyObject)} but raise {@link Py#ValueError} if <code>sub</code> is not
     * found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @return index of start of occurrence of sub within this byte array
     */
    public int index(PyObject sub, PyObject start) {
        return bytearray_index(sub, start, null);
    }

    /**
     * Implementation of Python <code>index( sub [, start [, end ]] )</code>. Like
     * {@link #find(PyObject,PyObject,PyObject)} but raise {@link Py#ValueError} if <code>sub</code>
     * is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @param end of slice to search
     * @return index of start of occurrence of sub within this byte array
     * @throws PyException ValueError if sub not found in byte array
     */
    public int index(PyObject sub, PyObject start, PyObject end) throws PyException {
        return bytearray_index(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytearray_index_doc)
    final int bytearray_index(PyObject sub, PyObject start, PyObject end) {
        // Like find but raise a ValueError if not found
        int pos = basebytes_find(sub, start, end);
        if (pos < 0) {
            throw Py.ValueError("subsection not found");
        }
        return pos;
    }

    /**
     * Insert the argument element into the byte array at the specified index. Same as
     * <code>s[index:index] = [o] if index &gt;= 0</code>.
     *
     * @param index the position where the element will be inserted.
     * @param value the element to insert.
     */
    public void insert(PyObject index, PyObject value) {
        bytearray_insert(index, value);
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_insert_doc)
    final synchronized void bytearray_insert(PyObject index, PyObject value) {
        // XXX: do something with delegator instead?
        pyinsert(boundToSequence(index.asIndex()), value);
    }


    /**
     * str.join(iterable)
    Return a bytearray which is the concatenation of the strings in the iterable <code>iterable</code>. The separator between elements is the string providing this method.
     *
     * @param iterable of byte array objects, or objects viewable as such.
.
     */
    public PyByteArray join(PyObject iterable) {
        return bytearray_join(iterable);
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_join_doc)
    final PyByteArray bytearray_join(PyObject iterable) {
        return basebytes_join(iterable.asIterable());
    }


    @ExposedMethod(doc = BuiltinDocs.bytearray___len___doc)
    final int bytearray___len__() {
        return __len__();
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray___reduce___doc)
    final PyObject bytearray___reduce__() {
        return basebytes___reduce__();
    }

    /**
     * Remove the first occurrence of an element from the array, equivalent to:
     * <code>del s[s.index(x)]</code>, although x must be convertable to a single byte value. The
     * argument must be a PyInteger, PyLong or string of length 1.
     *
     * @param o the value to remove from the list.
     * @throws PyException ValueError if o not found in bytearray
     */
    public void remove(PyObject o) throws PyException {
        bytearray_append(o);
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_remove_doc)
    final synchronized void bytearray_remove(PyObject o) {
        // Check and extract the value, and search for it.
        byte b = byteCheck(o);
        int pos = index(b);
        // Not finding it is an error
        if (pos < 0) {
            throw Py.ValueError("value not found in bytearray");
        } else {
            storageDelete(pos, 1);
        }
    }

    /**
     * An implementation of Python <code>replace( old, new )</code>, returning a
     * <code>PyByteArray</code> with all occurrences of sequence <code>oldB</code> replaced by
     * <code>newB</code>.
     *
     * @param oldB sequence to find
     * @param newB relacement sequence
     * @return result of replacement as a new PyByteArray
     */
    public PyByteArray replace(PyObject oldB, PyObject newB) {
        return basebytes_replace(oldB, newB, -1);
    }

    /**
     * An implementation of Python <code>replace( old, new [, count ] )</code>, returning a
     * <code>PyByteArray</code> with all occurrences of sequence <code>oldB</code> replaced by
     * <code>newB</code>. If the optional argument <code>count</code> is given, only the first
     * <code>count</code> occurrences are replaced.
     *
     * @param oldB sequence to find
     * @param newB relacement sequence
     * @param maxcount maximum occurrences are replaced or all if <code>maxcount &lt; 0</code>
     * @return result of replacement as a new PyByteArray
     */
    public PyByteArray replace(PyObject oldB, PyObject newB, int maxcount) {
        return basebytes_replace(oldB, newB, maxcount);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytearray_replace_doc)
    final PyByteArray bytearray_replace(PyObject oldB, PyObject newB, PyObject count) {
        int maxcount = (count == null) ? -1 : count.asInt(); // or count.asIndex() ?
        return basebytes_replace(oldB, newB, maxcount);
    }

    /**
     * Implementation of Python <code>rfind(sub)</code>. Return the highest index in the byte array
     * where byte sequence <code>sub</code> is found. Return -1 if <code>sub</code> is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @return index of start of rightmost occurrence of sub within this byte array
     */
    public int rfind(PyObject sub) {
        return basebytes_rfind(sub, null, null);
    }

    /**
     * Implementation of Python <code>rfind( sub [, start ] )</code>. Return the highest index in
     * the byte array where byte sequence <code>sub</code> is found, such that <code>sub</code> is
     * contained in the slice <code>[start:]</code>. Return -1 if <code>sub</code> is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @return index of start of rightmost occurrence of sub within this byte array
     */
    public int rfind(PyObject sub, PyObject start) {
        return basebytes_rfind(sub, start, null);
    }

    /**
     * Implementation of Python <code>rfind( sub [, start [, end ]] )</code>. Return the highest
     * index in the byte array where byte sequence <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> (which may be <code>null</code> or
     * <code>Py.None</code> ) are interpreted as in slice notation. Return -1 if <code>sub</code> is
     * not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @param end of slice to search
     * @return index of start of rightmost occurrence of sub within this byte array
     */
    public int rfind(PyObject sub, PyObject start, PyObject end) {
        return basebytes_rfind(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytearray_rfind_doc)
    final int bytearray_rfind(PyObject sub, PyObject start, PyObject end) {
        return basebytes_rfind(sub, start, end);
    }

    /**
     * Implementation of Python <code>rindex(sub)</code>. Like {@link #find(PyObject)} but raise
     * {@link Py#ValueError} if <code>sub</code> is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @return index of start of occurrence of sub within this byte array
     */
    public int rindex(PyObject sub) {
        return bytearray_rindex(sub, null, null);
    }

    /**
     * Implementation of Python <code>rindex( sub [, start ] )</code>. Like
     * {@link #find(PyObject,PyObject)} but raise {@link Py#ValueError} if <code>sub</code> is not
     * found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @return index of start of occurrence of sub within this byte array
     */
    public int rindex(PyObject sub, PyObject start) {
        return bytearray_rindex(sub, start, null);
    }

    /**
     * Implementation of Python <code>rindex( sub [, start [, end ]] )</code>. Like
     * {@link #find(PyObject,PyObject,PyObject)} but raise {@link Py#ValueError} if <code>sub</code>
     * is not found.
     *
     * @param sub sequence to find (of a type viewable as a byte sequence)
     * @param start of slice to search
     * @param end of slice to search
     * @return index of start of occurrence of sub within this byte array
     */
    public int rindex(PyObject sub, PyObject start, PyObject end) {
        return bytearray_rindex(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytearray_rindex_doc)
    final int bytearray_rindex(PyObject sub, PyObject start, PyObject end) {
        // Like rfind but raise a ValueError if not found
        int pos = basebytes_rfind(sub, start, end);
        if (pos < 0) {
            throw Py.ValueError("subsection not found");
        }
        return pos;
    }

    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.bytearray_rsplit_doc)
    final PyList bytearray_rsplit(PyObject sep, int maxsplit) {
        return basebytes_rsplit(sep, maxsplit);
    }

    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.bytearray_split_doc)
    final PyList bytearray_split(PyObject sep, int maxsplit) {
        return basebytes_split(sep, maxsplit);
    }

    /**
     * Implementation of Python <code>startswith(prefix)</code>.
     *
     * When <code>prefix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this bytearray starts with the <code>prefix</code>.
     * <code>prefix</code> can also be a tuple of prefixes to look for.
     *
     * @param prefix byte array to match, or object viewable as such, or a tuple of them
     * @return true if and only if this bytearray starts with the prefix (or one of them)
     */
    public boolean startswith(PyObject prefix) {
        return basebytes_starts_or_endswith(prefix, null, null, false);
    }

    /**
     * Implementation of Python <code>startswith( prefix [, start ] )</code>.
     *
     * When <code>prefix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this bytearray starts with the <code>prefix</code>.
     * <code>prefix</code> can also be a tuple of prefixes to look for. With optional
     * <code>start</code> (which may be <code>null</code> or <code>Py.None</code>), define the
     * effective bytearray to be the slice <code>[start:]</code> of this bytearray.
     *
     * @param prefix byte array to match, or object viewable as such, or a tuple of them
     * @param start of slice in this bytearray to match
     * @return true if and only if this[start:] starts with the prefix (or one of them)
     */
    public boolean startswith(PyObject prefix, PyObject start) {
        return basebytes_starts_or_endswith(prefix, start, null, false);
    }

    /**
     * Implementation of Python <code>startswith( prefix [, start [, end ]] )</code>.
     *
     * When <code>prefix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this bytearray starts with the <code>prefix</code>.
     * <code>prefix</code> can also be a tuple of prefixes to look for. With optional
     * <code>start</code> and <code>end</code> (which may be <code>null</code> or
     * <code>Py.None</code>), define the effective bytearray to be the slice
     * <code>[start:end]</code> of this bytearray.
     *
     * @param prefix byte array to match, or object viewable as such, or a tuple of them
     * @param start of slice in this bytearray to match
     * @param end of slice in this bytearray to match
     * @return true if and only if this[start:end] starts with the prefix (or one of them)
     */
    public boolean startswith(PyObject prefix, PyObject start, PyObject end) {
        return basebytes_starts_or_endswith(prefix, start, end, false);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytearray_startswith_doc)
    final boolean bytearray_startswith(PyObject prefix, PyObject start, PyObject end) {
        return basebytes_starts_or_endswith(prefix, start, end, false);
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray___setitem___doc)
    final synchronized void bytearray___setitem__(PyObject o, PyObject def) {
        seq___setitem__(o, def);
    }

    @Override
    public String toString() {
        return bytearray_toString();
    }

    @ExposedMethod(names = {"__repr__", "__str__"}, doc = BuiltinDocs.bytearray___repr___doc)
    final synchronized String bytearray_toString() {
        return "bytearray(b'" + asEscapedString() + "')";
    }

    /*
     * ============================================================================================
     * Manipulation of storage capacity
     * ============================================================================================
     *
     * Here we add to the inherited variables defining byte storage, the methods necessary to resize
     * it.
     */

    /**
     * Choose a size appropriate to store the given number of bytes, with some room for growth.
     * We'll be more generous than CPython for small array sizes to avoid needless reallocation.
     *
     * @param size of storage actually needed
     * @return n >= size a recommended storage array size
     */
    private static final int roundUp(int size) {
        /*
         * The CPython formula is: size + (size >> 3) + (size < 9 ? 3 : 6). But when the array
         * grows, CPython can use a realloc(), which will often be able to extend the allocation
         * into memory already secretly allocated by the initial malloc(). Extension in Java means
         * that we have to allocate a new array of bytes and copy to it.
         */
        final int ALLOC = 16;   // Must be power of two!
        final int SIZE2 = 10;   // Smallest size leading to a return value of 2*ALLOC
        if (size >= SIZE2) {       // Result > ALLOC, so work it out
            // Same recommendation as Python, but rounded up to multiple of ALLOC
            return (size + (size >> 3) + (6 + ALLOC - 1)) & ~(ALLOC - 1);
        } else if (size > 0) {  // Easy: save arithmetic
            return ALLOC;
        } else {                // Very easy
            return 0;
        }
    }

    /**
     * Recommend a length for (but don't allocate) a new storage array, taking into account the
     * current length and the number of bytes now needed. The returned value will always be at least
     * as big as the argument (the length will always be sufficient). If the return value is equal
     * to the present length, it is recommending no reallocation; otherwise, the return is either
     * comfortably bigger than what is currently needed, or significantly smaller than currently
     * allocated. This method embeds our policy for growing or shrinking the allocated array.
     *
     * @param needed number of bytes needed
     * @return recommended amount to allocate
     */
    private final int recLength(int needed) {
        final int L = storage.length;
        if (needed > L || needed * 2 < L) {
            // Amount needed is a lot less than current length, or
            // more space we have, so recommend the new ideal size.
            return roundUp(needed);
        } else {
            // Amount needed is less than current length, but it doesn't justify reallocation
            return L;
        }
    }

    /**
     * Allocate fresh storage for at least the requested number of bytes. Spare bytes are allocated
     * evenly at each end of the new storage by choice of a new value for offset. If the size needed
     * is zero, the "storage" allocated is the shared emptyStorage array.
     *
     * @param needed becomes the new value of this.size
     */
    protected void newStorage(int needed) {
        if (needed > 0) {
            final int L = recLength(needed);
            byte[] s = new byte[L]; // guaranteed zero (by JLS 2ed para 4.5.5)
            setStorage(s, needed, (L - needed) / 2);
        } else {
            setStorage(emptyStorage);
        }
    }

    /**
     * Delete <code>d</code> elements at index <code>a</code> and prepare to insert <code>e</code>
     * elements there by moving aside the surrounding elements. The method manipulates the
     * <code>storage</code> array contents, <code>size</code> and <code>offset</code>. It will
     * allocate a new array <code>storage</code> if necessary, or if desirable for efficiency. If
     * the initial storage looks like this:
     *
     * <pre>
     *       |-                  s                -|
     * |--f--|--------a--------|---d---|-----b-----|----------------|
     * </pre>
     *
     * then after the call the (possibly new) storage looks like this:
     *
     * <pre>
     *            |-                   s'                -|
     * |----f'----|--------a--------|----e----|-----b-----|--------------|
     * </pre>
     *
     * where the contents of regions of length <code>a</code> and <code>b=size-(a+d)</code> have
     * been preserved, although probably moved, and the gap between them has been adjusted to the
     * requested size.
     * <p>
     * The effect on this PyByteArray is that:
     *
     * <pre>
     * this.offset = f'
     * this.size = s' = a + e + b
     * </pre>
     *
     * The method does not implement the Python repertoire of slice indices, or bound-check the
     * sizes given, since code leading up to the call has done that.
     *
     * @param a index of hole in byte array
     * @param d number to discard (will discard x[a,a+d-1])
     * @param e size of hole to open (will be x[a, a+e-1])
     */
    private void storageReplace(int a, int d, int e) {

        final int b = size - (a + d); // Count of B section
        final int c = e - d; // Change in overall size

        if (c == 0) {
            return;// Everything stays where it is.
        } else if (c > 0 && b == 0) {
            storageExtend(c); // This is really an extend/append operation
            return;
        }

        // Compute some handy points of reference
        final int L = storage.length;
        final int f = offset;
        final int s2 = a + e + b; // Size of result s'
        final int L2 = recLength(s2); // Length of storage for result

        if (L2 == L) {
            // The result will definitely fit into the existing array

            if (a <= b) {

                // It would be less copying if we moved A=x[:a] not B=x[-b:].
                // If B is to stay where it is, it means A will land here:
                final int f2 = f - c;
                if (f2 >= 0) {
                    // ... which luckily is still inside the array
                    if (a > 0) {
                        System.arraycopy(storage, f, storage, f2, a);
                    }
                    offset = f2;
                    size = s2;

                } else {
                    // ... which unfortunately is before the start of the array.
                    // We have to move both A and B within the existing array
                    newStorageAvoided(a, d, b, e);
                }

            } else /* a > b */{

                // It would be less copying if we moved B=x[-b:] not A=x[:a]
                // If A is to stay where it is, it means B will land here:
                final int g2 = f + a + e;
                if (g2 + b <= L) {
                    // ... which luckily leaves all of B inside the array
                    if (b > 0) {
                        System.arraycopy(storage, g2 - c, storage, g2, b);
                    }
                    // this.offset is unchanged
                    size = s2;

                } else {
                    // ... which unfortunately runs beyond the end of the array.
                    // We have to move both A and B within the existing array
                    newStorageAvoided(a, d, b, e);
                }
            }

        } else if (L2 > 0) {

            // New storage (bigger or much smaller) is called for. Copy A and B to it.
            newStorage(L2, a, d, b, e);

        } else {

            // We need no storage at all
            setStorage(emptyStorage);

        }
    }

    /**
     * Use the existing storage but move two blocks within it to leave a gap of the required size.
     * This is the strategy usually used when the array is still big enough to hold the required new
     * value, but we can't leave either block fixed. If the initial storage looks like this:
     *
     * <pre>
     * |-----f-----|--------a--------|---d---|----------b----------|----------|
     * </pre>
     *
     * then after the call the storage looks like this:
     *
     * <pre>
     *        |-                             s'                          -|
     * |--f'--|--------a--------|---------e---------|----------b----------|---|
     * </pre>
     *
     * where the regions of length <code>a</code> and <code>b=size-(a+d)</code> have been preserved
     * and the gap between them adjusted to specification. The new offset f' is chosen heuristically
     * by the method to optimise the efficiency of repeated adjustment near either end of the array,
     * e.g. repeated prepend or append operations. The effect on this PyByteArray is that:
     *
     * <pre>
     * this.offset = f'
     * this.size = s' = a+e+b
     * </pre>
     *
     * Arguments are not checked for validity <b>at all</b>. At call time, a, d, b, and e are
     * non-negative and not all zero.
     *
     * @param a length of A
     * @param d gap between A and B in current storage layout
     * @param b length of B
     * @param e gap between A and B in new storage layout.
     */
    private void newStorageAvoided(int a, int d, int b, int e) {

        // Compute some handy points of reference
        final int f = offset;
        final int g = f + a + d; // Location of B section x[-b]
        final int s2 = a + e + b; // Size of result s'

        // Choose the new offset f' to make prepend or append operations quicker.
        // E.g. if insertion was near the end (b small) put most of the new space at the end.
        int f2;
        if (a == b) {
            // Mainly to trap the case a=b=0
            f2 = (storage.length - s2) / 2;
        } else {
            // a and b are not both zero (since not equal)
            long spare = storage.length - s2;
            f2 = (int)((spare * b) / (a + b));
        }

        // This puts B at
        final int g2 = f2 + a + e;

        // We can make do with the existing array. Do an in place copy.
        if (f2 + a > g) {
            // New A overlaps existing B so we must copy B first
            if (b > 0) {
                System.arraycopy(storage, g, storage, g2, b);
            }
            if (a > 0) {
                System.arraycopy(storage, f, storage, f2, a);
            }
        } else {
            // Safe to copy A first
            if (a > 0) {
                System.arraycopy(storage, f, storage, f2, a);
            }
            if (b > 0) {
                System.arraycopy(storage, g, storage, g2, b);
            }
        }

        // We have a new size and offset (but the same storage)
        size = s2;
        offset = f2;

    }

    /**
     * Allocate new storage and copy two blocks from the current storage to it. If the initial
     * storage looks like this:
     *
     * <pre>
     * |--f--|--------a--------|---d---|-----b-----|----------------|
     * </pre>
     *
     * then after the call the (definitely new) storage looks like this:
     *
     * <pre>
     *            |-                   s'                -|
     * |----f'----|--------a--------|----e----|-----b-----|--------------|
     * </pre>
     *
     * where the regions of length <code>a</code> and <code>b=size-(a+d)</code> have been preserved
     * and the gap between them adjusted to specification. The new offset f' is chosen heuristically
     * by the method to optimise the efficiency of repeated adjustment near either end of the array,
     * e.g. repeated prepend or append operations. The effect on this PyByteArray is that:
     *
     * <pre>
     * this.offset = f'
     * this.size = s' = a+e+b
     * </pre>
     *
     * Arguments are not checked for validity <b>at all</b>. At call time, a, e and b are
     * non-negative and not all zero.
     *
     * @param L2 length of storage array to allocate (decided by caller)
     * @param a length of A
     * @param d gap between A and B in current storage layout
     * @param b length of B
     * @param e gap between A and B in new storage layout.
     */
    private void newStorage(int L2, int a, int d, int b, int e) {

        // Compute some handy points of reference
        final int f = offset;
        final int g = f + a + d; // Location of B section x[-b]
        final int s2 = a + e + b; // Size of result s'

        // New storage as specified by caller
        byte[] newStorage = new byte[L2];

        // Choose the new offset f' to make repeated prepend or append operations quicker.
        // E.g. if insertion was near the end (b small) put most of the new space at the end.
        int f2;
        if (a == b) {
            // Mainly to trap the case a=b=0
            f2 = (L2 - s2) / 2;
        } else {
            // a and b are not both zero (since not equal)
            long spare = L2 - s2;
            f2 = (int)((spare * b) / (a + b));
        }

        // Copy across the data from existing to new storage.
        if (a > 0) {
            System.arraycopy(storage, f, newStorage, f2, a);
        }
        if (b > 0) {
            System.arraycopy(storage, g, newStorage, f2 + a + e, b);
        }
        setStorage(newStorage, s2, f2);
    }

    /**
     * Prepare to insert <code>e</code> elements at the end of the storage currently in use. If
     * necessary. existing elements will be moved. The method manipulates the <code>storage</code>
     * array contents, <code>size</code> and <code>offset</code>. It will allocate a new array
     * <code>storage</code> if necessary for growth. If the initial storage looks like this:
     *
     * <pre>
     *               |-          s          -|
     * |------f------|-----------a-----------|-----------|
     * </pre>
     *
     * then after the call the storage looks like this:
     *
     * <pre>
     *               |-              s'             -|
     * |------f------|-----------a-----------|---e---|---|
     * </pre>
     *
     * or like this if e was too big for the gap, but not enough to provoke reallocation:
     *
     * <pre>
     * |-                    s'                 -|
     * |-----------a-----------|--------e--------|-------|
     * </pre>
     *
     * or like this if was necessary to allocate more storage:
     *
     * <pre>
     * |-                        s'                       -|
     * |-----------a-----------|-------------e-------------|--------------|
     * </pre>
     *
     * where the contents of region <code>a</code> have been preserved, although possbly moved, and
     * the gap at the end has the requested size. this method never shrinks the total storage. The
     * effect on this PyByteArray is that:
     *
     * <pre>
     * this.offset = f or 0
     * this.size = s' = a + e
     * </pre>
     *
     * The method does not implement the Python repertoire of slice indices, or bound-check the
     * sizes given, since code leading up to the call has done that.
     *
     * @param e size of hole to open (will be x[a, a+e-1]) where a = size before call
     */
    private void storageExtend(int e) {

        if (e == 0) {
            return; // Everything stays where it is.
        }

        // Compute some handy points of reference
        final int L = storage.length;
        final int f = offset;
        final int s2 = size + e; // Size of result s'
        final int L2 = recLength(s2); // Length of storage for result

        if (L2 <= L) {
            // Ignore recommendations to shrink and use the existing array
            // If A is to stay where it is, it means E will end here:
            final int g2 = f + s2;
            if (g2 > L) {
                // ... which unfortunately runs beyond the end of the array.
                // We have to move A within the existing array to make room
                if (size > 0) {
                    System.arraycopy(storage, offset, storage, 0, size);
                }
                offset = 0;
            }
            // New size
            size = s2;

        } else {
            // New storage size as recommended
            byte[] newStorage = new byte[L2];

            // Choose the new offset f'=0 to make repeated append operations quicker.
            // Copy across the data from existing to new storage.
            if (size > 0) {
                System.arraycopy(storage, f, newStorage, 0, size);
            }
            setStorage(newStorage, s2);

        }
    }

    /**
     * Delete <code>d</code> elements at index <code>a</code> by moving together the surrounding
     * elements. The method manipulates the <code>storage</code> array, <code>size</code> and
     * <code>offset</code>, and will allocate a new storage array if necessary, or if the deletion
     * is big enough. If the initial storage looks like this:
     *
     * <pre>
     * |-                           L                              -|
     *       |-                  s                -|
     * |--f--|--------a--------|---d---|-----b-----|----------------|
     * </pre>
     *
     * then after the call the (possibly new) storage looks like this:
     *
     * <pre>
     * |-                 L'                     -|
     *      |-                  s'               -|
     * |-f'-|--------a--------|-----b-----|-------|
     * </pre>
     *
     * where the regions of length <code>a</code> and <code>b=size-(a+d)</code> have been preserved
     * and the gap between them eliminated. The effect on this PyByteArray is that:
     *
     * <pre>
     * this.offset = f'
     * this.size = s' = a+b
     * </pre>
     *
     * The method does not implement the Python repertoire of slice indices but avoids indexing
     * outside the bytearray by silently adjusting a to be within it. Negative d is treated as 0 and
     * if d is too large, it is truncated to the array end.
     *
     * @param a index of hole in byte array
     * @param d number to discard (will discard x[a,a+d-1])
     * @param e size of hole to open (will be x[a, a+e-1])
     */
    private void storageDelete(int a, int d) {
        // storageReplace specialised for delete (e=0)

        if (d == 0)
         {
            return; // Everything stays where it is.
        }

        // Compute some handy points of reference
        final int L = storage.length;
        final int f = offset;
        final int b = size - (a + d); // Count of B section
        final int s2 = a + b; // Size of result s'
        final int L2 = recLength(s2); // Length of storage for result

        if (L2 == L) {

            // We are re-using the existing array
            if (a <= b) {
                // It would be less copying if we moved A=x[:a] not B=x[-b:].
                // If B is to stay where it is, it means A will land here:
                final int f2 = f + d;
                if (a > 0) {
                    System.arraycopy(storage, f, storage, f2, a);
                }
                offset = f2;
                size = s2;

            } else /* a > b */{
                // It would be less copying if we moved B=x[-b:] not A=x[:a]
                // If A is to stay where it is, it means B will land here:
                final int g2 = f + a;
                if (b > 0) {
                    System.arraycopy(storage, g2 + d, storage, g2, b);
                }
                // this.offset is unchanged
                size = s2;
            }

        } else if (L2 > 0) {

            // New storage (much smaller) is called for. Copy A and B to it.
            final int g = f + a + d; // Location of B section x[-b]

            // Choose the new offset f' to distribute space evenly.
            int f2 = (L2 - s2) / 2;

            // New storage as specified
            byte[] newStorage = new byte[L2];

            // Copy across the data from existing to new storage.
            if (a > 0) {
                System.arraycopy(storage, f, newStorage, f2, a);
            }
            if (b > 0) {
                System.arraycopy(storage, g, newStorage, f2 + a, b);
            }
            setStorage(newStorage, s2, f2);

        } else {

            // Everything must go
            setStorage(emptyStorage);

        }
    }

    /**
     * Delete <code>d</code> elements on a stride of <code>c</code> beginning at index
     * <code>a</code> by moving together the surrounding elements. The method manipulates the
     * <code>storage</code> array, <code>size</code> and <code>offset</code>, and will allocate a
     * new storage array if the deletion is big enough. If the initial storage looks like this:
     *
     * <pre>
     * |-                               L                                -|
     *       |-                    s                    -|
     * |--f--|-----a-----|---------e---------|-----b-----|----------------|
     * </pre>
     *
     * then after the call the (possibly new) storage looks like this:
     *
     * <pre>
     * |-                   L'                         -|
     *      |-                s'               -|
     * |-f'-|-----a-----|---(e-d)---|-----b-----|-------|
     * </pre>
     *
     * where the regions of length <code>a</code> and <code>b=size-(a+e)</code> have been preserved
     * and the <code>e</code> intervening elements reduced to <code>e-d</code> elements, by removing
     * exactly the elements with indices (relative to the start of valid data) <code>a+k*c</code>
     * for <code>k=0...d-1</code>. The effect on this PyByteArray is that:
     *
     * <pre>
     * this.offset = f'
     * this.size = s' = a+b
     * </pre>
     *
     * The method does not implement the Python repertoire of slice indices but avoids indexing
     * outside the bytearray by silently adjusting a to be within it. Negative d is treated as 0 and
     * if d is too large, it is truncated to the array end.
     *
     * @param a index of hole in byte array
     * @param c (>0) step size between the locations of elements to delete
     * @param d number to discard (will discard x[a+k*c] for k=0...d-1)
     */
    private void storageDeleteEx(int a, int c, int d) {

        // XXX Base this on storageReplace with the same a<b logic but piecewise copy
        // XXX Change SequenceIndexDelegate to use (and PyList to implement) delslice()
    }
}

