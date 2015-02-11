package org.python.modules.itertools;

import java.util.Map;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyIterator;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.Visitproc;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.util.Generic;

@ExposedType(name = "itertools.tee", base = PyObject.class,
    isBaseType = false, doc = PyTeeIterator.tee_doc)
public class PyTeeIterator extends PyIterator {

    public static final String tee_doc = "Iterator wrapped to make it copyable";

    private static class PyTeeData {
        private PyObject iterator;
        private int total;
        private Map<Integer, PyObject> buffer;
        public PyException stopException;
        private Object lock;

        public PyTeeData(PyObject iterator) {
            this.iterator = iterator;
            buffer = Generic.concurrentMap();
            total = 0;
            lock = new Object();
        }

        public PyObject getItem(int pos) {
            if (pos == total) {
                synchronized (lock) {
                    if (pos == total) {
                        PyObject obj = nextElement(iterator);
                        if (obj == null) {
                            return null;
                        }
                        buffer.put(total++, obj);
                    }
                }
            }
            return buffer.get(pos);
        }

        private PyObject nextElement(PyObject pyIter) {
            PyObject element = null;
            try {
                element = pyIter.__iternext__();
            } catch (PyException pyEx) {
                if (pyEx.match(Py.StopIteration)) {
                    stopException = pyEx;
                } else {
                    throw pyEx;
                }
            }
            return element;
        }
    }

    private int position;
    private PyTeeData teeData;

    public PyTeeIterator() {
        super();
    }

    public PyTeeIterator(PyType subType) {
        super(subType);
    }

    public PyTeeIterator(PyTeeData teeData) {
        this.teeData = teeData;
    }
    
    @ExposedNew
    final static PyObject tee___new__ (PyNewWrapper new_, boolean init,
            PyType subtype, PyObject[] args, String[] keywords) {
        final int nargs = args.length;
        // CPython tee ignores keywords, so we do too!
        if (nargs < 1 || nargs > 1) {
            throw Py.TypeError("tee expected 1 arguments, got " + nargs);
        }
        return fromIterable(args[0]);
    }

    public static PyObject[] makeTees(PyObject iterable, int n) {
        if (n < 0) {
            throw Py.ValueError("n must be >= 0");
        }

        PyObject[] tees = new PyObject[n];

        if (n == 0) {
            return tees;
        }

        PyObject copyFunc = iterable.__findattr__("__copy__");
        if (copyFunc == null) {
            tees[0] = fromIterable(iterable);
            copyFunc = tees[0].__getattr__("__copy__");
        }
        else {
            tees[0] = iterable;
        }
        for (int i = 1; i < n; i++) {
            tees[i] = copyFunc.__call__();
        }
        return tees;
    }

    private static PyTeeIterator fromIterable(PyObject iterable) {
        if (iterable instanceof PyTeeIterator) {
            return ((PyTeeIterator) iterable).tee___copy__();
        }
        PyObject iterator = (PyObject)iterable.__iter__();
        PyTeeData teeData = new PyTeeData(iterator);
        return new PyTeeIterator(teeData);
    }

    @ExposedMethod
    public final PyObject tee_next() {
        return next();
    }
    
    public PyObject __iternext__() {
        PyObject obj = teeData.getItem(position++);
        if (obj == null) {
            stopException = teeData.stopException;
        }
        return obj;
    }

    @ExposedMethod
    public final PyTeeIterator tee___copy__() {
        return new PyTeeIterator(teeData);
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal = super.traverse(visit, arg);
        if (retVal != 0) {
            return retVal;
        }
        if (teeData != null) {
            if (teeData.iterator != null) {
                retVal = visit.visit(teeData.iterator, arg);
                if (retVal != 0) {
                    return retVal;
                }
            }
            if (teeData.buffer != null) {
                for (PyObject ob: teeData.buffer.values()) {
                    if (ob != null) {
                        retVal = visit.visit(ob, arg);
                        if (retVal != 0) {
                            return retVal;
                        }
                    }
                }
            }
            if (teeData.stopException != null) {
                retVal = teeData.stopException.traverse(visit, arg);
                if (retVal != 0) {
                    return retVal;
                }
            }
        }
        return 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) throws UnsupportedOperationException {
        if (ob == null) {
            return false;
        } else if (super.refersDirectlyTo(ob)) {
            return true;
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
