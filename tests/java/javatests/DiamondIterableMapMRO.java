// example from Storm and its use of Clojure

package javatests;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.python.util.Generic;

// The following tag interfaces duplicate the interface/abstract class supertypes of
// Storm's IndifferentAccessMap, including Clojure types

interface Seqable {}
interface IPersistentCollection extends Seqable {}
interface ILookup {}
interface Associative extends IPersistentCollection, ILookup {}
interface Counted {}
interface IPersistentMap extends Iterable, Associative, Counted {}
abstract class AFn implements IFn {}
interface IFn extends Callable, Runnable {}

public class DiamondIterableMapMRO extends AFn implements ILookup, IPersistentMap, Map {
    private final Map backing;

    public DiamondIterableMapMRO() {
        backing = Generic.map();
    }

    public Object call() { return null; }

    public void run() {}

    public Iterator iterator() {
        return backing.keySet().iterator();
    }

    public Set entrySet() {
        return backing.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return backing.equals(o);
    }

    @Override
    public int hashCode() {
        return backing.hashCode();
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return backing.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return backing.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return backing.get(key);
    }

    public Object put(Object key, Object value) {
        return backing.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return backing.remove(key);
    }

    public void putAll(Map m) {
        backing.putAll(m);
    }

    @Override
    public void clear() {
        backing.clear();
    }

    @Override
    public Set keySet() {
        return backing.keySet();
    }

    @Override
    public Collection values() {
        return backing.values();
    }
}

