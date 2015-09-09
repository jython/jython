package org.python.core;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Base class for Jython <code>bytearray</code> (and <code>bytes</code> in due course) that provides
 * most of the Java API, including Java {@link List} behaviour. Attempts to modify the contents
 * through this API will throw a <code>TypeError</code> if the actual type of the object is not
 * mutable. It is possible for a Java client to treat this class as a
 * <code>List&lt;PyInteger></code>, obtaining equivalent functionality to the Python interface in a
 * Java paradigm.
 * <p>
 * Subclasses must define (from {@link PySequence}):
 * <ul>
 * <li>{@link #getslice(int, int, int)}</li>
 * <li>{@link #repeat(int)}</li>
 * </ul>
 * each returning an appropriate concrete type. Mutable subclasses should override:
 * <ul>
 * <li>{@link #pyset(int, PyObject)}</li>
 * <li>{@link #setslice(int, int, int, PyObject)}</li>
 * <li>{@link #del(int)}</li>
 * <li>{@link #delRange(int, int)}</li>
 * </ul>
 * since the default implementations will otherwise throw an exception.
 * <p>
 * Many of the methods implemented here are inherited or thinly wrapped by {@link PyByteArray},
 * which offers them as Java API, or exposes them as Python methods. These prototype Python methods
 * mostly accept a {@link PyObject} as argument, where you might have expected a <code>byte[]</code>
 * or <code>BaseBytes</code>, in order to accommodate the full range of types accepted by the Python
 * equivalent: usually, any <code>PyObject</code> that implements {@link BufferProtocol}, providing
 * a one-dimensional array of bytes, is an acceptable argument. In the documentation, the reader
 * will often see the terms "byte array" or "object viewable as bytes" instead of
 * <code>BaseBytes</code> when this broader scope is intended.
 * <p>
 * Where the methods return a <code>BaseBytes</code>, this is will normally be an instance of the
 * class of the object on which the method was actually called. For example {@link #capitalize()},
 * defined in <code>BaseBytes</code> to return a BaseBytes, actually returns a {@link PyByteArray}
 * when applied to a <code>bytearray</code>. Or it may be that the method returns a
 * <code>PyList</code> of instances of the target type, for example {@link #rpartition(PyObject)}.
 * This is achieved by the sub-class defining {@link #getslice(int, int, int)} and
 * {@link #getBuilder(int)} to return instances of its own type. See the documentation of particular
 * methods for more information.
 */
@Untraversable
public abstract class BaseBytes extends PySequence implements List<PyInteger> {

    /**
     * Constructs a zero-length <code>BaseBytes</code> of explicitly-specified sub-type.
     *
     * @param type explicit Jython type
     */
    public BaseBytes(PyType type) {
        super(type, null);
        delegator = new IndexDelegate();
        setStorage(emptyStorage);
    }

    /**
     * Constructs a zero-filled array of defined size and type.
     *
     * @param size required
     * @param type explicit Jython type
     */
    public BaseBytes(PyType type, int size) {
        super(type, null);
        delegator = new IndexDelegate();
        newStorage(size);
    }

    /**
     * Constructs a byte array of defined type by copying values from int[].
     *
     * @param type explicit Jython type
     * @param value source of values (and size)
     */
    public BaseBytes(PyType type, int[] value) {
        super(type, null);
        delegator = new IndexDelegate();
        int n = value.length;
        newStorage(n);
        for (int i = offset, j = 0; j < n; i++, j++) {
            storage[i] = byteCheck(value[j]);
        }
    }

    /**
     * Constructs a byte array of defined type by copying character values from a String. These
     * values have to be in the Python byte range 0 to 255.
     *
     * @param type explicit Jython type
     * @param value source of characters
     * @throws PyException if any value[i] > 255
     */
    protected BaseBytes(PyType type, String value) throws PyException {
        super(type, null);
        delegator = new IndexDelegate();
        int n = value.length();
        newStorage(n);
        for (int i = offset, j = 0; j < n; j++) {
            storage[i++] = byteCheck(value.charAt(j));
        }
    }

    /**
     * Helper for constructors and methods that manipulate the storage in mutable subclasses. It
     * also permits shared storage between objects, which in general is unsafe if the storage is
     * subject to modification independent of the object now being created. Immutable types may
     * share storage (safely).
     *
     * @param storage byte array allocated by client
     * @param size number of bytes actually used
     * @param offset index of first byte used
     * @throws IllegalArgumentException if the range [offset:offset+size] is not within the array
     *             bounds of storage or size<0.
     */
    protected void setStorage(byte[] storage, int size, int offset) throws IllegalArgumentException {
        if (size < 0 || offset < 0 || offset + size > storage.length) {
            throw new IllegalArgumentException();
        } else {
            this.storage = storage;
            this.size = size;
            this.offset = offset;
        }
    }

    /**
     * Helper for constructors and methods that manipulate the storage in mutable subclasses in the
     * case where the storage should consist of the first part of the given array.
     *
     * @param storage byte array allocated by client
     * @param size number of bytes actually used
     * @throws IllegalArgumentException if the range [0:size] is not within the array bounds of
     *             storage.
     */
    protected void setStorage(byte[] storage, int size) throws IllegalArgumentException {
        if (size < 0 || size > storage.length) {
            throw new IllegalArgumentException();
        } else {
            this.storage = storage;
            this.size = size;
            this.offset = 0;
        }
    }

    /**
     * Helper for constructors and methods that manipulate the storage in mutable subclasses in the
     * case where the storage should consist of exactly the whole of the given array.
     *
     * @param storage byte array allocated by client
     */
    protected void setStorage(byte[] storage) {
        this.storage = storage;
        this.size = storage.length;
        this.offset = 0;
    }

    /*
     * ============================================================================================
     * Support for construction and initialisation
     * ============================================================================================
     *
     * Methods here help subclasses set the initial state. They are designed with bytearray in mind,
     * but note that from Python 3, bytes() has the same set of calls and behaviours. In Peterson's
     * "sort of backport" to Python 2.x, bytes is effectively an alias for str and it shows.
     */

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from
     * PyObject in subclasses.
     *
     * @see org.python.core.ByteArray#bytearray___init__(PyObject[], String[])
     * @see org.python.core.ByteArray#ByteArray(PyObject)
     * @param arg primary argument from which value is taken
     * @param encoding name of optional encoding (must be a string type)
     * @param errors name of optional errors policy (must be a string type)
     */
    protected void init(PyObject arg) {

        if (arg == null) {
            /*
             * bytearray() Construct a zero-length bytearray.
             */
            setStorage(emptyStorage);

        } else if (arg instanceof PyString) {
            /*
             * bytearray(string) Construct from a text string by default encoding and error policy.
             * Cases where encoding and error policy are specified explicitly are dealt with
             * elsewhere.
             */
            init((PyString)arg, (String)null, (String)null); // Casts select right init()

        } else if (arg.isIndex()) {
            /*
             * bytearray(int) Construct a zero-initialised bytearray of the given length.
             */
            init(arg.asIndex(Py.OverflowError)); // overflow if too big to be Java int

        } else if (arg instanceof BaseBytes) {
            /*
             * bytearray copy of bytearray (or bytes) -- do efficiently
             */
            init((BaseBytes)arg);

        } else if (arg instanceof BufferProtocol) {
            /*
             * bytearray copy of object supporting Jython implementation of PEP 3118
             */
            init((BufferProtocol)arg);

        } else {
            /*
             * The remaining alternative is an iterable returning (hopefully) right-sized ints. If
             * it isn't one, we get an exception about not being iterable, or about the values.
             */
            init(arg.asIterable());

        }
    }

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from a
     * text string with the specified encoding in subclasses.
     *
     * @see #bytearray___init__(PyObject[], String[])
     * @see PyByteArray#PyByteArray(PyString, String, String)
     * @param arg primary argument from which value is taken
     * @param encoding name of optional encoding (must be a string type)
     * @param errors name of optional errors policy (must be a string type)
     */
    protected void init(PyString arg, PyObject encoding, PyObject errors) {
        String enc = encoding == null ? null : encoding.asString();
        String err = errors == null ? null : errors.asString();
        init(arg, enc, err);
    }

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from a
     * text string with the specified encoding in subclasses.
     *
     * @see #bytearray___init__(PyObject[], String[])
     * @see PyByteArray#PyByteArray(PyString, String, String)
     * @param arg primary argument from which value is taken
     * @param encoding name of optional encoding
     * @param errors name of optional errors policy
     */
    protected void init(PyString arg, String encoding, String errors) {
        // Jython encode emits a String (not byte[])
        String encoded = encode(arg, encoding, errors);
        newStorage(encoded.length());
        setBytes(0, encoded);
    }

    /**
     * Helper for {@link #setslice(int, int, int, PyObject)}, for <code>__new__</code> and
     * <code>__init__</code> and the Java API constructor from a text string with the specified
     * encoding in subclasses. This method thinly wraps a call to the codecs module and deals with
     * checking for PyUnicode (where the encoding argument is mandatory).
     *
     * @see #ByteArray(PyString, String, String)
     * @param arg primary argument from which value is taken
     * @param encoding name of optional encoding
     * @param errors name of optional errors policy
     * @return encoded string
     * @throws PyException (TypeError) if the <code>PyString</code> is actually a {@link PyUnicode}
     *             and encoding is <code>null</code>
     */
    protected static String encode(PyString arg, String encoding, String errors) throws PyException {
        // Jython encode emits a String (not byte[])
        String encoded;

        if (arg instanceof PyUnicode) {
            if (encoding != null) {
                encoded = codecs.encode(arg, encoding, errors);
            } else {
                throw Py.TypeError("unicode argument without an encoding");
            }
        } else {
            if (encoding != null) {
                encoded = codecs.encode(arg, encoding, errors);
            } else {
                encoded = arg.getString();
            }
        }
        return encoded;
    }

    /**
     * Fill a defined section of a byte array by copying character values from a String. These
     * values have to be in the Python byte range 0 to 255.
     *
     * @param start index in this byte array at which the first character code lands
     * @param value source of characters
     * @throws PyException (ValueError) if any value[i] > 255
     */
    protected void setBytes(int start, String value) throws PyException {
        int n = value.length();
        int io = offset + start;
        for (int j = 0; j < n; j++) {
            storage[io++] = byteCheck(value.charAt(j));
        }
    }

    /**
     * Fill a strided slice of a byte array by copying character values from a String. These values
     * have to be in the Python byte range 0 to 255.
     *
     * @param start index in this byte array at which the first character code lands
     * @param value source of characters
     * @throws PyException (ValueError) if any value[i] > 255
     */
    protected void setBytes(int start, int step, String value) throws PyException {
        int n = value.length();
        int io = offset + start;
        for (int j = 0; j < n; j++) {
            storage[io] = byteCheck(value.charAt(j));
            io += step;
        }
    }

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from
     * int in subclasses. Construct zero-filled byte array of specified size.
     *
     * @param n size of zero-filled array
     */
    protected void init(int n) {
        if (n < 0) {
            throw Py.ValueError("negative count");
        }
        newStorage(n);
    }

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from
     * objects supporting the Jython implementation of PEP 3118 (Buffer API) in subclasses.
     *
     * @param value an object bearing the Buffer API and consistent with the slice assignment
     */
    protected void init(BufferProtocol value) throws PyException {
        // Get the buffer view
        try (PyBuffer view = value.getBuffer(PyBUF.FULL_RO)) {
            // Create storage for the bytes and have the view drop them in
            newStorage(view.getLen());
            view.copyTo(storage, offset);
        }
    }

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from
     * <code>bytearray</code> or <code>bytes</code> in subclasses.
     *
     * @param source <code>bytearray</code> (or <code>bytes</code>) to copy
     */
    protected void init(BaseBytes source) {
        newStorage(source.size);
        System.arraycopy(source.storage, source.offset, storage, offset, size);
    }

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from
     * an arbitrary iterable Python type in subclasses. This will include generators and lists.
     *
     * @param iter iterable source of values to enter in the array
     */
    protected void init(Iterable<? extends PyObject> iter) {
        /*
         * Different strategy is needed from that suited to "random append" operations. We shall
         * have a stream of append operations, and it might be long.
         */
        FragmentList fragList = new FragmentList();
        fragList.loadFrom(iter);

        // Now, aggregate all those fragments.
        //
        if (fragList.totalCount > 0) {

            if (fragList.size() == 1) {
                // Note that the first fragment is small: negligible waste if stolen directly.
                Fragment frag = fragList.getFirst();
                setStorage(frag.storage, frag.count);

            } else {
                // Stitch the fragments together in new storage of sufficient size
                newStorage(fragList.totalCount);
                fragList.emptyInto(storage, offset);
            }

        } else {
            // Nothing in the iterator
            setStorage(emptyStorage);
        }
    }

    /**
     * Intended as a fragment of temporary storage for use we do not know how many bytes of
     * allocate, and we are reading in some kind of iterable stream.
     */
    protected static class Fragment {

        static final int MINSIZE = 8;
        static final int MAXSIZE = 1024;

        byte[] storage;
        int count = 0;

        Fragment(int size) {
            storage = new byte[size];
        }

        // Convert to byte and add to buffer
        boolean isFilledBy(PyObject value) {
            storage[count++] = byteCheck(value);
            return count == storage.length;
        }
    }

    /**
     * A container of temporary storage when we do not know how many bytes to allocate, and we are
     * reading in some kind of iterable stream.
     */
    protected static class FragmentList extends LinkedList<Fragment> {

        /**
         * Total number of bytes being held.
         */
        int totalCount = 0;

        /**
         * Load bytes into the container from the given iterable
         *
         * @param iter iterable source of values to enter into the container
         * @throws PyException (TypeError) if any value not acceptable type
         * @throws PyException (ValueError) if any value<0 or value>255 or string length!=1
         */
        void loadFrom(Iterable<? extends PyObject> iter) throws PyException {

            int fragSize = Fragment.MINSIZE;
            Fragment curr = null;

            // Allocate series of fragments as needed, while the iterator runs to completion
            try {
                for (PyObject value : iter) {
                    if (curr == null) {
                        // Need a new Fragment
                        curr = new Fragment(fragSize);
                        add(curr);
                        if (fragSize < Fragment.MAXSIZE) {
                            fragSize <<= 1;
                        }
                    }
                    // Insert next item from iterator.
                    if (curr.isFilledBy(value)) {
                        // Fragment is now full: signal a new one will be needed
                        totalCount += curr.count;
                        curr = null;
                    }
                }
            } catch (OutOfMemoryError e) {
                throw Py.MemoryError(e.getMessage());
            }

            // Don't forget the bytes in the final Fragment
            if (curr != null) {
                totalCount += curr.count;
            }
        }

        /**
         * Move the contents of this container to the given byte array at the specified index. This
         * method leaves this container empty.
         *
         * @param target destination array
         * @param p position to write first byte
         */
        void emptyInto(byte[] target, int p) {

            for (Fragment frag : this) {
                System.arraycopy(frag.storage, 0, target, p, frag.count);
                p += frag.count;
            }
            clear(); // Encourage recycling
            totalCount = 0;
        }

        /**
         * Move the contents of this container to a strided subset of the given byte array at the
         * specified index. Bytes are assigned at start, start+step, start+2*step, and so on until
         * we run out. (You must have checked beforehand that the destination is big enough.) This
         * method leaves this container empty. If the step size is one, it would be much quicker to
         * call {@link BaseBytes#emptyInto(byte[], int)}
         *
         * @param target destination array
         * @param start position to write first byte
         * @param step amount to advance index with each byte
         */
        void emptyInto(byte[] target, int start, int step) {
            int p = start;
            for (Fragment frag : this) {
                for (int i = 0; i < frag.count; i++) {
                    target[p] = frag.storage[i];
                    p += step;
                }
            }
            clear(); // Encourage recycling
            totalCount = 0;
        }

    }

    /*
     * ============================================================================================
     * Sharable storage
     * ============================================================================================
     *
     * The storage is provided by a byte array that may be somewhat larger than the number of bytes
     * actually stored, and these bytes may begin at an offset within the storage. Immutable
     * subclasses of BaseBytes may exploit this to share storage when constructed from a slice of
     * another immutable subclass. Mutable subclasses may exploit it to provide efficient insertions
     * near the start of the array.
     */

    /** Empty storage constant */
    protected static final byte[] emptyStorage = new byte[0];

    /** Storage array. */
    protected byte[] storage = emptyStorage;

    /** Number of bytes actually used in storage array. */
    protected int size = 0;

    /** Index of first byte used in storage array. */
    protected int offset = 0;

    /**
     * Check that an index is within the range of the array, that is <tt>0&lt;=index&lt;size</tt>.
     *
     * @param index to check
     * @throws PyException (IndexError) if the index is outside the array bounds
     */
    protected final void indexCheck(int index) throws PyException {
        if (index < 0 || index >= size) {
            throw Py.IndexError(getType().fastGetName() + " index out of range");
        }
    }

    /**
     * Allocate fresh, zero-filled storage for the requested number of bytes and make that the size.
     * If the size needed is zero, the "storage" allocated is the shared emptyStorage array. The
     * allocated size may be bigger than needed, and the method chooses a value for offset.
     *
     * @param needed becomes the new value of this.size
     */
    protected void newStorage(int needed) {
        // The implementation for immutable arrays allocates only as many bytes as needed.
        if (needed > 0) {
            try {
                setStorage(new byte[needed]); // guaranteed zero (by JLS 2ed para 4.5.5)
            } catch (OutOfMemoryError e) {
                throw Py.MemoryError(e.getMessage());
            }
        } else {
            setStorage(emptyStorage);
        }
    }

    /**
     * Check that an integer is suitable for storage in a (Python) byte array, and convert it to the
     * Java byte value that can be stored there. (Java bytes run -128..127 whereas Python bytes run
     * 0..255.)
     *
     * @param value to convert.
     * @throws PyException (ValueError) if value<0 or value>255
     */
    protected static final byte byteCheck(int value) throws PyException {
        if (value < 0 || value > 255) {
            throw Py.ValueError("byte must be in range(0, 256)");
        }
        return (byte)value;
    }

    /**
     * Check that the value of an PyInteger is suitable for storage in a (Python) byte array, and
     * convert it to the Java byte value that can be stored there. (Java bytes run -128..127 whereas
     * Python bytes run 0..255.)
     *
     * @param value to convert.
     * @throws PyException (ValueError) if value<0 or value>255
     */
    protected static final byte byteCheck(PyInteger value) throws PyException {
        return byteCheck(value.asInt());
    }

    /**
     * Check that the type and value of a PyObject is suitable for storage in a (Python) byte array,
     * and convert it to the Java byte value that can be stored there. (Java bytes run -128..127
     * whereas Python bytes run 0..255.) Acceptable types are:
     * <ul>
     * <li>PyInteger in range 0 to 255 inclusive</li>
     * <li>PyLong in range 0 to 255 inclusive</li>
     * <li>Any type having an __index__() method, in range 0 to 255 inclusive</li>
     * <li>PyString of length 1</li>
     * </ul>
     *
     * @param value to convert.
     * @throws PyException (TypeError) if not acceptable type
     * @throws PyException (ValueError) if value<0 or value>255 or string length!=1
     */
    protected static final byte byteCheck(PyObject value) throws PyException {
        if (value.isIndex()) {
            // This will possibly produce Py.OverflowError("long int too large to convert")
            return byteCheck(value.asIndex());
        } else if (value.getType() == PyString.TYPE) {
            // Exactly PyString (not PyUnicode)
            String strValue = ((PyString)value).getString();
            if (strValue.length() != 1) {
                throw Py.ValueError("string must be of size 1");
            }
            return byteCheck(strValue.charAt(0));
        } else {
            throw Py.TypeError("an integer or string of size 1 is required");
        }
    }

    /**
     * Return a buffer exported by the argument, or return <code>null</code> if it does not bear the
     * buffer API. The caller is responsible for calling {@link PyBuffer#release()} on the buffer,
     * if the return value is not <code>null</code>.
     *
     * @param b object to wrap
     * @return byte-oriented view or <code>null</code>
     */
    protected static PyBuffer getView(PyObject b) {

        if (b == null) {
            return null;

        } else if (b instanceof PyUnicode) {
            /*
             * PyUnicode has the BufferProtocol interface as it extends PyString. (It would bring
             * you 0xff&charAt(i) in practice.) However, in CPython the unicode string does not have
             * the buffer API.
             */
            return null;

        } else if (b instanceof BufferProtocol) {
            return ((BufferProtocol)b).getBuffer(PyBUF.FULL_RO);

        } else {
            return null;
        }
    }

    /**
     * Return a buffer exported by the argument or raise an exception if it does not bear the buffer
     * API. The caller is responsible for calling {@link PyBuffer#release()} on the buffer. The
     * return value is never <code>null</code>.
     *
     * @param b object to wrap
     * @return byte-oriented view
     */
    protected static PyBuffer getViewOrError(PyObject b) {
        PyBuffer buffer = getView(b);
        if (buffer != null) {
            return buffer;
        } else {
            String fmt = "Type %s doesn't support the buffer API";
            throw Py.TypeError(String.format(fmt, b.getType().fastGetName()));
        }
    }

    /*
     * ============================================================================================
     * API for org.python.core.PySequence
     * ============================================================================================
     */
    @Override
    protected PyInteger pyget(int index) {
        return new PyInteger(intAt(index));
    }

    /*
     * We're not implementing these here, but we can give a stronger guarantee about the return type
     * and save some casting and type anxiety.
     */
    @Override
    protected abstract BaseBytes getslice(int start, int stop, int step);

    @Override
    protected abstract BaseBytes repeat(int count);

    /*
     * And this extension point should be overridden in mutable subclasses
     */

    /**
     * Insert the element (interpreted as a Python byte value) at the given index. The default
     * implementation produces a Python TypeError, for the benefit of immutable types. Mutable types
     * must override it.
     *
     * @param index to insert at
     * @param element to insert (by value)
     * @throws PyException (IndexError) if the index is outside the array bounds
     * @throws PyException (ValueError) if element<0 or element>255
     * @throws PyException (TypeError) if the subclass is immutable
     */
    public void pyinsert(int index, PyObject element) {
        // This won't succeed: it just produces the right error.
        // storageReplace(index, 0, 1);
        pyset(index, element);
    }

    /**
     * Specialisation of {@link #getslice(int, int, int)} to contiguous slices (of step size 1) for
     * brevity and efficiency. The default implementation is <code>getslice(start, stop, 1)</code>
     * but it is worth overriding.
     *
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @return a subclass instance of BaseBytes corresponding the the given range of elements.
     */
    protected BaseBytes getslice(int start, int stop) {
        return getslice(start, stop, 1);
    }

    /**
     * Class defining the behaviour of <code>bytearray</code> with respect to slice assignment,
     * etc., which differs from the default (list) behaviour in small ways.
     */
    private class IndexDelegate extends PySequence.DefaultIndexDelegate {

        /**
         * bytearray treats assignment of a zero-length object to a slice as equivalent to deletion,
         * unlike list, even for an extended slice.
         */
        @Override
        public void checkIdxAndSetSlice(PySlice slice, PyObject value) {
            if (value.__len__() != 0) {
                // Proceed as default
                super.checkIdxAndSetSlice(slice, value);
            } else {
                // Treat as deletion
                checkIdxAndDelItem(slice);
            }
        }

        @Override
        protected void delSlice(int[] indices) {
            delslice(indices[0], indices[1], indices[2], indices[3]);
        }
    };

    /*
     * ============================================================================================
     * Support for Python API common to mutable and immutable subclasses
     * ============================================================================================
     */

    @Override
    public int __len__() {
        return size;
    }

    /**
     * Comparison function between a byte array and a buffer of bytes exported by some other object,
     * such as a String, presented as a <code>PyBuffer</code>, returning 1, 0 or -1 as a>b, a==b, or
     * a&lt;b respectively. The comparison is by value, using Python unsigned byte conventions,
     * left-to-right (low to high index). Zero bytes are significant, even at the end of the array:
     * <code>[65,66,67]&lt;"ABC\u0000"</code>, for example and <code>[]</code> is less than every
     * non-empty b, while <code>[]==""</code>.
     *
     * @param a left-hand array in the comparison
     * @param b right-hand wrapped object in the comparison
     * @return 1, 0 or -1 as a>b, a==b, or a&lt;b respectively
     */
    private static int compare(BaseBytes a, PyBuffer b) {

        // Compare elements one by one in these ranges:
        int ap = a.offset;
        int aEnd = ap + a.size;
        int bp = 0;
        int bEnd = b.getLen();

        while (ap < aEnd) {
            if (bp >= bEnd) {
                // a is longer than b
                return 1;
            } else {
                // Compare the corresponding bytes
                int aVal = 0xff & a.storage[ap++];
                int bVal = b.intAt(bp++);
                int diff = aVal - bVal;
                if (diff != 0) {
                    return (diff < 0) ? -1 : 1;
                }
            }
        }

        // All the bytes matched and we reached the end of a
        if (bp < bEnd) {
            // But we didn't reach the end of b
            return -1;
        } else {
            // And the end of b at the same time, so they're equal
            return 0;
        }

    }

    /**
     * Comparison function between byte array types and any other object. The six "rich comparison"
     * operators are based on this.
     *
     * @param b
     * @return 1, 0 or -1 as this>b, this==b, or this&lt;b respectively, or -2 if the comparison is
     *         not implemented
     */
    private synchronized int basebytes_cmp(PyObject b) {

        // This is not exposed as bytearray and bytes have no __cmp__.

        if (this == b) {
            // Same object: quick result
            return 0;

        } else {
            // Try to get a byte-oriented view
            try (PyBuffer bv = getView(b)) {

                if (bv == null) {
                    // Signifies a type mis-match. See PyObject._cmp_unsafe() and related code.
                    return -2;

                } else {
                    // Compare this with other object viewed as a buffer
                    return compare(this, bv);
                }
            }
        }
    }

    /**
     * Fail-fast comparison function between byte array types and any other object, for when the
     * test is only for equality. The "rich comparison" operators <code>__eq__</code> and
     * <code>__ne__</code> are based on this.
     *
     * @param b
     * @return 0 if this==b, or +1 or -1 if this!=b, or -2 if the comparison is not implemented
     */
    private synchronized int basebytes_cmpeq(PyObject b) {

        if (this == b) {
            // Same object: quick result
            return 0;

        } else {

            // Try to get a byte-oriented view
            try (PyBuffer bv = getView(b)) {

                if (bv == null) {
                    // Signifies a type mis-match. See PyObject._cmp_unsafe() and related code.
                    return -2;

                } else {
                    if (bv.getLen() != size) {
                        // Different size: can't be equal, and we don't care which is bigger
                        return 1;
                    } else {
                        // Compare this with other object viewed as a buffer
                        return compare(this, bv);
                    }
                }
            }
        }
    }

    /**
     * Implementation of __eq__ (equality) operator, capable of comparison with another byte array.
     * Comparison with an invalid type returns <code>null</code>.
     *
     * @param other Python object to compare with
     * @return Python boolean result or <code>null</code> if not implemented for the other type.
     */
    final PyObject basebytes___eq__(PyObject other) {
        int cmp = basebytes_cmpeq(other);
        if (cmp == 0) {
            return Py.True;
        } else if (cmp > -2) {
            return Py.False;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __ne__ (not equals) operator, capable of comparison with another byte
     * array. Comparison with an invalid type returns <code>null</code>.
     *
     * @param other Python object to compare with
     * @return Python boolean result or <code>null</code> if not implemented for the other type.
     */
    final PyObject basebytes___ne__(PyObject other) {
        int cmp = basebytes_cmpeq(other);
        if (cmp == 0) {
            return Py.False;
        } else if (cmp > -2) {
            return Py.True;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __lt__ (less than) operator, capable of comparison with another byte array.
     * Comparison with an invalid type returns <code>null</code>.
     *
     * @param other Python object to compare with
     * @return Python boolean result or <code>null</code> if not implemented for the other type.
     */
    final PyObject basebytes___lt__(PyObject other) {
        int cmp = basebytes_cmp(other);
        if (cmp >= 0) {
            return Py.False;
        } else if (cmp > -2) {
            return Py.True;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __le__ (less than or equal to) operator, capable of comparison with another
     * byte array. Comparison with an invalid type returns <code>null</code>.
     *
     * @param other Python object to compare with
     * @return Python boolean result or <code>null</code> if not implemented for the other type.
     */
    final PyObject basebytes___le__(PyObject other) {
        int cmp = basebytes_cmp(other);
        if (cmp > 0) {
            return Py.False;
        } else if (cmp > -2) {
            return Py.True;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __ge__ (greater than or equal to) operator, capable of comparison with
     * another byte array. Comparison with an invalid type returns <code>null</code>.
     *
     * @param other Python object to compare with
     * @return Python boolean result or <code>null</code> if not implemented for the other type.
     */
    final PyObject basebytes___ge__(PyObject other) {
        int cmp = basebytes_cmp(other);
        if (cmp >= 0) {
            return Py.True;
        } else if (cmp > -2) {
            return Py.False;
        } else {
            return null;
        }
    }

    /**
     * Implementation of __gt__ (greater than) operator, capable of comparison with another byte
     * array. Comparison with an invalid type returns <code>null</code>.
     *
     * @param other Python object to compare with
     * @return Python boolean result or <code>null</code> if not implemented for the other type.
     */
    final PyObject basebytes___gt__(PyObject other) {
        int cmp = basebytes_cmp(other);
        if (cmp > 0) {
            return Py.True;
        } else if (cmp > -2) {
            return Py.False;
        } else {
            return null;
        }
    }

    /**
     * Search for the target in this byte array, returning true if found and false if not. The
     * target must either convertible to an integer in the Python byte range, or capable of being
     * viewed as a byte array.
     *
     * @param target byte value to search for
     * @return true iff found
     */
    protected final synchronized boolean basebytes___contains__(PyObject target) {
        if (target.isIndex()) {
            // Caller is treating this as an array of integers, so the value has to be in range.
            byte b = byteCheck(target.asIndex());
            return index(b) >= 0;
        } else {
            // Caller is treating this as a byte-string and looking for substring 'target'
            try (PyBuffer targetView = getViewOrError(target)) {
                Finder finder = new Finder(targetView);
                finder.setText(this);
                return finder.nextIndex() >= 0;
            }
        }
    }

    /**
     * Almost ready-to-expose implementation serving both Python
     * <code>startswith( prefix [, start [, end ]] )</code> and
     * <code>endswith( suffix [, start [, end ]] )</code>. An extra boolean argument specifies which
     * to implement on a given call, that is, whether the target is a suffix or prefix. The target
     * may also be a tuple of targets.
     *
     * @param target prefix or suffix sequence to find (of a type viewable as a byte sequence) or a
     *            tuple of those.
     * @param ostart of slice to search.
     * @param oend of slice to search.
     * @param endswith true if we are doing endswith, false if startswith.
     * @return true if and only if this byte array ends with (one of) <code>target</code>.
     */
    protected final synchronized boolean basebytes_starts_or_endswith(PyObject target,
            PyObject ostart, PyObject oend, boolean endswith) {
        /*
         * This cheap 'endswith' trick saves us from maintaining two almost identical methods and
         * mirrors CPython's _bytearray_tailmatch().
         */
        int[] index = indicesEx(ostart, oend);  // [ start, end, 1, end-start ]

        if (target instanceof PyTuple) {
            // target is a tuple of suffixes/prefixes and only one need match
            for (PyObject t : ((PyTuple)target).getList()) {
                if (match(t, index[0], index[3], endswith)) {
                    return true;
                }
            }
            return false; // None of them matched

        } else {
            return match(target, index[0], index[3], endswith);
        }
    }

    /**
     * Test whether the slice <code>[pos:pos+n]</code> of this byte array matches the given target
     * object (accessed as a {@link PyBuffer}) at one end or the orher. That is, if
     * <code>endswith==false</code> test whether the bytes from index <code>pos</code> match all the
     * bytes of the target; if <code>endswith==false</code> test whether the bytes up to index
     * <code>pos+n-1</code> match all the bytes of the target. By implication, the test returns
     * false if the target is bigger than n. The caller guarantees that the slice
     * <code>[pos:pos+n]</code> is within the byte array.
     *
     * @param target pattern to match
     * @param pos at which to start the comparison
     * @return true if and only if the slice [offset:<code>]</code> matches the given target
     */
    private boolean match(PyObject target, int pos, int n, boolean endswith) {

        // Error if not something we can treat as a view of bytes
        try (PyBuffer vt = getViewOrError(target)) {
            int j = 0, len = vt.getLen();

            if (!endswith) {
                // Match is at the start of the range [pos:pos+n]
                if (len > n) {
                    return false;
                }
            } else {
                // Match is at the end of the range [pos:pos+n]
                j = n - len;
                if (j < 0) {
                    return false;
                }
            }

            // Last resort: we have actually to look at the bytes!
            j += offset + pos;
            for (int i = 0; i < len; i++) {
                if (storage[j++] != vt.byteAt(i)) {
                    return false;
                }
            }
            return true; // They must all have matched
        }
    }

    /**
     * Helper to convert [ostart:oend] to integers with slice semantics relative to this byte array.
     * The retruned array of ints contains [ start, end, 1, end-start ].
     *
     * @param ostart of slice to define.
     * @param oend of slice to define.
     * @return [ start, end, 1, end-start ]
     */
    private int[] indicesEx(PyObject ostart, PyObject oend) {
        // Convert [ostart:oend] to integers with slice semantics relative to this byte array
        PySlice s = new PySlice(ostart, oend, null);
        return s.indicesEx(size);  // [ start, end, 1, end-start ]
    }

    /**
     * Present the bytes of a byte array, with no decoding, as a Java String. The bytes are treated
     * as unsigned character codes, and copied to the to the characters of a String with no change
     * in ordinal value. This could also be described as 'latin-1' or 'ISO-8859-1' decoding of the
     * byte array to a String, since this character encoding is numerically equal to Unicode.
     *
     * @return the byte array as a String
     */
    @Override
    public synchronized String asString() {
        char[] buf = new char[size];
        int j = offset + size;
        for (int i = size; --i >= 0;) {
            buf[i] = (char)(0xff & storage[--j]);
        }
        return new String(buf);
    }

    /**
     * Decode the byte array to a Unicode string according to the default encoding. The returned
     * PyObject should be a <code>PyUnicode</code>, since the default codec is well-behaved.
     *
     * @return object containing the decoded characters
     */
    public PyObject decode() {
        return decode(null, null);
    }

    /**
     * Decode the byte array to a Unicode string according to the specified encoding and default
     * error policy. The returned PyObject will usually be a <code>PyUnicode</code>, but in practice
     * it is whatever the <code>decode</code> method of the codec decides.
     *
     * @param encoding the name of the codec (uses default codec if <code>null</code>)
     * @return object containing the decoded characters
     */
    public PyObject decode(String encoding) {
        return decode(encoding, null);
    }

    /**
     * Decode the byte array to a Unicode string according to the specified encoding and error
     * policy. The returned PyObject will usually be a <code>PyUnicode</code>, but in practice it is
     * whatever the <code>decode</code> method of the codec decides.
     *
     * @param encoding the name of the codec (uses default codec if <code>null</code>)
     * @param errors the name of the error policy (uses 'strict' if <code>null</code>)
     * @return object containing the decoded characters
     */
    public PyObject decode(String encoding, String errors) {
        /*
         * Provide a Python <code>str</code> input to the decode method of a codec, which in v2.7
         * expects a PyString. (In Python 3k the codecs decode from the <code>bytes</code> type, so
         * we can pass this directly.)
         */
        PyString this_ = new PyString(this.asString());
        return codecs.decode(this_, encoding, errors);
    }

    /**
     * Ready-to-expose implementation of decode( [ encoding [, errors ]] )
     *
     * @param args Python argument list
     * @param keywords Assocaited keywords
     * @return
     */
    protected final PyObject basebytes_decode(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("decode", args, keywords, "encoding", "errors");
        String encoding = ap.getString(0, null);
        String errors = ap.getString(1, null);
        return decode(encoding, errors);
    }

    /**
     * Convenience method to create a <code>TypeError</code> PyException with the message
     * "can't concat {type} to {toType}"
     *
     * @param type
     * @param toType
     * @return PyException (TypeError) as detailed
     */
    static PyException ConcatenationTypeError(PyType type, PyType toType) {
        String fmt = "can't concat %s to %s";
        return Py.TypeError(String.format(fmt, type.fastGetName(), toType.fastGetName()));
    }

    /**
     * Support for pickling byte arrays: reduce a byte array to the actual type, arguments for
     * (re-)construction of the object, and the dictionary of any user-defined sub-class.
     *
     * @return PyTuple that is first stage in pickling byte array
     */
    @Override
    public PyObject __reduce__() {
        return basebytes___reduce__();
    }

    /**
     * Ready-to-expose implementation of Python __reduce__() method used in pickle (persistence) of
     * Python objects.
     *
     * @return required tuple of type, arguments needed by init, and any user-added attributes.
     */
    final PyTuple basebytes___reduce__() {
        PyUnicode encoded = new PyUnicode(this.asString());
        PyObject args = new PyTuple(encoded, getPickleEncoding());
        PyObject dict = __findattr__("__dict__");
        return new PyTuple(getType(), args, (dict != null) ? dict : Py.None);
    }

    private static PyString PICKLE_ENCODING;

    /**
     * Name the encoding effectively used in __reduce__() suport for pickling: this choice is
     * hard-coded in CPython as "latin-1".
     */
    private static final PyString getPickleEncoding() {
        if (PICKLE_ENCODING == null) {
            PICKLE_ENCODING = new PyString("latin-1");
        }
        return PICKLE_ENCODING;
    }

    /*
     * ============================================================================================
     * Python API for find and replace operations
     * ============================================================================================
     *
     * A large part of the CPython bytearray.c is devoted to replace( old, new [, count ] ). The
     * special section here reproduces that in Java, but whereas CPython makes heavy use of the
     * buffer API and C memcpy(), we use PyBuffer.copyTo. The logic is much the same, however, even
     * down to variable names.
     */

    /**
     * The very simplest kind of find operation: return the index in the byte array of the first
     * occurrence of the byte value
     *
     * @param b byte to search for
     * @return index in the byte array (0..size-1) or -1 if not found
     */
    protected int index(byte b) {
        int limit = offset + size;
        for (int p = offset; p < limit; p++) {
            if (storage[p] == b) {
                return p - offset;
            }
        }
        return -1;
    }

    /**
     * This class implements the Boyer-Moore-Horspool Algorithm for find a pattern in text, applied
     * to byte arrays. The BMH algorithm uses a table of bad-character skips derived from the
     * pattern. The bad-character skips table tells us how far from the end of the pattern is a byte
     * that might match the text byte currently aligned with the end of the pattern. For example,
     * suppose the pattern ("panama") is at position 6:
     *
     * <pre>
     *                    1         2         3
     *          0123456789012345678901234567890
     * Text:    a man, a map, a panama canal
     * Pattern:       panama
     * </pre>
     *
     * This puts the 'p' of 'map' against the last byte 'a' of the pattern. Rather than testing the
     * pattern, we will look up 'p' in the skip table. There is an 'p' just 5 steps from the end of
     * the pattern, so we will move the pattern 5 places to the right before trying to match it.
     * This allows us to move in large strides through the text.
     */
    protected static class Finder {

        /**
         * Construct a Finder object that may be used (repeatedly) to find matches with the pattern
         * in text (arrays of bytes).
         *
         * @param pattern A vew that presents the pattern as an array of bytes
         */
        public Finder(PyBuffer pattern) {
            this.pattern = pattern;
        }

        /**
         * Mask defining how many of the bits of each byte are used when looking up the skip, used
         * like: <code>skip = skipTable[MASK & currentByte]</code>.
         */
        private static final byte MASK = 0x1f;

        /**
         * Table for looking up the skip, used like:
         * <code>skip = skipTable[MASK & currentByte]</code>.
         */
        protected int[] skipTable = null;

        /**
         * This method creates a compressed table of bad-character skips from the pattern. The entry
         * for a given byte value tells us how far it is from the end of the pattern, being 0 for
         * the actual last byte, or is equal to the length of the pattern if the byte does not occur
         * in the pattern. The table is compressed in that only the least-significant bits of the
         * byte index are used. In the case where 5 bits are used, the table is only 32 elements
         * long, rather than (as it might be) 256 bytes, the number of distinct byte values.
         */
        protected int[] calculateSkipTable() {
            int[] skipTable = new int[MASK + 1];
            int m = pattern.getLen();
            // Default skip is the pattern length: for bytes not in the pattern.
            Arrays.fill(skipTable, m);
            // For each byte in the pattern, make an entry for how far it is from the end.
            // The last occurrence of the byte value prevails.
            for (int i = 0; i < m; i++) {
                skipTable[MASK & pattern.byteAt(i)] = m - i - 1;
            }
            return skipTable;
        }

        /**
         * Set the text to be searched in successive calls to <code>nextIndex()</code>, where the
         * text is the entire array <code>text[]</code>.
         *
         * @param text to search
         */
        public void setText(byte[] text) {
            setText(text, 0, text.length);
        }

        /**
         * Set the text to be searched in successive calls to <code>nextIndex()</code>, where the
         * text is the entire byte array <code>text</code>.
         *
         * @param text to search
         */
        public void setText(BaseBytes text) {
            setText(text.storage, text.offset, text.size);
        }

        /**
         * Set the text to be searched in successive calls to <code>nextIndex()</code>, where the
         * text is effectively only the bytes <code>text[start]</code> to
         * <code>text[start+size-1]</code> inclusive.
         *
         * @param text to search
         * @param start first position to consider
         * @param size number of bytes within which to match
         */
        public void setText(byte[] text, int start, int size) {

            this.text = text;
            this.left = start;
            right = start + size - pattern.getLen() + 1; // Last pattern position + 1

            /*
             * We defer computing the table from construction to this point mostly because
             * calculateSkipTable() may be overridden, and we want to use the right one.
             */
            if (pattern.getLen() > 1 && skipTable == null) {
                skipTable = calculateSkipTable();
            }

        }

        protected final PyBuffer pattern;
        protected byte[] text = emptyStorage; // in case we forget to setText()
        protected int left = 0; // Leftmost pattern position to use
        protected int right = 0; // Rightmost pattern position + 1

        /**
         * Return the index in the text array where the preceding pattern match ends (one beyond the
         * last character matched), which may also be one beyond the effective end ofthe text.
         * Between a call to setText() and the first call to <code>nextIndex()</code> return the
         * start position.
         * <p>
         * The following idiom may be used:
         *
         * <pre>
         * f.setText(text);
         * int p = f.nextIndex();
         * int q = f.currIndex();
         * // The range text[p:q] is the matched segment.
         * </pre>
         *
         * @return index beyond end of last match found, i.e. where search will resume
         */
        public int currIndex() {
            return left;
        }

        /**
         * Find the next index in the text array where the pattern starts. Successive calls to
         * <code>nextIndex()</code> return the successive (non-overlapping) occurrences of the
         * pattern in the text.
         *
         * @return matching index or -1 if no (further) occurrences found
         */
        public int nextIndex() {
            int m = pattern.getLen();

            if (skipTable != null) { // ... which it will not be if m>1 and setText() was called
                /*
                 * Boyer-Moore-Horspool Algorithm using a Bloom array. Based on CPython stringlib,
                 * but without avoiding a proper bad character skip array.
                 */
                for (int i = left; i < right; /* i incremented in loop */) {
                    /*
                     * Unusually, start by looking up the skip. If text[i+m-1] matches, skip==0,
                     * although it will also be zero if only the least-significant bits match.
                     */
                    int skip = skipTable[MASK & text[i + (m - 1)]];

                    if (skip == 0) {
                        // Possible match, but we only used the least-significant bits: check all
                        int j, k = i;
                        for (j = 0; j < m; j++) { // k = i + j
                            if (text[k++] != pattern.byteAt(j)) {
                                break;
                            }
                        }
                        // If we tested all m bytes, that's a match.
                        if (j == m) {
                            left = k; // Start at text[i+m] next time we're called
                            return i;
                        }
                        // It wasn't a match: advance by one
                        i += 1;

                    } else {
                        /*
                         * The last byte of the pattern does not match the corresponding text byte.
                         * Skip tells us how far back down the pattern is a potential match, so how
                         * far it is safe to advance before we do another last-byte test.
                         */
                        i += skip;
                    }
                }

            } else if (m == 1) {
                // Special case of single byte search
                byte b = pattern.byteAt(0);
                for (int i = left; i < right; i++) {
                    if (text[i] == b) {
                        left = i + 1; // Start at text[i+1] next time we're called
                        return i;
                    }
                }

            } else {
                // Special case of search for empty (m==0) byte string
                int i = left;
                if (i <= right) {
                    // It is an honorary match - even when left==right
                    left = i + 1;
                    return i;
                }
            }

            // All sections fall out here if they do not find a match (even m==0)
            return -1;
        }

        /**
         * Count the non-overlapping occurrences of the pattern in the text.
         *
         * @param text to search
         * @return number of occurrences
         */
        public int count(byte[] text) {
            return count(text, 0, text.length, Integer.MAX_VALUE);
        }

        /**
         * Count the non-overlapping occurrences of the pattern in the text, where the text is
         * effectively only the bytes <code>text[start]</code> to <code>text[start+size-1]</code>
         * inclusive.
         *
         * @param text to search
         * @param start first position to consider
         * @param size number of bytes within which to match
         * @return number of occurrences
         */
        public int count(byte[] text, int start, int size) {
            return count(text, start, size, Integer.MAX_VALUE);
        }

        /**
         * Count the non-overlapping occurrences of the pattern in the text, where the text is
         * effectively only the bytes <code>text[start]</code> to <code>text[start+size-1]</code>.
         *
         * @param text to search
         * @param start first position to consider
         * @param size number of bytes within which to match
         * @param maxcount limit to number of occurrences to find
         * @return number of occurrences
         */
        public int count(byte[] text, int start, int size, int maxcount) {
            setText(text, start, size);
            int count = 0;
            while (count < maxcount && nextIndex() >= 0) {
                count++;
            }
            return count;
        }

    }

    /**
     * This class is the complement of {@link Finder} and implements the Boyer-Moore-Horspool
     * Algorithm adapted for right-to-left search for a pattern in byte arrays.
     */
    protected static class ReverseFinder extends Finder {

        /**
         * Construct a ReverseFinder object that may be used (repeatedly) to find matches with the
         * pattern in text (arrays of bytes).
         *
         * @param pattern A vew that presents the pattern as an array of bytes
         */
        public ReverseFinder(PyBuffer pattern) {
            super(pattern);
        }

        /**
         * Mask defining how many of the bits of each byte are used when looking up the skip, used
         * like: <code>skip = skipTable[MASK & currentByte]</code>.
         * <p>
         * Note that the way this is written at the moment, if <code>MASK</code> is different from
         * <code>super.MASK</code> <code>calculateSkipTable()</code> and <code>nextIndex()</code>
         * must both be overridden consistently to use the local definition.
         */
        private static final byte MASK = 0x1f;

        /**
         * This method creates a compressed table of bad-character skips from the pattern for
         * reverse-searching. The entry for a given byte value tells us how far it is from the start
         * of the pattern, being 0 for the actual first byte, or is equal to the length of the
         * pattern if the byte does not occur in the pattern. The table is compressed in that only
         * the least-significant bits of the byte index are used. In the case where 5 bits are used,
         * the table is only 32 elements long, rather than (as it might be) 256 bytes, the number of
         * distinct byte values.
         */
        @Override
        protected int[] calculateSkipTable() {
            int[] skipTable = new int[MASK + 1];
            int m = pattern.getLen();
            // Default skip is the pattern length: for bytes not in the pattern.
            Arrays.fill(skipTable, m);
            // For each byte in the pattern, make an entry for how far it is from the start.
            // The last occurrence of the byte value prevails.
            for (int i = m; --i >= 0;) {
                skipTable[MASK & pattern.byteAt(i)] = i;
            }
            return skipTable;
        }

        /**
         *
         * @return the new effective end of the text
         */
        @Override
        public int currIndex() {
            return right + pattern.getLen() - 1;
        }

        /**
         * Find the next index in the text array where the pattern starts, but working backwards.
         * Successive calls to <code>nextIndex()</code> return the successive (non-overlapping)
         * occurrences of the pattern in the text.
         *
         * @return matching index or -1 if no (further) occurrences found
         */
        @Override
        public int nextIndex() {

            int m = pattern.getLen();

            if (skipTable != null) { // ... which it will not be if m>1 and setText() was called
                /*
                 * Boyer-Moore-Horspool Algorithm using a Bloom array. Based on CPython stringlib,
                 * but without avoiding a proper bad character skip array.
                 */
                for (int i = right - 1; i >= left; /* i decremented in loop */) {
                    /*
                     * Unusually, start by looking up the skip. If text[i] matches, skip==0,
                     * although it will also be zero if only the least-significant bits match.
                     */
                    int skip = skipTable[MASK & text[i]];

                    if (skip == 0) {
                        // Possible match, but we only used the least-significant bits: check all
                        int j, k = i;
                        for (j = 0; j < m; j++) { // k = i + j
                            if (text[k++] != pattern.byteAt(j)) {
                                break;
                            }
                        }
                        // If we tested all m bytes, that's a match.
                        if (j == m) {
                            right = i - m + 1; // Start at text[i-m] next time we're called
                            return i;
                        }
                        // It wasn't a match: move left by one
                        i -= 1;

                    } else {
                        /*
                         * The first byte of the pattern does not match the corresponding text byte.
                         * Skip tells us how far up the pattern is a potential match, so how far
                         * left it is safe to move before we do another first-byte test.
                         */
                        i -= skip;
                    }
                }

            } else if (m == 1) {
                // Special case of single byte search
                byte b = pattern.byteAt(0);
                for (int i = right; --i >= left;) {
                    if (text[i] == b) {
                        right = i; // Start at text[i-1] next time we're called
                        return i;
                    }
                }

            } else {
                // Special case of search for empty (m==0) byte string
                int i = right;
                if (--i >= left) {
                    // It is an honorary match - even when right==left
                    right = i;
                    return i;
                }
            }

            // All sections fall out here if they do not find a match (even m==0)
            return -1;
        }
    }

    /**
     * Class for quickly determining whether a given byte is a member of a defined set. this class
     * provides an efficient mechanism when a lot of bytes must be tested against the same set.
     */
    protected static class ByteSet {

        protected final long[] map = new long[4];   // 256 bits

        /**
         * Construct a set from a byte oriented view.
         *
         * @param bytes to be in the set.
         */
        public ByteSet(PyBuffer bytes) {
            int n = bytes.getLen();
            for (int i = 0; i < n; i++) {
                int c = bytes.intAt(i);
                long mask = 1L << c; // Only uses low 6 bits of c (JLS)
                int word = c >> 6;
                map[word] |= mask;
            }
        }

        /**
         * Test to see if the byte is in the set.
         *
         * @param b value of the byte
         * @return true iff b is in the set
         */
        public boolean contains(byte b) {
            int word = (b & 0xff) >> 6;
            long mask = 1L << b; // Only uses low 6 bits of b (JLS)
            return (map[word] & mask) != 0;
        }

        /**
         * Test to see if the byte (expressed as an integer) is in the set.
         *
         * @param b integer value of the byte
         * @return true iff b is in the set
         * @throws ArrayIndexOutOfBoundsException if b>255 or b&lt;0
         */
        public boolean contains(int b) {
            int word = b >> 6;
            long mask = 1L << b; // Only uses low 6 bits of b (JLS)
            return (map[word] & mask) != 0;
        }

    }

    /**
     * Convenience routine producing a ValueError for "empty separator" if the PyBuffer is of an
     * object with zero length, and returning the length otherwise.
     *
     * @param separator view to test
     * @return the length of the separator
     * @throws PyException if the PyBuffer is zero length
     */
    protected final static int checkForEmptySeparator(PyBuffer separator) throws PyException {
        int n = separator.getLen();
        if (n == 0) {
            throw Py.ValueError("empty separator");
        }
        return n;
    }

    /**
     * Return the index [0..size-1] of the leftmost byte not matching any in <code>byteSet</code>,
     * or <code>size</code> if they are all strippable.
     *
     * @param byteSet set of byte values to skip over
     * @return index of first unstrippable byte
     */
    protected int lstripIndex(ByteSet byteSet) {
        int limit = offset + size;
        // Run up the storage checking against byteSet (or until we hit the end)
        for (int left = offset; left < limit; left++) {
            // Check against the byteSet to see if this is one to strip.
            if (!byteSet.contains(storage[left])) {
                // None of them matched: this is the leftmost non-strippable byte
                return left - offset;
            }
        }
        // We went through the whole array and they can all be stripped
        return size;
    }

    /**
     * Return the index [0..size-1] of the leftmost non-whitespace byte, or <code>size</code> if
     * they are all whitespace.
     *
     * @return index of first non-whitespace byte
     */
    protected int lstripIndex() {
        int limit = offset + size;
        // Run up the storage until non-whitespace (or hit end)
        for (int left = offset; left < limit; left++) {
            if (!isspace(storage[left])) {
                return left - offset;
            }
        }
        // We went through the whole array and they are all whitespace
        return size;
    }

    /**
     * Return the index [0..size-1] such that all bytes from here to the right match one in
     * <code>byteSet</code>, that is, the index of the matching tail, or <code>size</code> if there
     * is no matching tail byte.
     *
     * @param byteSet set of byte values to strip
     * @return index of strippable tail
     */
    protected int rstripIndex(ByteSet byteSet) {
        // Run down the storage checking the next byte against byteSet (or until we hit the start)
        for (int right = offset + size; right > offset; --right) {
            // Check against the byteSet to see if this is one to strip.
            if (!byteSet.contains(storage[right - 1])) {
                // None of them matched: this is the first strippable byte in the tail
                return right - offset;
            }
        }
        // We went through the whole array and they can all be stripped
        return 0;
    }

    /**
     * Return the index [0..size-1] such that all bytes from here to the right are whitespace, that
     * is, the index of the whitespace tail, or <code>size</code> if there is no whitespace tail.
     *
     * @return index of strippable tail
     */
    protected int rstripIndex() {
        // Run down the storage until next is non-whitespace (or hit start)
        for (int right = offset + size; right > offset; --right) {
            if (!isspace(storage[right - 1])) {
                return right - offset;
            }
        }
        // We went through the whole array and they are all whitespace
        return size;
    }

    /**
     * Ready-to-expose implementation of Python <code>count( sub [, start [, end ]] )</code>. Return
     * the number of non-overlapping occurrences of <code>sub</code> in the range [start, end].
     * Optional arguments <code>start</code> and <code>end</code> (which may be <code>null</code> or
     * <code>Py.None</code> ) are interpreted as in slice notation.
     *
     * @param sub bytes to find
     * @param ostart of slice to search
     * @param oend of slice to search
     * @return count of occurrences of sub within this byte array
     */
    final int basebytes_count(PyObject sub, PyObject ostart, PyObject oend) {
        try (PyBuffer vsub = getViewOrError(sub)) {
            Finder finder = new Finder(vsub);

            // Convert [ostart:oend] to integers
            int[] index = indicesEx(ostart, oend);  // [ start, end, 1, end-start ]

            // Make this slice the thing we count within.
            return finder.count(storage, offset + index[0], index[3]);
        }
    }

    /**
     * Ready-to-expose implementation of Python <code>find( sub [, start [, end ]] )</code>. Return
     * the lowest index in the byte array where byte sequence <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> (which may be <code>null</code> or
     * <code>Py.None</code> ) are interpreted as in slice notation. Return -1 if <code>sub</code> is
     * not found.
     *
     * @param sub bytes to find
     * @param ostart of slice to search
     * @param oend of slice to search
     * @return index of start of occurrence of sub within this byte array
     */
    final int basebytes_find(PyObject sub, PyObject ostart, PyObject oend) {
        try (PyBuffer vsub = getViewOrError(sub)) {
            Finder finder = new Finder(vsub);
            return find(finder, ostart, oend);
        }
    }

    /**
     * Almost ready-to-expose implementation of Python class method <code>fromhex(string)</code>.
     * This assigns a value to the passed byte array object from a string of two-digit hexadecimal
     * numbers. Spaces (but not whitespace in general) are acceptable around the numbers, not
     * within. Non-hexadecimal characters or un-paired hex digits raise a <code>ValueError</code>.
     * Example:
     *
     * <pre>
     * bytearray.fromhex('B9 01EF') -> * bytearray(b'\xb9\x01\xef')."
     * </pre>
     *
     * @param result to receive the decoded values
     * @param hex specification of the bytes
     * @throws PyException (ValueError) if non-hex characters, or isolated ones, are encountered
     */
    static void basebytes_fromhex(BaseBytes result, String hex) throws PyException {

        final int hexlen = hex.length();
        result.newStorage(hexlen / 2); // Over-provides storage if hex has spaces

        // We might produce a ValueError with this message.
        String fmt = "non-hexadecimal number found in fromhex() arg at position %d";

        // Output pointer in the result array
        byte[] r = result.storage;
        int p = result.offset;

        /*
         * When charAt(i) is a hex digit, we will always access hex.charAt(i+1), and catch the
         * exception if that is beyond the end of the array.
         */
        for (int i = 0; i < hexlen; /* i incremented in loop by 1 or 2 */) {
            char c = hex.charAt(i++);
            if (c != ' ') {
                try {
                    // hexDigit throws IllegalArgumentException if non-hexadecimal character found
                    int value = hexDigit(c);
                    c = hex.charAt(i++); // Throw IndexOutOfBoundsException if no second digit
                    value = (value << 4) + hexDigit(c);
                    r[p++] = (byte)value;
                } catch (IllegalArgumentException e) {
                    throw Py.ValueError(String.format(fmt, i - 1));
                } catch (IndexOutOfBoundsException e) {
                    throw Py.ValueError(String.format(fmt, i - 2));
                }
            }
        }
        result.size = p - result.offset;
    }

    /**
     * Translate one character to its hexadecimal value.
     *
     * @param c to translate
     * @return value 0-15
     * @throws IllegalArgumentException if c is not '0-'9', 'A'-'F' or 'a'-'f'.
     */
    private static int hexDigit(char c) throws IllegalArgumentException {
        int result = c - '0';
        if (result >= 0) {
            if (result < 10) { // digit
                return result;
            } else {
                // If c is a letter, c & 0xDF is its uppercase.
                // If c is not a letter c & 0xDF is still not a letter.
                result = (c & 0xDF) - 'A';
                if (result >= 0 && result < 6) { // A-F or a-f
                    return result + 10;
                }
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Almost ready-to-expose implementation of Python <code>join(iterable)</code>.
     *
     * @param iter iterable of objects capable of being regarded as byte arrays
     * @return the byte array that is their join
     */
    final synchronized PyByteArray basebytes_join(Iterable<? extends PyObject> iter) {

        List<PyBuffer> iterList = new LinkedList<PyBuffer>();
        long mysize = this.size;
        long totalSize = 0;
        boolean first = true;

        try {
            for (PyObject o : iter) {
                // Scan the iterable into a list, checking type and accumulating size
                PyBuffer v = getView(o);
                if (v == null) {
                    // Unsuitable object to be in this join
                    String fmt = "can only join an iterable of bytes (item %d has type '%.80s')";
                    throw Py.TypeError(String.format(fmt, iterList.size(), o.getType()
                            .fastGetName()));
                }
                iterList.add(v);
                totalSize += v.getLen();

                // Each element after the first is preceded by a copy of this
                if (!first) {
                    totalSize += mysize;
                } else {
                    first = false;
                }

                if (totalSize > Integer.MAX_VALUE) {
                    throw Py.OverflowError("join() result would be too long");
                }
            }

            // Load the Views from the iterator into a new PyByteArray
            PyByteArray result = new PyByteArray((int)totalSize);
            int p = result.offset; // Copy-to pointer
            first = true;

            for (PyBuffer v : iterList) {
                // Each element after the first is preceded by a copy of this
                if (!first) {
                    System.arraycopy(storage, offset, result.storage, p, size);
                    p += size;
                } else {
                    first = false;
                }
                // Then the element from the iterable
                v.copyTo(result.storage, p);
                p += v.getLen();
            }

            return result;

        } finally {
            // All the buffers we acquired have to be realeased
            for (PyBuffer v : iterList) {
                v.release();
            }
        }
    }

    /**
     * Implementation of Python <code>partition(sep)</code>, returning a 3-tuple of byte arrays (of
     * the same type as <code>this</code>).
     *
     * Split the string at the first occurrence of <code>sep</code>, and return a 3-tuple containing
     * the part before the separator, the separator itself, and the part after the separator. If the
     * separator is not found, return a 3-tuple containing the string itself, followed by two empty
     * byte arrays.
     * <p>
     * The elements of the <code>PyTuple</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param sep the separator on which to partition this byte array
     * @return a tuple of (head, separator, tail)
     */
    public PyTuple partition(PyObject sep) {
        return basebytes_partition(sep);
    }

    /**
     * Ready-to-expose implementation of Python <code>partition(sep)</code>.
     * <p>
     * The elements of the <code>PyTuple</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param sep the separator on which to partition this byte array
     * @return a tuple of (head, separator, tail)
     */
    final synchronized PyTuple basebytes_partition(PyObject sep) {

        // View the separator as a byte array (or error if we can't)
        try (PyBuffer separator = getViewOrError(sep)) {
            // Create a Finder for the separator and set it on this byte array
            int n = checkForEmptySeparator(separator);
            Finder finder = new Finder(separator);
            finder.setText(this);

            // We only use it once, to find the first occurrence
            int p = finder.nextIndex() - offset;
            if (p >= 0) {
                // Found at p, so we'll be returning ([0:p], [p:p+n], [p+n:])
                return partition(p, p + n);
            } else {
                // Not found: choose values leading to ([0:size], '', '')
                return partition(size, size);
            }
        }
    }

    /**
     * Construct return value for implementation of Python <code>partition(sep)</code> or
     * <code>rpartition(sep)</code>, returns [0:p], [p:q], [q:]
     * <p>
     * The elements of the <code>PyTuple</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param p start of separator
     * @param q start of tail
     * @return ([0:p], [p:q], [q:])
     */
    private PyTuple partition(int p, int q) {
        BaseBytes head = this.getslice(0, p);
        BaseBytes sep = this.getslice(p, q);
        BaseBytes tail = this.getslice(q, size);
        return new PyTuple(head, sep, tail);
    }

    /**
     * Ready-to-expose implementation of Python <code>rfind( sub [, start [, end ]] )</code>. Return
     * the highest index in the byte array where byte sequence <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> (which may be <code>null</code> or
     * <code>Py.None</code>) are interpreted as in slice notation. Return -1 if <code>sub</code> is
     * not found.
     *
     * @param sub bytes to find
     * @param ostart of slice to search
     * @param oend of slice to search
     * @return index of start of occurrence of sub within this byte array
     */
    final int basebytes_rfind(PyObject sub, PyObject ostart, PyObject oend) {
        try (PyBuffer vsub = getViewOrError(sub)) {
            Finder finder = new ReverseFinder(vsub);
            return find(finder, ostart, oend);
        }
    }

    /**
     * Common code for Python <code>find( sub [, start [, end ]] )</code> and
     * <code>rfind( sub [, start [, end ]] )</code>. Return the lowest or highest index in the byte
     * array where byte sequence used to construct <code>finder</code> is found. The particular type
     * (plain <code>Finder</code> or <code>ReverseFinder</code>) determines the direction.
     *
     * @param finder for the bytes to find, sometime forwards, sometime backwards
     * @param ostart of slice to search
     * @param oend of slice to search
     * @return index of start of occurrence of sub within this byte array
     */
    private final int find(Finder finder, PyObject ostart, PyObject oend) {

        // Convert [ostart:oend] to integers
        int[] index = indicesEx(ostart, oend);  // [ start, end, 1, end-start ]

        // Make this slice the thing we search. Note finder works with Java index in storage.
        finder.setText(storage, offset + index[0], index[3]);
        int result = finder.nextIndex();

        // Compensate for the offset in returning a value
        return (result < 0) ? -1 : result - offset;
    }

    /**
     * An almost ready-to-expose implementation of Python
     * <code>replace( old, new [, count ] )</code>, returning a <code>PyByteArray</code> with all
     * occurrences of sequence <code>oldB</code> replaced by <code>newB</code>. If the optional
     * argument <code>count</code> is given, only the first <code>count</code> occurrences are
     * replaced.
     *
     * @param oldB sequence to find
     * @param newB relacement sequence
     * @param maxcount maximum occurrences are replaced or &lt; 0 for all
     * @return result of replacement as a new PyByteArray
     */
    final synchronized PyByteArray basebytes_replace(PyObject oldB, PyObject newB, int maxcount) {

        // View the to and from as byte arrays (or error if we can't)
        try (PyBuffer to = getViewOrError(newB); PyBuffer from = getViewOrError(oldB)) {
            /*
             * The logic of the first section is copied exactly from CPython in order to get the
             * same behaviour. The "headline" description of replace is simple enough but the corner
             * cases can be surprising:
             */
            // >>> bytearray(b'hello').replace(b'',b'-')
            // bytearray(b'-h-e-l-l-o-')
            // >>> bytearray(b'hello').replace(b'',b'-',3)
            // bytearray(b'-h-e-llo')
            // >>> bytearray(b'hello').replace(b'',b'-',1)
            // bytearray(b'-hello')
            // >>> bytearray().replace(b'',b'-')
            // bytearray(b'-')
            // >>> bytearray().replace(b'',b'-',1) # ?
            // bytearray(b'')

            if (maxcount < 0) {
                maxcount = Integer.MAX_VALUE;

            } else if (maxcount == 0 || size == 0) {
                // nothing to do; return the original bytes
                return new PyByteArray(this);
            }

            int from_len = from.getLen();
            int to_len = to.getLen();

            if (maxcount == 0 || (from_len == 0 && to_len == 0)) {
                // nothing to do; return the original bytes
                return new PyByteArray(this);

            } else if (from_len == 0) {
                // insert the 'to' bytes everywhere.
                // >>> "Python".replace("", ".")
                // '.P.y.t.h.o.n.'
                return replace_interleave(to, maxcount);

            } else if (size == 0) {
                // Special case for "".replace("", "A") == "A"
                return new PyByteArray(to);

            } else if (to_len == 0) {
                // Delete occurrences of the 'from' bytes
                return replace_delete_substring(from, maxcount);

            } else if (from_len == to_len) {
                // Result is same size as this byte array, whatever the number of replacements.
                return replace_substring_in_place(from, to, maxcount);

            } else {
                // Otherwise use the generic algorithm
                return replace_substring(from, to, maxcount);
            }
        }
    }

    /*
     * Algorithms for different cases of string replacement. CPython also has specialisations for
     * when 'from' or 'to' or both are single bytes. This may also be worth doing in Java when the
     * 'to' is a single byte. (The 'from' is turned into a Finder object which already makes a
     * special case of single bytes.)
     */

    /**
     * Helper for {@link #basebytes_replace(PyObject, PyObject, int)} implementing the general case
     * of byte-string replacement when the new and old strings have different lengths.
     *
     * @param from byte-string to find and replace
     * @param to replacement byte-string
     * @param maxcount maximum number of replacements to make
     * @return the result as a new PyByteArray
     */
    private PyByteArray replace_substring(PyBuffer from, PyBuffer to, int maxcount) {
        // size>=1, len(from)>=1, len(to)>=1, maxcount>=1

        // Initialise a Finder for the 'from' pattern
        Finder finder = new Finder(from);

        int count = finder.count(storage, offset, size, maxcount);
        if (count == 0) {
            // no matches
            return new PyByteArray(this);
        }

        int from_len = from.getLen();
        int to_len = to.getLen();

        // Calculate length of result and check for too big
        long result_len = size + count * (to_len - from_len);
        byte[] r; // Build result here
        try {
            // Good to go. As we know the ultimate size, we can do all our allocation in one
            r = new byte[(int)result_len];
        } catch (OutOfMemoryError e) {
            throw Py.OverflowError("replace bytes is too long");
        }

        int p = offset; // Copy-from index in this.storage
        int rp = 0;     // Copy-to index in r

        // Reset the Finder on the (active part of) this.storage
        finder.setText(storage, p, size);

        while (count-- > 0) {
            // First occurrence of 'from' bytes in storage
            int q = finder.nextIndex();
            if (q < 0) {
                // Never happens because we've got count right
                break;
            }

            // Output the stretch up to the discovered occurrence of 'from'
            int length = q - p;
            if (length > 0) {
                System.arraycopy(storage, p, r, rp, length);
                rp += length;
            }

            // Skip over the occurrence of the 'from' bytes
            p = q + from_len;

            // Output a copy of 'to'
            to.copyTo(r, rp);
            rp += to_len;
        }

        // Copy the rest of the original string
        int length = size + offset - p;
        if (length > 0) {
            System.arraycopy(storage, p, r, rp, length);
            rp += length;
        }

        // Make r[] the storage of a new bytearray
        return new PyByteArray(r);
    }

    /**
     * Handle the interleaving case b'hello'.replace(b'', b'..') = b'..h..e..l..l..o..' At the call
     * site we are guaranteed: size>=1, to.getLen()>=1, maxcount>=1
     *
     * @param to the replacement bytes as a byte-oriented view
     * @param maxcount upper limit on number of insertions
     */
    private PyByteArray replace_interleave(PyBuffer to, int maxcount) {

        // Insert one at the beginning and one after every byte, or as many as allowed
        int count = size + 1;
        if (maxcount < count) {
            count = maxcount;
        }

        int to_len = to.getLen();

        // Calculate length of result and check for too big
        long result_len = ((long)count) * to_len + size;
        byte[] r; // Build result here
        try {
            // Good to go. As we know the ultimate size, we can do all our allocation in one
            r = new byte[(int)result_len];
        } catch (OutOfMemoryError e) {
            throw Py.OverflowError("replace bytes is too long");
        }

        int p = offset; // Copy-from index in this.storage
        int rp = 0;     // Copy-to index in r

        // Lay the first one down (guaranteed this will occur as count>=1)
        to.copyTo(r, rp);
        rp += to_len;

        // And the rest
        for (int i = 1; i < count; i++) {
            r[rp++] = storage[p++];
            to.copyTo(r, rp);
            rp += to_len;
        }

        // Copy the rest of the original string
        int length = size + offset - p;
        if (length > 0) {
            System.arraycopy(storage, p, r, rp, length);
            rp += length;
        }

        // Make r[] the storage of a new bytearray
        return new PyByteArray(r);
    }

    /**
     * Helper for {@link #basebytes_replace(PyObject, PyObject, int)} implementing the special case
     * of byte-string replacement when the new string has zero length, i.e. deletion.
     *
     * @param from byte-string to find and delete
     * @param maxcount maximum number of deletions to make
     * @return the result as a new PyByteArray
     */
    private PyByteArray replace_delete_substring(PyBuffer from, int maxcount) {
        // len(self)>=1, len(from)>=1, to="", maxcount>=1

        // Initialise a Finder for the 'from' pattern
        Finder finder = new Finder(from);

        int count = finder.count(storage, offset, size, maxcount);
        if (count == 0) {
            // no matches
            return new PyByteArray(this);
        }

        int from_len = from.getLen();
        long result_len = size - (count * from_len);
        assert (result_len >= 0);

        byte[] r; // Build result here
        try {
            // Good to go. As we know the ultimate size, we can do all our allocation in one
            r = new byte[(int)result_len];
        } catch (OutOfMemoryError e) {
            throw Py.OverflowError("replace bytes is too long");
        }

        int p = offset; // Copy-from index in this.storage
        int rp = 0;     // Copy-to index in r

        // Reset the Finder on the (active part of) this.storage
        finder.setText(storage, offset, size);

        while (count-- > 0) {
            // First occurrence of 'from' bytes in storage
            int q = finder.nextIndex();
            if (q < 0) {
                // Never happens because we've got count right
                break;
            }

            // Output the stretch up to the discovered occurrence of 'from'
            int length = q - p;
            if (length > 0) {
                System.arraycopy(storage, p, r, rp, length);
                rp += length;
            }

            // Skip over the occurrence of the 'from' bytes
            p = q + from_len;
        }

        // Copy the rest of the original string
        int length = size + offset - p;
        if (length > 0) {
            System.arraycopy(storage, p, r, rp, length);
            rp += length;
        }

        // Make r[] the storage of a new bytearray
        return new PyByteArray(r);
    }

    /**
     * Helper for {@link #basebytes_replace(PyObject, PyObject, int)} implementing the special case
     * of byte-string replacement when the new and old strings have the same length. The key
     * observation here is that the result has the same size as this byte array, and we know this
     * even without counting how many replacements we shall make.
     *
     * @param from byte-string to find and replace
     * @param to replacement byte-string
     * @param maxcount maximum number of replacements to make
     * @return the result as a new PyByteArray
     */
    private PyByteArray replace_substring_in_place(PyBuffer from, PyBuffer to, int maxcount) {
        // len(self)>=1, len(from)==len(to)>=1, maxcount>=1

        // Initialise a Finder for the 'from' pattern
        Finder finder = new Finder(from);

        int count = maxcount;

        // The result will be this.size
        byte[] r; // Build result here
        try {
            r = new byte[this.size];
        } catch (OutOfMemoryError e) {
            throw Py.OverflowError("replace bytes is too long");
        }

        System.arraycopy(storage, offset, r, 0, size);

        // Change everything in-place: easiest if we search the destination
        finder.setText(r);

        while (count-- > 0) {
            int q = finder.nextIndex(); // Note q is an index into result.storage
            if (q < 0) {
                // Normal exit because we discover actual count as we go along
                break;
            }
            // Overwrite with 'to' the stretch corresponding to the matched 'from'
            to.copyTo(r, q);
        }

        // Make r[] the storage of a new bytearray
        return new PyByteArray(r);
    }

    /**
     * Implementation of Python <code>rpartition(sep)</code>, returning a 3-tuple of byte arrays (of
     * the same type as <code>this</code>).
     *
     * Split the string at the rightmost occurrence of <code>sep</code>, and return a 3-tuple
     * containing the part before the separator, the separator itself, and the part after the
     * separator. If the separator is not found, return a 3-tuple containing two empty byte arrays,
     * followed by the byte array itself.
     * <p>
     * The elements of the <code>PyTuple</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param sep the separator on which to partition this byte array
     * @return a tuple of (head, separator, tail)
     */
    public PyTuple rpartition(PyObject sep) {
        return basebytes_rpartition(sep);
    }

    /**
     * Ready-to-expose implementation of Python <code>rpartition(sep)</code>.
     * <p>
     * The elements of the <code>PyTuple</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param sep the separator on which to partition this byte array
     * @return a tuple of (head, separator, tail)
     */
    final synchronized PyTuple basebytes_rpartition(PyObject sep) {

        // View the separator as a byte array (or error if we can't)
        try (PyBuffer separator = getViewOrError(sep)) {
            // Create a Finder for the separtor and set it on this byte array
            int n = checkForEmptySeparator(separator);
            Finder finder = new ReverseFinder(separator);
            finder.setText(this);

            // We only use it once, to find the first (from the right) occurrence
            int p = finder.nextIndex() - offset;
            if (p >= 0) {
                // Found at p, so we'll be returning ([0:p], [p:p+n], [p+n:])
                return partition(p, p + n);
            } else {
                // Not found: choose values leading to ('', '', [0:size])
                return partition(0, 0);
            }
        }
    }

    /**
     * Implementation of Python <code>rsplit()</code>, that returns a list of the words in the byte
     * array, using whitespace as the delimiter. See {@link #rsplit(PyObject, int)}.
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @return PyList of byte arrays that result from the split
     */
    public PyList rsplit() {
        return basebytes_rsplit_whitespace(-1);
    }

    /**
     * Implementation of Python <code>rsplit(sep)</code>, that returns a list of the words in the
     * byte array, using <code>sep</code> as the delimiter. See {@link #rsplit(PyObject, int)} for
     * the semantics of the separator.
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param sep <code>bytes</code>, or object viewable as bytes, defining the separator
     * @return PyList of byte arrays that result from the split
     */
    public PyList rsplit(PyObject sep) {
        return basebytes_rsplit(sep, -1);
    }

    /**
     * Implementation of Python <code>rsplit(sep, maxsplit)</code>, that returns a list of the words
     * in the byte array, using <code>sep</code> as the delimiter. If <code>maxsplit</code> is
     * given, at most <code>maxsplit</code> splits are done (thus, the list will have at most
     * <code>maxsplit+1</code> elements). If <code>maxsplit</code> is not specified, then there is
     * no limit on the number of splits (all possible splits are made).
     * <p>
     * The semantics of <code>sep</code> and maxcount are identical to those of
     * <code>split(sep, maxsplit)</code> , except that splits are generated from the right (and
     * pushed onto the front of the result list). The result is only different from that of
     * <code>split</code> if <code>maxcount</code> limits the number of splits. For example,
     * <ul>
     * <li><code>bytearray(b' 1  2   3  ').rsplit()</code> returns
     * <code>[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')]</code>, and</li>
     * <li><code>bytearray(b'  1  2   3  ').rsplit(None, 1)</code> returns
     * <code>[bytearray(b'  1 2'), bytearray(b'3')]</code></li>.
     * </ul>
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param sep <code>bytes</code>, or object viewable as bytes, defining the separator
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    public PyList rsplit(PyObject sep, int maxsplit) {
        return basebytes_rsplit(sep, maxsplit);
    }

    /**
     * Ready-to-expose implementation of Python <code>rsplit(sep, maxsplit)</code>, that returns a
     * list of the words in the byte array, using <code>sep</code> as the delimiter. Use the defines
     * whitespace semantics if <code>sep</code> is <code>null</code>.
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param sep <code>bytes</code>, or object viewable as bytes, defining the separator
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    final PyList basebytes_rsplit(PyObject sep, int maxsplit) {
        if (sep == null || sep == Py.None) {
            return basebytes_rsplit_whitespace(maxsplit);
        } else {
            return basebytes_rsplit_explicit(sep, maxsplit);
        }
    }

    /**
     * Implementation of Python <code>rsplit(sep, maxsplit)</code>, that returns a list of the words
     * in the byte array, using <code>sep</code> (which is not <code>null</code>) as the delimiter.
     * If <code>maxsplit>=0</code>, at most <code>maxsplit</code> splits are done (thus, the list
     * will have at most <code>maxsplit+1</code> elements). If <code>maxsplit&lt;0</code>, then
     * there is no limit on the number of splits (all possible splits are made).
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param sep <code>bytes</code>, or object viewable as bytes, defining the separator
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    final synchronized PyList basebytes_rsplit_explicit(PyObject sep, int maxsplit) {

        // The separator may be presented as anything viewable as bytes
        try (PyBuffer separator = getViewOrError(sep)) {
            int n = checkForEmptySeparator(separator);

            PyList result = new PyList();

            // Use the Finder class to search in the storage of this byte array
            Finder finder = new ReverseFinder(separator);
            finder.setText(this);

            int q = offset + size; // q points to "honorary separator"
            int p;

            // At this point storage[q-1] is the last byte of the rightmost unsplit word, or
            // q=offset if there aren't any. While we have some splits left to do ...
            while (q > offset && maxsplit-- != 0) {
                // Delimit the word whose last byte is storage[q-1]
                int r = q;
                // Skip p backwards over the word and the separator
                q = finder.nextIndex();
                if (q < 0) {
                    p = offset;
                } else {
                    p = q + n;
                }
                // storage[p] is the first byte of the word.
                BaseBytes word = getslice(p - offset, r - offset);
                result.add(0, word);
            }

            // Prepend the remaining unsplit text if any
            if (q >= offset) {
                BaseBytes word = getslice(0, q - offset);
                result.add(0, word);
            }
            return result;
        }
    }

    /**
     * Implementation of Python <code>rsplit(None, maxsplit)</code>, that returns a list of the
     * words in the byte array, using whitespace as the delimiter. If <code>maxsplit</code> is
     * given, at most <code>maxsplit</code> splits are done (thus, the list will have at most
     * <code>maxsplit+1</code> elements). If maxsplit is not specified, then there is no limit on
     * the number of splits (all possible splits are made).
     * <p>
     * Runs of consecutive whitespace are regarded as a single separator, and the result will
     * contain no empty strings at the start or end if the string has leading or trailing
     * whitespace. Consequently, splitting an empty string or a string consisting of just whitespace
     * with a <code>None</code> separator returns [].
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this/self</code>.
     *
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    final synchronized PyList basebytes_rsplit_whitespace(int maxsplit) {

        PyList result = new PyList();
        int p, q; // Indexes of unsplit text and whitespace

        // Scan backwards over trailing whitespace
        for (q = offset + size; q > offset; --q) {
            if (!isspace(storage[q - 1])) {
                break;
            }
        }

        // Note: bytearray().rsplit() = bytearray(b' ').rsplit() = []

        // At this point storage[q-1] is the rightmost non-space byte, or
        // q=offset if there aren't any. While we have some splits left ...
        while (q > offset && maxsplit-- != 0) {
            // Delimit the word whose last byte is storage[q-1]
            // Skip p backwards over the non-whitespace
            for (p = q; p > offset; --p) {
                if (isspace(storage[p - 1])) {
                    break;
                }
            }
            // storage[p] is the first byte of the word. (p=offset or storage[p-1] is whitespace.)
            BaseBytes word = getslice(p - offset, q - offset);
            result.add(0, word);
            // Skip q backwards over the whitespace
            for (q = p; q > offset; --q) {
                if (!isspace(storage[q - 1])) {
                    break;
                }
            }
        }

        // Prepend the remaining unsplit text if any
        if (q > offset) {
            BaseBytes word = getslice(0, q - offset);
            result.add(0, word);
        }
        return result;
    }

    /**
     * Implementation of Python <code>split()</code>, that returns a list of the words in the byte
     * array, using whitespace as the delimiter. See {@link #split(PyObject, int)}.
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @return PyList of byte arrays that result from the split
     */
    public PyList split() {
        return basebytes_split_whitespace(-1);
    }

    /**
     * Implementation of Python <code>split(sep)</code>, that returns a list of the words in the
     * byte array, using <code>sep</code> as the delimiter. See {@link #split(PyObject, int)} for
     * the semantics of the separator.
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param sep <code>bytes</code>, or object viewable as bytes, defining the separator
     * @return PyList of byte arrays that result from the split
     */
    public PyList split(PyObject sep) {
        return basebytes_split(sep, -1);
    }

    /**
     * Implementation of Python <code>split(sep, maxsplit)</code>, that returns a list of the words
     * in the byte array, using <code>sep</code> as the delimiter. If <code>maxsplit</code> is
     * given, at most <code>maxsplit</code> splits are done. (Thus, the list will have at most
     * <code>maxsplit+1</code> elements). If <code>maxsplit</code> is not specified, then there is
     * no limit on the number of splits (all possible splits are made).
     * <p>
     * If <code>sep</code> is given, consecutive delimiters are not grouped together and are deemed
     * to delimit empty strings (for example, <code>'1,,2'.split(',')</code> returns
     * <code>['1', '', '2']</code>). The <code>sep</code> argument may consist of multiple
     * characters (for example, <code>'1&lt;>2&lt;>3'.split('&lt;>')</code> returns <code>['1',
     * '2', '3']</code>). Splitting an empty string with a specified separator <code> ['']</code>.
     * <p>
     * If <code>sep</code> is not specified or is <code>None</code>, a different splitting algorithm
     * is applied: runs of consecutive whitespace are regarded as a single separator, and the result
     * will contain no empty strings at the start or end if the string has leading or trailing
     * whitespace. Consequently, splitting an empty string or a string consisting of just whitespace
     * with a <code>None</code> separator returns <code>[]</code>. For example,
     * <ul>
     * <li><code>bytearray(b' 1  2   3  ').split()</code> returns
     * <code>[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')]</code>, and</li>
     * <li><code>bytearray(b'  1  2   3  ').split(None, 1)</code> returns
     * <code>[bytearray(b'1'), bytearray(b'2   3  ')]</code>.</li>
     * </ul>
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param sep <code>bytes</code>, or object viewable as bytes, defining the separator
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    public PyList split(PyObject sep, int maxsplit) {
        return basebytes_split(sep, maxsplit);
    }

    /**
     * Ready-to-expose implementation of Python <code>split(sep, maxsplit)</code>, that returns a
     * list of the words in the byte array, using <code>sep</code> as the delimiter. Use the defines
     * whitespace semantics if <code>sep</code> is <code>null</code>.
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param sep <code>bytes</code>, or object viewable as bytes, defining the separator
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    final PyList basebytes_split(PyObject sep, int maxsplit) {
        if (sep == null || sep == Py.None) {
            return basebytes_split_whitespace(maxsplit);
        } else {
            return basebytes_split_explicit(sep, maxsplit);
        }
    }

    /**
     * Implementation of Python <code>split(sep, maxsplit)</code>, that returns a list of the words
     * in the byte array, using <code>sep</code> (which is not <code>null</code>) as the delimiter.
     * If <code>maxsplit>=0</code>, at most <code>maxsplit</code> splits are done (thus, the list
     * will have at most <code>maxsplit+1</code> elements). If <code>maxsplit&lt;0</code>, then
     * there is no limit on the number of splits (all possible splits are made).
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param sep <code>bytes</code>, or object viewable as bytes, defining the separator
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    final synchronized PyList basebytes_split_explicit(PyObject sep, int maxsplit) {

        // The separator may be presented as anything viewable as bytes
        try (PyBuffer separator = getViewOrError(sep)) {
            checkForEmptySeparator(separator);

            PyList result = new PyList();

            // Use the Finder class to search in the storage of this byte array
            Finder finder = new Finder(separator);
            finder.setText(this);

            // Look for the first separator
            int p = finder.currIndex(); // = offset
            int q = finder.nextIndex(); // First separator (or <0 if not found)

            // Note: bytearray().split(' ') == [bytearray(b'')]

            // While we found a separator, and we have some splits left (if maxsplit started>=0)
            while (q >= 0 && maxsplit-- != 0) {
                // Note the Finder works in terms of indexes into this.storage
                result.append(getslice(p - offset, q - offset));
                p = finder.currIndex(); // Start of unsplit text
                q = finder.nextIndex(); // Next separator (or <0 if not found)
            }

            // Append the remaining unsplit text
            result.append(getslice(p - offset, size));
            return result;
        }
    }

    /**
     * Implementation of Python <code>split(None, maxsplit)</code>, that returns a list of the words
     * in the byte array, using whitespace as the delimiter. If <code>maxsplit</code> is given, at
     * most maxsplit splits are done (thus, the list will have at most <code>maxsplit+1</code>
     * elements). If <code>maxsplit</code> is not specified, then there is no limit on the number of
     * splits (all possible splits are made).
     * <p>
     * Runs of consecutive whitespace are regarded as a single separator, and the result will
     * contain no empty strings at the start or end if the string has leading or trailing
     * whitespace. Consequently, splitting an empty string or a string consisting of just whitespace
     * with a <code>None</code> separator returns [].
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    final synchronized PyList basebytes_split_whitespace(int maxsplit) {

        PyList result = new PyList();
        int limit = offset + size;
        int p, q; // Indexes of unsplit text and whitespace

        // Scan over leading whitespace
        for (p = offset; p < limit && isspace(storage[p]); p++) {
            ; // continue
        }

        // Note: bytearray().split() = bytearray(b' ').split() = []

        // At this point if p<limit it points to the start of a word.
        // While we have some splits left (if maxsplit started>=0)
        while (p < limit && maxsplit-- != 0) {
            // Delimit a word at p
            // storage[p] is not whitespace or at the limit: it is the start of a word
            // Skip q over the non-whitespace at p
            for (q = p; q < limit && !isspace(storage[q]); q++) {
                ; // continue
            }
            // storage[q] is whitespace or it is at the limit
            result.append(getslice(p - offset, q - offset));
            // Skip p over the whitespace at q
            for (p = q; p < limit && isspace(storage[p]); p++) {
                ; // continue
            }
        }

        // Append the remaining unsplit text if any
        if (p < limit) {
            result.append(getslice(p - offset, size));
        }
        return result;
    }

    /**
     * Implementation of Python <code>splitlines()</code>, returning a list of the lines in the byte
     * array, breaking at line boundaries. Line breaks are not included in the resulting segments.
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @return List of segments
     */
    public PyList splitlines() {
        return basebytes_splitlines(false);
    }

    /**
     * Implementation of Python <code>splitlines(keepends)</code>, returning a list of the lines in
     * the string, breaking at line boundaries. Line breaks are not included in the resulting list
     * unless <code>keepends</code> is true.
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param keepends if true, include the end of line bytes(s)
     * @return PyList of segments
     */
    public PyList splitlines(boolean keepends) {
        return basebytes_splitlines(keepends);
    }

    /**
     * Ready-to-expose implementation of Python <code>splitlines(keepends)</code>, returning a list
     * of the lines in the array, breaking at line boundaries. Line breaks are not included in the
     * resulting list unless keepends is given and true.
     * <p>
     * The elements of the <code>PyList</code> returned by this method are instances of the same
     * actual type as <code>this</code>.
     *
     * @param keepends if true, include the end of line bytes(s)
     * @return List of segments
     */
    protected final synchronized PyList basebytes_splitlines(boolean keepends) {

        PyList list = new PyList();
        int limit = offset + size;

        for (int p = offset; p < limit; /* p advanced in loop */) {
            int q, lenEOL = 0;
            // Scan q to the end of the line (or buffer) including the EOL bytes
            for (q = p; q < limit; q++) {
                byte b = storage[q];
                if (b == '\r') {
                    lenEOL = (storage[q + 1] == '\n') ? 2 : 1;
                    break;
                } else if (b == '\n') {
                    lenEOL = 1; // Just one EOL byte \n
                    break;
                }
            }

            // lenEOL =2: the line ended \r\n, and q points at \r;
            // lenEOL =1: the line ended \n or \r (only), and q points at it;
            // lenEOL =0: the line ended with the end of the data (and q=limit)

            if (keepends) {
                list.append(getslice(p - offset, q + lenEOL - offset));
            } else {
                list.append(getslice(p - offset, q - offset));
            }

            // Start next line after what terminated it
            p = q + lenEOL;
        }

        return list;
    }

    //
    // Padding, filling and centering
    //

    /**
     * Helper to check the fill byte for {@link #rjust(String)}, {@link #ljust(String)} and
     * {@link #center(String)}, which is required to be a single character string, treated as a
     * byte.
     *
     * @param function name
     * @param fillchar or <code>null</code>
     * @return
     */
    protected static byte fillByteCheck(String function, String fillchar) {
        if (fillchar == null) {
            return ' ';
        } else if (fillchar.length() == 1) {
            return (byte)fillchar.charAt(0);
        } else {
            throw Py.TypeError(function + "() argument 2 must be char, not str");
        }
    }

    /**
     * Helper function to construct the return value for {@link #rjust(String)},
     * {@link #ljust(String)} and {@link #center(String)}. Clients calculate the left and right fill
     * values according to their nature, and ignoring the possibility that the desired
     * <code>width=left+size+right</code> may be less than <code>this.size</code>. This method does
     * all the work, and deals with that exceptional case by returning <code>self[:]</code>.
     *
     * @param pad byte to fill with
     * @param left padding requested
     * @param right padding requested
     * @return (possibly new) byte array containing the result
     */
    private BaseBytes padHelper(byte pad, int left, int right) {

        if (left + right <= 0) {
            // Deal here with the case wher width <= size, and no padding is necessary.
            // If this is immutable getslice may return this same object
            return getslice(0, size);
        }

        // Construct the result in a Builder of the desired width
        Builder builder = getBuilder(left + size + right);
        builder.repeat(pad, left);
        builder.append(this);
        builder.repeat(pad, right);
        return builder.getResult();
    }

    /**
     * A ready-to-expose implementation of Python <code>center(width [, fillchar])</code>: return
     * the bytes centered in an array of length <code>width</code>. Padding is done using the
     * specified fillchar (default is a space). A copy of the original byte array is returned if
     * <code>width</code> is less than <code>this.size()</code>. (Immutable subclasses may return
     * exactly the original object.)
     *
     * @param width desired
     * @param fillchar one-byte String to fill with, or <code>null</code> implying space
     * @return (possibly new) byte array containing the result
     */
    final BaseBytes basebytes_center(int width, String fillchar) {
        // Argument check and default
        byte pad = fillByteCheck("center", fillchar);
        // How many pads will I need?
        int fill = width - size;
        // CPython uses this formula, which makes a difference when width is odd and size even
        int left = fill / 2 + (fill & width & 1);
        return padHelper(pad, left, fill - left);
    }

    /**
     * A ready-to-expose implementation of Python <code>ljust(width [, fillchar])</code>: return the
     * bytes left-justified in an array of length <code>width</code>. Padding is done using the
     * specified fillchar (default is a space). A copy of the original byte array is returned if
     * <code>width</code> is less than <code>this.size()</code>. (Immutable subclasses may return
     * exactly the original object.)
     *
     * @param width desired
     * @param fillchar one-byte String to fill with, or <code>null</code> implying space
     * @return (possibly new) byte array containing the result
     */
    final BaseBytes basebytes_ljust(int width, String fillchar) {
        // Argument check and default
        byte pad = fillByteCheck("rjust", fillchar);
        // How many pads will I need?
        int fill = width - size;
        return padHelper(pad, 0, fill);
    }

    /**
     * A ready-to-expose implementation of Python <code>rjust(width [, fillchar])</code>: return the
     * bytes right-justified in an array of length <code>width</code>. Padding is done using the
     * specified fillchar (default is a space). A copy of the original byte array is returned if
     * <code>width</code> is less than <code>this.size()</code>. (Immutable subclasses may return
     * exactly the original object.)
     *
     * @param width desired
     * @param fillchar one-byte String to fill with, or <code>null</code> implying space
     * @return (possibly new) byte array containing the result
     */
    final BaseBytes basebytes_rjust(int width, String fillchar) {
        // Argument check and default
        byte pad = fillByteCheck("rjust", fillchar);
        // How many pads will I need?
        int fill = width - size;
        return padHelper(pad, fill, 0);
    }

    /**
     * Ready-to-expose implementation of Python <code>expandtabs([tabsize])</code>: return a copy of
     * the byte array where all tab characters are replaced by one or more spaces, depending on the
     * current column and the given tab size. The column number is reset to zero after each newline
     * occurring in the array. This treats other non-printing characters or escape sequences as
     * regular characters.
     * <p>
     * The actual class of the returned object is determined by {@link #getBuilder(int)}.
     *
     * @param tabsize number of character positions between tab stops
     * @return copy of this byte array with tabs expanded
     */
    final BaseBytes basebytes_expandtabs(int tabsize) {
        // We could only work out the true size by doing the work twice,
        // so make a guess and let the Builder re-size if it's not enough.
        int estimatedSize = size + size / 8;
        Builder builder = getBuilder(estimatedSize);

        int carriagePosition = 0;
        int limit = offset + size;

        for (int i = offset; i < limit; i++) {
            byte c = storage[i];
            if (c == '\t') {
                // Number of spaces is 1..tabsize
                int spaces = tabsize - carriagePosition % tabsize;
                builder.repeat((byte)' ', spaces);
                carriagePosition += spaces;
            } else {
                // Transfer the character, but if it is a line break, reset the carriage
                builder.append(c);
                carriagePosition = (c == '\n' || c == '\r') ? 0 : carriagePosition + 1;
            }
        }

        return builder.getResult();
    }

    /**
     * Ready-to-expose implementation of Python <code>zfill(width):</code> return the numeric string
     * left filled with zeros in a byte array of length width. A sign prefix is handled correctly if
     * it is in the first byte. A copy of the original is returned if width is less than the current
     * size of the array.
     *
     * @param width desired
     * @return left-filled byte array
     */
    final BaseBytes basebytes_zfill(int width) {
        // How many zeros will I need?
        int fill = width - size;
        Builder builder = getBuilder((fill > 0) ? width : size);

        if (fill <= 0) {
            // width <= size so result is just a copy of this array
            builder.append(this);
        } else {
            // At least one zero must be added. Transfer the sign byte (if any) first.
            int p = 0;
            if (size > 0) {
                byte sign = storage[offset];
                if (sign == '-' || sign == '+') {
                    builder.append(sign);
                    p = 1;
                }
            }
            // Now insert enough zeros
            builder.repeat((byte)'0', fill);
            // And finally the numeric part. Note possibility of no text eg. ''.zfill(6).
            if (size > p) {
                builder.append(this, p, size);
            }
        }
        return builder.getResult();
    }

    //
    // Character class operations
    //


    // Bit to twiddle (XOR) for lowercase letter to uppercase and vice-versa.
    private static final int SWAP_CASE = 0x20;

    // Bit masks and sets to use with the byte classification table
    private static final byte UPPER = 0b1;
    private static final byte LOWER = 0b10;
    private static final byte DIGIT = 0b100;
    private static final byte SPACE = 0b1000;
    private static final byte ALPHA = UPPER | LOWER;
    private static final byte ALNUM = ALPHA | DIGIT;

    // Character (byte) classification table.
    private static final byte[] ctype = new byte[256];
    static {
        for (int c = 'A'; c <= 'Z'; c++) {
            ctype[0x80 + c] = UPPER;
            ctype[0x80 + SWAP_CASE + c] = LOWER;
        }
        for (int c = '0'; c <= '9'; c++) {
            ctype[0x80 + c] = DIGIT;
        }
        for (char c : " \t\n\u000b\f\r".toCharArray()) {
            ctype[0x80 + c] = SPACE;
        }
    }

    /** @return 'A'<= b <='Z'. */
    static final boolean isupper(byte b) {
        return (ctype[0x80 + b] & UPPER) != 0;
    }

    /** @return 'a'<= b <='z'. */
    static final boolean islower(byte b) {
        return (ctype[0x80 + b] & LOWER) != 0;
    }

    /** @return 'A'<= b <='Z' or 'a'<= b <='z'. */
    static final boolean isalpha(byte b) {
        return (ctype[0x80 + b] & ALPHA) != 0;
    }

    /** @return '0'<= b <='9'. */
    static final boolean isdigit(byte b) {
        return (ctype[0x80 + b] & DIGIT) != 0;
    }

    /** @return 'A'<= b <='Z' or 'a'<= b <='z' or '0'<= b <='9'. */
    static final boolean isalnum(byte b) {
        return (ctype[0x80 + b] & ALNUM) != 0;
    }

    /** @return b in ' \t\n\v\f\r' */
    static final boolean isspace(byte b) {
        return (ctype[0x80 + b] & SPACE) != 0;
    }

    /**
     * Java API equivalent of Python <code>isalnum()</code>. This method treats the bytes as
     * US-ASCII code points.
     *
     * @return true if all bytes in the array are code points for alphanumerics and there is at
     *         least one byte, false otherwise.
     */
    public boolean isalnum() {
        return basebytes_isalnum();
    }

    /**
     * Ready-to-expose implementation of Python <code>isalnum()</code>.
     *
     * @return true if all bytes in the array are code points for alphanumerics and there is at
     *         least one byte, false otherwise.
     */
    final boolean basebytes_isalnum() {
        if (size == 1) {
            // Special case strings of length one (i.e. characters)
            return isalnum(storage[offset]);
        } else {
            // Work through the bytes, stopping early if the test is false.
            for (int i = 0; i < size; i++) {
                if (!isalnum(storage[offset + i])) {
                    return false;
                }
            }
            // Result is true if we reached the end (and there were some bytes)
            return size > 0;
        }
    }

    /**
     * Java API equivalent of Python <code>isalpha()</code>. This method treats the bytes as
     * US-ASCII code points.
     *
     * @return true if all bytes in the array are alphabetic and there is at least one byte, false
     *         otherwise
     */
    public boolean isalpha() {
        return basebytes_isalpha();
    }

    /**
     * Ready-to-expose implementation of Python <code>isalpha()</code>.
     *
     * @return true if all bytes in the array are alphabetic and there is at least one byte, false
     *         otherwise
     */
    final boolean basebytes_isalpha() {
        if (size == 1) {
            // Special case strings of length one (i.e. characters)
            return isalpha(storage[offset]);
        } else {
            // Work through the bytes, stopping early if the test is false.
            for (int i = 0; i < size; i++) {
                if (!isalpha(storage[offset + i])) {
                    return false;
                }
            }
            // Result is true if we reached the end (and there were some bytes)
            return size > 0;
        }
    }

    /**
     * Java API equivalent of Python <code>isdigit()</code>. This method treats the bytes as
     * US-ASCII code points.
     *
     * @return true if all bytes in the array are code points for digits and there is at least one
     *         byte, false otherwise.
     */
    public boolean isdigit() {
        return basebytes_isdigit();
    }

    /**
     * Ready-to-expose implementation of Python <code>isdigit()</code>.
     *
     * @return true if all bytes in the array are code points for digits and there is at least one
     *         byte, false otherwise.
     */
    final boolean basebytes_isdigit() {
        if (size == 1) {
            // Special case strings of length one (i.e. characters)
            return isdigit(storage[offset]);
        } else {
            // Work through the bytes, stopping early if the test is false.
            for (int i = 0; i < size; i++) {
                if (!isdigit(storage[offset + i])) {
                    return false;
                }
            }
            // Result is true if we reached the end (and there were some bytes)
            return size > 0;
        }
    }

    /**
     * Java API equivalent of Python <code>islower()</code>. This method treats the bytes as
     * US-ASCII code points.
     *
     * @return true if all cased bytes in the array are code points for lowercase characters and
     *         there is at least one cased byte, false otherwise.
     */
    public boolean islower() {
        return basebytes_islower();
    }

    /**
     * Ready-to-expose implementation of Python <code>islower()</code>.
     *
     * @return true if all cased bytes in the array are code points for lowercase characters and
     *         there is at least one cased byte, false otherwise.
     */
    final boolean basebytes_islower() {
        if (size == 1) {
            // Special case strings of length one (i.e. characters)
            return islower(storage[offset]);

        } else {
            int i;
            byte c = 0;
            // Test the bytes until a cased byte is encountered
            for (i = 0; i < size; i++) {
                if (isalpha(c = storage[offset + i])) {
                    break;
                }
            }

            if (i == size || isupper(c)) {
                // We reached the end without finding a cased byte, or it was upper case.
                return false;
            }

            // Continue to end or until an upper case byte is encountered
            for (i = i + 1; i < size; i++) {
                if (isupper(storage[offset + i])) {
                    return false;
                }
            }

            // Found no upper case bytes, and at least one lower case byte.
            return true;
        }
    }

    /**
     * Java API equivalent of Python <code>isspace()</code>. This method treats the bytes as
     * US-ASCII code points.
     *
     * @return true if all the bytes in the array are code points for whitespace characters and
     *         there is at least one byte, false otherwise.
     */
    public boolean isspace() {
        return basebytes_isspace();
    }

    /**
     * Ready-to-expose implementation of Python <code>isspace()</code>.
     *
     * @return true if all the bytes in the array are code points for whitespace characters and
     *         there is at least one byte, false otherwise.
     */
    final boolean basebytes_isspace() {
        if (size == 1) {
            // Special case strings of length one (i.e. characters)
            return isspace(storage[offset]);
        } else {
            // Work through the bytes, stopping early if the test is false.
            for (int i = 0; i < size; i++) {
                if (!isspace(storage[offset + i])) {
                    return false;
                }
            }
            // Result is true if we reached the end (and there were some bytes)
            return size > 0;
        }
    }

    /**
     * Java API equivalent of Python <code>istitle()</code>. This method treats the bytes as
     * US-ASCII code points.
     *
     * @return true if the string is a titlecased string and there is at least one cased byte, for
     *         example uppercase characters may only follow uncased bytes and lowercase characters
     *         only cased ones. Return false otherwise.
     */
    public boolean istitle() {
        return basebytes_istitle();
    }

    /**
     * Ready-to-expose implementation of Python <code>istitle()</code>.
     *
     * @return true if the string is a titlecased string and there is at least one cased byte, for
     *         example uppercase characters may only follow uncased bytes and lowercase characters
     *         only cased ones. Return false otherwise.
     */
    final boolean basebytes_istitle() {

        int state = 0;
        // 0 = have seen no cased characters (can't be in a word)
        // 1 = have seen cased character, but am not in a word
        // 2 = in a word (hence have have seen cased character)

        for (int i = 0; i < size; i++) {
            byte c = storage[offset+i];
            if (isupper(c)) {
                if (state == 2) {
                    // Violation: can't continue a word in upper case
                    return false;
                } else {
                    // Validly in a word
                    state = 2;
                }
            } else if (islower(c)) {
                if (state != 2) {
                    // Violation: can't start a word in lower case
                    return false;
                }
            } else {
                if (state == 2) {
                    // Uncased character: end of the word as we know it
                    state = 1;
                }
            }
        }
        // Found no case violations, but did we find any cased bytes at all?
        return state != 0;
    }

    /**
     * Java API equivalent of Python <code>isupper()</code>. This method treats the bytes as
     * US-ASCII code points.
     *
     * @return true if all cased bytes in the array are code points for uppercase characters and
     *         there is at least one cased byte, false otherwise.
     */
    public boolean isupper() {
        return basebytes_isupper();
    }

    /**
     * Ready-to-expose implementation of Python <code>isupper()</code>.
     *
     * @return true if all cased bytes in the array are code points for uppercase characters and
     *         there is at least one cased byte, false otherwise.
     */
    final boolean basebytes_isupper() {
        if (size == 1) {
            // Special case strings of length one (i.e. characters)
            return isupper(storage[offset]);

        } else {
            int i;
            byte c = 0;
            // Test the bytes until a cased byte is encountered
            for (i = 0; i < size; i++) {
                if (isalpha(c = storage[offset + i])) {
                    break;
                }
            }

            if (i == size || islower(c)) {
                // We reached the end without finding a cased byte, or it was lower case.
                return false;
            }

            // Continue to end or until a lower case byte is encountered
            for (i = i + 1; i < size; i++) {
                if (islower(storage[offset + i])) {
                    return false;
                }
            }

            // Found no lower case bytes, and at least one upper case byte.
            return true;
        }
    }

    //
    // Case transformations
    //

    /**
     * Java API equivalent of Python <code>capitalize()</code>. This method treats the bytes as
     * US-ASCII code points. The <code>BaseBytes</code> returned by this method has the same actual
     * type as <code>this/self</code>.
     *
     * @return a copy of the array with its first character capitalized and the rest lowercased.
     */
    public BaseBytes capitalize() {
        return basebytes_capitalize();
    }

    /**
     * Ready-to-expose implementation of Python <code>capitalize()</code>. The
     * <code>BaseBytes</code> returned by this method has the same actual type as
     * <code>this/self</code>.
     *
     * @return a copy of the array with its first character capitalized and the rest lowercased.
     */
    final BaseBytes basebytes_capitalize() {

        Builder builder = getBuilder(size);

        if (size > 0) {
            // Treat first character
            byte c = storage[offset];
            if (islower(c)) {
                c ^= SWAP_CASE;         // 'a' -> 'A', etc.
            }
            // Put the adjusted character in the output as a byte
            builder.append(c);

            // Treat the rest
            for (int i = 1; i < size; i++) {
                c = storage[offset+i];
                if (isupper(c)) {
                    c ^= SWAP_CASE;     // 'A' -> 'a', etc.
                }
                // Put the adjusted character in the output as a byte
                builder.append(c);
            }
        }

        return builder.getResult();
    }

    /**
     * Java API equivalent of Python <code>lower()</code>. This method treats the bytes as US-ASCII
     * code points. The <code>BaseBytes</code> returned by this method has the same actual type as
     * <code>this/self</code>.
     *
     * @return a copy of the array with all the cased characters converted to lowercase.
     */
    public BaseBytes lower() {
        return basebytes_lower();
    }

    /**
     * Ready-to-expose implementation of Python <code>lower()</code>. The <code>BaseBytes</code>
     * returned by this method has the same actual type as <code>this/self</code>.
     *
     * @return a copy of the array with all the cased characters converted to lowercase.
     */
    final BaseBytes basebytes_lower() {

        Builder builder = getBuilder(size);

        for (int i = 0; i < size; i++) {
            byte c = storage[offset+i];
            if (isupper(c)) {
                c ^= SWAP_CASE;     // 'A' -> 'a', etc.
            }
            // Put the adjusted character in the output as a byte
            builder.append(c);
        }

        return builder.getResult();
    }

    /**
     * Java API equivalent of Python <code>swapcase()</code>. This method treats the bytes as
     * US-ASCII code points. The <code>BaseBytes</code> returned by this method has the same actual
     * type as <code>this/self</code>.
     *
     * @return a copy of the array with uppercase characters converted to lowercase and vice versa.
     */
    public BaseBytes swapcase() {
        return basebytes_swapcase();
    }

    /**
     * Ready-to-expose implementation of Python <code>swapcase()</code>. The <code>BaseBytes</code>
     * returned by this method has the same actual type as <code>this/self</code>.
     *
     * @return a copy of the array with uppercase characters converted to lowercase and vice versa.
     */
    final BaseBytes basebytes_swapcase() {

        Builder builder = getBuilder(size);

        for (int i = 0; i < size; i++) {
            byte c = storage[offset+i];
            if (isalpha(c)) {
                c ^= SWAP_CASE;     // 'a' -> 'A', 'A' -> 'a', etc.
            }
            // Put the adjusted character in the output as a byte
            builder.append(c);
        }

        return builder.getResult();
    }

    /**
     * Java API equivalent of Python <code>title()</code>. The algorithm uses a simple
     * language-independent definition of a word as groups of consecutive letters. The definition
     * works in many contexts but it means that apostrophes in contractions and possessives form
     * word boundaries, which may not be the desired result. The <code>BaseBytes</code> returned by
     * this method has the same actual type as <code>this/self</code>.
     *
     * @return a titlecased version of the array where words start with an uppercase character and
     *         the remaining characters are lowercase.
     */
    public BaseBytes title() {
        return basebytes_title();
    }

    /**
     * Ready-to-expose implementation of Python <code>title()</code>. The <code>BaseBytes</code>
     * returned by this method has the same actual type as <code>this/self</code>.
     *
     * @return a titlecased version of the array where words start with an uppercase character and
     *         the remaining characters are lowercase.
     */
    final BaseBytes basebytes_title() {

        Builder builder = getBuilder(size);
        boolean inWord = false; // We begin, not in a word (sequence of cased characters)

        for (int i = 0; i < size; i++) {
            byte c = storage[offset+i];

            if (!inWord) {
                // When we are not in a word ...
                if (islower(c)) {
                    c ^= SWAP_CASE;                 // ... a lowercase letter must be upcased
                    inWord = true;                  // and it starts a word.
                } else if (isupper(c)) {
                    inWord = true;                  // ... an uppercase letter just starts the word
                }

            } else {
                // When we are in a word ...
                if (isupper(c)) {
                    c ^= SWAP_CASE;                 // ... an uppercase letter must be downcased
                } else if (!islower(c)) {
                    inWord = false;                 // ... and a non-letter ends the word
                }
            }
            // Put the adjusted character in the output as a byte
            builder.append(c);
        }
        return builder.getResult();
    }

    /**
     * Java API equivalent of Python <code>upper()</code>. Note that
     * <code>x.upper().isupper()</code> might be <code>false</code> if the array contains uncased
     * characters. The <code>BaseBytes</code> returned by this method has the same actual type as
     * <code>this/self</code>.
     *
     * @return a copy of the array with all the cased characters converted to uppercase.
     */
    public BaseBytes upper() {
        return basebytes_upper();
    }

    /**
     * Ready-to-expose implementation of Python <code>upper()</code>. The <code>BaseBytes</code>
     * returned by this method has the same actual type as <code>this/self</code>.
     *
     * @return a copy of the array with all the cased characters converted to uppercase.
     */
    final BaseBytes basebytes_upper() {

        Builder builder = getBuilder(size);

        for (int i = 0; i < size; i++) {
            byte c = storage[offset+i];
            if (islower(c)) {
                c ^= SWAP_CASE;     // 'a' -> 'A' etc.
            }
            // Put the adjusted character in the output as a byte
            builder.append(c);
        }

        return builder.getResult();
    }

    /*
     * ============================================================================================
     * Java API for access as byte[]
     * ============================================================================================
     *
     * Just the immutable case for now
     */

    /**
     * No range check access to byte[index].
     *
     * @param index
     * @return the byte at the given index
     */
    private final synchronized byte byteAt(int index) {
        return storage[index + offset];
    }

    /**
     * Return the Python byte (in range 0 to 255 inclusive) at the given index.
     *
     * @param index of value in byte array
     * @return the integer value at the index
     * @throws PyException (IndexError) if the index is outside the array bounds
     */
    public synchronized int intAt(int index) throws PyException {
        indexCheck(index);
        return 0xff & byteAt(index);
    }

    /**
     * Helper to implement {@link #repeat(int)}. Use something like:
     *
     * <pre>
     * &#064;Override
     * protected PyByteArray repeat(int count) {
     *     PyByteArray ret = new PyByteArray();
     *     ret.setStorage(repeatImpl(count));
     *     return ret;
     * }
     * </pre>
     *
     * @param count the number of times to repeat this.
     * @return this byte array repeated count times.
     */
    protected synchronized byte[] repeatImpl(int count) {
        if (count <= 0) {
            return emptyStorage;
        } else {
            // Allocate new storage, in a guarded way
            long newSize = ((long)count) * size;
            byte[] dst;
            try {
                dst = new byte[(int)newSize];
            } catch (OutOfMemoryError e) {
                throw Py.MemoryError(e.getMessage());
            }
            // Now fill with the repetitions needed
            for (int i = 0, p = 0; i < count; i++, p += size) {
                System.arraycopy(storage, offset, dst, p, size);
            }
            return dst;
        }
    }

    //
    // str() and repr() have different behaviour (despite PEP 3137)
    //

    /**
     * Helper for __repr__()
     *
     * @param buf destination for characters
     * @param c curren (maybe unprintable) character
     */
    private static final void appendHexEscape(StringBuilder buf, int c) {
        buf.append("\\x").append(Character.forDigit((c & 0xf0) >> 4, 16))
                .append(Character.forDigit(c & 0xf, 16));
    }

    /**
     * Almost ready-to-expose Python <code>__repr__()</code>, based on treating the bytes as point
     * codes. The value added by this method is conversion of non-printing code points to
     * hexadecimal escapes in printable ASCII, and bracketed by the given before and after strings.
     * These are used to get the required presentation:
     *
     * <pre>
     * bytearray(b'Hello world!')
     * </pre>
     *
     * with the possibility that subclasses might choose something different.
     *
     * @param before String to insert before the quoted text
     * @param after String to insert after the quoted text
     * @return string representation: <code>before + "'" + String(this) + "'" + after</code>
     */
    final synchronized String basebytes_repr(String before, String after) {

        // Safety
        if (before == null) {
            before = "";
        }
        if (after == null) {
            after = "";
        }

        // Guess how long the result might be
        int guess = size + (size >> 2) + before.length() + after.length() + 10;
        StringBuilder buf = new StringBuilder(guess);
        buf.append(before).append('\'');

        // Scan and translate the bytes of the array
        int jmax = offset + size;
        for (int j = offset; j < jmax; j++) {
            int c = 0xff & storage[j];
            if (c >= 0x7f) {    // Unprintable high 128 and DEL
                appendHexEscape(buf, c);
            } else if (c >= ' ') { // Printable
                if (c == '\\' || c == '\'') {    // Special cases
                    buf.append('\\');
                }
                buf.append((char)c);
            } else if (c == '\t') { // Special cases in the low 32
                buf.append("\\t");
            } else if (c == '\n') {
                buf.append("\\n");
            } else if (c == '\r') {
                buf.append("\\r");
            } else {
                appendHexEscape(buf, c);
            }
        }

        buf.append('\'').append(after);
        return buf.toString();
    }

    /*
     * ============================================================================================
     * API for java.util.List<PyInteger>
     * ============================================================================================
     */

    /**
     * Access to the byte array as a {@link java.util.List}. The List interface supplied by
     * BaseBytes delegates to this object.
     */
    protected final List<PyInteger> listDelegate = new AbstractList<PyInteger>() {

        @Override
        public PyInteger get(int index) {
            // Not using __getitem__ as it applies Python index semantics to e.g. b[-1].
            indexCheck(index);
            return pyget(index);
        }

        @Override
        public int size() {
            return size;
        }

        // For mutable subclass use

        /**
         * Replaces the element at the specified position in this list with the specified element.
         *
         * @see java.util.AbstractList#set(int, java.lang.Object)
         * @throws PyException (TypeError) if actual class is immutable
         * @throws PyException (IndexError) if the index is outside the array bounds
         * @throws PyException (ValueError) if element<0 or element>255
         */
        @Override
        public PyInteger set(int index, PyInteger element) throws PyException {
            // Not using __setitem__ as it applies Python index semantics to e.g. b[-1].
            indexCheck(index);
            PyInteger result = pyget(index);
            pyset(index, element);      // TypeError if immutable
            return result;
        }

        /**
         * Inserts the specified element at the specified position in this list. Shifts the element
         * currently at that position and any subsequent elements to the right.
         *
         * @see java.util.AbstractList#add(int, java.lang.Object)
         * @throws PyException (IndexError) if the index is outside the array bounds
         * @throws PyException (ValueError) if element<0 or element>255
         * @throws PyException (TypeError) if the owning concrete subclass is immutable
         */
        @Override
        public void add(int index, PyInteger element) throws PyException {
            // Not using __setitem__ as it applies Python index semantics to e.g. b[-1].
            indexCheck(index);
            pyinsert(index, element);          // TypeError if immutable
        }

        /**
         * Removes the element at the specified position in this list. Shifts any subsequent
         * elements to the left (subtracts one from their indices). Returns the element that was
         * removed from the list.
         *
         * @see java.util.AbstractList#remove(int)
         * @throws PyException (IndexError) if the index is outside the array bounds
         */
        @Override
        public PyInteger remove(int index) {
            // Not using __delitem__ as it applies Python index semantics to e.g. b[-1].
            indexCheck(index);
            PyInteger result = pyget(index);
            del(index);      // TypeError if immutable
            return result;
        }
    };

    /**
     * Number of bytes in <code>bytearray</code> (or <code>bytes</code>) object.
     *
     * @see java.util.List#size()
     * @return Number of bytes in byte array.
     * */
    @Override
    public int size() {
        return size;
    }

    /*
     * @see java.util.List#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns true if this list contains the specified value. More formally, returns true if and
     * only if this list contains at least one integer e such that o.equals(PyInteger(e)).
     */
    @Override
    public boolean contains(Object o) {
        return listDelegate.contains(o);
    }

    /*
     * @see java.util.List#iterator()
     */
    @Override
    public Iterator<PyInteger> iterator() {
        return listDelegate.iterator();
    }

    /*
     * @see java.util.List#toArray()
     */
    @Override
    public Object[] toArray() {
        return listDelegate.toArray();
    }

    /*
     * @see java.util.List#toArray(T[])
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return listDelegate.toArray(a);
    }

    /*
     * @see java.util.List#add(java.lang.Object)
     */
    @Override
    public boolean add(PyInteger o) {
        return listDelegate.add(o);
    }

    /*
     * @see java.util.List#remove(java.lang.Object)
     */
    @Override
    public boolean remove(Object o) {
        return listDelegate.remove(o);
    }

    /*
     * @see java.util.List#containsAll(java.util.Collection)
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        return listDelegate.containsAll(c);
    }

    /*
     * @see java.util.List#addAll(java.util.Collection)
     */
    @Override
    public boolean addAll(Collection<? extends PyInteger> c) {
        return listDelegate.addAll(c);
    }

    /*
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    @Override
    public boolean addAll(int index, Collection<? extends PyInteger> c) {
        return listDelegate.addAll(index, c);
    }

    /*
     * @see java.util.List#removeAll(java.util.Collection)
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        return listDelegate.removeAll(c);
    }

    /*
     * @see java.util.List#retainAll(java.util.Collection)
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        return listDelegate.retainAll(c);
    }

    /*
     * @see java.util.List#clear()
     */
    @Override
    public void clear() {
        listDelegate.clear();
    }

    /**
     * Test for the equality of (the value of) this byte array to the object <code>other</code>. In
     * the case where <code>other</code> is a <code>PyObject</code>, the comparison used is the
     * standard Python <code>==</code> operation through <code>PyObject</code>. When
     * <code>other</code> is not a <code>PyObject</code>, this object acts as a
     * <code>List&lt;PyInteger></code>.
     *
     * @see java.util.List#equals(java.lang.Object)
     *
     * @param other object to compare this byte array to
     * @return <code>true</code> if and only if this byte array is equal (in value) to
     *         <code>other</code>
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        } else if (other instanceof PyObject) {
            return super.equals(other);
        } else {
            return listDelegate.equals(other);
        }
    }

    /*
     * @see java.util.List#hashCode()
     */
    @Override
    public int hashCode() {
        return listDelegate.hashCode();
    }

    /*
     * @see java.util.List#get(int)
     */
    @Override
    public PyInteger get(int index) {
        return listDelegate.get(index);
    }

    /*
     * @see java.util.List#set(int, java.lang.Object)
     */
    @Override
    public PyInteger set(int index, PyInteger element) {
        return listDelegate.set(index, element);
    }

    /*
     * @see java.util.List#add(int, java.lang.Object)
     */
    @Override
    public void add(int index, PyInteger element) {
        listDelegate.add(index, element);
    }

    /*
     * @see java.util.List#remove(int)
     */
    @Override
    public PyInteger remove(int index) {
        return listDelegate.remove(index);
    }

    /*
     * @see java.util.List#indexOf(java.lang.Object)
     */
    @Override
    public int indexOf(Object o) {
        return listDelegate.indexOf(o);
    }

    /*
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    @Override
    public int lastIndexOf(Object o) {
        return listDelegate.lastIndexOf(o);
    }

    /*
     * @see java.util.List#listIterator()
     */
    @Override
    public ListIterator<PyInteger> listIterator() {
        return listDelegate.listIterator();
    }

    /*
     * @see java.util.List#listIterator(int)
     */
    @Override
    public ListIterator<PyInteger> listIterator(int index) {
        return listDelegate.listIterator(index);
    }

    /*
     * @see java.util.List#subList(int, int)
     */
    @Override
    public List<PyInteger> subList(int fromIndex, int toIndex) {
        return listDelegate.subList(fromIndex, toIndex);
    }

    /*
     * ============================================================================================
     * Support for Builder
     * ============================================================================================
     */

    /**
     * A <code>Builder</code> holds a buffer of bytes to which new bytes may be appended while
     * constructing the value of byte array, even when the type ultimately constructed is immutable.
     * The value it builds may be transferred (normally without copying) to a new instance of the
     * type being built.
     * <p>
     * <code>Builder</code> is an abstract class. The each sub-class of <code>BaseBytes</code> may
     * define its own concrete implementation in which {@link Builder#getResult()} returns an object
     * of its own type, taking its value from the <code>Builder</code> contents using
     * {@link #getStorage()} and {@link #getSize()}. Methods in <code>BaseBytes</code> obtain a
     * <code>Builder</code> by calling the abstract method {@link BaseBytes#getBuilder(int)}, which
     * the sub-class also defines, to return an isnstance of its characteristic <code>Builder</code>
     * sub-class. The subclass that uses a method from <code>BaseBytes</code> returning a
     * <code>BaseBytes</code> has to cast a returned from a BaseBytes method to its proper type.
     * which it can do without error, since it was responsible for its actual type.
     * <p>
     * <b>Implementation note:</b> This can be done in a type-safe way but, in the present design,
     * only by making <code>BaseBytes</code> parameterised class.
     *
     */
    protected static abstract class Builder /* <B> */{

        /**
         * Return an object of type B extends <code>BaseBytes</code> whose content is what we built.
         */
        abstract BaseBytes getResult();

        // Internal state
        private byte[] storage = emptyStorage;
        private int size = 0;

        /**
         * Construct a builder with specified initial capacity.
         *
         * @param capacity
         */
        Builder(int capacity) {
            makeRoomFor(capacity);
        }

        /**
         * Get an array of bytes containing the accumulated value, and clear the existing contents
         * of the Builder. {@link #getCount()} returns the number of valid bytes in this array,
         * which may be longer than the valid data.
         * <p>
         * It is intended the client call this method only once to get the result of a series of
         * append operations. A second call to {@link #getCount()}, before any further appending,
         * returns a zero-length array. This is to ensure that the same array is not given out
         * twice. However, {@link #getCount()} continues to return the number bytes accumulated
         * until an append next occurs.
         *
         * @return an array containing the accumulated result
         */
        byte[] getStorage() {
            byte[] s = storage;
            storage = emptyStorage;
            return s;
        }

        /**
         * Number of bytes accumulated. In conjunctin with {@link #getStorage()}, this provides the
         * result. Unlike {@link #getStorage()}, it does not affect the contents.
         *
         * @return number of bytes accumulated
         */
        final int getSize() {
            return size;
        }

        /**
         * Append a single byte to the value.
         *
         * @param b
         */
        void append(byte b) {
            makeRoomFor(1);
            storage[size++] = b;
        }

        /**
         * Append a number of repeats of a single byte to the value, fo example in padding.
         *
         * @param b byte to repeat
         * @param n number of repeats (none if n<=0)
         */
        void repeat(byte b, int n) {
            if (n > 0) {
                makeRoomFor(n);
                while (n-- > 0) {
                    storage[size++] = b;
                }
            }
        }

        /**
         * Append the contents of the given byte array.
         *
         * @param b
         */
        void append(BaseBytes b) {
            append(b, 0, b.size);
        }

        /**
         * Append the contents of a slice of the given byte array.
         *
         * @param b
         * @param start index of first byte copied
         * @param end index of fisrt byte not copied
         */
        void append(BaseBytes b, int start, int end) {
            int n = end - start;
            makeRoomFor(n);
            System.arraycopy(b.storage, b.offset + start, storage, size, n);
            size += n;
        }

        /**
         * Append the contents of the given {@link PyBuffer}.
         *
         * @param b
         */
        void append(PyBuffer v) {
            int n = v.getLen();
            makeRoomFor(n);
            v.copyTo(storage, size);
            size += n;
        }

        // Ensure there is enough free space for n bytes (or allocate some)
        void makeRoomFor(int n) throws PyException {
            int needed = size + n;
            if (needed > storage.length) {
                try {
                    if (storage == emptyStorage) {
                        /*
                         * After getStorage(): size deliberately retains its prior value, even
                         * though storage is set to emptyStorage. However, the first (non-empty)
                         * append() operation after that lands us here, because storage.length==0.
                         */
                        size = 0;
                        if (n > 0) {
                            // When previously empty (incluing the constructor) allocate exactly n.
                            storage = new byte[n];
                        }
                    } else {
                        // We are expanding an existing allocation: be imaginative
                        byte[] old = storage;
                        storage = new byte[roundUp(needed)];
                        System.arraycopy(old, 0, storage, 0, size);
                    }
                } catch (OutOfMemoryError e) {
                    /*
                     * MemoryError is right for most clients. Some (e.g. bytearray.replace()) should
                     * convert it to an overflow, with a customised message.
                     */
                    throw Py.MemoryError(e.getMessage());
                }
            }
        }
    }

    /**
     * Choose a size appropriate to store the given number of bytes, with some room for growth, when
     * allocating storage for mutable types or <code>Builder</code>. We'll be more generous than
     * CPython for small array sizes to avoid needless reallocation.
     *
     * @param size of storage actually needed
     * @return n >= size a recommended storage array size
     */
    protected static final int roundUp(int size) {
        /*
         * The CPython formula is: size + (size >> 3) + (size < 9 ? 3 : 6). But when the array
         * grows, CPython can use a realloc(), which will often be able to extend the allocation
         * into memory already secretly allocated by the initial malloc(). Extension in Java means
         * that we have to allocate a new array of bytes and copy to it.
         */
        final int ALLOC = 16;   // Must be power of two!
        final int SIZE2 = 10;   // Smallest size leading to a return value of 2*ALLOC
        if (size >= SIZE2) {       // Result > ALLOC, so work it out
            // Same recommendation as CPython, but rounded up to multiple of ALLOC
            return (size + (size >> 3) + (6 + ALLOC - 1)) & ~(ALLOC - 1);
        } else if (size > 0) {  // Easy: save arithmetic
            return ALLOC;
        } else {                // Very easy
            return 0;
        }
    }

    /**
     * Every sub-class of BaseBytes overrides this method to return a <code>Builder&lt;B></code>
     * where <code>B</code> is (normally) that class's particular type, and it extends
     * <code>Builder&lt;B></code> so that {@link Builder#getResult()} produces an instance of
     * <code>B</code> from the contents.
     *
     * @param capacity of the <code>Builder&lt;B></code> returned
     * @return a <code>Builder&lt;B></code> for the correct sub-class
     */
    protected abstract Builder/* <? extends BaseBytes> */getBuilder(int capacity);

}
