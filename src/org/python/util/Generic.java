package org.python.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.python.core.util.ConcurrentHashSet;

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
    public static <T> Set<T> set() {
        return new HashSet<T>();
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
    public static <T> Set<T> concurrentSet() {
        return new ConcurrentHashSet<T>(CHM_INITIAL_CAPACITY, CHM_LOAD_FACTOR,
                                        CHM_CONCURRENCY_LEVEL);
    }
}
