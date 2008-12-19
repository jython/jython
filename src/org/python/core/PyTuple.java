// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

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

@ExposedType(name = "tuple", base = PyObject.class)
public class PyTuple extends PySequenceList
{
    public static final PyType TYPE = PyType.fromClass(PyTuple.class);

    public PyTuple() {
        this(TYPE, Py.EmptyObjects);
    }

    public PyTuple(PyObject... elements) {
        this(TYPE, elements);
    }

    public PyTuple(PyType subtype, PyObject[] elements) {
        super(subtype, elements);
    }

    @ExposedNew
    final static PyObject tuple_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("tuple", args, keywords, new String[] { "sequence" }, 0);
        PyObject S = ap.getPyObject(0, null);
        if (new_.for_type == subtype) {
            if (S == null) {
                return new PyTuple();
            }
            if (S instanceof PyTupleDerived) {
                return new PyTuple(((PyTuple)S).getArray());
            }
            if (S instanceof PyTuple) {
                return S;
            }
            return new PyTuple(Py.make_array(S));
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
        return new PyTuple(Py.make_array(iterable));
    }

    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start)
            stop = start;
        int n = sliceLength(start, stop, step);
        PyObject[] newArray = new PyObject[n];
        PyObject[] array = getArray();

        if (step == 1) {
            System.arraycopy(array, start, newArray, 0, stop-start);
            return new PyTuple(newArray);
        }
        int j = 0;
        for (int i=start; j<n; i+=step) {
            newArray[j] = array[i];
            j++;
        }
        return new PyTuple(newArray);
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
                return new PyTuple();
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
        return new PyTuple(newArray);
    }

    public int __len__() {
        return tuple___len__();
    }

    @ExposedMethod
    final int tuple___len__() {
        return size();
    }
    
    @ExposedMethod
    final boolean tuple___contains__(PyObject o) {
        return super.__contains__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject tuple___ne__(PyObject o) {
        return super.__ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject tuple___eq__(PyObject o) {
        return super.__eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject tuple___gt__(PyObject o) {
        return super.__gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject tuple___ge__(PyObject o) {
        return super.__ge__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject tuple___lt__(PyObject o) {
        return super.__lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject tuple___le__(PyObject o) {
        return super.__le__(o);
    }

    public PyObject __add__(PyObject generic_other) {
        return tuple___add__(generic_other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject tuple___add__(PyObject generic_other) {
        PyTuple sum = null;
        if (generic_other instanceof PyTuple) {
            PyTuple otherTuple = (PyTuple)generic_other;
            PyObject[] array = getArray();
            PyObject[] otherArray = otherTuple.getArray();
            int thisLen = size();
            int otherLen = otherTuple.size();
            PyObject[] newArray = new PyObject[thisLen + otherLen];
            System.arraycopy(array, 0, newArray, 0, thisLen);
            System.arraycopy(otherArray, 0, newArray, thisLen, otherLen);
            sum = new PyTuple(newArray);
        }
        return sum;
    }

    @Override
    public PyObject __mul__(PyObject o) {
        return tuple___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
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

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject tuple___rmul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    public PyObject __iter__() {
        return tuple___iter__();
    }

    @ExposedMethod
    public PyObject tuple___iter__() {
        return new PyFastSequenceIter(this);
    }

    @ExposedMethod(defaults = "null")
    final PyObject tuple___getslice__(PyObject s_start, PyObject s_stop, PyObject s_step) {
        return seq___getslice__(s_start,s_stop,s_step);
    }

    @ExposedMethod
    final PyObject tuple___getitem__(PyObject index) {
        PyObject ret = seq___finditem__(index);
        if(ret == null) {
            throw Py.IndexError("index out of range: " + index);
        }
        return ret;
    }

    @ExposedMethod
    final PyTuple tuple___getnewargs__() {
        return new PyTuple(new PyTuple(list.getArray()));
    }

    public PyTuple __getnewargs__() {
        return tuple___getnewargs__();
    }

    public int hashCode() {
        return tuple___hash__();
    }

    @ExposedMethod
    final int tuple___hash__() {
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
        return tuple___repr__();
    }

    @ExposedMethod
    final String tuple___repr__() {
        StringBuilder buf = new StringBuilder("(");
        PyObject[] array = getArray();
        int arrayLen = size();
        for (int i = 0; i < arrayLen-1; i++) {
            buf.append(subobjRepr(array[i]));
            buf.append(", ");
        }
        if (arrayLen > 0)
            buf.append(subobjRepr(array[arrayLen-1]));
        if (arrayLen == 1)
            buf.append(",");
        buf.append(")");
        return buf.toString();
    }

    public List subList(int fromIndex, int toIndex) {
        return Collections.unmodifiableList(list.subList(fromIndex, toIndex));
    }

    // Make PyTuple immutable from the collections interfaces by overriding
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
}
