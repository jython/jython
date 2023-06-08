// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;

import org.python.base.InterpreterError;
import org.python.core.PyObjectUtil.NoConversion;
import org.python.core.PySlice.Indices;
import org.python.core.PyType.Spec;

/**
 * The Python {@code list} object that is also a Java
 * {@code List<Object>}. Operations in both APIs are synchronised
 * for safety against concurrent threads.
 * <p>
 * It is {@code synchronized} so that competing threads should be
 * able to access it with roughly the same protection against
 * concurrent modification that CPython offers. There are also
 * necessary safeguards during {@code sort()} to detect modification
 * from within the current thread as a side effect of comparison.
 * Java brings its own safeguard within iterators against structural
 * concurrent modification.
 *
 * @implNote The design follows that in Jython 2 with a private Java
 *     list member to which operations are delegated directly or
 *     indirectly. In the present design, the indirect delegation is
 *     through a private delegate member where in the former design
 *     behaviour was inherited.
 */
// @ExposedType(name = "list", base = PyObject.class, doc = BuiltinDocs.list_doc)
public class PyList implements List<Object>, CraftedPyObject {

    public static final PyType TYPE =
            PyType.fromSpec(new Spec("list", MethodHandles.lookup()));

    /** The Python type of this instance. */
    protected final PyType type;

    /** Storage for the actual list elements (as a list). */
    private final List<Object> list;

    /** Implementation help for sequence methods. */
    private final ListDelegate delegate = new ListDelegate();

    /**
     * Synchronisation prevents modification by competing threads during
     * an operation, but does not defend against actions within that
     * operation by the same thread. One context where this occurs is
     * {@code sort()}, where the implementation of {@code __lt__} on
     * elements can, in principle, mutate the list being sorted. This
     * variable is cleared at the beginning of {@code sort()} and set by
     * any operation that modifies the list.
     */
    private boolean changed = false;

    /**
     * Fundamental constructor, specifying actual type and the list that
     * will become the storage object.
     *
     * @param type actual type
     * @param list storage object
     */
    private PyList(PyType type, List<Object> list) {
        this.type = type;
        this.list = list;
    }

    /**
     * Construct a Python {@code list} object, specifying actual type
     * and initial capacity.
     *
     * @param type actual type
     * @param initialCapacity capacity
     */
    public PyList(PyType type, int initialCapacity) {
        this(type, new ArrayList<>(initialCapacity));
    }

    /**
     * Construct a Python {@code list} object, specifying initial
     * capacity.
     *
     * @param initialCapacity capacity
     */
    public PyList(int initialCapacity) { this(TYPE, new ArrayList<>(initialCapacity)); }

    /**
     * Construct an empty Python {@code list} object, specifying actual
     * type.
     *
     * @param type actual type
     */
    public PyList(PyType type) { this(type, 0); }

    /** Construct an empty Python {@code list} object. */
    public PyList() { this(TYPE, 0); }

    /**
     * Construct a Python {@code list} object, specifying actual type
     * and initial contents. The contents will be a (shallow) copy of
     * the collection.
     *
     * @param type actual type
     * @param c initial contents
     */
    public PyList(PyType type, Collection<?> c) {
        this(type, c.size());
        addAll(c);
    }

    /**
     * Construct a Python {@code list} object, specifying initial
     * contents. The contents will be a (shallow) copy of the
     * collection.
     *
     * @param c initial contents
     */
    public PyList(Collection<?> c) { this(TYPE, c); }

    /**
     * Construct a {@code list} with initial contents from an array
     * slice.
     *
     * @param a the array
     * @param start of slice
     * @param count of elements to take
     */
    PyList(Object[] a, int start, int count) {
        this(TYPE, count);
        int stop = start + count;
        for (int i = start; i < stop; i++) { add(a[i]); }
    }

    /**
     * Return a Python {@code list} object, specifying initial
     * contents.
     *
     * @param elements initial element values
     * @return list of elements
     */
    public static PyList of(Object... elements) { return new PyList(List.of(elements)); }

    @Override
    public PyType getType() { return type; }

    // Special methods -----------------------------------------------

    // @ExposedNew
    // @ExposedMethod(doc = BuiltinDocs.list___init___doc)
    // @formatter:off
    /*
    void __init__(Object[] args, String[] kwds) {
        ArgParser ap = new ArgParser("list", args, kwds, new String[]{"sequence"}, 0);
        Object seq = ap.getPyObject(0, null);
        clear();
        if (seq == null) {
            return;
        }

        /* PyListDerived should be iterated over and not plain copied for cases where someone subclasses list
        and overrides __iter__
         * /
        if (seq instanceof PyListDerived) {
            for (Object item : seq.asIterable()) {
                append(item);
            }
        } else if (seq instanceof PyList) {
            list.addAll(((PyList) seq).list); // don't convert
        } else if (seq instanceof PyList) {
            list.addAll((PyTuple) seq);
        } else {
            for (Object item : seq.asIterable()) {
                append(item);
            }
        }
    }
    */
    // @formatter:on

    // @ExposedMethod(doc = BuiltinDocs.list___len___doc)
    synchronized int __len__() {
        return size();
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___ne___doc)
    synchronized Object __ne__(Object o) {
        return delegate.cmp(o, Comparison.NE);
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___eq___doc)
    synchronized Object __eq__(Object o) {
        return delegate.cmp(o, Comparison.EQ);
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___lt___doc)
    synchronized Object __lt__(Object o) {
        return delegate.cmp(o, Comparison.LT);
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___le___doc)
    synchronized Object __le__(Object o) {
        return delegate.cmp(o, Comparison.LE);
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___gt___doc)
    synchronized Object __gt__(Object o) {
        return delegate.cmp(o, Comparison.GT);
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___ge___doc)
    synchronized Object __ge__(Object o) {
        return delegate.cmp(o, Comparison.GE);
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___imul___doc)
    // @formatter:off
    /*
    synchronized Object __imul__(Object o) {
        if (!o.isIndex()) {
            return null;
        }
        int count = o.asIndex(Py.OverflowError);

        int size = size();
        if (size == 0 || count == 1) {
            return this;
        }

        if (count < 1) {
            clear();
            return this;
        }

        if (size > Integer.MAX_VALUE / count) {
            throw new MemoryError("");
        }

        int newSize = size * count;
        if (list instanceof ArrayList) {
            ((ArrayList<Object>) list).ensureCapacity(newSize);
        }
        List<Object> oldList = new ArrayList<Object>(list);
        for (int i = 1; i < count; i++) {
            list.addAll(oldList);
        }
        gListAllocatedStatus = list.size(); // now omit?
        return this;
    }
    */
    // @formatter:on

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___mul___doc)
    synchronized Object __mul__(Object n) throws Throwable { return delegate.__mul__(n); }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___rmul___doc)
    synchronized Object __rmul__(Object n) throws Throwable { return delegate.__mul__(n); }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___add___doc)
    synchronized Object __add__(Object o) throws Throwable { return delegate.__add__(o); }

    // @ExposedMethod(type = MethodType.BINARY)
    synchronized Object __radd__(Object o) throws Throwable { return delegate.__radd__(o); }

    // @ExposedMethod(doc = BuiltinDocs.list___contains___doc)
    synchronized boolean __contains__(Object o) throws Throwable {
        return delegate.__contains__(o);
    }

    // @ExposedMethod(doc = BuiltinDocs.list___delitem___doc)
    synchronized void __delitem__(Object index) throws Throwable {
        changed = true;
        delegate.__delitem__(index);
    }

    // @ExposedMethod(doc = BuiltinDocs.list___setitem___doc)
    synchronized void __setitem__(Object index, Object value) throws Throwable {
        changed = true;
        delegate.__setitem__(index, value);
    }

    // @ExposedMethod(doc = BuiltinDocs.list___getitem___doc)
    synchronized Object __getitem__(Object index) throws Throwable {
        return delegate.__getitem__(index);
    }

    // @formatter:off
    /*
    // @ExposedMethod(doc = BuiltinDocs.list___iter___doc)
    Object __iter__() {
        return new PyListIterator(this);
    }

    // @ExposedMethod(doc = BuiltinDocs.list___reversed___doc)
    synchronized PyIterator __reversed__() {
        return new PyReversedIterator(this);
    }
    */
    // @formatter:on

    @Override
    public String toString() {
        // XXX Use repr for elements and guard against recursive references
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (Object v : list) { sj.add(v.toString()); }
        return sj.toString();
    }

    // XXX object.__repr__ (calls toString()) should be enough.
    // Retaining this code to indicate toString() additions needed.
    // @formatter:off
    /*
    // @ExposedMethod(names = "__repr__")
    final synchronized String __repr__() {
        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) {
            return "[...]";
        }
        StringBuilder buf = new StringBuilder("[");
        int length = size();
        int i = 0;
        for (Object item : list) {
            buf.append(item.__repr__().toString());
            if (i < length - 1) {
                buf.append(", ");
            }
            i++;
        }
        buf.append("]");
        ts.exitRepr(this);
        return buf.toString();
    }
    */
    // @formatter:on

    /**
     * Add a single element to the end of list.
     *
     * @param o the element to add.
     */
    // @ExposedMethod(doc = BuiltinDocs.list_append_doc)
    final synchronized void list_append(Object o) {
        changed = true;
        list.add(o);
    }

    /**
     * Remove all items from the list (same as {@code del s[:]})
     */
    // @ExposedMethod in Python 3
    final synchronized void list_clear() {
        changed = true;
        list.clear();
    }

    /**
     * Return the number elements in the list that are Python-equal to
     * the argument.
     *
     * @param v the value to test for.
     * @return the number of occurrences.
     * @throws Throwable from the implementation of {@code __eq__}
     */
    // @ExposedMethod(doc = BuiltinDocs.list_count_doc)
    final synchronized int list_count(Object v) throws Throwable { return delegate.count(v); }

    /**
     * Return smallest index where an element in the list Python-equals the argument.
     *
     * @param v the value to look for.
     * @param start first index to test
     * @param stop first index not to test
     * @return index of the occurrence
     * @throws ValueError if {@code v} not found
     * @throws TypeError from bad {@code start} and {@code stop} types
     * @throws Throwable from errors other than indexing
     */
    // @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.list_index_doc)
    final synchronized int list_index(Object v, Object start, Object stop) throws TypeError, Throwable {
        return delegate.index(v, start, stop);
    }

    /**
     * Insert the argument element into the list at the specified index.
     * Same as {@code s[index:index] = [o] if index >= 0}.
     *
     * @param index the position where the element will be inserted.
     * @param o the element to insert.
     * @throws TypeError from bad {@code index} type
     * @throws Throwable from other conversion errors
     */
    // @ExposedMethod(doc = BuiltinDocs.list_insert_doc)
    final synchronized void list_insert(Object index, Object o) throws TypeError, Throwable {
        changed = true;
        delegate.insert(index, o);
    }

    /**
     * Remove from the list the first element that is Python-equal to
     * the argument. Same as {@code del s[s.index(x)]}.
     *
     * @param v the element to search for and remove.
     * @throws Throwable from the implementation of {@code __eq__}
     */
    // @ExposedMethod(doc = BuiltinDocs.list_remove_doc)
    final synchronized void list_remove(Object v) throws Throwable {
        int i = find(v);
        if (i >= 0) {
            changed = true;
            list.remove(i);
        } else {
            throw new ValueError("%s.remove(x): x not in list", getType().name);
        }
    }

    /**
     * Return the index of {@code v} in {@link #list} or -1 if not
     * found.
     *
     * @param v the element to search for and remove.
     * @return the index of {@code v} or -1 if not found.
     * @throws Throwable from the implementation of {@code __eq__}
     */
    private int find(Object v) throws Throwable {
        int n = list.size();
        for (int i = 0; i < n; i++) {
            if (Abstract.richCompareBool(v, list.get(i), Comparison.EQ)) { return i; }
        }
        return -1;
    }

    /**
     * Reverses the items of s in place. The reverse() methods modify the list in place for economy
     * of space when reversing a large list. It doesn't return the reversed list to remind you of
     * this side effect.
     */
    // @ExposedMethod(doc = BuiltinDocs.list_reverse_doc)
    final synchronized void reverse() {
        Collections.reverse(list);
        changed = true;
    }

    /**
     * Remove and return a specified element from the list.
     *
     * @param n the index of the element to remove and return.
     * @return the popped item
     */
    // @ExposedMethod(defaults = "-1", doc = BuiltinDocs.list_pop_doc)
    final synchronized Object list_pop(int n) {
        int size = size();
        if (size == 0) {
            throw new IndexError("pop from empty list");
        } else {
            if (n < 0) { n += size; }
            if (n < 0 || n >= size) { throw new IndexError("pop index out of range"); }
            changed = true;
            return list.remove(n);
        }
    }

    /**
     * Append the elements in the argument sequence to the end of the
     * list, {@code s[len(s):len(s)] = o}.
     *
     * @param o the sequence of items to append to the list.
     * @throws Throwable from attempting to get an iterator on {@code o}
     */
    // @ExposedMethod(doc = BuiltinDocs.list_extend_doc)
    final synchronized void list_extend(Object o) throws Throwable {
        list_extend(o, null);
    }

    /**
     * Append the elements in the argument sequence to the end of the
     * list, {@code s[len(s):len(s)] = o}.
     *
     * @param <E> the type of exception to throw
     * @param o the sequence of items to append to the list.
     * @param exc a supplier (e.g. lambda expression) for the exception
     *     to throw if an iterator cannot be formed (or {@code null} for
     *     a default {@code TypeError})
     * @throws E to throw if an iterator cannot be formed
     * @throws Throwable from the implementation of {@code o}.
     */
    final <E extends PyException> void list_extend(Object o, Supplier<E> exc) throws E, Throwable {
        changed = true;
        list.addAll(PySequence.fastList(o, exc));
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___iadd___doc)
    synchronized Object __iadd__(Object o) throws Throwable {
        changed = true;
        list_extend(o);
        return this;
    }

    /**
     * Sort the items of the list in place, using only &lt; comparisons
     * between items. Exceptions are not suppressed &mdash; if any
     * comparison operations fail, the entire sort operation will fail
     * (and the list will likely be left in a partially modified state).
     *
     * @param key specifies a function of one argument that is used to
     *     extract a comparison key from each list element, e.g.
     *     {@code key=str.lower}. The default value is {@code None}.
     * @param reverse the list elements are sorted as if each comparison
     *     were reversed.
     * @throws Throwable from object comparison
     */
    // @ExposedMethod(doc = BuiltinDocs.list_sort_doc)
    final synchronized void sort(Function<Object, Object> key, boolean reverse)
            throws Throwable {
        // Python: sort(*, key=None, reverse=False)
        if (key == null) {
            sortOnValue(reverse);
        } else {
            sortOnKey(key, reverse);
        }
    }

    private synchronized void sortOnValue(boolean reverse) throws Throwable {

        // We shall sort on values using this (Python) comparator
        final ListElementComparator<Object> cmp = new ListElementComparator<Object>(reverse) {

            @Override
            boolean lessThan(Object o1, Object o2) throws Throwable {
                return Abstract.richCompareBool(o1, o2, Comparison.LT);
            }

        };

        // Now sort the list, failing on any change
        changed = false;
        Collections.sort(list, cmp);
        if (cmp.raisedException()) { throw cmp.getRaisedException(); }
        changed = true;
    }

    /**
     * During {@link PyList#sortOnKey(Function, boolean)}, we actually
     * sort a list of these key-value objects.
     */
    private static class KV {

        private final Object key;
        private final Object value;

        KV(Object key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

    private synchronized void sortOnKey(Function<Object, Object> keyfunc, boolean reverse)
            throws Throwable {

        // Make a copy of the list as key-value pairs in kvList
        int size = list.size();
        final ArrayList<KV> kvList = new ArrayList<KV>(size);
        for (Object value : list) {
            Object k = keyfunc.apply(value);
            kvList.add(new KV(k, value));
        }

        // We shall sort kvList comparing keys
        final ListElementComparator<KV> cmp = new ListElementComparator<KV>(reverse) {

            @Override
            boolean lessThan(KV o1, KV o2) throws Throwable {
                return Abstract.richCompareBool(o1.key, o2.key, Comparison.LT);
            }

        };

        // Now sort the kvList, failing on any change
        changed = false;
        Collections.sort(kvList, cmp);
        if (cmp.raisedException()) { throw cmp.getRaisedException(); }

        // Copy values from kvList (sorted on key)
        assert kvList.size() == size;
        for (int i = 0; i < size; i++) { list.set(i, kvList.get(i).value); }
        changed = true;
    }

    public PyTuple __getnewargs__() { return new PyTuple(new PyTuple(list)); }

    // List interface ------------------------------------------------

    @Override
    public synchronized void add(int index, Object element) {
        changed = true;
        list.add(index, element);
    }

    @Override
    public synchronized boolean add(Object o) {
        changed = true;
        return list.add(o);
    }

    @Override
    public synchronized boolean addAll(int index, Collection<?> c) {
        changed = true;
        return list.addAll(index, c);
    }

    @Override
    public boolean addAll(Collection<?> c) {
        changed = true;
        return addAll(0, c);
    }

    @Override
    public synchronized void clear() {
        changed = true;
        list.clear();
    }

    @Override
    public synchronized boolean contains(Object o) {
        try {
            // Use the Python definition of equality (which may throw)
            return delegate.__contains__(o);
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        try {
            for (Object o : c) {
                // Use the Python definition of equality (which may throw)
                if (!delegate.__contains__(o)) { return false; }
            }
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public synchronized Object get(int index) {
        return list.get(index);
    }

    @Override
    public synchronized int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public synchronized boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {

            private final Iterator<Object> iter = list.iterator();

            @Override
            public boolean hasNext() { return iter.hasNext(); }

            @Override
            public Object next() {
                synchronized (PyList.this) {
                    return iter.next();
                }
            }

            @Override
            public void remove() {
                synchronized (PyList.this) {
                    changed = true;
                    iter.remove();
                }
            }
        };
    }

    @Override
    public synchronized int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<Object> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<Object> listIterator(final int index) {
        return new ListIterator<Object>() {

            private final ListIterator<Object> iter = list.listIterator(index);

            @Override
            public boolean hasNext() { return iter.hasNext(); }

            @Override
            public Object next() {
                synchronized (PyList.this) {
                    return iter.next();
                }
            }

            @Override
            public boolean hasPrevious() { return iter.hasPrevious(); }

            @Override
            public Object previous() {
                synchronized (PyList.this) {
                    return iter.previous();
                }
            }

            @Override
            public int nextIndex() { return iter.nextIndex(); }

            @Override
            public int previousIndex() { return iter.previousIndex(); }

            @Override
            public void remove() {
                synchronized (PyList.this) {
                    changed = true;
                    iter.remove();
                }
            }

            @Override
            public void set(Object o) {
                synchronized (PyList.this) {
                    changed = true;
                    iter.set(o);
                }
            }

            @Override
            public void add(Object o) {
                synchronized (PyList.this) {
                    changed = true;
                    iter.add(o);
                }
            }

        };
    }

    @Override
    public synchronized Object remove(int index) {
        changed = true;
        return list.remove(index);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        // Make a list of indices at which to remove an item
        List<Integer> erasures = new ArrayList<>(c.size());
        int n = list.size();
        for (int i = 0; i < n; i++) {
            Object item = list.get(i);
            for (Object o : c) {
                // Use the Python definition of equality (which may throw)
                try {
                    if (Abstract.richCompareBool(o, item, Comparison.EQ)) {
                        erasures.add(i);
                        break;
                    }
                } catch (Throwable e) {
                    // Treat as non-match
                }
            }
        }
        // Do the removal
        return erase(erasures);
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        // Make a list of indices at which to remove an item
        List<Integer> erasures = new ArrayList<>(c.size());
        int n = list.size();
        for (int i = 0; i < n; i++) {
            Object item = list.get(i);
            boolean retain = false;
            for (Object o : c) {
                // Use the Python definition of equality (which may throw)
                try {
                    if (Abstract.richCompareBool(o, item, Comparison.EQ)) {
                        retain = true;
                        break;
                    }
                } catch (Throwable e) {
                    // Treat as non-match
                }
            }
            if (!retain) { erasures.add(i); }
        }
        // Do the removal
        return erase(erasures);
    }

    @Override
    public synchronized Object set(int index, Object element) {
        changed = true;
        return list.set(index, element);
    }

    @Override
    public synchronized int size() {
        return list.size();
    }

    @Override
    public synchronized List<Object> subList(int fromIndex, int toIndex) {
        /*
         * XXX There is a difficulty here in our management of concurrency.
         * The sub-list is an *unsynchronised* view on this PyList's private
         * list variable, so it bypasses the synchronisation in the PyList
         * wrapper. Here, and in Jython 2, we wrap this view in a new
         * PyList. Although that is synchronised (on itself), it is not
         * synchronised on this PyList and concurrent access is possible
         * through the two objects. Compare
         * java.util.Collections.SynchronizedRandomAccessList<E>, where the
         * problem is solved by sharing a mutex.
         */
        return new PyList(TYPE, list.subList(fromIndex, toIndex));
    }

    @Override
    public synchronized Object[] toArray() { return list.toArray(); }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T> T[] toArray(T[] a) {

        Class<T> type = (Class<T>)a.getClass().getComponentType();

        if (Object.class.equals(type)) {
            // Special-case a request for Object[]
            a = list.toArray(a);

        } else {
            // Ensure we have a space the right size
            int size = size();
            if (a.length < size) {
                a = (T[])Array.newInstance(type, size);
            } else if (a.length > size) { a[size] = null; }

            // Copy list into the array with conversion to T
            for (int i = 0; i < size; i++) { a[i] = Abstract.tojava(list.get(i), type); }
        }
        return a;
    }

    @Override
    public synchronized boolean remove(Object o) {
        changed = true;
        return list.remove(o);
    }

    // Java hash and equals ------------------------------------------

    @Override
    public int hashCode() { return list.hashCode(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        synchronized (this) {
            if (other instanceof List) {
                return list.equals(other);
            } else {
                try {
                    return Abstract.richCompareBool(other, this, Comparison.EQ);
                } catch (Throwable t) {
                    return false;
                }
            }
        }
    }

    // Supporting code -----------------------------------------------

    /**
     * This comparator is used in
     * {@link PyList#sort(Function, boolean)}, sub-classed for the type
     * of sort.
     *
     * @param <T> type of element to sort (on practice {@code Object} or
     *     {@code KV}.
     */
    private abstract class ListElementComparator<T> implements Comparator<T> {

        private Throwable comparatorException;
        private final int less;

        /**
         * @param reverse whether comparisons will be in reveres sense
         */
        ListElementComparator(boolean reverse) { this.less = reverse ? 1 : -1; }

        /**
         * @return the exception raised by any application of
         *     {@link #lessThan(Object, Object)}
         */
        Throwable getRaisedException() {
            return comparatorException;
        }

        /**
         * @return whether an exception was raised by any application of
         *     {@link #lessThan(Object, Object)}
         */
        boolean raisedException() {
            return comparatorException != null;
        }

        /**
         * Defines the comparison operation to use.
         *
         * @param o1 left operand
         * @param o2 right operand
         * @return true iff o1 is less than o2
         * @throws Throwable on errors in the comparison
         */
        abstract boolean lessThan(T o1, T o2) throws Throwable;

        @Override
        public int compare(T o1, T o2) {
            try {
                // PEP 207: sort should only depend on "less-than"
                return lessThan(o1, o2) ? less : (lessThan(o2, o1) ? -less : 0);
            } catch (Throwable pye) {
                // Stash the exception to rethrow later
                comparatorException = pye;
                // XXX made up answer may violate contract of compare. Why stash?
                return 0;
            } finally {
                // Detect change occurring during the comparisons
                if (changed) { throw new ValueError("list modified during sort"); }
            }
        }
    }

    /**
     * Accept an index, treating negative values as end-relative, and
     * bound it to the sequence range. It is not an error for the index
     * value to fall outside the valid range. (It is simply clipped to
     * the nearer end.)
     *
     * @param index as presented
     * @return bounded {@code 0 <= index <= list.size()}
     */
    private int boundedIndex(int index) {
        int L = list.size();
        return index < 0 ? Math.max(0, index + L) : Math.min(L, index);
    }

    /**
     * Given an ordered ascending list of indices into {@link #list},
     * remove the elements at those indices.
     *
     * @param erasures to remove
     * @return {@code true} if {@code erasures} is not empty
     */
    private boolean erase(List<Integer> erasures) {
        // Copy list to itself skipping each erasure
        if (erasures.isEmpty()) { return false; }
        Iterator<Integer> ei = erasures.iterator();
        changed = true;

        /*
         * p, q are pointers into the list. We copy elements from list[q] to
         * list[p], for p<q. r is the position of the next erasure. Whenever
         * q reaches an erasure, we advance q over it without copy. Up to
         * the first erasure, no copies are necessary, so we start like
         * this, at he first erasure:
         */
        int r = ei.next(), p = r, q = r + 1, n = list.size();
        while (ei.hasNext()) {
            // r is the position of the next erasure
            r = ei.next();
            while (q < r) { list.set(p++, list.get(q++)); }
            q = r + 1;
        }

        // Copy elements after the last erasure.
        while (q < n) { list.set(p++, list.get(q++)); }

        // Trim now redundant elements from the end.
        list.subList(p, n).clear();
        return true;
    }

    // Delegate class ------------------------------------------------

    /**
     * Wrap the {@link #list} of this {@code PyList} as a
     * {@link PySequence.Delegate}, for the management of indexing and
     * other sequence operations. Note that this class is not
     * synchronised, but relies on the caller holding the lock on the
     * containing {@code PyList} method.
     */
    private class ListDelegate extends PySequence.Delegate<Object, PyList> {
        @Override
        public int length() { return list.size(); };

        @Override
        public PyType getType() { return PyList.this.getType(); }

        @Override
        public Object getItem(int i) { return list.get(i); }

        @Override
        public Object get(int i) { return list.get(i); }

        @Override
        public PyList getSlice(Indices slice) throws Throwable {
            PyList v;
            if (slice.step == 1)
                v = new PyList(list.subList(slice.start, slice.stop));
            else {
                v = new PyList(TYPE, slice.slicelength);
                int i = slice.start;
                for (int j = 0; j < slice.slicelength; j++) {
                    v.add(list.get(i));
                    i += slice.step;
                }
            }
            return v;
        }

        @Override
        public void setItem(int i, Object value) throws Throwable { list.set(i, value); }

        @Override
        public void setSlice(PySlice.Indices slice, Object value) throws Throwable {
            /*
             * Accept iterables (and iterators) by creating a Java List Jython 2
             * would also try __tojava__. Necessary?
             */
            List<Object> v = PySequence.fastList(value,
                    () -> new TypeError("can only assign an iterable to a slice"));
            if (v == PyList.this) { v = new ArrayList<>(list); } // self[slice] = self

            // Now we have a List of values to assign
            final int N = v.size(), M = slice.slicelength, D = N - M;
            final int step = slice.step;
            ;
            if (D != 0) {
                if (step == 1) {
                    // Contiguous slice requiring change of size
                    final int start = slice.start, stop = slice.stop;
                    if (D < 0) {
                        // v shorter by (-D) than slice it replaces.
                        // Discard (-D) elements [stop-(-D):stop]
                        list.subList(stop + D, stop).clear();
                        // Copy N elements
                        Collections.copy(list.subList(start, start + N), v);
                    } else {
                        // v is longer by D than slice it replaces.
                        // Copy M elements to [start:stop-D]
                        Collections.copy(list.subList(start, start + M), v.subList(0, M));
                        // Insert the last D elements of v at the slice stop.
                        list.addAll(stop, v.subList(N - D, N));
                    }
                } else {
                    // Extended slice, but not the same size as the value
                    throw new ValueError(
                            "attempt to assign sequence of size %d to extended slice of size %d", N,
                            M);
                }
            } else {
                // Extended or contiguous slice of just the right size
                for (int i = 0, k = slice.start; i < M; i++, k += step) { list.set(k, v.get(i)); }
            }
        }

        @Override
        public void delItem(int i) throws Throwable {
            list.remove(i);
        }

        @Override
        public void delSlice(PySlice.Indices slice) throws Throwable {
            final int M = slice.slicelength;
            if (M > 0) {
                final int step = slice.step;
                /*
                 * We will step through the list removing items. If we are stepping
                 * backwards, the increments will be slice.step (negative), but if
                 * stepping forwards, slice.step-1, because the items to right will
                 * have moved one closer, due to the deletion. (There are faster
                 * ways, but how often will it matter?)
                 */
                final int inc = step > 0 ? step - 1 : step;
                if (inc == 0) {
                    // Contiguous slice
                    list.subList(slice.start, slice.stop).clear();
                } else {
                    // Extended slice
                    for (int i = 0, k = slice.start; i < M; i++, k += inc) { list.remove(k); }
                }
            }
        }

        @Override
        Object add(Object ow) throws NoConversion {
            // We accept any sort of Java list as w except tuple
            if (ow instanceof List<?> && !(ow instanceof PyTuple)) {
                return PyList.concat(list, (List<?>)ow);
            } else {
                return Py.NotImplemented;
            }
        }

        @Override
        Object radd(Object ov) throws NoConversion {
            // We accept any sort of Java list as v except tuple
            if (ov instanceof List && !(ov instanceof PyTuple)) {
                return PyList.concat((List<?>)ov, list);
            } else {
                return Py.NotImplemented;
            }
        }

        @Override
        PyList repeat(int n) {
            ArrayList<Object> u = new ArrayList<>(n * list.size());
            for (int i = 0; i < n; i++) { u.addAll(list); }
            return new PyList(TYPE, u);
        }

        /**
         * {@inheritDoc}
         * <p>
         * The iterator returned for the {@code ListDelegate} is
         * specifically a {@code ListIterator<Object>}.
         */
        @Override
        public ListIterator<Object> iterator() { return list.listIterator(); }

        /**
         * Implementation of the {@code __contains__} method of sequences.
         * Determine whether the sequence contains an element equal to the
         * argument.
         *
         * @param v value to match in the client
         * @return whether found
         * @throws Throwable from the implementation of {@code __eq__}
         */
        // XXX Could this be supplied by PySequence.Delegate?
        public boolean __contains__(Object v) throws Throwable {
            // XXX What about changes to content during iteration?
            for (Object item : this) {
                if (Abstract.richCompareBool(v, item, Comparison.EQ)) { return true; }
            }
            return false;
        }

        /**
         * Implementation of the {@code count} method of sequences.
         * Determine the number of times the sequence contains an element
         * equal to the argument.
         *
         * @param v value to match in the client
         * @return the number of times found
         * @throws Throwable from the implementation of {@code __eq__}
         */
        // XXX Could this be supplied by PySequence.Delegate?
        public int count(Object v) throws Throwable {
            int count = 0;
            // XXX What about changes to content during iteration?
            for (Object item : this) {
                if (Abstract.richCompareBool(v, item, Comparison.EQ)) { count++; }
            }
            return count;
        }

        /**
         * Implementation of the {@code insert} method of sequences.
         *
         * @param index position to insert
         * @param v value to insert
         * @return the number of times found
         * @throws TypeError from bad {@code index} type
         * @throws Throwable from other conversion errors
         */
        public void insert(Object index, Object v) throws TypeError, Throwable {
            list.add(boundedIndex(index), v);
        }

        /**
         * Accept an object index, treating negative values as end-relative,
         * and bound it to the sequence range. The index object must be
         * convertible by
         * {@link PyNumber#asSize(Object, java.util.function.Function)
         * PyNumber.asSize}. It is not an error for the index value to fall
         * outside the valid range. (It is simply clipped to the nearer
         * end.)
         *
         * @param index purported index (not {@code null})
         * @return converted index
         * @throws TypeError from bad {@code index} type
         * @throws Throwable from other conversion errors
         */
        protected int boundedIndex(Object index) throws TypeError, Throwable {

            // Convert the argument (or raise a TypeError)
            int i, L = length();
            if (PyNumber.indexCheck(index)) {
                i = PyNumber.asSize(index, IndexError::new);
            } else {
                throw Abstract.indexTypeError(this, index);
            }

            // Bound the now integer index to the sequence (or L)
            return i < 0 ? Math.max(0, i + L) : Math.min(L, i);
        }

        @Override
        public int compareTo(PySequence.Delegate<Object, PyList> other) {
            try {
                int N = list.size(), M = other.length(), i;

                for (i = 0; i < N; i++) {
                    Object a = list.get(i);
                    if (i < M) {
                        Object b = other.getItem(i);
                        // if a != b, then we've found an answer
                        if (!Abstract.richCompareBool(a, b, Comparison.EQ))
                            return Abstract.richCompareBool(a, b, Comparison.GT) ? 1 : -1;
                    } else
                        // list has not run out, but other has. We win.
                        return 1;
                }

                /*
                 * The lists matched over the length of Pylist.this.list. The other
                 * is the winner if it still has elements. Otherwise it's a tie.
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
         * Compare this delegate with the delegate of the other {@code list}
         * for equality. We do this separately from
         * {@link #cmp(Object, Comparison)} because it is slightly cheaper,
         * but also because so we don't panic where an element is capable of
         * an equality test, but not a less-than test.
         *
         * @param other delegate of list at right of comparison
         * @return {@code true} if equal, {@code false} if not.
         */
        private boolean compareEQ(PySequence.Delegate<Object, PyList> other) {
            try {
                if (other.length() != list.size()) { return false; }
                Iterator<Object> i = list.iterator();
                for (Object b : other) {
                    Object a = i.next();
                    // if a != b, then we've found an answer
                    if (!Abstract.richCompareBool(a, b, Comparison.EQ))
                        return false;
                }
                // The arrays matched over their length.
                return true;
            } catch (PyException e) {
                // It's ok to throw legitimate Python exceptions
                throw e;
            } catch (Throwable t) {
                throw new InterpreterError(t, "non-Python exeption in comparison");
            }
        }

        /**
         * Compare this delegate with the delegate of the other
         * {@code list}, or return {@code NotImplemented} if the other is
         * not a {@code list}.
         *
         * @param other list at right of comparison
         * @param op type of operation
         * @return boolean result or {@code NotImplemented}
         */
        private Object cmp(Object other, Comparison op) {
            if (other instanceof PyList) {
                // A Python list is comparable only with another list
                ListDelegate o = ((PyList)other).delegate;
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

    /** Concatenate two lists (for {@code ListDelegate}). */
    private static PyList concat(List<?> v, List<?> w) {
        int n = v.size(), m = w.size();
        PyList u = new PyList(TYPE, n + m);
        u.addAll(v);
        u.addAll(w);
        return u;
    }
}
