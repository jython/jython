package org.python.core;

/**
 * base class for jython strings.
 */

public abstract class PyBaseString extends PySequence {
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="basestring";

    public static final Class exposed_base=PyObject.class;

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py
	
    public PyBaseString() {
        super();
    }

    protected PyBaseString(PyType type) {
        super(type);
    }
}


