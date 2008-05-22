package org.python.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class BaseSet extends PyObject /*implements Set*/ {

    /**
     * The underlying container.  HashSet is used rather than Set because
     * clone is protected on Object and I didn't want to cast.
     */
    protected HashSet _set;

    /**
     * Create a new, empty set instance.
     */
    public BaseSet() {
        super();
        _set = new HashSet();
    }

    /**
     * Create a new set instance from the values of the iterable object.
     *
     * @param data An iterable instance.
     */
    public BaseSet(PyObject data) {
        super();
        _set = new HashSet();
        _update(data);
    }

    public BaseSet(PyType type) {
        super(type);
        _set = new HashSet();
    }

    /**
     * Update the underlying set with the contents of the iterable.
     *
     * @param data An iterable instance.
     * @throws PyIgnoreMethodTag Ignore.
     */
    protected void _update(PyObject data) throws PyIgnoreMethodTag {
        if (data instanceof BaseSet) {
            // Skip the iteration if both are sets
            _set.addAll(((BaseSet)data)._set);
            return;
        }
        for (PyObject item : data.asIterable()) {
            _set.add(item);
        }
    }

    /**
     * The union of <code>this</code> with <code>other</code>. <p/> <br/> (I.e. all elements
     * that are in either set)
     * 
     * @param other
     *            A <code>BaseSet</code> instance.
     * @return The union of the two sets as a new set.
     */
    public PyObject __or__(PyObject other) {
        return baseset___or__(other);
    }

    final PyObject baseset___or__(PyObject other) {
        if (!(other instanceof BaseSet)) {
            throw Py.TypeError("Not Implemented");
        }
        return baseset_union(other);
    }

    /**
     * The intersection of the <code>this</code> with <code>other</code>.
     * <p/>
     * <br/>
     * (I.e. all elements that are in both sets)
     *
     * @param other A <code>BaseSet</code> instance.
     * @return The intersection of the two sets as a new set.
     */
    public PyObject __and__(PyObject other) {
        return baseset___and__(other);
    }

    final PyObject baseset___and__(PyObject other) {
        if (!(other instanceof BaseSet)) {
            throw Py.TypeError("Not Implemented");
        }
        return baseset_intersection(other);
    }

    /**
     * The difference of the <code>this</code> with <code>other</code>.
     * <p/>
     * <br/>
     * (I.e. all elements that are in this set and not in the other)
     *
     * @param other A <code>BaseSet</code> instance.
     * @return The difference of the two sets as a new set.
     */
    public PyObject __sub__(PyObject other) {
        return baseset___sub__(other);
    }

    final PyObject baseset___sub__(PyObject other) {
        if (!(other instanceof BaseSet)) {
            throw Py.TypeError("Not Implemented");
        }
        return difference(other);
    }

    public PyObject difference(PyObject other) {
        return baseset_difference(other);
    }

    final PyObject baseset_difference(PyObject other) {
        BaseSet bs = (other instanceof BaseSet) ? (BaseSet)other : new PySet(other);
        Set set = bs._set;
        BaseSet o = BaseSet.makeNewSet(getType());
        for (Object p : _set) {
            if (!set.contains(p)) {
                o._set.add(p);
            }
        }

        return o;
    }

    /**
     * The symmetric difference of the <code>this</code> with <code>other</code>.
     * <p/>
     * <br/>
     * (I.e. all elements that are in exactly one of the sets)
     *
     * @param other A <code>BaseSet</code> instance.
     * @return The symmetric difference of the two sets as a new set.
     */
    public PyObject __xor__(PyObject other) {
        return baseset___xor__(other);
    }

    final PyObject baseset___xor__(PyObject other) {
        if (!(other instanceof BaseSet)) {
            throw Py.TypeError("Not Implemented");
        }
        return symmetric_difference(other);
    }

    public PyObject symmetric_difference(PyObject other) {
        return baseset_symmetric_difference(other);
    }

    final PyObject baseset_symmetric_difference(PyObject other) {
        BaseSet bs = (other instanceof BaseSet) ? (BaseSet)other : new PySet(other);
        BaseSet o = BaseSet.makeNewSet(getType());
        for (Object p : _set) {
            if (!bs._set.contains(p)) {
                o._set.add(p);
            }
        }
        for (Object p : bs._set) {
            if (!_set.contains(p)) {
                o._set.add(p);
            }
        }
        return o;
    }

    /**
     * The hashCode of the set.  Only immutable instances can be hashed.
     *
     * @return The hashCode of the set.
     */
    public abstract int hashCode();

    /**
     * The length of the set.
     *
     * @return The length of the set.
     */
    public int __len__() {
        return baseset___len__();
    }

    final int baseset___len__() {
        return _set.size();
    }

    /**
     * Determines if the instance is considered <code>true</code> by Python.
     * This implementation returns true if the set is not empty.
     *
     * @return <code>true</code> if the set is not empty, <code>false</code> otherwise
     */
    public boolean __nonzero__() {
        return !_set.isEmpty();
    }

    /**
     * Produce an iterable object.
     *
     * @return An iteration of the set.
     */
    public PyObject __iter__() {
        return baseset___iter__();
    }

    final PyObject baseset___iter__() {
        return new PySetIterator(_set);
    }

    public boolean __contains__(PyObject other) {
        return baseset___contains__(other);
    }

    final boolean baseset___contains__(PyObject other) {
        try {
            return _set.contains(other);
        } catch (PyException pye) {
            PyFrozenSet frozen = asFrozen(pye, other);
            return _set.contains(frozen);
        }
    }

    public int __cmp__(PyObject other) {
        return baseset___cmp__(other);
    }

    final int baseset___cmp__(PyObject other) {
        throw Py.TypeError("cannot compare sets using cmp()");
    }

    public PyObject __eq__(PyObject other) {
        return baseset___eq__(other);
    }

    final PyObject baseset___eq__(PyObject other) {
        if (other instanceof BaseSet) {
            return Py.newBoolean(_set.equals(((BaseSet)other)._set));
        }
        return Py.False;
    }

    public PyObject __ne__(PyObject other) {
        return baseset___ne__(other);
    }

    final PyObject baseset___ne__(PyObject other) {
        if(other instanceof BaseSet) {
            return Py.newBoolean(!_set.equals(((BaseSet)other)._set));
        }
        return Py.True;
    }

    public PyObject __le__(PyObject other) {
        return baseset___le__(other);
    }

    final PyObject baseset___le__(PyObject other) {
        BaseSet bs = _binary_sanity_check(other);
        return baseset_issubset(other);
    }

    public PyObject __ge__(PyObject other) {
        return baseset___ge__(other);
    }

    final PyObject baseset___ge__(PyObject other) {
        BaseSet bs = _binary_sanity_check(other);
        return baseset_issuperset(other);
    }

    public PyObject __lt__(PyObject other) {
        return baseset___lt__(other);
    }

    final PyObject baseset___lt__(PyObject other) {
        BaseSet bs = _binary_sanity_check(other);
        return Py.newBoolean(__len__() < bs.__len__()
          && baseset_issubset(other).__nonzero__());
    }

    public PyObject __gt__(PyObject other) {
        return baseset___gt__(other);
    }

    final PyObject baseset___gt__(PyObject other) {
        BaseSet bs = _binary_sanity_check(other);
        return Py.newBoolean(__len__() > bs.__len__()
          && baseset_issuperset(other).__nonzero__());
    }

    /**
     * Used for pickling.  Uses the module <code>setsfactory</sets> to
     * export safe constructors.
     *
     * @return a tuple of (constructor, (elements))
     */
    public PyObject __reduce__() {
        return baseset___reduce__();
    }
    
    final PyObject baseset___reduce__(){
        PyObject args = new PyTuple(new PyList(this));
        PyObject dict = __findattr__("__dict__");
        if (dict == null) {
            dict = Py.None;
        }
        return new PyTuple(getType(), args, dict);
    }

    /**
     * Return this instance as a Java object.  Only coerces to Collection and subinterfaces.
     *
     * @param c The Class to coerce to.
     * @return the underlying HashSet (not a copy)
     */
    public Object __tojava__(Class c) {
        if (Collection.class.isAssignableFrom(c)) {
            return Collections.unmodifiableSet(_set);
        }
        return super.__tojava__(c);
    }

    final PyObject baseset_union(PyObject other) {
        BaseSet result = BaseSet.makeNewSet(getType(), this);
        result._update(other);
        return result;
    }

    final PyObject baseset_intersection(PyObject other) {
        PyObject little, big;
        if(!(other instanceof BaseSet)) {
            other = new PySet(other);
        }

        if (__len__() <= __builtin__.len(other)) {
            little = this;
            big = other;
        } else {
            little = other;
            big = this;
        }

        PyObject common = __builtin__.filter(big.__getattr__("__contains__"), little);
        return BaseSet.makeNewSet(getType(), common);
    }

    final PyObject baseset_copy() {
        BaseSet copy = BaseSet.makeNewSet(getType(), this);
        return copy;
    }

    final PyObject baseset_issubset(PyObject other) {
        BaseSet bs = (other instanceof BaseSet) ? (BaseSet)other : new PySet(other);
        if (__len__() > bs.__len__()) {
            return Py.False;
        }
        for (Object p : _set) {
            if (!bs._set.contains(p)) {
                return Py.False;
            }
        }
        return Py.True;
    }

    final PyObject baseset_issuperset(PyObject other) {
        BaseSet bs = (other instanceof BaseSet) ? (BaseSet)other : new PySet(other);
        if (__len__() < bs.__len__()) {
            return Py.False;
        }
        for (Object p : bs._set) {
            if (!_set.contains(p)) {
                return Py.False;
            }
        }
        return Py.True;
    }

    public String toString() {
        return baseset_toString();
    }

    final String baseset_toString() {
        String name = getType().fastGetName();
        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) {
            return name + "(...)";
        }
        StringBuffer buf = new StringBuffer(name).append("([");
        for (Iterator i = _set.iterator(); i.hasNext();) {
            buf.append(((PyObject)i.next()).__repr__().toString());
            if (i.hasNext()) {
                buf.append(", ");
            }
        }
        buf.append("])");
        ts.exitRepr(this);
        return buf.toString();
    }

    protected final BaseSet _binary_sanity_check(PyObject other) throws PyIgnoreMethodTag {
        try {
            return (BaseSet)other;
        } catch (ClassCastException e) {
            throw Py.TypeError("Binary operation only permitted between sets");
        }
    }

    /**
     * Return a PyFrozenSet whose contents are shared with value when
     * value is a BaseSet and pye is a TypeError.
     *
     * WARNING: The PyFrozenSet returned is only intended to be used
     * temporarily (and internally); since its contents are shared
     * with value, it could be mutated!
     * 
     * This is better than special-casing behavior based on
     * isinstance, because a Python subclass can override, say,
     * __hash__ and all of a sudden you can't assume that a
     * non-PyFrozenSet is unhashable anymore.
     *
     * @param pye   The exception thrown from a hashable operation.
     * @param value The object which was unhashable.
     * @return A PyFrozenSet if appropriate, otherwise the pye is rethrown
     */
    protected final PyFrozenSet asFrozen(PyException pye, PyObject value) {
        if (!(value instanceof BaseSet) || !Py.matchException(pye, Py.TypeError)) {
            throw pye;
        }
        PyFrozenSet tmp = new PyFrozenSet();
        tmp._set = ((BaseSet)value)._set;
        return tmp;
    }

    /**
     * Create a new set of type.
     *
     * @param type a set type
     * @return a new set
     */
    protected static BaseSet makeNewSet(PyType type) {
        return makeNewSet(type, null);
    }
 
    /**
     * Create a new <et of type from iterable.
     *
     * @param type a set type
     * @param iterable an iterable or null
     * @return a new set
     */
    protected static BaseSet makeNewSet(PyType type, PyObject iterable) {
        BaseSet so;
        if (type == PySet.TYPE) {
            so = new PySet();
        } else if (type == PyFrozenSet.TYPE) {
            so = new PyFrozenSet();
        } else if (Py.isSubClass(type, PySet.TYPE)) {
            so = new PySetDerived(type);
        } else {
            so = new PyFrozenSetDerived(type);
        }
        if (iterable != null) {
            so._update(iterable);
        }
        return so;
    }

 //    public int size() {
//        return _set.size();
//    }
//
//    public void clear() {
//        _set.clear();
//    }
//
//    public boolean isEmpty() {
//        return _set.isEmpty();
//    }
//
//    public Object[] toArray() {
//        return _set.toArray();
//    }
//
//    public boolean add(Object o) {
//        return _set.add(o);
//    }
//
//    public boolean contains(Object o) {
//        return _set.contains(o);
//    }
//
//    public boolean remove(Object o) {
//        return _set.remove(o);
//    }
//
//    public boolean addAll(Collection c) {
//        return _set.addAll(c);
//    }
//
//    public boolean containsAll(Collection c) {
//        return _set.containsAll(c);
//    }
//
//    public boolean removeAll(Collection c) {
//        return _set.removeAll(c);
//    }
//
//    public boolean retainAll(Collection c) {
//        return _set.retainAll(c);
//    }
//
//    public Iterator iterator() {
//        return _set.iterator();
//    }
//
//    public Object[] toArray(Object a[]) {
//        return _set.toArray(a);
//    }
}
