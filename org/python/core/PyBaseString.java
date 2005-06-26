package org.python.core;

/**
 * base class for jython strings.
 */

public abstract class PyBaseString extends PySequence {
    public static final String exposed_name="unicode";
    
    public PyBaseString() {
        super();
    }

    protected PyBaseString(PyType type) {
        super(type);
    }
}


