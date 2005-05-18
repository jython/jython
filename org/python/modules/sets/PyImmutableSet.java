
package org.python.modules.sets;

import org.python.core.PyObject;

import java.util.Collection;

public class PyImmutableSet extends BaseSet {

    public PyImmutableSet() {
        super();
    }

    public PyImmutableSet(PyObject data) {
        super(data);
    }

    public int hashCode() {
        return this._set.hashCode();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException();
    }
}
