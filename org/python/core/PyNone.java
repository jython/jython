// Copyright © Corporation for National Research Initiatives
package org.python.core;

public class PyNone extends PySingleton {
    public PyNone() {
	super("None");
    }

    public boolean __nonzero__() {
	return false;
    }

    public Object __tojava__(Class c) {
	//Danger here.  java.lang.Object gets null not None
	if (c == PyObject.class)
	    return this;
	if (c.isPrimitive())
	    return Py.NoConversion;
	return null;
    }

    public String safeRepr() {
	return "'None' object";
    }
}
