package org.python.core;

import static org.junit.Assert.*;
import static org.python.core.ByteBufferTestSupport.assertIntsEqual;
import static org.python.core.PyBufferTestSupport.bytesFromByteAt;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.python.core.ByteBufferTestSupport.ByteMaterial;
import org.python.core.PyBufferTestSupport.ExporterFactory;
import org.python.core.PyBufferTestSupport.ReadonlyExporterFactory;
import org.python.core.PyBufferTestSupport.SlicedTestSpec;
import org.python.core.PyBufferTestSupport.TestSpec;
import org.python.core.PyBufferTestSupport.TestSpec.ObjectAndView;
import org.python.core.PyBufferTestSupport.WritableExporterFactory;
import org.python.core.buffer.BaseBuffer;
import org.python.core.buffer.SimpleBuffer;
import org.python.core.buffer.SimpleStringBuffer;
import org.python.core.buffer.SimpleWritableBuffer;
import org.python.util.PythonInterpreter;

/**
 * Test the several implementations (and exporters) of the PyBuffer interface provided in the Jython
 * core.
 * <p>
 * The approach is to create test material once (static initialisation) that has the necessary
 * variety in byte array values. From these raw values, during a phase of (static) initialisation
 * invoked by the JUnit framework, we create a rich set of root objects, and slices made from them,
 * paired with the value those buffer views should have, represented as byte[] (and a few other
 * types). These are <code>BufferTestPair</code> objects. The collection is the <b>test data</b>.
 * <p>
 * The JUnit framework will then construct an instance of this test using one
 * <code>BufferTestPair</code> object from the test data, and call one test method. The
 * initialisation of the test fixture with a <code>BufferTestPair</code> object provides the test
 * method with a <code>PyBuffer</code> object on which to operate, and enough ancilliary information
 * to deduce the expected outcome. In particular, it will be apparent whether write actions should
 * succeed or raise an exception.
 * <p>
 * The Jython buffer API follows the structures of the CPython buffer API so that it supports in
 * principle the use of multi-dimensional, strided and indirect array structures as buffers.
 * However, actual buffers in the Jython core, and therefore these tests, limit themselves to one
 * dimensional (possibly non-contiguous) directly-indexed buffers. Some tests apply directly to the
 * N-dimensional cases, and some need a complete re-think. Sub-classing this test would probably be
 * a good way to extend it to a wider range.
 */
@RunWith(Parameterized.class)
public class PyBufferTest {

    /** Control amount of output. Instance variable so can be adjusted temporarily in test. */
    protected int verbosity = 0;

    /** Print a list of the test material. (From JUnit 4.12 use Parameters(name)). */
    protected static final boolean PRINT_KEY = false;

    /** Size of some large arrays. */
    static final int LONG = 1000;

    /** Lengths we will use if we can when slicing view */
    protected static final int[] sliceLengths = {1, 2, 5, 0, LONG / 4};

    /** Step sizes we will use if we can when slicing view */
    protected static final int[] sliceSteps = {1, 2, 3, 7};

    /** Exception raising requires the Jython interpreter to be initialised **/
    protected static PythonInterpreter interp = new PythonInterpreter();

    /** The test material and a buffer created by the test-runner. */
    protected TestSpec spec;
    protected ByteMaterial ref;
    protected BufferProtocol obj;
    protected PyBuffer view;

    /**
     * Construct an instance to run one test, using one set of test data.
     *
     * @param pair The test material and a buffer created by the test-runner.
     */
    public PyBufferTest(TestSpec spec) {
        this.spec = spec;
        ref = spec.ref;
        createObjAndView();
    }

    /**
     * Create (or re-create) the test object and view from the specification. Test cases that call a
     * mutator repeatedly must call this each time in order to work with clean objects.
     */
    protected void createObjAndView() {
        TestSpec.ObjectAndView pair = spec.makePair();
        obj = pair.obj;
        view = pair.view;
    }

    /*
     * Values for initialising the exporters.
     */
    protected static final ByteMaterial byteMaterial = new ByteMaterial(10, 16, 3);
    protected static final ByteMaterial abcMaterial = new ByteMaterial("abcdefgh");
    protected static final ByteMaterial stringMaterial = new ByteMaterial("Mon côté fâcheux");
    protected static final ByteMaterial emptyMaterial = new ByteMaterial(new byte[0]);
    protected static final ByteMaterial longMaterial = new ByteMaterial(0, LONG, 5);

    /**
     * Generate test data to be held in the testing framework and used to construct tests. This
     * method is called once by the test framework. Each element of the returned collection is a
     * specification that becomes the arguments to the constructor when JUnit prepares to invoke a
     * test.
     * <p>
     * Internally, this method creates a small number of instances of the object types whose
     * <code>PyBuffer</code> export mechanism is to be tested. Each is paired with a reference value
     * represented in several forms. The <code>PyBufferTestSupport</code> class then multiplies
     * these by creating a selection of feasible sliced views, the whole collection of root and
     * slice objects being returned.
     *
     * @return generated list of test data
     */
    @Parameters
    public static Collection<TestSpec[]> genTestSpecs() {

        PyBufferTestSupport s = new PyBufferTestSupport(sliceLengths, sliceSteps);

        // Tests using local types of exporter

        ExporterFactory simpleExporter = new SimpleExporterFactory();
        s.add(simpleExporter, byteMaterial);

        ExporterFactory simpleWritableExporter = new WritableExporterFactory() {

            @Override
            public BufferProtocol make(ByteMaterial m) {
                return new SimpleWritableExporter(m.getBytes());
            }

        };
        s.add(simpleWritableExporter, abcMaterial);
        s.add(simpleWritableExporter, emptyMaterial);

        ExporterFactory stringExporter = new ReadonlyExporterFactory() {

            @Override
            public BufferProtocol make(ByteMaterial m) {
                return new StringExporter(m.string);
            }

        };
        s.add(stringExporter, stringMaterial);

        // Tests with an buffer implementation directly extending BaseBuffer

        ExporterFactory rollYourOwnExporter = new WritableExporterFactory() {

            @Override
            public BufferProtocol make(ByteMaterial m) {
                return new RollYourOwnExporter(m.getBytes());
            }

        };
        s.add(rollYourOwnExporter, byteMaterial);
        s.add(rollYourOwnExporter, emptyMaterial);

        // Tests with PyByteArray

        ExporterFactory pyByteArrayExporter = new WritableExporterFactory() {

            @Override
            public BufferProtocol make(ByteMaterial m) {
                return new PyByteArray(m.getBytes());
            }

        };
        s.add(pyByteArrayExporter, byteMaterial);
        s.add(pyByteArrayExporter, longMaterial);
        s.add(pyByteArrayExporter, emptyMaterial);

        // Tests with PyString

        ExporterFactory pyStringExporter = new ReadonlyExporterFactory() {

            @Override
            public BufferProtocol make(ByteMaterial m) {
                return new PyString(m.string);
            }

        };
        s.add(pyStringExporter, abcMaterial);
        s.add(pyStringExporter, emptyMaterial);

        // Ensure case is tested where PyByteArray has an internal offset

        ExporterFactory offsetPyByteArrayExporter = new WritableExporterFactory() {

            @Override
            public BufferProtocol make(ByteMaterial m) {
                // In this PyByteArray the data will not start at storage[0]
                final int OFFSET = 4;
                byte[] b = m.getBytes();
                // Make a copy with padding at the start and wrap it in a bytearray
                byte[] data = new byte[OFFSET + b.length];
                System.arraycopy(b, 0, data, OFFSET, b.length);
                PyByteArray a = new PyByteArray(data);
                // This operation may (will) be implemented without data movement
                a.delRange(0, OFFSET);
                assert a.__alloc__() > b.length;
                return a;
            }

        };
        s.add(offsetPyByteArrayExporter, byteMaterial);
        s.add(offsetPyByteArrayExporter, longMaterial);

        // Return the generated test data

        List<TestSpec[]> ret = s.getTestData();
        if (PRINT_KEY) {
            int key = 0;
            for (TestSpec[] r : ret) {
                TestSpec spec = r[0];
                System.out.printf("%6d : %s\n", key++, spec.toString());
            }
        }
        return ret;
    }

    /**
     * Brevity allowing each test to announce itself by naming the part of the api tested.
     *
     * @param api naming the part of the api tested
     */
    protected void announce(String api) {
        if (verbosity > 0) {
            System.out.printf("%-30s %s\n", api + ":", spec.toString());
        }
    }

    /** Test method for {@link org.python.core.PyBUF#isReadonly()}. */
    @Test
    public void testIsReadonly() {
        announce("isReadonly");
        assertTrue(view.isReadonly() == spec.readonly);
    }

    /** Test method for {@link org.python.core.PyBUF#getNdim()}. */
    @Test
    public void testGetNdim() {
        announce("getNdim");
        // Only know how to do 1 dimension at the moment
        assertEquals("unexpected ndim", spec.shape.length, view.getNdim());
    }

    /** Test method for {@link org.python.core.PyBUF#getShape()}. */
    @Test
    public void testGetShape() {
        announce("getShape");
        int[] shape = view.getShape();
        assertNotNull("shape[] should always be provided", shape);
        assertIntsEqual("unexpected shape", spec.shape, shape);
    }

    /** Test method for {@link org.python.core.PyBUF#getLen()}. */
    @Test
    public void testGetLen() {
        announce("getLen");
        assertEquals("unexpected length", ref.length, view.getLen());
    }

    /** Test method for {@link org.python.core.PyBUF#getObj()}. */
    @Test
    public void testGetObj() {
        announce("getObj");
        assertEquals("unexpected exporting object", obj, view.getObj());
    }

    /** Test method for {@link org.python.core.PyBuffer#byteAt(int)}. */
    @Test
    public void testByteAt() {
        announce("byteAt");
        for (int i = 0; i < ref.length; i++) {
            assertEquals(ref.bytes[i], view.byteAt(i));
        }
    }

    /** Test method for {@link org.python.core.PyBuffer#byteAt(int[])}. */
    @Test
    public void testByteAtNdim() {
        announce("byteAt (n-dim)");
        int[] index = new int[1];

        if (view.getShape().length != 1) {
            fail("Test not implemented if dimensions != 1");
        }
        // Run through 1D index for view
        for (int i = 0; i < ref.length; i++) {
            index[0] = i;
            assertEquals(ref.bytes[i], view.byteAt(index));
        }

        // Check 2D index throws
        try {
            view.byteAt(0, 0);
            fail("Use of 2D index did not raise exception");
        } catch (PyException pye) {
            // Expect BufferError
            assertEquals(Py.BufferError, pye.type);
        }
    }

    /** Test method for {@link org.python.core.PyBuffer#intAt(int)}. */
    @Test
    public void testIntAt() {
        announce("intAt");
        for (int i = 0; i < ref.length; i++) {
            assertEquals(ref.ints[i], view.intAt(i));
        }
    }

    /** Test method for {@link org.python.core.PyBuffer#intAt(int[])}. */
    @Test
    public void testIntAtNdim() {
        announce("intAt (n-dim)");
        int[] index = new int[1];

        if (view.getShape().length != 1) {
            fail("Test not implemented for dimensions != 1");
        }
        // Run through 1D index for view
        for (int i = 0; i < ref.length; i++) {
            index[0] = i;
            assertEquals(ref.ints[i], view.intAt(index));
        }
        // Check 2D index throws
        try {
            view.intAt(0, 0);
            fail("Use of 2D index did not raise exception");
        } catch (PyException pye) {
            // Expect BufferError
            assertEquals(Py.BufferError, pye.type);
        }
    }

    /** Test method for {@link org.python.core.PyBuffer#storeAt(byte, int)}. */
    @Test
    public void testStoreAt() {
        announce("storeAt");
        int n = ref.length;
        int[] exp = ref.ints.clone();
        if (!spec.readonly) {
            // Write modified test material into each location using storeAt()
            for (int i = 0; i < n; i++) {
                byte v = (byte)(exp[i] ^ 3); // twiddle some bits
                view.storeAt(v, i);
            }
            // Compare each location with modified test data using intAt()
            for (int i = 0; i < n; i++) {
                assertEquals(exp[i] ^ 3, view.intAt(i));
            }
        } else {
            // Write should throw
            for (int i = 0; i < n; i++) {
                try {
                    view.storeAt((byte)3, i);
                    fail("Write access not prevented: " + spec);
                } catch (PyException pye) {
                    // Expect TypeError (not BufferError which getBuffer can raise)
                    assertEquals(Py.TypeError, pye.type);
                }
            }
        }
    }

    /** Test method for {@link org.python.core.PyBuffer#storeAt(byte, int[])}. */
    @Test
    public void testStoreAtNdim() {
        announce("storeAt (n-dim)");
        int[] index = new int[1];
        int n = ref.length;
        int[] exp = ref.ints.clone();
        if (!spec.readonly) {
            // Write modified test material into each location using storeAt()
            for (int i = 0; i < n; i++) {
                index[0] = i;
                byte v = (byte)(exp[i] ^ 3); // twiddle some bits
                view.storeAt(v, index);
            }
            // Compare each location with modified test data using intAt()
            for (int i = 0; i < n; i++) {
                index[0] = i;
                assertEquals(exp[i] ^ 3, view.intAt(index));
            }
            if (spec.shape.length == 1) {
                // Check 2D index throws
                try {
                    view.storeAt((byte)1, 0, 0);
                    fail("Use of 2D index did not raise exception");
                } catch (PyException pye) {
                    // Expect BufferError
                    // XXX ... but should it be TypeError here?
                    assertEquals(Py.BufferError, pye.type);
                }
            }
        } else {
            // Write should throw
            for (int i = 0; i < n; i++) {
                index[0] = i;
                try {
                    view.storeAt((byte)3, index);
                    fail("Write access not prevented: " + spec);
                } catch (PyException pye) {
                    // Expect TypeError (not BufferError which getBuffer can raise)
                    assertEquals(Py.TypeError, pye.type);
                }
            }
        }
    }

    /** Test method for {@link org.python.core.PyBuffer#copyTo(byte[], int)}. */
    @Test
    public void testCopyTo() {
        final int OFFSET = 5;
        announce("copyTo");
        int n = ref.length;
        // Try with zero offset
        byte[] actual = new byte[n];
        view.copyTo(actual, 0);
        ByteBufferTestSupport.assertBytesEqual("copyTo() incorrect", ref.bytes, actual, 0);
        // Try to middle of array
        actual = new byte[n + 2 * OFFSET];
        view.copyTo(actual, OFFSET);
        ByteBufferTestSupport.assertBytesEqual("copyTo(offset) incorrect", ref.bytes, actual,
                OFFSET);
        assertEquals("data before destination", 0, actual[OFFSET - 1]);
        assertEquals("data after destination", 0, actual[OFFSET + n]);
    }

    /** Test method for {@link org.python.core.PyBuffer#copyTo(int, byte[], int, int)}. */
    @Test
    public void testSliceCopyTo() {
        announce("copyTo (from slice)");
        final int OFFSET = 3;

        int n = ref.length;
        byte[] before = new byte[n + 2 * OFFSET];
        final byte BLANK = 7;
        Arrays.fill(before, BLANK);

        // Try destination positions in actual[] of 0 and OFFSET
        for (int destPos = 0; destPos <= OFFSET; destPos += OFFSET) {
            // Try source positions in 0 and OFFSET
            for (int srcIndex = 0; srcIndex <= OFFSET; srcIndex += OFFSET) {

                // A variety of lengths from zero to (n-srcIndex)-ish
                for (int length = 0; srcIndex + length <= n; length = 2 * length + 1) {
                    doTestSliceCopyTo(srcIndex, before, destPos, length, n);
                }

                // And from exactly n-srcIndex down to zero-ish
                for (int trim = 0; srcIndex + trim <= n; trim = 2 * trim + 1) {
                    int length = n - srcIndex - trim;
                    doTestSliceCopyTo(srcIndex, before, destPos, length, n);
                }
            }
        }
    }

    /** Helper function for {@link #testSliceCopyTo()} */
    private void doTestSliceCopyTo(int srcIndex, byte[] before, int destPos, int length, int n) {

        if (verbosity > 1) {
            System.out.printf("  copy src[%d:%d] (%d) to dst[%d:%d] (%d)\n", srcIndex, srcIndex
                    + length, n, destPos, destPos + length, before.length);
        }

        // Test the method
        byte[] dest = before.clone();
        view.copyTo(srcIndex, dest, destPos, length);

        // Check the write to dest contains a correctly positioned copy of the view (=ref.bytes)
        byte[] viewBytes = PyBufferTestSupport.bytesFromByteAt(view);
        ByteBufferTestSupport.checkReadCorrect(ref.bytes, viewBytes, dest, destPos, length, 1,
                srcIndex, 1);

    }

    /** Test method for {@link org.python.core.PyBuffer#copyFrom(byte[], int, int, int)}. */
    @Test
    public void testCopyFrom() {
        announce("copyFrom");
        final int OFFSET = 3;
        final int L = ref.length;

        // Make some source material to copy from (longer since need to test at OFFSET too).
        byte[] src = (new ByteMaterial(48, ref.length + OFFSET, 1)).bytes;

        // Our test is against the underlying object of which the view may be a slice
        TestSpec underlying = spec.getOriginal();
        int start = spec.getStart();
        int stride = spec.getStride();

        // Try source positions in 0 and OFFSET
        for (int srcPos = 0; srcPos <= OFFSET; srcPos += OFFSET) {

            // Try destination positions in test object of 0 and OFFSET
            for (int destIndex = 0; destIndex <= OFFSET; destIndex += OFFSET) {

                // A variety of lengths from zero to (n-destIndex)-ish
                for (int length = 0; destIndex + length <= L; length = 2 * length + 1) {
                    doTestCopyFrom(src, srcPos, underlying, start, length, stride, destIndex);
                }

                // And from exactly n-destIndex down to zero-ish
                for (int trim = 0; destIndex + trim <= L; trim = 2 * trim + 1) {
                    int length = ref.length - destIndex - trim;
                    doTestCopyFrom(src, srcPos, underlying, start, length, stride, destIndex);
                }
            }
        }
    }

    /** Helper function for {@link #testCopyFrom()} */
    private void doTestCopyFrom(byte[] src, int srcPos, TestSpec underlying, int start, int length,
            int stride, int destIndex) {

        if (verbosity > 1) {
            System.out.printf("  copy src[%d:%d] (%d) to dst[%d:%d]\n", srcPos, srcPos + length,
                    ref.length, destIndex, destIndex + length);
        }

        // Initialise the object (have to do each time)
        createObjAndView();
        PyBuffer underlyingView = obj.getBuffer(underlying.flags & ~PyBUF.WRITABLE);
        byte[] before = bytesFromByteAt(underlyingView);

        if (!spec.readonly) {
            // This is the call we are testing (a write operation).
            view.copyFrom(src, srcPos, destIndex, length);

            // Our test is against the underlying object of which the view may be a slice
            byte[] after = bytesFromByteAt(underlyingView);
            int underlyingDestIndex = start + destIndex * stride;

            // Test that the corresponding bytes of the underlying object match data copied in
            ByteBufferTestSupport.checkWriteCorrect(before, after, src, srcPos, length, 1,
                    underlyingDestIndex, stride);

        } else {
            // Should fail (write operation)
            try {
                view.copyFrom(src, srcPos, destIndex, length);
                fail("Write access not prevented: " + spec);
            } catch (PyException pye) {
                // Expect TypeError only if the buffer was readonly
                assertEquals(Py.TypeError, pye.type);
            }
        }
    }

    /** Test method for {@link org.python.core.PyBuffer#copyFrom(PyBuffer)}. */
    @Test
    public void testCopyFromPyBuffer() {
        announce("copyFrom (PyBuffer)");

        /*
         * The test material (this time) presents a view that has n items of size i, that are spaced
         * in the underlying buffer with stride s.
         */
        final int n = spec.ref.length;
        final int p = spec.getStride();

        // The material we copy to it should have these strides:
        int[] srcStrides;
        if (n < 2) {
            srcStrides = new int[] {1};
        } else if (p > 2 || p < -2) {
            srcStrides = new int[] {1, p - 1, p, p + 1, -p + 1, -p, -p - 1};
        } else if (p == 2 || p == -2) {
            srcStrides = new int[] {1, 2, 3, -1, -2, -3};
        } else { // ( s==1 || s==-1 )
            srcStrides = new int[] {1, 2, -1, -2};
        }

        // Also need the maximum absolute value
        int maxStride = 0;
        for (int stride : srcStrides) {
            if (stride > maxStride) {
                maxStride = stride;
            } else if (-stride > maxStride) {
                maxStride = -stride;
            }
        }

        // And these offsets to the lowest-indexed source item
        int maxOffset = n + 1;
        int[] srcOffsets = new int[] {0, (maxOffset + 1) / 3, maxOffset};

        // Make the source material to copy from, big enough to accommodate n strides
        int srcMaterialSize = n * maxStride + maxOffset;
        ByteMaterial srcMaterial = new ByteMaterial(48, srcMaterialSize, 1);

        /*
         * Now we need a series of PyBuffer views on the source data, sliced and offset according to
         * the offset and stride values we have enumerated. We'd like to use the same factory as the
         * current test view (this.view), because copy from its own type might be optimised, and a
         * different, bog-standard factory to test the general case.
         */
        ExporterFactory[] factories = {spec.factory, new SimpleExporterFactory()};

        for (ExporterFactory factory : factories) {
            /*
             * We'll use the same apparatus to create the source buffer as we use to make the test
             * cases. The specifications for them will all be derived from this one:
             */
            TestSpec original = new TestSpec(factory, srcMaterial);
            /*
             * Do this where the pattern of indices constituting src overlaps (or not) the pattern
             * of view in challenging ways, including greater and smaller strides.
             */
            for (int stride : srcStrides) {
                for (int offset : srcOffsets) {
                    int start = (stride > 0) ? offset : srcMaterialSize - offset - 1;
                    doTestCopyFrom(original, start, n, stride);
                }
            }
        }

    }

    /** Helper function for {@link #testCopyFromPyBuffer()} */
    private void doTestCopyFrom(TestSpec original, int start, int n, int stride) {

        // Derive sliced test material from the original
        TestSpec srcSpec = new SlicedTestSpec(original, 1, start, n, stride);
        ObjectAndView pair = srcSpec.makePair();
        PyBuffer src = pair.view;
        byte[] srcBytes = srcSpec.ref.bytes;

        // And for the test object
        int s = spec.getStart();
        int p = spec.getStride();
        String srcName = pair.obj.getClass().getSimpleName();
        if (verbosity > 1) {
            int end = start + (n - 1) * stride + (stride > 0 ? 1 : -1);
            int e = s + (n - 1) * p + (p > 0 ? 1 : -1);
            System.out.printf("  copy from src[%d:%d:%d] %s(%d) to obj[%d:%d:%d]\n", //
                    start, end, stride, srcName, n, //
                    s, e, p);
        }

        // Initialise the destination object and view (have to do each time) from spec
        createObjAndView();

        // Our test is against the underlying object of which the view may be a slice
        TestSpec underlying = spec.getOriginal();
        PyBuffer underlyingView = obj.getBuffer(underlying.flags & ~PyBUF.WRITABLE);
        byte[] before = bytesFromByteAt(underlyingView);

        if (!spec.readonly) {
            // This is the call we are testing (a write operation).
            view.copyFrom(src);

            // Our test is against the underlying object of which the view may be a slice
            byte[] after = bytesFromByteAt(underlyingView);

            // Test that the corresponding bytes of the underlying object match data copied in
            ByteBufferTestSupport.checkWriteCorrect(before, after, srcBytes, 0, n, 1, s, p);

        } else {
            // Should fail (write operation)
            try {
                view.copyFrom(src);
                fail("Write access not prevented: " + spec);
            } catch (PyException pye) {
                // Expect TypeError only if the buffer was readonly
                assertEquals(Py.TypeError, pye.type);
            }
        }
    }

    /** Test method for {@link org.python.core.PyBuffer#copyFrom(PyBuffer)} when source is same. */
    @Test
    public void testCopyFromSelf() {
        announce("copyFrom (self)");

        // The test material (this time) presents a view of n bytes from a buffer of L bytes.
        final int n = ref.length;
        TestSpec original = spec.getOriginal();
        if (spec.readonly || spec == original || n < 1) {
            // We're only testing with sliced writable views
            return;
        }
        final int p = spec.getStride();
        final int L = original.ref.length;

        /*
         * We want to make another sliced view on the same test object, with the same number of
         * items n, but different stride and/or offset. Strides above, equal to and below (if
         * possible) the destination stride are of interest.
         */
        int[] srcStrides;
        if (n < 2) {
            srcStrides = new int[] {1};
        } else if (p > 2 || p < -2) {
            srcStrides = new int[] {1, p - 1, p, p + 1, -p + 1, -p, -p - 1};
        } else if (p == 2 || p == -2) {
            srcStrides = new int[] {1, 2, 3, -1, -2, -3};
        } else { // ( p==1 || p==-1 )
            srcStrides = new int[] {1, 2, -1, -2};
        }

        for (int srcStride : srcStrides) {
            int absStride;
            if (srcStride > 0) {
                absStride = srcStride;
                /*
                 * Compute the highest start index such that we can fit n items spaced at absStride
                 * into the buffer before reaching the end.
                 */
                int maxOffset = L - 1 - absStride * (n - 1);
                // There might not be such an start. If there is, we can do one or more tests.
                if (maxOffset >= 0) {
                    // A positive-stepping slice could fit, for some start positions
                    int incOffset = 1 + maxOffset / 4;
                    for (int srcOffset = 0; srcOffset <= maxOffset; srcOffset += incOffset) {
                        doTestCopyFromSelf(srcOffset, srcStride, n);
                    }
                }
            } else {// srcStride < 0
                absStride = -srcStride;
                /*
                 * Compute the lowest start index such that we can fit n items spaced at absStride
                 * into the buffer before reaching the beginning.
                 */
                int minOffset = absStride * (n - 1) + 1;
                // There might not be such an start. If there is, we can do one or more tests.
                if (minOffset < L) {
                    // A negative-stepping slice could fit, for some start positions
                    int incOffset = 1 + (L - 1 - minOffset) / 4;
                    for (int srcOffset = L - 1; srcOffset > minOffset; srcOffset -= incOffset) {
                        doTestCopyFromSelf(srcOffset, srcStride, n);
                    }
                }
            }
        }
    }

    /** Helper function for {@link #testCopyFromPyBuffer()} */
    private void doTestCopyFromSelf(int srcStart, int srcStride, int n) {

        // Initialise the destination object and view (have to do each time) from spec
        createObjAndView();

        // Report the slice of the test object we are writing
        int dstStart = spec.getStart();
        int dstStride = spec.getStride();
        String srcName = obj.getClass().getSimpleName();
        if (verbosity > 1) {
            int srcEnd = srcStart + (n - 1) * srcStride + (srcStride > 0 ? 1 : -1);
            int dstEnd = dstStart + (n - 1) * dstStride + (dstStride > 0 ? 1 : -1);
            System.out.printf("  copy from obj[%d:%d:%d] %s(%d) to obj[%d:%d:%d]\n", //
                    srcStart, srcEnd, srcStride, srcName, n, //
                    dstStart, dstEnd, dstStride);
        }
        assert !spec.readonly;  // Test is only called if writable

        // Our test is against the underlying object of which the view may be a slice
        try (PyBuffer underlying = obj.getBuffer(PyBUF.FULL_RO)) {

            // Take a snapshot before the call
            byte[] before = bytesFromByteAt(underlying);

            // Take the required slice-view to use as the source.
            PyBuffer src = underlying.getBufferSlice(PyBUF.FULL_RO, srcStart, n, srcStride);
            byte[] srcBytes = bytesFromByteAt(src);

            // This is the call we are testing (a write operation).
            view.copyFrom(src);

            // Test that the corresponding bytes of the underlying object match data copied in
            byte[] after = bytesFromByteAt(underlying);
            ByteBufferTestSupport.checkWriteCorrect(before, after, srcBytes, 0, n, 1, dstStart,
                    dstStride);
        }
    }

    /**
     * Test method for {@link org.python.core.BufferProtocol#getBuffer()} and
     * {@link org.python.core.PyBuffer#getBuffer()}.
     */
    public void testGetBufferForRead() {
        announce("getBuffer(READ): ");
        // Go through all the allowed combinations of flags and tassles
        for (int flags : spec.validFlags) {
            for (int tassles : spec.validTassles) {
                PyBuffer view2 = view.getBuffer(flags | tassles);
                assertNotNull(view2);
            }
        }
    }

    /**
     * Test method for {@link org.python.core.BufferProtocol#getBuffer()} and
     * {@link org.python.core.PyBuffer#getBuffer()}.
     */
    public void testGetBufferForWrite() {
        announce("getBuffer(WRITE): ");
        if (!spec.readonly) {
            // Go through all the allowed combinations of flags and tassles adding WRITABLE
            for (int flags : spec.validFlags) {
                for (int tassles : spec.validTassles) {
                    PyBuffer view2 = view.getBuffer(flags | tassles | PyBUF.WRITABLE);
                    assertNotNull(view2);
                }
            }
        } else {
            // Go through all the allowed combinations of flags adding WRITABLE
            for (int flags : spec.validFlags) {
                try {
                    view.getBuffer(flags | PyBUF.WRITABLE);
                    fail("Write access not prevented: " + spec);
                } catch (PyException pye) {
                    // Expect BufferError
                    assertEquals(Py.BufferError, pye.type);
                }
            }
        }
    }

    /**
     * Test method for {@link org.python.core.PyBUF#release()}, exercising the release semantics of
     * PyBuffer in the try-with-resources pattern.
     */
    @Test
    public void testReleaseTryWithResources() {
        announce("release (via try)");
        /*
         * this.obj is an actual exporter and this.view is a buffer view onto it.
         */
        int flags = PyBUF.STRIDES | PyBUF.FORMAT;

        // The test setup should guarantee view is the only export
        try (PyBuffer c = obj.getBuffer(flags)) { // = 2 exports
            try (PyBuffer b = obj.getBuffer(PyBUF.FULL_RO); PyBuffer d = c.getBuffer(flags)) {
                maybeCheckExporting(obj);// = 4 exports
            }
            maybeCheckExporting(obj); // = 2 exports
            throw new Throwable("test");
        } catch (Throwable e) {
            // Meh
        }
        maybeCheckExporting(obj); // = 1 export
        view.release();
        maybeCheckNotExporting(obj); // = 0 exports
    }

    /**
     * Test method for {@link org.python.core.PyBUF#release()}, exercising release semantics in a
     * sequence orchestrated by the client code. At the end, the view should be fully released, (
     * {@link PyBuffer#isReleased()}<code>==true</code>).
     */
    @Test
    public void testRelease() {
        announce("release");
        int flags = PyBUF.STRIDES | PyBUF.FORMAT;

        // The object will be exporting view only
        PyBuffer a = view; // = 1 exports
        PyBuffer b = obj.getBuffer(PyBUF.FULL_RO); // = 2 export
        PyBuffer c = obj.getBuffer(flags); // = 3 exports
        maybeCheckExporting(obj);

        // Now see that releasing in some other order works correctly
        b.release(); // = 2 exports
        a.release(); // = 1 exports
        maybeCheckExporting(obj);

        // You can get a buffer from a buffer (c is unreleased)
        PyBuffer d = c.getBuffer(flags); // = 2 exports
        c.release(); // = 1 export
        maybeCheckExporting(obj);
        d.release(); // = no exports

        // Further releases are an error
        try {
            view.release(); // = -1 exports (oops)
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
    @Test
    public void testGetAfterRelease() {
        announce("getBuffer (after release)");

        // The test objects should have exactly one export
        view.release();

        // The view can be checked, but not always the obj
        maybeCheckNotExporting(obj);
        maybeCheckNotExporting(view);

        // Further gets via the released buffer are an error
        try {
            view.getBuffer(PyBUF.FULL_RO);
            fail("PyBuffer.getBuffer after final release not detected");
        } catch (Exception e) {
            // Detected *and* prevented?
            maybeCheckNotExporting(obj);
        }

        // And so are sliced gets
        try {
            view.getBufferSlice(PyBUF.FULL_RO, 0, 0);
            fail("PyBuffer.getBufferSlice after final release not detected");
        } catch (Exception e) {
            // Detected *and* prevented?
            maybeCheckNotExporting(obj);
        }

        // Even after some abuse, we can still get and release a buffer.
        PyBuffer b = obj.getBuffer(PyBUF.FULL_RO); // = 1 export
        maybeCheckExporting(obj);
        b.release(); // = 0 exports
        maybeCheckNotExporting(obj);
    }

    /**
     * Error if subject is a PyBuffer and is released, or is a real exporter that (we can tell) is
     * not actually exporting.
     *
     * @param subject
     */
    private void maybeCheckExporting(BufferProtocol subject) {
        if (subject instanceof TestableExporter) {
            assertTrue("exports not being counted", ((TestableExporter)subject).isExporting());
        } else if (subject instanceof PyBuffer) {
            assertFalse("exports not being counted (PyBuffer)", ((PyBuffer)subject).isReleased());
        } else if (subject instanceof PyByteArray) {
            // Size-changing access should fail
            try {
                ((PyByteArray)subject).bytearray_append(Py.One); // Appends one zero byte
                fail("bytearray_append with exports should fail");
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
    private void maybeCheckNotExporting(BufferProtocol subject) {
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

    /** Test method for {@link org.python.core.PyBuffer#getBufferSlice(int, int, int, int)}. */
    @Test
    public void testGetBufferSliceWithStride() {
        announce("getBuffer (slice & stride)");

        // Generate some slices from the material and the test view
        int N = ref.length;
        int M = (N + 4) / 4; // At least one and about N/4

        // For a range of start positions up to one beyond the end
        for (int start = 0; start <= N; start += M) {
            // For a range of lengths
            for (int length : sliceLengths) {

                if (length == 0) {
                    doTestGetBufferSliceWithStride(start, 0, 1);
                    doTestGetBufferSliceWithStride(start, 0, 2);

                } else if (length == 1 && start < N) {
                    doTestGetBufferSliceWithStride(start, 1, 1);
                    doTestGetBufferSliceWithStride(start, 1, 2);

                } else if (start < N) {

                    // And for a range of step sizes
                    for (int step : sliceSteps) {
                        // Check this is a feasible slice
                        if (start + (length - 1) * step < N) {
                            doTestGetBufferSliceWithStride(start, length, step);
                        }
                    }

                    // Now use all the step sizes negatively
                    for (int step : sliceSteps) {
                        // Check this is a feasible slice
                        if (start - (length - 1) * step >= 0) {
                            doTestGetBufferSliceWithStride(start, length, -step);
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
    private void doTestGetBufferSliceWithStride(int first, int count, int step) {

        // view is a view matching ref.bytes. Make a reference value for a further slice.
        TestSpec slicedSpec = new SlicedTestSpec(spec, spec.getItemsize(), first, count, step);

        if (verbosity > 1) {
            System.out.printf(
                    "  slice first=%4d, count=%4d, step=%4d -> underlying start=%4d, stride=%4d\n",
                    first, count, step, slicedSpec.getStart(), slicedSpec.getStride());
        }

        // Now compute that further slice using the library under test (not makePair)
        PyBuffer slicedView = view.getBufferSlice(spec.flags, first, count, step);
        byte[] slice = PyBufferTestSupport.bytesFromByteAt(slicedView);

        // Did we get the same as the reference material in the
        ByteBufferTestSupport.assertBytesEqual("slice incorrect", slicedSpec.ref.bytes, slice);
    }

    /** Test method for {@link org.python.core.PyBuffer#getNIOByteBuffer()}. */
    @Test
    public void testGetNIOByteBuffer() {
        announce("getNIOByteBuffer");
        int stride = spec.getStride();
        ByteBuffer bb = view.getNIOByteBuffer();
        ByteBufferTestSupport.assertBytesEqual("buffer does not match reference", ref.bytes, bb,
                stride);
        if (spec.readonly) {
            assertTrue("ByteBuffer should be read-only", bb.isReadOnly());
        } else {
            assertFalse("ByteBuffer should not be read-only", bb.isReadOnly());
        }

    }

    /** Test method for {@link org.python.core.PyBuffer#hasArray()}. */
    @Test
    public void testHasArray() {
        announce("hasArray");
        if (spec.hasArray) {
            assertTrue("a backing array was expected", view.hasArray());
        } else {
            assertFalse("no backing array was expected", view.hasArray());
        }
    }

    /** Test method for {@link org.python.core.PyBuffer#getBuf()}. */
    @Test
    @SuppressWarnings("deprecation")
    public void testGetBuf() {
        announce("getBuf");
        if (spec.hasArray) {
            int stride = spec.getStride();
            PyBuffer.Pointer bp = view.getBuf();
            assertBytesEqual("buffer does not match reference", ref.bytes, bp, stride);
        }
    }

    /** Test method for {@link org.python.core.PyBuffer#getPointer(int)}. */
    @Test
    @SuppressWarnings("deprecation")
    public void testGetPointer() {
        announce("getPointer");
        if (spec.hasArray) {
            int itemsize = spec.getItemsize();
            byte[] exp = new byte[itemsize], bytes = ref.bytes;

            // Try to get a pointer to an item at each byte location in the buffer
            for (int i = 0; i <= ref.length - itemsize; i++) {
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

    /** Test method for {@link org.python.core.PyBuffer#getPointer(int[])}. */
    @Test
    @SuppressWarnings("deprecation")
    public void testGetPointerNdim() {
        int[] index = new int[1];
        announce("getPointer(array)");
        if (spec.hasArray) {
            int n = ref.length, itemsize = view.getItemsize();
            byte[] exp = new byte[itemsize], bytes = ref.bytes;

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

    /** Test method for {@link org.python.core.PyBUF#getStrides()}. */
    @Test
    public void testGetStrides() {
        announce("getStrides");
        for (int flags : spec.validFlags) {
            PyBuffer view = this.view.getBuffer(flags);
            // Strides array irrespective of the client flags ... (different from CPython)
            int[] strides = view.getStrides();
            assertNotNull("strides[] should always be provided", strides);
            // The strides must have the expected value if length >1
            if (ref.bytes.length > 1) {
                assertIntsEqual("unexpected strides", spec.strides, strides);
            }
        }
    }

    /** Test method for {@link org.python.core.PyBUF#getSuboffsets()}. */
    @Test
    public void testGetSuboffsets() {
        announce("getSuboffsets");
        // Null for all test material
        assertNull(view.getSuboffsets());

    }

    /** Test method for {@link org.python.core.PyBUF#isContiguous(char)}. */
    @Test
    public void testIsContiguous() {
        announce("isContiguous");
        // All test material is 1-dimensional so it's fairly simple and same for all orders
        int ndim = spec.shape[0], stride = spec.getStride(), itemsize = spec.getItemsize();
        boolean contig = ndim < 2 || stride == itemsize;
        for (String orderMsg : validOrders) {
            char order = orderMsg.charAt(0);
            assertEquals(orderMsg, view.isContiguous(order), contig);
        }
    }

    private static final String[] validOrders = {"C-contiguous test fail",
            "F-contiguous test fail", "Any-contiguous test fail"};

    /** Test method for {@link org.python.core.PyBuffer#getFormat()}. */
    @Test
    public void testGetFormat() {
        announce("getFormat");
        TestSpec spec = this.spec;

        for (int flags : spec.validFlags) {
            PyBuffer view = this.view.getBuffer(flags);
            // Format given irrespective of the client flags ... (different from CPython)
            assertNotNull("format should always be provided", view.getFormat());
            assertEquals("B", view.getFormat());
            // And, we can ask for it explicitly ...
            view = this.view.getBuffer(flags | PyBUF.FORMAT);
            assertEquals("B", view.getFormat());
        }
    }

    /** Test method for {@link org.python.core.PyBUF#getItemsize()}. */
    @Test
    public void testGetItemsize() {
        announce("getItemsize");
        // Unity for all test material
        assertEquals(1, view.getItemsize());
    }

    /** Test method for {@link org.python.core.PyBuffer#toString()}. */
    @Test
    public void testToString() {
        announce("toString");
        String r = view.toString();
        assertEquals("buffer does not match reference", ref.string, r);
    }

    /**
     * Custom assert method comparing the bytes at a {@link PyBuffer.Pointer} to those in a byte
     * array, when that <code>Pointer</code> is obtained from a contiguous <code>PyBuffer</code>.
     * Let <code>bp[i]</code> denote <code>bp.storage[bp.offset+i]</code>, by analogy with a C
     * pointer. It is required that <code>bp[k] == expected[k]</code>, for every index in
     * <code>expected</code>. If not, a <code>fail()</code> is declared.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param bp result to test
     */
    @SuppressWarnings("deprecation")
    private static void assertBytesEqual(String message, byte[] expected, PyBuffer.Pointer bp) {
        assertBytesEqual(message, expected, bp, 1);
    }

    /**
     * Custom assert method comparing the bytes at a {@link PyBuffer.Pointer} to those in a byte
     * array, when that <code>Pointer</code> is obtained from a striding <code>PyBuffer</code>. Let
     * <code>bp[i]</code> denote <code>bp.storage[bp.offset+i]</code>, by analogy with a C pointer.
     * It is required that <code>bp[k*stride] == expected[k]</code>, for every index <code>k</code>
     * in <code>expected</code>. If not, a <code>fail()</code> is declared.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param bp result to test
     * @param stride in the <code>bp.storage</code> array
     */
    @SuppressWarnings("deprecation")
    private static void assertBytesEqual(String message, byte[] expected, PyBuffer.Pointer bp,
            int stride) {
        ByteBufferTestSupport.assertBytesEqual(message, expected, 0, expected.length, bp.storage,
                bp.offset, stride);
    }

    /*
     * --------------------------------------------------------------------------------------------
     * A series of custom exporters to permit testing abstracted from the Jython interpreter. These
     * use the implementation classes in org.python.core.buffer in ways very similar to the
     * implementations of bytearray and str.
     * --------------------------------------------------------------------------------------------
     */
    /**
     * A class to act as an exporter that uses the SimpleBuffer. The exporter exports a new PyBuffer
     * object to each consumer (although each references the same internal storage) and it does not
     * track their fate. You are most likely to use this approach with an exporting object that is
     * immutable (or at least fixed in size).
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
            return new SimpleBuffer(flags, this, storage);
        }

    }

    /** A factory for SimpleBuffer objects used in genTestSpects and some tests. */
    private static class SimpleExporterFactory extends ReadonlyExporterFactory {

        @Override
        public BufferProtocol make(ByteMaterial m) {
            return new SimpleExporter(m.getBytes());
        }

    }

    /**
     * Base class of certain exporters that permit testing abstracted from the Jython interpreter.
     */
    static abstract class TestableExporter implements BufferProtocol {

        protected Reference<BaseBuffer> export;

        /**
         * Try to re-use existing exported buffer, or return null if can't: modelled after the
         * buffer re-use strategy in {@link PyByteArray}.
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
                pybuf = new SimpleStringBuffer(flags, this, storage);
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
    private static class SimpleWritableExporter extends TestableExporter {

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
                pybuf = new SimpleWritableBuffer(flags, this, storage) {

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

    /** A class to act as an exporter that uses the RollYourOwnArrayBuffer class. */
    private static class RollYourOwnExporter extends TestableExporter {

        protected byte[] storage;

        public RollYourOwnExporter(byte[] storage) {
            this.storage = storage;
        }

        @Override
        public PyBuffer getBuffer(int flags) {
            // If we have already exported a buffer it may still be available for re-use
            BaseBuffer pybuf = getExistingBuffer(flags);
            if (pybuf == null) {
                // No existing export we can re-use
                pybuf = new RollYourOwnArrayBuffer(flags, this, storage);
                // Hold a reference for possible re-use
                export = new WeakReference<BaseBuffer>(pybuf);
            }
            return pybuf;
        }

    }

    /**
     * Minimal extension of BaseBuffer in order to test the default implementations there. They're
     * slow, so mostly we override them in the implementations BaseArrayBuffer and BaseNIOBuffer,
     * but they still have to be correct. The class represents a one-dimensional, strided array of
     * bytes, so it can represent a slice of itself.
     */
    private static class RollYourOwnArrayBuffer extends BaseBuffer {

        final static int FEATURES = PyBUF.WRITABLE | PyBUF.AS_ARRAY;

        final byte[] storage;
        final PyBuffer root;

        /**
         * Create a buffer view of the entire array.
         *
         * @param flags consumer requirements
         * @param obj exporting object (or <code>null</code>)
         * @param storage byte array exported in its entirety
         */
        public RollYourOwnArrayBuffer(int flags, BufferProtocol obj, byte[] storage) {
            this(flags, null /* =this */, obj, storage, 0, storage.length, 1);
        }

        /**
         * Construct a slice of a one-dimensional byte array.
         *
         * @param flags consumer requirements
         * @param root on which release must be called when this is released
         * @param obj exporting object (or <code>null</code>)
         * @param storage raw byte array containing exported data
         * @param index0 index into storage of item[0]
         * @param count number of items in the slice
         * @param stride in between successive elements of the new PyBuffer
         * @throws PyException (BufferError) when expectations do not correspond with the type
         */
        public RollYourOwnArrayBuffer(int flags, PyBuffer root, BufferProtocol obj, byte[] storage,
                int index0, int count, int stride) throws IndexOutOfBoundsException,
                NullPointerException, PyException {
            // Client will need to navigate using shape and strides if this is a slice
            super(FEATURES | ((index0 == 0 && stride == 1) ? 0 : STRIDES), //
                    index0, new int[] {count}, new int[] {stride});
            this.storage = storage;
            // Check the potential index range
            if (count > 0) {
                int end = index0 + (count - 1) * stride;
                final int END = storage.length - 1;
                if (index0 < 0 || index0 > END || end < 0 || end > END) {
                    throw new IndexOutOfBoundsException();
                }
            }
            // Check client is compatible
            checkRequestFlags(flags);
            // Get a lease on the root PyBuffer (read-only). Last in case a check above fails.
            if (root == null) {
                this.root = this;
                this.obj = obj;
            } else {
                this.root = root.getBuffer(FULL_RO);
                this.obj = root.getObj();
            }
        }

        @Override
        protected PyBuffer getRoot() {
            return root;
        }

        @Override
        public PyBuffer getBufferSlice(int flags, int start, int length, int stride) {
            int newStart = index0 + start * strides[0];
            int newStride = strides[0] * stride;
            return new RollYourOwnArrayBuffer(flags, root, null, storage, newStart, length,
                    newStride);
        }

        @Override
        public ByteBuffer getNIOByteBufferImpl() {
            return ByteBuffer.wrap(storage);
        }

        @Override
        protected byte byteAtImpl(int byteIndex) {
            return storage[byteIndex];
        }

        @Override
        protected void storeAtImpl(byte value, int byteIndex) throws IndexOutOfBoundsException,
                PyException {
            storage[byteIndex] = value;
        }
    }
}
