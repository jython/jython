package org.python.expose.generate;

import org.python.core.Py;
import org.python.core.PySystemState;
import org.python.core.RegistryKey;

import junit.framework.TestCase;

/**
 * Initializes PySystemState in its setUp for use in subclasses.
 */
public abstract class InterpTestCase extends TestCase {

    public void setUp() throws Exception {
        System.setProperty(RegistryKey.PYTHON_CACHEDIR_SKIP, "true");
        PySystemState.initialize();
    }
}
