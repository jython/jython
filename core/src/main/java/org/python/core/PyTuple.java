// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Stream;

import org.python.core.PySlice.Indices;
import org.python.core.PyUtil.NoConversion;
import org.python.internal.Util;
import org.python.support.InterpreterError;
import org.python.types.Exposed.PythonMethod;
import org.python.types.Feature;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/** The Python {@code tuple} object. */
public class PyTuple extends AbstractList<Object> implements WithClass {

    /** The Python type object for {@code tuple}. */
    public static final PyType TYPE = PyType.fromSpec( //
            new TypeSpec("tuple", MethodHandles.lookup()).add(Feature.BASETYPE,
                    Feature.SEQUENCE_PROTOCOL));

    /** The elements of the {@code tuple}. */
    private final Object[] value;

    /** Implementation help for sequence methods. */
    private TupleDelegate delegate = new TupleDelegate();

    /**
     * Potentially unsafe constructor, capable of creating a "{@code tuple}
     * view" of an array, or safely wrapping a copy of that array. We make a
     * copy (the safe option) if the caller is <b>not</b> prepared to
     * promise <b>not</b> to modify the array.
     *
     * @param <E> element type of the array
     * @param iPromiseNotToModifyTheArray if {@code true} try to re-use the
     *     array, otherwise make a copy.
     * @param value of the tuple
     * @throws ArrayStoreException if any element of {@code value} is not
     *     assignment compatible with {@code Object}. Caller would have to
     *     have cast {@code value} to avoid static checks.
     */
    @SuppressWarnings("unchecked")
    private <E> PyTuple(boolean iPromiseNotToModifyTheArray, E[] value) throws ArrayStoreException {

        if (iPromiseNotToModifyTheArray) {
            // The tuple will wrap the caller's array.
            this.value = value;

        } else {
            // We make a new array of the same size and element type.
            int n = value.length;
            Class<?> klass = value.getClass();
            // Set an array of the right type as this.value
            if ((Object)klass == (Object)Object[].class) {
                this.value = new Object[n];
            } else {
                this.value = (E[])Array.newInstance(klass.getComponentType(), n);
            }
            // Receive the elements
            System.arraycopy(value, 0, this.value, 0, n);
        }
    }

    /**
     * Unsafely wrap an array of {@code Object} in a "tuple view".
     * <p>
     * The method is unsafe insofar as the array becomes embedded as the
     * value of the tuple. <b>The client therefore promises not to modify
     * the content.</b> For this reason, this method should only ever be
     * private. If you feel tempted to make it otherwise, consider using (or
     * improving) {@link Builder}.
     *
     * @param <E> component type of the array in the new tuple
     * @param value of the new tuple or {@code null}
     * @return a tuple with the given contents or {@link #EMPTY}
     */
    private static <E> PyTuple wrap(E[] value) throws ArrayStoreException {
        if (value == null)
            return EMPTY;
        else
            return new PyTuple(true, value);
    }

    /**
     * Construct a {@code PyTuple} from the elements of a collection.
     *
     * @param c source of element values for this {@code tuple}
     */
    public PyTuple(Collection<?> c) { this(true, c.toArray()); }

    /**
     * Construct a {@code PyTuple} from an array of {@link Object}s or zero
     * or more {@link Object} arguments provided as a slice of an array. The
     * argument is copied for use, so it is safe to modify the array passed
     * in.
     *
     * @param <E> component type
     * @param a source of element values
     * @param start first element to include
     * @param count number of elements to take
     */
    public <E> PyTuple(E a[], int start, int count) {
        // We make a new array.
        this.value = Arrays.copyOfRange(a, start, start + count);
    }

    /**
     * Construct a {@code PyTuple} from a slice of another {@code PyTuple}.
     *
     * @param a source of element values
     * @param start first element to include
     * @param count number of elements to take
     */
    public PyTuple(PyTuple a, int start, int count) {
        // We make a new array.
        this.value = Arrays.copyOfRange(a.value, start, start + count);
    }

    /**
     * Construct a {@code PyTuple} from an array of {@link Object}s. The
     * argument is copied for use, so it is safe to modify the array passed
     * in.
     *
     * @param <E> component type
     * @param a source of element values
     */
    public <E> PyTuple(E a[]) { this(false, a); }

    /**
     * Construct a {@code PyTuple} from the variable argument list, or if
     * the list is empty, return {@link #EMPTY}.
     *
     * @param a value of new tuple
     * @return a tuple with the given contents or {@link #EMPTY}
     */
    public static PyTuple of(Object... a) {
        int n = a.length;
        return a.length == 0 ? EMPTY : new PyTuple(a, 0, n);
    }

    /**
     * Construct a {@code PyTuple} from the elements of a collection, or if
     * the collection is empty, return {@link #EMPTY}. In circumstances
     * where the argument will often be empty, this has space and time
     * advantages over the constructor {@link #PyTuple(Collection)}.
     *
     * @param <E> component type
     * @param c value of new tuple
     * @return a tuple with the given contents or {@link #EMPTY}
     */
    public static <E> PyTuple from(Collection<E> c) {
        return c.size() == 0 ? EMPTY : new PyTuple(c);
    }

    /**
     * Construct a {@code PyTuple} from the elements of a stream, or if the
     * array is zero-length, return {@link #EMPTY}.
     *
     * @param <E> component type
     * @param s source of element values for the new {@code tuple}
     * @return a tuple with the given contents or {@link #EMPTY}
     */
    public static <E> PyTuple from(Stream<E> s) {
        Object[] a = s.toArray();
        return a.length == 0 ? EMPTY : new PyTuple(true, a);
    }

    /**
     * Construct a {@code PyTuple} from the elements of an array, or if the
     * array is zero-length, return {@link #EMPTY}. In circumstances where
     * the argument will often be empty, this has space and time advantages
     * over the constructor {@link #PyTuple(Object[])}.
     *
     * @param <E> component type
     * @param a value of new tuple
     * @return a tuple with the given contents or {@link #EMPTY}
     */
    public static <E> PyTuple from(E[] a) { return a.length == 0 ? EMPTY : new PyTuple(a); }

    // Java API ------------------------------------------------------

    /**
     * Copy from the tuple value to a destination array provided by the
     * caller. Arguments are consciously modelled on those of
     * {@code System.arraycopy}.
     *
     * @param <E> component type of the destination
     * @param srcPos starting position in the source array.
     * @param dst the destination array.
     * @param dstPos starting position in the destination data.
     * @param length the number of array elements to be copied.
     * @throws ArrayStoreException if an element in the {@code tuple} array
     *     could not be stored into the {@code dst} array
     */
    public <E> void copyTo(int srcPos, E[] dst, int dstPos, int length) throws ArrayStoreException {
        System.arraycopy(value, srcPos, dst, dstPos, length);
    }

    /**
     * Check that all the objects in this tuple are of the required Java
     * type and return a new array of that type containing them. In certain
     * parts of the interpreter, we represent as tuples arrays of objects
     * that have to have a particular Java type. (An example is the closure
     * of a {@code function} object in which every element must be a
     * {@code PyCell}.) We throw the specified exception if we encounter an
     * element that is not of the required Java type (or a sub-type).
     * <p>
     * Note that it is the Java type that is checked. This method is not
     * entirely suitable for enforcing a specified Python type where the
     * desired type has multiple implementations.
     *
     * @param <E> the Java type of element the tuple has to contain
     * @param <X> type of exception to throw
     * @param klass a class object for the element Java type
     * @param exc to supply the exception based on the offending element
     * @return {@code E[]} array of tuple elements
     * @throws X if an element is not of Java type {@code E}
     */
    @SuppressWarnings("unchecked")
    <E, X extends Throwable> E[] toArray(Class<E> klass, Function<Object, X> exc) throws X {
        E[] a = (E[])Array.newInstance(klass, value.length);
        int i = 0;
        for (Object v : value) {
            try {
                /*
                 * Although the cast is not checked at runtime, since E is erased to
                 * Object, the JVM defends the array from the wrong kind of element.
                 */
                a[i++] = (E)v;
            } catch (ArrayStoreException e) {
                throw exc.apply(v);
            }
        }
        return a;
    }

    /**
     * Check that all the objects in this tuple are of the required Java
     * type and return a new array of that type containing them. We throw an
     * {@link InterpreterError} if we encounter an element that is not of
     * the required Java type (or a sub-type).
     *
     * @param <E> the Java type of element the tuple has to contain
     * @param klass a class object for the element Java type
     * @return {@code E[]} array of tuple elements
     * @throws InterpreterError if an element is not of Java type {@code E}
     */
    <E> E[] toArray(Class<E> klass) {
        return toArray(klass,
                v -> new InterpreterError("tuple element has incorrect Java element %s",
                        v.getClass()));
    }

    /**
     * Create a Python {@code dict} from this {@code tuple} taking its
     * elements as a key and corresponding value alternately. The last
     * element of a tuple with odd length is ignored.
     *
     * @return the {@code dict} of the key-value pairs
     */
    PyDict pairsToDict() {
        // XXX is this still used?
        return PyDict.fromKeyValuePairs(value, 0, value.length / 2);
    }

    @Override
    public PyType getType() { return TYPE; }

    /** Convenient constant for a {@code tuple} with zero elements. */
    static final PyTuple EMPTY = new PyTuple(true, new Object[0]);

    // Special methods -----------------------------------------------

    @SuppressWarnings("unused")
    private int __len__() { return size(); }

    @SuppressWarnings("unused")
    private boolean __contains__(Object o) throws Throwable {
        for (Object v : value) {
            if (Abstract.richCompareBool(v, o, Comparison.EQ)) { return true; }
        }
        return false;
    }

    @SuppressWarnings("unused")
    private Object __ne__(Object o) { return delegate.cmp(o, Comparison.NE); }

    @SuppressWarnings("unused")
    private Object __eq__(Object o) { return delegate.cmp(o, Comparison.EQ); }

    @SuppressWarnings("unused")
    private Object __gt__(Object o) { return delegate.cmp(o, Comparison.GT); }

    @SuppressWarnings("unused")
    private Object __ge__(Object o) { return delegate.cmp(o, Comparison.GE); }

    @SuppressWarnings("unused")
    private Object __lt__(Object o) { return delegate.cmp(o, Comparison.LT); }

    @SuppressWarnings("unused")
    private Object __le__(Object o) { return delegate.cmp(o, Comparison.LE); }

    @SuppressWarnings("unused")
    private Object __add__(Object w) throws Throwable { return delegate.__add__(w); }

    @SuppressWarnings("unused")
    private Object __radd__(Object v) throws Throwable { return delegate.__radd__(v); }

    @SuppressWarnings("unused")
    private Object __mul__(Object n) throws Throwable { return delegate.__mul__(n); }

    @SuppressWarnings("unused")
    private Object __rmul__(Object n) throws Throwable { return delegate.__mul__(n); }

    @SuppressWarnings("unused")
    private Object __getitem__(Object item) throws Throwable { return delegate.__getitem__(item); }

    private int __hash__() throws Throwable {
        /*
         * Ported from C in CPython 3.11 tupleobject.c, which in turn is based
         * on the xxHash specification. We do not attempt to maintain historic
         * hash of the empty tuple or avoid returning -1. Seed the accumulator
         * based on the length.
         */
        int acc = H32P5 * value.length;
        for (Object x : value) {
            acc += H32P2 * Abstract.hash(x);
            // The parenthetical expression is rotate left 13
            acc = H32P1 * (acc << 13 | acc >>> 19);
        }
        return acc;
    }

    @SuppressWarnings("unused")
    private Object __repr__() { return toString(); }

    @SuppressWarnings("unused")
    private Object __str__() { return toString(); }

    // Python API ----------------------------------------------------

    @PythonMethod
    private int count(Object v) {
        int count = 0;
        for (Object item : value) { if (item.equals(v)) { count++; } }
        return count;
    }

    @PythonMethod
    private Object index(Object v, Object start, Object stop) throws Throwable {
        return delegate.index(v, start, stop);
    }

    // AbstractList methods ------------------------------------------

    @Override
    public int hashCode() {
        try {
            return __hash__();
        } catch (PyBaseException e) {
            throw e;
        } catch (Throwable t) {
            throw new InterpreterError(t, "Non-Python exception in __hash__");
        }
    }

    @Override
    public boolean equals(Object other) {
        try {
            return Abstract.richCompareBool(this, other, Comparison.EQ);
        } catch (PyBaseException e) {
            throw e;
        } catch (Throwable e) {
            return false;
        }
    }

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
     * A class for constructing a tuple element-wise. Sometimes the elements
     * of a {@code tuple} have to be generated sequentially. The natural
     * thing is to allocate and fill an array, and then for the sake of
     * efficiency, to make that array the storage of a {@code PyTuple}. The
     * direct approach breaks the encapsulation that guarantees a
     * {@code PyTuple} is immutable.
     * <p>
     * This class lets a client allocate and write an array element-wise,
     * that becomes the storage of a {@code tuple}, without ever having a
     * direct reference to the array.
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

        /** Create an empty buffer of a default initial capacity. */
        Builder() {
            value = Util.EMPTY_ARRAY;
        }

        /**
         * The number of elements currently in the {@code Builder}.
         *
         * @return the number of elements currently.
         */
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
        public Builder extend(Collection<?> seq) {
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
        public Builder extend(Iterator<?> iter) {
            while (iter.hasNext()) { append(iter.next()); }
            return this;
        }

        /**
         * Provide the contents as a Python {@code tuple} and reset the builder
         * to empty. (This is a "destructive read".)
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
                value = Util.EMPTY_ARRAY;
            } else {
                // The array is partly filled: copy the part used.
                v = Arrays.copyOf(value, len);
            }
            len = 0;
            return wrap(v);
        }

        /**
         * Provide the contents as a Java {@code String} (non-destructively).
         */
        @Override
        public String toString() { return (new PyTuple(value)).toString(); }
    }

    // Plumbing ------------------------------------------------------

    /*
     * Constants used in __hash__ (from CPython tupleobject.c), in the
     * 32-bit configuration (SIZEOF_PY_UHASH_T > 4 is false). Although out
     * of range for signed 32 bit integers, the multiplications are correct,
     * since (U-C) * (V-C) = U*V when taken mod C. (C=2**32).
     */
    private static final int H32P1 = (int)2654435761L;
    private static final int H32P2 = (int)2246822519L;
    private static final int H32P5 = 374761393;

    /**
     * Wrap this {@code PyTuple} as a {@link PySequence.Delegate}, for the
     * management of indexing and other sequence operations.
     */
    class TupleDelegate extends PySequence.Delegate<Object, PyTuple> {

        @Override
        public int length() { return value.length; }

        @Override
        public PyType getType() {
            // Get the actual type of the client tuple subclass
            return PyTuple.this.getType();
        }

        @Override
        public Object getItem(int i) { return value[i]; }

        @Override
        public Object get(int i) { return value[i]; }

        @Override
        public PyTuple getSlice(Indices slice) throws Throwable {
            Object[] v;
            if (slice.step == 1)
                v = Arrays.copyOfRange(value, slice.start, slice.stop);
            else {
                v = new Object[slice.slicelength];
                int i = slice.start;
                for (int j = 0; j < slice.slicelength; j++) { v[j] = value[i]; i += slice.step; }
            }
            return wrap(v);
        }

        @Override
        Object add(Object ow) throws NoConversion {
            if (ow instanceof PyTuple) {
                PyTuple w = (PyTuple)ow;
                return PyTuple.concat(value, w.value);
            } else {
                return Py.NotImplemented;
            }
        }

        @Override
        Object radd(Object ov) throws NoConversion {
            if (ov instanceof PyTuple) {
                PyTuple v = (PyTuple)ov;
                return PyTuple.concat(v.value, value);
            } else {
                return Py.NotImplemented;
            }
        }

        @Override
        PyTuple repeat(int n) {
            if (n == 0)
                return EMPTY;
            else if (n == 1 || value.length == 0)
                return PyTuple.this;
            else {
                int m = value.length;
                Object[] b = new Object[n * m];
                for (int i = 0, p = 0; i < n; i++, p += m) { System.arraycopy(value, 0, b, p, m); }
                return wrap(b);
            }
        }

        @Override
        public Iterator<Object> iterator() { return PyTuple.this.iterator(); }

        @Override
        public int compareTo(PySequence.Delegate<Object, PyTuple> other) {
            try {
                int N = value.length, M = other.length(), i;

                for (i = 0; i < N; i++) {
                    Object a = value[i];
                    if (i < M) {
                        Object b = other.getItem(i);
                        // if a != b, then we've found an answer
                        if (!Abstract.richCompareBool(a, b, Comparison.EQ))
                            return Abstract.richCompareBool(a, b, Comparison.GT) ? 1 : -1;
                    } else
                        // value has not run out, but other has. We win.
                        return 1;
                }

                /*
                 * The arrays matched over the length of value. The other is the winner
                 * if it still has elements. Otherwise it's a tie.
                 */
                return i < M ? -1 : 0;
            } catch (PyBaseException e) {
                // It's ok to throw legitimate Python exceptions
                throw e;
            } catch (Throwable t) {
                /*
                 * Contract of Comparable prohibits propagation of checked exceptions,
                 * but richCompareBool in principle throws anything.
                 */
                // XXX perhaps need a PyException to wrap Java Throwable
                throw new InterpreterError(t, "non-Python exeption in comparison");
            }
        }

        /**
         * Compare this delegate with the delegate of the other {@code tuple}
         * for equality. We do this separately from
         * {@link #cmp(Object, Comparison)} because it is slightly cheaper, but
         * also because so we don't panic where an element is capable of an
         * equality test, but not a less-than test.
         *
         * @param other delegate of tuple at right of comparison
         * @return {@code true} if equal, {@code false} if not.
         */
        private boolean compareEQ(PySequence.Delegate<Object, PyTuple> other) {
            try {
                if (other.length() != value.length) { return false; }
                int i = 0;
                for (Object b : other) {
                    Object a = value[i++];
                    // if a != b, then we've found an answer
                    if (!Abstract.richCompareBool(a, b, Comparison.EQ))
                        return false;
                }
                // The arrays matched over their length.
                return true;
            } catch (PyBaseException e) {
                // It's ok to throw legitimate Python exceptions
                throw e;
            } catch (Throwable t) {
                throw new InterpreterError(t, "non-Python exeption in comparison");
            }
        }

        /**
         * Compare this delegate with the delegate of the other {@code tuple},
         * or return {@code NotImplemented} if the other is not a {@code tuple}.
         *
         * @param other tuple at right of comparison
         * @param op type of operation
         * @return boolean result or {@code NotImplemented}
         */
        private Object cmp(Object other, Comparison op) {
            if (other instanceof PyTuple) {
                // Tuple is comparable only with another tuple
                TupleDelegate o = ((PyTuple)other).delegate;
                if (op == Comparison.EQ) {
                    return compareEQ(o);
                } else if (op == Comparison.NE) {
                    return !compareEQ(o);
                } else {
                    return op.toBool(delegate.compareTo(o));
                }
            } else {
                return Py.NotImplemented;
            }
        }
    }

    /** Concatenate two arrays (for {@code TupleDelegate}). */
    private static PyTuple concat(Object[] v, Object[] w) {
        int n = v.length, m = w.length;
        Object[] b = new Object[n + m];
        System.arraycopy(v, 0, b, 0, n);
        System.arraycopy(w, 0, b, n, m);
        return wrap(b);
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
