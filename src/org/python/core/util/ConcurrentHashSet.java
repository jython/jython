/* Copyright (c) Jython Developers */
package org.python.core.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A Set backed by ConcurrentHashMap.
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> implements Serializable {

    /** The backing Map. */
    private final ConcurrentMap<E, Boolean> map;

    /** Backing's KeySet. */
    private transient Set<E> keySet;

    public ConcurrentHashSet() {
        map = new ConcurrentHashMap<E, Boolean>();
        keySet = map.keySet();
    }

    public ConcurrentHashSet(int initialCapacity) {
        map = new ConcurrentHashMap<E, Boolean>(initialCapacity);
        keySet = map.keySet();
    }

    public ConcurrentHashSet(int initialCapacity, float loadFactor, int concurrencyLevel) {
        map = new ConcurrentHashMap<E, Boolean>(initialCapacity, loadFactor, concurrencyLevel);
        keySet = map.keySet();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<E> iterator() {
        return keySet.iterator();
    }

    @Override
    public Object[] toArray() {
        return keySet.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return keySet.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return map.put(e, Boolean.TRUE) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return keySet.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return keySet.retainAll(c);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean equals(Object o) {
        return keySet.equals(o);
    }

    @Override
    public int hashCode() {
        return keySet.hashCode();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        keySet = map.keySet();
    }
}
