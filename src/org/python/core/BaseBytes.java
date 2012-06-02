package org.python.core;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Base class for Jython bytearray (and bytes in due course) that provides most of the Java API,
 * including Java List behaviour. Attempts to modify the contents through this API will throw a
 * TypeError if the actual type of the object is not mutable.
 * <p>
 * It is possible for a Java client to treat this class as a <tt>List&lt;PyInteger></tt>, obtaining
 * equivalent functionality to the Python interface in a Java paradigm.
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
 */
public abstract class BaseBytes extends PySequence implements MemoryViewProtocol, List<PyInteger> {

    /**
     * Simple constructor of empty zero-length array of defined type.
     *
     * @param type explicit Jython type
     */
    public BaseBytes(PyType type) {
        super(type);            // implicit setStorage( emptyStorage );
        setStorage(emptyStorage);
    }

    /**
     * Simple constructor of zero-filled array of defined size and type.
     *
     * @param size required
     * @param type explicit Jython type
     */
    public BaseBytes(PyType type, int size) {
        super(type);
        newStorage(size);
    }

    /**
     * Construct byte array of defined type by copying values from int[].
     *
     * @param type explicit Jython type
     * @param value source of values (and size)
     */
    public BaseBytes(PyType type, int[] value) {
        super(type);
        int n = value.length;
        newStorage(n);
        for (int i = offset, j = 0; j < n; i++, j++) {
            storage[i] = byteCheck(value[j]);
        }
    }

    /**
     * Construct byte array of defined type by copying character values from a String. These values
     * have to be in the Python byte range 0 to 255.
     *
     * @param type explicit Jython type
     * @param value source of characters
     * @throws PyException if any value[i] > 255
     */
    protected BaseBytes(PyType type, String value) throws PyException {
        super(type);
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
     * Support for memoryview
     * ============================================================================================
     *
     * This is present in order to facilitate development of PyMemoryView which a full
     * implementation of bytearray would depend on, while at the same time a full implementation of
     * memoryview depends on bytearray.
     */
    /**
     * Get hold of a <code>memoryview</code> on the current byte array.
     *
     * @see MemoryViewProtocol#getMemoryView()
     */
    @Override
    public MemoryView getMemoryView() {
        if (mv == null) {
            mv = new MemoryViewImpl();
        }
        return mv;
    }

    private MemoryView mv;

    /**
     * All instances of BaseBytes have one dimension with stride one.
     */
    private static final PyTuple STRIDES = new PyTuple(Py.One);

    /**
     * Very simple MemoryView for one-dimensional byte array. This lacks any actual access to the
     * underlying storage as the interface is not presently defined.
     */
    private class MemoryViewImpl implements MemoryView {

        private final PyTuple SHAPE = new PyTuple(new PyInteger(storage.length));

        @Override
        public String get_format() {
            return "B";
        }

        @Override
        public int get_itemsize() {
            return 1;
        }

        @Override
        public PyTuple get_shape() {
            return SHAPE;
        }

        @Override
        public int get_ndim() {
            return 1;
        }

        @Override
        public PyTuple get_strides() {
            return STRIDES;
        }

        @Override
        public boolean get_readonly() {
            return true;
        }

    }

    /*
     * ============================================================================================
     * Support for construction and initialisation
     * ============================================================================================
     *
     * Methods here help subclasses set the initial state. They are designed with bytearray in mind,
     * but note that from Python 3, bytes() has the same set of calls and behaviours. In
     * Peterson's "sort of backport" to Python 2.x, bytes is effectively an alias for str and it
     * shows.
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

        } else if (arg instanceof MemoryViewProtocol) {
            /*
             * bytearray copy of object supporting Jython implementation of PEP 3118
             */
            init(((MemoryViewProtocol)arg).getMemoryView());

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
     * @throws PyException(TypeError) if the PyString is actually a PyUnicode and encoding is null
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
     * @throws PyException(ValueError) if any value[i] > 255
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
     * @throws PyException(ValueError) if any value[i] > 255
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
     * int in subclasses. Construct zero-filled bytearray of specified size.
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
     * objects supporting the Jython implementation of PEP 3118 (memoryview) in subclasses.
     *
     * @param value a memoryview object consistent with the slice assignment
     * @throws PyException(NotImplementedError) until memoryview is properly supported
     * @throws PyException(TypeError) if the memoryview is not byte-oriented
     */
    protected void init(MemoryView value) throws PyException {
        // XXX Support memoryview once means of access to bytes is defined
        Py.NotImplementedError("memoryview not yet supported in bytearray");
        String format = value.get_format();
        boolean isBytes = format == null || "B".equals(format);
        if (value.get_ndim() != 1 || !isBytes) {
            Py.TypeError("memoryview value must be byte-oriented");
        } else {
            // Dimensions are given as a PyTuple (although only one)
            int len = value.get_shape().pyget(0).asInt();
            // XXX Access to memoryview bytes to go here
        }
    }

    /**
     * Helper for the Java API constructor from a {@link #View}. View is (perhaps) a stop-gap while
     * there is no Jython implementation of PEP 3118 (memoryview).
     *
     * @param value a byte-oriented view
     */
    void init(View value) {
        int n = value.size();
        newStorage(n);
        value.copyTo(storage, offset);
    }

    /**
     * Helper for <code>__new__</code> and <code>__init__</code> and the Java API constructor from
     * bytearray or bytes in subclasses.
     *
     * @param source bytearray (or bytes) to copy
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
         * @throws PyException(TypeError) if any value not acceptable type
         * @throws PyException(ValueError) if any value<0 or value>255 or string length!=1
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
     * @throws PyException(IndexError) if the index is outside the array bounds
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
     * @throws PyException(ValueError) if value<0 or value>255
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
     * @throws PyException(ValueError) if value<0 or value>255
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
     * @throws PyException(TypeError) if not acceptable type
     * @throws PyException(ValueError) if value<0 or value>255 or string length!=1
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

    /*
     * ============================================================================================
     * Wrapper class to make other objects into byte arrays
     * ============================================================================================
     *
     * In much of the bytearray and bytes API, the "other sequence" argument will accept any type
     * that supports the buffer protocol, that is, the object can supply a memoryview through which
     * the value is treated as a byte array. We have not implemented memoryview objects yet, and it
     * is not clear what the Java API should be. As a temporary expedient, we define here a
     * byte-oriented view on the key built-in types.
     */

    interface View {

        /**
         * Return the indexed byte as a byte
         *
         * @param index
         * @return byte at index
         */
        public byte byteAt(int index);

        /**
         * Return the indexed byte as an unsigned integer
         *
         * @param index
         * @return value of the byte at index
         */
        public int intAt(int index);

        /**
         * Number of bytes in the view: valid indexes are from <code>0</code> to
         * <code>size()-1</code>.
         *
         * @return the size
         */
        public int size();

        /**
         * Return a new view that is a simple slice of this one defined by <code>[start:end]</code>.
         * <code>Py.None</code> or <code>null</code> are acceptable for start and end, and have
         * Python slice semantics. Negative values for start or end are treated as "from the end",
         * in the usual manner of Python slices.
         *
         * @param start first element to include
         * @param end first element after slice, not to include
         * @return byte-oriented view
         */
        public View slice(PyObject start, PyObject end);

        /**
         * Copy the bytes of this view to the specified position in a destination array. All the
         * bytes of the View are copied.
         *
         * @param dest destination array
         * @param destPos index in the destination at which this.byteAt(0) is written
         * @throws ArrayIndexOutOfBoundsException if the destination is too small
         */
        public void copyTo(byte[] dest, int destPos) throws ArrayIndexOutOfBoundsException;

        /**
         * Test whether this View has the given prefix, that is, that the first bytes of this View
         * match all the bytes of the given prefix. By implication, the test returns false if there
         * are too few bytes in this view.
         *
         * @param prefix pattern to match
         * @return true if and only if this view has the given prefix
         */
        public boolean startswith(View prefix);

        /**
         * Test whether the slice <code>[offset:]</code> of this View has the given prefix, that is,
         * that the bytes of this View from index <code>offset</code> match all the bytes of the
         * give prefix. By implication, the test returns false if the offset puts the start or end
         * of the prefix outside this view (when <code>offset&lt;0</code> or
         * <code>offset+prefix.size()>size()</code>). Python slice semantics are <em>not</em>
         * applied to <code>offset</code>.
         *
         * @param prefix pattern to match
         * @param offset at which to start the comparison in this view
         * @return true if and only if the slice [offset:<code>]</code> this view has the given
         *         prefix
         */
        public boolean startswith(View prefix, int offset);

        /**
         * The standard memoryview out of bounds message (does not refer to the underlying type).
         */
        public static final String OUT_OF_BOUNDS = "index out of bounds";

    }

    /**
     * Some common apparatus for views including the implementation of slice semantics.
     */
    static abstract class ViewBase implements View {

        /**
         * Provides an implementation of {@link View#slice(PyObject, PyObject)} that implements
         * Python contiguous slice semantics so that sub-classes only receive simplified requests
         * involving properly-bounded integer arguments via {@link #sliceImpl(int, int)}, a call to
         * {@link #byteAt(int)}, if the slice has length 1, or in the extreme case of a zero length
         * slice, no call at all.
         */
        public View slice(PyObject ostart, PyObject oend) {
            PySlice s = new PySlice(ostart, oend, null);
            int[] index = s.indicesEx(size());  // [ start, end, 1, end-start ]
            int len = index[3];
            // Provide efficient substitute when length is zero or one
            if (len < 1) {
                return new ViewOfNothing();
            } else if (len == 1) {
                return new ViewOfByte(byteAt(index[0]));
            } else { // General case: delegate to sub-class
                return sliceImpl(index[0], index[1]);
            }
        }

        /**
         * Implementation-specific part of returning a slice of the current view. This is called by
         * the default implementations of {@link #slice(int, int)} and
         * {@link #slice(PyObject, PyObject)} once the <code>start</code> and <code>end</code>
         * arguments have been reduced to simple integer indexes. It is guaranteed that
         * <code>start>=0</code> and <code>size()>=end>=start+2</code> when the method is called.
         * View objects for slices of length zero and one are dealt with internally by the
         * {@link #slice(PyObject, PyObject)} method, see {@link ViewOfNothing} and
         * {@link ViewOfByte}. Implementors are encouraged to do something more efficient than
         * piling on another wrapper.
         *
         * @param start first element to include
         * @param end first element after slice, not to include
         * @return byte-oriented view
         */
        protected abstract View sliceImpl(int start, int end);

        /**
         * Copy the bytes of this view to the specified position in a destination array. All the
         * bytes of the View are copied. The Base implementation simply loops over byteAt().
         */
        public void copyTo(byte[] dest, int destPos) throws ArrayIndexOutOfBoundsException {
            int n = this.size(), p = destPos;
            for (int i = 0; i < n; i++) {
                dest[p++] = byteAt(i);
            }
        }

        /**
         * Test whether this View has the given prefix, that is, that the first bytes of this View
         * match all the bytes of the given prefix. This class provides an implementation of
         * {@link View#startswith(View)} that simply returns <code>startswith(prefix,0)</code>
         */
        @Override
        public boolean startswith(View prefix) {
            return startswith(prefix, 0);
        }

        /**
         * Test whether this View has the given prefix, that is, that the first bytes of this View
         * match all the bytes of the given prefix. This class provides an implementation of
         * {@link View#startswith(View,int)} that loops over
         * <code>byteAt(i+offset)==prefix.byteAt(i)</code>
         */
        @Override
        public boolean startswith(View prefix, int offset) {
            int j = offset; // index in this
            if (j < 0) {
                // // Start of prefix is outside this view
                return false;
            } else {
                int len = prefix.size();
                if (j + len > this.size()) {
                    // End of prefix is outside this view
                    return false;
                } else {
                    // Last resort: we have actually to look at the bytes!
                    for (int i = 0; i < len; i++) {
                        if (byteAt(j++) != prefix.byteAt(i)) {
                            return false;
                        }
                    }
                    return true; // They must all have matched
                }
            }
        }

    }

    /**
     * Return a wrapper providing a byte-oriented view for whatever object is passed, or return
     * <code>null</code> if we don't know how.
     *
     * @param b object to wrap
     * @return byte-oriented view or null
     */
    protected static View getView(PyObject b) {
        if (b == null) {
            return null;
        } else if (b instanceof BaseBytes) {
            BaseBytes bb = (BaseBytes)b;
            int len = bb.size;
            // Provide efficient substitute when length is zero or one
            if (len < 1) {
                return new ViewOfNothing();
            } else if (len == 1) {
                return new ViewOfByte(bb.byteAt(0));
            } else { // General case
                return new ViewOfBytes(bb);
            }
        } else if (b.getType() == PyString.TYPE) {
            String bs = b.asString();
            int len = bs.length();
            // Provide efficient substitute when length is zero
            if (len < 1) {
                return new ViewOfNothing();
            } else if (len == 1) {
                return new ViewOfByte(byteCheck(bs.charAt(0)));
            } else { // General case
                return new ViewOfString(bs);
            }
        }
        return null;
    }

    /**
     * Return a wrapper providing a byte-oriented view for a slice of whatever object is passed, or
     * return <code>null</code> if we don't know how.
     *
     * @param b object to wrap
     * @param start index of first byte to include
     * @param end index of first byte after slice
     * @return byte-oriented view or null
     */
    protected static View getView(PyObject b, PyObject start, PyObject end) {
        View whole = getView(b);
        if (whole != null) {
            return whole.slice(start, end);
        } else {
            return null;
        }
    }

    /**
     * Return a wrapper providing a byte-oriented view for whatever object is passed, or raise an
     * exception if we don't know how.
     *
     * @param b object to wrap
     * @return byte-oriented view
     */
    protected static View getViewOrError(PyObject b) {
        View res = getView(b);
        if (res == null) {
            String fmt = "cannot access type %s as bytes";
            throw Py.TypeError(String.format(fmt, b.getType().fastGetName()));
            // A more honest response here would have been:
            // . String fmt = "type %s doesn't support the buffer API"; // CPython
            // . throw Py.NotImplementedError(String.format(fmt, b.getType().fastGetName()));
            // since our inability to handle certain types is lack of a buffer API generally.
        }
        return res;
    }

    /**
     * Return a wrapper providing a byte-oriented view for a slice of whatever object is passed, or
     * raise an exception if we don't know how.
     *
     * @param b object to wrap
     * @param start index of first byte to include
     * @param end index of first byte after slice
     * @return byte-oriented view or null
     */
    protected static View getViewOrError(PyObject b, PyObject start, PyObject end) {
        View whole = getViewOrError(b);
        return whole.slice(start, end);
    }

    /**
     * Wrapper providing a byte-oriented view for String (or PyString).
     */
    protected static class ViewOfString extends ViewBase {

        private String str;

        /**
         * Create a byte-oriented view of a String.
         *
         * @param str
         */
        public ViewOfString(String str) {
            this.str = str;
        }

        public byte byteAt(int index) {
            return byteCheck(str.charAt(index));
        }

        public int intAt(int index) {
            return str.charAt(index);
        }

        public int size() {
            return str.length();
        }

        public View sliceImpl(int start, int end) {
            return new ViewOfString(str.substring(start, end));
        }

    }

    /**
     * Wrapper providing a byte-oriented view for byte arrays descended from BaseBytes. Not that
     * this view is not safe against concurrent modification by this or another thread: if the byte
     * array type is mutable, and the contents change, the contents of the view are likely to be
     * invalid.
     */
    protected static class ViewOfBytes extends ViewBase {

        private byte[] storage;
        private int offset;
        private int size;

        /**
         * Create a byte-oriented view of a byte array descended from BaseBytes.
         *
         * @param obj
         */
        public ViewOfBytes(BaseBytes obj) {
            this.storage = obj.storage;
            this.offset = obj.offset;
            this.size = obj.size;
        }

        /**
         * Create a byte-oriented view of a byte array explicitly. If the size<0, a zero-length
         * slice results.
         *
         * @param storage storage array
         * @param offset
         * @param size
         */
        ViewOfBytes(byte[] storage, int offset, int size) {
            if (size > 0) {
                this.storage = storage;
                this.offset = offset;
                this.size = size;
            } else {
                this.storage = emptyStorage;
                this.offset = 0;
                this.size = 0;
            }
        }

        public byte byteAt(int index) {
            return storage[offset + index];
        }

        public int intAt(int index) {
            return 0xff & storage[offset + index];
        }

        public int size() {
            return size;
        }

        public View sliceImpl(int start, int end) {
            return new ViewOfBytes(storage, offset + start, end - start);
        }

        /**
         * Copy the bytes of this view to the specified position in a destination array. All the
         * bytes of the View are copied. The view is of a byte array, so er can provide a more
         * efficient implementation than the default.
         */
        @Override
        public void copyTo(byte[] dest, int destPos) throws ArrayIndexOutOfBoundsException {
            System.arraycopy(storage, offset, dest, destPos, size);
        }

    }

    /**
     * Wrapper providing a byte-oriented view of just one byte. It looks silly, but it helps our
     * efficiency and code re-use.
     */
    protected static class ViewOfByte extends ViewBase {

        private byte storage;

        /**
         * Create a byte-oriented view of a byte array descended from BaseBytes.
         *
         * @param obj
         */
        public ViewOfByte(byte obj) {
            this.storage = obj;
        }

        public byte byteAt(int index) {
            return storage;
        }

        public int intAt(int index) {
            return 0xff & storage;
        }

        public int size() {
            return 1;
        }

        public View sliceImpl(int start, int end) {
            return new ViewOfByte(storage);
        }

        /**
         * Copy the byte the specified position in a destination array.
         */
        @Override
        public void copyTo(byte[] dest, int destPos) throws ArrayIndexOutOfBoundsException {
            dest[destPos] = storage;
        }

    }

    /**
     * Wrapper providing a byte-oriented view of an empty byte array or string. It looks even
     * sillier than wrapping one byte, but again helps our regularity and code re-use.
     */
    protected static class ViewOfNothing extends ViewBase {

        public byte byteAt(int index) {
            throw Py.IndexError(OUT_OF_BOUNDS);
        }

        public int intAt(int index) {
            throw Py.IndexError(OUT_OF_BOUNDS);
        }

        public int size() {
            return 0;
        }

        public View sliceImpl(int start, int end) {
            return new ViewOfNothing();
        }

        /**
         * Copy zero bytes the specified position, i.e. do nothing, even if dest[destPos] is out of
         * bounds.
         */
        @Override
        public void copyTo(byte[] dest, int destPos) {}

    }

    protected static final ViewOfNothing viewOfNothing = new ViewOfNothing();

    /*
     * ============================================================================================
     * API for org.python.core.PySequence
     * ============================================================================================
     */
    protected PyInteger pyget(int index) {
        return new PyInteger(intAt(index));
    }

    /*
     * We're not implementing these here, but we can give a stronger guarantee about the return type
     * and save some casting and type anxiety.
     */
    protected abstract BaseBytes getslice(int start, int stop, int step);

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
     * @throws PyException(IndexError) if the index is outside the array bounds
     * @throws PyException(ValueError) if element<0 or element>255
     * @throws PyException(TypeError) if the subclass is immutable
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
     * Comparison function between two byte arrays returning 1, 0, or -1 as a>b, a==b, or a&lt;b
     * respectively. The comparison is by value, using Python unsigned byte conventions, and
     * left-to-right (low to high index). Zero bytes are significant, even at the end of the array:
     * <code>[1,2,3]&lt;[1,2,3,0]</code>, for example and <code>[]</code> is less than every other
     * value, even <code>[0]</code>.
     *
     * @param a left-hand array in the comparison
     * @param b right-hand array in the comparison
     * @return 1, 0 or -1 as a>b, a==b, or a&lt;b respectively
     */
    private static int compare(BaseBytes a, BaseBytes b) {

        // Compare elements one by one in these ranges:
        int ap = a.offset;
        int aEnd = ap + a.size;
        int bp = b.offset;
        int bEnd = bp + b.size;

        while (ap < aEnd) {
            if (bp >= bEnd) {
                // a is longer than b
                return 1;
            } else {
                // Compare the corresponding bytes (as unsigned ints)
                int aVal = 0xff & a.storage[ap++];
                int bVal = 0xff & b.storage[bp++];
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
     * Comparison function between a byte array and a byte-oriented View of some other object, such
     * as a String, returning 1, 0 or -1 as a>b, a==b, or a&lt;b respectively. The comparison is by
     * value, using Python unsigned byte conventions, left-to-right (low to high index). Zero bytes
     * are significant, even at the end of the array: <code>[65,66,67]&lt;"ABC\u0000"</code>, for
     * example and <code>[]</code> is less than every non-empty b, while <code>[]==""</code>.
     *
     * @param a left-hand array in the comparison
     * @param b right-hand wrapped object in the comparison
     * @return 1, 0 or -1 as a>b, a==b, or a&lt;b respectively
     */
    private static int compare(BaseBytes a, View b) {

        // Compare elements one by one in these ranges:
        int ap = a.offset;
        int aEnd = ap + a.size;
        int bp = 0;
        int bEnd = b.size();

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
     * Comparison function between byte array types and any other object. The set of 6
     * "rich comparison" operators are based on this.
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
            View bv = getView(b);

            if (bv == null) {
                // Signifies a type mis-match. See PyObject _cmp_unsafe() and related code.
                return -2;

            } else {
                // Object supported by our interim memory view
                return compare(this, bv);

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
            View bv = getView(b);

            if (bv == null) {
                // Signifies a type mis-match. See PyObject _cmp_unsafe() and related code.
                return -2;

            } else if (bv.size() != size) {
                // Different size: can't be equal, and we don't care which is bigger
                return 1;

            } else {
                // Object supported by our interim memory view
                return compare(this, bv);

            }
        }
    }

    /**
     * Implementation of __eq__ (equality) operator, capable of comparison with another byte array
     * or bytes. Comparison with an invalid type returns null.
     *
     * @param other Python object to compare with
     * @return Python boolean result or null if not implemented for the other type.
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
     * Implementation of __ne__ (not equals) operator, capable of comparison with another byte array
     * or bytes. Comparison with an invalid type returns null.
     *
     * @param other Python object to compare with
     * @return Python boolean result or null if not implemented for the other type.
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
     * Implementation of __lt__ (less than) operator, capable of comparison with another byte array
     * or bytes. Comparison with an invalid type returns null.
     *
     * @param other Python object to compare with
     * @return Python boolean result or null if not implemented for the other type.
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
     * byte array or bytes. Comparison with an invalid type returns null.
     *
     * @param other Python object to compare with
     * @return Python boolean result or null if not implemented for the other type.
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
     * another byte array or bytes. Comparison with an invalid type returns null.
     *
     * @param other Python object to compare with
     * @return Python boolean result or null if not implemented for the other type.
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
     * array or bytes. Comparison with an invalid type returns null.
     *
     * @param other Python object to compare with
     * @return Python boolean result or null if not implemented for the other type.
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
     * Equivalent of the 'string_escape' decode to a String that is all-printable, showing non
     * printable charater as lowercase hex escapes, except '\t', '\n', and '\r'. This supports
     * <code>__repr__()</code>.
     *
     * @return the byte array as a String, still encoded
     */
    protected synchronized String asEscapedString() {
        StringBuilder buf = new StringBuilder(size + (size >> 8) + 10);
        int jmax = offset + size;
        for (int j = offset; j < jmax; j++) {
            int c = 0xff & storage[j];
            if (c >= 0x7f) {    // Unprintable high 128 and DEL
                appendHexEscape(buf, c);
            } else if (c >= ' ') { // Printable
                if (c == '\\') {    // Special case
                    buf.append("\\\\");
                } else {
                    buf.append((char)c);
                }
            } else if (c == '\t') { // Spacial cases in the low 32
                buf.append("\\t");
            } else if (c == '\n') {
                buf.append("\\n");
            } else if (c == '\r') {
                buf.append("\\r");
            } else {
                appendHexEscape(buf, c);
            }
        }
        return buf.toString();
    }

    private static final void appendHexEscape(StringBuilder buf, int c) {
        buf.append("\\x")
                .append(Character.forDigit((c & 0xf0) >> 4, 16))
                .append(Character.forDigit(c & 0xf, 16));
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
            View targetView = getViewOrError(target);
            Finder finder = new Finder(targetView);
            finder.setText(this);
            return finder.nextIndex() >= 0;
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
     * @param start of slice to search.
     * @param end of slice to search.
     * @param endswith true if we are doing endswith, false if startswith.
     * @return true if and only if this bytearray ends with (one of) <code>target</code>.
     */
    protected final synchronized boolean basebytes_starts_or_endswith(PyObject target,
                                                                      PyObject start,
                                                                      PyObject end,
                                                                      boolean endswith) {
        /*
         * This cheap trick saves us from maintaining two almost identical methods and mirrors
         * CPython's _bytearray_tailmatch().
         *
         * Start with a view of the slice we are searching.
         */
        View v = new ViewOfBytes(this).slice(start, end);
        int len = v.size();
        int offset = 0;

        if (target instanceof PyTuple) {
            // target is a tuple of suffixes/prefixes and only one need match
            for (PyObject s : ((PyTuple)target).getList()) {
                // Error if not something we can treat as a view of bytes
                View vt = getViewOrError(s);
                if (endswith) {
                    offset = len - vt.size();
                }
                if (v.startswith(vt, offset)) {
                    return true;
                }
            }
            return false; // None of them matched

        } else {
            // Error if target is not something we can treat as a view of bytes
            View vt = getViewOrError(target);
            if (endswith) {
                offset = len - vt.size();
            }
            return v.startswith(vt, offset);
        }
    }

    /**
     * Copy the bytes of a byte array to the characters of a String with no change in ordinal value.
     * This could also be described as 'latin-1' decoding of the byte array to a String.
     *
     * @return the byte array as a String, still encoded
     */
    private synchronized String asEncodedString() {
        StringBuilder buf = new StringBuilder(size);
        int jmax = offset + size;
        for (int j = offset; j < jmax; j++) {
            buf.append((char)(0xff & storage[j]));
        }
        return buf.toString();
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
     * @param encoding the name of the codec (uses default codec if null)
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
     * @param encoding the name of the codec (uses default codec if null)
     * @param errors the name of the error policy (uses 'strict' if null)
     * @return object containing the decoded characters
     */
    public PyObject decode(String encoding, String errors) {
        /*
         * Provide a Python <code>str</code> input to the decode method of a codec, which in v2.7
         * expects a PyString. (In Python 3k the codecs decode from the <code>bytes</code> type, so
         * we can pass this directly.)
         */
        PyString this_ = new PyString(this.asEncodedString());
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
        PyUnicode encoded = new PyUnicode(this.asEncodedString());
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
     * A large part of the CPython bytearray.c is devoted to replace( old, new [, count ] ).
     * The special section here reproduces that in Java, but whereas CPython makes heavy use
     * of the buffer API and C memcpy(), we use View.copyTo. The logic is much the same, however,
     * even down to variable names.
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
     * This class implements the Boyer-Moore-Horspool Algorithm for findind a pattern in text,
     * applied to byte arrays. The BMH algorithm uses a table of bad-character skips derived from
     * the pattern. The bad-character skips table tells us how far from the end of the pattern is a
     * byte that might match the text byte currently aligned with the end of the pattern. For
     * example, suppose the pattern (of length 6) is at position 4:
     *
     * <pre>
     *                    1         2         3
     *          0123456789012345678901234567890
     * Text:    a man, a map, a panama canal
     * Pattern:     panama
     * </pre>
     *
     * This puts the 'm' of 'map' against the last byte 'a' of the pattern. Rather than testing the
     * pattern, we will look up 'm' in the skip table. There is an 'm' just one step from the end of
     * the pattern, so we will move the pattern one place to the right before trying to match it.
     * This allows us to move in large strides throughthe text.
     */
    protected static class Finder {

        /**
         * Construct a Finder object that may be used (repeatedly) to find matches with the pattern
         * in text (arrays of bytes).
         *
         * @param pattern A vew that presents the pattern as an array of bytes
         */
        public Finder(View pattern) {
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
            int m = pattern.size();
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
            right = start + size - pattern.size() + 1; // Last pattern position + 1

            /*
             * We defer computing the table from construction to this point mostly because
             * calculateSkipTable() may be overridden, and we want to use the right one.
             */
            if (pattern.size() > 1 && skipTable == null) {
                skipTable = calculateSkipTable();
            }

        }

        protected final View pattern;
        protected byte[] text = emptyStorage; // in case we forget to setText()
        protected int left = 0; // Leftmost pattern position to use
        protected int right = 0; // Rightmost pattern position + 1

        /**
         * Return the  index in the text array where the preceding pattern match ends (one beyond the last
         * character matched), which may also be one beyond the effective end ofthe text.
         * Between a call to setText() and the first call to
         * <code>nextIndex()</code> return the start position.
         * <p>
         * The following idiom may be used:
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
            int m = pattern.size();

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
        public ReverseFinder(View pattern) {
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
        protected int[] calculateSkipTable() {
            int[] skipTable = new int[MASK + 1];
            int m = pattern.size();
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
        public int currIndex() {
            return right+pattern.size()-1;
        }

        /**
         * Find the next index in the text array where the pattern starts, but working backwards.
         * Successive calls to <code>nextIndex()</code> return the successive (non-overlapping)
         * occurrences of the pattern in the text.
         *
         * @return matching index or -1 if no (further) occurrences found
         */
        public int nextIndex() {

            int m = pattern.size();

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
     * Convenience routine producing a ValueError for "empty separator" if the View is of an object with zero length,
     * and returning the length otherwise.
     *
     * @param separator view to test
     * @return the length of the separator
     * @throws PyException if the View is zero length
     */
    protected final static int checkForEmptySeparator(View separator) throws PyException {
        int n = separator.size();
        if (n == 0) {
            throw Py.ValueError("empty separator");
        }
        return n;
    }

    /**
     * Return the index [0..size-1] of the leftmost byte not matching any in <code>byteSet</code>,
     * or <code>size</code> if they are all strippable.
     *
     * @param byteSet list of byte values to skip over
     * @return index of first unstrippable byte
     */
    protected int lstripIndex(View byteSet) {
        int limit = offset + size;
        int j, m = byteSet.size();
        // Run up the storage checking against byteSet (or until we hit the end)
        for (int left = offset; left < limit; left++) {
            byte curr = storage[left];
            // Check against the byteSet to see if this is one to strip.
            for (j = 0; j < m; j++) {
                if (curr == byteSet.byteAt(j)) {
                    break;
                }
            }
            if (j == m) {
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
        // Run up the storage until non-whitespace (or hit end)t
        for (int left = offset; left < limit; left++) {
            if (!Character.isWhitespace(storage[left] & 0xff)) {
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
     * @param byteSet list of byte values to strip
     * @return index of strippable tail
     */
    protected int rstripIndex(View byteSet) {
        int j, m = byteSet.size();
        // Run down the storage checking the next byte against byteSet (or until we hit the start)
        for (int right = offset + size; right > offset; --right) {
            byte next = storage[right - 1];
            // Check against the byteSet to see if this is one to strip.
            for (j = 0; j < m; j++) {
                if (next == byteSet.byteAt(j)) {
                    break;
                }
            }
            if (j == m) {
                // None of them matched: this is the rightmost strippable byte
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
            if (!Character.isWhitespace(storage[right - 1] & 0xff)) {
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
        Finder finder = new Finder(getViewOrError(sub));

        // Convert [start:end] to integers
        PySlice s = new PySlice(ostart, oend, null);
        int[] index = s.indicesEx(size());  // [ start, end, 1, end-start ]

        // Make this slice the thing we count within.
        return finder.count(storage, offset + index[0], index[3]);
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
        Finder finder = new Finder(getViewOrError(sub));
        return find(finder, ostart, oend);
    }

    /**
     * Almost ready-to-expose implementation of Python <code>join(iterable)</code>.
     *
     * @param iter iterable of objects capable of being regarded as byte arrays
     * @return the byte array that is their join
     */
    final synchronized PyByteArray basebytes_join(Iterable<? extends PyObject> iter) {

        List<View> iterList = new LinkedList<View>();
        long mysize = this.size;
        long totalSize = 0;
        boolean first = true;

        for (PyObject o : iter) {
            // Scan the iterable into a list, checking type and accumulating size
            View v = getView(o);
            if (v == null) {
                // Unsuitable object to be in this join
                String fmt = "can only join an iterable of bytes (item %d has type '%.80s')";
                throw Py.TypeError(String.format(fmt, iterList.size(), o.getType().fastGetName()));
            }
            iterList.add(v);
            totalSize += v.size();

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

        for (View v : iterList) {
            // Each element after the first is preceded by a copy of this
            if (!first) {
                System.arraycopy(storage, offset, result.storage, p, size);
                p += size;
            } else {
                first = false;
            }
            // Then the element from the iterable
            v.copyTo(result.storage, p);
            p += v.size();
        }

        return result;
    }

    /**
     * Implementation of Python <code>partition(sep)</code>, returning a 3-tuple of byte arrays (of
     * the same type as <code>this</code>).
     *
     * Split the string at the first occurrence of <code>sep</code>, and return a 3-tuple containing
     * the part before the separator, the separator itself, and the part after the separator. If the
     * separator is not found, return a 3-tuple containing the string itself, followed by two empty
     * byte arrays.
     *
     * @param sep the separator on which to partition this byte array
     * @return a tuple of (head, separator, tail)
     */
    public PyTuple partition(PyObject sep) {
        return basebytes_partition(sep);
    }

    /**
     * Ready-to-expose implementation of Python <code>partition(sep)</code>.
     *
     * @param sep the separator on which to partition this byte array
     * @return a tuple of (head, separator, tail)
     */
    final synchronized PyTuple basebytes_partition(PyObject sep) {

        // Create a Finder for the separtor and set it on this byte array
        View separator = getViewOrError(sep);
        int n = checkForEmptySeparator(separator);
        Finder finder = new Finder(separator);
        finder.setText(this);

        // We only uuse it once, to find the first occurrence
        int p = finder.nextIndex() - offset;
        if (p >= 0) {
            // Found at p, so we'll be returning ([0:p], [p:p+n], [p+n:])
            return partition(p, p + n);
        } else {
            // Not found: choose values leading to ([0:size], '', '')
            return partition(size, size);
        }
    }

    /**
     * Construct return value for implementation of Python <code>partition(sep)</code> or
     * <code>rpartition(sep)</code>, returns [0:p], [p:q], [q:]
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
        Finder finder = new ReverseFinder(getViewOrError(sub));
        return find(finder, ostart, oend);
    }

    /**
     * Common code for Python <code>find( sub [, start [, end ]] )</code> and
     * <code>rfind( sub [, start [, end ]] )</code>. Return the lowest or highest index in the byte
     * array where byte sequence used to construct <code>finder</code> is found.
     *
     * @param finder for the bytes to find, sometime forwards, sometime backwards
     * @param ostart of slice to search
     * @param oend of slice to search
     * @return index of start of occurrence of sub within this byte array
     */
    private final int find(Finder finder, PyObject ostart, PyObject oend) {

        // Convert [start:end] to integers
        PySlice s = new PySlice(ostart, oend, null);
        int[] index = s.indicesEx(size());  // [ start, end, 1, end-start ]

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

        View from = getViewOrError(oldB);
        View to = getViewOrError(newB);

        /*
         * The logic of the first section is copied exactly from CPython in order to get the same
         * behaviour. The "headline" description of replace is simple enough but the corner cases
         * can be surprising:
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

        int from_len = from.size();
        int to_len = to.size();

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
            // The result is the same size as this byte array, whatever the number of replacements.
            return replace_substring_in_place(from, to, maxcount);

        } else {
            // Otherwise use the generic algorithm
            return replace_substring(from, to, maxcount);
        }
    }

    /*
     * Algorithms for different cases of string replacement. CPython also has specialisations for
     * when 'from' or 'to' or both are single bytes. In Java we think this is unnecessary because
     * such speed gain as might be available that way is obtained by using the efficient one-byte
     * View object. Because Java cannot access memory bytes directly, unlike C, there is not so much
     * to be gained.
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
    private PyByteArray replace_substring(View from, View to, int maxcount) {
        // size>=1, len(from)>=1, len(to)>=1, maxcount>=1

        // Initialise a Finder for the 'from' pattern
        Finder finder = new Finder(from);

        int count = finder.count(storage, offset, size, maxcount);
        if (count == 0) {
            // no matches
            return new PyByteArray(this);
        }

        int from_len = from.size();
        int to_len = to.size();

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
     * site we are guaranteed: size>=1, to.size()>=1, maxcount>=1
     *
     * @param to the replacement bytes as a byte-oriented view
     * @param maxcount upper limit on number of insertions
     */
    private PyByteArray replace_interleave(View to, int maxcount) {

        // Insert one at the beginning and one after every byte, or as many as allowed
        int count = size + 1;
        if (maxcount < count) {
            count = maxcount;
        }

        int to_len = to.size();

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
    private PyByteArray replace_delete_substring(View from, int maxcount) {
        // len(self)>=1, len(from)>=1, to="", maxcount>=1

        // Initialise a Finder for the 'from' pattern
        Finder finder = new Finder(from);

        int count = finder.count(storage, offset, size, maxcount);
        if (count == 0) {
            // no matches
            return new PyByteArray(this);
        }

        int from_len = from.size();
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
    private PyByteArray replace_substring_in_place(View from, View to, int maxcount) {
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
     *
     * @param sep the separator on which to partition this byte array
     * @return a tuple of (head, separator, tail)
     */
    public PyTuple rpartition(PyObject sep) {
        return basebytes_rpartition(sep);
    }

    /**
     * Ready-to-expose implementation of Python <code>rpartition(sep)</code>.
     *
     * @param sep the separator on which to partition this byte array
     * @return a tuple of (head, separator, tail)
     */
    final synchronized PyTuple basebytes_rpartition(PyObject sep) {

        // Create a Finder for the separtor and set it on this byte array
        View separator = getViewOrError(sep);
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

    /**
     * Implementation of Python <code>rsplit()</code>, that returns a list of the words in the byte
     * array, using whitespace as the delimiter. See {@link #rsplit(PyObject, int)}.
     *
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    public PyList rsplit() {
        return basebytes_rsplit_whitespace(-1);
    }

    /**
     * Implementation of Python <code>rsplit(sep)</code>, that returns a list of the words in the
     * byte array, using sep as the delimiter. See {@link #rsplit(PyObject, int)} for the semantics
     * of the separator.
     *
     * @param sep bytes, or object viewable as bytes, defining the separator
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    public PyList rsplit(PyObject sep) {
        return basebytes_rsplit(sep, -1);
    }

    /**
     * Implementation of Python <code>rsplit(sep, maxsplit)</code>, that returns a list of the words
     * in the byte array, using sep as the delimiter. If maxsplit is given, at most maxsplit splits
     * are done (thus, the list will have at most maxsplit+1 elements). If maxsplit is not
     * specified, then there is no limit on the number of splits (all possible splits are made).
     * <p>
     * The semantics of sep and maxcount are identical to those of
     * <code>split(sep, maxsplit)</code>, except that splits are generated from the right (and pushed onto the front of the result list). The result is only different from that of <code>split</code> if <code>maxcount</code> limits the number of splits.
     * For example,
     * <ul>
     * <li><code>bytearray(b' 1  2   3  ').rsplit()</code> returns
     * <code>[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')]</code>, and</li>
     * <li><code>bytearray(b'  1  2   3  ').rsplit(None, 1)</code> returns
     * <code>[bytearray(b'  1 2'), bytearray(b'3')]</code></li>.
     * </ul>
     *
     * @param sep bytes, or object viewable as bytes, defining the separator
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    public PyList rsplit(PyObject sep, int maxsplit) {
        return basebytes_rsplit(sep, maxsplit);
    }

    /**
     * Ready-to-expose implementation of Python <code>rsplit(sep, maxsplit)</code>, that returns a
     * list of the words in the byte array, using sep as the delimiter. Use the defines whitespace
     * semantics if sep is null.
     *
     * @param sep bytes, or object viewable as bytes, defining the separator
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
     * in the byte array, using sep (which is not null) as the delimiter. If maxsplit>=0, at most
     * maxsplit splits are done (thus, the list will have at most maxsplit+1 elements). If
     * maxsplit&lt;0, then there is no limit on the number of splits (all possible splits are made).
     *
     * @param sep bytes, or object viewable as bytes, defining the separator
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    final synchronized PyList basebytes_rsplit_explicit(PyObject sep, int maxsplit) {

        // The separator may be presented as anything viewable as bytes
        View separator = getViewOrError(sep);
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

    /**
     * Implementation of Python <code>rsplit(None, maxsplit)</code>, that returns a list of the
     * words in the byte array, using whitespace as the delimiter. If maxsplit is given, at most
     * maxsplit splits are done (thus, the list will have at most maxsplit+1 elements). If maxsplit
     * is not specified, then there is no limit on the number of splits (all possible splits are
     * made).
     * <p>
     * Runs of consecutive whitespace are regarded as a single separator, and the result will
     * contain no empty strings at the start or end if the string has leading or trailing
     * whitespace. Consequently, splitting an empty string or a string consisting of just whitespace
     * with a None separator returns [].
     *
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    final synchronized PyList basebytes_rsplit_whitespace(int maxsplit) {

        PyList result = new PyList();
        int p, q; // Indexes of unsplit text and whitespace

        // Scan backwards over trailing whitespace
        for (q = offset + size; q > offset; --q) {
            if (!Character.isWhitespace(storage[q - 1] & 0xff)) {
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
                if (Character.isWhitespace(storage[p - 1] & 0xff)) {
                    break;
                }
            }
            // storage[p] is the first byte of the word. (p=offset or storage[p-1] is whitespace.)
            BaseBytes word = getslice(p - offset, q - offset);
            result.add(0, word);
            // Skip q backwards over the whitespace
            for (q = p; q > offset; --q) {
                if (!Character.isWhitespace(storage[q - 1] & 0xff)) {
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
     *
     * @return PyList of byte arrays that result from the split
     */
    public PyList split() {
        return basebytes_split_whitespace(-1);
    }

    /**
     * Implementation of Python <code>split(sep)</code>, that returns a list of the words in the
     * byte array, using sep as the delimiter. See {@link #split(PyObject, int)} for the semantics
     * of the separator.
     *
     * @param sep bytes, or object viewable as bytes, defining the separator
     * @return PyList of byte arrays that result from the split
     */
    public PyList split(PyObject sep) {
        return basebytes_split(sep, -1);
    }

    /**
     * Implementation of Python <code>split(sep, maxsplit)</code>, that returns a list of the words
     * in the byte array, using sep as the delimiter. If maxsplit is given, at most maxsplit splits
     * are done (thus, the list will have at most maxsplit+1 elements). If maxsplit is not
     * specified, then there is no limit on the number of splits (all possible splits are made).
     * <p>
     * If sep is given, consecutive delimiters are not grouped together and are deemed to delimit
     * empty strings (for example, '1,,2'.split(',') returns ['1', '', '2']). The sep argument may
     * consist of multiple characters (for example, '1&lt;>2&lt;>3'.split('&lt;>') returns ['1',
     * '2', '3']). Splitting an empty string with a specified separator returns [''].
     * <p>
     * If sep is not specified or is None, a different splitting algorithm is applied: runs of
     * consecutive whitespace are regarded as a single separator, and the result will contain no
     * empty strings at the start or end if the string has leading or trailing whitespace.
     * Consequently, splitting an empty string or a string consisting of just whitespace with a None
     * separator returns []. For example,
     *
     * <ul>
     * <li><code>bytearray(b' 1  2   3  ').split()</code> returns
     * <code>[bytearray(b'1'), bytearray(b'2'), bytearray(b'3')]</code>, and</li>
     * <li><code>bytearray(b'  1  2   3  ').split(None, 1)</code> returns
     * <code>[bytearray(b'1'), bytearray(b'2   3  ')]</code>.</li>
     * </ul>
     *
     * @param sep bytes, or object viewable as bytes, defining the separator
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    public PyList split(PyObject sep, int maxsplit) {
        return basebytes_split(sep, maxsplit);
    }

    /**
     * Ready-to-expose implementation of Python <code>split(sep, maxsplit)</code>, that returns a
     * list of the words in the byte array, using sep as the delimiter. Use the defines whitespace
     * semantics if sep is null.
     *
     * @param sep bytes, or object viewable as bytes, defining the separator
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    final PyList basebytes_split(PyObject sep, int maxsplit) {
        if (sep == null || sep==Py.None) {
            return basebytes_split_whitespace(maxsplit);
        } else {
            return basebytes_split_explicit(sep, maxsplit);
        }
    }

    /**
     * Implementation of Python <code>split(sep, maxsplit)</code>, that returns a list of the words
     * in the byte array, using sep (which is not null) as the delimiter. If maxsplit>=0, at most
     * maxsplit splits are done (thus, the list will have at most maxsplit+1 elements). If
     * maxsplit&lt;0, then there is no limit on the number of splits (all possible splits are made).
     *
     * @param sep bytes, or object viewable as bytes, defining the separator
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    final synchronized PyList basebytes_split_explicit(PyObject sep, int maxsplit) {

        // The separator may be presented as anything viewable as bytes
        View separator = getViewOrError(sep);
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

    /**
     * Implementation of Python <code>split(None, maxsplit)</code>, that returns a list of the words
     * in the byte array, using whitespace as the delimiter. If maxsplit is given, at most maxsplit
     * splits are done (thus, the list will have at most maxsplit+1 elements). If maxsplit is not
     * specified, then there is no limit on the number of splits (all possible splits are made).
     * <p>
     * Runs of consecutive whitespace are regarded as a single separator, and the result will
     * contain no empty strings at the start or end if the string has leading or trailing
     * whitespace. Consequently, splitting an empty string or a string consisting of just whitespace
     * with a None separator returns [].
     *
     * @param maxsplit maximum number of splits
     * @return PyList of byte arrays that result from the split
     */
    final synchronized PyList basebytes_split_whitespace(int maxsplit) {

        PyList result = new PyList();
        int limit = offset + size;
        int p, q; // Indexes of unsplit text and whitespace

        // Scan over leading whitespace
        for (p = offset; p < limit && Character.isWhitespace(storage[p] & 0xff); p++) {
            ; // continue
        }

        // Note: bytearray().split() = bytearray(b' ').split() = []

        // At this point if p<limit it points to the start of a word.
        // While we have some splits left (if maxsplit started>=0)
        while (p < limit && maxsplit-- != 0) {
            // Delimit a word at p
            // storage[p] is not whitespace or at the limit: it is the start of a word
            // Skip q over the non-whitespace at p
            for (q = p; q < limit && !Character.isWhitespace(storage[q] & 0xff); q++) {
                ; // continue
            }
            // storage[q] is whitespace or it is at the limit
            result.append(getslice(p - offset, q - offset));
            // Skip p over the whitespace at q
            for (p = q; p < limit && Character.isWhitespace(storage[p] & 0xff); p++) {
                ; // continue
            }
        }

        // Append the remaining unsplit text if any
        if (p<limit) {
            result.append(getslice(p - offset, size));
        }
        return result;
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
     * @throws PyException(IndexError) if the index is outside the array bounds
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

    /*
     * ============================================================================================
     * API for java.util.List<PyInteger>
     * ============================================================================================
     */

   /**
     * Access to the bytearray (or bytes) as a {@link java.util.List}. The List interface supplied
     * by BaseBytes delegates to this object.
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
         * @throws PyException(TypeError) if actual class is immutable
         * @throws PyException(IndexError) if the index is outside the array bounds
         * @throws PyException(ValueError) if element<0 or element>255
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
         * @throws PyException(IndexError) if the index is outside the array bounds
         * @throws PyException(ValueError) if element<0 or element>255
         * @throws PyException(TypeError) if the owning concrete subclass is immutable
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
         * @throws PyException(IndexError) if the index is outside the array bounds
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
     * Number of bytes in bytearray (or bytes) object.
     *
     * @see java.util.List#size()
     * @return Number of bytes in byte array.
     * */
    public int size() {
        return size;
    }

    /*
     * @see java.util.List#isEmpty()
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns true if this list contains the specified value. More formally, returns true if and
     * only if this list contains at least one integer e such that o.equals(PyInteger(e)).
     */
    public boolean contains(Object o) {
        return listDelegate.contains(o);
    }

    /*
     * @see java.util.List#iterator()
     */
    public Iterator<PyInteger> iterator() {
        return listDelegate.iterator();
    }

    /*
     * @see java.util.List#toArray()
     */
    public Object[] toArray() {
        return listDelegate.toArray();
    }

    /*
     * @see java.util.List#toArray(T[])
     */
    public <T> T[] toArray(T[] a) {
        return listDelegate.toArray(a);
    }

    /*
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean add(PyInteger o) {
        return listDelegate.add(o);
    }

    /*
     * @see java.util.List#remove(java.lang.Object)
     */
    public boolean remove(Object o) {
        return listDelegate.remove(o);
    }

    /*
     * @see java.util.List#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection<?> c) {
        return listDelegate.containsAll(c);
    }

    /*
     * @see java.util.List#addAll(java.util.Collection)
     */
    public boolean addAll(Collection<? extends PyInteger> c) {
        return listDelegate.addAll(c);
    }

    /*
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int index, Collection<? extends PyInteger> c) {
        return listDelegate.addAll(index, c);
    }

    /*
     * @see java.util.List#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection<?> c) {
        return listDelegate.removeAll(c);
    }

    /*
     * @see java.util.List#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection<?> c) {
        return listDelegate.retainAll(c);
    }

    /*
     * @see java.util.List#clear()
     */
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
    public int hashCode() {
        return listDelegate.hashCode();
    }

    /*
     * @see java.util.List#get(int)
     */
    public PyInteger get(int index) {
        return listDelegate.get(index);
    }

    /*
     * @see java.util.List#set(int, java.lang.Object)
     */
    public PyInteger set(int index, PyInteger element) {
        return listDelegate.set(index, element);
    }

    /*
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int index, PyInteger element) {
        listDelegate.add(index, element);
    }

    /*
     * @see java.util.List#remove(int)
     */
    public PyInteger remove(int index) {
        return listDelegate.remove(index);
    }

    /*
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object o) {
        return listDelegate.indexOf(o);
    }

    /*
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object o) {
        return listDelegate.lastIndexOf(o);
    }

    /*
     * @see java.util.List#listIterator()
     */
    public ListIterator<PyInteger> listIterator() {
        return listDelegate.listIterator();
    }

    /*
     * @see java.util.List#listIterator(int)
     */
    public ListIterator<PyInteger> listIterator(int index) {
        return listDelegate.listIterator(index);
    }

    /*
     * @see java.util.List#subList(int, int)
     */
    public List<PyInteger> subList(int fromIndex, int toIndex) {
        return listDelegate.subList(fromIndex, toIndex);
    }

}
