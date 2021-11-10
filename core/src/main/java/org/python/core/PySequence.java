// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.python.core.PyObjectUtil.NoConversion;
import org.python.core.PySlice.Indices;
import org.python.core.Slot.EmptyException;

/**
 * Abstract API for operations on sequence types, corresponding to
 * CPython methods defined in {@code abstract.h} and with names like:
 * {@code PySequence_*}.
 */
public class PySequence extends Abstract {

    private PySequence() {} // only static methods here

    /**
     * {@code len(o)} with Python semantics.
     *
     * @param o object to operate on
     * @return {@code len(o)}
     * @throws Throwable from invoked method implementations
     */
    // Compare CPython PyObject_Size in abstract.c
    static int size(Object o) throws Throwable {
        // Note that the slot is called op_len but this method, size.
        try {
            return (int)Operations.of(o).op_len.invokeExact(o);
        } catch (Slot.EmptyException e) {
            throw typeError(HAS_NO_LEN, o);
        }
    }

    /**
     * {@code o * count} with Python semantics.
     *
     * @param o object to operate on
     * @param count number of repetitions
     * @return {@code o*count}
     * @throws Throwable from invoked method implementations
     */
    // Compare CPython PySequence_Repeat in abstract.c
    public static Object repeat(Object o, int count) throws Throwable {
        // There is no equivalent slot to sq_repeat
        return PyNumber.multiply(o, count);
    }

    /**
     * {@code v + w} for sequences with Python semantics.
     *
     * @param v first object to concatenate
     * @param w second object to concatenate
     * @return {@code v + w}
     * @throws Throwable from invoked method implementations
     */
    // Compare CPython PySequence_Concat in abstract.c
    public static Object concat(Object v, Object w) throws Throwable {
        // There is no equivalent slot to sq_concat
        return PyNumber.add(v, w);
    }

    /**
     * {@code o[key]} with Python semantics, where {@code o} may be a
     * mapping or a sequence.
     *
     * @param o object to operate on
     * @param key index
     * @return {@code o[key]}
     * @throws TypeError when {@code o} does not allow subscripting
     * @throws Throwable from invoked method implementations
     */
    // Compare CPython PyObject_GetItem in abstract.c
    static Object getItem(Object o, Object key) throws Throwable {
        // Decisions are based on types of o and key
        try {
            Operations ops = Operations.of(o);
            return ops.op_getitem.invokeExact(o, key);
        } catch (EmptyException e) {
            throw typeError(NOT_SUBSCRIPTABLE, o);
        }
    }

    /**
     * {@code o[key] = value} with Python semantics, where {@code o} may
     * be a mapping or a sequence.
     *
     * @param o object to operate on
     * @param key index
     * @param value to put at index
     * @throws TypeError when {@code o} does not allow subscripting
     * @throws Throwable from invoked method implementations
     */
    // Compare CPython PyObject_SetItem in abstract.c
    static void setItem(Object o, Object key, Object value)
            throws Throwable {
        // Decisions are based on types of o and key
        Operations ops = Operations.of(o);
        try {
            ops.op_setitem.invokeExact(o, key, value);
            return;
        } catch (EmptyException e) {
            throw typeError(DOES_NOT_SUPPORT_ITEM, o, "assignment");
        }
    }

    /**
     * {@code del o[key]} with Python semantics, where {@code o} may be
     * a mapping or a sequence.
     *
     * @param o object to operate on
     * @param key index at which to delete element
     * @throws TypeError when {@code o} does not allow subscripting
     * @throws Throwable from invoked method implementations
     */
    // Compare CPython PyObject_DelItem in abstract.c
    static void delItem(Object o, Object key) throws Throwable {
        // Decisions are based on types of o and key
        Operations ops = Operations.of(o);
        try {
            ops.op_delitem.invokeExact(o, key);
            return;
        } catch (EmptyException e) {
            throw typeError(DOES_NOT_SUPPORT_ITEM, o, "deletion");
        }
    }

    // Convenience functions constructing errors ----------------------

    protected static final String HAS_NO_LEN =
            "object of type '%.200s' has no len()";
    private static final String NOT_SUBSCRIPTABLE =
            "'%.200s' object is not subscriptable";
    protected static final String DOES_NOT_SUPPORT_ITEM =
            "'%.200s' object does not support item %s";

    // Classes supporting implementations of sequence types -----------

    /**
     * In the implementation of sequence types, it is useful to be able
     * to create iterables and streams from their content. This
     * interface provides a standardised API. Several {@link Delegate}
     * implementations in the core also provide this interface.
     *
     * @param <T> the type of element returned by the iterators
     */
    static interface Of<T> extends Iterable<T>, Comparable<Of<T>> {

        /**
         * The length of this sequence.
         *
         * @return the length of this sequence
         */
        int length();

        /**
         * Get one element from the sequence.
         *
         * @param i index of element to get
         * @return value at {@code i}th position
         */
        T get(int i);

        // /** Set one element from the sequence (if mutable). */
        // void setItem(int i, Object v);

        /**
         * {@inheritDoc} The characteristics {@code SIZED} and
         * {@code SUBSIZED} are additionally reported.
         */
        @Override
        default Spliterator<T> spliterator() {
            return Spliterators.spliterator(iterator(), length(), 0);
        }

        /**
         * @return the elements of this sequence as a {@code Stream}
         */
        default Stream<T> asStream() {
            return StreamSupport.stream(spliterator(), false);
        }
    }

    /**
     * This is a helper class for implementations of sequence types. A
     * client sequence implementation may hold an instance of a
     * sub-class of {@code Delegate}, to which it delegates certain
     * operations. This sub-class could be an inner class with access to
     * private members and methods of the client.
     * <p>
     * {@code Delegate} declares abstract or overridable methods
     * representing elementary operations on the client sequence (to
     * get, set or delete an element or slice, or to enquire its length
     * or type). It offers methods based on these that are usable
     * implementations of the Python special methods
     * {@code __getitem__}, {@code __setitem__}, {@code __delitem__},
     * {@code __add__} and {@code __mul__}. It provides the boiler-plate
     * that tends to be the same from one Python type to another &ndash;
     * recognition that an index is a slice, end-relative addressing,
     * index type and range checks, and the raising of index-related
     * Python exceptions. (For examples of this similarity, compare the
     * CPython implementation of {@code list_subscript} with that of
     * {@code bytes_subscript} or any other {@code *_subscript} method.)
     * <p>
     * The client must override abstract methods declared here in the
     * sub-class it defines, to specialise the behaviour of the
     * delegate. A sub-class supporting a mutable sequence type must
     * additionally override {@link #setImpl(int)},
     * {@link #setImpl(Indices)}, {@link #delItem(int)} and
     * {@link #delSlice(Indices)}.
     *
     * @param <E> the element type, and return type of
     *     {@link #getItem(int)} etc..
     * @param <S> the slice type, and return type of
     *     {@link #getSlice(Indices)} etc..
     */
    /*
     * This has been adapted from Jython 2 SequenceIndexDelegate and
     * documented.
     */
    static abstract class Delegate<E, S> {

        /**
         * Returns the length of the client sequence from the
         * perspective of indexing and slicing operations.
         *
         * @return the length of the client sequence
         */
        public abstract int length();

        /**
         * Provide the type of client sequence, primarily for use in
         * error messages e.g. "&lt;TYPE> index out of bounds".
         *
         * @implNote This can simply return a constant characteristic of
         *     the the implementing class, the Python type implements or
         *     supports. E.g the adaptor for a Java String returns
         *     {@code PyUnicode.TYPE} which is {@code str}.
         *
         * @return the type of client sequence
         */
        public abstract PyType getType();

        /**
         * Return the name of the Python type of the client sequence.
         * This is used in exception messages generated here. By default
         * this is {@code getType().getName()}, which is normally
         * correct, but Python {@code str} likes to call itself
         * "string", exceptionally.
         *
         * @return the name of Python type being served
         */
        public String getTypeName() { return getType().getName(); }

        /**
         * Inner implementation of {@code __getitem__}, called by
         * {@link #__getitem__(Object)} when its argument is an integer.
         * The argument is the equivalent {@code int}, adjusted and
         * checked by {@link #adjustGet(int)}.
         *
         * @param i index of item to return
         * @return the element from the client sequence
         * @throws Throwable from errors other than indexing
         */
        public abstract E getItem(int i) throws Throwable;

        /**
         * Inner implementation of {@code __getitem__}, called by
         * {@link #__getitem__(Object)} when its argument is a
         * {@link PySlice}. The argument is the return from
         * {@link PySlice#getIndices(int)}, which is guaranteed to be
         * range-compatible with the sequence length {@link #length()}.
         *
         * @param slice containing [start, stop, step, count] of the
         *     slice to return
         * @return the slice from the client sequence
         * @throws Throwable from errors other than indexing
         */
        public abstract S getSlice(PySlice.Indices slice)
                throws Throwable;

        /**
         * Inner implementation of {@code __setitem__}, called by
         * {@link #__setitem__(Object,Object)} when its argument is an
         * integer. The argument is the equivalent {@code int}, adjusted
         * and checked by {@link #adjustSet(int)}.
         * <p>
         * In mutable types, override this to assign a value to the
         * given element of the client sequence. The default
         * implementation (for immutable types) does nothing.
         *
         * @param i index of item to set
         * @throws Throwable from errors other than indexing
         */
        public void setItem(int i, Object value) throws Throwable {};

        /**
         * Inner implementation of {@code __setitem__}, called by
         * {@link #__setitem__(Object,Object)} when its argument is a
         * {@link PySlice}. The argument is the return from
         * {@link PySlice#getIndices(int)}, which is guaranteed to be
         * range-compatible with the sequence length {@link #length()}.
         * <p>
         * In mutable types, override this to assign a value to the
         * given slice of the client sequence. The default
         * implementation (for immutable types) does nothing.
         *
         * @param slice to assign in the client sequence
         * @param value to assign
         * @throws Throwable from errors other than indexing
         */
        public void setSlice(PySlice.Indices slice, Object value)
                throws Throwable {};

        /**
         * Inner implementation of {@code __delitem__}, called by
         * {@link #__setitem__(Object,Object)} when its argument is an
         * integer. The argument is the equivalent {@code int}, adjusted
         * and checked by {@link #adjustSet(int)}.
         * <p>
         * The default implementation deletes a slice {@code [i:i+1]}
         * using {@link #delSlice(Indices)}.
         *
         * @param i index of item to delete
         * @throws Throwable from errors other than indexing
         */
        public void delItem(int i) throws Throwable {
            PySlice s = new PySlice(i, i + 1);
            delSlice(s.new Indices(length()));
        }

        /**
         * Inner implementation of {@code __delitem__}, called by
         * {@link #__delitem__(Object)} when its argument is a
         * {@link PySlice}. The argument is the return from
         * {@link PySlice#getIndices(int)}, which is guaranteed to be
         * range-compatible with the sequence length {@link #length()}.
         * <p>
         * In mutable types, override this to delete the given slice of
         * the client sequence. The default implementation (for
         * immutable types) does nothing.
         *
         * @param slice containing [start, stop, step, count] of the
         *     slice to delete
         * @throws Throwable
         * @throws TypeError
         */
        public void delSlice(PySlice.Indices slice) throws Throwable {}

        /**
         * Inner implementation of {@code __add__} on the client
         * sequence, called by {@link #__add__(Object)}.
         *
         * @param ow the right operand
         * @return concatenation {@code self+ow}
         * @throws OutOfMemoryError when allocating the result fails.
         *     {@link #__add__(Object) __add__} will raise a Python
         *     {@code OverflowError}.
         * @throws NoConversion when the client does not support the
         *     type of {@code ow}. {@link #__add__(Object) __add__} will
         *     return a Python {@code NotImplemented}.
         * @throws Throwable from other causes in the implementation
         */
        abstract S add(Object ow)
                throws OutOfMemoryError, NoConversion, Throwable;

        /**
         * Inner implementation of {@code __radd__} on the client
         * sequence, called by {@link #__radd__(Object)}.
         *
         * @param ov the left operand
         * @return concatenation {@code ov+self}
         * @throws OutOfMemoryError when allocating the result fails.
         *     {@link #__radd__(Object) __radd__} will raise a Python
         *     {@code OverflowError}.
         * @throws NoConversion when the client does not support the
         *     type of {@code ov}. {@link #__radd__(Object) __radd__}
         *     will return a Python {@code NotImplemented}.
         * @throws Throwable from other causes in the implementation
         */
        abstract S radd(Object ov)
                throws OutOfMemoryError, NoConversion, Throwable;

        /**
         * Inner implementation of {@code __mul__} on the client
         * sequence, called by {@link #__mul__(Object)}.
         *
         * @param n the number of repetitions
         * @return repetition {@code self*n}
         * @throws OutOfMemoryError when allocating the result fails.
         *     {@link #__mul__(Object) __mul__} will raise a Python
         *     {@code OverflowError}.
         * @throws Throwable from other causes in the implementation
         */
        abstract S repeat(int n) throws OutOfMemoryError, Throwable;

        /**
         * Implementation of {@code __getitem__}. Get either an element
         * or a slice of the client sequence, after checks, by calling
         * either {@link #getItem(int)} or {@link #getSlice(Indices)}.
         *
         * @param item (or slice) to get from in the client
         * @return the element or slice
         * @throws ValueError if {@code slice.step==0}
         * @throws TypeError from bad slice index types
         * @throws Throwable from errors other than indexing
         */
        public Object __getitem__(Object item)
                throws TypeError, Throwable {
            if (PyNumber.indexCheck(item)) {
                int i = PyNumber.asSize(item, IndexError::new);
                return getItem(adjustGet(i));
            } else if (item instanceof PySlice) {
                Indices slice = ((PySlice)item).new Indices(length());
                return getSlice(slice);
            } else {
                throw Abstract.indexTypeError(this, item);
            }
        }

        /**
         * Implementation of {@code __setitem__}. Assign a value to
         * either an element or a slice of the client sequence, after
         * checks, by calling either {@link #setItem(int, Object)} or
         * {@link #setSlice(Indices, Object)}.
         *
         * @param item (or slice) to assign in the client
         * @param value to assign
         * @throws ValueError if {@code slice.step==0} or
         *     {@code slice.step!=1} (an "extended" slice) and
         *     {@code value} is the wrong length.
         * @throws TypeError from bad slice index types
         * @throws Throwable from errors other than indexing
         */
        public void __setitem__(Object item, Object value)
                throws TypeError, Throwable {
            if (PyNumber.indexCheck(item)) {
                int i = PyNumber.asSize(item, IndexError::new);
                setItem(adjustSet(i), value);
            } else if (item instanceof PySlice) {
                Indices slice = ((PySlice)item).new Indices(length());
                setSlice(slice, value);
            } else {
                throw Abstract.indexTypeError(this, item);
            }
        }

        /**
         * Implementation of {@code __delitem__}. Delete either an
         * element or a slice of the client sequence, after checks, by
         * calling either {@link #delItem(int)} or
         * {@link #delImpl(Indices, Object)}.
         *
         * @param item (or slice) to delete in the client
         * @throws ValueError if {@code slice.step==0} or value is the
         *     wrong length in an extended slice ({@code slice.step!=1}
         * @throws TypeError from bad slice index types
         * @throws Throwable from errors other than indexing
         */
        public void __delitem__(Object item)
                throws TypeError, Throwable {
            if (PyNumber.indexCheck(item)) {
                int i = PyNumber.asSize(item, IndexError::new);
                delItem(adjustSet(i));
            } else if (item instanceof PySlice) {
                Indices slice = ((PySlice)item).new Indices(length());
                delSlice(slice);
            } else {
                throw Abstract.indexTypeError(this, item);
            }
        }

        /**
         * Implementation of {@code __add__} (concatenation) by calling
         * {@link #add(Object)}.
         * <p>
         * The wrapper attempts no conversion of the argument, but it
         * will catch {@link NoConversion} exceptions from
         * {@link #add(Object)}, to return {@code NotImplemented}. It
         * will also catch Java {@code OutOfMemoryError} and convert it
         * to a Python {@link OverflowError}.
         *
         * @param w right operand
         * @return the concatenated result {@code self+w}
         * @throws OverflowError when cannot allocate space
         * @throws Throwable from other causes in the implementation.
         */
        Object __add__(Object w) throws OverflowError, Throwable {
            try {
                return add(w);
            } catch (OutOfMemoryError e) {
                throw concatOverflow();
            } catch (NoConversion e) {
                /*
                 * Since we do not implement __concat__ separate from
                 * __add_, unlike CPython, we do not yet know that
                 * Object w has no __radd__, and cannot produce the
                 * TypeError "can only concatenate S to S". Instead,
                 * Abstract.add will produce a TypeError about
                 * "unsupported operand types" for '+'.
                 */
                return Py.NotImplemented;
            }
        }

        /**
         * Implementation of {@code __radd__} (reflected concatenation)
         * by calling {@link #radd(Object)}.
         * <p>
         * The wrapper attempts no conversion of the argument, but it
         * will catch {@link NoConversion} exceptions from
         * {@link #radd(Object)}, to return {@code NotImplemented}. It
         * will also catch Java {@code OutOfMemoryError} and convert it
         * to a Python {@link OverflowError}.
         *
         * @param v left operand
         * @return the concatenated result {@code v+self}
         * @throws OverflowError when cannot allocate space
         * @throws Throwable from other causes in the implementation.
         */
        Object __radd__(Object v) throws OverflowError, Throwable {
            try {
                return radd(v);
            } catch (OutOfMemoryError e) {
                throw concatOverflow();
            } catch (NoConversion e) {
                /*
                 * See comment in __add__, noting that sometimes
                 * __radd__ is called before v.__add__.
                 */
                return Py.NotImplemented;
            }
        }

        /**
         * Implementation of {@code __mul__} (repetition) and
         * {@code __rmul__} by calling {@link #repeat(Object)}.
         * <p>
         * The wrapper attempts conversion of the argument to
         * {@code int}, and if this cannot be achieved, it will return
         * {@code NotImplemented}. It will also catch Java
         * {@code OutOfMemoryError} and convert it to a Python
         * {@link OverflowError}.
         *
         * @param n number of repetitions in result
         * @return the repeating result {@code self*n}
         * @throws OverflowError when {@code n} over-size or cannot
         *     allocate space
         * @throws TypeError if {@code n} has no {@code __index__}
         * @throws Throwable from implementation of {@code __index__},
         *     or other causes in the implementation.
         */
        Object __mul__(Object n) throws TypeError, Throwable {
            if (PyNumber.indexCheck(n)) {
                int count = PyNumber.asSize(n, OverflowError::new);
                try {
                    return repeat(count);
                } catch (OutOfMemoryError e) {
                    throw repeatOverflow();
                }
            } else {
                /*
                 * Since we do not implement __repeat__ separate from
                 * __mul_, unlike CPython, we do not yet know that
                 * Object n has no __rmul__, and cannot produce the
                 * TypeError "can't multiply sequence by non-int".
                 * Instead, Abstract.multiply will produce a TypeError
                 * about "unsupported operand types" for '*'.
                 */
                return Py.NotImplemented;
            }
        }

        /**
         * Implementation of the {@code index} method of sequences. Find the
         * index, in the given range, of an element equal to the argument. or
         * a slice of the client sequence, after checks, by calling either
         * {@link #getItem(int)} or {@link #getSlice(Indices)}.
         *
         * @param v value to match in the client
         * @return the index at which found
         * @throws ValueError if {@code v} not found
         * @throws TypeError from bad {@code start} and {@code stop} types
         * @throws Throwable from errors other than indexing
         */
        public int index(Object v, Object start, Object stop) throws TypeError, Throwable {
            int iStart = boundedIndex(start, 0);
            int iStop = boundedIndex(stop, length());
            for (int i = iStart; i < iStop; i++) {
                if (Abstract.richCompareBool(v, getItem(i), Comparison.EQ)) { return i; }
            }
            throw new ValueError("%s.index(x): x not in %s", getType().name, getTypeName());
        }

        /**
         * Check that an index {@code i} satisfies
         * 0&le;i&lt;{@link #length()}. If the original index is
         * negative, treat it as end-relative by first adding
         * {@link #length()}.
         *
         * @param i to check is valid index
         * @return range-checked {@code i}
         * @throws IndexError if {@code i} out of range
         */
        protected int adjustGet(int i) {
            final int L = length();
            if (i < 0) {
                i += L;
                if (i >= 0) { return i; }
            } else if (i < L) { return i; }
            throw rangeIndexError("");
        }

        /**
         * Check that an index {@code i} satisfies
         * 0&le;i&lt;{@link #length()}. If the original index is
         * negative, treat it as end-relative by first adding
         * {@link #length()}. This differs from {@link #adjustGet(int)}
         * only in that the message produced mentions "assignment".
         *
         * @param i to check is valid index
         * @return range-checked {@code i}
         * @throws IndexError if {@code i} out of range
         */
        protected int adjustSet(int i) throws IndexError {
            final int L = length();
            if (i < 0) {
                i += L;
                if (i >= 0) { return i; }
            } else if (i < L) { return i; }
            throw rangeIndexError("assignment");
        }

        /**
         * Creates an {@link IndexError} with the message
         * "{@link #getTypeName()} KIND index out of range", e.g. "list
         * assignment index out of range".
         *
         * @param kind word to insert for KIND: "" or "assignment".
         * @return an exception to throw
         */
        final protected IndexError rangeIndexError(String kind) {
            String space = kind.length() > 0 ? " " : "";
            return new IndexError("%s%s%s index out of range",
                    getTypeName(), space, kind);
        }

        /**
         * An overflow error with the message "concatenated
         * {@link #getTypeName()} is too long", involving the type name
         * of the client sequence type.
         *
         * @param seq the sequence operated on
         * @return an exception to throw
         */
        private OverflowError concatOverflow() {
            return new OverflowError("concatenated %s is too long",
                    getTypeName());
        }

        /**
         * An overflow error with the message "repeated S is too long",
         * where S is the type mane of the argument.
         *
         * @return an exception to throw
         */
        private OverflowError repeatOverflow() {
            return new OverflowError("repeated %s is too long",
                    getTypeName());
        }

        /**
         * Accept an object index (or {@code null}), treating negative
         * values as end-relative, and bound it to the sequence range. The
         * index object must be convertible by
         * {@link PyNumber#asSize(Object, java.util.function.Function)
         * PyNumber.asSize}. Unlike {@link #adjustGet(int)}, is not an error
         * for the index value to fall outside the valid range. (It is
         * simply clipped to the nearer end.)
         * 
         * @param index purported index (or {@code null})
         * @param defaultValue to use if {@code index==null}
         * @return converted index
         * @throws TypeError from bad {@code index} type
         * @throws Throwable from other conversion errors
         */
        protected int boundedIndex(Object index, int defaultValue) throws TypeError, Throwable {

            // Convert the argument (or raise a TypeError)
            int i, L = length();
            if (index == null) {
                i = defaultValue;
            } else if (PyNumber.indexCheck(index)) {
                i = PyNumber.asSize(index, IndexError::new);
            } else {
                throw Abstract.indexTypeError(this, index);
            }

            // Bound the now integer index to the sequence
            if (i < 0) {
                i += L;
                return Math.max(0, i);
            } else {
                return Math.min(L - 1, i);
            }
        }
    }
}
