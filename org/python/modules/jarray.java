// Copyright © Corporation for National Research Initiatives
package org.python.modules;
import org.python.core.*;

public class jarray {
    public static PyArray array(PyObject seq, char typecode) {
	return PyArray.array(seq, PyArray.char2class(typecode));
    }

    public static PyArray array(PyObject seq, Class type) {
	return PyArray.array(seq, type);
    }
    public static PyArray zeros(int n, char typecode) {
	return PyArray.zeros(n, PyArray.char2class(typecode));
    }

    public static PyArray zeros(int n, Class type) {
	return PyArray.zeros(n, type);
    }
}
