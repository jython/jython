
package org.python.modules.sets;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;

import java.util.Iterator;

public class PySet extends BaseSet {
    public PySet() {
        super();
    }

    public PySet(PyObject data) {
        super(data);
    }

    public PyObject __ior__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set.addAll(bs._set);
        return this;
    }

    public PyObject __iand__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set = ((BaseSet) this.__and__(bs))._set;
        return this;
    }

    public PyObject __ixor__(PyObject other) {
        this._binary_sanity_check(other);
        this.symmetric_difference_update(other);
        return this;
    }

    public PyObject __isub__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set.removeAll(bs._set);
        return this;
    }

    public int hashCode() {
        throw Py.TypeError("Can't hash a Set, only an ImmutableSet.");
    }

    public void add(PyObject o) {
        try {
            this._set.add(o);
        } catch (PyException e) {
            PyObject immutable = this.asImmutable(e, o);
            this._set.add(immutable);
        }
    }

    public void remove(PyObject o) {
        boolean b = false;
        try {
            b = this._set.remove(o);
        } catch (PyException e) {
            PyObject immutable = this.asImmutable(e, o);
            b = this._set.remove(immutable);
        }
        if (!b) {
            throw new PyException(Py.LookupError, o.toString());
        }
    }

    public void discard(PyObject o) {
        try {
            this._set.remove(o);
        } catch (PyException e) {
            PyObject immutable = this.asImmutable(e, o);
            this._set.remove(immutable);
        }
    }

    public PyObject pop() {
        Iterator iterator = this._set.iterator();
        Object first = iterator.next();
        this._set.remove(first);
        return (PyObject) first;
    }

    public void clear() {
        this._set.clear();
    }

    public void update(PyObject data) {
        this._update(data);
    }

    public void union_update(PyObject other) {
        this._update(other);
    }

    public void intersection_update(PyObject other) {
        if(other instanceof BaseSet) {
            this.__iand__(other);
        } else {
            BaseSet set = (BaseSet)intersection(other);
            this._set = set._set;
        }
    }

    public void symmetric_difference_update(PyObject other) {
        BaseSet bs = (other instanceof BaseSet) ? (BaseSet) other : new PySet(other);
        for (Iterator iterator = bs._set.iterator(); iterator.hasNext();) {
            Object o = iterator.next();
            if (this._set.contains(o)) {
                this._set.remove(o);
            } else {
                this._set.add(o);
            }
        }
    }

    public void difference_update(PyObject other) {
        if(other instanceof BaseSet) {
            this.__isub__(other);
            return;
        }
        PyObject iter = other.__iter__();
        for (PyObject o; (o = iter.__iternext__()) != null;) {
            if(this.__contains__(o)) {
                this.remove(o);
            }
        }
    }

    public PyObject _as_immutable() {
        return new PyImmutableSet(this);
    }
}
