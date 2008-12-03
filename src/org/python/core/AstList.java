/*
 * I don't like that this is in org.python.core, but PySequence has package
 * private methods that I want to override. Hopefully we will clean up the
 * PyList hierarchy in the future, and then maybe we will find a more sensible
 * place for this class.
 */
package org.python.core;

import org.python.antlr.adapter.AstAdapter;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

@ExposedType(name = "_ast.astlist", base = PyList.class)
public class AstList extends PySequence implements Cloneable, List {

    public static final PyType TYPE = PyType.fromClass(AstList.class);

    private final static PyString[] fields = new PyString[0];

    @ExposedGet(name = "_fields")
    public PyString[] get_fields() { return fields; }


    /** The underlying Java List. */
    private List data;

    private AstAdapter adapter;

    public AstList() {
        this(TYPE, new ArrayList(), null);
    }

    public AstList(List data) {
        this(TYPE, data, null);
    }

    public AstList(List data, AstAdapter adapter) {
        this(TYPE, data, adapter);
    }

    public AstList(PyType type, List data, AstAdapter adapter) {
        super(TYPE);
        this.data = data;
        this.adapter = adapter;
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject astlist___ne__(PyObject o) {
        return seq___ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject astlist___eq__(PyObject o) {
        return seq___eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject astlist___lt__(PyObject o) {
        return seq___lt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject astlist___le__(PyObject o) {
        return seq___le__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject astlist___gt__(PyObject o) {
        return seq___gt__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject astlist___ge__(PyObject o) {
        return seq___ge__(o);
    }

    @ExposedMethod
    final boolean astlist___contains__(PyObject o) {
        return object___contains__(o);
    }

    @ExposedMethod
    final void astlist___delitem__(PyObject index) {
        seq___delitem__(index);
    }

    @ExposedMethod
    final void astlist___setitem__(PyObject o, PyObject def) {
        seq___setitem__(o, def);
    }

    @ExposedMethod
    final PyObject astlist___getitem__(PyObject o) {
        PyObject ret = seq___finditem__(o);
        if(ret == null) {
            throw Py.IndexError("index out of range: " + o);
        }
        return ret;
    }

    @ExposedMethod
    final boolean astlist___nonzero__() {
        return seq___nonzero__();
    }

    @ExposedMethod
    public PyObject astlist___iter__() {
        return seq___iter__();
    }

    @ExposedMethod(defaults = "null")
    final PyObject astlist___getslice__(PyObject start, PyObject stop, PyObject step) {
        return seq___getslice__(start, stop, step);
    }

    @ExposedMethod(defaults = "null")
    final void astlist___setslice__(PyObject start, PyObject stop, PyObject step, PyObject value) {
        if(value == null) {
            value = step;
            step = null;
        }
        seq___setslice__(start, stop, step, value);
    }

    @ExposedMethod(defaults = "null")
    final void astlist___delslice__(PyObject start, PyObject stop, PyObject step) {
        seq___delslice__(start, stop, step);
    }

    public PyObject __imul__(PyObject o) {
        return astlist___imul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject astlist___imul__(PyObject o) {
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

        int oldsize = data.size();
        for (int i = 1; i < count; i++) {
            data.addAll(data.subList(0, oldsize));
        }
        return this;
    }

    public PyObject __mul__(PyObject o) {
        return astlist___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject astlist___mul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    public PyObject __rmul__(PyObject o) {
        return astlist___rmul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject astlist___rmul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    public PyObject __iadd__(PyObject other) {
        return astlist___iadd__(other);
    }

    @ExposedMethod
    final PyObject astlist___iadd__(PyObject o) {
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

    public PyObject __add__(PyObject other) {
        return astlist___add__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject astlist___add__(PyObject o) {
        AstList sum = null;
        Object oList = o.__tojava__(List.class);
        if(oList != Py.NoConversion && oList != null) {
            List otherList = (List) oList;
            sum = new AstList();
            sum.extend(this);
            for(Iterator i = otherList.iterator(); i.hasNext();) {
                sum.add(i.next());
            }
        }
        return sum;
    }

    public PyObject __radd__(PyObject o) {
        return astlist___radd__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject astlist___radd__(PyObject o) {
        PyList sum = null;
        Object oList = o.__tojava__(List.class);
        if (oList != Py.NoConversion && oList != null) {
            sum = new PyList();
            sum.addAll((List) oList);
            sum.extend(this);
        }
        return sum;
    }

    public int __len__() {
        return data.size();
    }

    @Override
    public String toString() {
        return astlist_toString();
    }

    @ExposedMethod(names = "__repr__")
    final String astlist_toString() {
        return data.toString();
    }
 
    public void append(PyObject o) {
        astlist_append(o);
    }

    @ExposedMethod
    final void astlist_append(PyObject o) {
        data.add(o);
    }

    public Object clone() {
        return new AstList(this);
    }

    @ExposedMethod
    final int astlist_count(PyObject value) {
        int count = 0;
        for(Object o : data) {
            if(o.equals(value)) {
                count++;
            }
        }
        return count;
    }

    public int count(PyObject value) {
        return astlist_count(value);
    }

    public int index(PyObject o) {
        return astlist_index(o, 0, size());
    }

    public int index(PyObject o, int start) {
        return astlist_index(o, start, size());
    }

    public int index(PyObject o, int start, int stop) {
        return astlist_index(o, start, stop);
    }

    @ExposedMethod(defaults = {"null", "null"})
    final int astlist_index(PyObject o, PyObject start, PyObject stop) {
        int startInt = start == null ? 0 : PySlice.calculateSliceIndex(start);
        int stopInt = stop == null ? size() : PySlice.calculateSliceIndex(stop);
        return astlist_index(o, startInt, stopInt);
    }

    final int astlist_index(PyObject o, int start, int stop) {
        return _index(o, "astlist.index(x): x not in list", start, stop);
    }

    final int astlist_index(PyObject o, int start) {
        return _index(o, "astlist.index(x): x not in list", start, size());
    }

    final int astlist_index(PyObject o) {
        return _index(o, "astlist.index(x): x not in list", 0, size());
    }

    private int _index(PyObject o, String message, int start, int stop) {
        // Follow Python 2.3+ behavior
        int validStop = calculateIndex(stop);
        int validStart = calculateIndex(start);
        for(int i = validStart; i < validStop && i < size(); i++) {
            if(data.get(i).equals(o)) {
                return i;
            }
        }
        throw Py.ValueError(message);
    }

    protected void del(int i) {
        data.remove(i);
    }

    protected void delRange(int start, int stop, int step) {
        if(step >= 1) {
            for(int i = start; i < stop; i += step) {
                remove(i);
                i--;
                stop--;
            }
        } else if(step < 0) {
            for(int i = start; i >= 0 && i >= stop; i += step) {
                remove(i);
            }
        }
    }
    
    @ExposedMethod
    final void astlist_extend(PyObject iterable){
        int length = size();
        setslice(length, length, 1, iterable);
    }

    public void extend(PyObject iterable) {
        astlist_extend(iterable);
    }

    protected PyObject getslice(int start, int stop, int step) {
        if(step > 0 && stop < start) {
            stop = start;
        }
        int n = sliceLength(start, stop, step);
        List newList = data.subList(start, stop);
        if(step == 1) {
            newList = data.subList(start, stop);
            return new AstList(newList);
        }
        int j = 0;
        for(int i = start; j < n; i += step) {
            newList.set(j, data.get(i));
            j++;
        }
        return new AstList(newList);
    }

    public void insert(int index, PyObject o) {
        astlist_insert(index, o);
    }

    @ExposedMethod
    final void astlist_insert(int index, PyObject o) {
        if(index < 0) {
            index = Math.max(0, size() + index);
        }
        if(index > size()) {
            index = size();
        }
        data.add(index, o);
    }
    
    @ExposedMethod
    final void astlist_remove(PyObject value){
        del(_index(value, "astlist.remove(x): x not in list", 0, size()));
    }

    public void remove(PyObject value) {
        astlist_remove(value);
    }

    public void reverse() {
        astlist_reverse();
    }

    @ExposedMethod
    final void astlist_reverse() {
        Collections.reverse(data);
    }

    public PyObject pop() {
        return pop(-1);
    }

    public PyObject pop(int n) {
        return astlist_pop(n);
    }

    @ExposedMethod(defaults = "-1")
    final PyObject astlist_pop(int n) {
        return (PyObject)data.remove(n);
    }

    protected PyObject repeat(int count) {
        if (count < 0) {
            count = 0;
        }
        int size = size();
        int newSize = size * count;
        if (count != 0 && newSize / count != size) {
            throw Py.MemoryError("");
        }

        List newList = new ArrayList();
        for(int i = 0; i < count; i++) {
            newList.addAll(data);
        }
        return new AstList(newList);
    }
    
    protected void set(int i, PyObject value) {
        data.set(i, value);
    }

    protected void setslice(int start, int stop, int step, PyObject value) {
        //FIXME
    }
    
    public void add(int index, Object element) {
        data.add(index, element);
    }

    public boolean add(Object o) {
        return data.add(o);
    }

    public boolean addAll(int index, Collection c) {
        return data.addAll(index, c);
    }

    public boolean addAll(Collection c) {
        return data.addAll(c);
    }

    public void clear() {
        data.clear();
    }

    public boolean contains(Object o) {
        return data.contains(o);
    }

    public boolean containsAll(Collection c) {
        return data.containsAll(c);
    }

    public Object get(int index) {
        return data.get(index);
    }

    public int indexOf(Object o) {
        return data.indexOf(o);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Iterator iterator() {
        return data.iterator();
    }

    public int lastIndexOf(Object o) {
        return data.lastIndexOf(o);
    }

    public ListIterator listIterator() {
        return data.listIterator();
    }

    public ListIterator listIterator(int index) {
        return data.listIterator(index);
    }

    public boolean pyadd(PyObject o) {
        data.add(o);
        return true;
    }

    public void pyadd(int index, PyObject element) {
        data.add(index, element);
    }

    public PyObject pyget(int index) {
        if (adapter == null) {
            return (PyObject)data.get(index);
        }
        return adapter.ast2py(data.get(index));
    }

    public PyObject pyset(int index, PyObject element) {
        return (PyObject)data.set(index, element);
    }

    public Object remove(int index) {
        return data.remove(index);
    }

    public boolean remove(Object o) {
        return data.remove(o);
    }

    public boolean removeAll(Collection c) {
        return data.removeAll(c);
    }

    public boolean retainAll(Collection c) {
        return data.retainAll(c);
    }

    public Object set(int index, Object element) {
        return data.set(index, element);
    }

    public int size() {
        return data.size();
    }

    public List subList(int fromIndex, int toIndex) {
        return data.subList(fromIndex, toIndex);
    }

    public Object[] toArray() {
        return data.toArray();
    }

    public Object[] toArray(Object[] a) {
        return data.toArray(a);
    }

    public Object __tojava__(Class c) {
        if(c.isInstance(this)) {
            return this;
        }
        return Py.NoConversion;
    }

}
