// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.modules;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.python.core.Abstract;
import org.python.core.CPython311Code;
import org.python.core.EOFError;
import org.python.core.Exposed.Default;
import org.python.core.Exposed.Member;
import org.python.core.Exposed.PythonStaticMethod;
import org.python.core.OSError;
import org.python.core.Py;
import org.python.core.PyBaseObject;
import org.python.core.PyBool;
import org.python.core.PyBytes;
import org.python.core.PyCode;
import org.python.core.PyDict;
import org.python.core.PyException;
import org.python.core.PyFloat;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyObjectUtil;
import org.python.core.PyObjectUtil.NoConversion;
import org.python.core.PySequence;
import org.python.core.PySequence.OfInt;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.core.StopIteration;
import org.python.core.TypeError;
import org.python.core.ValueError;
import org.python.core.stringlib.ByteArrayBuilder;
import org.python.core.stringlib.IntArrayBuilder;

/**
 * Write Python objects to files and read them back. This is primarily
 * intended for writing and reading compiled Python code, even though
 * {@code dict}s, {@code list}s, {@code set}s a nd {@code frozenset}s,
 * not commonly seen in {@code code} objects, are supported. Version 3
 * of this protocol properly supports circular links and sharing.
 */

public class marshal /* extends JavaModule */ {

    @Member("version")
    final static int VERSION = 4;

    /*
     * High water mark to determine when the marshalled object is
     * dangerously deep and risks coring the interpreter. When the
     * object stack gets this deep, raise an exception instead of
     * continuing.
     */
    private final static int MAX_MARSHAL_STACK_DEPTH = 2000;

    /*
     * Enumerate all the legal record types. Each corresponds to a type
     * of data, or a specific value, except {@code NULL}. The values are
     * the same as in the CPython marshal.c, but the names have been
     * clarified.
     */
    private final static int TYPE_NULL = '0';
    /** The record encodes {@code None} (in one byte) */
    private final static int TYPE_NONE = 'N';
    /** The record encodes {@code False} (in one byte) */
    private final static int TYPE_FALSE = 'F';
    /** The record encodes {@code True} (in one byte) */
    private final static int TYPE_TRUE = 'T';
    /** The record encodes the <b>type</b> {@code StopIteration} */
    private final static int TYPE_STOPITER = 'S';
    /** The record encodes {@code Ellipsis} (in one byte) */
    private final static int TYPE_ELLIPSIS = '.';
    /** The record encodes an {@code int} (4 bytes follow) */
    private final static int TYPE_INT = 'i';
    /*
     * TYPE_INT64 is not generated anymore. Supported for backward
     * compatibility only.
     */
    private final static int TYPE_INT64 = 'I';
    private final static int TYPE_FLOAT = 'f';
    private final static int TYPE_BINARY_FLOAT = 'g';
    private final static int TYPE_COMPLEX = 'x';
    private final static int TYPE_BINARY_COMPLEX = 'y';
    /** The record encodes an {@code int} (counted 15-bit digits) */
    private final static int TYPE_LONG = 'l';
    private final static int TYPE_BYTES = 's'; // not TYPE_STRING
    private final static int TYPE_INTERNED = 't'; // str
    private final static int TYPE_REF = 'r';
    /** The record encodes a {@code tuple} (counted objects follow) */
    private final static int TYPE_TUPLE = '(';
    /** The record encodes a {@code list} (counted objects follow) */
    private final static int TYPE_LIST = '[';
    /** The record encodes a {@code dict} (key-value pairs follow) */
    private final static int TYPE_DICT = '{';
    private final static int TYPE_CODE = 'c';
    /** The record encodes a {@code str} (counted code points follow) */
    private final static int TYPE_UNICODE = 'u'; // str
    private final static int TYPE_UNKNOWN = '?';
    private final static int TYPE_SET = '<';
    private final static int TYPE_FROZENSET = '>';

    /** The record encodes a {@code str} (counted bytes follow) */
    private final static int TYPE_ASCII = 'a'; // str
    private final static int TYPE_ASCII_INTERNED = 'A'; // str
    /** The record encodes a {@code tuple} (counted objects follow) */
    private final static int TYPE_SMALL_TUPLE = ')';
    /** The record encodes a {@code str} (counted bytes follow) */
    private final static int TYPE_SHORT_ASCII = 'z'; // str
    private final static int TYPE_SHORT_ASCII_INTERNED = 'Z'; // str

    /**
     * We add this to a {@code TYPE_*} code to indicate that the encoded
     * object is cached at the next free index. When reading, each
     * occurrence appends its object to a list (thus the encounter order
     * defines the index). When writing, we look in a cache to see if
     * the object has already been encoded (therefore given an index)
     * and if it has, we record the index instead in a {@link #TYPE_REF}
     * record. If it is new, we assign it the next index.
     */
    private final static int FLAG_REF = 0x80;

    /** A mask for the low 15 bits. */
    private final static int MASK15 = 0x7fff;
    /** A mask for the low 15 bits. */
    private final static BigInteger BIG_MASK15 =
            BigInteger.valueOf(MASK15);

    /**
     * We apply a particular {@code Decoder} to the stream after we read
     * a type code byte that tells us which one to use, to decode the
     * data following. If that code has no data following, then the
     * corresponding {@link Decoder#read(Reader)} returns a constant.
     */
    @FunctionalInterface
    private interface Decoder {
        /**
         * Read an object value, of a particular Python type, from the
         * input managed by a given {@link Reader}, the matching type
         * code having been read from it already. If specified through,
         * the second argument {@code ref}, the decoder will also
         * allocate the next reference index in the given reader to the
         * object created. (Decoders for simple constants that are
         * always re-used may ignore the {@code ref} argument.)
         *
         * @param r from which to read
         * @param ref if {@code true}, define a reference in {@code r}
         * @return the object value read
         */
        Object read(Reader r, boolean ref);
    }

    /**
     * A {@code Codec} groups together the code for writing and reading
     * instances of a particular Python type. The {@code write()} method
     * encodes a value of that type onto the stream, choosing from
     * available representations when there is more than one. The
     * {@code Codec} provides (potentially) multiple {@link Decoder}s,
     * one for each representation (type code), in a {@link Map}
     * supplied by the decoders() method.
     */
    interface Codec {
        /**
         * The Python type this codec is implemented to encode and
         * decode.
         *
         * @return target Python type
         */
        PyType type();

        /**
         * Write a value, of a particular Python type, onto the output
         * managed by the {@link Writer}.
         *
         * @param w to receive the data
         * @param v to be written
         * @throws IOException on file write errors
         * @throws ArrayIndexOutOfBoundsException on byte array write
         *     errors
         * @throws Throwable from operations on {@code v}
         */
        void write(Writer w, Object v) throws IOException, Throwable;

        /**
         * Return a mapping from each type code supported to a function
         * that is able to read the object following that type code, in
         * the input managed by a given {@link Reader}.
         *
         * @return the table of decoders
         */
        Map<Integer, Decoder> decoders();
    }

    /**
     * A mapping from Python type to the Codec that is able to encode
     * and decode that type. Note that the {@code null} key is
     * supported.
     */
    private static HashMap<PyType, Codec> codecForType =
            new HashMap<>();

    /**
     * A mapping from the type code to the {@link Decoder} able to
     * render the record as a Python object.
     */
    private static HashMap<Integer, Decoder> decoderForCode =
            new HashMap<>();

    /**
     * Associate a codec with its target Python type in
     * {@link #codecForType} and each read method it supplies with the
     * type code it supports.
     *
     * @param codec to register
     */
    private static void register(Codec codec) {
        // Get the type served (object for reference, null for null).
        PyType targetType = codec.type();
        codecForType.put(targetType, codec);
        // Register a read method for each type code
        for (Map.Entry<Integer, Decoder> e : codec.decoders()
                .entrySet()) {
            Decoder d = decoderForCode.put(e.getKey(), e.getValue());
            assert d == null; // No codec should duplicate a code
        }
    }

    // Register all the defined codecs
    static {
        register(new TypeCodec());
        register(new BoolCodec());
        register(new IntCodec());
        register(new FloatCodec());

        register(new BytesCodec());
        register(new StrCodec());
        register(new TupleCodec());
        register(new ListCodec());
        register(new DictCodec());

        register(new CodeCodec());

        register(new RefCodec());
    }

    /**
     * {@code marshal.dump(value, file, version=4)}: Write the value on
     * the open file. The value must be a supported type. The file must
     * be a writable binary file.
     *
     * @param value to write
     * @param file on which to write
     * @param version of the format to use
     * @throws ValueError if the value has (or contains an object that
     *     has) an unsupported type
     * @throws OSError from file operations
     */
    @PythonStaticMethod
    public static void dump(Object value, Object file,
            @Default("4") int version) throws ValueError, OSError {
        try (OutputStream os = StreamWriter.adapt(file)) {
            Writer writer = new StreamWriter(os, version);
            writer.writeObject(value);
        } catch (NoConversion | IOException e) {
            throw Abstract.argumentTypeError("dump", "file",
                    "a file-like object with write", file);
        }
    }

    /**
     * {@code marshal.load(file)}: read one value from an open file and
     * return it. If no valid value is read (e.g. because the data has
     * an incompatible marshal format), raise {@code EOFError},
     * {@code ValueError} or {@code TypeError}. The file must be a
     * readable binary file.
     *
     * @param file to read
     * @return the object read
     * @throws ValueError when an object being read is over-size or
     *     contains values out of range.
     * @throws TypeError when file reading returns non-byte data or a
     *     container contains a null element.
     * @throws EOFError when a partial object is read
     * @throws OSError from file operations generally
     */
    @PythonStaticMethod
    public static Object load(Object file) {
        try (InputStream is = StreamReader.adapt(file)) {
            Reader reader = new StreamReader(is);
            return reader.readObject();
        } catch (NoConversion | IOException e) {
            throw Abstract.argumentTypeError("load", "file",
                    "a file-like object with read", file);
        }
    }

    /**
     * {@code marshal.dumps(value, version=4)}: Return a {@code bytes}
     * object into which the given value has been written, as to a file
     * using {@link #dump(Object, Object, int)}. The value must be a
     * supported type.
     *
     * @param value to write
     * @param version of the format to use
     * @return {@code bytes} containing result
     * @throws ValueError if the value has (or contains an object that
     *     has) an unsupported type
     */
    @PythonStaticMethod
    public static PyBytes dumps(Object value, @Default("4") int version)
            throws ValueError {
        ByteArrayBuilder bb = new ByteArrayBuilder();
        Writer writer = new BytesWriter(bb, version);
        writer.writeObject(value);
        return new PyBytes(bb);
    }

    /**
     * {@code marshal.loads(bytes)}: read one value from a bytes-like
     * object and return it. If no valid value is read, raise
     * {@code EOFError}, {@code ValueError} or {@code TypeError}.
     *
     * @param bytes to read
     * @return the object read
     * @throws ValueError when an object being read is over-size or
     *     contains values out of range.
     * @throws TypeError when a container contains a null element.
     * @throws EOFError when a partial object is read
     */
    @PythonStaticMethod
    public static Object loads(Object bytes) {
        try {
            ByteBuffer bb = BytesReader.adapt(bytes);
            Reader reader = new BytesReader(bb);
            return reader.readObject();
        } catch (NoConversion nc) {
            throw Abstract.argumentTypeError("loads", "bytes",
                    "a bytes-like object", bytes);
        }
    }

    /**
     * A {@code marshal.Writer} holds an {@code OutputStream} during the
     * time that the {@code marshal} module is serialising objects to
     * it. It provides operations to write individual field values to
     * the stream, that support classes extending {@link Codec} in their
     * implementation of {@link Codec#write(Writer, Object) write()}.
     * <p>
     * The wrapped {@code OutputStream} may be writing to a file or to
     * an array.
     */
    abstract static class Writer {

        /**
         * Version of the protocol this {@code Writer} is supposed to
         * write.
         */
        private final int version;

        /**
         * Create a {@code Writer} with a specified version of the
         * protocol. The version affects whether certain type codes will
         * be used.
         *
         * @param version of protocol to write
         */
        public Writer(int version) { this.version = version; }

        /**
         * Encode a complete object.
         *
         * @param obj to encode
         */
        public void writeObject(Object obj) {}

        /**
         * Write one {@code byte} onto the destination. The parameter is
         * an {@code int} because it may be the result of a calculation,
         * but only the the low 8 bits are used.
         *
         * @param v to write
         */
        abstract void writeByte(int v);

        /**
         * Write one {@code short} onto the destination. The parameter
         * is an {@code int} because it may be the result of a
         * calculation, but only the the low 16 bits are used.
         *
         * @param v to write
         */
        abstract void writeShort(int v);

        /**
         * Write one {@code int} onto the destination.
         *
         * @param v to write
         */
        abstract void writeInt(int v);

        /**
         * Write one {@code long} onto the destination.
         *
         * @param v to write
         */
        abstract void writeLong(long v);

        /**
         * Write one {@code float} onto the destination (8 bytes).
         *
         * @param v to write
         */
        void writeDouble(double v) {
            long bits = Double.doubleToLongBits(v);
            writeLong(bits);
        }

        /**
         * Write multiple {@code byte}s onto the destination supplied as
         * an integer sequence. Only the the low 8 bits of each element
         * are used.
         *
         * @param seq to write
         */
        void writeBytes(OfInt seq) {
            seq.asIntStream().forEachOrdered(v -> writeByte(v));
        }

        /**
         * Write multiple {@code int}s onto the destination supplied as
         * an integer sequence.
         *
         * @param seq to write
         */
        void writeInts(OfInt seq) {
            seq.asIntStream().forEachOrdered(v -> writeInt(v));
        }

        /**
         * Write a {@code BigInteger} as a counted sequence of 15-bit
         * units (the form Python expects).
         *
         * @param v value to write
         */
        void writeBigInteger(BigInteger v) {
            boolean negative = v.signum() < 0;
            if (negative) { v = v.negate(); }
            int size = (v.bitLength() + 14) / 15;
            writeInt(negative ? -size : size);
            for (int i = 0; i < size; i++) {
                writeShort(v.and(BIG_MASK15).intValue());
                v = v.shiftRight(15);
            }
        }

        /**
         * Construct a ValueError expressing the impossibility of
         * marshalling whatever it is.
         *
         * @param v object we couldn't marshal.
         * @return throwable exception
         */
        protected static ValueError unmarshallableObject(Object v) {
            String t = v == null ? "<null>"
                    : "of type '" + PyType.of(v).getName() + "'";
            return new ValueError("unmarshallable object %s", t);
        }
    }

    /**
     * A {@link Writer} that has a {@code java.io.OutputStream} as its
     * destination. When the underlying destination is a file, it is
     * preferable for efficiency that this be a
     * {@code java.io.BufferedOutputStream}. A
     * {@code java.io.ByteArrayOutputStream} needs no additional
     * buffering.
     */
    static class StreamWriter extends Writer {

        /**
         * The destination wrapped in a {@code DataOutputStream} on
         * which we shall call {@code getInt()} etc. to write items. A
         * Python marshal stream is little-endian, while Java will write
         * big-endian data. However, note that
         * {@code Integer.reverseBytes()} and friends are HotSpot
         * intrinsics.
         */
        private final DataOutputStream file;

        /**
         * Form a {@link Writer} on a {@code java.io.OutputStream}.
         *
         * @param file output
         * @param version of protocol to write
         */
        StreamWriter(OutputStream file, int version) {
            super(version);
            this.file = new DataOutputStream(file);
        }

        @Override
        void writeByte(int b) {
            try {
                file.write(b);
            } catch (IOException ioe) {
                throw new OSError(ioe);
            }
        }

        @Override
        void writeShort(int v) {
            try {
                file.writeShort(Short.reverseBytes((short)v));
            } catch (IOException ioe) {
                throw new OSError(ioe);
            }
        }

        @Override
        void writeInt(int v) {
            try {
                file.writeInt(Integer.reverseBytes(v));
            } catch (IOException ioe) {
                throw new OSError(ioe);
            }
        }

        @Override
        void writeLong(long v) {
            try {
                file.writeLong(Long.reverseBytes(v));
            } catch (IOException ioe) {
                throw new OSError(ioe);
            }
        }

        @Override
        void writeBytes(OfInt seq) {
            seq.asIntStream().forEachOrdered(v -> writeByte(v));
        }

        @Override
        void writeInts(OfInt seq) {
            seq.asIntStream().forEachOrdered(v -> writeInt(v));
        }

        /**
         * Recognise or wrap an eligible file-like data sink as an
         * {@code OutputStream}.
         */
        private static OutputStream adapt(Object file)
                throws NoConversion {
            if (file instanceof OutputStream) {
                return (OutputStream)file;
            } else {
                // Adapt any object with write accepting a byte
                // But for now ...
                throw PyObjectUtil.NO_CONVERSION;
            }
        }
    }

    /**
     * A {@link Writer} that has a {@link ByteArrayBuilder} as its
     * destination.
     */
    static class BytesWriter extends Writer {

        /**
         * The destination {@link ByteArrayBuilder} on which we write
         * little-endian
         */
        final ByteArrayBuilder builder;

        /**
         * Form a {@link Writer} on a byte array.
         *
         * @param builder destination
         * @param version of protocol to write
         */
        BytesWriter(ByteArrayBuilder builder, int version) {
            super(version);
            this.builder = builder;
        }

        @Override
        void writeByte(int v) { builder.append(v); }

        @Override
        void writeShort(int v) { builder.appendShortLE(v); }

        @Override
        void writeInt(int v) { builder.appendIntLE(v); }

        @Override
        void writeLong(long v) { builder.appendLongLE(v); }

        @Override
        void writeBytes(OfInt seq) { builder.append(seq); }

        @Override
        void writeInts(OfInt seq) {
            seq.asIntStream()
                    .forEachOrdered(v -> builder.appendIntLE(v));
        }
    }

    /**
     * A {@code marshal.Reader} holds either an {@code InputStream} or a
     * {@code java.nio.ByteBuffer} (maybe wrapping a {@code byte[]})
     * provided by the caller, from which it will read during the time
     * that the {@code marshal} module is de-serialising objects from
     * it. It provides operations to read individual field values from
     * this source, that support classes extending {@link Codec} in
     * their implementation of decoding methods registered against the
     * type codes they support. (See also {@link Codec#decoders()}.
     */
    public abstract static class Reader {

        /**
         * Objects read from the source may have been marked (by the
         * {@link Writer}) as defining potentially shared objects, and
         * are assigned an index (one up from zero) as they are
         * encountered. In other places within the same source, where
         * one of those occurs, a record beginning
         * {@link marshal#TYPE_REF} is created with only the
         * corresponding index as payload. This list is where we collect
         * those objects (in encounter order) so as to map an index to
         * an object.
         */
        // Allocate generous initial size for typical code object
        protected List<Object> refs = new ArrayList<Object>();

        /**
         * Decode a complete object from the source.
         *
         * @return the object read
         */
        public Object readObject() {
            // Get the type code and the decoder for it
            int tcflag = readByte(), tc = tcflag & ~FLAG_REF;
            Decoder decoder = decoderForCode.get(tc);
            // The decoder will define a reference if requested
            boolean ref = (tcflag & FLAG_REF) != 0;
            if (decoder != null) {
                // Decode using the decoder we retrieved for tc
                Object obj = decoder.read(this, ref);
                if (tc != TYPE_NULL && obj == null) {
                    throw nullObject("object");
                }
                return obj;
            } else {
                // No decoder registered for tc (see static init)
                throw badData("unknown type 0x%02x = '%c'%s", tcflag,
                        tc, ref ? "+ref" : "");
            }
        }

        /**
         * Read one {@code byte} from the source (as an unsigned
         * integer), advancing the stream one byte.
         *
         * @return byte read unsigned
         */
        // Compare CPython r_byte in marshal.c
        public abstract int readByte();

        /**
         * Read one {@code short} value from the source, advancing the
         * stream 2 bytes.
         *
         * @return value read
         */
        // Compare CPython r_int in marshal.c
        public abstract int readShort();

        /**
         * Read one {@code int} value from the source, advancing the
         * stream 4 bytes.
         *
         * @return value read
         */
        // Compare CPython r_long in marshal.c
        public abstract int readInt();

        /**
         * Read one {@code long} value from the source, advancing the
         * stream 8 bytes.
         *
         * @return value read
         */
        // Compare CPython r_long64 in marshal.c
        public abstract long readLong();

        /**
         * Read one {@code float} value from the source, advancing the
         * stream 8 bytes.
         *
         * @return value read
         */
        // Compare CPython r_float_bin in marshal.c
        public double readDouble() {
            long bits = readLong();
            return Double.longBitsToDouble(bits);
        }

        /**
         * Read a given number of {@code byte}s from the source and
         * present them as a read-only, little-endian,
         * {@code java.nio.ByteBuffer}, advancing the stream over these
         * bytes.
         *
         * @param n number of bytes to read
         * @return the next {@code n} bytes
         */
        // Compare CPython r_byte in marshal.c
        public abstract ByteBuffer readByteBuffer(int n);

        /**
         * Read one {@code BigInteger} value from the source, advancing
         * the stream a variable number of bytes.
         *
         * @return value read
         */
        // Compare CPython r_PyLong in marshal.c
        BigInteger readBigInteger() throws ValueError {
            // Encoded as size and 15-bit digits
            int size = readInt();
            if (size == Integer.MIN_VALUE) {
                throw badData("size out of range in big int");
            }

            // Size carries the sign
            boolean negative = size < 0;
            size = Math.abs(size);

            // Or each digit as we read it into v
            BigInteger v = BigInteger.ZERO;
            for (int i = 0, shift = 0; i < size; i++, shift += 15) {
                int digit = readShort();
                if ((digit & ~MASK15) != 0) {
                    // Bits set where they shouldn't be
                    throw badData("digit out of range in big int");
                }
                BigInteger d = BigInteger.valueOf(digit);
                v = (i == 0) ? d : v.or(d.shiftLeft(shift));
            }

            // Sign from size
            if (negative) { v = v.negate(); }
            return v;
        }

        /**
         * Reserve an index in the list of references for use later. The
         * entry will be {@code null} until replaced by the caller with
         * the genuine object.
         * <p>
         * We do this when reading objects that cannot be constructed
         * until their fields or elements have been constructed (such as
         * {@code tuple} and {@code code}), since objects take reference
         * numbers in the order they are encountered in the stream (both
         * reading and writing).
         *
         * @return the index that has been reserved
         */
        // Compare CPython r_ref_reserve() in marshal.c
        private int reserveRef() {
            int idx = refs.size();
            refs.add(null);
            return idx;
        }

        /**
         * Insert a new object {@code o} into the {@link #refs} list at
         * the index {@code idx} previously allocated by
         * {@link #reserveRef()}. If the index {@code idx&lt;0} there is
         * no insertion. (This is an implementation convenience for
         * codecs.)
         *
         * @param <T> type of object referred to
         * @param o object to insert or {@code null} (ignored)
         * @param idx previously allocated index
         * @return {@code o}
         * @throws IndexOutOfBoundsException on a bad index
         */
        // Compare CPython r_ref_insert() in marshal.c
        private <T> T defineRef(T o, int idx) {
            if (o != null && idx >= 0) { refs.set(idx, o); }
            return o;
        }

        /**
         * Add the object to the known references, if required.
         *
         * @param <T> type of object referred to
         * @param o to make the target of a reference.
         * @param ref if {@code true}, define a reference in {@code r}
         * @return {@code o}
         */
        // Compare CPython r_ref() or R_REF() in marshal.c
        private <T> T defineRef(T o, boolean ref) {
            if (ref && o != null) { refs.add(o); }
            return o;
        }

        /**
         * Prepare a Python {@link PyException} for throwing, based on
         * the Java {@code IOException}. We may return a Python
         * {@link EOFError} or {@link OSError}.
         *
         * @param ioe to convert
         * @return the chosen Python exception
         */
        protected PyException pyException(IOException ioe) {
            if (ioe instanceof EOFException) {
                return endOfData();
            } else {
                return new OSError(ioe);
            }
        }

        /**
         * Prepare a Python {@link EOFError} for throwing, with the
         * message that the data are too short. We throw one of these on
         * encountering and end of file or buffer where more of the
         * object was expected.
         *
         * @return a Python exception to throw
         */
        protected static EOFError endOfData() {
            return new EOFError("marshal data too short");
        }

        /**
         * Create a {@link ValueError} to throw, with a message along
         * the lines "bad marshal data (REASON(args))"
         *
         * @param reason to insert
         * @param args arguments to fill format
         * @return to throw
         */
        protected static ValueError badData(String reason,
                Object... args) {
            return badData(String.format(reason, args));
        }

        /**
         * Create a {@link ValueError} to throw, with a message along
         * the lines "bad marshal data (REASON)"
         *
         * @param reason to insert
         * @return to throw
         */
        protected static ValueError badData(String reason) {
            return new ValueError("bad marshal data (%s)", reason);
        }

        /**
         * Create a {@link TypeError} to throw, with a message along the
         * lines "null object in marshal data for (TYPE)"
         *
         * @param type to insert
         * @return to throw
         */
        protected static TypeError nullObject(String type) {
            return new TypeError("null object in marshal data for %s",
                    type);
        }
    }

    /**
     * A {@link Reader} that has a {@code java.io.InputStream} as its
     * source. When the underlying source is a file, it is preferable
     * for efficiency that this be a
     * {@code java.io.BufferedInputStream}. A
     * {@code java.io.ByteArrayInputStream} needs no additional
     * buffering.
     */
    public static class StreamReader extends Reader {

        /**
         * The source wrapped in a {@code DataInputStream} on which we
         * shall call {@code getInt()} etc. to read items. A Python
         * marshal stream is little-endian, while Java will read
         * big-endian data. However, note that
         * {@code Integer.reverseBytes()} and friends are HotSpot
         * intrinsics.
         */
        private final DataInputStream file;

        /**
         * Form a {@link Reader} on a {@code java.io.InputStream}.
         *
         * @param file input
         */
        public StreamReader(InputStream file) {
            this.file = new DataInputStream(file);
        }

        @Override
        public int readByte() {
            try {
                return file.readByte() & 0xff;
            } catch (IOException ioe) {
                throw new OSError(ioe);
            }
        }

        @Override
        public int readShort() {
            try {
                return Short.reverseBytes(file.readShort());
            } catch (IOException ioe) {
                throw new OSError(ioe);
            }
        }

        @Override
        public int readInt() {
            try {
                return Integer.reverseBytes(file.readInt());
            } catch (IOException ioe) {
                throw new OSError(ioe);
            }
        }

        @Override
        public long readLong() {
            try {
                return Long.reverseBytes(file.readLong());
            } catch (IOException ioe) {
                throw new OSError(ioe);
            }
        }

        @Override
        public ByteBuffer readByteBuffer(int n) {
            try {
                byte[] b = new byte[n];
                file.read(b);
                ByteBuffer slice = ByteBuffer.wrap(b).asReadOnlyBuffer()
                        .order(ByteOrder.LITTLE_ENDIAN);
                return slice;
            } catch (IOException ioe) {
                throw new OSError(ioe);
            }
        }

        /**
         * Recognise or wrap an eligible file-like data source as an
         * {@code InputStream}.
         */
        private static InputStream adapt(Object file)
                throws NoConversion {
            if (file instanceof InputStream) {
                return (InputStream)file;
            } else {
                // Adapt any object with read returning bytes
                // But for now ...
                throw PyObjectUtil.NO_CONVERSION;
            }
        }
    }

    /**
     * A {@link Reader} that has a {@code ByteBuffer} as its source.
     */
    public static class BytesReader extends Reader {

        /**
         * The source as little-endian a {@code ByteBuffer} on which we
         * shall call {@code getInt()} etc. to read items. A Python
         * marshal stream is little-endian
         */
        private final ByteBuffer buf;

        /**
         * Form a {@link Reader} on a byte array.
         *
         * @param bytes input
         */
        public BytesReader(byte[] bytes) {
            this(ByteBuffer.wrap(bytes));
        }

        /**
         * Form a {@link Reader} on an existing {@code ByteBuffer}. This
         * {@code ByteBuffer} will have its order set to
         * {@code ByteOrder.LITTLE_ENDIAN}.
         *
         * @param buf input
         */
        public BytesReader(ByteBuffer buf) {
            this.buf = buf;
            buf.order(ByteOrder.LITTLE_ENDIAN);
        }

        @Override
        public int readByte() {
            try {
                return buf.get() & 0xff;
            } catch (BufferUnderflowException boe) {
                throw endOfData();
            }
        }

        @Override
        public int readShort() {
            try {
                return buf.getShort();
            } catch (BufferUnderflowException boe) {
                throw endOfData();
            }
        }

        @Override
        public int readInt() {
            try {
                return buf.getInt();
            } catch (BufferUnderflowException boe) {
                throw endOfData();
            }
        }

        @Override
        public long readLong() {
            try {
                return buf.getLong();
            } catch (BufferUnderflowException boe) {
                throw endOfData();
            }
        }

        @Override
        public ByteBuffer readByteBuffer(int n) {
            try {
                ByteBuffer slice =
                        buf.slice().order(ByteOrder.LITTLE_ENDIAN);
                // The n bytes are read, as far as buf is concerned
                buf.position(buf.position() + n);
                // And we set the limit in slice at their end
                return slice.limit(n);
            } catch (BufferUnderflowException boe) {
                throw endOfData();
            }
        }

        /**
         * Recognise or wrap an eligible file-like data source as a
         * {@code ByteBuffer}.
         */
        private static ByteBuffer adapt(Object bytes)
                throws NoConversion {
            if (bytes instanceof ByteBuffer) {
                return (ByteBuffer)bytes;
            } else if (bytes instanceof PyBytes) {
                return ((PyBytes)bytes).getNIOByteBuffer();
            } else {
                if (bytes instanceof byte[]) {
                    ByteBuffer bb = ByteBuffer.wrap((byte[])bytes);
                    return bb;
                } else {
                    // Adapt any object with read returning bytes
                    // But for now ...
                    throw PyObjectUtil.NO_CONVERSION;
                }
            }
        }
    }

    /** {@link Codec} for several Python singletons. */
    private static class SingletonCodec implements Codec {
        private final int typeCode;
        private final Object value;

        private SingletonCodec(int typeCode, Object value) {
            this.typeCode = typeCode;
            this.value = value;
        }

        @Override
        public PyType type() {
            /*
             * It is possible to serialise a null, and reading it back
             * is not always an error.
             */
            return value == null ? null : PyType.of(value);
        }

        @Override
        public void write(Writer w, Object v)
                throws IOException, Throwable {
            // All objects of the value's type are considered the same
            assert v == value;
            w.writeByte(typeCode);
        }

        @Override
        public Map<Integer, Decoder> decoders() {
            // All this type share the same value (ref ignored)
            return Map.of(typeCode, (r, ref) -> value);
        }
    }

    static {
        register(new SingletonCodec(TYPE_NULL, null));
        register(new SingletonCodec(TYPE_NONE, Py.None));
        // register(new SingletonCodec(TYPE_ELLIPSIS, Py.Ellipsis));
    }

    /**
     * {@link Codec} for {@code type}s. The only case of this in
     * practice is the type {@code StopIteration}, but this codec will
     * receive any any type object.
     */
    private static class TypeCodec implements Codec {

        @Override
        public PyType type() { return PyType.TYPE; }

        @Override
        public void write(Writer w, Object v)
                throws IOException, Throwable {
            if (v == StopIteration.TYPE)
                w.writeByte(TYPE_STOPITER);
            else
                throw Writer.unmarshallableObject(v);
        }

        @Override
        public Map<Integer, Decoder> decoders() {
            // All this type share the same value (ref ignored)
            return Map.of(TYPE_STOPITER,
                    (r, ref) -> StopIteration.TYPE);
        }
    }

    /** {@link Codec} for Python {@code bool}. */
    private static class BoolCodec implements Codec {
        @Override
        public PyType type() { return PyBool.TYPE; }

        @Override
        public void write(Writer w, Object v)
                throws IOException, Throwable {
            assert type().checkExact(v);
            // Must be Boolean
            w.writeByte((Boolean)v ? TYPE_TRUE : TYPE_FALSE);
        }

        @Override
        public Map<Integer, Decoder> decoders() {
            return Map.of( //
                    TYPE_FALSE, (r, ref) -> Py.False, //
                    TYPE_TRUE, (r, ref) -> Py.True);
        }
    }

    /** {@link Codec} for Python {@code int}. */
    private static class IntCodec implements Codec {
        @Override
        public PyType type() { return PyLong.TYPE; }

        @Override
        public void write(Writer w, Object v)
                throws IOException, Throwable {
            assert type().checkExact(v);
            // May be Integer or BigInteger
            if (v instanceof Integer) {
                w.writeByte(TYPE_INT);
                w.writeInt(((Integer)v).intValue());
            } else {
                w.writeByte(TYPE_LONG);
                w.writeBigInteger((BigInteger)v);
            }
        }

        @Override
        public Map<Integer, Decoder> decoders() {
            Map<Integer, Decoder> m = new HashMap<>();
            m.put(TYPE_INT, (r, ref) -> r.defineRef(r.readInt(), ref));
            m.put(TYPE_LONG,
                    (r, ref) -> r.defineRef(r.readBigInteger(), ref));
            return m;
        }
    }

    /** {@link Codec} for Python {@code float}. */
    private static class FloatCodec implements Codec {
        @Override
        public PyType type() { return PyFloat.TYPE; }

        @Override
        public void write(Writer w, Object v) {
            // May be Double or PyFloat
            double d = PyFloat.doubleValue(v);
            if (w.version > 1) {
                w.writeByte(TYPE_BINARY_FLOAT);
                w.writeDouble(d);
            } else {
                PyUnicode u = PyUnicode
                        .fromJavaString(String.format("%17.0g", d));
                PySequence.OfInt seq = u.asSequence();
                w.writeByte(TYPE_FLOAT);
                w.writeBytes(seq);
            }
        }

        @Override
        public Map<Integer, Decoder> decoders() {
            Map<Integer, Decoder> m = new HashMap<>();
            m.put(TYPE_BINARY_FLOAT,
                    (r, ref) -> r.defineRef(r.readDouble(), ref));
            m.put(TYPE_FLOAT, FloatCodec::readStr);
            return m;
        }

        private static Object readStr(Reader r, boolean ref) {
            int n = r.readInt();
            ByteArrayBuilder builder = new ByteArrayBuilder(n);
            for (int i = 0; i < n; i++) {
                builder.append(r.readByte());
            }
            return r.defineRef(new PyBytes(builder), ref);
        }
    }

    /** {@link Codec} for Python {@code bytes}. */
    private static class BytesCodec implements Codec {
        @Override
        public PyType type() { return PyBytes.TYPE; }

        @Override
        public void write(Writer w, Object v)
                throws IOException, Throwable {
            assert type().checkExact(v);
            write(w, (PyBytes)v);
        }

        private static void write(Writer w, PyBytes v)
                throws IOException, Throwable {
            int n = PySequence.size(v);
            w.writeByte(TYPE_BYTES);
            w.writeInt(n);
            w.writeBytes(v.asSequence());
        }

        @Override
        public Map<Integer, Decoder> decoders() {
            return Map.of(TYPE_BYTES, BytesCodec::read);
        }

        private static Object read(Reader r, boolean ref) {
            int n = r.readInt();
            ByteArrayBuilder builder = new ByteArrayBuilder(n);
            for (int i = 0; i < n; i++) {
                builder.append(r.readByte());
            }
            return r.defineRef(new PyBytes(builder), ref);
        }
    }

    /** {@link Codec} for Python {@code str}. */
    private static class StrCodec implements Codec {
        @Override
        public PyType type() { return PyUnicode.TYPE; }

        @Override
        public void write(Writer w, Object v)
                throws IOException, Throwable {
            assert type().checkExact(v);
            if (v instanceof String) {
                write(w, PyUnicode.fromJavaString((String)v));
            } else {
                write(w, (PyUnicode)v);
            }
        }

        private static void write(Writer w, PyUnicode v)
                throws IOException, Throwable {
            int n = PySequence.size(v);
            if (w.version >= 4 && v.isascii()) {
                if (n < 256) {
                    w.writeByte(TYPE_SHORT_ASCII);
                    w.writeInt(n);
                } else {
                    w.writeByte(TYPE_ASCII);
                    w.writeInt(n);
                }
                w.writeBytes(v.asSequence());
            } else {
                w.writeByte(TYPE_UNICODE);
                w.writeInt(n);
                w.writeInts(v.asSequence());
            }
        }

        @Override
        public Map<Integer, Decoder> decoders() {
            Map<Integer, Decoder> m = new HashMap<>();
            m.put(TYPE_ASCII,
                    (r, ref) -> readAscii(r, ref, r.readInt(), false));
            m.put(TYPE_SHORT_ASCII,
                    (r, ref) -> readAscii(r, ref, r.readByte(), false));
            m.put(TYPE_ASCII_INTERNED,
                    (r, ref) -> readAscii(r, ref, r.readInt(), true));
            m.put(TYPE_SHORT_ASCII_INTERNED,
                    (r, ref) -> readAscii(r, ref, r.readByte(), true));
            m.put(TYPE_UNICODE,
                    (r, ref) -> readUtf8(r, ref, r.readInt(), false));
            m.put(TYPE_INTERNED,
                    (r, ref) -> readUtf8(r, ref, r.readInt(), true));
            return m;
        }

        private static Charset ASCII = Charset.forName("ASCII");
        private static Charset UTF8 = Charset.forName("UTF-8");

        private static Object readAscii(Reader r, boolean ref, int n,
                boolean interned) {
            ByteBuffer buf = r.readByteBuffer(n);
            CharBuffer cb = ASCII.decode(buf);
            String s = cb.toString();
            if (interned) { s = s.intern(); }
            return r.defineRef(s, ref);
        }

        private static String readUtf8(Reader r, boolean ref, int n,
                boolean interned) {
            ByteBuffer buf = r.readByteBuffer(n);
            // XXX use our own codec (& 'surrogatepass') when available
            CharBuffer cb = UTF8.decode(buf);
            // Note cb is chars, not code points so cp-length unknown
            IntArrayBuilder builder = new IntArrayBuilder();
            builder.append(cb.codePoints());
            // ??? Always a String, even if not BMP
            String s = builder.toString();
            if (interned) { s = s.intern(); }
            return r.defineRef(s, ref);
        }
    }

    /**
     * {@link Codec} for Python {@code int}. A {@code tuple} cannot
     * contain itself as a member.
     */
    private static class TupleCodec implements Codec {
        @Override
        public PyType type() { return PyTuple.TYPE; }

        @Override
        public void write(Writer w, Object v)
                throws IOException, Throwable {
            assert type().checkExact(v);
            // May only be PyTuple
            PyTuple tuple = (PyTuple)v;
            int n = tuple.size();
            // Version 4+ supports a small tuple
            if (w.version >= 4 && n < 256) {
                w.writeByte(TYPE_SMALL_TUPLE);
                w.writeByte(n);
            } else {
                w.writeByte(TYPE_TUPLE);
                w.writeInt(n);
            }
            // Write out the body of the tuple
            for (int i = 0; i < n; i++) { w.writeObject(tuple.get(i)); }
        }

        @Override
        public Map<Integer, Decoder> decoders() {
            return Map.of(//
                    TYPE_TUPLE, TupleCodec::read, // ;
                    TYPE_SMALL_TUPLE, TupleCodec::readSmall);
        }

        private static PyTuple read(Reader r, boolean ref) {
            return read(r, ref, r.readInt());
        }

        private static PyTuple readSmall(Reader r, boolean ref) {
            return read(r, ref, r.readByte());
        }

        private static PyTuple read(Reader r, boolean ref, int n) {
            // We may allocate a tuple builder of the right size
            if (n < 0) {
                throw Reader.badData("tuple size out of range");
            }
            PyTuple.Builder builder = new PyTuple.Builder(n);
            // Get an index now to ensure encounter-order numbering
            int idx = ref ? r.reserveRef() : -1;
            for (int i = 0; i < n; i++) {
                Object v = r.readObject();
                if (v == null) { throw Reader.nullObject("tuple"); }
                builder.append(v);
            }
            // Now we can give an object meaning to the index
            return r.defineRef(builder.take(), idx);
        }
    }

    /**
     * {@link Codec} for Python {@code list}. An interesting thing about
     * a {@code list} is that it can contain itself as a member.
     */
    private static class ListCodec implements Codec {
        @Override
        public PyType type() { return PyList.TYPE; }

        @Override
        public void write(Writer w, Object v)
                throws IOException, Throwable {
            assert type().checkExact(v);
            // May only be PyList
            PyList list = (PyList)v;
            w.writeByte(TYPE_LIST);
            int n = list.size();
            w.writeInt(n);
            for (int i = 0; i < n; i++) { w.writeObject(list.get(i)); }
        }

        @Override
        public Map<Integer, Decoder> decoders() {
            return Map.of(TYPE_LIST, ListCodec::read);
        }

        private static PyList read(Reader r, boolean ref) {
            // We may allocate a list of the right size
            int n = r.readInt();
            if (n < 0) {
                throw Reader.badData("list size out of range");
            }
            PyList list = new PyList(n);
            // Cache the object now: list may contain itself
            r.defineRef(list, ref);
            for (int i = 0; i < n; i++) {
                Object v = r.readObject();
                if (v == null) { throw Reader.nullObject("list"); }
                list.add(v);
            }
            return list;
        }
    }

    /**
     * {@link Codec} for Python {@code dict}.
     */
    private static class DictCodec implements Codec {
        @Override
        public PyType type() { return PyDict.TYPE; }

        @Override
        public void write(Writer w, Object v)
                throws IOException, Throwable {
            assert type().checkExact(v);
            // May only be PyDict
            PyDict dict = (PyDict)v;
            w.writeByte(TYPE_DICT);
            // The sequel is a null-terminated key-value pairs
            for (Map.Entry<Object, Object> e : dict.entrySet()) {
                w.writeObject(e.getKey());
                w.writeObject(e.getValue());
            }
            w.writeObject(null);
        }

        @Override
        public Map<Integer, Decoder> decoders() {
            return Map.of(TYPE_DICT, DictCodec::read);
        }

        private static PyDict read(Reader r, boolean ref) {
            // The sequel is a null-terminated key-value pairs
            PyDict dict = new PyDict();
            // Cache the object now: dict may contain itself
            r.defineRef(dict, ref);
            // The sequel is a null-terminated key-value pairs
            while (true) {
                Object key = r.readObject();
                if (key == null) { break; }
                // CPython does not treat (k,null) as an error
                Object value = r.readObject();
                if (value == null) { break; }
                dict.put(key, value);
            }
            return dict;
        }
    }

    /**
     * {@link Codec} for Python {@code code}.
     */
    private static class CodeCodec implements Codec {
        @Override
        public PyType type() { return PyCode.TYPE; }

        @Override
        public void write(Writer w, Object v)
                throws IOException, Throwable {
            assert type().checkExact(v);
            /*
             * We intend different concrete sub-classes of PyCode, that
             * create different frame types, but at the moment only one.
             */
            CPython311Code code = (CPython311Code)v;
            w.writeByte(TYPE_CODE);
            // XXX Write the fields (quite complicated)
        }

        @Override
        public Map<Integer, Decoder> decoders() {
            return Map.of(TYPE_CODE, CodeCodec::read);
        }

        private static CPython311Code read(Reader r, boolean ref) {

            // Get an index now to ensure encounter-order numbering
            int idx = ref ? r.reserveRef() : -1;

            int argcount = r.readInt();
            int posonlyargcount = r.readInt();
            int kwonlyargcount = r.readInt();
            int stacksize = r.readInt();

            int flags = r.readInt();
            Object code = r.readObject();

            Object consts = r.readObject();
            Object names = r.readObject();
            Object localsplusnames = r.readObject();
            Object localspluskinds = r.readObject();

            Object filename = r.readObject();
            Object name = r.readObject();
            Object qualname = r.readObject();

            int firstlineno = r.readInt();
            Object linetable = r.readObject();
            Object exceptiontable = r.readObject();

            // PySys_Audit("code.__new__", blah ...);

            CPython311Code v = CPython311Code.create( //
                    filename, name, qualname, flags, //
                    code, firstlineno, linetable, //
                    consts, names, //
                    localsplusnames, localspluskinds, //
                    argcount, posonlyargcount, kwonlyargcount,
                    stacksize, //
                    exceptiontable);

            return r.defineRef(v, idx);
        }
    }

    /**
     * Pseudo-{@link Codec} for records containing a reference, which
     * must previously have been defined (and not still be
     * {@code null}).
     */
    private static class RefCodec implements Codec {

        @Override
        public PyType type() {
            // It's not really a sensible question
            return PyBaseObject.TYPE;
        }

        @Override
        public void write(Writer w, Object v) {
            // XXX do we do this?
        }

        @Override
        public Map<Integer, Decoder> decoders() {
            return Map.of(TYPE_REF, (r, ref) -> read(r));
        }

        private static Object read(Reader r) {
            // The record makes reference to a cached object
            int idx = r.readInt();
            try {
                Object obj = r.refs.get(idx);
                if (obj == null) {
                    throw Reader.nullObject("object ref");
                }
                return obj;
            } catch (IndexOutOfBoundsException iobe) {
                throw Reader.badData("invalid reference");
            }
        }
    }
}
