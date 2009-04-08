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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

@ExposedType(name = "newlist", base = PyObject.class)
public class PyNewList extends PySequenceList implements List {

    public static final PyType TYPE = PyType.fromClass(PyNewList.class);
    protected final List<PyObject> list;

    public PyNewList() {
        this(TYPE);
    }

    public PyNewList(PyType type) {
        super(type);
        list = Generic.list();
    }

    public PyNewList(List list, boolean convert) {
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

    public PyNewList(PyType type, PyObject[] elements) {
        super(type);
        list = new ArrayList<PyObject>(Arrays.asList(elements));
    }

    public PyNewList(PyType type, Collection c) {
        super(type);
        list = new ArrayList<PyObject>(c.size());
        for (Object o : c) {
            add(o);
        }
    }

    public PyNewList(PyObject[] elements) {
        this(TYPE, elements);
    }

    public PyNewList(Collection c) {
        this(TYPE, c);
    }

    public PyNewList(PyObject o) {
        this(TYPE);
        for (PyObject item : o.asIterable()) {
            list.add(item);
        }
    }

    public PyNewList fromList(List list) {
        return new PyNewList(list, false);
    }

    private static List<PyObject> listify(Iterator<PyObject> iter) {
        List<PyObject> list = Generic.list();
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        return list;
    }

    public PyNewList(Iterator<PyObject> iter) {
        this(TYPE, listify(iter));
    }

    @ExposedNew
    @ExposedMethod(doc = BuiltinDocs.list___init___doc)
    final void newlist___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("newlist", args, kwds, new String[]{"sequence"}, 0);
        PyObject seq = ap.getPyObject(0, null);
        clear();
        if (seq == null) {
            return;
        }
        if (seq instanceof PyNewList) {
            list.addAll((PyNewList) seq); // don't convert
        } else {
            for (PyObject item : seq.asIterable()) {
                append(item);
            }
        }
    }

    @Override
    public int __len__() {
        return newlist___len__();
    }

    @ExposedMethod(doc = BuiltinDocs.list___len___doc)
    final int newlist___len__() {
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
        if ((value instanceof PySequence) || (!(value instanceof List))) {
            if (value == this) { // copy
                value = new PyNewList((PySequence) value);
            }
            setsliceIterator(start, stop, step, value.asIterable().iterator());
        } else {
            System.err.println("List");
            List valueList = (List) value.__tojava__(List.class);
            if (valueList != null && valueList != Py.NoConversion) {
                setsliceList(start, stop, step, valueList);
            }
        }
    }

    protected void setsliceList(int start, int stop, int step, List value) {
        int n = sliceLength(start, stop, step);
        if (list instanceof ArrayList) {
            ((ArrayList) list).ensureCapacity(start + n);
        }
        ListIterator src = value.listIterator();
        for (int j = start; src.hasNext(); j += step) {
            set(j, src.next());
        }
    }

    protected void setsliceIterator(int start, int stop, int step, Iterator<PyObject> iter) {
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

    @Override
    protected PyObject repeat(int count) {
        if (count < 0) {
            count = 0;
        }
        int size = size();
        int newSize = size * count;
        if (count != 0 && newSize / count != size) {
            throw Py.MemoryError("");
        }

        PyNewList newList = new PyNewList();
        for (int i = 0; i < count; i++) {
            newList.addAll(this);
        }
        return newList;
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___ne___doc)
    final PyObject newlist___ne__(PyObject o) {
        return seq___ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___eq___doc)
    final PyObject newlist___eq__(PyObject o) {
        return seq___eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___lt___doc)
    final PyObject newlist___lt__(PyObject o) {
        return seq___lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___le___doc)
    final PyObject newlist___le__(PyObject o) {
        return seq___le__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___gt___doc)
    final PyObject newlist___gt__(PyObject o) {
        return seq___gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___ge___doc)
    final PyObject newlist___ge__(PyObject o) {
        return seq___ge__(o);
    }

    @Override
    public PyObject __imul__(PyObject o) {
        return newlist___imul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___imul___doc)
    final PyObject newlist___imul__(PyObject o) {
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
        List oldList = new ArrayList<PyObject>(list);
        for (int i = 1; i < count; i++) {
            list.addAll(oldList);
        }
        gListAllocatedStatus = __len__(); // now omit?
        return this;
    }

    @Override
    public PyObject __mul__(PyObject o) {
        return newlist___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___mul___doc)
    final PyObject newlist___mul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    @Override
    public PyObject __rmul__(PyObject o) {
        return newlist___rmul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___rmul___doc)
    final PyObject newlist___rmul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    @Override
    public PyObject __add__(PyObject o) {
        return newlist___add__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___add___doc)
    final PyObject newlist___add__(PyObject o) {
        PyNewList sum = null;
        if (o instanceof PySequenceList && !(o instanceof PyTuple)) {
            if (o instanceof PyNewList) {
                List oList = ((PyNewList) o).list;
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
                sum = new PyNewList();
                sum.newlist_extend(this);
                for (Iterator i = otherList.iterator(); i.hasNext();) {
                    sum.add(i.next());
                }
            }
        }
        return sum;
    }

    @Override
    public PyObject __radd__(PyObject o) {
        return newlist___radd__(o);
    }

    //XXX: needs __doc__
    @ExposedMethod(type = MethodType.BINARY)
    final PyObject newlist___radd__(PyObject o) {
        // Support adding java.util.List, but prevent adding PyTuple.
        // 'o' should never be a PyNewList since __add__ is defined.
        PyNewList sum = null;
        if (o instanceof PySequence) {
            return null;
        }
        Object oList = o.__tojava__(List.class);
        if (oList != Py.NoConversion && oList != null) {
            sum = new PyNewList();
            sum.addAll((List) oList);
            sum.extend(this);
        }
        return sum;
    }

    @ExposedMethod(doc = BuiltinDocs.list___contains___doc)
    final boolean newlist___contains__(PyObject o) {
        return object___contains__(o);
    }

    @ExposedMethod(doc = BuiltinDocs.list___delitem___doc)
    final void newlist___delitem__(PyObject index) {
        seq___delitem__(index);
    }

    @ExposedMethod(doc = BuiltinDocs.list___setitem___doc)
    final void newlist___setitem__(PyObject o, PyObject def) {
        seq___setitem__(o, def);
    }

    @ExposedMethod(doc = BuiltinDocs.list___getitem___doc)
    final PyObject newlist___getitem__(PyObject o) {
        PyObject ret = seq___finditem__(o);
        if (ret == null) {
            throw Py.IndexError("index out of range: " + o);
        }
        return ret;
    }

    @Override
    public PyObject __iter__() {
        return newlist___iter__();
    }

    @ExposedMethod(doc = BuiltinDocs.list___iter___doc)
    public PyObject newlist___iter__() {
        return new PyFastSequenceIter(this);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.list___getslice___doc)
    final PyObject newlist___getslice__(PyObject start, PyObject stop, PyObject step) {
        return seq___getslice__(start, stop, step);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.list___setslice___doc)
    final void newlist___setslice__(PyObject start, PyObject stop, PyObject step, PyObject value) {
        seq___setslice__(start, stop, step, value);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.list___delslice___doc)
    final void newlist___delslice__(PyObject start, PyObject stop, PyObject step) {
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
        return newlist_toString();
    }

    //XXX: needs __doc__
    @ExposedMethod(names = "__repr__")
    final String newlist_toString() {
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
        newlist_append(o);
    }

    @ExposedMethod(doc = BuiltinDocs.list_append_doc)
    final void newlist_append(PyObject o) {
        pyadd(o);
        gListAllocatedStatus = __len__();
    }

    /**
     * Return the number elements in the list that equals the argument.
     *
     * @param o
     *            the argument to test for. Testing is done with the <code>==</code> operator.
     */
    public int count(PyObject o) {
        return newlist_count(o);
    }

    @ExposedMethod(doc = BuiltinDocs.list_count_doc)
    final int newlist_count(PyObject o) {
        int count = 0;
        PyObject[] array = getArray();
        for (int i = 0, n = size(); i < n; i++) {
            if (array[i].equals(o)) {
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
        return newlist_index(o, start, size());
    }

    public int index(PyObject o, int start, int stop) {
        return newlist_index(o, start, stop);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.list_index_doc)
    final int newlist_index(PyObject o, PyObject start, PyObject stop) {
        int startInt = start == null ? 0 : PySlice.calculateSliceIndex(start);
        int stopInt = stop == null ? size() : PySlice.calculateSliceIndex(stop);
        return newlist_index(o, startInt, stopInt);
    }

    final int newlist_index(PyObject o, int start, int stop) {
        return _index(o, "list.index(x): x not in list", start, stop);
    }

    final int newlist_index(PyObject o, int start) {
        return _index(o, "list.index(x): x not in list", start, size());
    }

    final int newlist_index(PyObject o) {
        return _index(o, "list.index(x): x not in list", 0, size());
    }

    private int _index(PyObject o, String message, int start, int stop) {
        // Follow Python 2.3+ behavior
        int validStop = boundToSequence(stop);
        int validStart = boundToSequence(start);
        PyObject[] array = getArray();
        for (int i = validStart; i < validStop && i < size(); i++) {
            if (array[i].equals(o)) {
                return i;
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
        newlist_insert(index, o);
    }

    @ExposedMethod(doc = BuiltinDocs.list_insert_doc)
    final void newlist_insert(int index, PyObject o) {
        if (index < 0) {
            index = Math.max(0, size() + index);
        }
        if (index > size()) {
            index = size();
        }
        pyadd(index, o);
        gListAllocatedStatus = __len__();
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
        newlist_remove(o);
    }

    @ExposedMethod(doc = BuiltinDocs.list_remove_doc)
    final void newlist_remove(PyObject o) {
        del(_index(o, "list.remove(x): x not in list", 0, size()));
        gListAllocatedStatus = __len__();
    }

    /**
     * Reverses the items of s in place. The reverse() methods modify the list in place for economy
     * of space when reversing a large list. It doesn't return the reversed list to remind you of
     * this side effect.
     */
    public void reverse() {
        newlist_reverse();
    }

    @ExposedMethod(doc = BuiltinDocs.list_reverse_doc)
    final void newlist_reverse() {
        Collections.reverse(list);
        gListAllocatedStatus = __len__();
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
        return newlist_pop(n);
    }

    @ExposedMethod(defaults = "-1", doc = BuiltinDocs.list_pop_doc)
    final PyObject newlist_pop(int n) {
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
        newlist_extend(o);
    }

    @ExposedMethod(doc = BuiltinDocs.list_extend_doc)
    final void newlist_extend(PyObject o) {
        if (o instanceof PyNewList) {
            list.addAll(((PyNewList) o).list);
        } else if (o instanceof PySequenceObjectList) {
            PyObject other[] = ((PySequenceObjectList) o).getArray();
            for (int i = 0; i < other.length; i++) {
                list.add(other[i]);
            }
        } else {
            for (PyObject item : o.asIterable()) {
                list.add(item);
            }
        }
        gListAllocatedStatus = __len__();
    }

    @Override
    public PyObject __iadd__(PyObject o) {
        return newlist___iadd__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___iadd___doc)
    final PyObject newlist___iadd__(PyObject o) {
        PyType oType = o.getType();
        if (oType == TYPE || oType == PyTuple.TYPE || this == o) {
            extend(fastSequence(o, "argument must be iterable"));
            return this;
        }

        PyObject it;
        try {
            it = o.__iter__();
        } catch (PyException pye) {
            if (!Py.matchException(pye, Py.TypeError)) {
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
    final void newlist_sort(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("list", args, kwds, new String[]{"cmp", "key", "reverse"}, 0);
        PyObject cmp = ap.getPyObject(0, Py.None);
        PyObject key = ap.getPyObject(1, Py.None);
        PyObject reverse = ap.getPyObject(2, Py.False);
        sort(cmp, key, reverse);
    }

    public void sort(PyObject compare) {
        sort(compare, Py.None, Py.False);
    }

    public void sort() {
        sort(Py.None, Py.None, Py.False);
    }

    public void sort(PyObject cmp, PyObject key, PyObject reverse) {
        MergeState ms = new MergeState(new PyList((Collection) this), cmp, key, reverse.__nonzero__());
        ms.sort();
    }

    public int hashCode() {
        return newlist___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.list___hash___doc)
    final int newlist___hash__() {
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
    public boolean addAll(int index, Collection c) {
        if (c instanceof PySequenceList) {
            list.addAll(index, c);
        } else {
            // need to use add to convert anything pulled from a collection into a PyObject
            for (Object element : c) {
                add(element);
            }
        }
        return c.size() > 0;
    }

    @Override
    public boolean addAll(Collection c) {
        if (c instanceof PySequenceList) {
            list.addAll(c);
        } else {
            // need to use add to convert anything pulled from a collection into a PyObject
            for (Object element : c) {
                add(element);
            }
        }
        return c.size() > 0;
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(Py.java2py(o));
    }

    @Override
    public boolean containsAll(Collection c) {
        if (c instanceof PySequenceList) {
            return list.containsAll(c);
        } else {
            return list.containsAll(new PyList(c));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyNewList) {
            return (((PyNewList) o).list.equals(list));
        }
        return false;
    }

    @Override
    public Object get(int index) {
        return list.get(index).__tojava__(Object.class);
    }

    /** @deprecated */
    @Override
    public PyObject[] getArray() {
        PyObject a[] = new PyObject[list.size()];
        return list.toArray(a);
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public Iterator iterator() {
        return list.iterator();
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator listIterator() {
        return list.listIterator();
    }

    @Override
    public ListIterator listIterator(int index) {
        return list.listIterator(index);
    }

    @Override
    public void pyadd(int index, PyObject element) {
        list.add(index, element);
    }

    @Override
    public boolean pyadd(PyObject o) {
        list.add(o);
        return true;
    }

    @Override
    public PyObject pyget(int index) {
        return list.get(index);
    }

    public void pyset(int index, PyObject element) {
        list.set(index, element);
    }

    @Override
    public Object remove(int index) {
        return list.remove(index);
    }

    @Override
    public void remove(int start, int stop) {
        list.subList(start, stop).clear();
    }

    @Override
    public boolean removeAll(Collection c) {
        if (c instanceof PySequenceList) {
            return list.removeAll(c);
        } else {
            return list.removeAll(new PyNewList(c));
        }
    }

    @Override
    public boolean retainAll(Collection c) {
        if (c instanceof PySequenceList) {
            return list.retainAll(c);
        } else {
            return list.retainAll(new PyNewList(c));
        }
    }

    @Override
    public Object set(int index, Object element) {
        return list.set(index, Py.java2py(element)).__tojava__(Object.class);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public List subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
        return list.toArray(a);
    }

    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start) {
            stop = start;
        }
        int n = sliceLength(start, stop, step);
        List newList;
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
    public boolean remove(Object o) {
        return list.remove(Py.java2py(o));
    }
}
