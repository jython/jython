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
     * but note that from Python 3, bytes() has the same set of calls and behaviours, although in
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
     * Helper for {@linkplain #setslice(int, int, int, PyObject)}, for <code>__new__</code> and
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
            //
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
            setStorage(new byte[needed]); // guaranteed zero (by JLS 2ed para 4.5.5)
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
         * Copy the bytes of this view to the specified position in a destination array.
         * All the bytes of the View are copied.
         * @param dest destination array
         * @param destPos index in the destination at which this.byteAt(0) is written
         * @throws ArrayIndexOutOfBoundsException if the destination is too small
         */
        public void copyTo(byte[] dest, int destPos) throws ArrayIndexOutOfBoundsException;

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
            // String fmt = "type %s doesn't support the buffer API"; // CPython
            String fmt = "cannot access type %s as bytes";
            throw Py.NotImplementedError(String.format(fmt, b.getType().fastGetName()));
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
     * @param other
     * @return
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
     * @param other
     * @return
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
     * @param other
     * @return
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
     * @param other
     * @return
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
     * @param other
     * @return
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
     * @param other
     * @return
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
             * calculateSkipTable() may be overridden.
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
         * Find the next index in the text array where the pattern starts. Successive callls to
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
         * Find the next index in the text array where the pattern starts, but working backwards.
         * Successive callls to <code>nextIndex()</code> return the successive (non-overlapping)
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
     * Ready-to-expose implementation of Python <code>count( sub [, start [, end ]] )</code>.
     *  Return
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
     * Ready-to-expose implementation of Python <code>rfind( sub [, start [, end ]] )</code>. Return
     * the highest index in the byte array where byte sequence <code>sub</code> is found, such that
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
        if (result_len > Integer.MAX_VALUE) {
            Py.OverflowError("replace bytes is too long");
        }

        // Good to go. As we know the ultimate size, we can do all our allocation in one
        byte[] r = new byte[(int)result_len];
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
        if (result_len > Integer.MAX_VALUE) {
            Py.OverflowError("replace bytes is too long");
        }

        // Good to go. As we know the ultimate size, we can do all our allocation in one
        byte[] r = new byte[(int)result_len];
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

        // Good to go. As we know the ultimate size, we can do all our allocation in one
        byte[] r = new byte[(int)result_len];
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
        byte[] r = new byte[size];
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
     *
     *
     *
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
        byte[] dst = new byte[count * size];
        for (int i = 0, p = 0; i < count; i++, p += size) {
            System.arraycopy(storage, offset, dst, p, size);
        }
        return dst;
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
     * @return
     *
     * @see java.util.List#iterator()
     */
    public Iterator<PyInteger> iterator() {
        return listDelegate.iterator();
    }

    /*
     * @return
     *
     * @see java.util.List#toArray()
     */
    public Object[] toArray() {
        return listDelegate.toArray();
    }

    /*
     * @param a
     *
     * @return
     *
     * @see java.util.List#toArray(T[])
     */
    public <T> T[] toArray(T[] a) {
        return listDelegate.toArray(a);
    }

    /*
     * @param o
     *
     * @return
     *
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean add(PyInteger o) {
        return listDelegate.add(o);
    }

    /*
     * @param o
     *
     * @return
     *
     * @see java.util.List#remove(java.lang.Object)
     */
    public boolean remove(Object o) {
        return listDelegate.remove(o);
    }

    /*
     * @param c
     *
     * @return
     *
     * @see java.util.List#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection<?> c) {
        return listDelegate.containsAll(c);
    }

    /*
     * @param c
     *
     * @return
     *
     * @see java.util.List#addAll(java.util.Collection)
     */
    public boolean addAll(Collection<? extends PyInteger> c) {
        return listDelegate.addAll(c);
    }

    /*
     * @param index
     *
     * @param c
     *
     * @return
     *
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int index, Collection<? extends PyInteger> c) {
        return listDelegate.addAll(index, c);
    }

    /*
     * @param c
     *
     * @return
     *
     * @see java.util.List#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection<?> c) {
        return listDelegate.removeAll(c);
    }

    /*
     * @param c
     *
     * @return
     *
     * @see java.util.List#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection<?> c) {
        return listDelegate.retainAll(c);
    }

    /*
     *
     * @see java.util.List#clear()
     */
    public void clear() {
        listDelegate.clear();
    }

    /*
     * @param o
     *
     * @return
     *
     * @see java.util.List#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        return listDelegate.equals(o);
    }

    /*
     * @return
     *
     * @see java.util.List#hashCode()
     */
    public int hashCode() {
        return listDelegate.hashCode();
    }

    /*
     * @param index
     *
     * @return
     *
     * @see java.util.List#get(int)
     */
    public PyInteger get(int index) {
        return listDelegate.get(index);
    }

    /*
     * @param index
     *
     * @param element
     *
     * @return
     *
     * @see java.util.List#set(int, java.lang.Object)
     */
    public PyInteger set(int index, PyInteger element) {
        return listDelegate.set(index, element);
    }

    /*
     * @param index
     *
     * @param element
     *
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int index, PyInteger element) {
        listDelegate.add(index, element);
    }

    /*
     * @param index
     *
     * @return
     *
     * @see java.util.List#remove(int)
     */
    public PyInteger remove(int index) {
        return listDelegate.remove(index);
    }

    /*
     * @param o
     *
     * @return
     *
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object o) {
        return listDelegate.indexOf(o);
    }

    /*
     * @param o
     *
     * @return
     *
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object o) {
        return listDelegate.lastIndexOf(o);
    }

    /*
     * @return
     *
     * @see java.util.List#listIterator()
     */
    public ListIterator<PyInteger> listIterator() {
        return listDelegate.listIterator();
    }

    /*
     * @param index
     *
     * @return
     *
     * @see java.util.List#listIterator(int)
     */
    public ListIterator<PyInteger> listIterator(int index) {
        return listDelegate.listIterator(index);
    }

    /*
     * @param fromIndex
     *
     * @param toIndex
     *
     * @return
     *
     * @see java.util.List#subList(int, int)
     */
    public List<PyInteger> subList(int fromIndex, int toIndex) {
        return listDelegate.subList(fromIndex, toIndex);
    }

}
