package org.python.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.python.core.ByteBufferTestSupport.ByteMaterial;

/**
 * Supporting test fixtures for testing {@link PyBuffer} implementations, this class provides means
 * to generate test specifications and organise them into a list. This object creates and holds
 * factories for the multiple examples of the several implementation types the PyBufferTest needs,
 * together with the configuration the factory and the test need.
 */
public class PyBufferTestSupport {

    /** Control amount of output while generating material. */
    protected int verbosity;

    /** Lengths we will use if we can when slicing view */
    private final int[] sliceLengths;

    /** Step sizes we will use if we can when slicing view */
    private final int[] sliceSteps;

    /** List of test data configurations. */
    private List<TestSpec> testSpecList = new LinkedList<TestSpec>();

    /**
     * Create an instance, and choose the number an variety of tests that each call to
     * {@link #generate(BufferProtocol, ByteMaterial, boolean)} will produce.
     *
     * @param sliceLengths what length of slices to try to make from each original
     * @param sliceSteps step sizes (strides) to try to use
     */
    PyBufferTestSupport(int[] sliceLengths, int[] sliceSteps) {
        this(0, sliceLengths, sliceSteps);
    }

    /**
     * Create an instance, and choose the number an variety of tests that each call to
     * {@link #generate(BufferProtocol, ByteMaterial, boolean)} will produce.
     *
     * @param verbosity how much noise to make when generating test data
     * @param sliceLengths what length of slices to try to make from each original
     * @param sliceSteps step sizes (strides) to try to use
     */
    PyBufferTestSupport(int verbosity, int[] sliceLengths, int[] sliceSteps) {
        this.verbosity = verbosity;
        this.sliceLengths = sliceLengths;
        this.sliceSteps = sliceSteps;
    }

    /**
     * Add to the test queue a series of test specifications for a particular type of exporter and
     * byte material, in various sliced versions. The first argument provides a factory able to
     * construct a test object bearing the {@link BufferProtocol} interface, from the
     * {@link ByteMaterial} also supplied. The first test specification queued is based directly on
     * such construction. Construction takes place when {@link TestSpec#make()} is called during the
     * test constructor.
     * <p>
     * The method goes on to create a series of specifications that when invoked in test
     * initialisation will provide sliced views.
     * <p>
     * When the test runs, it will be given one test specification. Either:
     * <ol>
     * <li>the test is given the original root specification and makes a <code>PyBuffer</code> from
     * it, by a call to {@link TestSpec#make()}, whose implementation creates a test subject of
     * appropriate type, or</li>
     * <li>the test is given a derived sliced specification and makes a buffer from it, by a call to
     * {@link TestSpec#make()}, whose implementation slices a buffer provided by the original root
     * specification.</li>
     * </ol>
     * The slices are made with a variety of argument combinations, filtered down to those that make
     * sense for the size of the direct view. The reference value in the derived specification
     * {@link TestSpec#ref} is computed independently of the test subject, from the slice
     * specification and a the reference value in the root specification.
     *
     * @param original to specify a test and from which to generate other tests
     */
    void add(ExporterFactory factory, ByteMaterial material) {

        // Add test using the specification passed as arguments
        TestSpec original = new TestSpec(factory, material);
        queue(original);

        // Generate some slices from the material and this direct view
        int N = original.ref.length;
        int M = (N + 4) / 4;    // At least one and about N/4

        // For a range of start positions up to one beyond the end
        for (int start = 0; start <= N; start += M) {
            // For a range of lengths
            for (int length : sliceLengths) {

                if (length == 0) {
                    queue(original, start, 0, 1);
                    queue(original, start, 0, 2);

                } else if (length == 1 && start < N) {
                    queue(original, start, 1, 1);
                    queue(original, start, 1, 2);

                } else if (start < N) {

                    // And for a range of step sizes
                    for (int step : sliceSteps) {
                        // Check this is a feasible slice
                        if (start + (length - 1) * step < N) {
                            queue(original, start, length, step);
                        }
                    }

                    // Now use all the step sizes negatively
                    for (int step : sliceSteps) {
                        // Check this is a feasible slice
                        if (start - (length - 1) * step >= 0) {
                            queue(original, start, length, -step);
                        }
                    }
                }
            }
        }
    }

    /** Generate and queue one test of non-slice type (if getting a buffer succeeds). */
    private void queue(TestSpec spec) {
        if (verbosity > 2) {
            System.out.printf("queue non-slice: length=%d, readonly=%s\n", spec.ref.length,
                    spec.readonly);
        }
        testSpecList.add(spec);
    }

    /** Generate and queue one test of slice type (if getting a buffer succeeds). */
    private void queue(TestSpec original, int start, int length, int step) {
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
            TestSpec spec = new SlicedTestSpec(original, 1, start, length, step);
            testSpecList.add(spec);
        } catch (Exception e) {
            /*
             * We ignore this case if we fail, because we are not testing slice creation here, but
             * making slices to be tested as buffers. We'll test slice creation elsewhere.
             */
            if (verbosity > 2) {
                System.out.printf("*** SKIP %s\n", e);
            }
        }
    }

    /**
     * Return a copy of the generated list of test data in a form suitable for test construction
     * with a JUnit parameterised runner, which is as a collection of arrays of objects, where each
     * array becomes the arguments to the test constructor. (@see org.junit.runners.Parameterized)
     *
     * @return generated list of test data
     */
    List<TestSpec[]> getTestData() {
        List<TestSpec[]> r = new ArrayList<TestSpec[]>(testSpecList.size());
        for (TestSpec spec : testSpecList) {
            r.add(new TestSpec[] {spec});
        }
        return r;
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
     * Interface to a factory capable of making a {@link PyBuffer} exporter from
     * {@link ByteMaterial}.
     */
    interface ExporterFactory {

        /** Make fresh test object. */
        BufferProtocol make(ByteMaterial m);

        /** Whether the test object will be read-only. */
        boolean isReadonly();

        /** Whether the test object will be able to provide access as a byte array. */
        boolean hasArray();
    }

    abstract static class ReadonlyExporterFactory implements ExporterFactory {

        @Override
        public boolean isReadonly() {
            return true;
        }

        @Override
        public boolean hasArray() {
            return true;
        }

    };

    abstract static class WritableExporterFactory implements ExporterFactory {

        @Override
        public boolean isReadonly() {
            return false;
        }

        @Override
        public boolean hasArray() {
            return true;
        }

    };

    /**
     * Class holding reference data for a test and a factory method that will produce an object with
     * interface {@link BufferProtocol} for use in tests. The class has one principal method
     * {@link TestSpec#makePair()}, which must return an {@link ObjectAndView} where the view
     * element is equal to the reference {@link TestSpec#ref}. During a JUnit test, the test
     * constructor will be called with a particular instance of this class and will call
     * <code>makePair()</code> one or more times to get fresh test material.
     */
    static class TestSpec {

        /** Factory for test objects. */
        final ExporterFactory factory;
        /** The value of the associated test object. */
        final ByteMaterial ref;
        /** The associated <code>PyBuffer</code> should be read-only. */
        final boolean readonly;
        /** The associated <code>PyBuffer</code> should be accessible as a JVM array. */
        final boolean hasArray;
        /** Parent TestSpec, when this is a derived one, or null if it is an original. */
        final TestSpec parent;
        /** The value of shape array that the view should have that matches {@link #ref}. */
        final int[] shape;
        /** The value of strides array that the view should have that matches {@link #ref}. */
        final int[] strides;

        /** Either {@link PyBUF#FULL_RO} or {@link PyBUF#FULL} according to {@link #readonly}. */
        final int flags;

        /** Allowable basic flag combinations, such as {@link PyBUF#STRIDES}. */
        final int[] validFlags;

        /** Allowable additional flag combinations, such as {@link PyBUF#FORMAT} */
        final int[] validTassles;

        /**
         * A one-dimensional exporter should be able to give us a buffer for all these flag types.
         */
        static final int[] simpleFlags = {PyBUF.SIMPLE, PyBUF.ND, PyBUF.STRIDES, PyBUF.INDIRECT,
                PyBUF.FULL_RO};

        /** To {@link #simpleFlags} we can add any of these */
        static final int[] simpleTassles = {0, PyBUF.FORMAT, PyBUF.C_CONTIGUOUS,
                PyBUF.F_CONTIGUOUS, PyBUF.ANY_CONTIGUOUS};

        /**
         * Construct a specification for a 1D contiguous byte-array based on the exporter factory
         * and reference data supplied.
         *
         * @param factory makes exporters of the particular type
         * @param ref the fill those exporters should have
         */
        TestSpec(ExporterFactory factory, ByteMaterial ref) {
            this(null, factory, ref, new int[] {ref.length}, new int[] {1}, simpleFlags,
                    simpleTassles);
        }

        /**
         * Construct a specification for a 1D contiguous item-array based on the exporter factory,
         * shape data and reference data supplied.
         *
         * @param parent of this test specification
         * @param ref the fill those exporters should have (also determines the item size)
         * @param shape array defining number and size of dimensions (as {@link PyBUF#getShape()}
         * @param strides array defining addressing polynomial (as {@link PyBUF#getStrides()})
         * @param validFlags allowable basic flag combinations usable with this specification
         * @param validTassles allowable additional flag combinations
         */
        protected TestSpec(TestSpec parent, ByteMaterial ref, int[] shape, int[] strides,
                int[] validFlags, int[] validTassles) {
            this(parent, parent.getOriginal().factory, ref, shape, strides, validFlags,
                    validTassles);
        }

        /**
         * Construct a specification for a 1D contiguous item-array based on the exporter factory,
         * shape data and reference data supplied.
         *
         * @param parent of this test specification
         * @param factory makes exporters of the particular type, given <code>ref</code>
         * @param ref the fill those exporters should have (also determines the item size)
         * @param shape array defining number and size of dimensions (as {@link PyBUF#getShape()}
         * @param strides array defining addressing polynomial (as {@link PyBUF#getStrides()})
         * @param validFlags allowable basic flag combinations usable with this specification
         * @param validTassles allowable additional flag combinations
         */
        protected TestSpec(TestSpec parent, ExporterFactory factory, ByteMaterial ref, int[] shape,
                int[] strides, int[] validFlags, int[] validTassles) {
            this.parent = parent;
            this.factory = factory;
            this.readonly = factory.isReadonly();
            this.hasArray = factory.hasArray();
            this.flags = (readonly ? PyBUF.FULL_RO : PyBUF.FULL) | (hasArray ? PyBUF.AS_ARRAY : 0);
            this.ref = ref;
            this.shape = shape;
            this.strides = strides;
            this.validFlags = validFlags;
            this.validTassles = validTassles;
        }

        /** Return the parent of this specification (or null when it is an original). */
        final TestSpec getParent() {
            return parent;
        }

        /** This is an original specification (parent is null). */
        final boolean isOriginal() {
            return parent == null;
        }

        /** Return the original of this specification (ancestor with no parent). */
        final TestSpec getOriginal() {
            TestSpec p = this;
            while (!p.isOriginal()) {
                p = p.getParent();
            }
            return p;
        }

        /** Return the item size. */
        int getItemsize() {
            return 1;
        }

        /** Return the stride that a buffer made from this specification should have. */
        int getStride() {
            return strides[0];
        }

        /** Return the start index that a buffer made from this specification should have. */
        int getStart() {
            return 0;
        }

        /** Simple holder class for a buffer exporter object and a related buffer. */
        static class ObjectAndView {

            final BufferProtocol obj;
            final PyBuffer view;

            ObjectAndView(BufferProtocol obj, PyBuffer view) {
                this.obj = obj;
                this.view = view;
            }
        }

        /**
         * Make the test object which must implement <code>BufferProtocol</code> and its
         * <code>PyBuffer</code> view. The value as a byte array must equal {@link #ref}.
         */
        public ObjectAndView makePair() {
            BufferProtocol obj = factory.make(ref);
            PyBuffer view = obj.getBuffer(flags);
            return new ObjectAndView(obj, view);
        }

        @SuppressWarnings("deprecation")
        @Override
        public String toString() {

            ObjectAndView pair = makePair();
            BufferProtocol obj = pair.obj;
            PyBuffer view = pair.view;

            StringBuilder sb = new StringBuilder(100);
            sb.append(obj.getClass().getSimpleName()).append('[');

            int offset, stride = getStride();

            if (view.hasArray()) {
                offset = view.getBuf().offset;
            } else {
                offset = view.getNIOByteBuffer().position();
            }

            if (offset > 0) {
                sb.append(offset);
            }

            String plus = offset == 0 ? "" : "+";

            if (stride == 1) {
                sb.append(plus).append("k]");
            } else if (stride == -1) {
                sb.append("-k]");
            } else if (stride < 0) {
                sb.append("-").append(-stride).append("*k]");
            } else {
                /* stride>1 or ==0) */sb.append(plus).append(stride).append("*k]");
            }

            while (sb.length() < 30) {
                sb.append(' ');
            }
            sb.append(view.isReadonly()?"R ":"W ");
            sb.append("ref = ").append(ref.toString());

            return sb.toString();
        }
    }

    /**
     * A test specification that is derived from a parent test specification, but will construct
     * views sliced a particular way. In order to construct a test object, the factory of the parent
     * is used, so that objects returned from here have the same type and root buffer value as the
     * parent. However, {@link SlicedTestSpec#makePair()} returns a sliced view with the base
     * exporter, and the reference material here is sliced correspondingly.
     */
    static class SlicedTestSpec extends TestSpec {

        /** Number of consecutive bytes forming one item */
        final int itemsize;
        /** Index in the parent object of item 0 of this slice */
        final int first;
        /** The number of items that make up the slice. */
        final int count;
        /** The item-index distance in the parent from one item to the next of this slice. */
        final int step;

        /** Byte-index in the original byte-array object of byte 0 of item 0 of the slice */
        final int start;

        /**
         * A one-dimensional exporter with stride!=1 is restricted to give us a buffer only for
         * these flag types.
         */
        static final int[] strided1DFlags = {PyBUF.STRIDES, PyBUF.INDIRECT, PyBUF.FULL_RO};

        /** To {@link #strided1DFlags} we can add any of these */
        static final int[] strided1DTassles = {0, PyBUF.FORMAT};

        /**
         * Construct a test specification based on a parent, but yielding objects and reference
         * material whose values are related to those of the parent according to the slice
         * specification.
         *
         * @param parent specification of byte buffer to slice
         * @param itemsize number of consecutive bytes forming one item
         * @param first byte-index in the parent of byte 0 of item 0 the result
         * @param count number of items in the slice
         * @param step byte-index increment in the parent between items
         */
        SlicedTestSpec(TestSpec parent, int itemsize, int first, int count, int step) {
            super(parent, parent.ref.slice(itemsize, first, count, step), new int[] {count},
                    new int[1], strided1DFlags, strided1DTassles);
            // It only seems to make sense for byte-array parent (or does all scale?)
            if (parent.getItemsize() != 1) {
                throw new IllegalArgumentException("Only byte-array parent supported");
            }
            this.itemsize = itemsize;
            // Write these down verbatim for subsequent call to getBufferSlice
            this.first = first;
            this.count = count;
            this.step = step;
            // But these must be calculated carefully
            this.start = parent.getStart() + first * parent.getStride();
            this.strides[0] = step * parent.getStride();
        }

        /**
         * {@inheritDoc}
         * <p>
         * The size given in construction of a <code>SlicedTestSpec</code>.
         */
        @Override
        int getItemsize() {
            return itemsize;
        }

        /**
         * {@inheritDoc}
         * <p>
         * The start given in construction of a <code>SlicedTestSpec</code> is a start byte index
         * specification, which could itself be striding on the underlying object's storage.
         */
        @Override
        int getStart() {
            return start;
        }

        /**
         * {@inheritDoc}
         * <p>
         * In <code>SlicedTestSpec</code> the returned pair are a new instance of the root object
         * (to be the original exporter) created by
         *
         * <pre>
         * pair = parent.makePair();
         * obj = pair.obj;
         * </pre>
         * and a <i>sliced</i> buffer view onto it created by
         *
         * <pre>
         * view = pair.view.getBufferSlice(flags, first, count, step);
         * </pre>
         * This view-slicing will apply recursively if the parent is a {@link SlicedTestSpec}, just
         * as the slicing of reference material was iterated in construction.
         */
        @Override
        public ObjectAndView makePair() {
            // Create a fresh test object and buffer view from the parent spec
            ObjectAndView pair = parent.makePair();
            // Make a sliced view and release the parent
            PyBuffer view = pair.view.getBufferSlice(flags, first, count, step);
            // Controlled release of the parent buffer since pair is local
            pair.view.release();
            return new ObjectAndView(pair.obj, view);
        }

    }
}
