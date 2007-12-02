
package org.python.modules.sets;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyIgnoreMethodTag;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.__builtin__;
import org.python.core.PyType;

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
        this._set = new HashSet();
    }

    /**
     * Create a new set instance from the values of the iterable object.
     *
     * @param data An iterable instance.
     */
    public BaseSet(PyObject data) {
        super();
        this._set = new HashSet();
        this._update(data);
    }

    public BaseSet(PyType type) {
        super(type);
        this._set = new HashSet();
    }

    /**
     * Update the underlying set with the contents of the iterable.
     *
     * @param data An iterable instance.
     * @throws PyIgnoreMethodTag Ignore.
     */
    protected void _update(PyObject data) throws PyIgnoreMethodTag {

        if(data instanceof BaseSet) {
            // Skip the iteration if both are sets
            this._set.addAll(((BaseSet)data)._set);
            return;
        }

        PyObject value = null;
        if (data.__findattr__("__iter__") != null) {
            PyObject iter = data.__iter__();
            while ((value = iter.__iternext__()) != null) {
                try {
                    this._set.add(value);
                } catch (PyException e) {
                    PyObject immutable = this.asImmutable(e, value);
                    this._set.add(immutable);
                }
            }
        } else {
            int i = 0;
            while(true) {
                try {
                    value = data.__finditem__(i++);
                    if (value == null) {
                        break;
                    }
                } catch (PyException e) {
                    if(Py.matchException(e, Py.AttributeError)) {
                        throw Py.TypeError("object not iterable");
                    }
                    throw e;
                }
                try {
                    this._set.add(value);
                } catch (PyException e) {
                    PyObject immutable = this.asImmutable(e, value);
                    this._set.add(immutable);
                }
            }
        }
    }

    /**
     * The union of <code>this</code> with <code>other</code>.
     * <p/>
     * <br/>
     * (I.e. all elements that are in either set)
     *
     * @param other A <code>BaseSet</code> instance.
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
        BaseSet iterable = (other instanceof BaseSet) ? (BaseSet) other : new PySet(other);
        Set set = iterable._set;
        BaseSet o = (BaseSet) this.getType().__call__();
        for (Iterator i = this._set.iterator(); i.hasNext();) {
            Object p = i.next();
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

    public PyObject baseset_symmetric_difference(PyObject other) {
        BaseSet iterable = (other instanceof BaseSet) ? (BaseSet) other : new PySet(other);
        BaseSet o = (BaseSet) this.getType().__call__();
        for (Iterator i = this._set.iterator(); i.hasNext();) {
            Object p = i.next();
            if (!iterable._set.contains(p)) {
                o._set.add(p);
            }
        }
        for (Iterator i = iterable._set.iterator(); i.hasNext();) {
            Object p = i.next();
            if (!this._set.contains(p)) {
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
        return this._set.size();
    }

    /**
     * Determines if the instance is considered <code>true</code> by Python.
     * This implementation returns true if the set is not empty.
     *
     * @return <code>true</code> if the set is not empty, <code>false</code> otherwise
     */
    public boolean __nonzero__() {
        return baseset___nonzero__();
    }

    final boolean baseset___nonzero__() {
        return !this._set.isEmpty();
    }

    /**
     * Produce an iterable object.
     *
     * @return An iteration of the set.
     */
    public PyObject __iter__() {
        return new PySetIterator(this._set);
    }

    public boolean __contains__(PyObject other) {
        return baseset___contains__(other);
    }

    final boolean baseset___contains__(PyObject other) {
        return this._set.contains(other);
    }

    public PyObject __eq__(PyObject other) {
        return baseset___eq__(other);
    }

    final PyObject baseset___eq__(PyObject other) {
        if(other instanceof BaseSet) {
            BaseSet bs = this._binary_sanity_check(other);
            return Py.newBoolean(this._set.equals(bs._set));
        }
        return Py.Zero;
    }

    public PyObject __ne__(PyObject other) {
        return baseset___ne__(other);
    }

    final PyObject baseset___ne__(PyObject other) {
        if(other instanceof BaseSet) {
            BaseSet bs = this._binary_sanity_check(other);
            return Py.newBoolean(!this._set.equals(bs._set));
        }
        return Py.One;
    }

    public PyObject __le__(PyObject other) {
        return baseset___le__(other);
    }

    final PyObject baseset___le__(PyObject other) {
        return this.baseset_issubset(other);
    }

    public PyObject __ge__(PyObject other) {
        return baseset___ge__(other);
    }

    final PyObject baseset___ge__(PyObject other) {
        return this.baseset_issuperset(other);
    }

    public PyObject __lt__(PyObject other) {
        return baseset___lt__(other);
    }

    final PyObject baseset___lt__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        return Py.newBoolean(this.__len__() < bs.__len__()
          && this.baseset_issubset(other).__nonzero__());
    }

    public PyObject __gt__(PyObject other) {
        return baseset___gt__(other);
    }

    public PyObject baseset___gt__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        return Py.newBoolean(this.__len__() > bs.__len__()
          && this.baseset_issuperset(other).__nonzero__());
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
        String name = getType().getFullName();
        PyObject factory = __builtin__.__import__("setsfactory");
        PyObject func = factory.__getattr__(name);
        return new PyTuple(func, new PyTuple(new PyList(this)));
    }

    public PyObject __deepcopy__(PyObject memo) {
        return baseset___deepcopy__(memo);
    }

    final PyObject baseset___deepcopy__(PyObject memo) {
        PyObject copy = __builtin__.__import__("copy");
        PyObject deepcopy = copy.__getattr__("deepcopy");
        BaseSet result = (BaseSet) this.getType().__call__();
        memo.__setitem__(Py.newInteger(Py.id(this)), result);
        for (Iterator iterator = this._set.iterator(); iterator.hasNext();) {
            result._set.add(deepcopy.__call__(Py.java2py(iterator.next()), memo));
        }
        return result;
    }

    /**
     * Return this instance as a Java object.  Only coerces to Collection and subinterfaces.
     *
     * @param c The Class to coerce to.
     * @return the underlying HashSet (not a copy)
     */
    public Object __tojava__(Class c) {
        if (Collection.class.isAssignableFrom(c)) {
            return Collections.unmodifiableSet(this._set);
        }
        return super.__tojava__(c);
    }

    public PyObject baseset_union(PyObject other) {
        BaseSet result = (BaseSet) this.getType().__call__(this);
        result._update(other);
        return result;
    }

    public PyObject baseset_intersection(PyObject other) {

        PyObject little, big;
        if(!(other instanceof BaseSet)) {
            other = new PySet(other);
        }

        if (this.__len__() <= __builtin__.len(other)) {
            little = this;
            big = other;
        } else {
            little = other;
            big = this;
        }

        PyObject common = __builtin__.filter(big.__getattr__("__contains__"), little);
        return other.getType().__call__(common);
    }

    public PyObject baseset_copy() {
        BaseSet copy = (BaseSet) this.getType().__call__();
        copy._set = (HashSet) this._set.clone();
        return copy;
    }

    public PyObject baseset_issubset(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        if (this.__len__() > bs.__len__()) {
            return Py.Zero;
        }
        for (Iterator iterator = this._set.iterator(); iterator.hasNext();) {
            if (!bs._set.contains(iterator.next())) {
                return Py.Zero;
            }
        }
        return Py.One;
    }

    public PyObject baseset_issuperset(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        if (this.__len__() < bs.__len__()) {
            return Py.Zero;
        }
        for (Iterator iterator = bs._set.iterator(); iterator.hasNext();) {
            if (!this._set.contains(iterator.next())) {
                return Py.Zero;
            }
        }
        return Py.One;
    }

    final String baseset_toString() {
        return toString();
    }

    public String toString() {
        String name = getType().getFullName();
        StringBuffer buf = new StringBuffer(name).append("([");
        for (Iterator i = this._set.iterator(); i.hasNext();) {
            buf.append(((PyObject) i.next()).__repr__().toString());
            if (i.hasNext()) {
                buf.append(", ");
            }
        }
        buf.append("])");
        return buf.toString();
    }

    protected final BaseSet _binary_sanity_check(PyObject other) throws PyIgnoreMethodTag {
        try {
            return (BaseSet) other;
        } catch (ClassCastException e) {
            throw Py.TypeError("Binary operation only permitted between sets");
        }
    }

    /**
     * If the exception <code>e</code> is a <code>TypeError</code>, attempt to convert
     * the object <code>value</code> into an ImmutableSet.
     *
     * @param e     The exception thrown from a hashable operation.
     * @param value The object which was unhashable.
     * @return An ImmutableSet if available, a <code>TypeError</code> is thrown otherwise.
     */
    protected final PyObject asImmutable(PyException e, PyObject value) {
        if (Py.matchException(e, Py.TypeError)) {
            PyObject transform = value.__findattr__("_as_immutable");
            if (transform != null) {
                return transform.__call__();
            }
        }
        throw e;
    }

//    public int size() {
//        return this._set.size();
//    }
//
//    public void clear() {
//        this._set.clear();
//    }
//
//    public boolean isEmpty() {
//        return this._set.isEmpty();
//    }
//
//    public Object[] toArray() {
//        return this._set.toArray();
//    }
//
//    public boolean add(Object o) {
//        return this._set.add(o);
//    }
//
//    public boolean contains(Object o) {
//        return this._set.contains(o);
//    }
//
//    public boolean remove(Object o) {
//        return this._set.remove(o);
//    }
//
//    public boolean addAll(Collection c) {
//        return this._set.addAll(c);
//    }
//
//    public boolean containsAll(Collection c) {
//        return this._set.containsAll(c);
//    }
//
//    public boolean removeAll(Collection c) {
//        return this._set.removeAll(c);
//    }
//
//    public boolean retainAll(Collection c) {
//        return this._set.retainAll(c);
//    }
//
//    public Iterator iterator() {
//        return this._set.iterator();
//    }
//
//    public Object[] toArray(Object a[]) {
//        return this._set.toArray(a);
//    }
}
