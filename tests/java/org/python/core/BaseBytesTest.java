package org.python.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.python.core.buffer.SimpleBuffer;
import org.python.util.PythonInterpreter;

/**
 * Unit test of org.python.core.BaseBytes, a class that supplies much of the behaviour of the Jython
 * bytearray. In fact, it supplies almost all the immutable behaviour, and is abstract. In order to
 * test it, we need to define a concrete extension nested class MyBytes that is, almost, the Jython
 * 3.x bytes type.
 * <p>
 * Tests here are aimed at:
 * <ul>
 * <li>construction of a correct internal buffer through the init methods</li>.
 * <li>access methods for immutable types (such as {@link BaseBytes#getslice(int, int, int)}</li>.
 * <li>access methods for mutable types throw exceptions</li>.
 * <li>the java.util.List<PyInteger> interface</li>.
 * </ul>
 * From this list the test currently lacks testing deletion, testing the List API and memoryview.
 * <p>
 * The bulk of the functionality is tested in the Python regression tests (from CPythonLib. This
 * test class can be extended to test subclasses of BaseBytes, and all the tests defined here will
 * run for the subclass.
 */
public class BaseBytesTest extends TestCase {

    // Constants for array sizes
    public static final int SMALL = 7; // Less than minimum storage size
    public static final int MEDIUM = 25; // Medium array size
    public static final int LARGE = 2000; // Large enough for performance measurement
    public static final int HUGE = 100000; // Serious performance challenge

    /**
     * @param name
     */
    public BaseBytesTest(String name) {
        super(name);
    }

    /** Sometimes we need the interpreter to be initialised **/
    PythonInterpreter interp;

    /** State for random data fills */
    Random random;

    public static char toChar(int b) {
        return Character.toChars(0xff & b)[0];
    }

    /**
     * Turn a String into ints, but in the Python byte range, reducing character codes mod 256.
     *
     * @param s the string
     * @return
     */
    public static int[] toInts(String s) {
        int n = s.length();
        int[] r = new int[n];
        for (int i = 0; i < n; i++) {
            int c = s.codePointAt(i);
            r[i] = 0xff & c;
        }
        return r;
    }

    /**
     * Generate ints at random in the range 0..255.
     *
     * @param random the random generator
     * @param n length of array
     * @return the array of random values
     */
    public static int[] randomInts(Random random, int n) {
        int[] r = new int[n];
        for (int i = 0; i < n; i++) {
            r[i] = random.nextInt(256);
        }
        return r;
    }

    /**
     * Generate ints at random in a restricted range.
     *
     * @param random the random generator
     * @param n length of array
     * @param lo lowest value to generate
     * @param hi highest value to generate
     * @return the array of random values
     */
    public static int[] randomInts(Random random, int n, int lo, int hi) {
        int[] r = new int[n];
        int m = hi + 1 - lo;
        for (int i = 0; i < n; i++) {
            r[i] = lo + random.nextInt(m);
        }
        return r;
    }

    /**
     * Compare expected and result array sections at specified locations and length.
     *
     * @param expected reference values (may be null iff len==0)
     * @param first first value to compare in expected values
     * @param result bytearray from method under test
     * @param start first value to compare in result values
     * @param len number of values to compare
     */
    static void checkInts(int[] expected, int first, BaseBytes result, int start, int len) {
        if (len > 0) {
            int end = first + len;
            if (end > expected.length) {
                end = expected.length;
            }
            for (int i = first, j = start; i < end; i++, j++) {
                assertEquals("element value", expected[i], result.intAt(j));
            }
        }
    }

    /**
     * Compare expected and result array in their entirety.
     *
     * @param expected
     * @param result
     */
    static void checkInts(int[] expected, BaseBytes result) {
        // Size must be the same
        assertEquals("size", expected.length, result.size());
        // And each element
        for (int i = 0; i < expected.length; i++) {
            assertEquals("element value", expected[i], result.intAt(i));
        }
    }

    /**
     * Compare expected List<PyInteger> and result array in their entirety.
     *
     * @param expected
     * @param result
     */
    static void checkInts(List<PyInteger> expected, BaseBytes result) {
        // Size must be the same
        assertEquals("size", expected.size(), result.size());
        // And each element
        for (int i = 0; i < result.size; i++) {
            PyInteger res = result.pyget(i);
            PyInteger exp = expected.get(i);
            // System.out.printf("    expected[%2d]=%3d  b[%2d]=%3d\n",
            // i, exp.asInt(), i, res.asInt());
            assertEquals("element value", exp, res);
        }
    }

    /**
     * Compare expected List<PyInteger> and result object in their entirety.
     *
     * @param expected
     * @param result
     */
    static void checkInts(List<PyInteger> expected, PyObject result) {
        checkInts(expected, (BaseBytes)result);
    }

    /**
     * Turn array into Iterable<PyObject>, treating as unsigned (Python-style) bytes, and producing
     * an abusive mixture of object types.
     *
     * @return iterable list
     */
    public static Iterable<PyObject> iterableBytes(int[] source) {
        List<PyObject> list = new ArrayList<PyObject>(source.length);
        int choose = 0;
        for (int b : source) {
            switch (choose++) {
                case 0:
                    PyInteger i = new PyInteger(b);
                    list.add(i);
                    break;

                case 1:
                    PyLong l = new PyLong(b);
                    list.add(l);
                    break;

                default:
                    PyString s = new PyString(toChar(b));
                    list.add(s);
                    choose = 0;
                    break;
            }
        }
        return list;
    }

    /*
     * (non-Javadoc)
     *
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        random = new Random(20120310L);
    }

    /**
     * Test method for {@link org.python.core.BaseBytes#init(int)} via MyBytes constructor, and for
     * {@link org.python.core.BaseBytes#size()}.
     */
    public void testSize() {
        // Local constructor from byte[]
        int[] aRef = toInts("Chaque coquillage incrusté");
        BaseBytes a = getInstance(aRef);
        System.out.println(toString(a));
        assertEquals(aRef.length, a.size());

        // init(int) at various sizes
        for (int n : new int[] {0, 1, 2, 7, 8, 9, MEDIUM, LARGE, HUGE}) {
            a = getInstance(n);
            // System.out.println(toString(a));
            assertEquals("size()", n, a.size());
            assertEquals("__len__()", n, a.__len__());
        }
    }

    /**
     * Test method for {@link org.python.core.BaseBytes#init(BaseBytes)} via constructor on MyBytes.
     */
    public void testInit_intArray() {
        int[] aRef = toInts("Dans la grotte où nous nous aimâmes");
        BaseBytes a = getInstance(aRef);
        // Copy constructor b = bytes(a)
        BaseBytes b = getInstance(a);
        System.out.println(toString(b));
        assertEquals(a.size(), b.size());
        // assertEquals(a.storage, b.storage); // Supposed to share?
        // Check we got the same bytes
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.intAt(i), b.intAt(i));
        }
    }

    /**
     * Test method for {@link org.python.core.BaseBytes#init(BaseBytes)} via constructor on MyBytes.
     */
    public void testInit_Iterable() {
        int[] aRef = toInts("A sa particularité.");
        // Make an Iterable<? extends PyObject> of that
        Iterable<? extends PyObject> ia = iterableBytes(aRef);
        BaseBytes a = getInstance(ia);
        System.out.println(toString(a));
        assertEquals(aRef.length, a.size());
        checkInts(aRef, a);

        // Special cases: zero length
        BaseBytes b = getInstance(iterableBytes(new int[0]));
        // System.out.println(toString(b));
        assertEquals(0, b.size());

        // Special cases: very short (innards of init() take a short cut in this case)
        int[] cRef = toInts(":-)");
        BaseBytes c = getInstance(iterableBytes(cRef));
        // System.out.println(toString(c));
        assertEquals(cRef.length, c.size());
        checkInts(cRef, c);
    }

    /**
     * Test method for {@link org.python.core.BaseBytes#init(PyObject)} via constructor on MyBytes.
     */
    public void testInit_PyObject() {
        // A scary set of objects
        final PyObject[] brantub =
                {null, new PyInteger(5), new PyString("\u00A0\u00A1\u00A2\u00A3\u00A4"),
                        getInstance(new int[] {180, 190, 200}), new PyXRange(1, 301, 50)};
        // The array contents we should obtain
        final int[][] prize =
                { {}, {0, 0, 0, 0, 0}, {160, 161, 162, 163, 164}, {180, 190, 200},
                        {1, 51, 101, 151, 201, 251}};
        // Work down the lists
        for (int dip = 0; dip < brantub.length; dip++) {
            int[] aRef = prize[dip];
            BaseBytes a = getInstance(brantub[dip]);
            // System.out.println(toString(a));
            assertEquals(aRef.length, a.size());
            // Check we got the same bytes
            checkInts(aRef, a);
        }
    }

    /**
     * Test method for {@link org.python.core.BaseBytes#init(PyObject)} via constructor on MyBytes,
     * but where every example produced some kind of exception.
     */
    public void testInit_Exceptions() {
        // Need interpreter for exceptions to be formed properly
        interp = new PythonInterpreter();
        // A scary set of objects
        final PyObject[] brantub = {Py.None, new PyInteger(-1), //
                new PyLong(0x80000000L), //
                new PyXRange(3, -2, -1), //
                new PyXRange(250, 257) //
                };
        // The PyException types we should obtain
        final PyObject[] boobyPrize = {Py.TypeError, // None
                Py.ValueError, // -1
                Py.OverflowError, // 0x80000000L
                Py.ValueError, // -1 in iterable
                Py.ValueError // 256 in iterable
                };
        // Work down the lists
        for (int dip = 0; dip < brantub.length; dip++) {
            PyObject aRef = boobyPrize[dip];
            try {
                BaseBytes a = getInstance(brantub[dip]);
                System.out.println(toString(a));
                fail("Exception not thrown for " + brantub[dip]);
            } catch (PyException pye) {
                // System.out.println(pye);
                PyObject a = pye.type;
                assertEquals(aRef, a);
            }
        }
    }

    /**
     * Test method for {@link org.python.core.BaseBytes#pyget(int)}.
     */
    public void testPyget() {
        // Need interpreter
        interp = new PythonInterpreter();
        // Fill and access via pyget
        int[] aRef = randomInts(random, MEDIUM);
        BaseBytes a = getInstance(aRef);
        for (int i = 0; i < MEDIUM; i++) {
            PyInteger r = a.pyget(i);
            // System.out.printf("    aRef[%2d]=%3d  r=%3d\n", i, aRef[i], r.asInt());
            assertEquals(aRef[i], r.asInt());
        }
        // Check IndexError exceptions generated
        for (int i : new int[] {-1, -100, MEDIUM, MEDIUM + 1}) {
            try {
                PyInteger r = a.pyget(i);
                fail("Exception not thrown for pyget(" + i + ") =" + r);
            } catch (PyException pye) {
                assertEquals(Py.IndexError, pye.type);
                // System.out.printf("    Exception: %s\n", pye);
            }
        }
    }

    /**
     * Test method for {@link BaseBytes#getslice(int, int, int)}.
     *
     * @see PySequence#__getslice__(PyObject, PyObject)
     */
    public void testGetslice() {
        // getslice() deals with start, stop, step already 'interpreted' by SequenceIndexDelegate.
        String ver = "L'un a la pourpre de nos âmes";
        final int L = ver.length();
        int[] aRef = toInts(ver);
        BaseBytes a = getInstance(aRef);
        List<PyInteger> bList = new ArrayList<PyInteger>(L);

        final int[] posStart = new int[] {0, 1, 18, L - 8, L - 1};
        final int[] negStart = new int[] {0, 3, 16, L - 10, L - 1};

        // Positive step
        for (int step = 1; step < 4; step++) {
            for (int start : posStart) {
                // Use step positively
                for (int stop = start; stop <= L; stop++) {
                    // Make a reference answer by picking elements of aRef in slice pattern
                    bList.clear();
                    for (int i = start; i < stop; i += step) {
                        // System.out.printf("    (%d,%d,%d) i=%d\n", start, stop, step, i);
                        bList.add(new PyInteger(aRef[i]));
                    }
                    // Generate test result
                    // System.out.printf("    getslice(%d,%d,%d)\n", start, stop, step);
                    BaseBytes b = a.getslice(start, stop, step);
                    // System.out.println(toString(b));
                    // Now check size and contents
                    checkInts(bList, b);
                }
            }
        }

        // Negative step
        for (int step = -1; step > -4; step--) {
            for (int start : negStart) {
                // Use step positively
                for (int stop = -1; stop <= start; stop++) {
                    // Make a reference answer by picking elements of aRef in slice pattern
                    bList.clear();
                    for (int i = start; i > stop; i += step) {
                        // System.out.printf("    (%d,%d,%d) i=%d\n", start, stop, step, i);
                        bList.add(new PyInteger(aRef[i]));
                    }
                    // Generate test result
                    // System.out.printf("    getslice(%d,%d,%d)\n", start, stop, step);
                    BaseBytes b = a.getslice(start, stop, step);
                    // System.out.println(toString(b));
                    // Now check size and contents
                    checkInts(bList, b);
                }
            }
        }
    }

    /**
     * Test method for {@link org.python.core.BaseBytes#repeat(int)}.
     */
    public void testRepeatInt() {
        String spam = "Spam, "; // Could it be anything else?
        final int maxCount = 10;
        final int L = spam.length();
        int[] aRef = toInts(spam);
        BaseBytes a = getInstance(aRef);

        for (int count = 0; count <= maxCount; count++) {
            // Reference answer
            int[] bRef = new int[count * L];
            for (int i = 0; i < count; i++) {
                for (int j = 0; j < L; j++) {
                    bRef[i * L + j] = aRef[j];
                }
            }
            // Test
            BaseBytes b = a.repeat(count);
            // System.out.println(toString(b));
            checkInts(bRef, b);
        }
    }

    /**
     * Test method for {@link org.python.core.BaseBytes#pyset(int,PyObject)}, that it throws an
     * exception by default. Override in tests of mutable subclasses.
     */
    public void testPyset() {
        PyObject bRef = Py.TypeError;
        int[] aRef = toInts("This immutable type seems to allow modifications.");
        BaseBytes a = getInstance(aRef);
        int start = a.size() / 2;
        PyInteger x = new PyInteger('x');

        try {
            a.pyset(start, x);
            System.out.println(toString(a));
            fail(String.format("Exception not thrown for pyset(%d,%s)", start, x));
        } catch (PyException pye) {
            // System.out.println(pye);
            PyObject b = pye.type;
            assertEquals(bRef, b);
        }
    }

    /**
     * Test method for {@link org.python.core.BaseBytes#setslice(int,int,int,PyObject)}, that it
     * throws an exception by default. Override in tests of mutable subclasses.
     */
    public void testSetslice3() {
        PyObject bRef = Py.TypeError;
        int[] aRef = toInts("This immutable type seems to allow modifications.");
        BaseBytes a = getInstance(aRef);
        int start = a.size() / 4;
        int stop = (3 * a.size() + 3) / 4;
        int step = 3;
        BaseBytes x = new MyBytes(randomInts(random, SMALL));

        try {
            a.setslice(start, stop, step, x);
            System.out.println(toString(a));
            fail(String.format("Exception not thrown for setslice(%d,%d,%d,%s)", start, stop, step,
                    x));
        } catch (PyException pye) {
            // System.out.println(pye);
            PyObject b = pye.type;
            assertEquals(bRef, b);
        }
    }

    /*
     * Note that JUnit test classes extending this one inherit all the test* methods, and they will
     * be run by JUnit. Each test uses getInstance() methods where it might have used a constructor
     * with a similar signature. The idea is to override the getInstance() methods to return an
     * instance of the class actually under test in the derived test.
     */
    public BaseBytes getInstance(PyType type) {
        return new MyBytes(type);
    }

    public BaseBytes getInstance() {
        return new MyBytes();
    }

    public BaseBytes getInstance(int size) {
        return new MyBytes(size);
    }

    public BaseBytes getInstance(int[] value) {
        return new MyBytes(value);
    }

    public BaseBytes getInstance(BaseBytes value) throws PyException {
        return new MyBytes(value);
    }

    public BaseBytes getInstance(BufferProtocol value) throws PyException {
        return new MyBytes(value);
    }

    public BaseBytes getInstance(Iterable<? extends PyObject> value) throws PyException {
        return new MyBytes(value);
    }

    public BaseBytes getInstance(PyString arg, PyObject encoding, PyObject errors)
            throws PyException {
        return new MyBytes(arg, encoding, errors);
    }

    public BaseBytes getInstance(PyString arg, String encoding, String errors) throws PyException {
        return new MyBytes(arg, encoding, errors);
    }

    public BaseBytes getInstance(PyObject arg) throws PyException {
        return new MyBytes(arg);
    }

// protected BaseBytes getInstance(int start, int stop, BaseBytes source) {
// return new MyBytes(start, stop, source);
// }

    /**
     * Extension of class under test that makes the internal variables visible and adds constructors
     * like a derived class would.
     */
    public static class MyBytes extends BaseBytes {

        public static final PyType TYPE = PyType.fromClass(MyBytes.class);

        /**
         * Create a zero-length Python byte array of explicitly-specified sub-type
         *
         * @param type explicit Jython type
         */
        public MyBytes(PyType type) {
            super(type);
        }

        /**
         * Create a zero-length Python byte array of my type.
         */
        public MyBytes() {
            super(TYPE);
        }

        /**
         * Create zero-filled Python byte array of specified size.
         *
         * @param size of byte array
         */
        public MyBytes(int size) {
            super(TYPE, size);
        }

        /**
         * Create from integer array
         *
         * @param value
         */
        MyBytes(int[] value) {
            super(TYPE, value);
        }

        /**
         * Create a new array filled exactly by a copy of the contents of the source byte array.
         *
         * @param value of the bytes
         */
        public MyBytes(BaseBytes value) {
            super(TYPE);
            init(value);
        }

        /**
         * Create a new array filled exactly by a copy of the contents of the source.
         *
         * @param value source of the bytes (and size)
         */
        public MyBytes(BufferProtocol value) {
            super(TYPE);
            init((BufferProtocol)value.getBuffer(PyBUF.SIMPLE));
        }

        /**
         * Create a new array filled from an iterable of PyObject. The iterable must yield objects
         * convertible to Python bytes (non-negative integers less than 256 or strings of length 1).
         *
         * @param value of the bytes
         */
        public MyBytes(Iterable<? extends PyObject> value) {
            super(TYPE);
            init(value);
        }

        /**
         * Create a new array by encoding a PyString argument to bytes. If the PyString is actually
         * a PyUnicode, the encoding must be explicitly specified.
         *
         * @param arg primary argument from which value is taken
         * @param encoding name of optional encoding (must be a string type)
         * @param errors name of optional errors policy (must be a string type)
         */
        public MyBytes(PyString arg, PyObject encoding, PyObject errors) {
            super(TYPE);
            init(arg, encoding, errors);
        }

        /**
         * Create a new array by encoding a PyString argument to bytes. If the PyString is actually
         * a PyUnicode, the encoding must be explicitly specified.
         *
         * @param arg primary argument from which value is taken
         * @param encoding name of optional encoding (may be null to select the default for this
         *            installation)
         * @param errors name of optional errors policy
         */
        public MyBytes(PyString arg, String encoding, String errors) {
            super(TYPE);
            init(arg, encoding, errors);
        }

        /**
         * Create a new MyBytes object from an arbitrary Python object according to the same rules
         * as apply in Python to the bytes() constructor:
         * <ul>
         * <li>bytes() Construct a zero-length bytes (arg is null).</li>
         * <li>bytes(int) Construct a zero-initialized bytes of the given length.</li>
         * <li>bytes(iterable_of_ints) Construct from iterable yielding integers in [0..255]</li>
         * <li>bytes(string [, encoding [, errors] ]) Construct from a text string, optionally using
         * the specified encoding.</li>
         * <li>bytes(unicode, encoding [, errors]) Construct from a unicode string using the
         * specified encoding.</li>
         * <li>bytes(bytes_or_bytearray) Construct as a mutable copy of existing bytes or bytearray
         * object.</li>
         * </ul>
         * When it is necessary to specify an encoding, as in the Python signature
         * <code>bytes(string, encoding[, errors])</code>, use the constructor
         * {@link #MyBytes(PyString, String, String)}. If the PyString is actually a PyUnicode, an
         * encoding must be specified, and using this constructor will throw an exception about
         * that.
         *
         * @param arg primary argument from which value is taken (may be null)
         * @throws PyException in the same circumstances as bytes(arg), TypeError for non-iterable,
         *             non-integer argument type, and ValueError if iterables do not yield byte
         *             [0..255] values.
         */
        public MyBytes(PyObject arg) throws PyException {
            super(TYPE);
            init(arg);
        }

        /**
         * Constructor for local use that avoids copying the source data, providing a range-view
         * into it. The need for this arises (probably) during expressions that refer to a slice as
         * just one term. It is safe because MyBytes is immutable. But it may not be wise if the
         * source object is a large array from which only a small part needs to be retained in
         * memory.
         *
         * @param type explicit Jython type
         * @param start index of first byte to use in source
         * @param stop 1 + index of last byte to use in source
         * @param source of the bytes
         */
        protected MyBytes(int start, int stop, BaseBytes source) {
            super(TYPE);
            setStorage(source.storage, stop - start, start);
        }

        /**
         * Returns a PyByteArray that repeats this sequence the given number of times, as in the
         * implementation of <tt>__mul__</tt> for strings.
         *
         * @param count the number of times to repeat this.
         * @return this byte array repeated count times.
         */
        @Override
        protected MyBytes repeat(int count) {
            MyBytes ret = new MyBytes();
            ret.setStorage(repeatImpl(count));
            return ret;
        }

        /**
         * Returns a range of elements from the sequence.
         *
         * @see org.python.core.PySequence#getslice(int, int, int)
         */
        @Override
        protected MyBytes getslice(int start, int stop, int step) {
            MyBytes r;
            if (step == 1) {
                // This is a contiguous slice [start:stop] so we can share storage
                r = new MyBytes();
                if (stop > start) {
                    r.setStorage(storage, stop - start, start + offset);
                }
            } else {
                // This is an extended slice [start:stop:step] so we have to copy elements from it
                r = new MyBytes(sliceLength(start, stop, step));
                int iomax = r.size + r.offset;
                for (int io = r.offset, jo = start; io < iomax; jo += step, io++) {
                    r.storage[io] = storage[jo]; // Assign r[i] = this[j]
                }
            }
            return r;
        }

        /**
         * Return number of elements
         *
         * @see org.python.core.PyObject#__len__()
         */
        @Override
        public int __len__() {
            return size;
        }

        /**
         * Construct the MyBytes from a builder object.
         *
         * @param builder
         */
        protected MyBytes(Builder builder) {
            super(TYPE);
            setStorage(builder.getStorage(), builder.getSize());
        }

        /*
         * (non-Javadoc)
         *
         * @see org.python.core.BaseBytes#getBuilder(int)
         */
        @Override
        protected Builder getBuilder(int capacity) {
            // Return a Builder specialised for my class
            return new Builder(capacity) {

                @Override
                MyBytes getResult() {
                    // Create a MyBytes from the storage that the builder holds
                    return new MyBytes(this);
                }
            };
        }
    }

    /**
     * An object that for test purposes (of construction and slice assignment) contains an array of
     * values that it is able to offer for reading through the PyBuffer interface.
     */
    public static class BufferedObject extends PyObject implements BufferProtocol {

        public static final PyType TYPE = PyType.fromClass(BufferedObject.class);

        private byte[] store;

        /**
         * Store integer array as bytes: range must be 0..255 inclusive.
         *
         * @param value integers to store
         */
        BufferedObject(int[] value) {
            super(TYPE);
            int n = value.length;
            store = new byte[n];
            for (int i = 0; i < n; i++) {
                store[i] = (byte)value[i];
            }
        }

        @Override
        public PyBuffer getBuffer(int flags) {
            return new SimpleBuffer(flags, store);
        }

    }

    /**
     * Stringify in a form helpful when testing buffer manipulation. Show picture if not too long.
     */
    protected String toString(BaseBytes b) {
        Image i = new Image();
        i.showSummary(b);
        if (b.storage.length >= 0 && b.storage.length <= 70) {
            i.padTo(15);
            i.showContent(b);
        }
        return i.toString();
    }

    /**
     * Apparatus for producing a nice representation when studying buffer management.
     */
    protected static class Image {

        private StringBuilder image = new StringBuilder(100);

        private void repeat(char c, int n) {
            for (int i = 0; i < n; i++) {
                image.append(i == 0 ? '|' : ' ').append(c);
            }
        }

        // Show in image s[pos:pos+n] (as 2*n characters)
        private void append(byte[] s, int pos, int n) {
            if (pos < 0 || pos + n > s.length) {
                return;
            }
            for (int i = 0; i < n; i++) {
                int c = 0xff & ((int)s[pos + i]);
                if (c == 0) {
                    c = '.';
                } else if (Character.isISOControl(c)) {
                    c = '#';
                }
                image.append(i == 0 ? '|' : ' ').append(toChar(c));
            }
        }

        // Show an extent of n bytes (as 2*n characters)
        public void padTo(int n) {
            while (n > image.length()) {
                image.append(' ');
            }
        }

        /**
         * Write summary numbers offset [ size ] remainder
         *
         * @param b
         */
        public String showSummary(BaseBytes b) {
            image.append(b.offset);
            image.append(" [ ").append(b.size).append(" ] ");
            image.append(b.storage.length - (b.offset + b.size));
            return image.toString();
        }

        /**
         * Make text image of just the buffer boundaries.
         *
         * @param b
         */
        public String showExtent(BaseBytes b) {
            repeat('-', b.offset);
            repeat('x', b.size);
            int tail = b.storage.length - (b.offset + b.size);
            repeat('-', tail);
            image.append('|');
            return image.toString();
        }

        /**
         * Make text image of the buffer content and boundaries.
         *
         * @param b
         */
        public String showContent(BaseBytes b) {
            append(b.storage, 0, b.offset);
            append(b.storage, b.offset, b.size);
            int tail = b.storage.length - (b.offset + b.size);
            append(b.storage, b.offset + b.size, tail);
            image.append('|');
            return image.toString();
        }

        @Override
        public String toString() {
            return image.toString();
        }
    }

    /**
     * Example code equivalent to the code illustrating <code>suboffsets</code> in the C API at <a
     * href="http://docs.python.org/c-api/buffer.html#bufferobjects">The new-style Py_buffer
     * struct</a>. <code>buf</code> is an n-dimensional array of Object, implementing the storage of
     * some Python type, and it is required to access one element of it at an index defined by n
     * integers in sequence.
     *
     * @param n The number of dimensions the memory represents as a multi-dimensional array.
     * @param buf An n-dimensional array containing the value of the object
     * @param strides An array of length n giving the number of elements to skip to get to a new
     *            element in each dimension
     * @param suboffsets An array the length of n.
     * @param indices An array of n indices indexing the element to retrieve.
     * @return
     */
    private static Object
            getItem(int n, Object buf, int[] strides, int[] suboffsets, int[] indices) {
        for (int i = 0; i < n; i++) {
            Object[] p = (Object[])buf;
            buf = p[indices[i] * strides[i] + suboffsets[i]];
        }
        return buf;
    }

    /*
     * If it was an ndim-dimensional array of byte, we treat it as an (ndim-1)-dimensional array of
     * byte[] arrays. This method exemplifies getting just one byte.
     */
    private static byte
            getByte(int ndim, Object buf, int[] strides, int[] suboffsets, int[] indices) {
        int n = ndim - 1;
        byte[] b = (byte[])getItem(n, buf, strides, suboffsets, indices);
        return b[indices[n] + suboffsets[n]];
    }

}
