package org.python.modules;
import org.python.core.*;

public class operator {
	public static PyObject __add__(PyObject o1, PyObject o2) {
		return o1._add(o2);
	}
	public static PyObject add(PyObject o1, PyObject o2) {
		return o1._add(o2);
	}
	
}