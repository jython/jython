package org.python.core;

import java.util.Map;

/**
 * Python objects that have instance dictionaries implement this interface.
 */
public interface DictPyObject extends CraftedPyObject {
    /**
     * The dictionary of the instance, (not necessarily a Python {@code dict} or
     * writable. If the returned {@code Map} is not writable, it should throw a Java
     * {@code UnsupportedOperationException} on attempts to modify it.
     *
     * @implSpec A class that implements {@code PyObjectDict} should always return a
     *     mapping, which may be {@code Collections.emptyMap()} if the instance
     *     dictionary is intended to be permanently empty.
     * @return a mapping to treat like a dictionary (not {@code null}).
     */
    Map<Object, Object> getDict();
}
