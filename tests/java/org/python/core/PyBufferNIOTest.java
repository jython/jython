package org.python.core;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

import org.junit.runners.Parameterized.Parameters;
import org.python.core.ByteBufferTestSupport.ByteMaterial;
import org.python.core.PyBufferTestSupport.ExporterFactory;
import org.python.core.PyBufferTestSupport.TestSpec;
import org.python.core.PyBufferTestSupport.WritableExporterFactory;
import org.python.core.buffer.BaseBuffer;
import org.python.core.buffer.SimpleNIOBuffer;

public class PyBufferNIOTest extends PyBufferTest {

    public PyBufferNIOTest(TestSpec spec) {
        super(spec);
    }

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

        ExporterFactory rollYourOwnExporter = new WritableExporterFactory() {

            @Override
            public BufferProtocol make(ByteMaterial m) {
                return new RollYourOwnExporter(m.getBuffer());
            }

        };
        s.add(rollYourOwnExporter, byteMaterial);
        s.add(rollYourOwnExporter, emptyMaterial);

        // All combinations of heap/direct, writable and empty/small/large (I'm so thorough!)

        ExporterFactory readonlyHeapNIOExporter = new TestNIOExporterFactory(false, false);
        s.add(readonlyHeapNIOExporter, emptyMaterial);
        s.add(readonlyHeapNIOExporter, byteMaterial);
        s.add(readonlyHeapNIOExporter, longMaterial);

        ExporterFactory writableHeapNIOExporter = new TestNIOExporterFactory(true, false);
        s.add(writableHeapNIOExporter, emptyMaterial);
        s.add(writableHeapNIOExporter, byteMaterial);
        s.add(writableHeapNIOExporter, longMaterial);

        ExporterFactory readonlyDirectNIOExporter = new TestNIOExporterFactory(false, true);
        s.add(readonlyDirectNIOExporter, emptyMaterial);
        s.add(readonlyDirectNIOExporter, byteMaterial);
        s.add(readonlyDirectNIOExporter, longMaterial);

        ExporterFactory writableDirectNIOExporter = new TestNIOExporterFactory(true, true);
        s.add(writableDirectNIOExporter, emptyMaterial);
        s.add(writableDirectNIOExporter, byteMaterial);
        s.add(writableDirectNIOExporter, longMaterial);

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

    /*
     * --------------------------------------------------------------------------------------------
     * A series of custom exporters that use a java.nio.ByteBuffer to store and export their
     * implementation data.
     * --------------------------------------------------------------------------------------------
     */
    /**
     * A class to act as an exporter that uses the SimpleBuffer. The exporter shares a single
     * exported buffer between all consumers and needs to take any action immediately when that
     * buffer is finally released. You are most likely to use this approach with an exporting object
     * type that modifies its behaviour while there are active exports, but where it is worth
     * avoiding the cost of duplicate buffers. This is the case with PyByteArray, which prohibits
     * operations that would resize it, while there are outstanding exports.
     */
    private static class TestNIOExporter extends TestableExporter {

        protected ByteBuffer storage;

        /**
         * Construct a simple exporter from the bytes supplied.
         *
         * @param storage
         */
        public TestNIOExporter(ByteBuffer storage) {
            this.storage = storage;
        }

        @Override
        public PyBuffer getBuffer(int flags) {
            // If we have already exported a buffer it may still be available for re-use
            BaseBuffer pybuf = getExistingBuffer(flags);
            if (pybuf == null) {
                // No existing export we can re-use
                pybuf = new SimpleNIOBuffer(flags, this, storage) {

                    @Override
                    protected void releaseAction() {
                        export = null; // Final release really is final (not reusable)
                    }
                };

                // Hold a reference for possible re-use
                export = new WeakReference<BaseBuffer>(pybuf);
            }
            return pybuf;
        }

    }

    /**
     * A factory for exporting objects to be used in the tests. These objects use a
     * <code>ByteBuffer</code> for their exported representation, and the factory is programmed on
     * creation to whether these buffers should be writable or direct.
     */
    static class TestNIOExporterFactory implements ExporterFactory {

        final boolean writable;
        final boolean isDirect;

        TestNIOExporterFactory(boolean writable, boolean isDirect) {
            this.writable = writable;
            this.isDirect = isDirect;
        }

        public boolean isWritable() {
            return writable;
        }

        public boolean isDirect() {
            return isDirect;
        }

        @Override
        public BufferProtocol make(ByteMaterial m) {
            ByteBuffer bb = m.getBuffer();
            if (isDirect) {
                // Replace bb with a direct buffer containing the same bytes
                ByteBuffer direct = ByteBuffer.allocateDirect(bb.capacity());
                direct.put(bb).flip();
                bb = direct;
            }
            if (!writable) {
                bb = bb.asReadOnlyBuffer();
            }
            return new TestNIOExporter(bb);
        }

        @Override
        public boolean isReadonly() {
            return !writable;
        }

        @Override
        public boolean hasArray() {
            return !isDirect && writable;
        }

    }

    /** A class to act as an exporter that uses the RollYourOwnNIOBuffer class. */
    private static class RollYourOwnExporter extends TestableExporter {

        protected ByteBuffer storage;

        public RollYourOwnExporter(ByteBuffer storage) {
            this.storage = storage;
        }

        @Override
        public PyBuffer getBuffer(int flags) {
            // If we have already exported a buffer it may still be available for re-use
            BaseBuffer pybuf = getExistingBuffer(flags);
            if (pybuf == null) {
                // No existing export we can re-use
                pybuf = new RollYourOwnNIOBuffer(flags, this, storage);
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
    private static class RollYourOwnNIOBuffer extends BaseBuffer {

        final static int FEATURES = PyBUF.WRITABLE | PyBUF.AS_ARRAY;

        final ByteBuffer storage;
        final PyBuffer root;

        /**
         * Create a buffer view of a given <code>ByteBuffer</code> in which the data is the
         * contiguous sequence of bytes from the position to the limit.
         *
         * @param flags consumer requirements
         * @param obj exporting object (or <code>null</code>)
         * @param storage buffer exported (from the position to the limit)
         */
        public RollYourOwnNIOBuffer(int flags, BufferProtocol obj, ByteBuffer storage) {
            this(flags, null /* =this */, obj, storage, storage.position(), storage.remaining(), 1);
        }

        /**
         * Construct a slice of a one-dimensional byte buffer.
         *
         * @param flags consumer requirements
         * @param obj exporting object (or <code>null</code>)
         * @param root on which release must be called when this is released
         * @param storage buffer containing exported data
         * @param index0 index into storage of item[0]
         * @param count number of items in the slice
         * @param stride in between successive elements of the new PyBuffer
         * @throws PyException (BufferError) when expectations do not correspond with the type
         */
        public RollYourOwnNIOBuffer(int flags, PyBuffer root, BufferProtocol obj,
                ByteBuffer storage, int index0, int count, int stride)
                throws IndexOutOfBoundsException, NullPointerException, PyException {
            // Client will need to navigate using shape and strides if this is a slice
            super(FEATURES | ((index0 == 0 && stride == 1) ? 0 : STRIDES), //
                    index0, new int[] {count}, new int[] {stride});
            this.storage = storage.duplicate();

            // Check the potential index range
            if (count > 0) {
                int end = index0 + (count - 1) * stride;
                final int END = storage.capacity() - 1;
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
        public PyBuffer getBufferSlice(int flags, int start, int count, int stride) {
            int newStart = index0 + start * strides[0];
            int newStride = strides[0] * stride;
            return new RollYourOwnNIOBuffer(flags, root, null, storage, newStart, count, newStride);
        }

        @Override
        public ByteBuffer getNIOByteBufferImpl() {
            return storage.duplicate();
        }

        @Override
        protected byte byteAtImpl(int byteIndex) {
            return storage.get(byteIndex);
        }

        @Override
        protected void storeAtImpl(byte value, int byteIndex) throws IndexOutOfBoundsException,
                PyException {
            storage.put(byteIndex, value);
        }
    }
}
