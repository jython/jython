package org.python.core.adapter;

import org.python.core.PyObject;

/**
 * PyObjectAdapters turn Java Objects into PyObjects.
 */
public interface PyObjectAdapter {

    /**
     * Returns true if o can be adapted by this adapter.
     */
    public abstract boolean canAdapt(Object o);

    /**
     * Returns the PyObject version of o or null if canAdapt(o) returns false.
     */
    public abstract PyObject adapt(Object o);
}