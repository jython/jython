// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.Arrays;
import java.util.Collection;
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
@ExposedType(name = "tuple", base = PyObject.class)
public class PyTuple extends PySequenceList implements List {

    public static final PyType TYPE = PyType.fromClass(PyTuple.class);

    private final PyObject[] array;

    private volatile List<PyObject> cachedList = null;

    private static final PyTuple EMPTY_TUPLE = new PyTuple();

    public PyTuple() {
        this(TYPE, Py.EmptyObjects);
    }

    public PyTuple(PyObject... elements) {
        this(TYPE, elements);
    }

    public PyTuple(PyType subtype, PyObject[] elements) {
        super(subtype);
        if (elements == null) {
            array = new PyObject[0];
        } else {
            array = new PyObject[elements.length];
            System.arraycopy(elements, 0, array, 0, elements.length);
        }
    }

    public PyTuple(PyObject[] elements, boolean copy) {
        this(TYPE, elements, copy);
    }

    public PyTuple(PyType subtype, PyObject[] elements, boolean copy) {
        super(subtype);

        if (copy) {
            array = new PyObject[elements.length];
            System.arraycopy(elements, 0, array, 0, elements.length);
        } else {
            array = elements;
        }
    }

    private static PyTuple fromArrayNoCopy(PyObject[] elements) {
        return new PyTuple(elements, false);
    }

    List<PyObject> getList() {
        if (cachedList == null) {
            cachedList = Arrays.asList(array);
        }
        return cachedList;
    }

    @ExposedNew
    final static PyObject tuple_new(PyNewWrapper new_, boolean init, PyType subtype,
                                    PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("tuple", args, keywords, new String[] {"sequence"}, 0);
        PyObject S = ap.getPyObject(0, null);
        if (new_.for_type == subtype) {
            if (S == null) {
                return EMPTY_TUPLE;
            }
            if (S instanceof PyTupleDerived) {
                return new PyTuple(((PyTuple) S).getArray());
            }
            if (S instanceof PyTuple) {
                return S;
            }
            return fromArrayNoCopy(Py.make_array(S));
        } else {
            if (S == null) {
                return new PyTupleDerived(subtype, Py.EmptyObjects);
            }
            return new PyTupleDerived(subtype, Py.make_array(S));
        }
    }

    /**
     * Return a new PyTuple from an iterable.
     *
     * Raises a TypeError if the object is not iterable.
     *
     * @param iterable an iterable PyObject
     * @return a PyTuple containing each item in the iterable
     */
    public static PyTuple fromIterable(PyObject iterable) {
        return fromArrayNoCopy(Py.make_array(iterable));
    }

    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start) {
            stop = start;
        }
        int n = sliceLength(start, stop, step);
        PyObject[] newArray = new PyObject[n];

        if (step == 1) {
            System.arraycopy(array, start, newArray, 0, stop - start);
            return fromArrayNoCopy(newArray);
        }
        for (int i = start, j = 0; j < n; i += step, j++) {
            newArray[j] = array[i];
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

        PyObject[] newArray = new PyObject[newSize];
        for (int i = 0; i < count; i++) {
            System.arraycopy(array, 0, newArray, i * size, size);
        }
        return fromArrayNoCopy(newArray);
    }

    @Override
    public int __len__() {
        return tuple___len__();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___len___doc)
    final int tuple___len__() {
        return size();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___contains___doc)
    final boolean tuple___contains__(PyObject o) {
        return super.__contains__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___ne___doc)
    final PyObject tuple___ne__(PyObject o) {
        return super.__ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___eq___doc)
    final PyObject tuple___eq__(PyObject o) {
        return super.__eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___gt___doc)
    final PyObject tuple___gt__(PyObject o) {
        return super.__gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___ge___doc)
    final PyObject tuple___ge__(PyObject o) {
        return super.__ge__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___lt___doc)
    final PyObject tuple___lt__(PyObject o) {
        return super.__lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___le___doc)
    final PyObject tuple___le__(PyObject o) {
        return super.__le__(o);
    }

    @Override
    public PyObject __add__(PyObject generic_other) {
        return tuple___add__(generic_other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___add___doc)
    final PyObject tuple___add__(PyObject generic_other) {
        PyTuple sum = null;
        if (generic_other instanceof PyTuple) {
            PyTuple other = (PyTuple) generic_other;
            PyObject[] newArray = new PyObject[array.length + other.array.length];
            System.arraycopy(array, 0, newArray, 0, array.length);
            System.arraycopy(other.array, 0, newArray, array.length, other.array.length);
            sum = fromArrayNoCopy(newArray);
        }
        return sum;
    }

    @Override
    public PyObject __mul__(PyObject o) {
        return tuple___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___mul___doc)
    final PyObject tuple___mul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    @Override
    public PyObject __rmul__(PyObject o) {
        return tuple___rmul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.tuple___rmul___doc)
    final PyObject tuple___rmul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    @Override
    public PyObject __iter__() {
        return tuple___iter__();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___iter___doc)
    public PyObject tuple___iter__() {
        return new PyFastSequenceIter(this);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.tuple___getslice___doc)
    final PyObject tuple___getslice__(PyObject s_start, PyObject s_stop, PyObject s_step) {
        return seq___getslice__(s_start, s_stop, s_step);
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___getitem___doc)
    final PyObject tuple___getitem__(PyObject index) {
        PyObject ret = seq___finditem__(index);
        if (ret == null) {
            throw Py.IndexError("index out of range: " + index);
        }
        return ret;
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___getnewargs___doc)
    final PyTuple tuple___getnewargs__() {
        return new PyTuple(new PyTuple(getArray()));
    }

    @Override
    public PyTuple __getnewargs__() {
        return tuple___getnewargs__();
    }

    @Override
    public int hashCode() {
        return tuple___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___hash___doc)
    final int tuple___hash__() {
        // strengthened hash to avoid common collisions. from CPython
        // tupleobject.tuplehash. See http://bugs.python.org/issue942952
        int y;
        int len = size();
        int mult = 1000003;
        int x = 0x345678;
        while (--len >= 0) {
            y = array[len].hashCode();
            x = (x ^ y) * mult;
            mult += 82520 + len + len;
        }
        return x + 97531;
    }

    private String subobjRepr(PyObject o) {
        if (o == null) {
            return "null";
        }
        return o.__repr__().toString();
    }

    @Override
    public String toString() {
        return tuple___repr__();
    }

    @ExposedMethod(doc = BuiltinDocs.tuple___repr___doc)
    final String tuple___repr__() {
        StringBuilder buf = new StringBuilder("(");
        for (int i = 0; i < array.length - 1; i++) {
            buf.append(subobjRepr(array[i]));
            buf.append(", ");
        }
        if (array.length > 0) {
            buf.append(subobjRepr(array[array.length - 1]));
        }
        if (array.length == 1) {
            buf.append(",");
        }
        buf.append(")");
        return buf.toString();
    }

    public List subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size()) {
            throw new IndexOutOfBoundsException();
        } else if (fromIndex > toIndex) {
            throw new IllegalArgumentException();
        }
        PyObject elements[] = new PyObject[toIndex - fromIndex];
        for (int i = 0, j = fromIndex; i < elements.length; i++, j++) {
            elements[i] = array[j];
        }
        return new PyTuple(elements);
    }

    public Iterator iterator() {
        return new Iterator() {

            private final Iterator<PyObject> iter = getList().iterator();

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public boolean hasNext() {
                return iter.hasNext();
            }

            public Object next() {
                return iter.next().__tojava__(Object.class);
            }
        };
    }

    public boolean add(Object o) {
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

            private final ListIterator<PyObject> iter = getList().listIterator(index);

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
        return getList().contains(Py.java2py(o));
    }

    @Override
    public boolean containsAll(Collection c) {
        return getList().containsAll(new PyList(c));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyTuple) {
            return Arrays.equals(array, ((PyTuple) o).array);
        } else if (o instanceof List && !(o instanceof PyList)) {
            return o.equals(this);
        }
        return false;
    }

    @Override
    public Object get(int index) {
        return array[index].__tojava__(Object.class);
    }

    @Override
    public PyObject[] getArray() {
        return array;
    }

    @Override
    public int indexOf(Object o) {
        return getList().indexOf(o);
    }

    @Override
    public boolean isEmpty() {
        return array.length == 0;
    }

    @Override
    public int lastIndexOf(Object o) {
        return getList().lastIndexOf(Py.java2py(o));
    }

    @Override
    public void pyadd(int index, PyObject element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean pyadd(PyObject o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PyObject pyget(int index) {
        return array[index];
    }

    @Override
    public void remove(int start, int stop) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return array.length;
    }

    @Override
    public Object[] toArray() {
        Object[] converted = new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            converted[i] = array[i].__tojava__(Object.class);
        }
        return converted;
    }

    @Override
    public Object[] toArray(Object[] converted) {
        if (converted.length != array.length) {
            converted = new Object[array.length];
        }
        for (int i = 0; i < array.length; i++) {
            converted[i] = array[i].__tojava__(Object.class);
        }
        if (array.length < converted.length) {
            for (int i = array.length; i < converted.length; i++) {
                converted[i] = null;
            }
        }
        return converted;
    }
}
