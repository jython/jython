package org.python.types;

import org.python.core.PyType;

/**
 * All Python object implementations that we write ourselves implement this
 * interface.
 */
public interface WithClass {
    /**
     * The Python {@code type} of this object.
     *
     * @return {@code type} of this object
     */
    PyType getType();
}
