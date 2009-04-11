// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * A builtin python tuple.
 */

@ExposedType(name = "newtuple", base = PyObject.class)
public class PyNewTuple extends PySequenceList implements List {
    public static final PyType TYPE = PyType.fromClass(PyNewTuple.class);

    private final PyObject[] array;
    private final List<PyObject> list;
    private static final PyNewTuple EMPTY_TUPLE = new PyNewTuple();

    public PyNewTuple() {
        this(TYPE, Py.EmptyObjects);
    }

    public PyNewTuple(PyObject... elements) {
        this(TYPE, elements);
    }

    public PyNewTuple(Collection<PyObject> collection) {
        this(TYPE, collection);
    }

    public PyNewTuple(PyType subtype, PyObject[] elements) {
        super(subtype);
//        System.err.println("Initializing from " + Arrays.toString(elements));
        if (elements == null) {
            array = new PyObject[0];
            list = Collections.emptyList();
        } else {
            array = new PyObject[elements.length];
            System.arraycopy(elements, 0, array, 0, elements.length);
            list = Collections.unmodifiableList(Arrays.asList(array));
        }
    }

    public PyNewTuple(PyObject[] elements, boolean copy) {
        this(TYPE, elements, copy);
    }

    public PyNewTuple(PyType subtype, PyObject[] elements, boolean copy) {
        super(subtype);

        if (copy) {
            array = new PyObject[elements.length];
            System.arraycopy(elements, 0, array, 0, elements.length);
        } else {
            array = elements;
        }
        list = Collections.unmodifiableList(Arrays.asList(array));
    }

    public PyNewTuple(PyType subtype, Collection<PyObject> elements) {
        super(subtype);
        if (elements == null) {
            array = new PyObject[0];
            list = Collections.emptyList();
        } else {
            array = new PyObject[elements.size()];
            elements.toArray(array);
            list = Collections.unmodifiableList(Arrays.asList(array));
        }
    }

    private static PyNewTuple fromArrayNoCopy(PyObject[] elements) {
//        System.err.println("newtuple (no copy):" + Arrays.toString(elements));
        return new PyNewTuple(elements, false);
    }

    @ExposedNew
    final static PyObject newtuple_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
//        System.err.println("newtuple_new");
        ArgParser ap = new ArgParser("newtuple", args, keywords, new String[] { "sequence" }, 0);
        PyObject S = ap.getPyObject(0, null);
//        System.err.println("newtuple: new_=" + new_ + ",S=" + S);
        if (new_.for_type == subtype) {
            if (S == null) {
                return EMPTY_TUPLE;
            }
            if (S instanceof PyNewTupleDerived) {
                return new PyNewTuple(((PyNewTuple)S).getArray());
            }
            if (S instanceof PyNewTuple) {
                return S;
            }
            return fromArrayNoCopy(Py.make_array(S));
        } else {
            if (S == null) {
                return new PyNewTupleDerived(subtype, Py.EmptyObjects);
            }
            return new PyNewTupleDerived(subtype, Py.make_array(S));
        }
    }

    /**
     * Return a new PyNewTuple from an iterable.
     *
     * Raises a TypeError if the object is not iterable.
     *
     * @param iterable an iterable PyObject
     * @return a PyNewTuple containing each item in the iterable
     */
    public static PyNewTuple fromIterable(PyObject iterable) {
        return fromArrayNoCopy(Py.make_array(iterable));
    }



    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start)
            stop = start;
        int n = sliceLength(start, stop, step);
        PyObject[] newArray = new PyObject[n];
        PyObject[] array = getArray();

        if (step == 1) {
            System.arraycopy(array, start, newArray, 0, stop-start);
            return fromArrayNoCopy(newArray);
        }
        int j = 0;
        for (int i=start; j<n; i+=step) {
            newArray[j] = array[i];
            j++;
        }
        return fromArrayNoCopy(newArray);
    }

    protected PyObject repeat(int count) {
        if (count < 0) {
            count = 0;
        }
        int size = size();
        if (size == 0 || count == 1) {
            if (getType() == TYPE) {
                // Since tuples are immutable, we can return a shared copy in this case
                return this;
            }
            if (size == 0) {
                return EMPTY_TUPLE;
            }
        }

        int newSize = size * count;
        if (newSize / size != count) {
            throw Py.MemoryError("");
        }

        PyObject[] array = getArray();
        PyObject[] newArray = new PyObject[newSize];
        for (int i = 0; i < count; i++) {
            System.arraycopy(array, 0, newArray, i * size, size);
        }
        return fromArrayNoCopy(newArray);
    }

    public int __len__() {
        return newtuple___len__();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___len___doc)
    final int newtuple___len__() {
        return size();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___contains___doc)
    final boolean newtuple___contains__(PyObject o) {
        return super.__contains__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___ne___doc)
    final PyObject newtuple___ne__(PyObject o) {
        return super.__ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___eq___doc)
    final PyObject newtuple___eq__(PyObject o) {
        return super.__eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___gt___doc)
    final PyObject newtuple___gt__(PyObject o) {
        return super.__gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___ge___doc)
    final PyObject newtuple___ge__(PyObject o) {
        return super.__ge__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___lt___doc)
    final PyObject newtuple___lt__(PyObject o) {
        return super.__lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___le___doc)
    final PyObject newtuple___le__(PyObject o) {
        return super.__le__(o);
    }

    public PyObject __add__(PyObject generic_other) {
        return newtuple___add__(generic_other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___add___doc)
    final PyObject newtuple___add__(PyObject generic_other) {
        PyNewTuple sum = null;
        if (generic_other instanceof PyNewTuple) {
            PyNewTuple other = (PyNewTuple)generic_other;
            PyObject[] newArray = new PyObject[array.length + other.array.length];
            System.arraycopy(array, 0, newArray, 0, array.length);
            System.arraycopy(other.array, 0, newArray, array.length, other.array.length);
            sum = fromArrayNoCopy(newArray);
        }
        return sum;
    }

    @Override
    public PyObject __mul__(PyObject o) {
        return newtuple___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___mul___doc)
    final PyObject newtuple___mul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    @Override
    public PyObject __rmul__(PyObject o) {
        return newtuple___rmul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___rmul___doc)
    final PyObject newtuple___rmul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    public PyObject __iter__() {
        return newtuple___iter__();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___iter___doc)
    public PyObject newtuple___iter__() {
        return new PyFastSequenceIter(this);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.tuple___getslice___doc)
    final PyObject newtuple___getslice__(PyObject s_start, PyObject s_stop, PyObject s_step) {
        return seq___getslice__(s_start,s_stop,s_step);
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___getitem___doc)
    final PyObject newtuple___getitem__(PyObject index) {
        PyObject ret = seq___finditem__(index);
        if(ret == null) {
            throw Py.IndexError("index out of range: " + index);
        }
        return ret;
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___getnewargs___doc)
    final PyTuple newtuple___getnewargs__() {
        return new PyTuple(new PyTuple(getArray()));
    }

    public PyTuple __getnewargs__() {
        return newtuple___getnewargs__();
    }

    public int hashCode() {
        return newtuple___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___hash___doc)
    final int newtuple___hash__() {
        // strengthened hash to avoid common collisions. from CPython
        // tupleobject.tuplehash. See http://bugs.python.org/issue942952
        int y;
        int len = size();
        int mult = 1000003;
        int x = 0x345678;
        PyObject[] array = getArray();
        while (--len >= 0) {
            y = array[len].hashCode();
            x = (x ^ y) * mult;
            mult += 82520 + len + len;
        }
        return x + 97531;
    }

    private String subobjRepr(PyObject o) {
        if (o == null)
            return "null";
        return o.__repr__().toString();
    }

    public String toString() {
        return newtuple___repr__();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___repr___doc)
    final String newtuple___repr__() {
        StringBuilder buf = new StringBuilder("(");
        for (int i = 0; i < array.length-1; i++) {
            buf.append(subobjRepr(array[i]));
            buf.append(", ");
        }
        if (array.length > 0)
            buf.append(subobjRepr(array[array.length-1]));
        if (array.length == 1)
            buf.append(",");
        buf.append(")");
        return buf.toString();
    }

    public List subList(int fromIndex, int toIndex) {
        return Collections.unmodifiableList(list.subList(fromIndex, toIndex));
    }

    // Make PyNewTuple immutable from the collections interfaces by overriding
    // all the mutating methods to throw UnsupportedOperationException exception.
    // This is how Collections.unmodifiableList() does it.
    public Iterator iterator() {
        return new Iterator() {
            Iterator i = list.iterator();
            public void remove() {
                throw new UnsupportedOperationException();
            }
            public boolean hasNext() {
                return i.hasNext();
                }
            public Object next() {
                return i.next();
                }
        };
    }

    public boolean add(Object o){
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection coll) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public Object set(int index, Object element) {
        throw new UnsupportedOperationException();
    }

    public void add(int index, Object element) {
        throw new UnsupportedOperationException();
    }

    public Object remove(int index) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(int index, Collection c) {
        throw new UnsupportedOperationException();
    }

    public ListIterator listIterator() {
        return listIterator(0);
    }

    public ListIterator listIterator(final int index) {
        return new ListIterator() {
            ListIterator i = list.listIterator(index);

            public boolean hasNext()     {return i.hasNext();}
            public Object next()         {return i.next();}
            public boolean hasPrevious() {return i.hasPrevious();}
            public Object previous()     {return i.previous();}
            public int nextIndex()       {return i.nextIndex();}
            public int previousIndex()   {return i.previousIndex();}

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public void set(Object o) {
                throw new UnsupportedOperationException();
            }

            public void add(Object o) {
                throw new UnsupportedOperationException();
            }
        };
    }

    protected String unsupportedopMessage(String op, PyObject o2) {
        if (op.equals("+")) {
            return "can only concatenate tuple (not \"{2}\") to tuple";
        }
        return super.unsupportedopMessage(op, o2);
    }

    public void pyset(int index, PyObject value) {
        throw Py.TypeError("'tuple' object does not support item assignment");
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public boolean containsAll(Collection c) {
        return list.containsAll(c);
    }

    @Override
    public boolean equals(Object o) {
        return list.equals(o);
    }

    @Override
    public Object get(int index) {
        return list.get(index);
    }

    @Override
    public PyObject[] getArray() {
        return array;
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
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public void pyadd(int index, PyObject element) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean pyadd(PyObject o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PyObject pyget(int index) {
        return array[index];
    }

    @Override
    public void remove(int start, int stop) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int size() {
        return array.length;
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
        return list.toArray(a);
    }

}
