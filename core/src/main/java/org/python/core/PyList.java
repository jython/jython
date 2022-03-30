// Copyright (c)2022 Jython Developers.
// Licensed to PSF under a contributor agreement.
// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.python.core.PyType.Spec;

// @ExposedType(name = "list", base = PyObject.class, doc = BuiltinDocs.list_doc)
public class PyList implements List<Object>, CraftedPyObject {

    public static final PyType TYPE =
            PyType.fromSpec(new Spec("list", MethodHandles.lookup()));

    /** The Python type of this instance. */
    protected final PyType type;

    private final ArrayList<Object> list;
    public volatile int gListAllocatedStatus = -1;

    PyList(PyType type, int size) {
        this.type = type;
        this.list = new ArrayList<>(size);
    }

    public PyList() {
        this(TYPE, 0);
    }

    public PyList(PyType type) {
        this(type, 0);
    }

    public PyList(Collection<?> c) {
        this(TYPE, c);
    }

    public PyList(PyType type, Collection<?> c) {
        this(type, c.size());
        addAll(c);
    }

    public PyList(Object[] elements) {
        this(TYPE, elements);
    }

    public PyList(PyType type, Object[] elements) {
        this(type, Arrays.asList(elements));
    }

    public PyList(Object o) {
        this(TYPE);
        for (Object item : o.asIterable()) {
            list.add(item);
        }
    }

    private static List<Object> listify(Iterator<Object> iter) {
        List<Object> list = new ArrayList<>();
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        return list;
    }

    public PyList(Iterator<Object> iter) {
        this(TYPE, listify(iter));
    }

    // refactor and put in Py presumably;
    // presumably we can consume an arbitrary iterable too!
    private static void addCollection(List<Object> list, Collection<Object> seq) {
        Map<Long, Object> seen = new HashMap<>();
        for (Object item : seq) {
            long id = Py.java_obj_id(item);
            Object seen_obj = seen.get(id);
            if (seen_obj != null) {
                seen_obj = Py.java2py(item);
                seen.put(id, seen_obj);
            }
            list.add(seen_obj);
        }
    }

    @Override
    public PyType getType() { return type; }


    // Special methods -----------------------------------------------

    @SuppressWarnings("unchecked")
    // @ExposedNew
    // @ExposedMethod(doc = BuiltinDocs.list___init___doc)
    void __init__(Object[] args, String[] kwds) {
        ArgParser ap = new ArgParser("list", args, kwds, new String[]{"sequence"}, 0);
        Object seq = ap.getPyObject(0, null);
        clear();
        if (seq == null) {
            return;
        }

        /* PyListDerived should be iterated over and not plain copied for cases where someone subclasses list
        and overrides __iter__
         */
        if (seq instanceof PyListDerived) {
            for (Object item : seq.asIterable()) {
                append(item);
            }
        } else if (seq instanceof PyList) {
            list.addAll(((PyList) seq).list); // don't convert
        } else if (seq instanceof PyList) {
            list.addAll((PyTuple) seq);
        } else if (seq.getClass().isAssignableFrom(Collection.class)) {
            System.err.println("Adding from collection");
            addCollection(list, (Collection<Object>) seq);
        } else {
            for (Object item : seq.asIterable()) {
                append(item);
            }
        }
    }

    @Override
    public int __len__() {
        return list___len__();
    }

    // @ExposedMethod(doc = BuiltinDocs.list___len___doc)
    synchronized int __len__() {
        return size();
    }

    @Override
    protected void del(int i) {
        remove(i);
    }

    @Override
    protected void delRange(int start, int stop) {
        remove(start, stop);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void setslice(int start, int stop, int step, Object value) {
        if (stop < start) {
            stop = start;
        }
        if (value instanceof PyList) {
            if (value == this) { // copy
                value = new PyList((PySequence) value);
            }
            setslicePyList(start, stop, step, (PyList) value);
        } else if (value instanceof PySequence) {
            setsliceIterator(start, stop, step, value.asIterable().iterator());
        } else if (value instanceof List) {
                setsliceList(start, stop, step, (List<Object>)value);
        } else {
            Object valueList = value.__tojava__(List.class);
            if (valueList != null && valueList != Py.NoConversion) {
                setsliceList(start, stop, step, (List<Object>)valueList);
            } else {
                value = new PyList(value);
                setsliceIterator(start, stop, step, value.asIterable().iterator());
            }
        }
    }

    final private void setsliceList(int start, int stop, int step, List<Object> value) {
        if (step == 1) {
            list.subList(start, stop).clear();
            int n = value.size();
            for (int i=0, j=start; i<n; i++, j++) {
                list.add(j, Py.java2py(value.get(i)));
            }
        } else {
            int size = list.size();
            Iterator<Object> iter = value.listIterator();
            for (int j = start; iter.hasNext(); j += step) {
                Object item = Py.java2py(iter.next());
                if (j >= size) {
                    list.add(item);
                } else {
                    list.set(j, item);
                }
            }
        }
    }

    final private void setsliceIterator(int start, int stop, int step, Iterator<Object> iter) {
        if (step == 1) {
            List<Object> insertion = new ArrayList<Object>();
            if (iter != null) {
                while (iter.hasNext()) {
                    insertion.add(iter.next());
                }
            }
            list.subList(start, stop).clear();
            list.addAll(start, insertion);
        } else {
            int size = list.size();
            for (int j = start; iter.hasNext(); j += step) {
                Object item = iter.next();
                if (j >= size) {
                    list.add(item);
                } else {
                    list.set(j, item);
                }
            }
        }
    }

    final private void setslicePyList(int start, int stop, int step, PyList other) {
        if (step == 1) {
            list.subList(start, stop).clear();
            list.addAll(start, other.list);
        } else {
            int size = list.size();
            Iterator<Object> iter = other.list.listIterator();
            for (int j = start; iter.hasNext(); j += step) {
                Object item = iter.next();
                if (j >= size) {
                    list.add(item);
                } else {
                    list.set(j, item);
                }
            }
        }
    }

    @Override
    protected synchronized Object repeat(int count) {
        if (count < 0) {
            count = 0;
        }
        int size = size();
        int newSize = size * count;
        if (count != 0 && newSize / count != size) {
            throw Py.MemoryError("");
        }

        Object[] elements = list.toArray(new Object[size]);
        Object[] newList = new Object[newSize];
        for (int i = 0; i < count; i++) {
            System.arraycopy(elements, 0, newList, i * size, size);
        }
        return new PyList(newList);
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___ne___doc)
    synchronized Object __ne__(Object o) {
        return seq___ne__(o);
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___eq___doc)
    synchronized Object __eq__(Object o) {
        return seq___eq__(o);
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___lt___doc)
    synchronized Object __lt__(Object o) {
        return seq___lt__(o);
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___le___doc)
    synchronized Object __le__(Object o) {
        return seq___le__(o);
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___gt___doc)
    synchronized Object __gt__(Object o) {
        return seq___gt__(o);
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___ge___doc)
    synchronized Object __ge__(Object o) {
        return seq___ge__(o);
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___imul___doc)
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
            throw Py.MemoryError("");
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

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___mul___doc)
    synchronized Object __mul__(Object o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___rmul___doc)
    synchronized Object __rmul__(Object o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___add___doc)
    synchronized Object __add__(Object o) {
        PyList sum = null;
        if (o instanceof PySequenceList && !(o instanceof PyTuple)) {
            if (o instanceof PyList) {
                List<Object> oList = ((PyList) o).list;
                ArrayList<Object> newList = new ArrayList<>(list.size() + oList.size());
                newList.addAll(list);
                newList.addAll(oList);
                sum = fromList(newList);
            }
        } else if (!(o instanceof PySequenceList)) {
            // also support adding java lists (but not PyTuple!)
            Object oList = o.__tojava__(List.class);
            if (oList != Py.NoConversion && oList != null) {
                @SuppressWarnings("unchecked")
                List<Object> otherList = (List<Object>) oList;
                sum = new PyList();
                sum.list_extend(this);
                for (Object ob: otherList) {
                    sum.add(ob);
                }
            }
        }
        return sum;
    }

    @SuppressWarnings("unchecked")
    // @ExposedMethod(type = MethodType.BINARY)
    synchronized Object __radd__(Object o) {
        // Support adding java.util.List, but prevent adding PyTuple.
        // 'o' should never be a PyNewList since __add__ is defined.
        PyList sum = null;
        if (o instanceof PySequence) {
            return null;
        }
        Object oList = o.__tojava__(List.class);
        if (oList != Py.NoConversion && oList != null) {
            sum = new PyList();
            sum.addAll((List<Object>) oList);
            sum.extend(this);
        }
        return sum;
    }

    // @ExposedMethod(doc = BuiltinDocs.list___contains___doc)
    synchronized boolean __contains__(Object o) {
        return object___contains__(o);
    }

    // @ExposedMethod(doc = BuiltinDocs.list___delitem___doc)
    synchronized void __delitem__(Object index) {
        seq___delitem__(index);
    }

    // @ExposedMethod(doc = BuiltinDocs.list___setitem___doc)
    synchronized void __setitem__(Object o, Object def) {
        seq___setitem__(o, def);
    }

    // @ExposedMethod(doc = BuiltinDocs.list___getitem___doc)
    synchronized Object __getitem__(Object o) {
        Object ret = seq___finditem__(o);
        if (ret == null) {
            throw Py.IndexError("index out of range: " + o);
        }
        return ret;
    }

    // @ExposedMethod(doc = BuiltinDocs.list___iter___doc)
    Object __iter__() {
        return new PyListIterator(this);
    }

    // @ExposedMethod(doc = BuiltinDocs.list___reversed___doc)
    synchronized PyIterator __reversed__() {
        return new PyReversedIterator(this);
    }

    // @ExposedMethod(defaults = "null", doc = BuiltinDocs.list___getslice___doc)
    synchronized Object __getslice__(Object start, Object stop, Object step) {
        return seq___getslice__(start, stop, step);
    }

    // @ExposedMethod(defaults = "null", doc = BuiltinDocs.list___setslice___doc)
    synchronized void __setslice__(Object start, Object stop, Object step, Object value) {
        seq___setslice__(start, stop, step, value);
    }

    // @ExposedMethod(defaults = "null", doc = BuiltinDocs.list___delslice___doc)
    synchronized void __delslice__(Object start, Object stop, Object step) {
        seq___delslice__(start, stop, step);
    }

    @Override
    protected String unsupportedopMessage(String op, Object o2) {
        if (op.equals("+")) {
            return "can only concatenate list (not \"{2}\") to list";
        }
        return super.unsupportedopMessage(op, o2);
    }

    public String toString() {
        return list_toString();
    }

    //XXX: needs __doc__
    // @ExposedMethod(names = "__repr__")
    final synchronized String list_toString() {
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

    /**
     * Add a single element to the end of list.
     *
     * @param o
     *            the element to add.
     */
    public void append(Object o) {
        list_append(o);
    }

    // @ExposedMethod(doc = BuiltinDocs.list_append_doc)
    final synchronized void list_append(Object o) {
        pyadd(o);
        gListAllocatedStatus = list.size();
    }

    /**
     * Return the number elements in the list that equals the argument.
     *
     * @param o
     *            the argument to test for. Testing is done with the <code>==</code> operator.
     */
    public int count(Object o) {
        return list_count(o);
    }

    // @ExposedMethod(doc = BuiltinDocs.list_count_doc)
    final synchronized int list_count(Object o) {
        int count = 0;
        for (Object item : list) {
            if (item.equals(o)) {
                count++;
            }
        }
        return count;
    }

    /**
     * return smallest index where an element in the list equals the argument.
     *
     * @param o
     *            the argument to test for. Testing is done with the <code>==</code> operator.
     */
    public int index(Object o) {
        return index(o, 0);
    }

    public int index(Object o, int start) {
        return list_index(o, start, size());
    }

    public int index(Object o, int start, int stop) {
        return list_index(o, start, stop);
    }

    // @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.list_index_doc)
    final synchronized int list_index(Object o, Object start, Object stop) {
        int startInt = start == null ? 0 : PySlice.calculateSliceIndex(start);
        int stopInt = stop == null ? size() : PySlice.calculateSliceIndex(stop);
        return list_index(o, startInt, stopInt);
    }

    final synchronized int list_index(Object o, int start, int stop) {
        return _index(o, "list.index(x): x not in list", start, stop);
    }

    final synchronized int list_index(Object o, int start) {
        return _index(o, "list.index(x): x not in list", start, size());
    }

    final synchronized int list_index(Object o) {
        return _index(o, "list.index(x): x not in list", 0, size());
    }

    private int _index(Object o, String message, int start, int stop) {
        // Follow Python 2.3+ behavior
        int validStop = boundToSequence(stop);
        int validStart = boundToSequence(start);
        int i = validStart;
        if (validStart <= validStop) {
            try {
                for (Object item : list.subList(validStart, validStop)) {
                    if (item.equals(o)) {
                        return i;
                    }
                    i++;
                }
            } catch (ConcurrentModificationException ex) {
                throw Py.ValueError(message);
            }
        }
        throw Py.ValueError(message);
    }

    /**
     * Insert the argument element into the list at the specified index. <br>
     * Same as <code>s[index:index] = [o] if index &gt;= 0</code>.
     *
     * @param index
     *            the position where the element will be inserted.
     * @param o
     *            the element to insert.
     */
    public void insert(int index, Object o) {
        list_insert(index, o);
    }

    // @ExposedMethod(doc = BuiltinDocs.list_insert_doc)
    final synchronized void list_insert(int index, Object o) {
        if (index < 0) {
            index = Math.max(0, size() + index);
        }
        if (index > size()) {
            index = size();
        }
        pyadd(index, o);
        gListAllocatedStatus = list.size();
    }

    /**
     * Remove the first occurence of the argument from the list. The elements arecompared with the
     * <code>==</code> operator. <br>
     * Same as <code>del s[s.index(x)]</code>
     *
     * @param o
     *            the element to search for and remove.
     */
    public void remove(Object o) {
        list_remove(o);
    }

    // @ExposedMethod(doc = BuiltinDocs.list_remove_doc)
    final synchronized void list_remove(Object o) {
        del(_index(o, "list.remove(x): x not in list", 0, size()));
        gListAllocatedStatus = list.size();
    }

    /**
     * Reverses the items of s in place. The reverse() methods modify the list in place for economy
     * of space when reversing a large list. It doesn't return the reversed list to remind you of
     * this side effect.
     */
    public void reverse() {
        list_reverse();
    }

    // @ExposedMethod(doc = BuiltinDocs.list_reverse_doc)
    final synchronized void list_reverse() {
        Collections.reverse(list);
        gListAllocatedStatus = list.size();
    }

    /**
     * Removes and return the last element in the list.
     */
    public Object pop() {
        return pop(-1);
    }

    /**
     * Removes and return the <code>n</code> indexed element in the list.
     *
     * @param n
     *            the index of the element to remove and return.
     */
    public Object pop(int n) {
        return list_pop(n);
    }

    // @ExposedMethod(defaults = "-1", doc = BuiltinDocs.list_pop_doc)
    final synchronized Object list_pop(int n) {
        int length = size();
        if (length == 0) {
            throw Py.IndexError("pop from empty list");
        }
        if (n < 0) {
            n += length;
        }
        if (n < 0 || n >= length) {
            throw Py.IndexError("pop index out of range");
        }
        Object v = list.remove(n);
        return v;
    }

    /**
     * Append the elements in the argument sequence to the end of the list. <br>
     * Same as <code>s[len(s):len(s)] = o</code>.
     *
     * @param o
     *            the sequence of items to append to the list.
     */
    public void extend(Object o) {
        list_extend(o);
    }

    // @ExposedMethod(doc = BuiltinDocs.list_extend_doc)
    final synchronized void list_extend(Object o) {
        if (o instanceof PyList) {
            list.addAll(((PyList) o).list);
        } else {
            for (Object item : o.asIterable()) {
                list.add(item);
            }
        }
        gListAllocatedStatus = list.size();
    }

    // @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___iadd___doc)
    synchronized Object __iadd__(Object o) {
        PyType oType = o.getType();
        if (oType == TYPE || oType == PyTuple.TYPE || this == o) {
            extend(fastSequence(o, "argument must be iterable"));
            return this;
        }

        Object it;
        try {
            it = o.__iter__();
        } catch (PyException pye) {
            if (!pye.match(Py.TypeError)) {
                throw pye;
            }
            return null;
        }
        extend(it);
        return this;
    }

    /**
     * Sort the items of the list in place. The compare argument is a function of two arguments
     * (list items) which should return -1, 0 or 1 depending on whether the first argument is
     * considered smaller than, equal to, or larger than the second argument. Note that this slows
     * the sorting process down considerably; e.g. to sort a list in reverse order it is much faster
     * to use calls to the methods sort() and reverse() than to use the built-in function sort()
     * with a comparison function that reverses the ordering of the elements.
     *
     * @param compare
     *            the comparison function.
     */
    /**
     * Sort the items of the list in place. Items is compared with the normal relative comparison
     * operators.
     */
    // @ExposedMethod(doc = BuiltinDocs.list_sort_doc)
    final synchronized void list_sort(Object[] args, String[] kwds) {
        ArgParser ap = new ArgParser("list", args, kwds, new String[]{"cmp", "key", "reverse"}, 0);
        Object cmp = ap.getPyObject(0, Py.None);
        Object key = ap.getPyObject(1, Py.None);
        Object reverse = ap.getPyObject(2, Py.False);
        sort(cmp, key, reverse);
    }

    pObjectid sort(Object cmp, Object key, Object reverse) {
        boolean bReverse = reverse.__nonzero__();
        if (key == Py.None || key == null) {
            if (cmp == Py.None || cmp == null) {
                sort(bReverse);
            } else {
                sort(cmp, bReverse);
            }
        } else {
            sort(cmp, key, bReverse);
        }
    }

    // a bunch of optimized paths for sort to avoid unnecessary work, such as DSU or checking compare functions for null

    public void sort() {
        sort(false);
    }

    private synchronized void sort(boolean reverse) {
        gListAllocatedStatus = -1;
        if (reverse) {
            Collections.reverse(list); // maintain stability of sort by reversing first
        }
        final PyObjectDefaultComparator comparator = new PyObjectDefaultComparator(this);
        Collections.sort(list, comparator);
        if (comparator.raisedException()) {
            throw comparator.getRaisedException();
        }
        if (reverse) {
            Collections.reverse(list); // maintain stability of sort by reversing first
        }
        gListAllocatedStatus = list.size();
    }

    private static class PyObjectDefaultComparator implements Comparator<Object> {

        private final PyList list;
        private PyException comparatorException;

        PyObjectDefaultComparator(PyList list) {
            this.list = list;
        }

        public PyException getRaisedException() {
            return comparatorException;
        }

        public boolean raisedException() {
            return comparatorException != null;
        }

        @Override
        public int compare(Object o1, Object o2) {
            // PEP 207 specifies that sort should only depend on "less-than" (Issue #1767)
            int result = 0; // If exception is raised return objects are equal
            try {
                if (o1._lt(o2).__nonzero__()) {
                    result = -1;
                } else if (o2._lt(o1).__nonzero__()) {
                    result = 1;
                }
            } catch (PyException pye) {
                // #2399 Stash the exception so we can rethrow it later, and allow the sort to continue
                comparatorException = pye;
            }
            if (this.list.gListAllocatedStatus >= 0) {
                throw Py.ValueError("list modified during sort");
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof PyObjectDefaultComparator) {
                return true;
            }
            return false;
        }
    }

    public void sort(Object compare) {
        sort(compare, false);
    }

    private synchronized void sort(Object compare, boolean reverse) {
        gListAllocatedStatus = -1;
        if (reverse) {
            Collections.reverse(list); // maintain stability of sort by reversing first
        }
        final PyObjectComparator comparator = new PyObjectComparator(this, compare);
        Collections.sort(list, comparator);
        if (comparator.raisedException()) {
            throw comparator.getRaisedException();
        }
        if (reverse) {
            Collections.reverse(list);
        }
        gListAllocatedStatus = list.size();
    }

    private static class PyObjectComparator implements Comparator<Object> {

        private final PyList list;
        private final Object cmp;
        private PyException comparatorException;

        PyObjectComparator(PyList list, Object cmp) {
            this.list = list;
            this.cmp = cmp;
        }

        public PyException getRaisedException() {
            return comparatorException;
        }

        public boolean raisedException() {
            return comparatorException != null;
        }

        @Override
        public int compare(Object o1, Object o2) {
            int result = 0; // If exception is raised return objects are equal
            try {
                result = cmp.__call__(o1, o2).asInt();
            } catch (PyException pye) {
                // #2399 Stash the exception so we can rethrow it later, and allow the sort to continue
                comparatorException = pye;
            }
            if (this.list.gListAllocatedStatus >= 0) {
                throw Py.ValueError("list modified during sort");
            }
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (o instanceof PyObjectComparator) {
                return cmp.equals(((PyObjectComparator) o).cmp);
            }
            return false;
        }
    }

    private static class KV {

        private final Object key;
        private final Object value;

        KV(Object key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

    private static class KVComparator implements Comparator<KV> {

        private final PyList list;
        private final Object cmp;

        KVComparator(PyList list, Object cmp) {
            this.list = list;
            this.cmp = cmp;
        }

        public int compare(KV o1, KV o2) {
            int result;
            if (cmp != null && cmp != Py.None) {
                result = cmp.__call__(o1.key, o2.key).asInt();
            } else {
                // PEP 207 specifies that sort should only depend on "less-than" (Issue #1767)
                if (o1.key._lt(o2.key).__nonzero__()) {
                    result = -1;
                } else if (o2.key._lt(o1.key).__nonzero__()) {
                    result = 1;
                } else {
                    result = 0;
                }
            }
            if (this.list.gListAllocatedStatus >= 0) {
                throw Py.ValueError("list modified during sort");
            }
            return result;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (o instanceof KVComparator) {
                return cmp.equals(((KVComparator) o).cmp);
            }
            return false;
        }
    }

    private synchronized void sort(Object cmp, Object key, boolean reverse) {
        gListAllocatedStatus = -1;

        int size = list.size();
        final ArrayList<KV> decorated = new ArrayList<KV>(size);
        for (Object value : list) {
            decorated.add(new KV(key.__call__(value), value));
        }
        list.clear();
        KVComparator c = new KVComparator(this, cmp);
        if (reverse) {
            Collections.reverse(decorated); // maintain stability of sort by reversing first
        }
        Collections.sort(decorated, c);
        if (reverse) {
            Collections.reverse(decorated);
        }
        if (list instanceof ArrayList) {
            ((ArrayList<Object>) list).ensureCapacity(size);
        }
        for (KV kv : decorated) {
            list.add(kv.value);
        }
        gListAllocatedStatus = list.size();
    }

    public int hashCode() {
        return list___hash__();
    }

    // @ExposedMethod(doc = BuiltinDocs.list___hash___doc)
    synchronized int __hash__() {
        throw Py.TypeError(String.format("unhashable type: '%.200s'", getType().fastGetName()));
    }

    @Override
    public PyTuple __getnewargs__() {
        return new PyTuple(new PyTuple(getArray()));
    }

    @Override
    public void add(int index, Object element) {
        pyadd(index, Py.java2py(element));
    }

    @Override
    public boolean add(Object o) {
        pyadd(Py.java2py(o));
        return true;
    }

    @Override
    public synchronized boolean addAll(int index, Collection c) {
        PyList elements = new PyList(c);
        return list.addAll(index, elements.list);
    }

    @Override
    public boolean addAll(Collection c) {
        return addAll(0, c);
    }

    @Override
    public synchronized void clear() {
        list.clear();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return list.contains(Py.java2py(o));
    }

    @Override
    public synchronized boolean containsAll(Collection c) {
        if (c instanceof PyList) {
            return list.containsAll(((PyList) c).list);
        } else if (c instanceof PyTuple) {
            return list.containsAll((PyTuple) c);
        } else {
            return list.containsAll(new PyList(c));
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof Object) {
            synchronized (this) {
                return _eq((Object)other).__nonzero__();
            }
        }
        if (other instanceof List) {
            synchronized (this) {
                return list.equals(other);
            }
        }
        return false;
    }

    @Override
    public synchronized Object get(int index) {
        return list.get(index).__tojava__(Object.class);
    }

    @Override
    public synchronized Object[] getArray() {
        return list.toArray(Py.EmptyObjects);
    }

    @Override
    public synchronized int indexOf(Object o) {
        return list.indexOf(Py.java2py(o));
    }

    @Override
    public synchronized boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {

            private final Iterator<Object> iter = list.iterator();

            public boolean hasNext() {
                return iter.hasNext();
            }

            public Object next() {
                return iter.next().__tojava__(Object.class);
            }

            public void remove() {
                iter.remove();
            }
        };
    }

    @Override
    public synchronized int lastIndexOf(Object o) {
        return list.lastIndexOf(Py.java2py(o));
    }

    @Override
    public ListIterator listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator listIterator(final int index) {
        return new ListIterator() {

            private final ListIterator<Object> iter = list.listIterator(index);

            public boolean hasNext() {
                return iter.hasNext();
            }

            public Object next() {
                return iter.next().__tojava__(Object.class);
            }

            public boolean hasPrevious() {
                return iter.hasPrevious();
            }

            public Object previous() {
                return iter.previous().__tojava__(Object.class);
            }

            public int nextIndex() {
                return iter.nextIndex();
            }

            public int previousIndex() {
                return iter.previousIndex();
            }

            public void remove() {
                iter.remove();
            }

            public void set(Object o) {
                iter.set(Py.java2py(o));
            }

            public void add(Object o) {
                iter.add(Py.java2py(o));
            }
        };
    }

    @Override
    public synchronized void pyadd(int index, Object element) {
        list.add(index, element);
    }

    @Override
    public synchronized boolean pyadd(Object o) {
        list.add(o);
        return true;
    }

    @Override
    public synchronized Object pyget(int index) {
        return list.get(index);
    }

    public synchronized void pyset(int index, Object element) {
        list.set(index, element);
    }

    @Override
    public synchronized Object remove(int index) {
        return list.remove(index);
    }

    @Override
    public synchronized void remove(int start, int stop) {
        list.subList(start, stop).clear();
    }

    @Override
    public synchronized boolean removeAll(Collection c) {
        if (c instanceof PySequenceList) {
            return list.removeAll(c);
        } else {
            return list.removeAll(new PyList(c));
        }
    }

    @Override
    public synchronized boolean retainAll(Collection c) {
        if (c instanceof PySequenceList) {
            return list.retainAll(c);
        } else {
            return list.retainAll(new PyList(c));
        }
    }

    @Override
    public synchronized Object set(int index, Object element) {
        return list.set(index, Py.java2py(element)).__tojava__(Object.class);
    }

    @Override
    public synchronized int size() {
        return list.size();
    }

    @Override
    public synchronized List subList(int fromIndex, int toIndex) {
        return fromList(list.subList(fromIndex, toIndex));
    }

    @Override
    public synchronized Object[] toArray() {
        Object copy[] = list.toArray();
        for (int i = 0; i < copy.length; i++) {
            copy[i] = ((Object) copy[i]).__tojava__(Object.class);
        }
        return copy;
    }

    @Override
    public synchronized Object[] toArray(Object[] a) {
        int size = size();
        Class<?> type = a.getClass().getComponentType();
        if (a.length < size) {
            a = (Object[])Array.newInstance(type, size);
        }
        for (int i = 0; i < size; i++) {
            a[i] = list.get(i).__tojava__(type);
        }
        if (a.length > size) {
            for (int i = size; i < a.length; i++) {
                a[i] = null;
            }
        }
        return a;
    }

    protected Object getslice(int start, int stop, int step) {
        if (step > 0 && stop < start) {
            stop = start;
        }
        int n = sliceLength(start, stop, step);
        List<Object> newList;
        if (step == 1) {
            newList = new ArrayList<Object>(list.subList(start, stop));
        } else {
            newList = new ArrayList<Object>(n);
            for (int i = start, j = 0; j < n; i += step, j++) {
                newList.add(list.get(i));
            }
        }
        return fromList(newList);
    }

    @Override
    public synchronized boolean remove(Object o) {
        return list.remove(Py.java2py(o));
    }


}
