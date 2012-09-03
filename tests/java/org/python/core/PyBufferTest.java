package org.python.core;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

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
 * principle the use of multi-dimensional, strided add indirect array structures as buffers.
 * However, actual buffers in the Jython core, and therefore these tests, limit themselves to one
 * dimensional contiguous buffers with a simple organisation. Some tests apply directly to the
 * N-dimensional cases, and some need a complete re-think. Sub-classing this test would probably be
 * a good way to extend it to a wider range.
 */
public class PyBufferTest extends TestCase {

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
    private static final ByteMaterial byteMaterial = new ByteMaterial(0, 17, 16);
    private static final ByteMaterial abcMaterial = new ByteMaterial("abcdef");
    private static final ByteMaterial stringMaterial = new ByteMaterial("Mon côté fâcheux");
    private static final ByteMaterial emptyMaterial = new ByteMaterial(new byte[0]);
    private static final ByteMaterial longMaterial = new ByteMaterial(0, 5, 1000);

    protected void setUp() throws Exception {
        super.setUp();

        // Exception raising requires the Jython interpreter
        interp = new PythonInterpreter();

        // Tests using local examples
        queueWrite(new SimpleWritableExporter(abcMaterial.getBytes()), abcMaterial);
        queueReadonly(new SimpleExporter(byteMaterial.getBytes()), byteMaterial);
        queueReadonly(new StringExporter(stringMaterial.string), stringMaterial);
        queueWrite(new SimpleWritableExporter(emptyMaterial.getBytes()), emptyMaterial);

        // Tests with PyByteArray
        queueWrite(new PyByteArray(abcMaterial.getBytes()), abcMaterial);
        queueWrite(new PyByteArray(longMaterial.getBytes()), longMaterial);
        queueWrite(new PyByteArray(), emptyMaterial);

        // Tests with PyString
        queueReadonly(new PyString(abcMaterial.string), abcMaterial);
        queueReadonly(new PyString(), emptyMaterial);

        // Ensure case is tested where PyByteArray has an internal offset
        PyByteArray truncated = new PyByteArray(stringMaterial.getBytes());
        truncated.delRange(0, 4);
        ByteMaterial truncatedMaterial = new ByteMaterial(stringMaterial.string.substring(4));
        assert truncated.__alloc__() > truncatedMaterial.length;
        queueWrite(truncated, truncatedMaterial);
    }

    private void queueWrite(BufferProtocol exporter, ByteMaterial material) {
        BufferTestPair pair = new BufferTestPair(exporter, material);
        buffersToRead.add(pair);
        buffersToWrite.add(pair);
    }

    private void queueReadonly(BufferProtocol exporter, ByteMaterial material) {
        BufferTestPair pair = new BufferTestPair(exporter, material);
        buffersToRead.add(pair);
        buffersToFailToWrite.add(pair);
    }

    /** Read operations should succeed on all these objects. */
    private List<BufferTestPair> buffersToRead = new LinkedList<BufferTestPair>();
    /** Write operations should succeed on all these objects. */
    private List<BufferTestPair> buffersToWrite = new LinkedList<BufferTestPair>();
    /** Write operations should fail on all these objects. */
    private List<BufferTestPair> buffersToFailToWrite = new LinkedList<BufferTestPair>();

    /** We should be able to get a buffer for all these flag types. */
    private int[] validFlags = {PyBUF.SIMPLE, PyBUF.ND, PyBUF.STRIDES, PyBUF.INDIRECT};

    /** To which we can add any of these (in one dimension, anyway) */
    private int[] validTassles = {0,
                                  PyBUF.FORMAT,
                                  PyBUF.C_CONTIGUOUS,
                                  PyBUF.F_CONTIGUOUS,
                                  PyBUF.ANY_CONTIGUOUS};

    /**
     * Test method for {@link org.python.core.BufferProtocol#getBuffer()}.
     */
    public void testExporterGetBuffer() {

        for (BufferTestPair test : buffersToRead) {
            System.out.println("getBuffer(): " + test);
            for (int flags : validFlags) {
                for (int tassle : validTassles) {
                    PyBuffer view = test.exporter.getBuffer(flags | tassle);
                    assertNotNull(view);
                }
            }
        }

        for (BufferTestPair test : buffersToWrite) {
            System.out.println("getBuffer(WRITABLE): " + test);
            for (int flags : validFlags) {
                for (int tassle : validTassles) {
                    PyBuffer view = test.exporter.getBuffer(flags | tassle | PyBUF.WRITABLE);
                    assertNotNull(view);
                }
            }
        }

        for (BufferTestPair test : buffersToFailToWrite) {
            System.out.println("getBuffer(WRITABLE): " + test);
            for (int flags : validFlags) {
                try {
                    test.exporter.getBuffer(flags | PyBUF.WRITABLE);
                    fail("Write access not prevented: " + test);
                } catch (PyException pye) {
                    // Expect BufferError
                    assertEquals(Py.BufferError, pye.type);
                }
            }
        }

    }

    /**
     * Test method for {@link org.python.core.PyBUF#isReadonly()}.
     */
    public void testIsReadonly() {

        for (BufferTestPair test : buffersToWrite) {
            System.out.println("isReadonly: " + test);
            assertFalse(test.simple.isReadonly());
        }

        for (BufferTestPair test : buffersToFailToWrite) {
            System.out.println("isReadonly: " + test);
            assertTrue(test.simple.isReadonly());
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#getNdim()}.
     */
    public void testGetNdim() {
        for (BufferTestPair test : buffersToRead) {
            System.out.println("getNdim: " + test);
            assertEquals("simple ndim", test.shape.length, test.simple.getNdim());
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#getShape()}.
     */
    public void testGetShape() {
        for (BufferTestPair test : buffersToRead) {
            System.out.println("getShape: " + test);
            int[] shape = test.simple.getShape();
            assertNotNull(shape);
            assertIntsEqual("simple shape", test.shape, shape);
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#getLen()}.
     */
    public void testGetLen() {
        for (BufferTestPair test : buffersToRead) {
            System.out.println("getLen: " + test);
            assertEquals(" simple len", test.material.bytes.length, test.simple.getLen());
            assertEquals("strided len", test.material.bytes.length, test.strided.getLen());
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#byteAt(int)}.
     */
    public void testByteAt() {
        for (BufferTestPair test : buffersToRead) {
            System.out.println("byteAt: " + test);
            int n = test.material.length;
            byte[] exp = test.material.bytes;
            for (int i = 0; i < n; i++) {
                assertEquals(exp[i], test.simple.byteAt(i));
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#byteAt(int[])}.
     */
    public void testByteAtNdim() {
        int[] index = new int[1];
        for (BufferTestPair test : buffersToRead) {
            System.out.println("byteAt(array): " + test);
            if (test.strided.getShape().length != 1) {
                fail("Test not implemented dimensions != 1");
            }
            byte[] exp = test.material.bytes;
            int n = test.material.length;
            // Run through 1D index for simple
            for (int i = 0; i < n; i++) {
                index[0] = i;
                assertEquals(exp[i], test.simple.byteAt(index));
            }
            // Check 2D index throws
            try {
                test.simple.byteAt(0, 0);
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
            System.out.println("intAt: " + test);
            int n = test.material.length;
            int[] exp = test.material.ints;
            for (int i = 0; i < n; i++) {
                assertEquals(exp[i], test.simple.intAt(i));
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#intAt(int[])}.
     */
    public void testIntAtNdim() {
        int[] index = new int[1];
        for (BufferTestPair test : buffersToRead) {
            System.out.println("intAt(array): " + test);
            if (test.strided.getShape().length != 1) {
                fail("Test not implemented dimensions != 1");
            }
            int[] exp = test.material.ints;
            int n = test.material.length;
            // Run through 1D index for simple
            for (int i = 0; i < n; i++) {
                index[0] = i;
                assertEquals(exp[i], test.simple.intAt(index));
            }
            // Check 2D index throws
            try {
                test.simple.intAt(0, 0);
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
            System.out.println("storeAt: " + test);
            int n = test.material.length;
            int[] exp = test.material.ints;
            // Write modified test material into each location using storeAt()
            for (int i = 0; i < n; i++) {
                byte v = (byte)(exp[i] ^ 3);    // twiddle some bits
                test.simple.storeAt(v, i);
            }
            // Compare each location with modified test data using intAt()
            for (int i = 0; i < n; i++) {
                assertEquals(exp[i] ^ 3, test.simple.intAt(i));
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#storeAt(byte, int[])}.
     */
    public void testStoreAtNdim() {
        for (BufferTestPair test : buffersToWrite) {
            System.out.println("storeAt: " + test);
            int n = test.material.length;
            int[] exp = test.material.ints;
            // Write modified test material into each location using storeAt()
            for (int i = 0; i < n; i++) {
                byte v = (byte)(exp[i] ^ 3);    // twiddle some bits
                test.simple.storeAt(v, i);
            }
            // Compare each location with modified test data using intAt()
            for (int i = 0; i < n; i++) {
                assertEquals(exp[i] ^ 3, test.simple.intAt(i));
            }
            // Check 2D index throws
            try {
                test.simple.storeAt((byte)1, 0, 0);
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
            System.out.println("copyTo: " + test);
            int n = test.material.length;
            // Try with zero offset
            byte[] actual = new byte[n];
            test.simple.copyTo(actual, 0);
            assertBytesEqual("copyTo() incorrect", test.material.bytes, actual, 0);
            // Try to middle of array
            actual = new byte[n + 2 * OFFSET];
            test.simple.copyTo(actual, OFFSET);
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
            System.out.println("copyTo(from slice): " + test);
            PyBuffer view = test.simple;

            int n = test.material.length;
            byte[] actual = new byte[n + 2 * OFFSET];

            // Try destination positions in actual[] of 0 and OFFSET
            for (int destPos = 0; destPos <= OFFSET; destPos += OFFSET) {
                // Try source positions in 0 and OFFSET
                for (int srcIndex = 0; srcIndex <= OFFSET; srcIndex += OFFSET) {

                    // A variety of lengths from zero to (n-srcIndex)-ish
                    for (int length = 0; srcIndex + length <= n; length = 2 * length + 1) {
                        /*
                         * System.out.printf("  copy src[%d:%d] (%d) to dst[%d:%d] (%d)\n",
                         * srcIndex, srcIndex + length, n, destPos, destPos + length,
                         * actual.length);
                         */
                        Arrays.fill(actual, BLANK);

                        // Test the method
                        view.copyTo(srcIndex, actual, destPos, length);

                        // Check changed part of destination
                        assertBytesEqual("copyTo(slice) incorrect", test.material.bytes, srcIndex,
                                         actual, destPos, length);
                        if (destPos > 0) {
                            assertEquals("data before destination", BLANK, actual[destPos - 1]);
                        }
                        assertEquals("data after destination", BLANK, actual[destPos + length]);
                    }

                    // And from exactly n-srcIndex down to zero-ish
                    for (int trim = 0; srcIndex + trim <= n; trim = 2 * trim + 1) {
                        int length = n - srcIndex - trim;
                        /*
                         * System.out.printf("  copy src[%d:%d] (%d) to dst[%d:%d] (%d)\n",
                         * srcIndex, srcIndex + length, n, destPos, destPos + length,
                         * actual.length);
                         */
                        Arrays.fill(actual, BLANK);

                        // Test the method
                        view.copyTo(srcIndex, actual, destPos, length);

                        // Check changed part of destination
                        assertBytesEqual("copyTo(slice) incorrect", test.material.bytes, srcIndex,
                                         actual, destPos, length);
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
            System.out.println("copyFrom(): " + test);
            PyBuffer view = test.simple;

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

                        // System.out.printf("  copy src[%d:%d] (%d) to dst[%d:%d] (%d)\n", srcPos,
                        // srcPos + length, n, destIndex, destIndex + length,
                        // actual.length);

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

                        // System.out.printf("  copy src[%d:%d] (%d) to dst[%d:%d] (%d)\n", srcPos,
                        // srcPos + length, n, destIndex, destIndex + length,
                        // actual.length);

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
     * Test method for {@link org.python.core.PyBuffer#getBuf()}.
     */
    public void testGetBuf() {
        for (BufferTestPair test : buffersToRead) {
            System.out.println("getBuf: " + test);
            PyBuffer view = test.exporter.getBuffer(PyBUF.SIMPLE);
            ByteMaterial m = test.material;

            BufferPointer bp = view.getBuf();
            assertBytesEqual("getBuf: ", m.bytes, bp);
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#getPointer(int)}.
     */
    public void testGetPointer() {
        for (BufferTestPair test : buffersToRead) {
            System.out.println("getPointer: " + test);
            PyBuffer view = test.strided;
            int n = test.material.length, itemsize = view.getItemsize();
            byte[] exp = new byte[itemsize], bytes = test.material.bytes;

            for (int i = 0; i < n; i++) {
                // Expected result is one item (allow for itemsize)
                int p = i * itemsize;
                for (int j = 0; j < itemsize; j++) {
                    exp[j] = bytes[p + j];
                }

                // Get pointer and check contents for correct data
                BufferPointer bp = view.getPointer(i);
                assertBytesEqual("getPointer value", exp, bp.storage, bp.offset);
                assertEquals("getPointer size wrong", itemsize, bp.size);
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBuffer#getPointer(int[])}.
     */
    public void testGetPointerNdim() {
        int[] index = new int[1];
        for (BufferTestPair test : buffersToRead) {
            System.out.println("getPointer(array): " + test);
            PyBuffer view = test.strided;
            int n = test.material.length, itemsize = view.getItemsize();
            byte[] exp = new byte[itemsize], bytes = test.material.bytes;

            for (int i = 0; i < n; i++) {
                // Expected result is one item (allow for itemsize)
                int p = i * itemsize;
                for (int j = 0; j < itemsize; j++) {
                    exp[j] = bytes[p + j];
                }

                // Get pointer and check contents for correct data
                index[0] = i;
                BufferPointer bp = view.getPointer(index);
                assertBytesEqual("getPointer value", exp, bp.storage, bp.offset);
                assertEquals("getPointer size wrong", itemsize, bp.size);
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
     * Test method for {@link org.python.core.PyBUF#release()}.
     */
    public void testRelease() {
        for (BufferTestPair test : buffersToRead) {
            System.out.println("release: " + test);
            BufferProtocol obj = test.exporter;

            // The object should already be exporting test.simple and test.strided = 2 exports
            PyBuffer a = test.simple; // 1 exports
            PyBuffer b = test.strided; // 2 exports
            PyBuffer c = obj.getBuffer(PyBUF.SIMPLE | PyBUF.FORMAT); // = 3 exports
            checkExporting(obj);

            // Now see that releasing in some other order works correctly
            b.release(); // = 2 exports
            a.release(); // = 1 export
            checkExporting(obj);
            int flags = PyBUF.STRIDES | PyBUF.FORMAT;

            // You can get a buffer from a buffer (for SimpleExporter only c is alive)
            PyBuffer d = c.getBuffer(flags); // = 2 exports
            c.release(); // = 1 export
            checkExporting(obj);
            d.release(); // = 0 exports
            checkNotExporting(obj);

            // But fails if buffer has been finally released
            try {
                a = d.getBuffer(flags); // = 0 exports (since disallowed)
                fail("getBuffer after final release not detected");
            } catch (Exception e) {
                // Detected *and* prevented?
                checkNotExporting(obj);
            }

            // Further releases are also an error
            try {
                a.release(); // = -1 exports (oops)
                fail("excess release not detected");
            } catch (Exception e) {
                // Success
            }

        }
    }

    /**
     * Error if exporter is not actually exporting (and is of a type that locks on export).
     *
     * @param exporter
     */
    private void checkExporting(BufferProtocol exporter) {
        if (exporter instanceof TestableExporter) {
            assertTrue("exports not being counted", ((TestableExporter)exporter).isExporting());
        } else if (exporter instanceof PyByteArray) {
            // Size-changing access should fail
            try {
                ((PyByteArray)exporter).bytearray_extend(Py.One); // Appends one zero byte
                fail("bytearray_extend with exports should fail");
            } catch (Exception e) {
                // Success
            }
        }
        // Other types cannot be checked
    }

    /**
     * Error if exporter is exporting (and is of a type that locks on export).
     *
     * @param exporter
     */
    private void checkNotExporting(BufferProtocol exporter) {
        if (exporter instanceof TestableExporter) {
            assertFalse("exports falsely counted", ((TestableExporter)exporter).isExporting());
        } else if (exporter instanceof PyByteArray) {
            // Size-changing access should fail
            try {
                ((PyByteArray)exporter).bytearray_extend(Py.One);
            } catch (Exception e) {
                fail("bytearray unexpectedly locked");
            }
        }
        // Other types cannot be checked
    }

    /**
     * Check that reusable PyBuffer is re-used, and that non-reusable PyBuffer is not re-used.
     *
     * @param exporter
     */
    private void checkReusable(BufferProtocol exporter, PyBuffer previous, PyBuffer latest) {
        assertNotNull("Re-used PyBuffer reference null", latest);
        if (exporter instanceof PyByteArray) {
            // Re-use prohibited because might have resized while released
            assertFalse("PyByteArray buffer reused unexpectedly", latest == previous);
        } else if (exporter instanceof TestableExporter && !((TestableExporter)exporter).reusable) {
            // Special test case where re-use prohibited
            assertFalse("PyBuffer reused unexpectedly", latest == previous);
        } else {
            // Other types of TestableExporter and PyString all re-use
            assertTrue("PyBuffer not re-used as expected", latest == previous);
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#getStrides()}.
     */
    public void testGetStrides() {
        for (BufferTestPair test : buffersToRead) {
            System.out.println("getStrides: " + test);
            // When not requested ... (different from CPython)
            int[] strides = test.simple.getStrides();
            assertNotNull("strides[] should always be provided", strides);
            assertIntsEqual("simple.strides", test.strides, strides);
            // And when requested, ought to be as expected
            strides = test.strided.getStrides();
            assertNotNull("strides[] not provided when requested", strides);
            assertIntsEqual("strided.strides", test.strides, strides);
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#getSuboffsets()}.
     */
    public void testGetSuboffsets() {
        for (BufferTestPair test : buffersToRead) {
            System.out.println("getSuboffsets: " + test);
            // Null for all test material
            assertNull(test.simple.getSuboffsets());
            assertNull(test.strided.getSuboffsets());
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#isContiguous(char)}.
     */
    public void testIsContiguous() {
        for (BufferTestPair test : buffersToRead) {
            System.out.println("isContiguous: " + test);
            // True for all test material and orders (since 1-dimensional)
            for (String orderMsg : validOrders) {
                char order = orderMsg.charAt(0);
                assertTrue(orderMsg, test.simple.isContiguous(order));
                assertTrue(orderMsg, test.strided.isContiguous(order));
            }
        }
    }

    private static final String[] validOrders = {"C-contiguous test fail",
                                                 "F-contiguous test fail",
                                                 "Any-contiguous test fail"};

    /**
     * Test method for {@link org.python.core.PyBuffer#getFormat()}.
     */
    public void testGetFormat() {
        for (BufferTestPair test : buffersToRead) {
            System.out.println("getFormat: " + test);
            // When not requested ... (different from CPython)
            assertNotNull("format should always be provided", test.simple.getFormat());
            assertNotNull("format should always be provided", test.strided.getFormat());
            // And, we can ask for it explicitly ...
            PyBuffer simpleWithFormat = test.exporter.getBuffer(PyBUF.SIMPLE | PyBUF.FORMAT);
            PyBuffer stridedWithFormat = test.exporter.getBuffer(PyBUF.STRIDES | PyBUF.FORMAT);
            // "B" for all test material where requested in flags
            assertEquals("B", simpleWithFormat.getFormat());
            assertEquals("B", stridedWithFormat.getFormat());
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#getItemsize()}.
     */
    public void testGetItemsize() {
        for (BufferTestPair test : buffersToRead) {
            System.out.println("getItemsize: " + test);
            // Unity for all test material
            assertEquals(1, test.simple.getItemsize());
            assertEquals(1, test.strided.getItemsize());
        }
    }

    /**
     * A class to act as an exporter that uses the SimpleReadonlyBuffer. This permits testing
     * abstracted from the Jython interpreter.
     * <p>
     * The exporter exports a new PyBuffer object to each consumer (although each references the
     * same internal storage) and it does not track their fate. You are most likely to use this
     * approach with an exporting object that is immutable (or at least fixed in size).
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

        protected Reference<PyBuffer> export;

        /**
         * Try to re-use existing exported buffer, or return null if can't.
         */
        protected PyBuffer getExistingBuffer(int flags) {
            PyBuffer pybuf = null;
            if (export != null) {
                // A buffer was exported at some time.
                pybuf = export.get();
                if (pybuf != null) {
                    // And this buffer still exists: expect this to provide a further reference
                    pybuf = pybuf.getBuffer(flags);
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

        /**
         * Determine whether this object permits it's buffers to re-animate themselves. If not, a
         * call to getBuffer on a released buffer should not return the same buffer.
         */
        public boolean reusable = true;

    }

    /**
     * A class to act as an exporter that uses the SimpleStringBuffer. This permits testing
     * abstracted from the Jython interpreter.
     * <p>
     * The exporter shares a single exported buffer between all consumers but does not need to take
     * any action when that buffer is finally released. You are most likely to use this approach
     * with an exporting object type that does not modify its behaviour while there are active
     * exports, but where it is worth avoiding the cost of duplicate buffers. This is the case with
     * PyString, where some buffer operations cause construction of a byte array copy of the Java
     * String, which it is desirable to do only once.
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
            PyBuffer pybuf = getExistingBuffer(flags);
            if (pybuf == null) {
                // No existing export we can re-use
                pybuf = new SimpleStringBuffer(flags, storage);
                // Hold a reference for possible re-use
                export = new SoftReference<PyBuffer>(pybuf);
            }
            return pybuf;
        }

    }

    /**
     * A class to act as an exporter that uses the SimpleBuffer. This permits testing abstracted
     * from the Jython interpreter.
     * <p>
     * The exporter shares a single exported buffer between all consumers and needs to take any
     * action immediately when that buffer is finally released. You are most likely to use this
     * approach with an exporting object type that modifies its behaviour while there are active
     * exports, but where it is worth avoiding the cost of duplicate buffers. This is the case with
     * PyByteArray, which prohibits operations that would resize it, while there are outstanding
     * exports.
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
            reusable = false;
        }

        @Override
        public PyBuffer getBuffer(int flags) {
            // If we have already exported a buffer it may still be available for re-use
            PyBuffer pybuf = getExistingBuffer(flags);
            if (pybuf == null) {
                // No existing export we can re-use
                pybuf = new SimpleWritableBuffer(flags, storage) {

                    protected void releaseAction() {
                        export = null;
                    }
                };

                // Hold a reference for possible re-use
                export = new WeakReference<PyBuffer>(pybuf);
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
        public ByteMaterial(int start, int inc, int count) {
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
    }

    /**
     * Customised assert method comparing a buffer pointer to a byte array, usually the one from
     * ByteMaterial.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param bp result to test
     */
    void assertBytesEqual(String message, byte[] expected, BufferPointer bp) {
        int size = bp.size;
        if (size != expected.length) {
            fail(message + " (size)");
        } else {
            int len = bp.storage.length;
            if (bp.offset < 0 || bp.offset + size > len) {
                fail(message + " (offset)");
            } else {
                // Should be safe to compare the bytes
                int i = bp.offset, j;
                for (j = 0; j < size; j++) {
                    if (bp.storage[i++] != expected[j]) {
                        break;
                    }
                }
                if (j < size) {
                    fail(message + " (byte at " + j + ")");
                }
            }
        }
    }

    /**
     * Customised assert method comparing a buffer pointer to a byte array, usually the one from
     * ByteMaterial.
     *
     * @param expected expected byte array
     * @param bp result to test
     */
    void assertBytesEqual(byte[] expected, BufferPointer bp) {
        assertBytesEqual("", expected, bp);
    }

    /**
     * Customised assert method comparing a byte arrays: values in the actual value starting at
     * actual[actualStart] must match all those in expected[], and there must be enough of them.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param actual result to test
     * @param actualStart where to start the comparison in actual
     */
    void assertBytesEqual(String message, byte[] expected, byte[] actual, int actualStart) {
        assertBytesEqual(message, expected, 0, actual, actualStart, expected.length);
    }

    /**
     * Customised assert method comparing a byte arrays: values starting at actual[actualStart] must
     * those starting at actual[actualStart], for a distance of n bytes.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param expectedStart where to start the comparison in expected
     * @param actual result to test
     * @param actualStart where to start the comparison in actual
     * @param n number of bytes to test
     */
    void assertBytesEqual(String message, byte[] expected, int expectedStart, byte[] actual,
            int actualStart, int n) {
        if (actualStart < 0 || expectedStart < 0) {
            fail(message + " (start<0)");
        } else if (actualStart + n > actual.length || expectedStart + n > expected.length) {
            fail(message + " (too short)");
        } else {
            // Should be safe to compare the values
            int i = actualStart, j, jLimit = expectedStart + n;
            for (j = expectedStart; j < jLimit; j++) {
                if (actual[i++] != expected[j]) {
                    break;
                }
            }
            if (j < jLimit) {
                System.out.println("  expected:"
                        + Arrays.toString(Arrays.copyOfRange(expected, expectedStart, expectedStart
                                + n)));
                System.out.println("    actual:"
                        + Arrays.toString(Arrays.copyOfRange(actual, actualStart, actualStart + n)));
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
    void assertIntsEqual(String message, int[] expected, int[] actual, int offset) {
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
    void assertIntsEqual(String message, int[] expected, int[] actual) {
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
     * Element for queueing tests, wraps an exporter object with (a copy of) the material from which
     * it was created, and several PyBuffer views.
     */
    static class BufferTestPair {

        static final int[] STRIDES_1D = {1};

        BufferProtocol exporter;
        ByteMaterial material;
        PyBuffer simple, strided;
        int[] shape, strides;

        /**
         * A test to do and the material for constructing it (and its results).
         *
         * @param exporter
         * @param material
         * @param shape of the array, when testing in N-dimensions
         * @param stride of the array, when testing in N-dimensions
         */
        public BufferTestPair(BufferProtocol exporter, ByteMaterial material, int[] shape,
                int[] strides) {
            this.exporter = exporter;
            this.material = new ByteMaterial(material.ints);
            this.shape = shape;
            this.strides = strides;
            try {
                simple = exporter.getBuffer(PyBUF.SIMPLE);
                strided = exporter.getBuffer(PyBUF.STRIDES);
            } catch (Exception e) {
                // Leave them null if we can't get a PyBuffer: test being set up will fail.
                // Silent here, but explicit test of getBuffer will reproduce and log this failure.
            }
        }

        /**
         * A test to do and the material for constructing it (and its results) in one dimension.
         *
         * @param exporter
         * @param material
         */
        public BufferTestPair(BufferProtocol exporter, ByteMaterial material) {
            this(exporter, material, new int[1], STRIDES_1D);
            shape[0] = material.length;
        }

        @Override
        public String toString() {
            return exporter.getClass().getSimpleName() + "( " + material.toString() + " )";
        }

    }
}
