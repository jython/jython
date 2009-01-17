package org.python.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Static methods to make instances of collections with their generic types inferred from what
 * they're being assigned to. The idea is stolen from <code>Sets</code>, <code>Lists</code> and
 * <code>Maps</code> from <a href="http://code.google.com/p/google-collections/">Google
 * Collections</a>.
 */
public class Generic {
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
}
