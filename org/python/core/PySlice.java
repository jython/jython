package org.python.core;

public class PySlice extends PyObject {
	public PyObject start, stop, step;

    public static PyClass __class__;
	public PySlice(PyObject start, PyObject stop, PyObject step) {
	    super(__class__);
		this.start = start;
		this.stop = stop;
		this.step = step;
	}

	public PyString __str__() throws PyException {
		return new PyString(start.__repr__()+":"+stop.__repr__()+":"+step.__repr__());
	}

	public PyString __repr__() throws PyException {
		return new PyString("slice("+start.__repr__()+", "+
			stop.__repr__()+", "+
			step.__repr__()+")");
	}
}