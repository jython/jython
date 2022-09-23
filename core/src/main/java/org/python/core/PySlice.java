// Copyright (c)2022 Jython Developers.
// Copyright (c) Corporation for National Research Initiatives
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

import org.python.core.Exposed.Member;
import org.python.core.PyType.Flag;

/**
 * The Python {@code slice} object.
 */
public class PySlice extends AbstractPyObject {

    /** The type of Python object this class implements. */
    static final PyType TYPE = PyType.fromSpec( //
            new PyType.Spec("slice", MethodHandles.lookup()).flagNot(Flag.BASETYPE));

    @Member
    final private Object start;

    @Member
    final private Object stop;

    @Member
    final private Object step;

    /**
     * Create a Python {@code slice} from {@code object} arguments.
     *
     * @param start index or {@code null} (for {@code None}).
     * @param stop index or {@code null} (for {@code None}).
     * @param step or {@code null} (for {@code None}).
     */
    public PySlice(Object start, Object stop, Object step) {
        super(TYPE);
        this.start = start != null ? start : Py.None;
        this.stop = stop != null ? stop : Py.None;
        this.step = step != null ? step : Py.None;
    }

    /**
     * Create a Python {@code slice} from two {@code object} arguments.
     * The step is implicitly {@code None}.
     *
     * @param start index or {@code null} (for {@code None}).
     * @param stop index or {@code null} (for {@code None}).
     */
    public PySlice(Object start, Object stop) { this(start, stop, null); }

    /**
     * Create a Python {@code slice} from Java {@code int} arguments.
     *
     * @param start index of first item in slice.
     * @param stop index of first item <b>not</b> in slice.
     */
    // Compare CPython _PySlice_FromIndices in sliceobject.c
    public PySlice(int start, int stop) { this(start, stop, Py.None); }

    // @formatter:off
    /*
    @ExposedNew
    static PyObject slice_new(PyNewWrapper new_, boolean init, PyType subtype, PyObject[] args,
                              String[] keywords) {
        if (args.length == 0) {
            throw new TypeError("slice expected at least 1 arguments, got " + args.length);
        } else if (args.length > 3) {
            throw new TypeError("slice expected at most 3 arguments, got " + args.length);
        }
        ArgParser ap = new ArgParser("slice", args, keywords, "start", "stop", "step");
        PySlice slice = new PySlice();
        if (args.length == 1) {
            slice.stop = ap.getPyObject(0);
        } else if (args.length == 2) {
            slice.start = ap.getPyObject(0);
            slice.stop = ap.getPyObject(1);
        } else if (args.length == 3) {
            slice.start = ap.getPyObject(0);
            slice.stop = ap.getPyObject(1);
            slice.step = ap.getPyObject(2);
        }
        return slice;
    }
    */
    // @formatter:on

    @SuppressWarnings("unused")
    private Object __eq__(Object o) throws Throwable {
        return this == o ? true : compare(o, Comparison.EQ);
    }

    @SuppressWarnings("unused")
    private Object __ne__(Object o) throws Throwable {
        return this == o ? false : compare(o, Comparison.NE);
    }

    /*
     * @ExposedMethod(doc = BuiltinDocs.slice_indices_doc)
     */
    final Object indices(Object length) throws Throwable {
        Indices indices = new Indices(PyNumber.asSize(length, OverflowError::new));
        return Py.tuple(indices.start, indices.stop, indices.step);
    }

    /**
     * Calculate the actual indices of this slice in relation to a
     * sequence of length {@code length}, reporting the effective start,
     * stop, and step, and the number of elements in the slice.
     *
     * @param length of the sequence
     * @return an {@link Indices} from this slice and the length
     * @throws TypeError if any index has no {@code __index__}
     * @throws Throwable from implementation of {@code __index__}
     */
    // Compare CPython PySlice_GetIndicesEx in sliceobject.c
    Indices getIndices(int length) throws TypeError, Throwable { return new Indices(length); }

    @SuppressWarnings("unused")
    private Object __repr__() { return String.format("slice(%s, %s, %s)", start, stop, step); }

    /*
     * @ExposedMethod
     */
    final Object __reduce__() { return Py.tuple(TYPE, Py.tuple(start, stop, step)); }

    /**
     * An object that presents the {@code start}, {@code stop} and
     * {@code step} data members from this {@code slice} object as Java
     * {@code int}s in an immutable data object, adjusted to a specific
     * length of a notional source sequence (see
     * {@link Indices#Indices(int)}).
     * <p>
     * End-relative addressing (as in {@code a[-3:-1]}) and {@code None}
     * indices (as in {@code a[:]}) have been translated in construction
     * to absolute indices a client may use without further range
     * checks.
     */
    // Compare CPython PySlice_GetIndicesEx in sliceobject.c
    public class Indices {
        private static final int MIN = Integer.MIN_VALUE;
        private static final int MAX = Integer.MAX_VALUE;
        /**
         * Absolute index in the source sequence of the first element to be
         * taken by the slice. If {@link #slicelength}{@code  != 0}, this
         * index lies within the bounds of the sequence.
         */
        public final int start;
        /**
         * Absolute index relative to the source sequence that is the image
         * of {@link PySlice#stop}. Dealing correctly with a step size other
         * than one is difficult. Clients should normally choose
         * {@link #slicelength}, to decide how many elements to take from
         * the sequence, rather than use {@code stop} to decide when to
         * stop.
         */
        public final int stop;
        /**
         * The index step to make when selecting elements from the source
         * sequence. Never zero.
         */
        public final int step;
        /**
         * The number of elements to select from the source sequence, and
         * therefore the length of the slice to be generated.
         */
        public final int slicelength;

        /**
         * Extract the {@code start}, {@code stop} and {@code step} data
         * members from the parent {@code slice} as Java {@code int}s in an
         * instance of {@code Indices}, then adjust {@code start} and
         * {@code stop} assuming they apply to a sequence of the specified
         * {@code length}. Store in {@code slicelength} the number of
         * elements the parent slice will take from that sequence.
         * <p>
         * Out of bounds indices are clipped in a manner consistent with the
         * handling of normal slices. The idiom:<pre>
         * Indices x = slice.new Indices(a.length);
         * for (int k=0; k&lt;x.slicelength; k++) {
         *     f(a[x.start + k*x.step]);
         * }
         * </pre> will access only in in-range elements.
         * <p>
         * <b>Detail:</b> Before adjusting to the specific sequence length,
         * the following occurs. Extract the {@code start}, {@code stop} and
         * {@code step} data members from the parent {@code slice} and
         * convert them to Java {@code int}s using their {@code __index__}
         * methods, or mapping {@code None} to conventional values. Silently
         * reduce values larger than {@code Integer.MAX_VALUE} to {@code
         Integer.MAX_VALUE}. Silently boost {@code start} and {@code stop}
         * values less than {@code Integer.MIN_VALUE} to {@code
         Integer.MIN_VALUE}. And silently boost {@code step} values less
         * than {@code -Integer.MAX_VALUE} to {@code -Integer.MAX_VALUE}.
         *
         * @param length of the target sequence
         * @throws TypeError if any index not {@code None} has no
         *     {@code __index__}
         * @throws ValueError if {@code step==0}
         * @throws Throwable from the implementation of {@code __index__}
         */
        // Compare CPython PySlice_Unpack in sliceobject.c
        // Compare CPython PySlice_AdjustIndices in sliceobject.c
        public Indices(int length) throws TypeError, ValueError, Throwable {

            // Counterparts while we think about final values.
            int start0, stop0, step0;

            // Bound and validate the step.
            step0 = PyNumber.sliceIndex(PySlice.this.step, 1);
            if (step0 == 0)
                throw new ValueError("slice step cannot be zero");
            /*
             * Here step0 might be MIN = -MAX-1; in this case we replace it with
             * -MAX. This doesn't affect the semantics, and it guards against
             * later undefined behaviour resulting from code that does
             * "step = -step" as part of a slice reversal.
             */
            step = Math.max(step0, -MAX);

            if (step > 0) {
                // The start, stop while ignoring the sequence length.
                start0 = PyNumber.sliceIndex(PySlice.this.start, 0);
                stop0 = PyNumber.sliceIndex(PySlice.this.stop, MAX);

                // Now adjust to the actual sequence length

                if (start0 < 0)
                    start = Math.max(start0 + length, 0);
                else
                    start = Math.min(start0, length);

                if (stop0 < 0)
                    stop = Math.max(stop0 + length, start);
                else
                    stop = Math.min(Math.max(stop0, start), length);

                assert stop >= start;
                slicelength = (stop - start + step - 1) / step;

            } else {
                // The start, stop while ignoring the sequence length.
                start0 = PyNumber.sliceIndex(PySlice.this.start, MAX);
                stop0 = PyNumber.sliceIndex(PySlice.this.stop, MIN);

                // Now adjust to the actual sequence length

                if (start0 < 0)
                    start = Math.max(start0 + length, -1);
                else
                    start = Math.min(start0, length - 1);

                if (stop0 < 0)
                    stop = Math.max(stop0 + length, -1);
                else
                    stop = Math.min(stop0, start);

                assert stop <= start;
                slicelength = (start - stop - step - 1) / (-step);
            }
        }

        @Override
        public String toString() {
            return String.format("[%d:%d:%d] len= %d", start, stop, step, slicelength);
        }
    }

    // Plumbing -------------------------------------------------------

    /**
     * Invoke the comparison specified (supports {@code __eq__} and
     * {@code __ne__}).
     *
     * @param o must be a slice or return {@code NotImplemented}
     * @param op {@link Comparison#EQ} or {@link Comparison#NE}
     * @return result of comparison or {@code NotImplemented}
     * @throws Throwable from element comparison
     */
    private Object compare(Object o, Comparison op) throws Throwable {
        if (TYPE.checkExact(o)) {
            // Compare the slices as if they were tuples
            PySlice s = (PySlice)o;
            return Abstract.richCompare(Py.tuple(start, stop, step),
                    Py.tuple(s.start, s.stop, s.step), op);
        } else {
            return Py.NotImplemented;
        }
    }
}
