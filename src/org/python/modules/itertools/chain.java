/* Copyright (c) 2012 Jython Developers */
package org.python.modules.itertools;

import org.python.core.ArgParser;
import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

import java.util.ArrayList;

@ExposedType(name = "itertools.chain", base = PyObject.class)
public class chain extends PyObject {

    public static final PyType TYPE = PyType.fromClass(chain.class);
    private itertools.ItertoolsIterator iter;

    @ExposedGet
    public static PyString __doc__ = new PyString(
            "chain(*iterables) --> chain object\n\nReturn a chain object "
                    + "whose .next() method returns elements from the\nfirst iterable until it is exhausted, then elements"
                    + " from the next\niterable, until all of the iterables are exhausted.");

    public chain() {
        super();
    }

    public chain(PyType subType) {
        super(subType);
    }

    public chain(PyObject iterable) {
        super();
        chain___init__(iterable.__iter__());
    }

    @ExposedClassMethod
    public static final PyObject from_iterable(PyType type, PyObject iterable) {
        return new chain(iterable);
    }

    /**
     * Creates an iterator that iterates over a <i>chain</i> of iterables.
     */
    @ExposedNew
    @ExposedMethod
    final void chain___init__(final PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("chain", args, kwds, "iterables");
        ap.noKeywords();

        //ArgParser always returns a PyTuple - I wonder why we make it pass back a PyObject?
        PyTuple tuple = (PyTuple)ap.getList(0);
        chain___init__(tuple.__iter__());
    }

    private void chain___init__(final PyObject superIterator) {

        iter = new itertools.ItertoolsIterator() {

            int iteratorIndex = 0;
            PyObject currentIterator = new PyObject();

            public PyObject __iternext__() {
                PyObject nextIterable;
                PyObject next = null;
                do {
                    next = nextElement(currentIterator);
                    if (next != null) {
                        break;
                    }

                }
                while ((nextIterable = nextElement(superIterator)) != null &&
                       (currentIterator = nextIterable.__iter__()) != null);
                return next;
            }

        };
    }

    @ExposedMethod
    public PyObject __iter__() {
        return iter;
    }

    @ExposedMethod
    public PyObject next() {
        return iter.next();
    }

}
