// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.python.internal.Util;
import org.python.kernel.BaseType;
import org.python.kernel.KernelTypeFlag;

/**
 * Abstract API for operations on mapping types, corresponding to
 * CPython methods defined in {@code abstract.h} and with names like:
 * {@code PyMapping_*}.
 */
public class PyMapping extends Abstract {

    private PyMapping() {}   // only static methods here

    /**
     * Check that an object o is a Python mapping. See
     * {@link PyType#isMapping()}.
     *
     * @param o to test
     * @return whether {@code type(o)} is a mapping
     */
    // Compare CPython PyMapping_Check in abstract.c
    public static boolean check(Object o) { return PyType.of(o).isMapping(); }

    /**
     * Return the mapping object {@code o} as a Java {@code Map}. If
     * {@code o} is one of several built-in types that implement Java
     * {@code Map<Object, Object>}, this will be the object itself.
     * Otherwise, it will be an adapter on the provided object.
     *
     * @param o to present as a map
     * @return the map
     */
    static Map<Object, Object> map(Object o) {
        if (o instanceof PyDict) {
            return (PyDict)o;
        } else {
            return new MapWrapper(o);
        }
    }

    /**
     * A wrapper on objects that conform to the Python mapping protocol, to
     * make them act (almost) as a Java {@code Map}.
     */
    static class MapWrapper extends AbstractMap<Object, Object> {
        private final Object map;

        /**
         * Create a wrapper on an object to treat it as a Python mapping
         * producing a Java {@code Map<Object,Object>} from it.
         *
         * @param map to wrap
         */
        MapWrapper(Object map) { this.map = map; }

        // XXX Add a static method to check if possible.
        // Look for __getitem__, keys (etc. if write needed).

        /**
         * Determine whether an object claimed to be a map supports the
         * operations necessary to be read and iterated as a Python mapping.
         *
         * @param map to test
         * @return true iff {@code __getitem__} and {@code keys()} are defined.
         */
        static boolean check(Object map) { return check(map, true); }

        /**
         * Determine whether an object claimed to be a map supports the
         * operations necessary to be read, written and iterated as a Python
         * mapping.
         *
         * @param map to test
         * @return true iff {@code __getitem__}, {@code __setitem__} and
         *     {@code keys()} are defined.
         */
        static boolean checkWrite(Object map) { return check(map, false); }

        // Helper for check and checkWrite
        private static boolean check(Object map, boolean readonly) {
            BaseType type = BaseType.of(map);
            try {
                return type.hasFeature(KernelTypeFlag.HAS_GETITEM)
                        && Abstract.lookupAttr(map, "keys") != null
                        && type.hasFeature(KernelTypeFlag.HAS_SETITEM);
            } catch (Throwable e) {
                return false;
            }
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() { return new EntrySetImpl(); }

        @Override
        public Object get(Object key) {
            try {
                return getItem(map, key);
            } catch (Throwable t) {
                // Tunnel out non-Python errors as internal
                throw Util.asUnchecked(t, "during map.get(%.50s)", key);
            }
        }

        /**
         * @return {@code null}
         * @implNote This implementation always returns {@code null} as if there
         *     was previously no binding for the key in the map. This is to
         *     avoid the cost of interrogating the wrapped object.
         */
        @Override
        public Object put(Object key, Object value) {
            try {
                setItem(map, key, value);
                return null;
            } catch (Throwable t) {
                throw Util.asUnchecked(t, "during map.put(%.50s, ...)", key);
            }
        }

        /**
         * @return {@code null}
         * @implNote This implementation always returns {@code null} as if there
         *     was previously no binding for the key in the map. This is to
         *     avoid the cost of interrogating the wrapped object.
         */
        @Override
        public Object remove(Object key) {
            try {
                delItem(map, key);
                return null;
            } catch (Throwable t) {
                throw Util.asUnchecked(t, "during map.put(%.50s, ...)", key);
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("MapWrapper [map=");
            builder.append(map);
            builder.append("]");
            return builder.toString();
        }

        /**
         * An instance of this class is returned by
         * {@link MapWrapper#entrySet()}, and provides the view of the entries
         * in the object supplied to the constructor.
         */
        private class EntrySetImpl extends AbstractSet<Entry<Object, Object>> {

            @Override
            public Iterator<Entry<Object, Object>> iterator() { return new EntrySetIteratorImpl(); }

            @Override
            public int size() {
                try {
                    return PySequence.size(map);
                } catch (Throwable t) {
                    throw Util.asUnchecked(t, "during map.size()");
                }
            }
        }

        /**
         * An instance of this class is returned by
         * {@link EntrySetImpl#iterator()}.
         *
         * It is backed by a Python iterator on the underlying map.
         */
        private class EntrySetIteratorImpl implements Iterator<Entry<Object, Object>> {

            /**
             * A key object waits here that has been read from the map, e.g. to
             * answer {@link #hasNext()}, but has not yet been returned in a pair by
             * {@link #next()},
             */
            private final Object keyIterator;
            private Object nextKey = null;
            private Object currKey = null;
            private boolean exhausted = false;

            EntrySetIteratorImpl() {
                try {
                    keyIterator = Callables.callMethod(map, "keys");
                } catch (Throwable t) {
                    throw Util.asUnchecked(t, "getting iterator");
                }
            }

            /**
             * {@inheritDoc} If necessary, this method reads from the underlying
             * iterator, and the return value depends upon whether that raises a
             * Python {@code StopIteration}.
             */
            @Override
            public boolean hasNext() {
                if (exhausted) {
                    assert nextKey == null;
                    return false;
                } else if (nextKey == null) {
                    // This does not advance this iterator, but ...
                    try {
                        // ... we advance keyIterator to peek at next.
                        nextKey = Abstract.next(keyIterator);
                    } catch (PyStopIteration si) {
                        exhausted = true;
                        return false;
                    } catch (Throwable t) {
                        throw Util.asUnchecked(t, "during map.hasNext()");
                    }
                }
                assert nextKey != null;
                assert exhausted == false;
                return true;
            }

            @Override
            public Entry<Object, Object> next() {
                if (hasNext()) {
                    // hasNext()==true has already set nextKey
                    Object k = nextKey;
                    try {
                        Object v = getItem(map, k);
                        currKey = k;
                        nextKey = null;
                        return new SimpleEntry<Object, Object>(k, v);
                    } catch (Throwable t) {
                        throw Util.asUnchecked(t, "during map.next()");
                    }
                } else {
                    throw new NoSuchElementException("Python iterator");
                }
            }

            @Override
            public void remove() {
                if (currKey == null) {
                    // What java.util containers do
                    throw new IllegalStateException();
                } else {
                    try {
                        delItem(map, currKey);
                        currKey = null;
                    } catch (Throwable t) {
                        throw Util.asUnchecked(t, "during map.remove(%.50s)", currKey);
                    }
                }
            }
        }
    }
}
