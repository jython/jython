package org.python.core;

import java.util.Arrays;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

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
     * @param size of bytearray
     */
    public PyByteArray(int size) {
        super(TYPE);
        init(size);
    }

    /**
     * Construct bytearray by copying values from int[].
     * 
     * @param value source of the bytes (and size)
     */
    public PyByteArray(int[] value) {
        super(TYPE, value);
    }

    /**
     * Create a new array filled exactly by a copy of the contents of the
     * source.
     * @param value source of the bytes (and size)
     */
    public PyByteArray(BaseBytes value) {
        super(TYPE);
        init(value);
    }
    
    /**
     * Create a new array filled exactly by a copy of the contents of the
     * source.
     * @param value source of the bytes (and size)
     */
    public PyByteArray(MemoryViewProtocol value) {
        super(TYPE);
        init(value.getMemoryView());
    }
    
    /**
     * Create a new array filled from an iterable of PyObject. The iterable must yield objects
     * convertible to Python bytes (non-negative integers less than 256 or strings of length 1).
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
     * non-integer argument type, and ValueError if iterables do not yield byte [0..255] values.
     */
    public PyByteArray(PyObject arg) throws PyException {
        super(TYPE);
        init(arg);
    }
    
    /* ========================================================================================
     * API for org.python.core.PySequence
     * ========================================================================================
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
            int n = stop-start;
            if (n<=0)
                return new PyByteArray();
            else {
                PyByteArray ret = new PyByteArray(n);
                System.arraycopy(storage, offset+start, ret.storage, ret.offset, n);
                return ret;
            }
        } else {
            int n = sliceLength(start, stop, step);
            PyByteArray ret = new PyByteArray(n);
            n += ret.offset;
            byte[] dst = ret.storage;
            for (int io = start + offset, jo = ret.offset; jo < n; io += step, jo++)
                dst[jo] = storage[io];
            return ret;
        }
    }

    
    /**
     * Returns a PyByteArray that repeats this  sequence the given number of times, as
     * in the implementation of <tt>__mul__</tt> for strings.
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
     * Sets the indexed element of the bytearray to the given value.
     * This is an extension point called by PySequence in its implementation of
     * {@link #__setitem__}
     * It is guaranteed by PySequence that the index is within the bounds of the array.
     * Any other clients calling <tt>pyset(int)</tt> must make the same guarantee.
     *
     * @param index index of the element to set.
     * @param value the value to set this element to.
     * @throws PyException(AttributeError) if value cannot be converted to an integer
     * @throws PyException(ValueError) if value<0 or value>255
     */
    public synchronized void pyset(int index, PyObject value) throws PyException {
        storage[index+offset] = byteCheck(value); 
    }

    /**
     * Insert the element (interpreted as a Python byte value) at the given index. The default
     * implementation produces a Python TypeError, for the benefit of immutable types. Mutable types
     * must override it.
     * 
     * @param index to insert at
     * @param element to insert (by value)
     * @throws PyException(IndexError) if the index is outside the array bounds
     * @throws PyException(ValueError) if element<0 or element>255
     * @throws PyException(TYpeError) if the subclass is immutable
     */
    public synchronized void pyadd(int index, PyInteger element) {
        // Open a space at the right location.
        storageReplace(index, 0, 1);
        storage[index] = byteCheck(element); 
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

        if (step == 1 && stop < start)
        // Because "b[5:2] = v" means insert v just before 5 not 2.
        // ... although "b[5:2:-1] = v means b[5]=v[0], b[4]=v[1], b[3]=v[2]
        stop = start;

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
            if (n != len) throw SliceSizeError("bytes", len, n);
            for (int io = start + offset; n > 0; io += step, --n)
                storage[io] = 0;
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
            if (n != len) throw SliceSizeError("bytes", len, n);
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
        Py.NotImplementedError("memoryview not yet supported in bytearray");
        String format = value.get_format();
        boolean isBytes = format == null || "B".equals(format);
        if (value.get_ndim() != 1 || !isBytes)
            Py.TypeError("memoryview value must be byte-oriented");
        else {
            // Dimensions are given as a PyTple (although only one)
            int len = value.get_shape().pyget(0).asInt();
            if (step == 1) {
                // Delete this[start:stop] and open a space of the right size
                storageReplace(start, stop - start, len);
                // System.arraycopy(value.storage, value.offset, storage, start
                // + offset, len);
            } else {
                // This is an extended slice which means we are replacing elements
                int n = sliceLength(start, stop, step);
                if (n != len) throw SliceSizeError("bytes", len, n);
                // int no = n + value.offset;
                // for (int io = start + offset, jo = value.offset; jo < no; io += step, jo++) {
                // storage[io] = value.storage[jo]; // Assign this[i] = value[j]
                // }
            }
        }
    }

    /**
     * Sets the given range of elements according to Python slice assignment semantics
     * from a bytearray (or bytes).
     * @see #setslice(int, int, int, PyObject)
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param value a bytearray (or bytes) object consistent with the slice assignment
     * @throws PyException(SliceSizeError) if the value size is inconsistent with an extended slice
     */
    private void setslice(int start, int stop, int step, BaseBytes value) throws PyException {
        if (value == this) value = new PyByteArray(value);  // Must work with a copy
        int len = value.size;
        if (step == 1) {
            //Delete this[start:stop] and open a space of the right size
            storageReplace(start, stop - start, len);
            System.arraycopy(value.storage, value.offset, storage, start
                    + offset, len);
        } else {
            // This is an extended slice which means we are replacing elements
            int n = sliceLength(start, stop, step);
            if (n != len) throw SliceSizeError("bytes", len, n);
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
         * until after we run the iterator. We use this elastic byte structure to hold the bytes until then.
         */
        FragmentList fragList = new BaseBytes.FragmentList();
        fragList.loadFrom(iter);

        if (step == 1) {
            // Delete this[start:stop] and open a space of the right size
            storageReplace(start, stop - start, fragList.totalCount);
            if (fragList.totalCount > 0) 
                // Stitch the fragments together in the space we made
                fragList.emptyInto(storage, start + offset);

        } else {
            // This is an extended slice which means we are replacing elements
            int n = sliceLength(start, stop, step);
            if (n != fragList.totalCount) throw SliceSizeError("bytes", fragList.totalCount, n);
            fragList.emptyInto(storage, start + offset, step);
        }
    }


// Idiom:    
//    if (step == 1) {
//        // Do something efficient with block start...stop-1
//    } else {
//        int n = sliceLength(start, stop, step);
//        for (int i = start, j = 0; j < n; i += step, j++) {
//            // Perform jth operation with element i
//        }
//    }
    
    
    
    /*
     * Deletes an element from the sequence (and closes up the gap).
     * @param index index of the element to delete.
     */
    protected synchronized void del(int index) {
        // XXX Change SequenceIndexDelegate to avoid repeated calls to del(int) for extended slice
        storageDelete(index, 1);
    }

    /*
     * Deletes contiguous sub-sequence (and closes up the gap).
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     */
    protected synchronized void delRange(int start, int stop) {
        // XXX Use the specialised storageDelete()
        storageReplace(start, stop-start, 0);
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
                if (step > 0)
                    // The first element is x[start] and the last is x[start+(n-1)*step+1]
                    storageDeleteEx(start, step, n);
                else
                    // The first element is x[start+(n-1)*step+1] and the last is x[start]
                    storageDeleteEx(start + (n - 1) * step + 1, -step, n);
            }
        }
    }

    /**
     * Convenience method to create a <code>ValueError</code> PyException with the message
     * "attempt to assign {type} of size {valueSize} to extended slice of size {sliceSize}"
     * 
     * @param valueType
     * @param valueSize size of sequence being assigned to slice
     * @param sliceSize size of slice expected to receive
     * @throws PyException (ValueError) as detailed
     */
    public static PyException SliceSizeError(String valueType, int valueSize, int sliceSize)
            throws PyException {
        String fmt = "attempt to assign %s of size %d to extended slice of size %d";
        return Py.ValueError(String.format(fmt, valueType, valueSize, sliceSize));
        // XXX consider moving to SequenceIndexDelegate.java or somewhere else generic
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
     * non-integer argument type, and ValueError if iterables do not yield byte [0..255] values.
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
         * This whole method is modelled on CPython (see Objects/bytearrayobject.c : bytes_init())
         * but reorganised somewhat to maximise re-use withthe implementation of assignment to a
         * slice, which essentially has to construct a bytearray from the right-hand side.
         * Hopefully, it still tries the same things in the same order and fails in the same way.
         */

        if (encoding != null || errors != null) {
            /*
             * bytearray(string [, encoding [, errors]]) Construct from a text string by encoding it
             * using the specified encoding.
             */
            if (arg == null || !(arg instanceof PyString)) throw Py.TypeError("encoding or errors without sequence argument");
            init((PyString)arg, encoding, errors);
            
        } else {
            // Now construct from arbitrary object (or null)
            init(arg);
        }

    }
    
    
    @Override
    public int __len__() {
        return list___len__();
    }

    @ExposedMethod(doc = BuiltinDocs.list___len___doc)
    final int list___len__() {
        return size;
    }

    
// Based on PyList and not yet properly implemented.
//
//    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___ne___doc)
//    final synchronized PyObject bytearray___ne__(PyObject o) {
//        return seq___ne__(o);
//    }
//
//    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___eq___doc)
//    final synchronized PyObject bytearray___eq__(PyObject o) {
//        return seq___eq__(o);
//    }
//
//    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___lt___doc)
//    final synchronized PyObject bytearray___lt__(PyObject o) {
//        return seq___lt__(o);
//    }
//
//    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___le___doc)
//    final synchronized PyObject bytearray___le__(PyObject o) {
//        return seq___le__(o);
//    }
//
//    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___gt___doc)
//    final synchronized PyObject bytearray___gt__(PyObject o) {
//        return seq___gt__(o);
//    }
//
//    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___ge___doc)
//    final synchronized PyObject bytearray___ge__(PyObject o) {
//        return seq___ge__(o);
//    }
//
//    @Override
//    public PyObject __imul__(PyObject o) {
//        return bytearray___imul__(o);
//    }
//
//    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___imul___doc)
//    final synchronized PyObject bytearray___imul__(PyObject o) {
//        if (!o.isIndex()) {
//            return null;
//        }
//        int count = o.asIndex(Py.OverflowError);
//
//        int size = size();
//        if (size == 0 || count == 1) {
//            return this;
//        }
//
//        if (count < 1) {
//            clear();
//            return this;
//        }
//
//        if (size > Integer.MAX_VALUE / count) {
//            throw Py.MemoryError("");
//        }
//
//        int newSize = size * count;
//        if (storage instanceof ArrayList) {
//            ((ArrayList) storage).ensureCapacity(newSize);
//        }
//        List<PyObject> oldList = new ArrayList<PyObject>(storage);
//        for (int i = 1; i < count; i++) {
//            storage.addAll(oldList);
//        }
//        gListAllocatedStatus = storage.size(); // now omit?
//        return this;
//    }
//
//    @Override
//    public PyObject __mul__(PyObject o) {
//        return bytearray___mul__(o);
//    }
//
//    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___mul___doc)
//    final synchronized PyObject bytearray___mul__(PyObject o) {
//        if (!o.isIndex()) {
//            return null;
//        }
//        return repeat(o.asIndex(Py.OverflowError));
//    }
//
//    @Override
//    public PyObject __rmul__(PyObject o) {
//        return bytearray___rmul__(o);
//    }
//
//    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___rmul___doc)
//    final synchronized PyObject bytearray___rmul__(PyObject o) {
//        if (!o.isIndex()) {
//            return null;
//        }
//        return repeat(o.asIndex(Py.OverflowError));
//    }
//
//    @Override
//    public PyObject __add__(PyObject o) {
//        return bytearray___add__(o);
//    }
//
//    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___add___doc)
//    final synchronized PyObject bytearray___add__(PyObject o) {
//        PyByteArray sum = null;
//        if (o instanceof PySequenceList && !(o instanceof PyTuple)) {
//            if (o instanceof PyByteArray) {
//                List oList = ((PyByteArray) o).storage;
//                List newList = new ArrayList(storage.size() + oList.size());
//                newList.addAll(storage);
//                newList.addAll(oList);
//                sum = fromList(newList);
//            }
//        } else if (!(o instanceof PySequenceList)) {
//            // also support adding java lists (but not PyTuple!)
//            Object oList = o.__tojava__(List.class);
//            if (oList != Py.NoConversion && oList != null) {
//                List otherList = (List) oList;
//                sum = new PyByteArray();
//                sum.bytearray_extend(this);
//                for (Iterator i = otherList.iterator(); i.hasNext();) {
//                    sum.add(i.next());
//                }
//            }
//        }
//        return sum;
//    }
//
//    @ExposedMethod(doc = BuiltinDocs.bytearray___contains___doc)
//    final synchronized boolean bytearray___contains__(PyObject o) {
//        return object___contains__(o);
//    }
//
//    @ExposedMethod(doc = BuiltinDocs.bytearray___delitem___doc)
//    final synchronized void bytearray___delitem__(PyObject index) {
//        seq___delitem__(index);
//    }
//
    @ExposedMethod(doc = BuiltinDocs.bytearray___setitem___doc)
    final synchronized void bytearray___setitem__(PyObject o, PyObject def) {
        seq___setitem__(o, def);
    }

//    @ExposedMethod(doc = BuiltinDocs.bytearray___getitem___doc)
//    final synchronized PyObject bytearray___getitem__(PyObject o) {
//        PyObject ret = seq___finditem__(o);
//        if (ret == null) {
//            throw Py.IndexError("index out of range: " + o);
//        }
//        return ret;
//    }
//
//    @Override
//    public PyObject __iter__() {
//        return bytearray___iter__();
//    }
//
//    @ExposedMethod(doc = BuiltinDocs.bytearray___iter___doc)
//    public synchronized PyObject bytearray___iter__() {
//        return new PyFastSequenceIter(this);
//    }
//
//    @Override
//    protected String unsupportedopMessage(String op, PyObject o2) {
//        if (op.equals("+")) {
//            return "can only concatenate storage (not \"{2}\") to storage";
//        }
//        return super.unsupportedopMessage(op, o2);
//    }
//
//    public String toString() {
//        return bytearray_toString();
//    }
//
    @ExposedMethod(names = "__repr__", doc = BuiltinDocs.bytearray___repr___doc)
    final synchronized String bytearray_toString() {
        // XXX revisit: understand the thread state logic and use encode()
//        ThreadState ts = Py.getThreadState();
//        if (!ts.enterRepr(this)) {
//            return "[...]";
//        }
        StringBuilder buf = new StringBuilder("bytearray(b'");
        final int last = size()-1;
        for (int i=0; i<=last; i++) {
            int element = intAt(i);
            if (Character.isISOControl(element))
                buf.append(String.format("\\x%02x", element));
            else
                buf.append((char)element);
        }
        buf.append("')");
//        ts.exitRepr(this);
        return buf.toString();
    }

    
    
    
    
    /*
     * ========================================================================================
     * Manipulation of storage capacity
     * ========================================================================================
     * 
     * Here we add to the inherited variables defining byte storage, the methods necessary to resize
     * it.
     */    
   
    /**
     * Choose a size appropriate to store the given number of bytes, with some room for growth.
     * @param size
     * @return n >= needed
     */
    private static final int roundUp(int size) {
        // XXX Consider equivalent case statement
        int alloc = size + (size >> 3) + (size < 9 ? 3 : 6);    // As CPython
        // XXX What's a good allocation unit size here?
        final int ALLOC = 8;
        return (alloc+(ALLOC-1)) & ~(ALLOC-1);                  // round up to multiple of ALLOC
    }
    
    /**
     * Used mainly to prevent repeated attempts to shrink an array that is already minimal.
     */
    private static final int minAlloc = roundUp(1);
        
    /**
     * Decide whether a new storage array should be allocated (but don't do it). This method returns
     * true if the needed storage is bigger than the allocated array length.
     * 
     * @param needed number of bytes needed
     * @return true if needed number of bytes justifies a new array
     */
    private final boolean shouldGrow(int needed) {
        return needed > storage.length;
    }

    /**
     * Decide whether a smaller storage array should be allocated (but don't do it). This method
     * returns true if the needed storage size is much smaller than the allocated array length.
     * 
     * @param needed number of bytes needed
     * @return true if needed number of bytes justifies a new array
     */
    private final boolean shouldShrink(int needed) {
        return needed == 0 || (needed * 2 + minAlloc) < storage.length;
    }

    /**
     * Decide whether a new storage array should be allocated (but don't do it). This method returns
     * true if the needed storage is bigger, or much smaller, than the allocated array length.
     * 
     * @param needed number of bytes needed
     * @return true if needed number of bytes justifies a new array
     */
    private final boolean shouldResize(int needed) {
        return shouldGrow(needed) || shouldShrink(needed);
    }

    /**
     * Allocate fresh storage for at least the requested number of bytes. Spare bytes are alloceted
     * evenly at each end of the new storage by choice of a new value for offset.
     * If the size needed is zero, the "storage" allocated is the shared emptyStorage array.
     * @param needed becomes the new value of this.size
     */
    protected void newStorage(int needed) {
        if (needed > 0) {
            byte[] s = new byte[roundUp(needed)]; // guaranteed zero (by JLS 2ed para 4.5.5)
            setStorage(s, needed, (s.length - needed) / 2);
        } else
            setStorage(emptyStorage);
    }

    /**
     * Ensure there is storage for at least the requested number of bytes, optionally clearing
     * elements to zero. After the call, the needed number of bytes will be available,
     * and if requested in the second parameter, they are guaranteed to be zero.
     * @param needed number of bytes
     * @param clear if true, storage bytes guaranteed zero
     */
    private void newStorage(int needed, boolean clear) {
        if (shouldResize(needed))
            newStorage(needed);                 // guaranteed zero
        else {
            setStorage(storage, needed, (storage.length - needed) / 2);
            if (clear) Arrays.fill(storage, (byte)0); // guarantee zero
        }
    }


    
    /**
     * Delete <code>d</code> elements at index <code>a</code> and prepare to insert
     * <code>e</code> elements there by moving aside the surrounding elements.
     * The method manipulates the <code>storage</code> array contents, <code>size</code> and
     * <code>offset</code>. It will allocate a new array <code>storage</code> if necessary,
     * or if desirable for efficiency. If the initial storage looks like this:
     * <pre>
     *       |-                  s                -|
     * |--f--|--------a--------|---d---|-----b-----|----------------|
     * </pre>
     * then after the call the (possibly new) storage looks like this:
     * <pre>
     *            |-                   s'                -|
     * |----f'----|--------a--------|----e----|-----b-----|--------------|
     * </pre>
     * where the contents of regions of length <code>a</code> and <code>b=size-(a+d)</code> have
     * been preserved, although probably moved, and the gap between them has been adjusted to
     * the requested size.
     * <p>
     * The effect on this PyByteArray is that:
     * <pre>
     * this.offset = f'
     * this.size = s' = a + e + b
     * </pre>
     * The method does not implement the Python repertoire of slice indices but avoids indexing
     * outside the bytearray by silently adjusting a to be within it.
     * Negative d or e is treated as 0 and if d is too large, it is truncated to the array end.
     * @param a index of hole in byte array 
     * @param d number to discard (will discard x[a,a+d-1])
     * @param e size of hole to open (will be x[a, a+e-1])
     */
    private void storageReplace(int a, int d, int e) {

        int s = this.size;

        // Some of these should perhaps be errors but let's silently correct insane requests
        if (a<0) a=0; else if (a>s) a = s;
        if (d<0) d=0; else if (d>s-a) d = s-a;
        if (e<0) e=0;

        if (e != d) {
            // Otherwise, everything stays where it is.
            // Handy derived values:
            int b = s - (a + d);    // which is >= 0
            int s2 = a + e + b;     // which is >= 0
            int f = this.offset;    // Location of x[0]
            int g = f + (a + d);    // Location of x[-b]
            
            if (shouldShrink(s2)) {
                if (s2 > 0)
                    // We have far more storage than we need: shrink and copy both parts
                    newStorage(f, a, g, b, e);
                else
                    // Need no storage as a+e+b = 0
                    setStorage(emptyStorage);

            } else if (a < b) {
                // It would be less copying if we moved A=x[:a] not B=x[-b:].
                // If B is to stay where it is, it means A will land here:
                int f2 = f - (e - d);
                if (f2 >= 0) {
                    // ... which luckily is still inside the array
                    if (a > 0) System.arraycopy(storage, f, storage, f2, a);
                    this.offset = f2;
                    size = s2;
                } else {
                    // ... which unfortunately is before the start of the array.
                    // We have to move both A and B and it might be time for a new array.
                    if (s2<=storage.length)
                        // Repack it all in the existing array
                        newStorageAvoided(f, a, g, b, e);
                    else
                        newStorage(f, a, g, b, e);
                }

            } else /* a >= b */{
                // It would be less copying if we moved B=x[-b:] not A=x[:a]
                // If A is to stay where it is, it means B will land here:
                int g2 = g + (e - d);
                if (g2 + b <= storage.length) {
                    // ... which luckily leaves all of B inside the array
                    if (b > 0) System.arraycopy(storage, g, storage, g2, b);
                    // this.offset is unchanged
                    size = s2;
                } else {
                    // ... which unfortunately runs beyond the end of the array.
                    // We have to move both A and B and it might be time for a new array.
                    if (s2<=storage.length)
                        // Repack it all in the existing array
                        newStorageAvoided(f, a, g, b, e);
                    else
                        newStorage(f, a, g, b, e);
                }
            }
        }

    }
    
   
    /**
     * Use the existing storage but move two blocks within it to leave a gap of the required size.
     * This is the strategy usually used when the array is still big enough to hold the required
     * new value, but we can't leave either block fixed.
     * If the initial storage looks like this:
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
     * Arguments are not checked for validity <b>at all</b>.
     * a, e and b are non-negative and not all zero.
     * 
     * @param f location (with offset) of A
     * @param a length of A
     * @param g = f+a+d location (with offset) of B
     * @param b length of B
     * @param e gap between A and B in new storage.
     */
    private void newStorageAvoided(int f, int a, int g, int b, int e) {

        // Shorthands
        int s2 = a + e + b;

        // Choose the new offset f' to make prepend or append operations quicker.
        // E.g. if insertion was near the end (b small) put most of the new space at the end.
        int f2;
        if (a == b)
            // Mainly to trap the case a=b=0
            f2 = (storage.length - s2) / 2;
        else {
            // a and b are not both zero (since not equal)
            long spare = storage.length - s2;
            f2 = (int)((spare * b) / (a + b));
        }
        // We have a new size and offset (but the same storage)
        size = s2;
        offset = f2;
        
        // This puts B at
        int g2 = f2 + a + e;

        // We can make do with the existing array. Do an in place copy.
        if (f2 + a > g) {
            // New A overlaps existing B so we must copy B first
            if (b > 0) System.arraycopy(storage, g, storage, g2, b);
            if (a > 0) System.arraycopy(storage, f, storage, f2, a);
        } else {
            // Safe to copy A first
            if (a > 0) System.arraycopy(storage, f, storage, f2, a);
            if (b > 0) System.arraycopy(storage, g, storage, g2, b);
        }

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
     * Arguments are not checked for validity <b>at all</b>.
     * a, e and b are non-negative and not all zero.
     * 
     * @param f location (with offset) of A
     * @param a length of A
     * @param g = f+a+d location (with offset) of B
     * @param b length of B
     * @param e gap between A and B in new storage.
     */
    private void newStorage(int f, int a, int g, int b, int e) {
        // Enough room for the data and the gap
        int s2 = a + e + b;
        // Preserve a reference to the current data in the storage being discarded
        byte[] source = this.storage;
        // New storage with a bit of elbow-room
        byte[] newStorage = new byte[roundUp(s2)];
        // Choose the new offset f' to make prepend or append operations quicker.
        // E.g. if insertion was near the end (b small) put most of the new space at the end.
        int f2;
        if (a == b)
            // Mainly to trap the case a=b=0
            f2 = (newStorage.length - s2) / 2;
        else {
            // a and b are not both zero (since not equal)
            long spare = newStorage.length - s2;
            f2 = (int)((spare * b) / (a + b));
        }
        setStorage(newStorage, s2, f2);

        // Copy across the data
        if (a > 0) System.arraycopy(source, f, storage, offset, a);
        if (b > 0) System.arraycopy(source, g, storage, offset + (a + e), b);
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
     * The method does not implement the Python repertoire of slice indices but avoids indexing
     * outside the bytearray by silently adjusting a to be within it.
     * Negative d is treated as 0 and if d is too large, it is truncated to the array end.
     * 
     * @param a index of hole in byte array
     * @param d number to discard (will discard x[a,a+d-1])
     * @param e size of hole to open (will be x[a, a+e-1])
     */
    private void storageDelete(int a, int d) {
        // storageReplace specialised for delete (e=0)
        int s = this.size;

        // Some of these should perhaps be errors but let's silently correct insane requests
        if (a < 0) a = 0; else if (a > s) a = s;
        if (d < 0) d = 0; else if (d > s - a) d = s - a;

        // Handy derived values
        int b = s - (a + d);    // which is >= 0
        int s2 = s - d;         // which is >= 0
        int f = this.offset;    // Location of x[0]
        int g = f + (a + d);    // Location of x[-b]

        if (shouldShrink(s2)) {
            // We have far more storage than we need: shrink and copy both parts
            // Preserve a reference to the current data in the storage being discarded
            byte[] source = this.storage;
            // New storage with a bit of elbow-room
            newStorage(s2);
            // Copy across the data
            if (a > 0) System.arraycopy(source, f, storage, offset, a);
            if (b > 0) System.arraycopy(source, g, storage, offset + a, b);

        } else {
            if (a < b) {
                // It would be less copying if we moved A=x[:a] not B=x[-b:].
                // If B is to stay where it is, it means A will land here:
                int f2 = f + d;
                if (a > 0) System.arraycopy(storage, f, storage, f2, a);
                this.offset = f2;

            } else /* a >= b */{
                // It would be less copying if we moved B=x[-b:] not A=x[:a]
                // If A is to stay where it is, it means B will land here:
                int g2 = f + a;
                if (b > 0) System.arraycopy(storage, g, storage, g2, b);
            }
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
     * |-                  L'                  -|
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

/*
 *  >>> for method in dir(bytearray):
        ...     print method
        ...
        __add__
        __alloc__
        __class__
        __contains__
        __delattr__
        __delitem__
        __doc__
        __eq__
        __format__
        __ge__
        __getattribute__
        __getitem__
        __gt__
        __hash__
        __iadd__
        __imul__
        __init__
        __iter__
        __le__
        __len__
        __lt__
        __mul__
        __ne__
        __new__
        __reduce__
        __reduce_ex__
        __repr__
        __rmul__
        __setattr__
        __setitem__
        __sizeof__
        __str__
        __subclasshook__
        append
        capitalize
        center
        count
        decode
        endswith
        expandtabs
        extend
        find
        fromhex
        index
        insert
        isalnum
        isalpha
        isdigit
        islower
        isspace
        istitle
        isupper
        join
        ljust
        lower
        lstrip
        partition
        pop
        remove
        replace
        reverse
        rfind
        rindex
        rjust
        rpartition
        rsplit
        rstrip
        split
        splitlines
        startswith
        strip
        swapcase
        title
        translate
        upper
        zfill
        >>>
 */