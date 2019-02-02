// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * Interface implemented by compiled modules which allow access to
 * to the module code object.
 */
public interface PyRunnable {
    /** Return the module's code object. */
    abstract public PyCode getMain();
}
