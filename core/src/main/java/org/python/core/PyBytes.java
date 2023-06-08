package org.python.core;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.python.core.PyObjectUtil.NoConversion;
import org.python.core.PySequence.Delegate;
import org.python.core.PySlice.Indices;
import org.python.core.stringlib.ByteArrayBuilder;

/** The Python {@code bytes} object. */
public class PyBytes extends AbstractList<Integer> implements CraftedPyObject {

    /** The type of Python object this class implements. */
    public static final PyType TYPE = PyType.fromSpec( //
            new PyType.Spec("bytes", MethodHandles.lookup()));
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[] {};
    static final PyBytes EMPTY = new PyBytes(EMPTY_BYTE_ARRAY);

    /** The Python type of this instance. */
    protected final PyType type;

    /** The elements of the {@code bytes}. */
    final byte[] value;

    /**
     * Helper to implement {@code __getitem__} and other index-related
     * operations.
     */
    private BytesDelegate delegate = new BytesDelegate();

    /**
     * As {@link #PyBytes(byte[])} for Python sub-class specifying
     * {@link #type}. Construct an instance of {@code PyBytes} or a
     * sub-class, from a given array of bytes, with the option to re-use
     * that array as the implementation. If the actual array is is
     * re-used the caller must give up ownership and never modify it
     * after the call. See {@link #concat(PySequenceInterface)} for a
     * correct use.
     *
     * @param type sub-type for which this is being created
     * @param iPromiseNotToModify if {@code true}, the array becomes the
     *     implementation array, otherwise the constructor takes a copy.
     * @param value the array of the bytes to contain
     */
    private PyBytes(PyType type, boolean iPromiseNotToModify, byte[] value) {
        this.type = type;
        if (value.length == 0)
            this.value = EMPTY_BYTE_ARRAY;
        else if (iPromiseNotToModify)
            this.value = value;
        else
            this.value = Arrays.copyOf(value, value.length);
    }

    /**
     * As {@link #PyBytes(byte[])} for Python sub-class specifying
     * {@link #type}.
     *
     * @param type sub-type for which this is being created
     * @param value of the bytes
     */
    protected PyBytes(PyType type, byte[] value) { this(type, false, value); }

    /**
     * As {@link #PyBytes(int...)} for Python sub-class specifying
     * {@link #type}.
     *
     * @param type sub-type for which this is being created
     * @param value of the bytes
     */
    protected PyBytes(PyType type, int... value) {
        this.type = type;
        int n = value.length;
        if (n == 0)
            this.value = EMPTY_BYTE_ARRAY;
        else {
            byte[] b = new byte[n];
            for (int i = 0; i < n; i++) { b[i] = (byte)value[i]; }
            this.value = b;
        }
    }

    /**
     * Construct a Python {@code bytes} object from bytes treated as
     * unsigned.
     *
     * @param value of the bytes
     */
    public PyBytes(byte[] value) { this(TYPE, false, value); }

    /**
     * Construct a Python {@code bytes} object from Java {@code int}s
     * treated as unsigned.
     *
     * @param value of the bytes
     */
    public PyBytes(int... value) { this(TYPE, value); }

    /**
     * Construct a Python {@code bytes} object from a
     * {@link ByteArrayBuilder}. This provides a safe, zero-copy way to
     * supply the contents from an algorithm.
     *
     * @param value of the bytes
     */
    public PyBytes(ByteArrayBuilder value) { this(TYPE, true, value.take()); }

    /**
     * Unsafely wrap an array of bytes as a {@code PyBytes}. The caller
     * must not hold a reference to the argument array (and definitely
     * not manipulate the contents).
     *
     * @param value to wrap as a {@code bytes}
     * @return the {@code bytes}
     */
    private static PyBytes wrap(byte[] value) { return new PyBytes(TYPE, true, value); }

    @Override
    public PyType getType() { return type; }

    // Special methods ------------------------------------------------

    @SuppressWarnings("unused")
    private Object __add__(Object other) throws Throwable { return delegate.__add__(other); }

    @SuppressWarnings("unused")
    private Object __radd__(Object other) throws Throwable { return delegate.__radd__(other); }

    @SuppressWarnings("unused")
    private Object __mul__(Object n) throws Throwable { return delegate.__mul__(n); }

    @SuppressWarnings("unused")
    private Object __rmul__(Object n) throws Throwable { return delegate.__mul__(n); }

    @SuppressWarnings("unused")
    private int __len__() { return value.length; }

    @SuppressWarnings("unused")
    private Object __getitem__(Object item) throws Throwable { return delegate.__getitem__(item); }

    // AbstractList methods -------------------------------------------

    @Override
    public Integer get(int i) { return 0xff & value[i]; }

    @Override
    public int size() { return value.length; }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {

            private int i = 0;

            @Override
            public boolean hasNext() { return i < value.length; }

            @Override
            public Integer next() { return 0xff & value[i++]; }
        };
    }

    // Java API -------------------------------------------------------

    /**
     * Expose the contents of the object as a read-only
     * {@code ByteBuffer}.
     * {@code This is temporary API until we implement the buffer interface.}
     *
     * @return a Java NIO buffer
     */
    public ByteBuffer getNIOByteBuffer() { return ByteBuffer.wrap(value).asReadOnlyBuffer(); }

    /**
     * Expose the contents of the object as a read-only sequence of
     * {@code int}.
     *
     * @return sequence of {@code int}
     */
    public PySequence.OfInt asSequence() { return delegate; }

    /**
     * Return the contents of the object as an array of of {@code byte}.
     *
     * @return array of {@code byte}
     */
    public byte[] asByteArray() {
        return Arrays.copyOf(value, value.length);
    }

    // Plumbing -------------------------------------------------------

    /**
     * A class to act as the delegate implementing {@code __getitem__}
     * and other index-related operations. By inheriting {@link Delegate
     * PySequence.Delegate} in this inner class, we obtain boilerplate
     * implementation code for slice translation and range checks. We
     * need only specify the work specific to {@link PyBytes} instances.
     */
    class BytesDelegate extends PySequence.Delegate<Integer, PyBytes> implements PySequence.OfInt {

        @Override
        public int length() { return value.length; }

        @Override
        public PyType getType() { return type; }

        @Override
        public Integer getItem(int i) { return 0xff & value[i]; }

        @Override
        public int getInt(int i) { return 0xff & value[i]; }

        @Override
        public PyBytes getSlice(Indices slice) {
            byte[] v;
            if (slice.step == 1)
                v = Arrays.copyOfRange(value, slice.start, slice.stop);
            else {
                v = new byte[slice.slicelength];
                int i = slice.start;
                for (int j = 0; j < slice.slicelength; j++) {
                    v[j] = value[i];
                    i += slice.step;
                }
            }
            return wrap(v);
        }

        @Override
        PyBytes add(Object ow) throws OutOfMemoryError, NoConversion, Throwable {
            return concatBytes(delegate, adapt(ow));
        }

        @Override
        PyBytes radd(Object ov) throws OutOfMemoryError, NoConversion, Throwable {
            return concatBytes(adapt(ov), delegate);
        }

        @Override
        PyBytes repeat(int n) throws OutOfMemoryError, Throwable {
            int m = value.length;
            if (n == 0)
                return EMPTY;
            else if (n == 1 || m == 0)
                return PyBytes.this;
            else {
                byte[] b = new byte[n * m];
                for (int i = 0, p = 0; i < n; i++, p += m) { System.arraycopy(value, 0, b, p, m); }
                return wrap(b);
            }
        }

        // PySequence.OfInt interface --------------------------------

        @Override
        public Spliterator.OfInt spliterator() { return new BytesSpliterator(); }

        @Override
        public Iterator<Integer> iterator() { return PyBytes.this.iterator(); }

        @Override
        public IntStream asIntStream() { return StreamSupport.intStream(spliterator(), false); }

        @Override
        public int compareTo(PySequence.Delegate<Integer, PyBytes> other) {
            Iterator<Integer> ib = other.iterator();
            for (int a : value) {
                if (ib.hasNext()) {
                    int b = ib.next();
                    // if a != b, then we've found an answer
                    if (a > b)
                        return 1;
                    else if (a < b)
                        return -1;
                } else
                    // value has not run out, but other has. We win.
                    return 1;
            }
            /*
             * The sequences matched over the length of value. The other is the
             * winner if it still has elements. Otherwise its a tie.
             */
            return ib.hasNext() ? -1 : 0;
        }

        /**
         * Compare for equality with a sequence. This is a little simpler
         * than {@code compareTo}.
         *
         * @param b another
         * @return whether values equal
         */
        boolean equals(BytesDelegate b) {
            // Lengths must be equal
            if (length() != b.length()) { return false; }
            // Scan the codes points in this.value and b
            Iterator<Integer> ib = b.iterator();
            for (int c : value) { if (c != ib.next()) { return false; } }
            return true;
        }
    }

    private static PyBytes concatBytes(PySequence.OfInt v, PySequence.OfInt w)
            throws OutOfMemoryError {
        int n = v.length(), m = w.length();
        byte[] b = new byte[n + m];
        IntStream.concat(v.asIntStream(), w.asIntStream()).forEach(new ByteStore(b, 0));
        return wrap(b);
    }

    /**
     * Inner class defining the return type of
     * {@link PyBytes#spliterator()}. We need this only because
     * {@link #tryAdvance(IntConsumer) tryAdvance} deals in java
     * {@code int}s, while our array is {@code byte[]}. There is no
     * ready-made {@code Spliterator.OfByte}, and if there were, it
     * would return signed values.
     */
    private class BytesSpliterator extends Spliterators.AbstractIntSpliterator {

        static final int flags = Spliterator.IMMUTABLE | Spliterator.SIZED | Spliterator.ORDERED;
        private int i = 0;

        BytesSpliterator() { super(value.length, flags); }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (i < value.length) {
                action.accept(0xff & value[i++]);
                return true;
            } else
                return false;
        }
    }

    /**
     * A consumer of primitive int values that stores them in an array
     * given it at construction.
     */
    private static class ByteStore implements IntConsumer {

        private final byte[] b;
        private int i = 0;

        ByteStore(byte[] bytes, int start) {
            this.b = bytes;
            this.i = start;
        }

        @Override
        public void accept(int value) { b[i++] = (byte)value; }
    }

    /**
     * Adapt a Python object to a sequence of Java {@code int} values or
     * throw an exception. If the method throws the special exception
     * {@link NoConversion}, the caller must catch it and deal with it,
     * perhaps by throwing a {@link TypeError}. A binary operation will
     * normally return {@link Py#NotImplemented} in that case.
     * <p>
     * Note that implementing {@link PySequence.OfInt} is not enough,
     * which other types may, but be incompatible in Python.
     *
     * @param v to wrap or return
     * @return adapted to a sequence
     * @throws NoConversion if {@code v} is not a Python {@code str}
     */
    static BytesDelegate adapt(Object v) throws NoConversion {
        // Check against supported types, most likely first
        if (v instanceof PyBytes /* || v instanceof PyByteArray */)
            return ((PyBytes)v).delegate;
        throw PyObjectUtil.NO_CONVERSION;
    }
}
