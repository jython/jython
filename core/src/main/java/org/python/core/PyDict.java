// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.python.base.InterpreterError;

/**
 * The Python {@code dict} object. The Java API is provided directly
 * by the base class implementing {@code Map}, while the Python API
 * has been implemented on top of the Java one.
 */
public class PyDict extends AbstractMap<Object, Object> implements CraftedPyObject {

    /** The type of Python object this class implements. */
    public static final PyType TYPE = PyType.fromSpec( //
            new PyType.Spec("dict", MethodHandles.lookup()));

    /** The Python type of this instance. */
    protected final PyType type;

    /** The dictionary as a hash map preserving insertion order. */
    private final LinkedHashMap<Key, Object> map = new LinkedHashMap<>();

    /**
     * Construct an empty dictionary of a specified Python sub-class of
     * {@code dict}.
     *
     * @param type sub-type for which this is being created
     */
    protected PyDict(PyType type) { this.type = type; }

    /**
     * Construct a dictionary of a specified Python sub-class of
     * {@code dict}, filled by copying from a given Java map.
     *
     * @param <K> key type of incoming map
     * @param <V> value type of incoming map
     * @param type sub-type for which this is being created
     * @param map Java map from which to copy
     */
    protected <K, V> PyDict(PyType type, Map<K, V> map) {
        this(type);
        // Cannot bulk add since keys may need Pythonising
        for (Map.Entry<K, V> e : map.entrySet()) { put(e.getKey(), e.getValue()); }
    }

    /** Construct an empty {@code dict}. */
    public PyDict() { this(TYPE); }

    /**
     * Create a {@code dict} and add entries from key-value pairs that
     * are supplied as successive values in an array slice. (This method
     * supports the CPython byte code interpreter.)
     *
     * @param stack array containing key-value pairs
     * @param start index of first key
     * @param count number of pairs
     * @return a new {@code dict}
     */
    static PyDict fromKeyValuePairs(Object[] stack, int start, int count) {
        PyDict dict = new PyDict(TYPE);
        for (int i = 0, p = start; i < count; i++) { dict.put(stack[p++], stack[p++]); }
        return dict;
    }

    /**
     * Create a {@code dict} and add entries from key-value pairs.
     *
     * @param tuples specifying key-value pairs to enter
     * @return new {@code dict}
     */
    public static PyDict fromKeyValuePairs(PyTuple... tuples) {
        PyDict dict = new PyDict(TYPE);
        for (int i = 0; i < tuples.length; i++) {
            PyTuple t = tuples[i];
            if (t.size() == 2) {
                dict.put(t.get(0), t.get(1));
            } else {
                throw new ValueError(KV_TUPLE_LENGTH, i, t.size());
            }
        }
        return dict;
    }

    @Override
    public PyType getType() { return type; }

    @Override
    public String toString() { return Py.defaultToString(this); }

    /**
     * Override {@code Map.get} to give keys Python semantics.
     *
     * @param key whose associated value is to be returned
     * @return value at {@code key} or {@code null} if not found
     */
    @Override
    public Object get(Object key) { return map.get(toKey(key)); }

    /**
     * Override {@code Map.put} to give keys Python semantics.
     *
     * @param key with which the specified value is to be associated
     * @param value to be associated
     * @return previous value associated
     */
    @Override
    public Object put(Object key, Object value) { return map.put(toKey(key), value); }

    /**
     * Override {@code Map.putIfAbsent} to give keys Python semantics.
     *
     * @param key with which the specified value is to be associated
     * @param value to be associated
     * @return previous value associated
     */
    @Override
    public Object putIfAbsent(Object key, Object value) {
        return map.putIfAbsent(toKey(key), value);
    }

    /** Modes for use with {@link #merge(Object, MergeMode)}. */
    enum MergeMode {
        /**
         * Ignore the operation if the key is already in the map. (CPython
         * mode 0: first occurrence wins).
         */
        IF_ABSENT,
        /**
         * Overwrite any existing entry with the same key. (CPython mode 1:
         * last occurrence wins).
         */
        PUT,
        /**
         * Raise a {@link KeyError} if the key is already in the map.
         * (CPython mode 2.)
         */
        UNIQUE
    }

    /**
     * Optimised get on this {@code PyDict} and some other {@code Map},
     * specifically to support the {@code LOAD_GLOBAL} opcode. We avoid
     * the cost of a second key holder if the second mapping is also a
     * {@code PyDict}, which it commonly is.
     *
     * @param builtins final mapping to search
     * @param key to find
     * @return found object or {@code null}
     */
    // Compare CPython _PyDict_LoadGlobal in dictobject.c
    final Object loadGlobal(Map<Object, Object> builtins, String key) {
        Key k = toKey(key);
        Object v = map.get(k);
        if (v == null) {
            if (builtins instanceof PyDict) {
                v = ((PyDict)builtins).map.get(k);
            } else {
                v = builtins.get(key);
            }
        }
        return v;
    }

    /**
     * Update this dictionary from a sequence of key-value pairs, in the
     * chosen mode. The sequence is any iterable object producing
     * iterable objects of length 2, typically a {@code list} of
     * {@code tuple}s.
     *
     * @param seq sequence of KV pairs to merge
     * @param mode merge policy
     * @throws TypeError
     * @throws KeyError
     * @throws Throwable
     */
    // Compare CPython PyDict_MergeFromSeq2 in dictobject.c
    void mergeFromSeq(Object seq, MergeMode mode) throws TypeError, KeyError, Throwable {

        // Python-iterate over the sequence
        Object it = Abstract.getIterator(seq);
        for (int i = 0;; i++) {
            Object item = Abstract.next(it); // seq[i]
            // Convert item to List and verify length 2.
            try {
                List<Object> fast = PySequence.fastList(item, null);
                if (fast.size() != 2) { throw new ValueError(KV_TUPLE_LENGTH, i, fast.size()); }
                // Update/merge with this (key, value) pair.
                mergeSingle(fast.get(0), fast.get(1), mode);
            } catch (TypeError te) {
                /*
                 * We could not give PySequence.fastList a custom exception supplier
                 * because the message refers to non-final i.
                 */
                throw new TypeError(CANNOT_CONVERT_SEQ, i);
            }
        }
    }

    // slot functions -------------------------------------------------

    @SuppressWarnings("unused")
    private Object __repr__() throws Throwable { return PyObjectUtil.mapRepr(this); }

    @SuppressWarnings("unused")
    private Object __ne__(Object o) {
        if (TYPE.check(o)) {
            return !compareEQ((PyDict)o);
        } else {
            return Py.NotImplemented;
        }
    }

    @SuppressWarnings("unused")
    private Object __eq__(Object o) {
        if (TYPE.check(o)) {
            return compareEQ((PyDict)o);
        } else {
            return Py.NotImplemented;
        }
    }

    @SuppressWarnings("unused")
    private Object __getitem__(Object key) {
        // This may be over-simplifying things but ... :)
        return get(key);
    }

    @SuppressWarnings("unused")
    private void __setitem__(Object key, Object value) {
        // This may be over-simplifying things but ... :)
        put(key, value);
    }

    @SuppressWarnings("unused")
    private void __delitem__(Object key) {
        // This may be over-simplifying things but ... :)
        remove(key);
    }

    // methods --------------------------------------------------------

    /**
     * Update the dictionary with the contents of another mapping
     * object, allowing replacement of matching keys. This supports the
     * {@code UPDATE_DICT} opcode.
     *
     * @param o to merge into this dictionary
     */
    // Compare CPython PyDict_Update in dictobject.c
    void update(Object o) { mergeObject(o, MergeMode.PUT); }

    /**
     * Merge the contents of another mapping into this dictionary
     * allowing replacement of matching keys only as specified.
     *
     * @param o to merge into this dictionary
     * @param mode policy on replacement
     */
    // Compare CPython _PyDict_MergeEx in dictobject.c
    void merge(Object o, MergeMode mode) { mergeObject(o, mode); }

    // Non-Python API -------------------------------------------------

    /**
     * A {@code PyDict} is a {@code Map<Object, Object>}, but contains a
     * private implementation of {@code java.utilMap<Key, Object>}. We
     * use this class {@code Key} so that when Java needs key hashes or
     * comparisons, it receives the hash or comparison that Python would
     * produce. An object with the {@code Key} interface defines the
     * standard Java {@code hashCode()} and {@code equals()} to return
     * the answers Python would give for {@code __hash__} and
     * {@code __eq__}.
     * <p>
     * Some implementations of Python objects (e.g. the
     * {@link PyUnicode} implementation of {@code str}) implement
     * {@code PyDict.Key}, and can give Python semantics to hash and
     * comparison directly. Other implementations (e.g. a Java
     * {@code String} implementation of {@code str}) have to be wrapped
     * in a {@link KeyHolder} that implements {@code PyDict.Key}.
     * Conversely, when any method requires the keys of a Python
     * {@code dict}, the {@code Key} must yield up the original Python
     * object it contains.
     * <p>
     * An implementation of a Python type that allows Python sub-classes
     * must respect re-definition of the corresponding special methods.
     * This is best done by by calling abstract API
     * {@link Abstract#hash(Object)}, etc..
     */
    interface Key {

        /**
         * If this object is a holder for the actual key, return the actual
         * key. By default, return {@code this}. Python object
         * implementations that implement {@link Key#hashCode()} and
         * {@link Key#equals(Object)}, do not usually override this method.
         *
         * @return the underlying key object (by default {@code this})
         */
        default Object get() { return this; }

        /**
         * Python objects that implement the interface {@code Key}, define
         * this method so that calls from Java libraries to
         * {@code Object.hashCode()} receive the hash defined by Python. A
         * sufficient implementation is:<Pre>
         * &#64;Override
         * public int hashCode() throws PyException {
         *     return PyDict.pythonHash(this);
         * }
         * </pre> Where it is known {@code __hash__} cannot have been
         * redefined, object implementations may have a shorter option.
         *
         * @throws PyException from {@code __hash__} implementations
         */
        @Override
        public int hashCode() throws PyException;

        /**
         * Python objects that implement the interface {@code Key}, define
         * this method so that calls from Java libraries to
         * {@code Object.equals(Object)} are answered with Python semantics.
         * A sufficient implementation is:<Pre>
         * &#64;Override
         * public int equals(Object obj) throws PyException {
         *     return PyDict.pythonEquals(this, obj);
         * }
         * </pre> Objects that provide their own specialised implementation
         * of {@code equals}, receiving a {@code Key} a object as the
         * {@code other} argument, must dereference it with {@code get()}
         * and work on those contents. An idiom like this may be used:<pre>
         * &#64;Override
         * public boolean equals(Object other) {
         *     if (other instanceof PyDict.Key)
         *         other = ((PyDict.Key) other).get();
         *     // ... rest of implementation
         * }
         *  </pre>
         *
         * @throws PyException from {@code __eq__} implementations
         */
        @Override
        public boolean equals(Object other) throws PyException;
    }

    /**
     * This is a wrapper that gives Python semantics to objects used as
     * keys. When using a Java collection, it is necessary to intercept
     * the calls Java will make to {@code Object.hashCode} and
     * {@code Object.equals}, and direct them to Python {@code __hash__}
     * and {@code __eq__}.
     */
    static class KeyHolder implements Key {

        /** The actual key this object is holding. */
        private final Object key;

        /**
         * Create a key on the given object Python {@code __eq__}
         * definitions on objects offered as keys.
         *
         * @param key to wrap
         * @throws PyException from {@code __eq__}
         */
        KeyHolder(Object key) { this.key = key; }

        /** Return the actual object held by this {@code Key} object. */
        @Override
        public Object get() { return key; }

        @Override
        public int hashCode() { return pythonHash(this); }

        /**
         * Impose Python {@code __eq__} definitions on objects offered as
         * keys.
         *
         * @throws PyException from {@code __eq__}
         */
        @Override
        public boolean equals(Object other) throws PyException { return pythonEquals(this, other); }

        @Override
        public String toString() { return String.format("KeyHolder(%s)", key); }
    }

    /**
     * Turn an object into a {@link Key} suitable for lookup in
     * {@link #map}.
     *
     * @param key to return or wrap
     */
    private static Key toKey(Object key) {
        if (key instanceof Key)
            return (Key)key;
        else
            return new KeyHolder(key);
    }

    /**
     * Convenience function for Python objects that implement
     * {@link PyDict.Key}, to impose Python semantics for {@code hash()}
     * on {@code Object.hashCode}. See {@link PyDict.Key#hashCode()}.
     *
     * @param key to hash
     * @return the hash
     * @throws PyException from {@code __hash__} implementations
     */
    public static int pythonHash(Key key) throws PyException {
        try {
            return Abstract.hash(key.get());
        } catch (PyException e) {
            // A PyException is allowed to propagate as itself
            throw e;
        } catch (Throwable e) {
            // Tunnel out non-Python errors as internal
            throw new InterpreterError(e, "during hash(%s)", PyType.of(key));
        }
    }

    /**
     * Convenience function for Python objects that implement
     * {@link PyDict.Key}, to impose Python semantics for {@code ==} on
     * {@code Object.equals}. See {@link Key#equals(Object)}.
     *
     * @param key to test equal
     * @param other to test equal
     * @return whether equal
     * @throws PyException from {@code __eq__} implementations
     */
    public static boolean pythonEquals(Key key, Object other) throws PyException {

        if (other instanceof Key) {
            // De-reference the key to its contents
            other = ((Key)other).get();
        }

        // Quick answer if it contains the same object
        Object self = key.get();
        if (other == self) { return true; }

        // Otherwise, make a full comparison
        try {
            Object r = Comparison.EQ.apply(self, other);
            return Abstract.isTrue(r);
        } catch (PyException e) {
            // A PyException is allowed to propagate as itself
            throw e;
        } catch (Throwable e) {
            // Tunnel out non-Python errors as internal
            throw new InterpreterError(e, "during equals(%s, %s)", PyType.of(self),
                    PyType.of(other));
        }
    }

    // Map interface --------------------------------------------------

    @Override
    public Set<Entry<Object, Object>> entrySet() { return new EntrySetImpl(); }

    /**
     * An instance of this class is returned by
     * {@link PyDict#entrySet()}, and provides the view of the entries
     * in the {@code PyDict} mentioned there.
     * <p>
     * It is probably also the backing for a {@code dict_keys}.
     */
    private class EntrySetImpl extends AbstractSet<Entry<Object, Object>> {

        @Override
        public Iterator<Entry<Object, Object>> iterator() { return new EntrySetIteratorImpl(); }

        @Override
        public int size() { return map.size(); }
    }

    /**
     * An instance of this class is returned by
     * {@link EntrySetImpl#iterator()}. It is backed by an iterator on
     * the underlying {@link #map}, and its job is to return an entry in
     * which the {@link PyDict#Key} has been replaced with its contained
     * object, the true key at the Python level.
     */
    private class EntrySetIteratorImpl implements Iterator<Entry<Object, Object>> {

        /** Backing iterator on the "real" implementation. */
        private final Iterator<Entry<Key, Object>> mapIterator = map.entrySet().iterator();

        @Override
        public boolean hasNext() { return mapIterator.hasNext(); }

        /**
         * {@inheritDoc} The difference from the underlying
         * {@link mapIterator} is that the key in the entry returned by this
         * method is the object embedded in the {@link KeyHolder}, which is
         * the key as far as Python is concerned.
         */
        @Override
        public Entry<Object, Object> next() {
            Entry<Key, Object> e = mapIterator.next();
            return new SimpleEntry<Object, Object>(e.getKey().get(), e.getValue());
        }

        @Override
        public void remove() { mapIterator.remove(); }
    }

    // plumbing -------------------------------------------------------

    private static final String ELEMENT_N = "dictionary update sequence element %d ";
    private static final String CANNOT_CONVERT_SEQ =
            "cannot convert " + ELEMENT_N + "to a sequence";
    private static final String KV_TUPLE_LENGTH = ELEMENT_N + "has length %d; 2 is required";

    /**
     * Compare this dictionary with the other {@code dict} for equality.
     *
     * @param other {@code dict}
     * @return {@code true} if equal, {@code false} if not.
     */
    private boolean compareEQ(PyDict other) {
        try {
            // Must we equal size to be equal
            if (other.size() != size()) { return false; }
            // All the keys must map to equal values.
            for (Map.Entry<Object, Object> e : entrySet()) {
                Object w = other.get(e.getKey());
                if (w == null)
                    return false;
                else if (!Abstract.richCompareBool(e.getValue(), w, Comparison.EQ))
                    return false;
            }
            // The dictionaries matched at every key.
            return true;
        } catch (PyException e) {
            // It's ok to throw legitimate Python exceptions
            throw e;
        } catch (Throwable t) {
            throw new InterpreterError(t, "non-Python exeption in comparison");
        }
    }

    /**
     * Merge a key-value pair into this {@code dict}.
     *
     * @param k key
     * @param v value
     * @param mode what to do about duplicates
     * @throws KeyError on duplicate key (if {@code mode == }
     *     {@link MergeMode#UNIQUE})
     */
    private void mergeSingle(Object k, Object v, MergeMode mode) throws KeyError {
        if (mode == MergeMode.PUT) {
            put(k, v);
        } else {
            // Sensitive to whether already present
            Object u = putIfAbsent(k, v);
            if (u != null && mode == MergeMode.UNIQUE) { throw new KeyError.Duplicate(k); }
        }
    }

    /**
     * Merge the mapping {@code src} into this {@code dict}.
     *
     * @param src a mapping to merge in
     * @param mode what to do about duplicates
     * @throws KeyError on duplicate key (if {@code mode == }
     *     {@link MergeMode#UNIQUE})
     */
    // Compare CPython dict_merge in dictobject.c
    private void mergeObject(Object src, MergeMode mode) throws KeyError, AttributeError {

        // Try to make src a Java Map
        Map<Object, Object> map;
        if (src instanceof PyDict) {
            map = (PyDict)src;
            // XXX MissingFeature("Non-dict wrapped as mapping");
            // } else if (PyMapping.MapWrapper.check(src)) {
            // map = PyMapping.map(src);
        } else {
            throw new AttributeError("'%.200s' object is not a mapping", PyType.of(src).getName());
        }

        // Now update according to the mode, using Java semantics.
        if (mode == MergeMode.PUT) {
            putAll(map);
        } else {
            // Sensitive to whether already present
            for (Map.Entry<Object, Object> e : map.entrySet()) {
                Object k = e.getKey();
                Object u = putIfAbsent(k, e.getValue());
                if (u != null && mode == MergeMode.UNIQUE) { throw new KeyError.Duplicate(k); }
            }
        }
    }
}
