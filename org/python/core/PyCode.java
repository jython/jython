// Copyright © Corporation for National Research Initiatives
package org.python.core;

public abstract class PyCode extends PyObject {
    public PyCode(PyClass c) { super(c); }
    public String co_name;

    abstract public PyObject call(PyFrame frame);
    abstract public PyObject call(PyObject args[], String keywords[], 
                                  PyObject globals, PyObject[] defaults);
    abstract public PyObject call(PyObject self, PyObject args[],
                                  String keywords[], 
                                  PyObject globals, PyObject[] defaults);
                
    abstract public PyObject call(PyObject globals, PyObject[] defaults);
    abstract public PyObject call(PyObject arg1, PyObject globals,
                                  PyObject[] defaults);
    abstract public PyObject call(PyObject arg1, PyObject arg2,
                                  PyObject globals, PyObject[] defaults);
    abstract public PyObject call(PyObject arg1, PyObject arg2, PyObject arg3,
                                  PyObject globals, PyObject[] defaults);
}
