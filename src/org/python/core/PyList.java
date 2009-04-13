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
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

@ExposedType(name = "list", base = PyObject.class)
public class PyList extends PySequenceList implements List {

    public static final PyType TYPE = PyType.fromClass(PyList.class);
    protected List<PyObject> list;

    public PyList() {
        this(TYPE);
    }

    public PyList(PyType type) {
        super(type);
        list = Generic.list();
    }

    public PyList(List list, boolean convert) {
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

    public static PyList fromList(List list) {
        return new PyList(list, false);
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
            list.addAll((PyList) seq); // don't convert
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
    final int list___len__() {
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
    protected void setslice(int start, int stop, int step, PyObject value, int sliceLength, int valueLength) {
        if (stop < start) {
            stop = start;
        }
        int size = list.size();
//        System.err.println("start=" + start + ",stop=" + stop + ",step=" + step + ",sliceLength=" + sliceLength);


        // optimize copy into: x[:] = ...
        if (start == 0 && stop == size && step == 1) { // x[:] = ...
            Iterator<PyObject> src;
            if (value == this) {
                src = ((PyList) value).safeIterator();
            } else if (value instanceof PySequenceList) {
                src = ((PySequenceList) value).iterator();
            } else {
                src = value.asIterable().iterator();
            }
            List<PyObject> copy = new ArrayList<PyObject>(valueLength > 0 ? valueLength : 4);
            if (src != null) {
                while (src.hasNext()) {
                    copy.add(src.next());
                }
            }
            list = copy;
            return;
        }

        if (value instanceof PyObject) {
            Iterator<PyObject> src;
            if (value == this) {
                PyList valueList = (PyList) value;
                if (start == 0 && stop == 0 && step == -1) { // x[::-1] = x
                    valueList.reverse();
                    return;
                }
                src = ((PyList) value).safeIterator();
            } else if (value instanceof PySequenceList) {
                src = ((PySequenceList) value).iterator();
            } else if (value instanceof PySequence) {
                src = value.asIterable().iterator();
            } else {
                src = new PyList(value).iterator();
            }

            if (sliceLength != -1) {
                int srcRange = stop - start;
                int destRange = step * sliceLength;

//                System.err.println("start=" + start + ",stop=" + stop + ",step=" + step +
//                        ",sliceLength=" + sliceLength + ",srcRange=" + srcRange + ",destRange=" + destRange);

                if ((destRange >= srcRange && srcRange > destRange - step)) {
                    setsliceSimple(start, stop, step, src);
                    return;
                }
            }
            if (stop == start && step < 0) {
                setsliceSimple(start, stop, step, src);
            } else {
                setsliceIterator(start, stop, src);
            }

        } //        else if (value != null && !(value instanceof List)) {
        //            //XXX: can we avoid copying here?  Needed to pass test_userlist
        //            value = new PyList(value);
        //            setsliceIterator(start, stop, value.asIterable().iterator());
        //        }
        else {
//            System.err.println("List");
            List valueList = (List) value.__tojava__(List.class);
            if (valueList != null && valueList != Py.NoConversion) {
                setsliceList(start, stop, step, valueList);
            }
        }
    }

    private final Iterator<PyObject> safeIterator() {
        return new Iterator<PyObject>() {

            private int i = 0;
            private final PyObject elements[] = getArray();

            public boolean hasNext() {
                return (i < elements.length);
            }

            public PyObject next() {
                return elements[i++];
            }

            public void remove() {
                throw new UnsupportedOperationException("Immutable");
            }
        };
    }

    private final void setsliceSimple(int start, int stop, int step, Iterator<PyObject> src) {
        for (int i = start; src.hasNext(); i += step) {
            list.set(i, src.next());
        }
    }

    // XXX needs to follow below logic for setsliceIterator
    private final void setsliceList(int start, int stop, int step, List value) {
        ListIterator src = value.listIterator();
        for (int j = start; src.hasNext(); j += step) {
            set(j, src.next());
        }
    }


    // XXX need to support prepending via ops like x[:0] = whatever;
    // note that step must equal 1 in this case
    private final void setsliceIterator(int start, int stop, Iterator<PyObject> iter) {
        List<PyObject> copy = new ArrayList<PyObject>();
        copy.addAll(this.list.subList(0, start));
        if (iter != null) {
            while (iter.hasNext()) {
                copy.add(iter.next());
            }
        }
        copy.addAll(this.list.subList(stop, this.list.size()));
        this.list = copy;
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

        PyList newList = new PyList();
        for (int i = 0; i < count; i++) {
            newList.addAll(this);
        }
        return newList;
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___ne___doc)
    final PyObject list___ne__(PyObject o) {
        return seq___ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___eq___doc)
    final PyObject list___eq__(PyObject o) {
        return seq___eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___lt___doc)
    final PyObject list___lt__(PyObject o) {
        return seq___lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___le___doc)
    final PyObject list___le__(PyObject o) {
        return seq___le__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___gt___doc)
    final PyObject list___gt__(PyObject o) {
        return seq___gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___ge___doc)
    final PyObject list___ge__(PyObject o) {
        return seq___ge__(o);
    }

    @Override
    public PyObject __imul__(PyObject o) {
        return list___imul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___imul___doc)
    final PyObject list___imul__(PyObject o) {
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
        return list___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___mul___doc)
    final PyObject list___mul__(PyObject o) {
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
    final PyObject list___rmul__(PyObject o) {
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
    final PyObject list___add__(PyObject o) {
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
    final PyObject list___radd__(PyObject o) {
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
    final boolean list___contains__(PyObject o) {
        return object___contains__(o);
    }

    @ExposedMethod(doc = BuiltinDocs.list___delitem___doc)
    final void list___delitem__(PyObject index) {
        seq___delitem__(index);
    }

    @ExposedMethod(doc = BuiltinDocs.list___setitem___doc)
    final void list___setitem__(PyObject o, PyObject def) {
        seq___setitem__(o, def);
    }

    @ExposedMethod(doc = BuiltinDocs.list___getitem___doc)
    final PyObject list___getitem__(PyObject o) {
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
    public PyObject list___iter__() {
        return new PyFastSequenceIter(this);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.list___getslice___doc)
    final PyObject list___getslice__(PyObject start, PyObject stop, PyObject step) {
        return seq___getslice__(start, stop, step);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.list___setslice___doc)
    final void list___setslice__(PyObject start, PyObject stop, PyObject step, PyObject value) {
        seq___setslice__(start, stop, step, value);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.list___delslice___doc)
    final void list___delslice__(PyObject start, PyObject stop, PyObject step) {
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
    final String list_toString() {
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
    final void list_append(PyObject o) {
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
        return list_count(o);
    }

    @ExposedMethod(doc = BuiltinDocs.list_count_doc)
    final int list_count(PyObject o) {
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
    final int list_index(PyObject o, PyObject start, PyObject stop) {
        int startInt = start == null ? 0 : PySlice.calculateSliceIndex(start);
        int stopInt = stop == null ? size() : PySlice.calculateSliceIndex(stop);
        return list_index(o, startInt, stopInt);
    }

    final int list_index(PyObject o, int start, int stop) {
        return _index(o, "list.index(x): x not in list", start, stop);
    }

    final int list_index(PyObject o, int start) {
        return _index(o, "list.index(x): x not in list", start, size());
    }

    final int list_index(PyObject o) {
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
    final void list_insert(int index, PyObject o) {
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
        list_remove(o);
    }

    @ExposedMethod(doc = BuiltinDocs.list_remove_doc)
    final void list_remove(PyObject o) {
        del(_index(o, "list.remove(x): x not in list", 0, size()));
        gListAllocatedStatus = __len__();
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
    final void list_reverse() {
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
        return list_pop(n);
    }

    @ExposedMethod(defaults = "-1", doc = BuiltinDocs.list_pop_doc)
    final PyObject list_pop(int n) {
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
    final void list_extend(PyObject o) {
        if (o instanceof PyList) {
            list.addAll(((PyList) o).list);
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
        return list___iadd__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.list___iadd___doc)
    final PyObject list___iadd__(PyObject o) {
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
    final void list_sort(PyObject[] args, String[] kwds) {
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

    public synchronized void sort(PyObject cmp, PyObject key, PyObject reverse) {
        gListAllocatedStatus = -1;
        PyComparator c = new PyComparator(this, cmp, key, reverse.__nonzero__());
        Collections.sort(list, c);
        gListAllocatedStatus = __len__();
    }

    public int hashCode() {
        return list___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.list___hash___doc)
    final int list___hash__() {
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
            //XXX: FJW this seems unnecessary.
            return list.containsAll(new PyList(c));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyList) {
            return (((PyList) o).list.equals(list));
        } else if (o instanceof List) { // XXX copied from PyList, but...
            return o.equals(this);     // XXX shouldn't this compare using py2java?
        }
        return false;
    }

    @Override
    public Object get(int index) {
        return list.get(index).__tojava__(Object.class);
    }

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
            return list.removeAll(new PyList(c));
        }
    }

    @Override
    public boolean retainAll(Collection c) {
        if (c instanceof PySequenceList) {
            return list.retainAll(c);
        } else {
            return list.retainAll(new PyList(c));
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

    // NOTE: this attempt would seem faster, but the JVM is better optimized for
    // the ArrayList implementation in limited testing
    // keeping around for now...
//    protected PyObject getslice(int start, int stop, int step) {
//        if (step > 0 && stop < start) {
//            stop = start;
//        }
//        int n = sliceLength(start, stop, step);
//        PyObject elements[] = new PyObject[n];
//        if (step == 1) {
//            ListIterator<PyObject> iter = list.listIterator(start);
//            for (int i = 0; i < n; i++) {
//                elements[i] = iter.next();
//            }
//        } else {
//            for (int i = start, j = 0; j < n; i += step, j++) {
//                elements[j] = list.get(i);
//            }
//        }
//        return new PyList(elements);
//    }
    @Override
    public boolean remove(Object o) {
        return list.remove(Py.java2py(o));
    }
}
