package org.python.core;

import java.util.Map;

import org.python.base.MissingFeature;

/**
 * Abstract API for operations on mapping types, corresponding to
 * CPython methods defined in {@code abstract.h} and with names
 * like: {@code PyMapping_*}.
 */
public class PyMapping extends PySequence {

    protected PyMapping() {}    // only static methods here

    /**
     * Return the mapping object {@code o} as a Java {@code Map}. If
     * {@code o} is one of several built-in types that implement Java
     * {@code Map<Object, Object>}, this will be the object itself.
     * Otherwise, it will be an adapter on the provided object.
     *
     * @param <K> Object (distinguished here for readability)
     * @param <V> Object (distinguished here for readability)
     * @param o to present as a map
     * @return the map
     */
    static Map<Object, Object> map(Object o) {
        if (PyDict.TYPE.check(o)) {
            return (PyDict)o;
        } else {
            // return new MapWrapper<Object, Object>(o);
            throw new MissingFeature("Non-dict wrapped as mapping");
        }
    }
}
