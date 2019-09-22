package org.python.core;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import org.python.core.buffer.BaseBuffer;
import org.python.core.buffer.SimpleWritableBuffer;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * Implementation of Python <code>bytearray</code> with a Java API that includes equivalents to most
 * of the Python API. These Python equivalents accept a {@link PyObject} as argument, where you
 * might have expected a <code>byte[]</code> or <code>PyByteArray</code>, in order to accommodate
 * the full range of types accepted by the Python equivalent: usually, any <code>PyObject</code>
 * that implements {@link BufferProtocol}, providing a one-dimensional array of bytes, is an
 * acceptable argument. In the documentation, the reader will often see the terms "byte array" or
 * "object viewable as bytes" instead of <code>bytearray</code> when this broader scope is intended.
 * This may relate to parameters, or to the target object itself (in text that applies equally to
 * base or sibling classes).
 */
@Untraversable
@ExposedType(name = "bytearray", base = PyObject.class, doc = BuiltinDocs.bytearray_doc)
public class PyByteArray extends BaseBytes implements BufferProtocol {

    /** The {@link PyType} of <code>bytearray</code>. */
    public static final PyType TYPE = PyType.fromClass(PyByteArray.class);

    /**
     * Constructs a zero-length Python <code>bytearray</code> of explicitly-specified sub-type
     *
     * @param type explicit Jython type
     */
    public PyByteArray(PyType type) {
        super(type);
    }

    /**
     * Constructs a zero-length Python <code>bytearray</code>.
     */
    public PyByteArray() {
        super(TYPE);
    }

    /**
     * Constructs zero-filled Python <code>bytearray</code> of specified size.
     *
     * @param size of <code>bytearray</code>
     */
    public PyByteArray(int size) {
        super(TYPE);
        init(size);
    }

    /**
     * Constructs a <code>bytearray</code> by copying values from int[].
     *
     * @param value source of the bytes (and size)
     */
    public PyByteArray(int[] value) {
        super(TYPE, value);
    }

    /**
     * Constructs a new array filled exactly by a copy of the contents of the source, which is a
     * <code>bytearray</code> (or <code>bytes</code>).
     *
     * @param value source of the bytes (and size)
     */
    public PyByteArray(BaseBytes value) {
        super(TYPE);
        init(value);
    }

    /**
     * Constructs a new array filled exactly by a copy of the contents of the source, which is a
     * byte-oriented {@link PyBuffer}.
     *
     * @param value source of the bytes (and size)
     */
    PyByteArray(PyBuffer value) {
        super(TYPE);
        init(value);
    }

    /**
     * Constructs a new array filled exactly by a copy of the contents of the source, which is an
     * object supporting the Jython version of the PEP 3118 buffer API.
     *
     * @param value source of the bytes (and size)
     */
    public PyByteArray(BufferProtocol value) {
        super(TYPE);
        init(value);
    }

    /**
     * Constructs a new array filled from an iterable of PyObject. The iterable must yield objects
     * convertible to Python bytes (non-negative integers less than 256 or strings of length 1).
     *
     * @param value source of the bytes (and size)
     */
    public PyByteArray(Iterable<? extends PyObject> value) {
        super(TYPE);
        init(value);
    }

    /**
     * Constructs a new array by encoding a PyString argument to bytes. If the PyString is actually
     * a PyUnicode, the encoding must be explicitly specified.
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
     * Constructs a new array by encoding a PyString argument to bytes. If the PyString is actually
     * a PyUnicode, the encoding must be explicitly specified.
     *
     * @param arg primary argument from which value is taken
     * @param encoding name of optional encoding (may be <code>null</code> to select the default for
     *            this installation)
     * @param errors name of optional errors policy
     */
    public PyByteArray(PyString arg, String encoding, String errors) {
        super(TYPE);
        init(arg, encoding, errors);
    }

    /**
     * Constructs a new array by encoding a PyString argument to bytes. If the PyString is actually
     * a PyUnicode, an exception is thrown saying that the encoding must be explicitly specified.
     *
     * @param arg primary argument from which value is taken
     */
    public PyByteArray(PyString arg) {
        super(TYPE);
        init(arg, (String)null, (String)null);
    }

    /**
     * Constructs a <code>bytearray</code> by re-using an array of byte as storage initialised by
     * the client.
     *
     * @param storage pre-initialised with desired value: the caller should not keep a reference
     */
    public PyByteArray(byte[] storage) {
        super(TYPE);
        setStorage(storage);
    }

    /**
     * Constructs a <code>bytearray</code> by re-using an array of byte as storage initialised by
     * the client.
     *
     * @param storage pre-initialised with desired value: the caller should not keep a reference
     * @param size number of bytes actually used
     * @throws IllegalArgumentException if the range [0:size] is not within the array bounds of the
     *             storage.
     */
    public PyByteArray(byte[] storage, int size) {
        super(TYPE);
        setStorage(storage, size);
    }

    /**
     * Constructs a new <code>bytearray</code> object from an arbitrary Python object according to
     * the same rules as apply in Python to the <code>bytearray()</code> constructor:
     * <ul>
     * <li><code>bytearray()</code> Construct a zero-length <code>bytearray</code>.</li>
     * <li><code>bytearray(int)</code> Construct a zero-initialized <code>bytearray</code> of the
     * given length.</li>
     * <li><code>bytearray(iterable_of_ints)</code> Construct from iterable yielding integers in
     * [0..255]</li>
     * <li><code>bytearray(buffer)</code> Construct by reading from any object implementing
     * {@link BufferProtocol}, including <code>str/bytes</code> or another <code>bytearray</code>.</li>
     * </ul>
     * When it is necessary to specify an encoding, as in the Python signature
     * <code>bytearray(string, encoding [, errors])</code>, use the constructor
     * {@link #PyByteArray(PyString, String, String)}. If the <code>PyString</code> is actually a
     * <code>PyUnicode</code>, an encoding must be specified, and using this constructor will throw
     * an exception about that.
     *
     * @param arg primary argument from which value is taken (may be <code>null</code>)
     * @throws PyException {@code TypeError} for non-iterable,
     * @throws PyException {@code ValueError} if iterables do not yield byte [0..255] values.
     */
    public PyByteArray(PyObject arg) throws PyException {
        super(TYPE);
        init(arg);
    }

    /*
     * ============================================================================================
     * Support for the Buffer API
     * ============================================================================================
     *
     * The buffer API allows other classes to access the storage directly.
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
     * resizing</b> the byte array. This prohibition is not only on the consumer of the view but
     * extends to any other operations, such as any kind or insertion or deletion.
     */
    @Override
    public synchronized PyBuffer getBuffer(int flags) {

        // If we have already exported a buffer it may still be available for re-use
        BaseBuffer pybuf = getExistingBuffer(flags);

        if (pybuf == null) {
            // No existing export we can re-use: create a new one
            pybuf = new SimpleWritableBuffer(flags, this, storage, offset, size);
            // Hold a reference for possible re-use
            export = new WeakReference<BaseBuffer>(pybuf);
        }

        return pybuf;
    }

    /**
     * Try to re-use an existing exported buffer, or return <code>null</code> if we can't.
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
     * Test to see if the byte array may be resized and raise a BufferError if not. This must be
     * called by the implementation of any append or insert that changes the number of bytes in the
     * array.
     *
     * @throws PyException {@code BufferError} if there are buffer exports preventing a resize
     */
    protected void resizeCheck() throws PyException {
        if (export != null) {
            // A buffer was exported at some time and we have not explicitly discarded it.
            PyBuffer pybuf = export.get();
            if (pybuf != null && !pybuf.isReleased()) {
                // A consumer still has the exported buffer
                throw Py.BufferError("Existing exports of data: object cannot be re-sized");
            } else {
                /*
                 * Either the reference has expired or all consumers have released it. Either way,
                 * the weak reference is useless now.
                 */
                export = null;
            }
        }
    }

    /*
     * ============================================================================================
     * API for org.python.core.PySequence
     * ============================================================================================
     */

    /**
     * Returns a slice of elements from this sequence as a <code>PyByteArray</code>.
     *
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @return a <code>PyByteArray</code> corresponding the the given range of elements.
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
        // If this were immutable, start==0 and end==size we would return (this).
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
     * Returns a <code>PyByteArray</code> that repeats this sequence the given number of times, as
     * in the implementation of <tt>__mul__</tt> for strings.
     *
     * @param count the number of times to repeat this.
     * @return this byte array repeated count times.
     */
    @Override
    protected synchronized PyByteArray repeat(int count) {
        Builder builder = new Builder(size * (long) count);
        builder.repeat(this, count);
        return getResult(builder);
    }

    /**
     * Replace the contents of this <code>PyByteArray</code> with the given number of repeats of the
     * original contents, as in the implementation of <tt>__mul__</tt> for strings.
     *
     * @param count the number of times to repeat this.
     */
    protected synchronized void irepeat(int count) {
        // There are several special cases
        if (size == 0 || count == 1) {
            // No resize, so no check (consistent with CPython)
            // Value is unchanged.

        } else if (count <= 0) {
            // Treat as if count == 0.
            resizeCheck();
            this.setStorage(emptyStorage);

        } else {
            // Open up space (remembering the original size)
            int orginalSize = size;
            storageExtend(orginalSize * (count - 1));
            if (orginalSize == 1) {
                // Do it efficiently for single bytes
                byte b = storage[offset];
                for (int i = 1, p = offset + 1; i < count; i++) {
                    storage[p++] = b;
                }
            } else {
                // General case
                for (int i = 1, p = offset + orginalSize; i < count; i++, p += orginalSize) {
                    System.arraycopy(storage, offset, storage, p, orginalSize);
                }
            }
        }
    }

    /**
     * Sets the indexed element of the <code>bytearray</code> to the given value. This is an
     * extension point called by PySequence in its implementation of {@link #__setitem__} It is
     * guaranteed by PySequence that the index is within the bounds of the array. Any other clients
     * calling <code>pyset(int)</code> must make the same guarantee.
     *
     * @param index index of the element to set.
     * @param value the value to set this element to.
     * @throws PyException {@code AttributeError} if value cannot be converted to an integer
     * @throws PyException {@code ValueError} if value&lt;0 or value&gt;255
     */
    @Override
    public synchronized void pyset(int index, PyObject value) throws PyException {
        storage[index + offset] = byteCheck(value);
    }

    /**
     * Insert the element (interpreted as a Python byte value) at the given index. Python
     * <code>int</code>, <code>long</code> and <code>str</code> types of length 1 are allowed.
     *
     * @param index to insert at
     * @param element to insert (by value)
     * @throws PyException {@code IndexError} if the index is outside the array bounds
     * @throws PyException {@code ValueError} if element&lt;0 or element&gt;255
     * @throws PyException {@code TypeError} if the subclass is immutable
     */
    @Override
    public synchronized void pyinsert(int index, PyObject element) {
        // Open a space at the right location.
        storageReplace(index, 0, 1);
        storage[offset + index] = byteCheck(element);
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
     * {@link PyObject}s, but acceptable ones are {@link PyInteger}, {@link PyLong} or
     * {@link PyString} of length 1. If any one of them proves unsuitable for assignment to a Python
     * <code>bytearray</code> element, an exception is thrown and this <code>bytearray</code> is
     * unchanged.
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
         * buffer interface".
         *
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

        } else if (setsliceFromBuffer(start, stop, step, value)) {
            // Value supports Jython buffer API. (We're done.)

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
     * zero-filled <code>bytearray</code> of the given length.
     *
     * @see #setslice(int, int, int, PyObject)
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param len number of zeros to insert consistent with the slice assignment
     * @throws PyException {@code SliceSizeError} if the value size is inconsistent with an extended slice
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
     * {@link PyString} that is not a {@link PyUnicode}.
     *
     * @see #setslice(int, int, int, PyObject)
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param value a PyString object consistent with the slice assignment
     * @throws PyException {@code SliceSizeError} if the value size is inconsistent with an extended slice
     * @throws PyException {@code ValueError} if the value is a <code>PyUnicode</code>
     */
    private void setslice(int start, int stop, int step, PyString value) throws PyException {
        if (value instanceof PyUnicode) {
            // Has to be 8-bit PyString
            throw Py.TypeError("can't set bytearray slice from unicode");
        } else {
            // Assignment is from 8-bit data
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
    }

    /**
     * Sets the given range of elements according to Python slice assignment semantics from an
     * object supporting the Jython implementation of PEP 3118.
     *
     * @see #setslice(int, int, int, PyObject)
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param value an object supporting the buffer API consistent with the slice assignment
     * @throws PyException {@code SliceSizeError} if the value size is inconsistent with an extended slice
     */
    private void setslice(int start, int stop, int step, BufferProtocol value) throws PyException, ClassCastException {

        try (PyBuffer view = value.getBuffer(PyBUF.SIMPLE)) {

            int len = view.getLen();

            if (step == 1) {
                // Delete this[start:stop] and open a space of the right size
                storageReplace(start, stop - start, len);
                view.copyTo(storage, start + offset);

            } else {
                // This is an extended slice which means we are replacing elements
                int n = sliceLength(start, stop, step);
                if (n != len) {
                    throw SliceSizeError("bytes", len, n);
                }

                for (int io = start + offset, j = 0; j < n; io += step, j++) {
                    storage[io] = view.byteAt(j);    // Assign this[i] = value[j]
                }
            }
        }
    }

    /**
     * Sets the given range of elements according to Python slice assignment semantics from an
     * object that <b>might</b> support the Jython Buffer API.
     *
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param value an object possibly bearing the Buffer API
     * @return <code>true</code> if the slice was set successfully, <code>false</code> otherwise
     * @throws PyException {@code SliceSizeError} if the value size is inconsistent with an extended slice
     */
    private boolean setsliceFromBuffer(int start, int stop, int step, PyObject value)
            throws PyException {
        if (value instanceof BufferProtocol) {
            try {
                setslice(start, stop, step, (BufferProtocol) value);
                return true;
            } catch (ClassCastException e) { /* fall through to false */ }
        }
        return false;
    }


    /**
     * Sets the given range of elements according to Python slice assignment semantics from a
     * <code>bytearray</code> (or bytes).
     *
     * @see #setslice(int, int, int, PyObject)
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param value a <code>bytearray</code> (or bytes) object consistent with the slice assignment
     * @throws PyException {@code SliceSizeError} if the value size is inconsistent with an extended slice
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
     * <code>bytearray</code> (or bytes).
     *
     * @see #setslice(int, int, int, PyObject)
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param iter iterable source of values to enter in the array
     * @throws PyException {@code SliceSizeError} if the iterable size is inconsistent with an
     *             extended slice
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

    @Override
    protected synchronized void del(int index) {
        storageDelete(index, 1);
    }

    @Override
    protected synchronized void delRange(int start, int stop) {
        storageDelete(start, stop - start);
    }

    @Override
    protected synchronized void delslice(int start, int stop, int step, int n) {
        // This will not be possible if this object has buffer exports
        resizeCheck();

        if (step == 1) {
            // Delete this[start:stop] and close up the space.
            storageDelete(start, n);

        } else if (step == -1) {
            // Also a contiguous case, but start > stop.
            storageDelete(stop + 1, n);

        } else {
            // This is an extended slice. We will be deleting n isolated elements.
            int p, m;

            // We delete by copying from high to low memory, whatever the sign of step.
            if (step > 1) {
                // The lowest numbered element to delete is x[start]
                p = start;
                m = step - 1;
            } else {
                // The lowest numbered element to delete is x[start+(n-1)*step]]
                p = start + (n - 1) * step;
                m = -1 - step;
            }

            // Offset p to be a storage index.
            p += offset;

            // Copy n-1 blocks blocks of m bytes, each time skipping the byte to be deleted.
            for (int i = 1; i < n; i++) {
                // Skip q over the one we are deleting
                int q = p + i;
                // Now copy the m elements that follow.
                for (int j = 0; j < m; j++) {
                    storage[p++] = storage[q++];
                }
            }

            // Close up the gap. Note that for the copy, p was adjusted by the storage offset.
            storageDelete(p - offset, n);
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
     * Initialise a mutable <code>bytearray</code> object from various arguments. This single
     * initialisation must support:
     * <ul>
     * <li><code>bytearray()</code> Construct a zero-length <code>bytearray</code>.</li>
     * <li><code>bytearray(int)</code> Construct a zero-initialized <code>bytearray</code> of the
     * given length.</li>
     * <li><code>bytearray(iterable_of_ints)</code> Construct from iterable yielding integers in
     * [0..255]</li>
     * <li><code>bytearray(buffer)</code> Construct by reading from any object implementing
     * {@link BufferProtocol}, including <code>str/bytes</code> or another <code>bytearray</code>.</li>
     * <li><code>bytearray(string, encoding [, errors])</code> Construct from a
     * <code>str/bytes</code>, decoded using the system default encoding, and encoded to bytes using
     * the specified encoding.</li>
     * <li><code>bytearray(unicode, encoding [, errors])</code> Construct from a
     * <code>unicode</code> string, encoded to bytes using the specified encoding.</li>
     * </ul>
     * Although effectively a constructor, it is possible to call <code>__init__</code> on a 'used'
     * object so the method does not assume any particular prior state.
     *
     * @param args argument array according to Jython conventions
     * @param kwds Keywords according to Jython conventions
     * @throws PyException {@code TypeError} for non-iterable,
     * @throws PyException {@code ValueError} if iterables do not yield byte [0..255] values.
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

    /*
     * ============================================================================================
     * Support for Builder
     * ============================================================================================
     *
     */

    @Override
    protected PyByteArray getResult(Builder b) {
        return new PyByteArray(b.getStorage(), b.getSize());
    }

    /*
     * ============================================================================================
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

/*
 * ============================================================================================
 * Python API for bytearray
 * ============================================================================================
 */

    @Override
    public PyObject __add__(PyObject o) {
        return bytearray___add__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___add___doc)
    final synchronized PyObject bytearray___add__(PyObject o) {
        // Duplicate this buffer, but size it large enough to hold the sum
        byte[] copy = new byte[size + o.__len__()];
        System.arraycopy(storage, offset, copy, 0, size);
        PyByteArray sum = new PyByteArray(copy, size);

        // Concatenate the other buffer
        return sum.bytearray___iadd__(o);
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
     * Equivalent to the standard Python <code>__imul__</code> method, that for a byte array returns
     * a new byte array containing the same thing n times.
     */
    @Override
    public PyObject __imul__(PyObject n) {
        return bytearray___imul__(n);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___imul___doc)
    final PyObject bytearray___imul__(PyObject n) {
        if (!n.isIndex()) {
            return null;
        }
        irepeat(n.asIndex(Py.OverflowError));
        return this;
    }

    /**
     * Equivalent to the standard Python <code>__mul__</code> method, that for a byte array returns
     * a new byte array containing the same thing n times.
     */
    @Override
    public PyObject __mul__(PyObject n) {
        return bytearray___mul__(n);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___mul___doc)
    final PyObject bytearray___mul__(PyObject n) {
        if (!n.isIndex()) {
            return null;
        }
        return repeat(n.asIndex(Py.OverflowError));
    }

    /**
     * Equivalent to the standard Python <code>__rmul__</code> method, that for a byte array returns
     * a new byte array containing the same thing n times.
     */
    @Override
    public PyObject __rmul__(PyObject n) {
        return bytearray___rmul__(n);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bytearray___rmul___doc)
    final PyObject bytearray___rmul__(PyObject n) {
        if (!n.isIndex()) {
            return null;
        }
        return repeat(n.asIndex(Py.OverflowError));
    }

    /**
     * Append a single byte to the end of the array.
     *
     * @param element the byte to append.
     */
    public void append(byte element) {
        // Open a space at the end.
        storageExtend(1);
        storage[offset + size - 1] = element;
    }

    /**
     * Append a single element to the end of the array, equivalent to:
     * <code>s[len(s):len(s)] = o</code>. The argument must be a PyInteger, PyLong or string of
     * length 1.
     *
     * @param element the item to append.
     * @throws PyException {@code ValueError} if element&lt;0 or element&gt;255
     */
    public void append(PyObject element) {
        bytearray_append(element);
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_append_doc)
    final synchronized void bytearray_append(PyObject element) {
        // Insert at the end, checked for type and range
        storageExtend(1);
        storage[offset + size - 1] = byteCheck(element);
    }

    /**
     * Implement to the standard Python __contains__ method, which in turn implements the
     * <code>in</code> operator.
     *
     * @param o the element to search for in this <code>bytearray</code>.
     * @return the result of the search.
     **/
    @Override
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
     * Java API equivalent of Python <code>center(width)</code>: return the bytes centered in an
     * array of length <code>width</code>, padded by spaces. A copy of the original byte array is
     * returned if width is less than <code>this.size()</code>.
     *
     * @param width desired
     * @return new byte array containing result
     */
    public PyByteArray center(int width) {
        return (PyByteArray)basebytes_center(width, " ");
    }

    /**
     * Java API equivalent of Python <code>center(width [, fillchar])</code>: return the bytes
     * centered in an array of length <code>width</code>. Padding is done using the specified
     * fillchar (default is a space). A copy of the original byte array is returned if
     * <code>width</code> is less than <code>this.size()</code>.
     *
     * @param width desired
     * @param fillchar one-byte String to fill with, or <code>null</code> implying space
     * @return new byte array containing the result
     */
    public PyByteArray center(int width, String fillchar) {
        return (PyByteArray)basebytes_center(width, fillchar);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytearray_center_doc)
    final PyByteArray bytearray_center(int width, String fillchar) {
        return (PyByteArray)basebytes_center(width, fillchar);
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
     * <code>true</code> if and only if this <code>bytearray</code> ends with the
     * <code>suffix</code>. <code>suffix</code> can also be a tuple of suffixes to look for.
     *
     * @param suffix byte array to match, or object viewable as such, or a tuple of them
     * @return true if and only if this <code>bytearray</code> ends with the suffix (or one of them)
     */
    public boolean endswith(PyObject suffix) {
        return basebytes_starts_or_endswith(suffix, null, null, true);
    }

    /**
     * Implementation of Python <code>endswith( suffix [, start ] )</code>.
     *
     * When <code>suffix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this <code>bytearray</code> ends with the
     * <code>suffix</code>. <code>suffix</code> can also be a tuple of suffixes to look for. With
     * optional <code>start</code> (which may be <code>null</code> or <code>Py.None</code>), define
     * the effective <code>bytearray</code> to be the slice <code>[start:]</code> of this
     * <code>bytearray</code>.
     *
     * @param suffix byte array to match, or object viewable as such, or a tuple of them
     * @param start of slice in this <code>bytearray</code> to match
     * @return true if and only if this[start:] ends with the suffix (or one of them)
     */
    public boolean endswith(PyObject suffix, PyObject start) {
        return basebytes_starts_or_endswith(suffix, start, null, true);
    }

    /**
     * Implementation of Python <code>endswith( suffix [, start [, end ]] )</code>.
     *
     * When <code>suffix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this <code>bytearray</code> ends with the
     * <code>suffix</code>. <code>suffix</code> can also be a tuple of suffixes to look for. With
     * optional <code>start</code> and <code>end</code> (which may be <code>null</code> or
     * <code>Py.None</code>), define the effective <code>bytearray</code> to be the slice
     * <code>[start:end]</code> of this <code>bytearray</code>.
     *
     * @param suffix byte array to match, or object viewable as such, or a tuple of them
     * @param start of slice in this <code>bytearray</code> to match
     * @param end of slice in this <code>bytearray</code> to match
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
     * Implementation of Python <code>expandtabs()</code>: return a copy of the byte array where all
     * tab characters are replaced by one or more spaces, as {@link #expandtabs(int)} with a tab
     * size of 8 characters.
     *
     * @return copy of this byte array with tabs expanded
     */
    public PyByteArray expandtabs() {
        return (PyByteArray)basebytes_expandtabs(8);
    }

    /**
     * Implementation of Python <code>expandtabs(tabsize)</code>: return a copy of the byte array
     * where all tab characters are replaced by one or more spaces, depending on the current column
     * and the given tab size. The column number is reset to zero after each newline occurring in
     * the array. This treats other non-printing characters or escape sequences as regular
     * characters.
     *
     * @param tabsize number of character positions between tab stops
     * @return copy of this byte array with tabs expanded
     */
    public PyByteArray expandtabs(int tabsize) {
        return (PyByteArray)basebytes_expandtabs(tabsize);
    }

    @ExposedMethod(defaults = "8", doc = BuiltinDocs.bytearray_expandtabs_doc)
    final PyByteArray bytearray_expandtabs(int tabsize) {
        return (PyByteArray)basebytes_expandtabs(tabsize);
    }

    /**
     * Append the elements in the argument sequence to the end of the array, equivalent to:
     * <code>s[len(s):len(s)] = o</code>. The argument must be a subclass of {@link BaseBytes} or an
     * iterable type returning elements compatible with byte assignment.
     *
     * @param o the sequence of items to append to the list.
     */
    public void extend(PyObject o) {
        bytearray_extend(o);
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_extend_doc)
    final synchronized void bytearray_extend(PyObject o) {
        // Raise TypeError if the argument is not iterable
        o.__iter__();
        // Use the general method, assigning to the crack at the end of the array.
        // Note this deals with all legitimate PyObject types including the case o==this.
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

    /**
     * Implementation of Python class method <code>bytearray.fromhex(string)</code>, that returns .
     * a new <code>PyByteArray</code> with a value taken from a string of two-digit hexadecimal
     * numbers. Spaces (but not whitespace in general) are acceptable around the numbers, not
     * within. Non-hexadecimal characters or un-paired hex digits raise a <code>ValueError</code>. *
     * Example:
     *
     * <pre>
     * bytearray.fromhex('B9 01EF') -> * bytearray(b'\xb9\x01\xef')."
     * </pre>
     *
     * @param hex specification of the bytes
     * @throws PyException {@code ValueError} if non-hex characters, or isolated ones, are
     *             encountered
     */
    static PyByteArray fromhex(String hex) throws PyException {
        return bytearray_fromhex(TYPE, hex);
    }

    @ExposedClassMethod(doc = BuiltinDocs.bytearray_fromhex_doc)
    static PyByteArray bytearray_fromhex(PyType type, String hex) {
        // I think type tells us the actual class but we always return exactly a bytearray
        // PyObject ba = type.__call__();
        PyByteArray result = new PyByteArray();
        basebytes_fromhex(result, hex);
        return result;
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray___getitem___doc)
    final synchronized PyObject bytearray___getitem__(PyObject index) {
        // Let the SequenceIndexDelegate take care of it
        return delegator.checkIdxAndGetItem(index);
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
        } else if (setsliceFromBuffer(size, size, 1, o)) {
            // No-op setsliceFromBuffer has already done the work and if it returns true then were done.
            // setsliceFromBuffer will return false for PyUnicode where the buffer cannot be obtained.
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
     * This type is not hashable.
     *
     * @throws PyException {@code TypeError} as this type is not hashable.
     */
    @Override
    public int hashCode() throws PyException {
        return bytearray___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray___hash___doc)
    final int bytearray___hash__() {
        throw Py.TypeError(String.format("unhashable type: '%.200s'", getType().fastGetName()));
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

    //
    // Character class operations
    //

    @ExposedMethod(doc = BuiltinDocs.bytearray_isalnum_doc)
    final boolean bytearray_isalnum() {
        return basebytes_isalnum();
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_isalpha_doc)
    final boolean bytearray_isalpha() {
        return basebytes_isalpha();
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_isdigit_doc)
    final boolean bytearray_isdigit() {
        return basebytes_isdigit();
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_islower_doc)
    final boolean bytearray_islower() {
        return basebytes_islower();
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_isspace_doc)
    final boolean bytearray_isspace() {
        return basebytes_isspace();
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_istitle_doc)
    final boolean bytearray_istitle() {
        return basebytes_istitle();
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_isupper_doc)
    final boolean bytearray_isupper() {
        return basebytes_isupper();
    }

    //
    // Case transformations
    //

    @ExposedMethod(doc = BuiltinDocs.bytearray_capitalize_doc)
    final PyByteArray bytearray_capitalize() {
        return (PyByteArray)basebytes_capitalize();
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_lower_doc)
    final PyByteArray bytearray_lower() {
        return (PyByteArray)basebytes_lower();
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_swapcase_doc)
    final PyByteArray bytearray_swapcase() {
        return (PyByteArray)basebytes_swapcase();
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_title_doc)
    final PyByteArray bytearray_title() {
        return (PyByteArray)basebytes_title();
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_upper_doc)
    final PyByteArray bytearray_upper() {
        return (PyByteArray)basebytes_upper();
    }

    /**
     * Implementation of Python <code>join(iterable)</code>. Return a <code>bytearray</code> which
     * is the concatenation of the byte arrays in the iterable <code>iterable</code>. The separator
     * between elements is the byte array providing this method.
     *
     * @param iterable of byte array objects, or objects viewable as such.
     * @return byte array produced by concatenation.
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
        return super.__len__();
    }

    /**
     * Java API equivalent of Python <code>ljust(width)</code>: return the bytes left justified in
     * an array of length <code>width</code>, padded by spaces. A copy of the original byte array is
     * returned if width is less than <code>this.size()</code>.
     *
     * @param width desired
     * @return new byte array containing result
     */
    public PyByteArray ljust(int width) {
        return (PyByteArray)basebytes_ljust(width, " ");
    }

    /**
     * Java API equivalent of Python <code>ljust(width [, fillchar])</code>: return the bytes
     * left-justified in an array of length <code>width</code>. Padding is done using the specified
     * fillchar (default is a space). A copy of the original byte array is returned if
     * <code>width</code> is less than <code>this.size()</code>.
     *
     * @param width desired
     * @param fillchar one-byte String to fill with, or <code>null</code> implying space
     * @return new byte array containing the result
     */
    public PyByteArray ljust(int width, String fillchar) {
        return (PyByteArray)basebytes_ljust(width, fillchar);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytearray_ljust_doc)
    final PyByteArray bytearray_ljust(int width, String fillchar) {
        // If this was immutable and width<=this.size we could return (this).
        return (PyByteArray)basebytes_ljust(width, fillchar);
    }

    /**
     * Implementation of Python <code>lstrip()</code>. Return a copy of the byte array with the
     * leading whitespace characters removed.
     *
     * @return a byte array containing this value stripped of those bytes
     */
    public PyByteArray lstrip() {
        return bytearray_lstrip(null);
    }

    /**
     * Implementation of Python <code>lstrip(bytes)</code>
     *
     * Return a copy of the byte array with the leading characters removed. The bytes argument is an
     * object specifying the set of characters to be removed. If <code>null</code> or
     * <code>None</code>, the bytes argument defaults to removing whitespace. The bytes argument is
     * not a prefix; rather, all combinations of its values are stripped.
     *
     * @param bytes treated as a set of bytes defining what values to strip
     * @return a byte array containing this value stripped of those bytes (at the left)
     */
    public PyByteArray lstrip(PyObject bytes) {
        return bytearray_lstrip(bytes);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytearray_lstrip_doc)
    final synchronized PyByteArray bytearray_lstrip(PyObject bytes) {
        int left;
        if (bytes == null || bytes == Py.None) {
            // Find left bound of the slice that results from the stripping of whitespace
            left = lstripIndex();
        } else {
            // Find left bound of the slice that results from the stripping of the specified bytes
            ByteSet byteSet = new ByteSet(getViewOrError(bytes));
            left = lstripIndex(byteSet);
        }
        return getslice(left, size);
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_partition_doc)
    final PyTuple bytearray_partition(PyObject sep) {
        return basebytes_partition(sep);
    }

    /**
     * Remove and return the last element in the byte array.
     *
     * @return PyInteger representing the value
     */
    public PyInteger pop() {
        return bytearray_pop(-1);
    }

    /**
     * Remove and return the <code>n</code>th byte element in the array.
     *
     * @param i the index of the byte to remove and return.
     * @return PyInteger representing the value
     */
    public PyInteger pop(int i) {
        return bytearray_pop(i);
    }

    @ExposedMethod(defaults = "-1", doc = BuiltinDocs.bytearray_pop_doc)
    final synchronized PyInteger bytearray_pop(int i) {
        if (size == 0) {
            throw Py.IndexError("pop from empty list");
        } else {
            // Deal with slice interpretation of single index
            if (i < 0) {
                i += size;
            }
            // Use List.remove(int)
            return remove(i);
        }
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
     * @throws PyException ValueError if o not found in <code>bytearray</code>
     */
    public void remove(PyObject o) throws PyException {
        bytearray_remove(o);
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
     * Reverses the contents of the byte array in place. The reverse() methods modify in place for
     * economy of space when reversing a large array. It doesn't return the reversed array to remind
     * you that it works by side effect.
     */
    public void reverse() {
        bytearray_reverse();
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_reverse_doc)
    final synchronized void bytearray_reverse() {
        // In place reverse
        int a = offset, b = offset + size;
        while (--b > a) {
            byte t = storage[b];
            storage[b] = storage[a];
            storage[a++] = t;
        }
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
     * Java API equivalent of Python <code>rjust(width)</code>: return the bytes right justified in
     * an array of length <code>width</code>, padded by spaces. A copy of the original byte array is
     * returned if width is less than <code>this.size()</code>.
     *
     * @param width desired
     * @return new byte array containing result
     */
    public PyByteArray rjust(int width) {
        return (PyByteArray)basebytes_rjust(width, " ");
    }

    /**
     * Java API equivalent of Python <code>rjust(width [, fillchar])</code>: return the bytes
     * right-justified in an array of length <code>width</code>. Padding is done using the specified
     * fillchar (default is a space). A copy of the original byte array is returned if
     * <code>width</code> is less than <code>this.size()</code>.
     *
     * @param width desired
     * @param fillchar one-byte String to fill with, or <code>null</code> implying space
     * @return new byte array containing the result
     */
    public PyByteArray rjust(int width, String fillchar) {
        return (PyByteArray)basebytes_rjust(width, fillchar);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytearray_rjust_doc)
    final PyByteArray bytearray_rjust(int width, String fillchar) {
        return (PyByteArray)basebytes_rjust(width, fillchar);
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

    @ExposedMethod(doc = BuiltinDocs.bytearray_rpartition_doc)
    final PyTuple bytearray_rpartition(PyObject sep) {
        return basebytes_rpartition(sep);
    }

    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.bytearray_rsplit_doc)
    final PyList bytearray_rsplit(PyObject sep, int maxsplit) {
        return basebytes_rsplit(sep, maxsplit);
    }

    /**
     * Implementation of Python <code>rstrip()</code>. Return a copy of the byte array with the
     * trailing whitespace characters removed.
     *
     * @return a byte array containing this value stripped of those bytes (at right)
     */
    public PyByteArray rstrip() {
        return bytearray_rstrip(null);
    }

    /**
     * Implementation of Python <code>rstrip(bytes)</code>
     *
     * Return a copy of the byte array with the trailing characters removed. The bytes argument is
     * an object specifying the set of characters to be removed. If <code>null</code> or
     * <code>None</code>, the bytes argument defaults to removing whitespace. The bytes argument is
     * not a suffix; rather, all combinations of its values are stripped.
     *
     * @param bytes treated as a set of bytes defining what values to strip
     * @return a byte array containing this value stripped of those bytes (at right)
     */
    public PyByteArray rstrip(PyObject bytes) {
        return bytearray_rstrip(bytes);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytearray_rstrip_doc)
    final synchronized PyByteArray bytearray_rstrip(PyObject bytes) {
        int right;
        if (bytes == null || bytes == Py.None) {
            // Find right bound of the slice that results from the stripping of whitespace
            right = rstripIndex();
        } else {
            // Find right bound of the slice that results from the stripping of the specified bytes
            ByteSet byteSet = new ByteSet(getViewOrError(bytes));
            right = rstripIndex(byteSet);
        }
        return getslice(0, right);
    }

    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.bytearray_split_doc)
    final PyList bytearray_split(PyObject sep, int maxsplit) {
        return basebytes_split(sep, maxsplit);
    }

    @ExposedMethod(defaults = "false", doc = BuiltinDocs.bytearray_splitlines_doc)
    final PyList bytearray_splitlines(boolean keepends) {
        return basebytes_splitlines(keepends);
    }

    /**
     * Implementation of Python <code>startswith(prefix)</code>.
     *
     * When <code>prefix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this <code>bytearray</code> starts with the
     * <code>prefix</code>. <code>prefix</code> can also be a tuple of prefixes to look for.
     *
     * @param prefix byte array to match, or object viewable as such, or a tuple of them
     * @return true if and only if this <code>bytearray</code> starts with the prefix (or one of
     *         them)
     */
    public boolean startswith(PyObject prefix) {
        return basebytes_starts_or_endswith(prefix, null, null, false);
    }

    /**
     * Implementation of Python <code>startswith( prefix [, start ] )</code>.
     *
     * When <code>prefix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this <code>bytearray</code> starts with the
     * <code>prefix</code>. <code>prefix</code> can also be a tuple of prefixes to look for. With
     * optional <code>start</code> (which may be <code>null</code> or <code>Py.None</code>), define
     * the effective <code>bytearray</code> to be the slice <code>[start:]</code> of this
     * <code>bytearray</code>.
     *
     * @param prefix byte array to match, or object viewable as such, or a tuple of them
     * @param start of slice in this <code>bytearray</code> to match
     * @return true if and only if this[start:] starts with the prefix (or one of them)
     */
    public boolean startswith(PyObject prefix, PyObject start) {
        return basebytes_starts_or_endswith(prefix, start, null, false);
    }

    /**
     * Implementation of Python <code>startswith( prefix [, start [, end ]] )</code>.
     *
     * When <code>prefix</code> is of a type that may be treated as an array of bytes, return
     * <code>true</code> if and only if this <code>bytearray</code> starts with the
     * <code>prefix</code>. <code>prefix</code> can also be a tuple of prefixes to look for. With
     * optional <code>start</code> and <code>end</code> (which may be <code>null</code> or
     * <code>Py.None</code>), define the effective <code>bytearray</code> to be the slice
     * <code>[start:end]</code> of this <code>bytearray</code>.
     *
     * @param prefix byte array to match, or object viewable as such, or a tuple of them
     * @param start of slice in this <code>bytearray</code> to match
     * @param end of slice in this <code>bytearray</code> to match
     * @return true if and only if this[start:end] starts with the prefix (or one of them)
     */
    public boolean startswith(PyObject prefix, PyObject start, PyObject end) {
        return basebytes_starts_or_endswith(prefix, start, end, false);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.bytearray_startswith_doc)
    final boolean bytearray_startswith(PyObject prefix, PyObject start, PyObject end) {
        return basebytes_starts_or_endswith(prefix, start, end, false);
    }

    /**
     * Implementation of Python <code>strip()</code>. Return a copy of the byte array with the
     * leading and trailing whitespace characters removed.
     *
     * @return a byte array containing this value stripped of those bytes (left and right)
     */
    public PyByteArray strip() {
        return bytearray_strip(null);
    }

    /**
     * Implementation of Python <code>strip(bytes)</code>
     *
     * Return a copy of the byte array with the leading and trailing characters removed. The bytes
     * argument is anbyte arrayt specifying the set of characters to be removed. If
     * <code>null</code> or <code>None</code>, the bytes argument defaults to removing whitespace.
     * The bytes argument is not a prefix or suffix; rather, all combinations of its values are
     * stripped.
     *
     * @param bytes treated as a set of bytes defining what values to strip
     * @return a byte array containing this value stripped of those bytes (left and right)
     */
    public PyByteArray strip(PyObject bytes) {
        return bytearray_strip(bytes);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytearray_strip_doc)
    final synchronized PyByteArray bytearray_strip(PyObject bytes) {
        int left, right;
        if (bytes == null || bytes == Py.None) {
            // Find bounds of the slice that results from the stripping of whitespace
            left = lstripIndex();
            // If we hit the end that time, no need to work backwards
            right = (left == size) ? size : rstripIndex();
        } else {
            // Find bounds of the slice that results from the stripping of the specified bytes
            ByteSet byteSet = new ByteSet(getViewOrError(bytes));
            left = lstripIndex(byteSet);
            // If we hit the end that time, no need to work backwards
            right = (left == size) ? size : rstripIndex(byteSet);
        }
        return getslice(left, right);
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray___setitem___doc)
    final synchronized void bytearray___setitem__(PyObject index, PyObject value) {
        // Let the SequenceIndexDelegate take care of it
        delegator.checkIdxAndSetItem(index, value);
    }

    /**
     * An overriding of the standard Java {@link #toString()} method, returning a printable
     * expression of this byte array in the form <code>bytearray(b'hello')</code>, where in the
     * "inner string", any special characters are escaped to their well-known backslash equivalents
     * or a hexadecimal escape. The built-in function <code>repr()</code> is expected to call this
     * method, and wraps the result in a Python <code>str</code>.
     */
    @Override
    public String toString() {
        return this.asString();
    }

    @Override
    public PyString __repr__(){
       return bytearray___repr__();
    }

    @ExposedMethod(names = {"__repr__"}, doc = BuiltinDocs.bytearray___repr___doc)
    final synchronized PyString bytearray___repr__() {
        return new PyString(basebytes_repr("bytearray(b", ")"));
    }

    /**
     * An overriding of the {@link PyObject#__str__()} method, returning <code>PyString</code>,
     * where in the characters are simply those with a point-codes given in this byte array. The
     * built-in function <code>str()</code> is expected to call this method.
     */
    @Override
    public PyString __str__() {
        return bytearray_str();
    }

    @ExposedMethod(names = {"__str__"}, doc = BuiltinDocs.bytearray___str___doc)
    final PyString bytearray_str() {
        return new PyString(this.asString());
    }

    /**
     * Implementation of Python <code>translate(table).</code>
     *
     * Return a copy of the byte array where all bytes occurring in the optional argument
     * <code>deletechars</code> are removed, and the remaining bytes have been mapped through the
     * given translation table, which must be of length 256.
     *
     * @param table length 256 translation table (of a type that may be regarded as a byte array)
     * @return translated byte array
     */
    public PyByteArray translate(PyObject table) {
        return bytearray_translate(table, null);
    }

    /**
     * Implementation of Python <code>translate(table[, deletechars]).</code>
     *
     * Return a copy of the byte array where all bytes occurring in the optional argument
     * <code>deletechars</code> are removed, and the remaining bytes have been mapped through the
     * given translation table, which must be of length 256.
     *
     * You can use the Python <code>maketrans()</code> helper function in the <code>string</code>
     * module to create a translation table. For string objects, set the table argument to
     * <code>None</code> for translations that only delete characters:
     *
     * @param table length 256 translation table (of a type that may be regarded as a byte array)
     * @param deletechars object that may be regarded as a byte array, defining bytes to delete
     * @return translated byte array
     */
    public PyByteArray translate(PyObject table, PyObject deletechars) {
        return bytearray_translate(table, deletechars);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.bytearray_translate_doc)
    final PyByteArray bytearray_translate(PyObject table, PyObject deletechars) {

        // Work with the translation table (if there is one) as a PyBuffer view.
        try (PyBuffer tab = getTranslationTable(table)) {

            // Accumulate the result here
            PyByteArray result = new PyByteArray();

            // There are 4 cases depending on presence/absence of table and deletechars

            if (deletechars != null) {

                // Work with the deletion characters as a buffer too.
                try (PyBuffer d = getViewOrError(deletechars)) {
                    // Use a ByteSet to express which bytes to delete
                    ByteSet del = new ByteSet(d);
                    int limit = offset + size;
                    if (tab == null) {
                        // No translation table, so we're just copying with omissions
                        for (int i = offset; i < limit; i++) {
                            int b = storage[i] & 0xff;
                            if (!del.contains(b)) {
                                result.append((byte)b);
                            }
                        }
                    } else {
                        // Loop over this byte array and write translated bytes to the result
                        for (int i = offset; i < limit; i++) {
                            int b = storage[i] & 0xff;
                            if (!del.contains(b)) {
                                result.append(tab.byteAt(b));
                            }
                        }
                    }
                }

            } else {
                // No deletion set.
                if (tab == null) {
                    // ... and no translation table either: just copy
                    result.extend(this);
                } else {
                    int limit = offset + size;
                    // Loop over this byte array and write translated bytes to the result
                    for (int i = offset; i < limit; i++) {
                        int b = storage[i] & 0xff;
                        result.append(tab.byteAt(b));
                    }
                }
            }

            return result;
        }
    }

    /**
     * Return a {@link PyBuffer} representing a translation table, or raise an exception if it is
     * the wrong size. The caller is responsible for calling {@link PyBuffer#release()} on any
     * returned buffer.
     *
     * @param table the translation table (or <code>null</code> or {@link PyNone})
     * @return the buffer view of the table or null if there is no table
     * @throws PyException if the table is not exacltly 256 bytes long
     */
    private PyBuffer getTranslationTable(PyObject table) throws PyException {
        PyBuffer tab = null;
        // Normalise the translation table to a View (if there is one).
        if (table != null && table != Py.None) {
            tab = getViewOrError(table);
            if (tab.getLen() != 256) {
                throw Py.ValueError("translation table must be 256 bytes long");
            }
        }
        return tab;
    }

    /**
     * Implementation of Python <code>zfill(width):</code> return the numeric string left filled
     * with zeros in a byte array of length <code>width</code>. A sign prefix is handled correctly
     * if it is in the first byte. A copy of the original is returned if width is less than the
     * current size of the array.
     *
     * @param width desired
     * @return left-filled byte array
     */
    public PyByteArray zfill(int width) {
        return (PyByteArray)basebytes_zfill(width);
    }

    @ExposedMethod(doc = BuiltinDocs.bytearray_zfill_doc)
    final PyByteArray bytearray_zfill(int width) {
        return (PyByteArray)basebytes_zfill(width);
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
    @Override
    protected void newStorage(int needed) {
        if (needed > 0) {
            final int L = recLength(needed);
            try {
                byte[] s = new byte[L]; // guaranteed zero (by JLS 2ed para 4.5.5)
                setStorage(s, needed, (L - needed) / 2);
            } catch (OutOfMemoryError e) {
                throw Py.MemoryError(e.getMessage());
            }
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
     * The effect on this <code>PyByteArray</code> is that:
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

        // This will not be possible if this object has buffer exports
        resizeCheck();

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
     * e.g. repeated prepend or append operations. The effect on this <code>PyByteArray</code> is
     * that:
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
     * e.g. repeated prepend or append operations. The effect on this <code>PyByteArray</code> is
     * that:
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
     * effect on this <code>PyByteArray</code> is that:
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

        // XXX Do a better job here or where called of checking offset+size+e <= storage.length

        if (e == 0) {
            return; // Everything stays where it is.
        }

        // This will not be possible if this object has buffer exports
        resizeCheck();

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
     * and the gap between them eliminated. The effect on this <code>PyByteArray</code> is that:
     *
     * <pre>
     * this.offset = f'
     * this.size = s' = a+b
     * </pre>
     *
     * The method does not implement the Python repertoire of slice indices but avoids indexing
     * outside the <code>bytearray</code> by silently adjusting a to be within it. Negative d is
     * treated as 0 and if d is too large, it is truncated to the array end.
     *
     * @param a index of hole in byte array
     * @param d number to discard (will discard x[a,a+d-1])
     * @param e size of hole to open (will be x[a, a+e-1])
     */
    private void storageDelete(int a, int d) {
        // storageReplace specialised for delete (e=0)

        if (d == 0) {
            return; // Everything stays where it is.
        }

        // This will not be possible if this object has buffer exports
        resizeCheck();

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

}
