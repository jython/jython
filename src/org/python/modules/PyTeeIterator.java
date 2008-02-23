package org.python.modules;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyIterator;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "itertools.tee", base = PyObject.class)
public class PyTeeIterator extends PyIterator {

    private final int position;
    private int count = 0;
    private final PyObject iterator;
    private final Map<Integer, PyObject> buffer;
    private final int[] offsets;

    PyTeeIterator(PyObject iterator, Map<Integer, PyObject> buffer, int[] offsets, int position) {
        this.iterator = iterator;
        this.buffer = buffer;
        this.offsets = offsets;
        this.position = position;
    }
    
    @ExposedNew
    final static PyObject tee___new__ (PyNewWrapper new_, boolean init,
            PyType subtype, PyObject[] args, String[] keywords) {
        final int nargs = args.length;
        // CPython tee ignores keywords, so we do too!
        if (nargs < 1 || nargs > 1) {
            throw Py.TypeError("tee expected 1 arguments, got " + nargs);
        }
        return makeTees(args[0], 1)[0];
    }

    public static PyTeeIterator[] makeTees(PyObject iterable, int n) {
        if (n < 0) {
            throw Py.ValueError("n must be >= 0");
        }
        PyObject iterator = iterable.__iter__();
        Map<Integer, PyObject> buffer = new ConcurrentHashMap<Integer, PyObject>();
        int[] offsets = new int[n];
        PyTeeIterator[] tees = new PyTeeIterator[n];
        for (int i = 0; i < n; i++) {
            offsets[i] = -1;
            tees[i] = new PyTeeIterator(iterator, buffer, offsets, i);
        }
        return tees;
    }

    protected PyObject nextElement(PyObject pyIter) {
        PyObject element = null;
        try {
            element = pyIter.__iternext__();//next();
        } catch (PyException pyEx) {
            if (Py.matchException(pyEx, Py.StopIteration)) {
                // store exception - will be used by PyIterator.next()
                stopException = pyEx;
            } else {
                throw pyEx;
            }
        }
        return element;
    }

    @ExposedMethod
    public final PyObject tee_next() {
        return next();
    }
    
    public PyObject __iternext__() {
        final PyObject item;
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        for (int j = 0; j < offsets.length; j++) {
            if (max < offsets[j]) {
                max = offsets[j];
            }
            if (min > offsets[j]) {
                min = offsets[j];
            }
        }
        if (count > max) {
            item = nextElement(iterator);
            if (item != null) {
                buffer.put(count, item);
            }
        } else if (count < min) {
            item = buffer.remove(count);
        } else {
            item = buffer.get(count);
        }
        offsets[position] = count;
        count++;
        return item;
    }
}


