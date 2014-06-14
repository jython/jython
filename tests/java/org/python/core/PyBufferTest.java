package org.python.core;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.python.core.buffer.BaseBuffer;
import org.python.core.buffer.SimpleBuffer;
import org.python.core.buffer.SimpleStringBuffer;
import org.python.core.buffer.SimpleWritableBuffer;
import org.python.util.PythonInterpreter;

/**
 * Test the several implementations (and exporters) of the PyBuffer interface provided in the Jython
 * core.
 * <p>
 * The approach is to create test material once that has the necessary variety in byte array values,
 * then for each test, when the JUnit framework creates an instance of the function-specific test,
 * to use this material to create instances of each read-only type and each writable type. Writable
 * instance types go onto the lists buffersToRead and buffersToWrite, while read-only instances go
 * onto the lists buffersToRead and buffersToFailToWrite.
 * <p>
 * In general, tests of methods that read data apply themselves to all the elements of the
 * buffersToRead list, while tests of methods that write data apply themselves to all the elements
 * of the buffersToWrite list and check that members of the buffersToFailToWrite list raise an
 * exception.
 * <p>
 * The Jython buffer API follows the structures of the CPython buffer API so that it supports in
 * principle the use of multi-dimensional, strided and indirect array structures as buffers.
 * However, actual buffers in the Jython core, and therefore these tests, limit themselves to one
 * dimensional (possibly non-contiguous) directly-indexed buffers. Some tests apply directly to the
 * N-dimensional cases, and some need a complete re-think. Sub-classing this test would probably be
 * a good way to extend it to a wider range.
 */
public class PyBufferTest extends TestCase {

    /** Control amount of output. Instance variable so can be adjusted temporarily in test. */
    protected int verbosity = 0;

    /**
     * Generated constructor
     *
     * @param name
     */
    public PyBufferTest(String name) {
        super(name);
    }

    /** Sometimes we need the interpreter to be initialised **/
    PythonInterpreter interp;

    /*
     * Values for initialising the exporters.
     */
    private static final ByteMaterial byteMaterial = new ByteMaterial(0, 16, 17);
    private static final ByteMaterial abcMaterial = new ByteMaterial("abcdefgh");
    private static final ByteMaterial stringMaterial = new ByteMaterial("Mon côté fâcheux");
    private static final ByteMaterial emptyMaterial = new ByteMaterial(new byte[0]);
    public static final int LONG = 1000;
    private static final ByteMaterial longMaterial = new ByteMaterial(0, LONG, 5);

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Exception raising requires the Jython interpreter
        interp = new PythonInterpreter();

        // Tests using local types of exporter
        genWritable(new SimpleWritableExporter(abcMaterial.getBytes()), abcMaterial);
        genReadonly(new SimpleExporter(byteMaterial.getBytes()), byteMaterial);
        genReadonly(new StringExporter(stringMaterial.string), stringMaterial);
        genWritable(new SimpleWritableExporter(emptyMaterial.getBytes()), emptyMaterial);

        // Tests with PyByteArray
        genWritable(new PyByteArray(abcMaterial.getBytes()), abcMaterial);
        genWritable(new PyByteArray(longMaterial.getBytes()), longMaterial);
        genWritable(new PyByteArray(), emptyMaterial);

        // Tests with PyString
        genReadonly(new PyString(abcMaterial.string), abcMaterial);
        genReadonly(new PyString(), emptyMaterial);

        // Ensure case is tested where PyByteArray has an internal offset
        PyByteArray truncated = new PyByteArray(stringMaterial.getBytes());
        truncated.delRange(0, 4);
        ByteMaterial truncatedMaterial = new ByteMaterial(stringMaterial.string.substring(4));
        assert truncated.__alloc__() > truncatedMaterial.length;
        genWritable(truncated, truncatedMaterial);
    }

    /** Generate a series of test material for a writable object. */
    private void genWritable(BufferProtocol exporter, ByteMaterial material) {
        generate(exporter, material, false);
    }

    /** Generate a series of test material for a read-only object. */
    private void genReadonly(BufferProtocol exporter, ByteMaterial material) {
        generate(exporter, material, true);
    }

    /** Lengths we will use if we can when slicing view */
    private static final int[] sliceLengths = {1, 2, 5, 0, LONG / 4};

    /** Step sizes we will use if we can when slicing view */
    private static final int[] sliceSteps = {1, 2, 3, 7};

    /**
     * Generate a series of test material for a read-only or writable object. Given one exporter,
     * and its reference ByteMaterial this method first queues a BufferTestPair corresponding to the
     * exporter as the test subject and its test material. This provides a "direct" PyBuffer view on
     * the exporter. It then goes on to make a variety of sliced PyBuffer views of the exporter by
     * calling {@link PyBuffer#getBufferSlice(int, int, int, int)} on the direct view. The slices
     * are made with a variety of argument combinations, filtered down to those that make sense for
     * the size of the direct view. Each sliced buffer (considered a test subject now), together
     * with correspondingly sliced reference ByteMaterial is queued as BufferTestPair.
     *
     * @param exporter underlying object
     * @param material reference material corresponding to the exporter
     * @param readonly whether the exporter is of read-only type
     */
    private void generate(BufferProtocol exporter, ByteMaterial material, boolean readonly) {

        // Generate a test using the buffer directly exported by the exporter
        PyBuffer direct = queue(exporter, material, readonly);

        // Generate some slices from the material and this direct view
        int N = material.length;
        int M = (N + 4) / 4;    // At least one and about N/4

        // For a range of start positions up to one beyond the end
        for (int start = 0; start <= N; start += M) {
            // For a range of lengths
            for (int length : sliceLengths) {

                if (length == 0) {
                    queue(direct, material, start, 0, 1, readonly);
                    queue(direct, material, start, 0, 2, readonly);

                } else if (length == 1 && start < N) {
                    queue(direct, material, start, 1, 1, readonly);
                    queue(direct, material, start, 1, 2, readonly);

                } else if (start < N) {

                    // And for a range of step sizes
                    for (int step : sliceSteps) {
                        // Check this is a feasible slice
                        if (start + (length - 1) * step < N) {
                            queue(direct, material, start, length, step, readonly);
                        }
                    }

                    // Now use all the step sizes negatively
                    for (int step : sliceSteps) {
                        // Check this is a feasible slice
                        if (start - (length - 1) * step >= 0) {
                            queue(direct, material, start, length, -step, readonly);
                        }
                    }
                }
            }
        }
    }

    /** Generate and queue one test of non-slice type (if getting a buffer succeeds). */
    private PyBuffer queue(BufferProtocol exporter, ByteMaterial material, boolean readonly) {
        if (verbosity > 2) {
            System.out.printf("queue non-slice: length=%d, readonly=%s\n", material.length,
                    readonly);
        }
        BufferTestPair pair = new BufferTestPair(exporter, material, readonly);
        queue(pair);
        return pair.view;
    }

    /** Generate and queue one test of slice type (if getting a buffer succeeds). */
    private PyBuffer queue(PyBuffer direct, ByteMaterial material, int start, int length, int step,
            boolean readonly) {

        int flags = readonly ? PyBUF.FULL_RO : PyBUF.FULL;
        PyBuffer subject = null;

        /*
         * Make a slice. We ignore this case if we fail, because we are not testing slice creation
         * here, but making slices to be tested as buffers. We'll test slice creation in
         * testGetBufferSlice.
         */
        try {
            if (verbosity > 2) {
                System.out.printf("  queue slice: start=%4d, length=%4d, step=%4d\n", start,
                        length, step);
            }
            subject = direct.getBufferSlice(flags, start, length, step);
            ByteMaterial sliceMaterial = material.slice(start, length, step);
            BufferTestPair pair = new BufferTestPair(subject, sliceMaterial, step, readonly);
            queue(pair);
        } catch (Exception e) {
            /*
             * We ignore this case if we fail, because we are not testing slice creation here, but
             * making slices to be tested as buffers. We'll test slice creation elsewhere.
             */
            if (verbosity > 2) {
                System.out.printf("*** SKIP %s\n", e);
            }
        }

        return subject;
    }

    /** Queue one instance of test material for a read-only or writable object. */
    private void queue(BufferTestPair pair) {
        buffersToRead.add(pair);
        if (pair.readonly) {
            buffersToFailToWrite.add(pair);
        } else {
            buffersToWrite.add(pair);
        }
    }

    /** Read operations should succeed on all these objects. */
    private List<BufferTestPair> buffersToRead = new LinkedList<BufferTestPair>();
    /** Write operations should succeed on all these objects. */
    private List<BufferTestPair> buffersToWrite = new LinkedList<BufferTestPair>();
    /** Write operations should fail on all these objects. */
    private List<BufferTestPair> buffersToFailToWrite = new LinkedList<BufferTestPair>();

    /**
     * A one-dimensional exporter should be able to give us a buffer for all these flag types.
     */
    private static final int[] simpleFlags = {PyBUF.SIMPLE, PyBUF.ND, PyBUF.STRIDES,
            PyBUF.INDIRECT, PyBUF.FULL_RO};

    /** To {@link #simpleFlags} we can add any of these */
    private static final int[] simpleTassles = {0, PyBUF.FORMAT, PyBUF.C_CONTIGUOUS,
            PyBUF.F_CONTIGUOUS, PyBUF.ANY_CONTIGUOUS};

    /**
     * A one-dimensional exporter with stride!=1 is restricted to give us a buffer only for these
     * flag types.
     */
    private static final int[] strided1DFlags = {PyBUF.STRIDES, PyBUF.INDIRECT, PyBUF.FULL_RO};

    /** To {@link #strided1DFlags} we can add any of these */
    private static final int[] strided1DTassles = {0, PyBUF.FORMAT};

    /**
     * Test method for {@link org.python.core.PyBUF#isReadonly()}.
     */
    public void testIsReadonly() {

        for (BufferTestPair test : buffersToWrite) {
            if (verbosity > 0) {
                System.out.println("isReadonly: " + test);
            }
            assertFalse(test.view.isReadonly());
        }

        for (BufferTestPair test : buffersToFailToWrite) {
            if (verbosity > 0) {
                System.out.println("isReadonly: " + test);
            }
            assertTrue(test.view.isReadonly());
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#getNdim()}.
     */
    public void testGetNdim() {
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("getNdim: " + test);
            }
            assertEquals("unexpected ndim", test.shape.length, test.view.getNdim());
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#getShape()}.
     */
    public void testGetShape() {
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("getShape: " + test);
            }
            int[] shape = test.view.getShape();
            assertNotNull("shape[] should always be provided", shape);
            assertIntsEqual("unexpected shape", test.shape, shape);
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#getLen()}.
     */
    public void testGetLen() {
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("getLen: " + test);
            }
            assertEquals("unexpected length", test.material.length, test.view.getLen());
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#byteAt(int)}.
     */
    public void testByteAt() {
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("byteAt: " + test);
            }
            int n = test.material.length;
            byte[] exp = test.material.bytes;
            for (int i = 0; i < n; i++) {
                assertEquals(exp[i], test.view.byteAt(i));
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#byteAt(int[])}.
     */
    public void testByteAtNdim() {
        int[] index = new int[1];
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("byteAt(array): " + test);
            }
            if (test.view.getShape().length != 1) {
                fail("Test not implemented if dimensions != 1");
            }
            byte[] exp = test.material.bytes;
            int n = test.material.length;
            // Run through 1D index for view
            for (int i = 0; i < n; i++) {
                index[0] = i;
                assertEquals(exp[i], test.view.byteAt(index));
            }

            // Check 2D index throws
            try {
                test.view.byteAt(0, 0);
                fail("Use of 2D index did not raise exception");
            } catch (PyException pye) {
                // Expect BufferError
                assertEquals(Py.BufferError, pye.type);
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#intAt(int)}.
     */
    public void testIntAt() {
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("intAt: " + test);
            }
            int n = test.material.length;
            int[] exp = test.material.ints;
            for (int i = 0; i < n; i++) {
                assertEquals(exp[i], test.view.intAt(i));
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#intAt(int[])}.
     */
    public void testIntAtNdim() {
        int[] index = new int[1];
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("intAt(array): " + test);
            }
            if (test.view.getShape().length != 1) {
                fail("Test not implemented for dimensions != 1");
            }
            int[] exp = test.material.ints;
            int n = test.material.length;
            // Run through 1D index for view
            for (int i = 0; i < n; i++) {
                index[0] = i;
                assertEquals(exp[i], test.view.intAt(index));
            }
            // Check 2D index throws
            try {
                test.view.intAt(0, 0);
                fail("Use of 2D index did not raise exception");
            } catch (PyException pye) {
                // Expect BufferError
                assertEquals(Py.BufferError, pye.type);
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#storeAt(byte, int)}.
     */
    public void testStoreAt() {
        for (BufferTestPair test : buffersToWrite) {
            if (verbosity > 0) {
                System.out.println("storeAt: " + test);
            }
            int n = test.material.length;
            int[] exp = test.material.ints;
            // Write modified test material into each location using storeAt()
            for (int i = 0; i < n; i++) {
                byte v = (byte)(exp[i] ^ 3);    // twiddle some bits
                test.view.storeAt(v, i);
            }
            // Compare each location with modified test data using intAt()
            for (int i = 0; i < n; i++) {
                assertEquals(exp[i] ^ 3, test.view.intAt(i));
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#storeAt(byte, int[])}.
     */
    public void testStoreAtNdim() {
        for (BufferTestPair test : buffersToWrite) {
            if (verbosity > 0) {
                System.out.println("storeAt: " + test);
            }
            int n = test.material.length;
            int[] exp = test.material.ints;
            // Write modified test material into each location using storeAt()
            for (int i = 0; i < n; i++) {
                byte v = (byte)(exp[i] ^ 3);    // twiddle some bits
                test.view.storeAt(v, i);
            }
            // Compare each location with modified test data using intAt()
            for (int i = 0; i < n; i++) {
                assertEquals(exp[i] ^ 3, test.view.intAt(i));
            }
            // Check 2D index throws
            try {
                test.view.storeAt((byte)1, 0, 0);
                fail("Use of 2D index did not raise exception");
            } catch (PyException pye) {
                // Expect BufferError
                assertEquals(Py.BufferError, pye.type);
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#copyTo(byte[], int)}.
     */
    public void testCopyTo() {
        final int OFFSET = 5;
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("copyTo: " + test);
            }
            int n = test.material.length;
            // Try with zero offset
            byte[] actual = new byte[n];
            test.view.copyTo(actual, 0);
            assertBytesEqual("copyTo() incorrect", test.material.bytes, actual, 0);
            // Try to middle of array
            actual = new byte[n + 2 * OFFSET];
            test.view.copyTo(actual, OFFSET);
            assertBytesEqual("copyTo(offset) incorrect", test.material.bytes, actual, OFFSET);
            assertEquals("data before destination", 0, actual[OFFSET - 1]);
            assertEquals("data after destination", 0, actual[OFFSET + n]);
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#copyTo(int, byte[], int, int)}.
     */
    public void testSliceCopyTo() {
        final int OFFSET = 5;
        final byte BLANK = 7;

        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("copyTo(from slice): " + test);
            }
            PyBuffer view = test.view;

            int n = test.material.length;
            byte[] actual = new byte[n + 2 * OFFSET];

            // Try destination positions in actual[] of 0 and OFFSET
            for (int destPos = 0; destPos <= OFFSET; destPos += OFFSET) {
                // Try source positions in 0 and OFFSET
                for (int srcIndex = 0; srcIndex <= OFFSET; srcIndex += OFFSET) {

                    // A variety of lengths from zero to (n-srcIndex)-ish
                    for (int length = 0; srcIndex + length <= n; length = 2 * length + 1) {

                        if (verbosity > 1) {
                            System.out.printf("  copy src[%d:%d] (%d) to dst[%d:%d] (%d)\n",
                                    srcIndex, srcIndex + length, n, destPos, destPos + length,
                                    actual.length);
                        }

                        Arrays.fill(actual, BLANK);

                        // Test the method
                        view.copyTo(srcIndex, actual, destPos, length);

                        // Check changed part of destination
                        assertBytesEqual("copyTo(slice) incorrect", test.material.bytes, srcIndex,
                                length, actual, destPos);
                        if (destPos > 0) {
                            assertEquals("data before destination", BLANK, actual[destPos - 1]);
                        }
                        assertEquals("data after destination", BLANK, actual[destPos + length]);
                    }

                    // And from exactly n-srcIndex down to zero-ish
                    for (int trim = 0; srcIndex + trim <= n; trim = 2 * trim + 1) {
                        int length = n - srcIndex - trim;

                        if (verbosity > 1) {
                            System.out.printf("  copy src[%d:%d] (%d) to dst[%d:%d] (%d)\n",
                                    srcIndex, srcIndex + length, n, destPos, destPos + length,
                                    actual.length);
                        }

                        Arrays.fill(actual, BLANK);

                        // Test the method
                        view.copyTo(srcIndex, actual, destPos, length);

                        // Check changed part of destination
                        assertBytesEqual("copyTo(slice) incorrect", test.material.bytes, srcIndex,
                                length, actual, destPos);
                        if (destPos > 0) {
                            assertEquals("data before destination", BLANK, actual[destPos - 1]);
                        }
                        assertEquals("data after destination", BLANK, actual[destPos + length]);
                    }
                }
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#copyFrom(byte[], int, int, int)}.
     */
    public void testCopyFrom() {
        final int OFFSET = 5;
        final byte BLANK = 7;

        for (BufferTestPair test : buffersToWrite) {
            if (verbosity > 0) {
                System.out.println("copyFrom(): " + test);
            }
            PyBuffer view = test.view;

            int n = test.material.length;
            byte[] actual = new byte[n];
            byte[] expected = new byte[n];

            // Make some source material for copies (need to test at OFFSET too).
            byte[] src = new byte[n + OFFSET];
            for (int i = 0; i < src.length; i++) {
                src[i] = (byte)i;
            }

            // Try destination positions in test object of 0 and OFFSET
            for (int destIndex = 0; destIndex <= OFFSET; destIndex += OFFSET) {

                // Try source positions in 0 and OFFSET
                for (int srcPos = 0; srcPos <= OFFSET; srcPos += OFFSET) {

                    // A variety of lengths from zero to (n-destIndex)-ish
                    for (int length = 0; destIndex + length <= n; length = 2 * length + 1) {

                        if (verbosity > 1) {
                            System.out.printf("  copy src[%d:%d] (%d) to dst[%d:%d] (%d)\n",
                                    srcPos, srcPos + length, n, destIndex, destIndex + length,
                                    actual.length);
                        }

                        // Initialise the object (have to do each time) and expected value
                        for (int i = 0; i < n; i++) {
                            expected[i] = BLANK;
                            view.storeAt(BLANK, i);
                        }

                        // Test the method and extract the result to actual[]
                        view.copyFrom(src, srcPos, destIndex, length);
                        view.copyTo(actual, 0);

                        // Complete what is should be in expected[]
                        for (int i = 0; i < length; i++) {
                            expected[destIndex + i] = src[srcPos + i];
                        }
                        assertBytesEqual("copyFrom() incorrect", expected, actual, 0);
                    }

                    // And from exactly n-destIndex down to zero-ish
                    for (int trim = 0; destIndex + trim <= n; trim = 2 * trim + 1) {
                        int length = n - destIndex - trim;

                        if (verbosity > 1) {
                            System.out.printf("  copy src[%d:%d] (%d) to dst[%d:%d] (%d)\n",
                                    srcPos, srcPos + length, n, destIndex, destIndex + length,
                                    actual.length);
                        }

                        // Initialise the object (have to do each time) and expected value
                        for (int i = 0; i < n; i++) {
                            expected[i] = BLANK;
                            view.storeAt(BLANK, i);
                        }

                        // Test the method and extract the result to actual[]
                        view.copyFrom(src, srcPos, destIndex, length);
                        view.copyTo(actual, 0);

                        // Complete what is should be in expected[]
                        for (int i = 0; i < length; i++) {
                            expected[destIndex + i] = src[srcPos + i];
                        }
                        assertBytesEqual("copyFrom() incorrect", expected, actual, 0);
                    }
                }
            }
        }
    }

    /**
     * Test method for {@link org.python.core.BufferProtocol#getBuffer()} and
     * {@link org.python.core.PyBuffer#getBuffer()}.
     */
    public void testGetBuffer() {

        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("getBuffer(): " + test);
            }
            for (int flags : test.validFlags) {
                for (int tassle : test.validTassles) {
                    PyBuffer view = test.subject.getBuffer(flags | tassle);
                    assertNotNull(view);
                }
            }
        }

        for (BufferTestPair test : buffersToWrite) {
            if (verbosity > 0) {
                System.out.println("getBuffer(WRITABLE): " + test);
            }
            for (int flags : test.validFlags) {
                for (int tassle : test.validTassles) {
                    PyBuffer view = test.subject.getBuffer(flags | tassle | PyBUF.WRITABLE);
                    assertNotNull(view);
                }
            }
        }

        for (BufferTestPair test : buffersToFailToWrite) {
            if (verbosity > 0) {
                System.out.println("getBuffer(WRITABLE): " + test);
            }
            for (int flags : test.validFlags) {
                try {
                    test.subject.getBuffer(flags | PyBUF.WRITABLE);
                    fail("Write access not prevented: " + test);
                } catch (PyException pye) {
                    // Expect BufferError
                    assertEquals(Py.BufferError, pye.type);
                }
            }
        }

    }

    /**
     * Test method for {@link org.python.core.PyBUF#release()}, exercising the release semantics of
     * PyBuffer.
     */
    public void testRelease() {

        /*
         * Testing the semantics of release() is tricky when it comes to 'final' release behaviour.
         * We'd like to test that buffers can be acquired and released, that "over release" is
         * detected as an error, and that after final release of the buffer (where the export count
         * becomes zero) an exporter remains capable of exporting again. Each test is constructed
         * with a subject and a view on the subject (if the subject is an exporter), so you might
         * think the export count would be one in every case. Two problems: in many tests, the
         * subject is a PyBuffer, which has the option (if it would work) to return itself; and a
         * PyBuffer is not expected to provide a new buffer view once finally released.
         */

        Set<PyBuffer> uniqueBuffers = new HashSet<PyBuffer>();

        // Test a balanced sequence of acquire and release using try-with-resources
        for (BufferTestPair test : buffersToRead) {
            doTestTryWithResources(test);
        }

        // Now test a pattern of acquire and release with one more release than acquire
        for (BufferTestPair test : buffersToRead) {
            doTestRelease(test);
            uniqueBuffers.add(test.view);
        }

        // All buffers are released: test that any further release is detected as an error.
        for (PyBuffer view : uniqueBuffers) {
            doTestOverRelease(view);
        }

        // All exporters are currently not exporting buffers
        for (BufferTestPair test : buffersToRead) {
            if (!(test.subject instanceof PyBuffer)) {
                doTestGetAfterRelease(test);
            }
        }

    }

    /**
     * Exercise try-with-resources on one BufferTestPair.
     */
    private void doTestTryWithResources(BufferTestPair test) {

        if (verbosity > 0) {
            System.out.println("try with resources: " + test);
        }
        int flags = PyBUF.STRIDES | PyBUF.FORMAT;
        BufferProtocol sub = test.subject;

        // The object will be exporting test.view and N other views we don't know about
        try (PyBuffer c = sub.getBuffer(flags)) {   // = N+1 exports
            try (PyBuffer b = sub.getBuffer(PyBUF.FULL_RO); PyBuffer d =c.getBuffer(flags)) {
                checkExporting(sub);// = N+3 exports
            }
            checkExporting(sub);                    // = N+1 exports
        }
        checkExporting(sub);                        // = N export
    }

    /**
     * Exercise the release semantics of one BufferTestPair. At the end, the view in the
     * BufferTestPair should be fully released, ({@link PyBuffer#isReleased()}<code>==true</code>).
     */
    private void doTestRelease(BufferTestPair test) {

        if (verbosity > 0) {
            System.out.println("release: " + test);
        }
        int flags = PyBUF.STRIDES | PyBUF.FORMAT;
        BufferProtocol sub = test.subject;

        // The object will be exporting test.view and N other views we don't know about
        PyBuffer a = test.view;                     // = N+1 exports
        PyBuffer b = sub.getBuffer(PyBUF.FULL_RO);  // = N+2 export
        PyBuffer c = sub.getBuffer(flags);          // = N+3 exports
        checkExporting(sub);

        // Now see that releasing in some other order works correctly
        b.release();                                // = N+2 exports
        a.release();                                // = N+1 exports
        checkExporting(sub);

        // You can get a buffer from a buffer (c is unreleased)
        PyBuffer d = c.getBuffer(flags);            // = N+2 exports
        c.release();                                // = N+1 export
        checkExporting(sub);
        d.release();                                // = N exports
    }

    /**
     * The view argument should be a fully released buffer, ({@link PyBuffer#isReleased()}
     * <code>==true</code>). We check that further releases raise an error.
     */
    private void doTestOverRelease(PyBuffer view) {

        // Was it released finally?
        assertTrue("Buffer not finally released as expected", view.isReleased());

        // Further releases are an error
        try {
            view.release();                        // = -1 exports (oops)
            fail("excess release not detected");
        } catch (Exception e) {
            // Success
        }

    }

    /**
     * The test in the argument is one where the subject is a real object (not another buffer) from
     * which all buffer views should have been released in {@link #doTestRelease(BufferTestPair)}.
     * We check this is true, and that a new buffer may still be acquired from the real object, but
     * not from the released buffer.
     */
    private void doTestGetAfterRelease(BufferTestPair test) {

        if (verbosity > 0) {
            System.out.println("get again: " + test);
        }
        BufferProtocol sub = test.subject;

        // Fail here if doTestRelease did not fully release, or
        checkNotExporting(sub);

        // Further gets via the released buffer are an error
        try {
            test.view.getBuffer(PyBUF.FULL_RO);
            fail("PyBuffer.getBuffer after final release not detected");
        } catch (Exception e) {
            // Detected *and* prevented?
            checkNotExporting(sub);
        }

        // And so are sliced gets
        try {
            test.view.getBufferSlice(PyBUF.FULL_RO, 0, 0);
            fail("PyBuffer.getBufferSlice after final release not detected");
        } catch (Exception e) {
            // Detected *and* prevented?
            checkNotExporting(sub);
        }

        /*
         * Even after some abuse, we can still get and release a buffer.
         */
        PyBuffer b = sub.getBuffer(PyBUF.FULL_RO);      // = 1 export
        checkExporting(sub);
        b.release();                                    // = 0 exports
        checkNotExporting(sub);
    }

    /**
     * Error if subject is a PyBuffer and is released, or is a real exporter that (we can tell) is
     * not actually exporting.
     *
     * @param subject
     */
    private void checkExporting(BufferProtocol subject) {
        if (subject instanceof TestableExporter) {
            assertTrue("exports not being counted", ((TestableExporter)subject).isExporting());
        } else if (subject instanceof PyBuffer) {
            assertFalse("exports not being counted (PyBuffer)", ((PyBuffer)subject).isReleased());
        } else if (subject instanceof PyByteArray) {
            // Size-changing access should fail
            try {
                ((PyByteArray)subject).bytearray_extend(Py.One); // Appends one zero byte
                fail("bytearray_extend with exports should fail");
            } catch (Exception e) {
                // Success
            }
        }
        // Other types cannot be checked
    }

    /**
     * Error if subject is a PyBuffer that is released, or is a real exporter (that we can tell) is
     * locked.
     *
     * @param subject
     */
    private void checkNotExporting(BufferProtocol subject) {
        if (subject instanceof TestableExporter) {
            assertFalse("exports counted incorrectly", ((TestableExporter)subject).isExporting());
        } else if (subject instanceof PyBuffer) {
            assertTrue("exports counted incorrectly (PyBuffer)", ((PyBuffer)subject).isReleased());
        } else if (subject instanceof PyByteArray) {
            // Size-changing access should succeed
            try {
                PyByteArray sub = ((PyByteArray)subject);
                sub.bytearray_append(Py.Zero);
                sub.del(sub.__len__() - 1);
            } catch (Exception e) {
                fail("bytearray unexpectedly locked");
            }
        }
        // Other types cannot be checked
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#getBufferSlice(int, int, int, int)}.
     */
    public void testGetBufferSliceWithStride() {

        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("getBufferSliceWithStride: " + test);
            }
            ByteMaterial material = test.material;
            PyBuffer view = test.view;
            boolean readonly = test.readonly;

            // Generate some slices from the material and the test view
            int N = material.length;
            int M = (N + 4) / 4;    // At least one and about N/4

            // For a range of start positions up to one beyond the end
            for (int start = 0; start <= N; start += M) {
                // For a range of lengths
                for (int length : sliceLengths) {

                    if (length == 0) {
                        checkSlice(view, material, start, 0, 1, readonly);
                        checkSlice(view, material, start, 0, 2, readonly);

                    } else if (length == 1 && start < N) {
                        checkSlice(view, material, start, 1, 1, readonly);
                        checkSlice(view, material, start, 1, 2, readonly);

                    } else if (start < N) {

                        // And for a range of step sizes
                        for (int step : sliceSteps) {
                            // Check this is a feasible slice
                            if (start + (length - 1) * step < N) {
                                checkSlice(view, material, start, length, step, readonly);
                            }
                        }

                        // Now use all the step sizes negatively
                        for (int step : sliceSteps) {
                            // Check this is a feasible slice
                            if (start - (length - 1) * step >= 0) {
                                checkSlice(view, material, start, length, -step, readonly);
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * Helper for {@link #testGetBufferSliceWithStride()} that obtains one sliced buffer to
     * specification and checks it against the material.
     */
    private void checkSlice(PyBuffer view, ByteMaterial material, int start, int length, int step,
            boolean readonly) {

        int flags = readonly ? PyBUF.FULL_RO : PyBUF.FULL;

        if (verbosity > 1) {
            System.out.printf("  checkSlice: start=%4d, length=%4d, step=%4d \n", start, length,
                    step);
        }
        byte[] expected = sliceBytes(material.bytes, start, length, step);
        PyBuffer sliceView = view.getBufferSlice(flags, start, length, step);

        byte[] result = bytesFromByteAt(sliceView);
        assertBytesEqual("  testGetBufferSliceWithStride failure: ", expected, result);
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#getBuf()}.
     */
    public void testGetBuf() {
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("getBuf: " + test);
            }
            int stride = test.strides[0];

            if (stride == 1) {

                // The client should not have to support navigation with the strides array
                int flags = test.readonly ? PyBUF.SIMPLE : PyBUF.SIMPLE + PyBUF.WRITABLE;
                PyBuffer view = test.subject.getBuffer(flags);

                PyBuffer.Pointer bp = view.getBuf();
                assertBytesEqual("buffer does not match reference", test.material.bytes, bp);

            } else {
                // The client will have to navigate with the strides array
                int flags = test.readonly ? PyBUF.STRIDED_RO : PyBUF.STRIDED;
                PyBuffer view = test.subject.getBuffer(flags);

                stride = view.getStrides()[0];  // Just possibly != test.strides when length<=1
                PyBuffer.Pointer bp = view.getBuf();
                assertBytesEqual("buffer does not match reference", test.material.bytes, bp, stride);
            }

        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#getPointer(int)}.
     */
    public void testGetPointer() {
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("getPointer: " + test);
            }
            PyBuffer view = test.view;
            int n = test.material.length, itemsize = view.getItemsize();
            byte[] exp = new byte[itemsize], bytes = test.material.bytes;

            for (int i = 0; i < n; i++) {
                // Expected result is one item (allow for itemsize)
                int p = i * itemsize;
                for (int j = 0; j < itemsize; j++) {
                    exp[j] = bytes[p + j];
                }

                // Get pointer and check contents for correct data
                PyBuffer.Pointer bp = view.getPointer(i);
                assertBytesEqual("getPointer value", exp, bp);
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#getPointer(int[])}.
     */
    public void testGetPointerNdim() {
        int[] index = new int[1];
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("getPointer(array): " + test);
            }
            PyBuffer view = test.view;
            int n = test.material.length, itemsize = view.getItemsize();
            byte[] exp = new byte[itemsize], bytes = test.material.bytes;

            for (int i = 0; i < n; i++) {
                // Expected result is one item (allow for itemsize)
                int p = i * itemsize;
                for (int j = 0; j < itemsize; j++) {
                    exp[j] = bytes[p + j];
                }

                // Get pointer and check contents for data matching exp
                index[0] = i;
                PyBuffer.Pointer bp = view.getPointer(index);
                assertBytesEqual("getPointer value", exp, bp);
            }

            // Check 2D index throws
            try {
                view.getPointer(0, 0);
                fail("Use of 2D index did not raise exception");
            } catch (PyException pye) {
                // Expect BufferError
                assertEquals(Py.BufferError, pye.type);
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#getStrides()}.
     */
    public void testGetStrides() {
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("getStrides: " + test);
            }
            for (int flags : test.validFlags) {
                PyBuffer view = test.subject.getBuffer(flags);
                // Strides array irrespective of the client flags ... (different from CPython)
                int[] strides = view.getStrides();
                assertNotNull("strides[] should always be provided", strides);

                // The strides must have the expected value if length >1
                if (test.material.bytes.length > 1) {
                    assertIntsEqual("unexpected strides", test.strides, strides);
                }
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#getSuboffsets()}.
     */
    public void testGetSuboffsets() {
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("getSuboffsets: " + test);
            }
            // Null for all test material
            assertNull(test.view.getSuboffsets());
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#isContiguous(char)}.
     */
    public void testIsContiguous() {
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("isContiguous: " + test);
            }
            // True for all test material and orders (since 1-dimensional)
            for (String orderMsg : validOrders) {
                char order = orderMsg.charAt(0);
                assertTrue(orderMsg, test.view.isContiguous(order));
            }
        }
    }

    private static final String[] validOrders = {"C-contiguous test fail",
            "F-contiguous test fail", "Any-contiguous test fail"};

    /**
     * Test method for {@link org.python.core.PyBuffer#getFormat()}.
     */
    public void testGetFormat() {
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("getFormat: " + test);
            }
            for (int flags : test.validFlags) {
                PyBuffer view = test.subject.getBuffer(flags);
                // Format given irrespective of the client flags ... (different from CPython)
                assertNotNull("format should always be provided", view.getFormat());
                assertEquals("B", view.getFormat());
                // And, we can ask for it explicitly ...
                view = test.subject.getBuffer(flags | PyBUF.FORMAT);
                assertEquals("B", view.getFormat());
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#getItemsize()}.
     */
    public void testGetItemsize() {
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("getItemsize: " + test);
            }
            // Unity for all test material
            assertEquals(1, test.view.getItemsize());
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#toString()}.
     */
    public void testToString() {
        for (BufferTestPair test : buffersToRead) {
            if (verbosity > 0) {
                System.out.println("toString: " + test);
            }
            String r = test.view.toString();
            assertEquals("buffer does not match reference", test.material.string, r);
        }
    }

    /*
     * ------------------------------------------------------------------------------------------- A
     * series of custom exporters to permit testing abstracted from the Jython interpreter. These
     * use the implementation classes in org.python.core.buffer in ways very similar to the
     * implementations of bytearray and str.
     * -------------------------------------------------------------------------------------------
     */
    /**
     * A class to act as an exporter that uses the SimpleReadonlyBuffer. The exporter exports a new
     * PyBuffer object to each consumer (although each references the same internal storage) and it
     * does not track their fate. You are most likely to use this approach with an exporting object
     * that is immutable (or at least fixed in size).
     */
    static class SimpleExporter implements BufferProtocol {

        protected byte[] storage;

        /**
         * Construct a simple read only exporter from the bytes supplied.
         *
         * @param storage
         */
        public SimpleExporter(byte[] storage) {
            this.storage = storage;
        }

        @Override
        public PyBuffer getBuffer(int flags) {
            return new SimpleBuffer(flags, storage);
        }

    }

    /**
     * Base class of certain exporters that permit testing abstracted from the Jython interpreter.
     */
    static abstract class TestableExporter implements BufferProtocol {

        protected Reference<BaseBuffer> export;

        /**
         * Try to re-use existing exported buffer, or return null if can't.
         */
        protected BaseBuffer getExistingBuffer(int flags) {
            BaseBuffer pybuf = null;
            if (export != null) {
                // A buffer was exported at some time.
                pybuf = export.get();
                if (pybuf != null) {
                    // And this buffer still exists: expect this to provide a further reference
                    pybuf = pybuf.getBufferAgain(flags);
                }
            }
            return pybuf;
        }

        /**
         * Determine whether this object is exporting a buffer: modelled after
         * {@link PyByteArray#resizeCheck()}.
         *
         * @return true iff exporting
         */
        public boolean isExporting() {
            if (export != null) {
                // A buffer was exported at some time.
                PyBuffer pybuf = export.get();
                if (pybuf != null) {
                    return !pybuf.isReleased();
                } else {
                    // In fact the reference has expired: go quicker next time.
                    export = null;
                }
            }
            return false;
        }

    }

    /**
     * A class to act as an exporter that uses the SimpleStringBuffer. The exporter shares a single
     * exported buffer between all consumers but does not need to take any action when that buffer
     * is finally released. You are most likely to use this approach with an exporting object type
     * that does not modify its behaviour while there are active exports, but where it is worth
     * avoiding the cost of duplicate buffers. This is the case with PyString, where some buffer
     * operations cause construction of a byte array copy of the Java String, which it is desirable
     * to do only once.
     */
    static class StringExporter extends TestableExporter {

        String storage;

        /**
         * Construct a simple exporter from the String supplied.
         *
         * @param s
         */
        public StringExporter(String s) {
            storage = s;
        }

        @Override
        public PyBuffer getBuffer(int flags) {
            // If we have already exported a buffer it may still be available for re-use
            BaseBuffer pybuf = getExistingBuffer(flags);
            if (pybuf == null) {
                // No existing export we can re-use
                pybuf = new SimpleStringBuffer(flags, storage);
                // Hold a reference for possible re-use
                export = new SoftReference<BaseBuffer>(pybuf);
            }
            return pybuf;
        }

    }

    /**
     * A class to act as an exporter that uses the SimpleBuffer. The exporter shares a single
     * exported buffer between all consumers and needs to take any action immediately when that
     * buffer is finally released. You are most likely to use this approach with an exporting object
     * type that modifies its behaviour while there are active exports, but where it is worth
     * avoiding the cost of duplicate buffers. This is the case with PyByteArray, which prohibits
     * operations that would resize it, while there are outstanding exports.
     */
    static class SimpleWritableExporter extends TestableExporter {

        protected byte[] storage;

        /**
         * Construct a simple exporter from the bytes supplied.
         *
         * @param storage
         */
        public SimpleWritableExporter(byte[] storage) {
            this.storage = storage;
        }

        @Override
        public PyBuffer getBuffer(int flags) {
            // If we have already exported a buffer it may still be available for re-use
            BaseBuffer pybuf = getExistingBuffer(flags);
            if (pybuf == null) {
                // No existing export we can re-use
                pybuf = new SimpleWritableBuffer(flags, storage) {

                    @Override
                    protected void releaseAction() {
                        export = null;  // Final release really is final (not reusable)
                    }
                };

                // Hold a reference for possible re-use
                export = new WeakReference<BaseBuffer>(pybuf);
            }
            return pybuf;
        }

    }

    /**
     * Class to hold test material representing the same sequence of values 0..255 in several
     * different ways.
     */
    protected static class ByteMaterial {

        final String string;
        final byte[] bytes;
        final int[] ints;
        final int length;

        /** Construct from String. */
        public ByteMaterial(String s) {
            string = s;
            length = s.length();
            bytes = new byte[length];
            ints = new int[length];
            for (int i = 0; i < length; i++) {
                int x = s.charAt(i);
                ints[i] = x;
                bytes[i] = (byte)x;
            }
        }

        /** Construct from byte array. */
        public ByteMaterial(byte[] b) {
            length = b.length;
            StringBuilder buf = new StringBuilder(length);
            bytes = new byte[length];
            ints = new int[length];
            for (int i = 0; i < length; i++) {
                int x = 0xff & b[i];
                ints[i] = x;
                bytes[i] = (byte)x;
                buf.appendCodePoint(x);
            }
            string = buf.toString();
        }

        /** Construct from int array. */
        public ByteMaterial(int[] a) {
            length = a.length;
            StringBuilder buf = new StringBuilder(length);
            bytes = new byte[length];
            ints = new int[length];
            for (int i = 0; i < length; i++) {
                int x = a[i];
                ints[i] = x;
                bytes[i] = (byte)x;
                buf.appendCodePoint(x);
            }
            string = buf.toString();
        }

        /** Construct from pattern on values (used modulo 256). */
        public ByteMaterial(int start, int count, int inc) {
            length = count;
            StringBuilder buf = new StringBuilder(length);
            bytes = new byte[length];
            ints = new int[length];
            int x = start;
            for (int i = 0; i < length; i++) {
                ints[i] = x;
                bytes[i] = (byte)x;
                buf.appendCodePoint(x);
                x = (x + inc) & 0xff;
            }
            string = buf.toString();
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(100);
            buf.append("byte[").append(length).append("]={ ");
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                if (i >= 5) {
                    buf.append(" ...");
                    break;
                } else {
                    buf.append(ints[i]);
                }
            }
            buf.append(" }");
            return buf.toString();
        }

        /**
         * @return a copy of the bytes array (that the client is allowed to modify)
         */
        byte[] getBytes() {
            return bytes.clone();
        }

        /**
         * Create material equivalent to a slice. this will not be used to create an exporter, but
         * rather to specify data equivalent to the export.
         *
         * @param start first index to include
         * @param length number of indices
         * @param stride between indices
         * @return ByteMaterial in which the arrays are a slice of this one
         */
        ByteMaterial slice(int start, int length, int stride) {
            return new ByteMaterial(sliceBytes(bytes, start, length, stride));
        }
    }

    /**
     * Create a byte array from the values of the PyBuffer obtained using
     * {@link PyBuffer#byteAt(int)}, to a length obtained from {@link PyBuffer#getLen()}.
     *
     * @param v the buffer
     * @return the byte array
     */
    static byte[] bytesFromByteAt(PyBuffer v) {
        final int N = v.getLen();
        byte[] a = new byte[N];
        for (int i = 0; i < N; i++) {
            a[i] = v.byteAt(i);
        }
        return a;
    }

    /**
     * Create a byte array that is a strided copy of the one passed in. The specifications are
     * assumed correct for the size of that array.
     *
     * @param b source array
     * @param start first index to include
     * @param length number of indices
     * @param stride between indices
     * @return slice of b
     */
    static byte[] sliceBytes(byte[] b, int start, int length, int stride) {
        byte[] a = new byte[length];
        for (int i = 0, j = start; i < length; i++, j += stride) {
            a[i] = b[j];
        }
        return a;
    }

    /**
     * Customised assert method comparing a buffer pointer to a byte array, usually the one from
     * ByteMaterial.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param bp result to test
     */
    static void assertBytesEqual(String message, byte[] expected, PyBuffer.Pointer bp) {
        assertBytesEqual(message, expected, bp, 1);
    }

    /**
     * Customised assert method comparing a buffer pointer to a byte array, usually the one from
     * ByteMaterial.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param bp result to test
     * @param stride in the storage array
     */
    static void assertBytesEqual(String message, byte[] expected, PyBuffer.Pointer bp, int stride) {
        assertBytesEqual(message, expected, 0, expected.length, bp.storage, bp.offset, stride);
    }

    /**
     * Customised assert method comparing a buffer pointer to a byte array, usually the one from
     * ByteMaterial.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param expectedStart where to start the comparison in expected
     * @param n number of bytes to test
     * @param bb result to test
     * @param stride in the storage array
     */
    static void assertBytesEqual(String message, byte[] expected, int expectedStart, int n,
            PyBuffer.Pointer bp, int stride) {
        assertBytesEqual(message, expected, expectedStart, n, bp.storage, bp.offset, stride);
    }

    /**
     * Customised assert method comparing a byte arrays: values in the actual value must match all
     * those in expected[], and they must be the same length.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param actual result to test
     */
    static void assertBytesEqual(String message, byte[] expected, byte[] actual) {
        assertEquals(message + " (array size)", expected.length, actual.length);
        assertBytesEqual(message, expected, 0, expected.length, actual, 0, 1);
    }

    /**
     * Customised assert method comparing byte arrays: values in the actual value starting at
     * actual[actualStart] must match all those in expected[], and there must be enough of them.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param actual result to test
     * @param actualStart where to start the comparison in actual
     */
    static void assertBytesEqual(String message, byte[] expected, byte[] actual, int actualStart) {
        assertBytesEqual(message, expected, 0, expected.length, actual, actualStart, 1);
    }

    /**
     * Customised assert method comparing byte arrays: values starting at actual[actualStart] must
     * those starting at expected[expectedStart], for a distance of n bytes.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param expectedStart where to start the comparison in expected
     * @param n number of bytes to test
     * @param actual result to test
     * @param actualStart where to start the comparison in actual
     */
    static void assertBytesEqual(String message, byte[] expected, int expectedStart, int n,
            byte[] actual, int actualStart) {
        assertBytesEqual(message, expected, expectedStart, n, actual, actualStart, 1);
    }

    /**
     * Customised assert method comparing byte arrays: values starting at actual[actualStart] must
     * those starting at expected[expectedStart], for a distance of n bytes.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param expectedStart where to start the comparison in expected
     * @param n number of bytes to test
     * @param actual result to test
     * @param actualStart where to start the comparison in actual
     * @param stride spacing of bytes in actual array
     */
    static void assertBytesEqual(String message, byte[] expected, int expectedStart, int n,
            byte[] actual, int actualStart, int stride) {

        if (actualStart < 0) {
            fail(message + " (start<0 in result)");

        } else if (expectedStart < 0) {
            fail(message + " (start<0 in expected result): bug in test?");

        } else if (actualStart + (n - 1) * stride + 1 > actual.length) {
            fail(message + " (result too short)");

        } else if (expectedStart + n > expected.length) {
            fail(message + " (expected result too short): bug in test?");

        } else {
            // Should be safe to compare the values
            int i = actualStart, j, jLimit = expectedStart + n;
            for (j = expectedStart; j < jLimit; j++) {
                if (actual[i] != expected[j]) {
                    break;
                }
                i += stride;
            }

            // If we stopped early, diagnose the problem
            if (j < jLimit) {
                System.out.println("  expected:"
                        + Arrays.toString(Arrays.copyOfRange(expected, expectedStart, expectedStart
                                + n)));
                System.out
                        .println("    actual:"
                                + Arrays.toString(Arrays.copyOfRange(actual, actualStart,
                                        actualStart + n)));
                System.out.println("  _actual_:" + Arrays.toString(actual));
                fail(message + " (byte at " + j + ")");
            }
        }
    }

    /**
     * Customised assert method comparing a int arrays: values in the actual value starting at
     * actual[offset] must match all those in expected[], and there must be enough of them.
     *
     * @param message to issue on failure
     * @param expected expected array
     * @param actual result to test
     * @param offset where to start the comparison in actual
     */
    static void assertIntsEqual(String message, int[] expected, int[] actual, int offset) {
        int n = expected.length;
        if (offset < 0) {
            fail(message + " (offset<0)");
        } else if (offset + n > actual.length) {
            fail(message + " (too short)");
        } else {
            // Should be safe to compare the values
            int i = offset, j;
            for (j = 0; j < n; j++) {
                if (actual[i++] != expected[j]) {
                    break;
                }
            }
            if (j < n) {
                System.out.println("  expected:" + Arrays.toString(expected));
                System.out.println("    actual:" + Arrays.toString(actual));
                fail(message + " (int at " + j + ")");
            }
        }
    }

    /**
     * Customised assert method comparing a int arrays: int in the actual value must match all those
     * in expected[], and there must be the same number of them.
     *
     * @param message to issue on failure
     * @param expected expected array
     * @param actual result to test
     */
    static void assertIntsEqual(String message, int[] expected, int[] actual) {
        int n = expected.length;
        assertEquals(message, n, actual.length);
        // Should be safe to compare the values
        int j;
        for (j = 0; j < n; j++) {
            if (actual[j] != expected[j]) {
                break;
            }
        }
        if (j < n) {
            System.out.println("  expected:" + Arrays.toString(expected));
            System.out.println("    actual:" + Arrays.toString(actual));
            fail(message + " (int at " + j + ")");
        }
    }

    /**
     * Within a given test case (e.g. the test of one particular method) we run many data sets, and
     * these are created by {@link PyBufferTest#setUp()} as instances of this class. The main
     * contents of the BufferTestPair are the test subject and the material. The subject may be one
     * of the base objects specified in <code>setUp()</code>, or it may itself be a
     * <code>PyBuffer</code> onto one of these (often a sliced buffer). The material contains an
     * array of bytes (and equivalent int array and String) that is the array of bytes equivalent to
     * the subject.
     */
    private static class BufferTestPair {

        /**
         * An object (or PyBuffer) that is the subject of the test
         */
        final BufferProtocol subject;

        /**
         * Test material (a byte array and its value as several different types) that has a value
         * equivalent to the subject of the test.
         */
        final ByteMaterial material;

        /**
         * As a convenience for the simple tests (which is most of them!) this element is guaranteed
         * to be a PyBuffer: if {@link #subject} is a {@link PyBuffer}, this member is simply
         * another reference to the <code>subject</code>. If <code>subject</code> is a real
         * exporter, {@link #view} is a new view on the subject.
         */
        final PyBuffer view;

        /** The base exporter is of a type that can only provide read-only views. */
        final boolean readonly;

        /**
         * Flags that may be used in {@link BufferProtocol#getBuffer(int)} or
         * {@link PyBuffer#getBufferSlice(int, int, int, int)}.
         */
        final int[] validFlags;

        /**
         * Modifier flags that may be used in {@link BufferProtocol#getBuffer(int)} or
         * {@link PyBuffer#getBufferSlice(int, int, int, int)}.
         */
        final int[] validTassles;

        static final int[] STRIDES_1D = {1};

        /** The shape array that the subject should match (will be single element in present tests) */
        int[] shape;

        /** The shape array that the subject should match (will be single element in present tests) */
        int[] strides;

        /**
         * A subject and its reference material, together with explicit shape and strides arrays
         * expected.
         *
         * @param subject of the test
         * @param material containing a Java byte array that a view of the subject should equal
         * @param shape of the array, when testing in N-dimensions
         * @param strides of the array, when testing sliced views
         * @param readonly if true the base exporter can only provide read-only views
         */
        public BufferTestPair(BufferProtocol subject, ByteMaterial material, int[] shape,
                int[] strides, boolean readonly, int[] validFlags, int[] validTassles) {
            this.subject = subject;
            this.material = new ByteMaterial(material.ints);    // Copy in case modified
            this.shape = shape;
            this.strides = strides;
            this.readonly = readonly;
            this.validFlags = validFlags;
            this.validTassles = validTassles;

            int flags = readonly ? PyBUF.FULL_RO : PyBUF.FULL;

            if (subject instanceof PyBuffer) {
                this.view = (PyBuffer)subject;
            } else {
                PyBuffer v = null;
                try {
                    // System.out.printf("BufferTestPair: length=%d, readonly=%s\n",
                    // material.length, readonly);
                    v = subject.getBuffer(flags);
                } catch (Exception e) {
                    /*
                     * We ignore this case if we fail, because we are not testing buffer creation
                     * here, but making buffers to be tested. We'll test buffer creation in
                     * testGetBuffer.
                     */
                }
                this.view = v;
            }
        }

        /**
         * Short constructor for contiguous arrays in one dimension.
         *
         * @param subject of the test
         * @param material containing a Java byte array that a view of the subject should equal
         * @param readonly if true the base exporter can only provide read-only views
         */
        public BufferTestPair(BufferProtocol subject, ByteMaterial material, boolean readonly) {
            this(subject, material, new int[1], STRIDES_1D, readonly, simpleFlags, simpleTassles);
            shape[0] = material.length;
        }

        /**
         * Short constructor for strided arrays in one dimension.
         *
         * @param subject of the test
         * @param material containing a Java byte array that a view of the subject should equal
         * @param stride of the array, when testing sliced views
         * @param readonly if true the base exporter can only provide read-only views
         */
        public BufferTestPair(PyBuffer subject, ByteMaterial material, int stride, boolean readonly) {
            this(subject, material, new int[1], new int[1], readonly, strided1DFlags,
                    strided1DTassles);
            shape[0] = material.length;
            strides[0] = stride;
        }

        @Override
        public String toString() {
            int offset = view.getBuf().offset;
            String offsetSpec = offset > 0 ? "[0@(" + offset + "):" : "[:";
            int stride = strides[0];
            String sliceSpec = offsetSpec + shape[0] + (stride != 1 ? "*(" + stride + ")]" : "]");
            return subject.getClass().getSimpleName() + sliceSpec + " ( " + material.toString()
                    + " )";
        }

    }
}
