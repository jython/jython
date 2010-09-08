// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.ArrayList;
import java.util.Arrays;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;
import org.python.util.Generic;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import java.lang.reflect.Array;

@ExposedType(name = "list", base = PyObject.class, doc = BuiltinDocs.list_doc)
public class PyList extends PySequenceList implements List {

    public static final PyType TYPE = PyType.fromClass(PyList.class);
    private final List<PyObject> list;
    public volatile int gListAllocatedStatus = -1;

    public PyList() {
        this(TYPE);
    }

    public PyList(PyType type) {
        super(type);
        list = Generic.list();
    }

    private PyList(List list, boolean convert) {
        super(TYPE);
        if (!convert) {
            this.list = list;
        } else {
            this.list = Generic.list();
            for (Object o : list) {
                add(o);
            }
        }
    }

    public PyList(PyType type, PyObject[] elements) {
        super(type);
        list = new ArrayList<PyObject>(Arrays.asList(elements));
    }

    public PyList(PyType type, Collection c) {
        super(type);
        list = new ArrayList<PyObject>(c.size());
        for (Object o : c) {
            add(o);
        }
    }

    public PyList(PyObject[] elements) {
        this(TYPE, elements);
    }

    public PyList(Collection c) {
        this(TYPE, c);
    }

    public PyList(PyObject o) {
        this(TYPE);
        for (PyObject item : o.asIterable()) {
            list.add(item);
        }
    }

    public static PyList fromList(List<PyObject> list) {
        return new PyList(list, false);
    }

    List<PyObject> getList() {
        return Collections.unmodifiableList(list);
    }

    private static List<PyObject> listify(Iterator<PyObject> iter) {
        List<PyObject> list = Generic.list();
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        return list;
    }

    public PyList(Iterator<PyObject> iter) {
        this(TYPE, listify(iter));
    }

    @ExposedNew
    @ExposedMethod(doc = BuiltinDocs.list___init___doc)
    final void list___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("list", args, kwds, new String[]{"sequence"}, 0);
        PyObject seq = ap.getPyObject(0, null);
        clear();
        if (seq == null) {
            return;
        }
        if (seq instanceof PyList) {
            list.addAll(((PyList) seq).list); // don't convert
        } else if (seq instanceof PyTuple) {
            list.addAll(((PyTuple) seq).getList());
        } else {
            for (PyObject item : seq.asIterable()) {
                append(item);
            }
        }
    }

    @Override
    public int __len__() {
        return list___len__();
    }

    @ExposedMethod(doc = BuiltinDocs.list___len___doc)
    final synchronized int list___len__() {
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

    @Override
    protected void setslice(int start, int stop, int step, PyObject value) {
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
                setsliceList(start, stop, step, (List)value);
        } else {
            Object valueList = value.__tojava__(List.class);
            if (valueList != null && valueList != Py.NoConversion) {
                setsliceList(start, stop, step, (List)valueList);
            } else {
                value = new PyList(value);
                setsliceIterator(start, stop, step, value.asIterable().iterator());
            }
        }
    }

    final private void setsliceList(int start, int stop, int step, List value) {
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
                PyObject item = Py.java2py(iter.next());
                if (j >= size) {
                    list.add(item);
                } else {
                    list.set(j, item);
                }
            }
        }
    }

    final private void setsliceIterator(int start, int stop, int step, Iterator<PyObject> iter) {
        if (step == 1) {
            List<PyObject> insertion = new ArrayList<PyObject>();
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
                PyObject item = iter.next();
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
            Iterator<PyObject> iter = other.list.listIterator();
            for (int j = start; iter.hasNext(); j += step) {
                PyObject item = iter.next();
                if (j >= size) {
                    list.add(item);
                } else {
                    list.set(j, item);
                }
            }
        }
    }

    @Override
    protected synchronized PyObject repeat(int count) {
        if (count < 0) {
            count = 0;
        }
        int size = size();
        int newSize = size * count;
        if (count != 0 && newSize / count != size) {
            throw Py.MemoryError("");
        }

        PyObject[] elements = list.toArray(new PyObject[size]);
        PyObject[] newList = new PyObject[newSize];
        for (int i = 0; i < count; i++) {
            System.arraycopy(elements, 0, newList, i * size, size);
        }
        return new PyList(newList);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___ne___doc)
    final synchronized PyObject list___ne__(PyObject o) {
        return seq___ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___eq___doc)
    final synchronized PyObject list___eq__(PyObject o) {
        return seq___eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___lt___doc)
    final synchronized PyObject list___lt__(PyObject o) {
        return seq___lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___le___doc)
    final synchronized PyObject list___le__(PyObject o) {
        return seq___le__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___gt___doc)
    final synchronized PyObject list___gt__(PyObject o) {
        return seq___gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___ge___doc)
    final synchronized PyObject list___ge__(PyObject o) {
        return seq___ge__(o);
    }

    @Override
    public PyObject __imul__(PyObject o) {
        return list___imul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___imul___doc)
    final synchronized PyObject list___imul__(PyObject o) {
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
            ((ArrayList) list).ensureCapacity(newSize);
        }
        List<PyObject> oldList = new ArrayList<PyObject>(list);
        for (int i = 1; i < count; i++) {
            list.addAll(oldList);
        }
        gListAllocatedStatus = list.size(); // now omit?
        return this;
    }

    @Override
    public PyObject __mul__(PyObject o) {
        return list___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___mul___doc)
    final synchronized PyObject list___mul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    @Override
    public PyObject __rmul__(PyObject o) {
        return list___rmul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___rmul___doc)
    final synchronized PyObject list___rmul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    @Override
    public PyObject __add__(PyObject o) {
        return list___add__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___add___doc)
    final synchronized PyObject list___add__(PyObject o) {
        PyList sum = null;
        if (o instanceof PySequenceList && !(o instanceof PyTuple)) {
            if (o instanceof PyList) {
                List oList = ((PyList) o).list;
                List newList = new ArrayList(list.size() + oList.size());
                newList.addAll(list);
                newList.addAll(oList);
                sum = fromList(newList);
            }
        } else if (!(o instanceof PySequenceList)) {
            // also support adding java lists (but not PyTuple!)
            Object oList = o.__tojava__(List.class);
            if (oList != Py.NoConversion && oList != null) {
                List otherList = (List) oList;
                sum = new PyList();
                sum.list_extend(this);
                for (Iterator i = otherList.iterator(); i.hasNext();) {
                    sum.add(i.next());
                }
            }
        }
        return sum;
    }

    @Override
    public PyObject __radd__(PyObject o) {
        return list___radd__(o);
    }

    //XXX: needs __doc__
    @ExposedMethod(type = MethodType.BINARY)
    final synchronized PyObject list___radd__(PyObject o) {
        // Support adding java.util.List, but prevent adding PyTuple.
        // 'o' should never be a PyNewList since __add__ is defined.
        PyList sum = null;
        if (o instanceof PySequence) {
            return null;
        }
        Object oList = o.__tojava__(List.class);
        if (oList != Py.NoConversion && oList != null) {
            sum = new PyList();
            sum.addAll((List) oList);
            sum.extend(this);
        }
        return sum;
    }

    @ExposedMethod(doc = BuiltinDocs.list___contains___doc)
    final synchronized boolean list___contains__(PyObject o) {
        return object___contains__(o);
    }

    @ExposedMethod(doc = BuiltinDocs.list___delitem___doc)
    final synchronized void list___delitem__(PyObject index) {
        seq___delitem__(index);
    }

    @ExposedMethod(doc = BuiltinDocs.list___setitem___doc)
    final synchronized void list___setitem__(PyObject o, PyObject def) {
        seq___setitem__(o, def);
    }

    @ExposedMethod(doc = BuiltinDocs.list___getitem___doc)
    final synchronized PyObject list___getitem__(PyObject o) {
        PyObject ret = seq___finditem__(o);
        if (ret == null) {
            throw Py.IndexError("index out of range: " + o);
        }
        return ret;
    }

    @Override
    public PyObject __iter__() {
        return list___iter__();
    }

    @ExposedMethod(doc = BuiltinDocs.list___iter___doc)
    public synchronized PyObject list___iter__() {
        return new PyFastSequenceIter(this);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.list___getslice___doc)
    final synchronized PyObject list___getslice__(PyObject start, PyObject stop, PyObject step) {
        return seq___getslice__(start, stop, step);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.list___setslice___doc)
    final synchronized void list___setslice__(PyObject start, PyObject stop, PyObject step, PyObject value) {
        seq___setslice__(start, stop, step, value);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.list___delslice___doc)
    final synchronized void list___delslice__(PyObject start, PyObject stop, PyObject step) {
        seq___delslice__(start, stop, step);
    }

    @Override
    protected String unsupportedopMessage(String op, PyObject o2) {
        if (op.equals("+")) {
            return "can only concatenate list (not \"{2}\") to list";
        }
        return super.unsupportedopMessage(op, o2);
    }

    public String toString() {
        return list_toString();
    }

    //XXX: needs __doc__
    @ExposedMethod(names = "__repr__")
    final synchronized String list_toString() {
        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) {
            return "[...]";
        }
        StringBuilder buf = new StringBuilder("[");
        int length = size();
        int i = 0;
        for (PyObject item : list) {
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
    public void append(PyObject o) {
        list_append(o);
    }

    @ExposedMethod(doc = BuiltinDocs.list_append_doc)
    final synchronized void list_append(PyObject o) {
        pyadd(o);
        gListAllocatedStatus = list.size();
    }

    /**
     * Return the number elements in the list that equals the argument.
     *
     * @param o
     *            the argument to test for. Testing is done with the <code>==</code> operator.
     */
    public int count(PyObject o) {
        return list_count(o);
    }

    @ExposedMethod(doc = BuiltinDocs.list_count_doc)
    final synchronized int list_count(PyObject o) {
        int count = 0;
        for (PyObject item : list) {
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
    public int index(PyObject o) {
        return index(o, 0);
    }

    public int index(PyObject o, int start) {
        return list_index(o, start, size());
    }

    public int index(PyObject o, int start, int stop) {
        return list_index(o, start, stop);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.list_index_doc)
    final synchronized int list_index(PyObject o, PyObject start, PyObject stop) {
        int startInt = start == null ? 0 : PySlice.calculateSliceIndex(start);
        int stopInt = stop == null ? size() : PySlice.calculateSliceIndex(stop);
        return list_index(o, startInt, stopInt);
    }

    final synchronized int list_index(PyObject o, int start, int stop) {
        return _index(o, "list.index(x): x not in list", start, stop);
    }

    final synchronized int list_index(PyObject o, int start) {
        return _index(o, "list.index(x): x not in list", start, size());
    }

    final synchronized int list_index(PyObject o) {
        return _index(o, "list.index(x): x not in list", 0, size());
    }

    private int _index(PyObject o, String message, int start, int stop) {
        // Follow Python 2.3+ behavior
        int validStop = boundToSequence(stop);
        int validStart = boundToSequence(start);
        int i = validStart;
        if (validStart <= validStop) {
            try {
                for (PyObject item : list.subList(validStart, validStop)) {
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
    public void insert(int index, PyObject o) {
        list_insert(index, o);
    }

    @ExposedMethod(doc = BuiltinDocs.list_insert_doc)
    final synchronized void list_insert(int index, PyObject o) {
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
    public void remove(PyObject o) {
        list_remove(o);
    }

    @ExposedMethod(doc = BuiltinDocs.list_remove_doc)
    final synchronized void list_remove(PyObject o) {
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

    @ExposedMethod(doc = BuiltinDocs.list_reverse_doc)
    final synchronized void list_reverse() {
        Collections.reverse(list);
        gListAllocatedStatus = list.size();
    }

    /**
     * Removes and return the last element in the list.
     */
    public PyObject pop() {
        return pop(-1);
    }

    /**
     * Removes and return the <code>n</code> indexed element in the list.
     *
     * @param n
     *            the index of the element to remove and return.
     */
    public PyObject pop(int n) {
        return list_pop(n);
    }

    @ExposedMethod(defaults = "-1", doc = BuiltinDocs.list_pop_doc)
    final synchronized PyObject list_pop(int n) {
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
        PyObject v = list.remove(n);
        return v;
    }

    /**
     * Append the elements in the argument sequence to the end of the list. <br>
     * Same as <code>s[len(s):len(s)] = o</code>.
     *
     * @param o
     *            the sequence of items to append to the list.
     */
    public void extend(PyObject o) {
        list_extend(o);
    }

    @ExposedMethod(doc = BuiltinDocs.list_extend_doc)
    final synchronized void list_extend(PyObject o) {
        if (o instanceof PyList) {
            list.addAll(((PyList) o).list);
        } else {
            for (PyObject item : o.asIterable()) {
                list.add(item);
            }
        }
        gListAllocatedStatus = list.size();
    }

    @Override
    public PyObject __iadd__(PyObject o) {
        return list___iadd__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___iadd___doc)
    final synchronized PyObject list___iadd__(PyObject o) {
        PyType oType = o.getType();
        if (oType == TYPE || oType == PyTuple.TYPE || this == o) {
            extend(fastSequence(o, "argument must be iterable"));
            return this;
        }

        PyObject it;
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
    @ExposedMethod(doc = BuiltinDocs.list_sort_doc)
    final synchronized void list_sort(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("list", args, kwds, new String[]{"cmp", "key", "reverse"}, 0);
        PyObject cmp = ap.getPyObject(0, Py.None);
        PyObject key = ap.getPyObject(1, Py.None);
        PyObject reverse = ap.getPyObject(2, Py.False);
        sort(cmp, key, reverse);
    }

    public void sort(PyObject cmp, PyObject key, PyObject reverse) {
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
        Collections.sort(list, new PyObjectDefaultComparator(this));
        if (reverse) {
            Collections.reverse(list); // maintain stability of sort by reversing first
        }
        gListAllocatedStatus = list.size();
    }

    private static class PyObjectDefaultComparator implements Comparator<PyObject> {

        private final PyList list;

        PyObjectDefaultComparator(PyList list) {
            this.list = list;
        }

        public int compare(PyObject o1, PyObject o2) {
            int result = o1._cmp(o2);
            if (this.list.gListAllocatedStatus >= 0) {
                throw Py.ValueError("list modified during sort");
            }
            return result;
        }

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

    public void sort(PyObject compare) {
        sort(compare, false);
    }

    private synchronized void sort(PyObject compare, boolean reverse) {
        gListAllocatedStatus = -1;
        if (reverse) {
            Collections.reverse(list); // maintain stability of sort by reversing first
        }
        PyObjectComparator c = new PyObjectComparator(this, compare);
        Collections.sort(list, c);
        if (reverse) {
            Collections.reverse(list);
        }
        gListAllocatedStatus = list.size();
    }

    private static class PyObjectComparator implements Comparator<PyObject> {

        private final PyList list;
        private final PyObject cmp;

        PyObjectComparator(PyList list, PyObject cmp) {
            this.list = list;
            this.cmp = cmp;
        }

        public int compare(PyObject o1, PyObject o2) {
            int result = cmp.__call__(o1, o2).asInt();
            if (this.list.gListAllocatedStatus >= 0) {
                throw Py.ValueError("list modified during sort");
            }
            return result;
        }

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

        private final PyObject key;
        private final PyObject value;

        KV(PyObject key, PyObject value) {
            this.key = key;
            this.value = value;
        }
    }

    private static class KVComparator implements Comparator<KV> {

        private final PyList list;
        private final PyObject cmp;

        KVComparator(PyList list, PyObject cmp) {
            this.list = list;
            this.cmp = cmp;
        }

        public int compare(KV o1, KV o2) {
            int result;
            if (cmp != null && cmp != Py.None) {
                result = cmp.__call__(o1.key, o2.key).asInt();
            } else {
                result = o1.key._cmp(o2.key);
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

    private synchronized void sort(PyObject cmp, PyObject key, boolean reverse) {
        gListAllocatedStatus = -1;

        int size = list.size();
        final ArrayList<KV> decorated = new ArrayList<KV>(size);
        for (PyObject value : list) {
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
            ((ArrayList) list).ensureCapacity(size);
        }
        for (KV kv : decorated) {
            list.add(kv.value);
        }
        gListAllocatedStatus = list.size();
    }

    public int hashCode() {
        return list___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.list___hash___doc)
    final synchronized int list___hash__() {
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
            return list.containsAll(((PyTuple) c).getList());
        } else {
            return list.containsAll(new PyList(c));
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof PyObject) {
            synchronized (this) {
                return _eq((PyObject)other).__nonzero__();
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
    public synchronized PyObject[] getArray() {
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
    public Iterator iterator() {
        return new Iterator() {

            private final Iterator<PyObject> iter = list.iterator();

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

            private final ListIterator<PyObject> iter = list.listIterator(index);

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
    public synchronized void pyadd(int index, PyObject element) {
        list.add(index, element);
    }

    @Override
    public synchronized boolean pyadd(PyObject o) {
        list.add(o);
        return true;
    }

    @Override
    public synchronized PyObject pyget(int index) {
        return list.get(index);
    }

    public synchronized void pyset(int index, PyObject element) {
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
            copy[i] = ((PyObject) copy[i]).__tojava__(Object.class);
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

    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start) {
            stop = start;
        }
        int n = sliceLength(start, stop, step);
        List<PyObject> newList;
        if (step == 1) {
            newList = new ArrayList<PyObject>(list.subList(start, stop));
        } else {
            newList = new ArrayList<PyObject>(n);
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
