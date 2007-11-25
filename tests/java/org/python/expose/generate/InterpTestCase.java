package org.python.expose.generate;

import org.python.core.PySystemState;

import junit.framework.TestCase;

/**
 * Initializes PySystemState in its setUp for use in subclasses.
 */
public abstract class InterpTestCase extends TestCase {

    public void setUp() {
        System.setProperty(PySystemState.PYTHON_CACHEDIR_SKIP, "true");
        PySystemState.initialize();
    }
}
