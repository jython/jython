// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.python.core.PySlice.Indices;
import org.python.core.PyUtil.NoConversion;
import org.python.kernel.BaseType;
import org.python.kernel.KernelTypeFlag;
import org.python.kernel.Representation;
import org.python.types.TypeFlag;

/**
 * Abstract API for operations on sequence types, corresponding to
 * CPython methods defined in {@code abstract.h} and with names like:
 * {@code PySequence_*}.
 */
public class PySequence extends Abstract {

    private PySequence() {}   // only static methods here

    /**
     * Return whether the object (its type, rather) provides the sequence
     * protocol. Note that it returns {@code true} for Python classes with a
     * {@code __getitem__} method, unless they are dict subclasses, since in
     * general it is impossible to determine what type of keys the class
     * supports.
     *
     * @param s of which to test the type
     * @return whether the object provides the sequence protocol
     */
    // Compare CPython PySequence_check in abstract.c
    static boolean check(Object s) {
        Representation rep = representation(s);
        return rep.hasFeature(s, TypeFlag.SEQUENCE_PROTOCOL)
                || (rep.hasFeature(rep, KernelTypeFlag.HAS_GETITEM)
                        && !rep.hasFeature(s, TypeFlag.DICT_SUBCLASS));
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
     * Return a Python {@code tuple} with the same contents as the sequence
     * or iterable {@code o}. The returned {@code tuple} is guaranteed to be
     * new. This is equivalent to the Python expression {@code tuple(o)}.
     *
     * @param o to represent
     * @return the contents as a tuple
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if an
     *     iterator cannot be formed on {@code o}
     * @throws Throwable from the implementation of {@code o}
     */
    // Compare CPython PySequence_Tuple in abstract.c
    public static PyTuple tuple(Object o) throws PyBaseException, Throwable {
        PyTuple.Builder tb = collect(o, PyTuple.Builder::new, PyTuple.Builder::append);
        return tb.take();
    }

    /**
     * Return a Python {@code list} with the same contents as the sequence
     * or iterable {@code o}. The returned {@code list} is guaranteed to be
     * new. This is equivalent to the Python expression {@code list(o)}.
     *
     * @param o to represent
     * @return the contents as a list
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if an
     *     iterator cannot be formed on {@code o}
     * @throws Throwable from the implementation of {@code o}
     */
    // Compare CPython PySequence_List in abstract.c
    public static PyList list(Object o) throws PyBaseException, Throwable {
        return collect(o, PyList::new, PyList::add);
    }

    /**
     * Return the sequence or iterable {@code o} as a Java {@code List}. If
     * {@code o} is one of several built-in types that implement Java
     * {@code List<Object>}, this will be the object itself. Otherwise, it
     * will be a copy in a Java list that supports efficient random access.
     * <p>
     * If the object is not a Python sequence (defines {@code __getitem__})
     * or Python iterable (defines {@code __iter__}), call {@code exc} to
     * raise an exception (typically a {@link PyBaseException TypeError}).
     *
     * @param <E> the type of exception to throw
     * @param o to present as a list
     * @param exc a supplier (e.g. lambda expression) for the exception to
     *     throw if an iterator cannot be formed (or {@code null} for a
     *     default {@code TypeError})
     * @return the iterable (if a {@code List}) or its contents
     * @throws E if an iterator cannot be formed
     * @throws Throwable from the implementation of {@code o}.
     */
    // Compare CPython PySequence_Fast in abstract.c
    public static <E extends PyBaseException> List<Object> fastList(Object o, Supplier<E> exc)
            throws E, Throwable {

        if (PyList.TYPE.checkExact(o)) {
            return (PyList)o;

        } else if (PyTuple.TYPE.checkExact(o)) {
            return (PyTuple)o;

        } else {
            // Not one of the ready-made lists
            return fastNewList(o, exc);
        }
    }

    /**
     * Inner implementation of {@link #fastList(Object, Supplier)}, q.v..
     *
     * @param <E> the type of exception to throw
     * @param o a Python iterable or sequence to present as a Java
     *     {@code List<Object>}.
     * @param exc a supplier (e.g. lambda expression) for the exception (or
     *     {@code null} for a default {@code TypeError})
     * @return the contents of {@code o} as a list
     * @throws E to throw if an iterator cannot be formed
     * @throws Throwable from the implementation of {@code o}.
     */
    private static <E extends PyBaseException> List<Object> fastNewList(Object o, Supplier<E> exc)
            throws E, Throwable {
        List<Object> list = new ArrayList<>();
        Representation rep = representation(o);
        BaseType type = rep.pythonType(o);

        if (type.hasFeature(KernelTypeFlag.HAS_ITER)) {
            // Go via the iterator on o
            Object iter = rep.op_iter().invokeExact(o);
            // Check iter is an iterator (defines __next__).
            Representation iterRep = representation(iter);
            if (iterRep.hasFeature(iter, KernelTypeFlag.HAS_NEXT)) {
                // Create a handle on __next__
                MethodHandle next = iterRep.op_next().bindTo(iter);
                // Iterate o into a list
                try {
                    for (;;) { list.add(next.invokeExact()); }
                } catch (PyStopIteration stop) {
                    // Not a problem. (Every other exception is.)
                }
                return list;
            } // else fall out at throw exc

        } else if ((type.hasFeature(KernelTypeFlag.HAS_GETITEM))) {
            // o defines __getitem__
            MethodHandle getitem = rep.op_getitem().bindTo(o);
            try {
                for (int i = 0; /* until IndexError */ ; i++) {
                    list.add(getitem.invokeExact((Object)i));
                }
            } catch (PyBaseException e) {
                e.only(PyExc.IndexError);
            }
            return list;
        }

        // Out of possibilities: throw caller-defined exception
        if (exc != null) {
            throw exc.get();
        } else {
            throw requiredTypeError("an iterable", o);
        }
    }

    /**
     * Return the number of occurrences of {@code value} in {@code o}, that
     * is, return the number of keys for which o[key] == value. On a
     * list-like {@code o} this is equivalent to the Python expression
     * {@code o.count(value)}, but for a string {@code o}, it is equivalent
     * only if {@code value} is a single character of a type comparable with
     * {@code o}.
     *
     * @param o sequence to search
     * @param value to find
     * @return count of {@code value} in {@code o}
     * @throws PyBaseException on overflow or type errors
     * @throws Throwable on other errors in iteration or comparison
     */
    public static int count(Object o, Object value) throws PyBaseException, Throwable {
        // Iterate and count the matches
        int n = 0;
        for (Object item : getIterable(o)) {
            if (richCompareBool(value, item, Comparison.EQ)) {
                if (n < Integer.MAX_VALUE) {
                    n += 1;
                } else {
                    throw PyErr.format(PyExc.OverflowError, "count",
                            "%s exceeds Java integer size");
                }
            }
        }
        return n;
    }

    /**
     * Return the index of the first occurrences of {@code value} in
     * {@code o}, that is, return the first key for which o[key] == value.
     * On failure to find, raise a Python {@code ValueError}. On a list-like
     * {@code o} this is equivalent to the Python expression
     * {@code o.index(value)}, but for a string {@code o}, it is equivalent
     * only if {@code value} is a single character of a type comparable with
     * {@code o}.
     *
     * @param o sequence to search
     * @param value to find
     * @return count of {@code value} in {@code o}
     * @throws PyBaseException (ValueError) if not found, or overflow or
     *     type errors as appropriate
     * @throws Throwable on other errors in iteration or comparison
     */
    public static int index(Object o, Object value) throws PyBaseException, Throwable {
        // Iterate and count the items until a match
        int n = 0;
        for (Object item : getIterable(o)) {
            if (richCompareBool(value, item, Comparison.EQ)) { return n; }
            if (n < Integer.MAX_VALUE) {
                n += 1;
            } else {
                throw PyErr.format(PyExc.OverflowError, "index", "%s exceeds Java integer size");
            }
        }
        throw PyErr.format(PyExc.ValueError, "sequence.index(x): x not in sequence");
    }

    /**
     * Return whether {@code value} in {@code o}, that is, there is a key
     * for which o[key] == value. On a list-like {@code o} this is
     * equivalent to the Python expression {@code value in o}, but for a
     * string {@code o}, it is equivalent only if {@code value} is a single
     * character of a type comparable with {@code o}.
     *
     * @param o sequence to search
     * @param value to find
     * @return count of {@code value} in {@code o}
     * @throws PyBaseException on overflow or type errors
     * @throws Throwable on other errors in iteration or comparison
     */
    public static boolean contains(Object o, Object value) throws PyBaseException, Throwable {
        // Iterate until an item match
        for (Object item : getIterable(o)) {
            if (richCompareBool(value, item, Comparison.EQ)) { return true; }
        }
        return false;
    }

    // Classes supporting implementations of sequence types -----------

    /**
     * In the implementation of sequence types, it is useful to be able to
     * create iterables and streams from their content. This interface
     * provides a standardised API. Several {@link Delegate} implementations
     * in the core also provide this interface.
     *
     * @param <E> the type of element returned by the iterators
     */
    public static interface Of<E> extends Iterable<E> {

        /**
         * The length of this sequence.
         *
         * @return the length of this sequence
         */
        int length();

        /**
         * Get an item from the sequence at a given index {@code i}.
         *
         * @param i index
         * @return item at index {@code i}.
         */
        E get(int i);

        /**
         * {@inheritDoc} The characteristics {@code SIZED} and {@code SUBSIZED}
         * are additionally reported.
         */
        @Override
        default Spliterator<E> spliterator() {
            return Spliterators.spliterator(iterator(), length(), 0);
        }

        /**
         * @return the elements of this sequence as a {@code Stream}
         */
        default Stream<E> asStream() { return StreamSupport.stream(spliterator(), false); }
    }

    /**
     * A specialisation of {@link Of PySequence.Of&lt;Integer>} where the
     * elements may be consumed as primitive {@code int}.
     */
    public static interface OfInt extends Of<Integer> {

        /**
         * Get the int item from the sequence at a given index {@code i}.
         *
         * @param i index
         * @return item at index {@code i}.
         */
        int getInt(int i);

        @Override
        default Integer get(int i) { return getInt(i); }

        @Override
        Spliterator.OfInt spliterator();

        /**
         * Provide a stream specialised to primitive {@code int}.
         *
         * @return a stream of primitive {@code int}
         */
        IntStream asIntStream();

        /**
         * {@inheritDoc}
         *
         * @implNote The default implementation is the stream of values from
         *     {@link #asIntStream()}, boxed to {@code Integer}. Consumers that
         *     are able, will obtain improved efficiency by preferring
         *     {@link #asIntStream()} and specialising intermediate processing
         *     to {@code int}.
         */
        @Override
        default Stream<Integer> asStream() { return asIntStream().boxed(); }
    }

    /**
     * This is a helper class for implementations of sequence types. A
     * client sequence implementation may hold an instance of a sub-class of
     * {@code Delegate}, to which it delegates certain operations. This
     * sub-class could be an inner class with access to private members and
     * methods of the client.
     * <p>
     * {@code Delegate} declares abstract or overridable methods
     * representing elementary operations on the client sequence (to get,
     * set or delete an element or slice, or to enquire its length or type).
     * It offers methods based on these that are usable implementations of
     * the Python special methods {@code __getitem__}, {@code __setitem__},
     * {@code __delitem__}, {@code __add__} and {@code __mul__}. It provides
     * the boiler-plate that tends to be the same from one Python type to
     * another &ndash; recognition that an index is a slice, end-relative
     * addressing, index type and range checks, and the raising of
     * index-related Python exceptions. (For examples of this similarity,
     * compare the CPython implementation of {@code list_subscript} with
     * that of {@code bytes_subscript} or any other {@code *_subscript}
     * method.)
     * <p>
     * The client must override abstract methods declared here, in the
     * delegate sub-class it defines, to specialise the behaviour. A
     * sub-class supporting a mutable sequence type must additionally
     * override {@link #setItem(int, Object)},
     * {@link #setSlice(Indices, Object)} and {@link #delSlice(Indices)}. It
     * <i>may</i> also override {@link #delItem(int)}, or rely on the
     * default implementation using {@code delSlice}.
     *
     * @param <E> the element type returned by {@code iterator().next()}
     * @param <S> the slice type, and return type of
     *     {@link #getSlice(Indices)} etc..
     */
    /*
     * This has been adapted from Jython 2 SequenceIndexDelegate and
     * documented.
     */
    static abstract class Delegate<E, S> implements Of<E>, Comparable<Delegate<E, S>> {

        /**
         * Returns the length of the client sequence from the perspective of
         * indexing and slicing operations.
         *
         * @return the length of the client sequence
         */
        @Override
        public abstract int length();

        /**
         * Provide the type of client sequence, primarily for use in error
         * messages e.g. "TYPE index out of bounds".
         *
         * @implNote This can simply return a constant characteristic of the the
         *     implementing class, the Python type implements or supports. E.g
         *     the adaptor for a Java String returns {@code PyUnicode.TYPE}
         *     which is {@code str}.
         *
         * @return the type of client sequence
         */
        public abstract PyType getType();

        /**
         * Return the name of the Python type of the client sequence. This is
         * used in exception messages generated here. By default this is
         * {@code getType().getName()}, which is normally correct, but Python
         * {@code str} likes to call itself "string", exceptionally.
         *
         * @return the name of Python type being served
         */
        public String getTypeName() { return getType().getName(); }

        /**
         * Inner implementation of {@code __getitem__}, called by
         * {@link #__getitem__(Object)} when its argument is an integer. The
         * argument is the equivalent {@code int}, adjusted and checked by
         * {@link #adjustGet(int)}.
         *
         * @param i index of item to return
         * @return the element from the client sequence
         * @throws Throwable from accessing the client data
         */
        public abstract Object getItem(int i) throws Throwable;

        /**
         * Inner implementation of {@code __getitem__}, called by
         * {@link #__getitem__(Object)} when its argument is a {@link PySlice}.
         * The argument is the return from {@link PySlice#getIndices(int)},
         * which is guaranteed to be range-compatible with the sequence length
         * {@link #length()}.
         *
         * @param slice containing [start, stop, step, count] of the slice to
         *     return
         * @return the slice from the client sequence
         * @throws Throwable from accessing the client data
         */
        public abstract S getSlice(PySlice.Indices slice) throws Throwable;

        /**
         * Inner implementation of {@code __setitem__}, called by
         * {@link #__setitem__(Object,Object)} when its argument is an integer.
         * The argument is the equivalent {@code int}, adjusted and checked by
         * {@link #adjustSet(int)}.
         * <p>
         * In mutable types, override this to assign a value to the given
         * element of the client sequence. The default implementation (for
         * immutable types) does nothing.
         *
         * @param i index of item to set
         * @param value to set at {@code i}
         * @throws Throwable from accessing the client data
         */
        public void setItem(int i, Object value) throws Throwable {}

        /**
         * Inner implementation of {@code __setitem__}, called by
         * {@link #__setitem__(Object,Object)} when its argument is a
         * {@link PySlice}. The argument is the return from
         * {@link PySlice#getIndices(int)}, which is guaranteed to be
         * range-compatible with the sequence length {@link #length()}.
         * <p>
         * In mutable types, override this to assign a value to the given slice
         * of the client sequence. The default implementation (for immutable
         * types) does nothing.
         *
         * @param slice to assign in the client sequence
         * @param value to assign
         * @throws Throwable from accessing the client data
         */
        public void setSlice(PySlice.Indices slice, Object value) throws Throwable {}

        /**
         * Inner implementation of {@code __delitem__}, called by
         * {@link #__setitem__(Object,Object)} when its argument is an integer.
         * The argument is the equivalent {@code int}, adjusted and checked by
         * {@link #adjustSet(int)}.
         * <p>
         * The default implementation deletes a slice {@code [i:i+1]} using
         * {@link #delSlice(Indices)}.
         *
         * @param i index of item to delete
         * @throws Throwable from accessing the client data
         */
        public void delItem(int i) throws Throwable {
            PySlice s = new PySlice(i, i + 1);
            delSlice(s.new Indices(length()));
        }

        /**
         * Inner implementation of {@code __delitem__}, called by
         * {@link #__delitem__(Object)} when its argument is a {@link PySlice}.
         * The argument is the return from {@link PySlice#getIndices(int)},
         * which is guaranteed to be range-compatible with the sequence length
         * {@link #length()}.
         * <p>
         * In mutable types, override this to delete the given slice of the
         * client sequence. The default implementation (for immutable types)
         * does nothing.
         *
         * @param slice containing [start, stop, step, count] of the slice to
         *     delete
         * @throws Throwable from accessing the client data
         */
        public void delSlice(PySlice.Indices slice) throws Throwable {}

        /**
         * Inner implementation of {@code __add__} on the client sequence,
         * called by {@link #__add__(Object)}.
         * <p>
         * The implementation of this method is responsible for validating the
         * argument. If an {@code __add__} is being attempted between
         * incompatible types it should return {@link Py#NotImplemented}, or
         * throw a {@link NoConversion} exception, which will cause
         * {@code __add__} to return {@code NotImplemented}.
         *
         * @param ow the right operand
         * @return concatenation {@code self+ow} or {@link Py#NotImplemented}
         * @throws OutOfMemoryError when allocating the result fails.
         *     {@link #__add__(Object) __add__} will raise a Python
         *     {@code OverflowError}.
         * @throws NoConversion (optionally) when the client does not support
         *     the type of {@code ow}.
         * @throws Throwable from other causes in the implementation
         */
        abstract Object add(Object ow) throws OutOfMemoryError, NoConversion, Throwable;

        /**
         * Inner implementation of {@code __radd__} on the client sequence,
         * called by {@link #__radd__(Object)}.
         * <p>
         * The implementation of this method is responsible for validating the
         * argument. If an {@code __radd__} is being attempted between
         * incompatible types it should return {@link Py#NotImplemented}, or
         * throw a {@link NoConversion} exception, which will cause
         * {@code __radd__} to return {@code NotImplemented}.
         *
         * @param ov the left operand
         * @return concatenation {@code ov+self} or {@link Py#NotImplemented}
         * @throws OutOfMemoryError when allocating the result fails.
         *     {@link #__radd__(Object) __radd__} will raise a Python
         *     {@code OverflowError}.
         * @throws NoConversion (optionally) when the client does not support
         *     the type of {@code ov}.
         * @throws Throwable from other causes in the implementation
         */
        abstract Object radd(Object ov) throws OutOfMemoryError, NoConversion, Throwable;

        /**
         * Inner implementation of {@code __mul__} on the client sequence,
         * called by {@link #__mul__(Object)}.
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
         * Implementation of {@code __getitem__}. Get either an element or a
         * slice of the client sequence, after checks, by calling either
         * {@link #getItem(int)} or {@link #getSlice(Indices)}.
         *
         * @param item (or slice) to get from in the client
         * @return the element or slice
         * @throws PyBaseException ({@link PyExc#ValueError ValueError}) if
         *     {@code slice.step==0}
         * @throws PyBaseException ({@link PyExc#TypeError TypeError}) from bad
         *     slice index types
         * @throws Throwable from errors other than indexing
         */
        public Object __getitem__(Object item) throws PyBaseException, Throwable {
            if (PyNumber.indexCheck(item)) {
                int i = PyNumber.asSize(item, (s) -> PyErr.format(PyExc.IndexError, s));
                return getItem(adjustGet(i));
            } else if (item instanceof PySlice) {
                Indices slice = ((PySlice)item).new Indices(length());
                return getSlice(slice);
            } else {
                throw Abstract.indexTypeError(this, item);
            }
        }

        /**
         * Implementation of {@code __setitem__}. Assign a value to either an
         * element or a slice of the client sequence, after checks, by calling
         * either {@link #setItem(int, Object)} or
         * {@link #setSlice(Indices, Object)}.
         *
         * @param item (or slice) to assign in the client
         * @param value to assign
         * @throws PyBaseException ({@link PyExc#ValueError ValueError}) if
         *     {@code slice.step==0} or {@code slice.step!=1} (an "extended"
         *     slice) and {@code value} is the wrong length.
         * @throws PyBaseException ({@link PyExc#TypeError TypeError}) from bad
         *     slice index types
         * @throws Throwable from errors other than indexing
         */
        public void __setitem__(Object item, Object value) throws PyBaseException, Throwable {
            if (PyNumber.indexCheck(item)) {
                int i = PyNumber.asSize(item, (s) -> PyErr.format(PyExc.IndexError, s));
                setItem(adjustSet(i), value);
            } else if (item instanceof PySlice) {
                Indices slice = ((PySlice)item).new Indices(length());
                setSlice(slice, value);
            } else {
                throw Abstract.indexTypeError(this, item);
            }
        }

        /**
         * Implementation of {@code __delitem__}. Delete either an element or a
         * slice of the client sequence, after checks, by calling either
         * {@link #delItem(int)} or {@link #delSlice(Indices)}.
         *
         * @param item (or slice) to delete in the client
         * @throws PyBaseException ({@link PyExc#ValueError ValueError}) if
         *     {@code slice.step==0} or value is the wrong length in an extended
         *     slice ({@code slice.step!=1}
         * @throws PyBaseException ({@link PyExc#TypeError TypeError}) from bad
         *     slice index types
         * @throws Throwable from errors other than indexing
         */
        public void __delitem__(Object item) throws PyBaseException, Throwable {
            if (PyNumber.indexCheck(item)) {
                int i = PyNumber.asSize(item, (s) -> PyErr.format(PyExc.IndexError, s));
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
         * The wrapper attempts no conversion of the argument, but it will catch
         * {@link NoConversion} exceptions from {@link #add(Object)}, to return
         * {@code NotImplemented}. It will also catch Java
         * {@code OutOfMemoryError} and convert it to a Python
         * {@link PyBaseException OverflowError}.
         *
         * @param w right operand
         * @return {@code self+w} or {@code NotImplemented}
         * @throws PyBaseException ({@link PyExc#OverflowError OverflowError})
         *     when cannot allocate space
         * @throws Throwable from other causes in the implementation.
         */
        Object __add__(Object w) throws PyBaseException, Throwable {
            try {
                return add(w);
            } catch (OutOfMemoryError e) {
                throw concatOverflow();
            } catch (NoConversion e) {
                /*
                 * Since we do not implement __concat__ separate from __add_, unlike
                 * CPython, we do not yet know that Object w has no __radd__, and cannot
                 * produce the TypeError "can only concatenate S to S". Instead,
                 * Abstract.add will produce a TypeError about
                 * "unsupported operand types" for '+'.
                 */
                return Py.NotImplemented;
            }
        }

        /**
         * Implementation of {@code __radd__} (reflected concatenation) by
         * calling {@link #radd(Object)}.
         * <p>
         * The wrapper attempts no conversion of the argument, but it will catch
         * {@link NoConversion} exceptions from {@link #radd(Object)}, to return
         * {@code NotImplemented}. It will also catch Java
         * {@code OutOfMemoryError} and convert it to a Python
         * {@link PyBaseException OverflowError}.
         *
         * @param v left operand
         * @return {@code v+self} or {@code NotImplemented}
         * @throws PyBaseException ({@link PyExc#OverflowError OverflowError})
         *     when cannot allocate space
         * @throws Throwable from other causes in the implementation.
         */
        Object __radd__(Object v) throws PyBaseException, Throwable {
            try {
                return radd(v);
            } catch (OutOfMemoryError e) {
                throw concatOverflow();
            } catch (NoConversion e) {
                /*
                 * See comment in __add__, noting that sometimes __radd__ is called
                 * before v.__add__.
                 */
                return Py.NotImplemented;
            }
        }

        /**
         * Implementation of {@code __mul__} (repetition) and {@code __rmul__}
         * by calling {@link #repeat(int)}.
         * <p>
         * The wrapper attempts conversion of the argument to {@code int}, and
         * if this cannot be achieved, it will return {@code NotImplemented}. It
         * will also catch Java {@code OutOfMemoryError} and convert it to a
         * Python {@link PyBaseException OverflowError}.
         *
         * @param n number of repetitions in result
         * @return {@code self*n} or {@code NotImplemented}
         * @throws PyBaseException ({@link PyExc#OverflowError OverflowError})
         *     when {@code n} over-size or cannot allocate space
         * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if
         *     {@code n} has no {@code __index__}
         * @throws Throwable from implementation of {@code __index__}, or other
         *     causes in the implementation.
         */
        Object __mul__(Object n) throws PyBaseException, Throwable {
            if (PyNumber.indexCheck(n)) {
                int count = PyNumber.asSize(n, (s) -> PyErr.format(PyExc.OverflowError, s));
                try {
                    return repeat(count);
                } catch (OutOfMemoryError e) {
                    throw repeatOverflow();
                }
            } else {
                /*
                 * Since we do not implement __repeat__ separate from __mul_, unlike
                 * CPython, we do not yet know that Object n has no __rmul__, so we
                 * cannot produce the TypeError "can't multiply sequence by non-int".
                 * Instead, Abstract.multiply will produce a TypeError about
                 * "unsupported operand types" for '*'.
                 */
                return Py.NotImplemented;
            }
        }

        /**
         * Implementation of the {@code index} method of sequences. Find the
         * index, in the given range, of an element equal to the argument. or a
         * slice of the client sequence, after checks, by calling either
         * {@link #getItem(int)} or {@link #getSlice(Indices)}.
         *
         * @param v value to match in the client
         * @param start index of first element in range
         * @param stop index of first element not in range
         * @return the index at which found
         * @throws PyBaseException ({@link PyExc#ValueError ValueError}) if
         *     {@code v} not found
         * @throws PyBaseException ({@link PyExc#TypeError TypeError}) from bad
         *     {@code start} and {@code stop} types
         * @throws Throwable from errors other than indexing
         */
        public int index(Object v, Object start, Object stop) throws PyBaseException, Throwable {
            int iStart = boundedIndex(start, 0);
            int iStop = boundedIndex(stop, length());
            /*
             * Note it is possible for iStart to be length(), but then iStop<=iStart
             * so the loop doesn't run.
             */
            for (int i = iStart; i < iStop; i++) {
                if (Abstract.richCompareBool(v, getItem(i), Comparison.EQ)) { return i; }
            }
            throw PyErr.format(PyExc.ValueError, "%s.index(x): x not in %s", getType().getName(),
                    getTypeName());
        }

        /**
         * Check that an index {@code i} satisfies 0&le;i&lt;{@link #length()}.
         * If the original index is negative, treat it as end-relative by first
         * adding {@link #length()}.
         *
         * @param i to check is valid index
         * @return range-checked {@code i}
         * @throws PyBaseException ({@link PyExc#IndexError IndexError}) if
         *     {@code i} out of range
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
         * Check that an index {@code i} satisfies 0&le;i&lt;{@link #length()}.
         * If the original index is negative, treat it as end-relative by first
         * adding {@link #length()}. This differs from {@link #adjustGet(int)}
         * only in that the message produced mentions "assignment".
         *
         * @param i to check is valid index
         * @return range-checked {@code i}
         * @throws PyBaseException ({@link PyExc#IndexError IndexError}) if
         *     {@code i} out of range
         */
        protected int adjustSet(int i) throws PyBaseException {
            final int L = length();
            if (i < 0) {
                i += L;
                if (i >= 0) { return i; }
            } else if (i < L) { return i; }
            throw rangeIndexError("assignment");
        }

        /**
         * Creates an {@link PyBaseException IndexError} with the message
         * "{@link #getTypeName()} KIND index out of range", e.g. "list
         * assignment index out of range".
         *
         * @param kind word to insert for KIND: "" or "assignment".
         * @return an exception to throw
         */
        final protected PyBaseException rangeIndexError(String kind) {
            String space = kind.length() > 0 ? " " : "";
            return PyErr.format(PyExc.IndexError, "%s%s%s index out of range", getTypeName(), space,
                    kind);
        }

        /**
         * An overflow error with the message "concatenated
         * {@link #getTypeName()} is too long", involving the type name of the
         * client sequence type.
         *
         * @param seq the sequence operated on
         * @return an exception to throw
         */
        private PyBaseException concatOverflow() {
            return PyErr.format(PyExc.OverflowError, "concatenated %s is too long", getTypeName());
        }

        /**
         * An overflow error with the message "repeated S is too long", where S
         * is the type mane of the argument.
         *
         * @return an exception to throw
         */
        private PyBaseException repeatOverflow() {
            return PyErr.format(PyExc.OverflowError, "repeated %s is too long", getTypeName());
        }

        /**
         * Accept an object index (or {@code null}), treating negative values as
         * end-relative, and bound it to the sequence range. The index object
         * must be convertible by
         * {@link PyNumber#asSize(Object, java.util.function.Function)
         * PyNumber.asSize}. Unlike {@link #adjustGet(int)}, is not an error for
         * the index value to fall outside the valid range. (It is simply
         * clipped to the nearer end.)
         *
         * @param index purported index (or {@code null})
         * @param defaultValue to use if {@code index==null}
         * @return converted index
         * @throws PyBaseException ({@link PyExc#TypeError TypeError}) from bad
         *     {@code index} type
         * @throws Throwable from other conversion errors
         */
        protected int boundedIndex(Object index, int defaultValue)
                throws PyBaseException, Throwable {

            // Convert the argument (or raise a TypeError)
            int i, L = length();
            if (index == null) {
                i = defaultValue;
            } else if (PyNumber.indexCheck(index)) {
                i = PyNumber.asSize(index, (s) -> PyErr.format(PyExc.IndexError, s));
            } else {
                throw Abstract.indexTypeError(this, index);
            }

            // Bound the now integer index to the sequence
            if (i < 0) {
                i += L;
                return Math.max(0, i);
            } else {
                return Math.min(L, i);
            }
        }
    }

    // Plumbing -------------------------------------------------------

    /**
     * Create a collection of the given kind with the same contents as the
     * sequence or iterable {@code o}.
     *
     * @param <R> the type of collection
     * @param o to represent
     * @param factory a constructor for {@code R}
     * @param accumulator to add one element to an {@code R}
     * @return the collection
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if an
     *     iterator cannot be formed on {@code o}
     * @throws Throwable from the implementation of {@code o}
     */
    private static <R> R collect(Object o, Supplier<R> factory, BiConsumer<R, Object> accumulator)
            throws PyBaseException, Throwable {

        // Create an iterator on o and a bound handle on __next__
        Object iter = getIterator(o);
        Representation iterRep = representation(iter);
        MethodHandle next = iterRep.op_next().bindTo(iter);

        // Iterate o into the collection
        R r = factory.get();
        try {
            for (;;) { accumulator.accept(r, next.invokeExact()); }
        } catch (PyStopIteration stop) {
            // Not a problem. (Every other exception is.)
        }
        return r;
    }
}
