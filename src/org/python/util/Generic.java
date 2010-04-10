package org.python.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Static methods to make instances of collections with their generic types inferred from what
 * they're being assigned to. The idea is stolen from <code>Sets</code>, <code>Lists</code> and
 * <code>Maps</code> from <a href="http://code.google.com/p/google-collections/">Google
 * Collections</a>.
 */
public class Generic {

    /**
     * Our default ConcurrentHashMap sizes. Only concurreny level differs from
     * ConcurrentHashMap's defaults: it's significantly lower to reduce allocation cost.
     */
    public static final int CHM_INITIAL_CAPACITY = 16;
    public static final float CHM_LOAD_FACTOR = 0.75f;
    public static final int CHM_CONCURRENCY_LEVEL = 2;

    /**
     * Makes a List with its generic type inferred from whatever it's being assigned to.
     */
    public static <T> List<T> list() {
        return new ArrayList<T>();
    }
    /**
     * Makes a List with its generic type inferred from whatever it's being assigned to filled with
     * the items in <code>contents</code>.
     */
    public static <T, U extends T> List<T> list(U...contents) {
        List<T> l = new ArrayList<T>(contents.length);
        for (T t : contents) {
            l.add(t);
        }
        return l;
    }

    /**
     * Makes a Map using generic types inferred from whatever this is being assigned to.
     */
    public static <K, V> Map<K, V> map() {
        return new HashMap<K, V>();
    }

    /**
     * Makes a ConcurrentMap using generic types inferred from whatever this is being
     * assigned to.
     */
    public static <K, V> ConcurrentMap<K, V> concurrentMap() {
        return new ConcurrentHashMap<K, V>(CHM_INITIAL_CAPACITY, CHM_LOAD_FACTOR,
                                           CHM_CONCURRENCY_LEVEL);
    }

    /**
     * Makes a Set using the generic type inferred from whatever this is being assigned to.
     */
    public static <E> Set<E> set() {
        return new HashSet<E>();
    }

    /**
     * Makes a Set using the generic type inferred from whatever this is being assigned to filled
     * with the items in <code>contents</code>.
     */
    public static <T, U extends T> Set<T> set(U...contents) {
        Set<T> s = new HashSet<T>(contents.length);
        for (U u : contents) {
            s.add(u);
        }
        return s;
    }

    /**
     * Makes a Set, ensuring safe concurrent operations, using generic types inferred from
     * whatever this is being assigned to.
     */
    public static <E> Set<E> concurrentSet() {
        return newSetFromMap(new ConcurrentHashMap<E, Boolean>(CHM_INITIAL_CAPACITY,
                                                               CHM_LOAD_FACTOR,
                                                               CHM_CONCURRENCY_LEVEL));
    }

    /**
     * Return a Set backed by the specified Map with the same ordering, concurrency and
     * performance characteristics.
     *
     * The specified Map must be empty at the time this method is invoked.
     *
     * Note that this method is based on Java 6's Collections.newSetFromMap, and will be
     * removed in a future version of Jython (likely 2.6) that will rely on Java 6.
     *
     * @param map the backing Map
     * @return a Set backed by the Map
     * @throws IllegalArgumentException if Map is not empty
     */
    public static <E> Set<E> newSetFromMap(Map<E, Boolean> map) {
        return new SetFromMap<E>(map);
    }

    /**
     * A Set backed by a generic Map.
     */
    private static class SetFromMap<E> extends AbstractSet<E>
        implements Serializable {

        /** The backing Map. */
        private final Map<E, Boolean> map;

        /** Backing's KeySet. */
        private transient Set<E> keySet;

        public SetFromMap(Map<E, Boolean> map) {
            if (!map.isEmpty()) {
                throw new IllegalArgumentException("Map is non-empty");
            }
            this.map = map;
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
        public boolean containsAll(Collection<?> c) {
            return keySet.containsAll(c);
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
            return o == this || keySet.equals(o);
        }

        @Override
        public int hashCode() {
            return keySet.hashCode();
        }

        @Override
        public String toString() {
            return keySet.toString();
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            keySet = map.keySet();
        }
    }

}
