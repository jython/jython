// Copyright © Corporation for National Research Initiatives
package org.python.core;

public class PyFunction extends PyObject {
    public String __name__;
    public PyObject __doc__;
    public PyObject func_globals;
    public PyObject[] func_defaults;
    public PyCode func_code;

    public static PyClass __class__;

    public PyFunction(PyObject globals, PyObject[] defaults, PyCode code,
		      PyObject doc)
    {
        super(__class__);
	func_globals = globals;
	__name__ = code.co_name;
        if (doc == null)
	    __doc__ = Py.None;
        else
	    __doc__ = doc;
	func_defaults = defaults;
	func_code = code;
    }
	
    public PyFunction(PyObject globals, PyObject[] defaults, PyCode code) {
	this(globals, defaults, code, null);
    }
	
    public PyObject _doget(PyObject container) {
	return new PyMethod(container, this);
    }
    public PyObject __call__() {
	return func_code.call(func_globals, func_defaults);
    }	
    public PyObject __call__(PyObject arg) {
	return func_code.call(arg, func_globals, func_defaults);
    }
    public PyObject __call__(PyObject arg1, PyObject arg2) {
	return func_code.call(arg1, arg2, func_globals, func_defaults);
    }
    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
	return func_code.call(arg1, arg2, arg3, func_globals, func_defaults);
    }
	
    public PyObject __call__(PyObject[] args, String[] keywords) {
	return func_code.call(args, keywords, func_globals, func_defaults);
    }
    public PyObject __call__(PyObject arg1, PyObject[] args, String[] keywords)
    {
	return func_code.call(arg1, args, keywords, func_globals,
			      func_defaults);
    }
    public String toString() {
	return "<function "+__name__+" at "+hashCode()+">";
    }
}
