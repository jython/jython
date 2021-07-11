package org.python.core;

/**
 * All Python object implementations that we write ourselves implement this
 * interface.
 */
public interface CraftedPyObject {
    /**
     * The Python {@code type} of this object.
     *
     * @return {@code type} of this object
     */
    PyType getType();
}
