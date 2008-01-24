package org.python.modules;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyTuple;

public class PyTee extends PyObject {

    private final PyObject iterator;
    private final ConcurrentMap buffer;
    private final int[] offsets;
    private PyTeeIterator[] tees;


    public PyTee(PyObject iterable, final int n) {
        if (n < 0) {
            throw Py.ValueError("n must be >= 0");
        }
        iterator = iterable.__iter__();
        buffer = new ConcurrentHashMap();
        offsets = new int[n];
        tees = new PyTeeIterator[n];
        for (int i = 0; i < n; i++) {
            offsets[i] = -1;
            tees[i] = new PyTeeIterator(iterator, buffer, offsets, i);
        }
    }

    public static PyTuple makeTees(PyObject iterable, final int n) {
        return new PyTuple((new PyTee(iterable, n)).tees);
    }
    
    public PyTeeIterator[] getTees() {
        return tees;
    }

}
   

