// Copyright © Corporation for National Research Initiatives
package org.python.core;

/**
 * An entry point for class that implements several function calls.
 * <P>
 * Used together with the PyTableCode class.
 *
 * @see PyTableCode
 */

public abstract class PyFunctionTable {
    abstract public PyObject call_function(int index, PyFrame frame);
}
