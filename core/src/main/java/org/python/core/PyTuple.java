// Copyright (c)2023 Jython Developers.
// Copyright (c) Corporation for National Research Initiatives
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.StringJoiner;
import java.util.stream.Stream;

import org.python.base.InterpreterError;
import org.python.core.PyObjectUtil.NoConversion;
import org.python.core.PySlice.Indices;
import org.python.core.PyType.Spec;

/** The Python {@code tuple} object. */
public class PyTuple extends AbstractList<Object> implements CraftedPyObject {

    /** The Python type object for {@code tuple}. */
    public static final PyType TYPE = PyType.fromSpec( //
            new Spec("tuple", MethodHandles.lookup()));

    /** The Python type of this instance. */
    protected final PyType type;

    /** The elements of the {@code tuple}. */
    final Object[] value;

    /** Implementation help for sequence methods. */
    private TupleDelegate delegate = new TupleDelegate();

    /**
     * Potentially unsafe constructor, capable of creating a
     * "{@code tuple} view" of an array, or a copy. We make a copy (the
     * safe option) if the caller is <b>not</b> prepared to promise
     * <b>not</b> to modify the array. The arguments begin with a
     * claimed element type for the array, or the element type of the
     * array to create.
     *
     * @param type sub-type for which this is being created
     * @param cls class of elements
     * @param iPromiseNotToModifyTheArray if {@code true} try to re-use
     *     the array, otherwise make a copy.
     * @param value of the tuple
     * @throws ArrayStoreException if any element of {@code value} is
     *     not assignment compatible with {@code Object}. Caller would
     *     have to have cast {@code value} to avoid static checks.
     */
    // @SuppressWarnings("unchecked")
    private <E> PyTuple(PyType type, boolean iPromiseNotToModifyTheArray, E[] value)
            throws ArrayStoreException {
        this.type = type;
        if (iPromiseNotToModifyTheArray) {
            this.value = value;
        } else {
            // We make a new array .
            int n = value.length;
            this.value = new Object[n];
            System.arraycopy(value, 0, this.value, 0, n);
        }
    }

    /**
     * Unsafely wrap an array of {@code Object} in a "tuple view".
     * <p>
     * The method is unsafe insofar as the array becomes embedded as the
     * value of the tuple. <b>The client therefore promises not to
     * modify the content.</b> For this reason, this method should only
     * ever be private. If you feel tempted to make it otherwise,
     * consider using (or improving) {@link Builder}.
     *
     * @param <E> component type of the array in the new tuple
     * @param value of the new tuple or {@code null}
     * @return a tuple with the given contents or {@link #EMPTY}
     */
    private static <E> PyTuple wrap(E[] value) throws ArrayStoreException {
        if (value == null)
            return EMPTY;
        else
            return new PyTuple(TYPE, true, value);
    }

    /**
     * Construct a {@code PyTuple} from an array of {@link Object}s or
     * zero or more {@link Object} arguments. The argument is copied for
     * use, so it is safe to modify an array passed in.
     *
     * @param <E> Actual element type
     * @param elements of the tuple
     */
    @SafeVarargs
    public <E> PyTuple(E... elements) { this(TYPE, elements); }

    /**
     * As {@link #PyTuple(Object...)} for Python sub-class specifying
     * {@link #type}.
     *
     * @param <E> element type of the {@code tuple} internally
     * @param type actual Python sub-class to being created
     * @param elements of the tuple
     */
    @SafeVarargs
    protected <E> PyTuple(PyType type, E... elements) { this(type, false, elements); }

    /**
     * Construct a {@code PyTuple} from the elements of a collection.
     *
     * @param c source of element values for this {@code tuple}
     */
    PyTuple(Collection<?> c) { this(TYPE, c); }

    /**
     * As {@link #PyTuple(Collection)} for Python sub-class specifying
     * {@link #type}.
     *
     * @param type actual Python sub-class to being created
     * @param c elements of the tuple
     */
    protected PyTuple(PyType type, Collection<?> c) {
        this(type, true, c.toArray(new Object[c.size()]));
    }

    /**
     * Construct a {@code PyTuple} from the elements of a stream.
     *
     * @param s source of element values for this {@code tuple}
     */
    PyTuple(Stream<?> s) { this(TYPE, true, s.toArray()); }

    /**
     * As {@link #PyTuple(Object[], int, int)} for Python sub-class
     * specifying {@link #type}.
     *
     * @param type actual Python type to construct
     * @param a source of element values
     * @param start first element to include
     * @param count number of elements to take
     */
    protected PyTuple(PyType type, Object a[], int start, int count) {
        this.type = type;
        // We make a new array.
        this.value = new Object[count];
        System.arraycopy(a, start, this.value, 0, count);
    }

    /**
     * Construct a {@code PyTuple} from an array of {@link Object}s or
     * zero or more {@link Object} arguments provided as a slice of an
     * array. The argument is copied for use, so it is safe to modify an
     * array passed in.
     *
     * @param a source of element values
     * @param start first element to include
     * @param count number of elements to take
     */
    PyTuple(Object a[], int start, int count) { this(TYPE, a, start, count); }

    /**
     * Construct a {@code PyTuple} from the elements of an array, or if
     * the collection is empty, return {@link #EMPTY}.
     *
     * @param <E> component type
     * @param a value of new tuple
     * @return a tuple with the given contents or {@link #EMPTY}
     */
    static <E> PyTuple from(E[] a) {
        int n = a.length;
        return a.length == 0 ? EMPTY : new PyTuple(a, 0, n);
    }

    /**
     * Construct a {@code PyTuple} from the elements of a collection, or
     * if the collection is empty, return {@link #EMPTY}. In
     * circumstances where the argument will often be empty, this has
     * space and time advantages over the constructor
     * {@link #PyTuple(Collection)}.
     *
     * @param <E> component type
     * @param c value of new tuple
     * @return a tuple with the given contents or {@link #EMPTY}
     */
    static <E> PyTuple from(Collection<E> c) { return c.size() == 0 ? EMPTY : new PyTuple(c); }

    // @formatter:off
    /*
    @ExposedNew
    final static Object tuple_new(PyNewWrapper new_, boolean init, PyType subtype,
                                    Object[] args, String[] keywords) {
        ArgParser ap = new ArgParser("tuple", args, keywords, new String[] {"sequence"}, 0);
        Object S = ap.getPyObject(0, null);
        if (new_.for_type == subtype) {
            if (S == null) {
                return EMPTY;
            }
            if (S.getType() == PyTuple.TYPE) {
                return S;
            }
            if (S instanceof PyTuple.Derived) {
                return new PyTuple(((PyTuple) S).getArray());
            }
            return fromArrayNoCopy(Py.make_array(S));
        } else {
            if (S == null) {
                return new PyTuple.Derived(subtype, Py.EmptyObjects);
            }
            return new PyTuple.Derived(subtype, Py.make_array(S));
        }
    }

    /**
     * Return a new PyTuple from an iterable.
     *
     * Raises a TypeError if the object is not iterable.
     *
     * @param iterable an iterable Object
     * @return a PyTuple containing each item in the iterable
     * /
    public static PyTuple fromIterable(Object iterable) {
        return fromArrayNoCopy(Py.make_array(iterable));
    }
    */
    // @formatter:on


    @Override
    public PyType getType() { return type; }

    /** Convenient constant for a {@code tuple} with zero elements. */
    static final PyTuple EMPTY = new PyTuple();

    // Special methods -----------------------------------------------

    /*
    @ExposedMethod(doc = BuiltinDocs.tuple___len___doc)
    */
    @SuppressWarnings("unused")
    private int __len__() { return size(); }

    /*
    @ExposedMethod(doc = BuiltinDocs.tuple___contains___doc)
    */
    @SuppressWarnings("unused")
    private boolean __contains__(Object o) throws Throwable {
        for (Object v : value) {
            if (Abstract.richCompareBool(v, o, Comparison.EQ)) { return true; }
        }
        return false;
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___ne___doc)
    */
    @SuppressWarnings("unused")
    private Object __ne__(Object o) {
        return delegate.cmp(o, Comparison.NE);
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___eq___doc)
    */
    @SuppressWarnings("unused")
    private Object __eq__(Object o) {
        return delegate.cmp(o, Comparison.EQ);
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___gt___doc)
    */
    @SuppressWarnings("unused")
    private Object __gt__(Object o) {
        return delegate.cmp(o, Comparison.GT);
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___ge___doc)
    */
    @SuppressWarnings("unused")
    private Object __ge__(Object o) {
        return delegate.cmp(o, Comparison.GE);
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___lt___doc)
    */
    @SuppressWarnings("unused")
    private Object __lt__(Object o) {
        return delegate.cmp(o, Comparison.LT);
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___le___doc)
    */
    @SuppressWarnings("unused")
    private Object __le__(Object o) {
        return delegate.cmp(o, Comparison.LE);
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___add___doc)
    */
    @SuppressWarnings("unused")
    private Object __add__(Object w) throws Throwable {
        return delegate.__add__(w);
    }

    @SuppressWarnings("unused")
    private Object __radd__(Object v) throws Throwable {
        return delegate.__radd__(v);
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___mul___doc)
    */
    @SuppressWarnings("unused")
    private Object __mul__(Object n) throws Throwable {
        return delegate.__mul__(n);
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___rmul___doc)
    */
    @SuppressWarnings("unused")
    private Object __rmul__(Object n) throws Throwable {
        return delegate.__mul__(n);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.tuple___iter___doc)
    */
    // @formatter:off
    /*
    @SuppressWarnings("unused")
    private Object tuple___iter__() {
        return new PyTupleIterator(this);
    }
    */
    // @formatter:on

    /*
    @ExposedMethod(doc = BuiltinDocs.tuple___getitem___doc)
    */
    @SuppressWarnings("unused")
    private Object __getitem__(Object item) throws Throwable {
        return delegate.__getitem__(item);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.tuple___getnewargs___doc)
    */
    @SuppressWarnings("unused")
    private PyTuple __getnewargs__() {
        return new PyTuple((Object)this);
    }

    @Override
    public int hashCode() {
        try {
            return __hash__();
        } catch (PyException e) {
            throw e;
        } catch (Throwable t) {
            throw new InterpreterError(t, "Non-Python exception in __hash__");
        }
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.tuple___hash___doc)
    */
    @SuppressWarnings("unused")
    private int __hash__() throws Throwable {
        /*
         * Ported from C in CPython 3.11, which in turn is based on the
         * xxHash specification. We do not attempt to maintain historic
         * hash of () or avoid returning -1. Seed the accumulator based
         * on the length.
         */
        int acc = H32P5 * value.length;
        for (Object x : value) {
            acc += H32P2 * Abstract.hash(x);
            // The parenthetical expression is rotate left 13
            acc = H32P1 * (acc << 13 | acc >>> 19);
        }
        return acc;
    }
    /*
    @ExposedMethod(doc = BuiltinDocs.tuple_count_doc)
    */
    @SuppressWarnings("unused")
    private int count(Object v) throws Throwable {
        int count = 0;
        for (Object item : value) {
            if (Abstract.richCompareBool(item, v, Comparison.EQ)) { count++; }
        }
        return count;
    }

    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.tuple_index_doc)
    */
    @SuppressWarnings("unused")
    private Object index(Object v, Object start, Object stop) throws Throwable {
        return delegate.index(v, start, stop);
    }

    @Override
    public boolean equals(Object other) {
        try {
            return Abstract.richCompareBool(this, other, Comparison.EQ);
        } catch (PyException e) {
            throw e;
        } catch (Throwable e) {
            return false;
        }
    }

    // AbstractList methods ------------------------------------------

    @Override
    public Object get(int i) { return value[i]; }

    @Override
    public int size() { return value.length; }

    @Override
    public Iterator<Object> iterator() { return listIterator(0); }

    @Override
    public ListIterator<Object> listIterator(final int index) {

        if (index < 0 || index > value.length)
            throw new IndexOutOfBoundsException(
                    String.format("%d outside [0, %d)", index, value.length));

        return new ListIterator<Object>() {

            private int i = index;

            @Override
            public boolean hasNext() { return i < value.length; }

            @Override
            public Object next() {
                if (i < value.length)
                    return value[i++];
                else
                    throw new NoSuchElementException();
            }

            @Override
            public boolean hasPrevious() { return i > 0; }

            @Override
            public Object previous() {
                if (i > 0)
                    return value[--i];
                else
                    throw new NoSuchElementException();
            }

            @Override
            public int nextIndex() { return i; }

            @Override
            public int previousIndex() { return i - 1; }

            @Override
            public void remove() { throw new UnsupportedOperationException(); }

            @Override
            public void set(Object o) { throw new UnsupportedOperationException(); }

            @Override
            public void add(Object o) { throw new UnsupportedOperationException(); }
        };
    }

    /**
     * A class for constructing a tuple element-wise. Sometimes the
     * elements of a {@code tuple} have to be generated sequentially.
     * The natural thing is to allocate and fill an array, and then for
     * the sake of efficiency, to make that array the storage of a
     * {@code PyTuple}. The direct approach breaks the encapsulation
     * that guarantees a {@code PyTuple} is immutable.
     * <p>
     * This class lets a client allocate and write an array
     * element-wise, that becomes the storage of a {@code tuple},
     * without ever having a direct reference to the array.
     */
    public static class Builder {
        private static final int MINSIZE = 16;
        private Object[] value;
        private int len = 0;

        /**
         * Create an empty buffer of a defined initial capacity.
         *
         * @param capacity initially
         */
        public Builder(int capacity) { value = new Object[capacity]; }

        /**
         * Create an empty buffer of a default initial capacity.
         */
        Builder() { this.value = Py.EMPTY_ARRAY; }

        /** @return the number of elements currently. */
        public int length() { return len; }

        /** Ensure there is room for another {@code n} elements. */
        private void ensure(int n) {
            if (len + n > value.length) {
                int newSize = Math.max(value.length * 2, MINSIZE);
                Object[] newValue = new Object[newSize];
                System.arraycopy(value, 0, newValue, 0, len);
                value = newValue;
            }
        }

        /**
         * Append one element.
         *
         * @param v to append
         * @return this builder
         */
        public Object append(Object v) {
            ensure(1);
            value[len++] = v;
            return this;
        }

        /**
         * Append all the elements from a sequence.
         *
         * @param seq supplying elements to append
         * @return this builder
         */
        public Builder append(Collection<?> seq) {
            ensure(seq.size());
            for (Object v : seq) { value[len++] = v; }
            return this;
        }

        /**
         * Append all the elements available from an iterator.
         *
         * @param iter supplying elements to append
         * @return this builder
         */
        public Builder append(Iterator<?> iter) {
            while (iter.hasNext()) { append(iter.next()); }
            return this;
        }

        /**
         * Provide the contents as a Python {@code tuple} and reset the
         * builder to empty. (This is a "destructive read".)
         *
         * @return the contents as a Python {@code tuple}
         */
        public PyTuple take() {
            Object[] v;
            if (len == 0) {
                return EMPTY;
            } else if (len == value.length) {
                // The array is exactly filled: use it without copy.
                v = value;
                value = Py.EMPTY_ARRAY;
            } else {
                // The array is partly filled: copy the part used.
                v = Arrays.copyOf(value, len);
            }
            len = 0;
            return wrap(v);
        }

        /**
         * Provide the contents as a Java {@code String}
         * (non-destructively).
         */
        @Override
        public String toString() { return (new PyTuple(TYPE, value, 0, len)).toString(); }
    }

    // Plumbing ------------------------------------------------------

    /*
     * Constants used in __hash__ (from CPython tupleobject.c), in the
     * 32-bit configuration (SIZEOF_PY_UHASH_T > 4 is false). Although
     * out of range for signed 32 bit integers, the multiplications are
     * correct, since (U-C) * (V-C) = U*V when taken mod C.
     */
    private static final int H32P1 = (int)2654435761L;
    private static final int H32P2 = (int)2246822519L;
    private static final int H32P5 = 374761393;

    /**
     * Wrap this {@code PyTuple} as a {@link PySequence.Delegate}, for
     * the management of indexing and other sequence operations.
     */
    class TupleDelegate extends PySequence.Delegate<Object, Object> {

        @Override
        public int length() { return value.length; };

        @Override
        public PyType getType() { return type; }

        @Override
        public Object getItem(int i) { return value[i]; }

        @Override
        public Object get(int i) { return value[i]; }

        @Override
        public Object getSlice(Indices slice) throws Throwable {
            Object[] v;
            if (slice.step == 1)
                v = Arrays.copyOfRange(value, slice.start, slice.stop);
            else {
                v = new Object[slice.slicelength];
                int i = slice.start;
                for (int j = 0; j < slice.slicelength; j++) {
                    v[j] = value[i];
                    i += slice.step;
                }
            }
            return new PyTuple(TYPE, true, v);
        }

        @Override
        Object add(Object ow) throws NoConversion {
            if (ow instanceof PyTuple) {
                PyTuple w = (PyTuple)ow;
                return PyTuple.concat(value, w.value);
            } else {
                throw PyObjectUtil.NO_CONVERSION;
            }
        }

        @Override
        Object radd(Object ov) throws NoConversion {
            if (ov instanceof PyTuple) {
                PyTuple v = (PyTuple)ov;
                return PyTuple.concat(v.value, value);
            } else {
                throw PyObjectUtil.NO_CONVERSION;
            }
        }

        @Override
        Object repeat(int n) {
            if (n == 0)
                return EMPTY;
            else if (n == 1 || value.length == 0)
                return PyTuple.this;
            else {
                int m = value.length;
                Object[] b = new Object[n * m];
                for (int i = 0, p = 0; i < n; i++, p += m) { System.arraycopy(value, 0, b, p, m); }
                return new PyTuple(TYPE, true, b);
            }
        }

        @Override
        public Iterator<Object> iterator() { return PyTuple.this.iterator(); }

        @Override
        public int compareTo(PySequence.Delegate<Object, Object> other) {
            try {
                // Tuple is comparable only with another tuple
                int N = value.length, M = other.length(), i = 0;

                for (i = 0; i < N; i++) {
                    Object a = value[i];
                    if (i < M) {
                        Object b = other.getItem(i);
                        // if a != b, then we've found an answer
                        if (!Abstract.richCompareBool(a, b, Comparison.EQ))
                            return Abstract.richCompareBool(a, b, Comparison.GT) ? 1 : -1;
                        if (!Abstract.richCompareBool(a, b, Comparison.EQ))
                            return Abstract.richCompareBool(a, b, Comparison.GT) ? 1 : -1;
                    } else
                        // value has not run out, but other has. We win.
                        return 1;
                }

                /*
                 * The arrays matched over the length of value. The other is the
                 * winner if it still has elements. Otherwise it's a tie.
                 */
                return i < M ? -1 : 0;
            } catch (PyException e) {
                // It's ok to throw legitimate Python exceptions
                throw e;
            } catch (Throwable t) {
                /*
                 * Contract of Comparable prohibits propagation of checked
                 * exceptions, but richCompareBool in principle throws anything.
                 */
                // XXX perhaps need a PyException to wrap Java Throwable
                throw new InterpreterError(t, "non-Python exeption in comparison");
            }
        }

        /**
         * Compare this delegate with the delegate of the other
         * {@code tuple}, or return {@code NotImplemented} if the other is
         * not a {@code tuple}.
         *
         * @param other tuple at right of comparison
         * @param op type of operation
         * @return boolean result or {@code NotImplemented}
         */
        Object cmp(Object other, Comparison op) {
            if (other instanceof PyTuple) {
                // Tuple is comparable only with another tuple
                TupleDelegate o = ((PyTuple)other).delegate;
                return op.toBool(delegate.compareTo(o));
            } else {
                return Py.NotImplemented;
            }
        }
    }

    /** Concatenate two arrays into a tuple (for TupleAdapter). */
    private static PyTuple concat(Object[] v, Object[] w) {
        int n = v.length, m = w.length;
        Object[] b = new Object[n + m];
        System.arraycopy(v, 0, b, 0, n);
        System.arraycopy(w, 0, b, n, m);
        return new PyTuple(TYPE, true, b);
    }

    @Override
    public String toString() {
        // Support the expletive comma "(x,)" for one element.
        String suffix = value.length == 1 ? ",)" : ")";
        StringJoiner sj = new StringJoiner(", ", "(", suffix);
        for (Object v : value) { sj.add(v.toString()); }
        return sj.toString();
    }
}
