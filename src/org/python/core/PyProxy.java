// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * Common methods for all generated proxy classes.
 *
 * Proxy classes are created whenever a python class inherits from a java class. Instances of such a
 * python class consists of two objects:
 * <ul>
 * <li>An instance of the proxy class. The _getPyInstance() will return a reference to the
 * PyInstance.
 * <li>An instance of PyInstance. The PyInstance.javaProxy contains a reference to the proxy class
 * instance.
 * </ul>
 *
 * All proxy classes implement this interface.
 */
// This interface should be applicable to ANY class
// Choose names that are extremely unlikely to have conflicts
public interface PyProxy {

    /**
     * Associate a PyObject with this proxy instance. This is done during construction and
     * initialization of the proxy instance.
     */
    void _setPyInstance(PyObject proxy);

    /**
     * Return the associated PyObject.
     */
    PyObject _getPyInstance();

    /**
     * Associate an system state with this proxy instance. This is done during construction and
     * initialization of the proxy instance.
     */
    abstract public void _setPySystemState(PySystemState ss);

    /**
     * Return the associated system state.
     */
    PySystemState _getPySystemState();

    /**
     * Initialize the proxy instance. If the proxy is not initialized, this will call the python
     * constructor with <code>args</code>.
     * <p>
     * In some situations is it necessary to call the __initProxy__ method from the java superclass
     * ctor before the ctor makes call to methods that is overridden in python.
     * <p>
     * In most situation the __initProxy__ is called automatically by the jython runtime.
     */
    void __initProxy__(Object[] args);
}
